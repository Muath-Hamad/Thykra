package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.RecapsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.AlbumMemberSummary
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.model.PublicMediaDto
import com.jameeli.thykra.model.PublicReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.model.RecapDto
import com.jameeli.thykra.model.RecapStatus
import com.jameeli.thykra.model.RecapViewDto
import com.jameeli.thykra.storage.StorageService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class RecapRepository(private val storageService: StorageService) {

    sealed interface BuildResult {
        data class NotEnoughMedia(val needed: Int) : BuildResult
        data class Built(val recap: RecapDto) : BuildResult
    }

    /**
     * Builds a recap synchronously and stores it READY.
     *
     * Builder rules (per Screen Specs doc 3):
     * - Considers ACTIVE media only; fewer than [MIN_MEDIA] items gates with NotEnoughMedia(needed).
     * - Effective instant = takenAt ?: uploadedAt; days are UTC calendar dates of that instant.
     * - Chooses min(total, 24) items, round-robin across days in date order; within a day items
     *   are ranked by reactions+comments desc, then earliest effective instant. Chosen items are
     *   stored in chronological (effective instant) order — the edition reads front to back.
     * - Title: "N days in {albumTitle}" where N = number of distinct calendar days the album's
     *   media covers, or just "{albumTitle}" for a single-day trip.
     * - coverMediaId = the most-reacted chosen item (ties -> earliest effective instant).
     * - periodStart/End = min/max effective instants across ALL considered media (the trip span).
     */
    suspend fun build(albumId: UUID, requestedBy: UUID): BuildResult = newSuspendedTransaction {
        val albumRow = AlbumsTable.selectAll().where { AlbumsTable.id eq albumId }.single()
        val albumTitle = albumRow[AlbumsTable.title]

        val mediaRows = MediaTable.selectAll()
            .where { (MediaTable.albumId eq albumId) and (MediaTable.status eq MediaStatus.ACTIVE.name) }
            .toList()
        if (mediaRows.size < MIN_MEDIA) {
            return@newSuspendedTransaction BuildResult.NotEnoughMedia(needed = MIN_MEDIA - mediaRows.size)
        }

        val mediaIds = mediaRows.map { it[MediaTable.id].value }
        val reactionCounts = countByMedia(ReactionsTable.mediaId, ReactionsTable.id.count(), mediaIds)
        val commentCounts = countByMedia(CommentsTable.mediaId, CommentsTable.id.count(), mediaIds)

        data class Item(
            val id: UUID,
            val effective: Instant,
            val reactions: Int,
            val engagement: Int,
            val row: ResultRow
        )

        val items = mediaRows.map { row ->
            val id = row[MediaTable.id].value
            val reactions = reactionCounts[id] ?: 0
            Item(
                id = id,
                effective = row[MediaTable.takenAt] ?: row[MediaTable.uploadedAt],
                reactions = reactions,
                engagement = reactions + (commentCounts[id] ?: 0),
                row = row
            )
        }

        val byDay = items.groupBy { it.effective.toLocalDateTime(TimeZone.UTC).date }
        val dayCount = byDay.keys.size

        // Round-robin across days (date order); within a day: most engaged first, then earliest.
        val queues = byDay.keys.sorted().map { day ->
            ArrayDeque(
                byDay.getValue(day).sortedWith(
                    compareByDescending<Item> { it.engagement }.thenBy { it.effective }
                )
            )
        }
        val target = items.size.coerceAtMost(MAX_MEDIA)
        val chosen = mutableListOf<Item>()
        while (chosen.size < target) {
            for (queue in queues) {
                if (chosen.size >= target) break
                queue.removeFirstOrNull()?.let { chosen.add(it) }
            }
        }
        val orderedChosen = chosen.sortedBy { it.effective }
        val cover = chosen.sortedWith(
            compareByDescending<Item> { it.reactions }.thenBy { it.effective }
        ).first()

        val now = Clock.System.now()
        val recapId = UUID.randomUUID()
        val shareToken = UUID.randomUUID().toString()
        val title = if (dayCount > 1) "$dayCount days in $albumTitle" else albumTitle
        val periodStart = items.minOf { it.effective }
        val periodEnd = items.maxOf { it.effective }
        val chosenIdStrings = orderedChosen.map { it.id.toString() }

        RecapsTable.insert {
            it[RecapsTable.id] = recapId
            it[RecapsTable.albumId] = albumId
            it[RecapsTable.requestedBy] = requestedBy
            it[RecapsTable.title] = title
            it[RecapsTable.coverMediaId] = cover.id
            it[RecapsTable.mediaIds] = json.encodeToString(chosenIdStrings)
            it[RecapsTable.periodStart] = periodStart
            it[RecapsTable.periodEnd] = periodEnd
            it[RecapsTable.status] = RecapStatus.READY.name
            it[RecapsTable.shareToken] = shareToken
            it[RecapsTable.createdAt] = now
        }

        BuildResult.Built(
            RecapDto(
                id = recapId.toString(),
                albumId = albumId.toString(),
                albumTitle = albumTitle,
                title = title,
                coverMediaId = cover.id.toString(),
                coverUrl = publicUrl(cover.row),
                mediaIds = chosenIdStrings,
                periodStart = periodStart,
                periodEnd = periodEnd,
                status = RecapStatus.READY,
                shareToken = shareToken,
                createdAt = now
            )
        )
    }

    /** Recaps across all albums the user is a member of, newest first. */
    suspend fun listForUser(userId: UUID): List<RecapDto> = newSuspendedTransaction {
        val albumIds = AlbumMembersTable
            .selectAll().where { AlbumMembersTable.userId eq userId }
            .map { it[AlbumMembersTable.albumId].value }
        if (albumIds.isEmpty()) return@newSuspendedTransaction emptyList()
        val rows = (RecapsTable innerJoin AlbumsTable)
            .selectAll()
            .where { RecapsTable.albumId inList albumIds }
            .orderBy(RecapsTable.createdAt, SortOrder.DESC)
            .toList()
        toRecapDtos(rows)
    }

    suspend fun listForAlbum(albumId: UUID): List<RecapDto> = newSuspendedTransaction {
        val rows = (RecapsTable innerJoin AlbumsTable)
            .selectAll()
            .where { RecapsTable.albumId eq albumId }
            .orderBy(RecapsTable.createdAt, SortOrder.DESC)
            .toList()
        toRecapDtos(rows)
    }

    /**
     * Public recap reader. Works regardless of album visibility — the owner explicitly
     * shared the recap link. Exposes only the chosen media (with anonymous reaction
     * counts) and the uploaders of those media, never the album's member list.
     */
    suspend fun findViewByShareToken(shareToken: String): RecapViewDto? = newSuspendedTransaction {
        val row = (RecapsTable innerJoin AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = AlbumsTable.ownerId,
                otherColumn = UsersTable.id
            )
            .selectAll()
            .where { RecapsTable.shareToken eq shareToken }
            .singleOrNull()
            ?: return@newSuspendedTransaction null

        val idStrings: List<String> = json.decodeFromString(row[RecapsTable.mediaIds])
        val ids = idStrings.map { UUID.fromString(it) }
        val mediaById = if (ids.isEmpty()) emptyMap() else {
            MediaTable.selectAll().where { MediaTable.id inList ids }
                .associateBy { it[MediaTable.id].value }
        }

        // Reaction counts for all chosen media in one grouped query — counts only.
        val reactionCountExpr = ReactionsTable.id.count()
        val reactionSummaries: Map<UUID, List<PublicReactionSummaryDto>> = if (ids.isEmpty()) emptyMap() else {
            ReactionsTable
                .select(ReactionsTable.mediaId, ReactionsTable.type, reactionCountExpr)
                .where { ReactionsTable.mediaId inList ids }
                .groupBy(ReactionsTable.mediaId, ReactionsTable.type)
                .map { r ->
                    r[ReactionsTable.mediaId].value to PublicReactionSummaryDto(
                        type = ReactionType.valueOf(r[ReactionsTable.type]),
                        count = r[reactionCountExpr].toInt()
                    )
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, summaries) -> summaries.sortedByDescending { it.count } }
        }

        val media = ids.mapNotNull { mediaById[it] }.map { mediaRow ->
            val mediaId = mediaRow[MediaTable.id].value
            PublicMediaDto(
                id = mediaId.toString(),
                type = MediaType.valueOf(mediaRow[MediaTable.type]),
                url = storageService.getPublicUrl(mediaRow[MediaTable.storageKey]),
                thumbnailUrl = mediaRow[MediaTable.thumbnailKey]?.let { storageService.getPublicUrl(it) },
                width = mediaRow[MediaTable.width],
                height = mediaRow[MediaTable.height],
                takenAt = mediaRow[MediaTable.takenAt],
                uploadedAt = mediaRow[MediaTable.uploadedAt],
                reactionSummary = reactionSummaries[mediaId] ?: emptyList()
            )
        }

        val uploaderIds = ids.mapNotNull { mediaById[it]?.get(MediaTable.uploaderId)?.value }.distinct()
        val contributors = if (uploaderIds.isEmpty()) emptyList() else {
            UsersTable.selectAll().where { UsersTable.id inList uploaderIds }
                .map {
                    AlbumMemberSummary(
                        userId = it[UsersTable.id].value.toString(),
                        displayName = it[UsersTable.displayName],
                        avatarUrl = it[UsersTable.avatarUrl]
                    )
                }
        }

        RecapViewDto(
            recap = toRecapDto(row, coverUrl = row[RecapsTable.coverMediaId]?.let { coverId ->
                mediaById[coverId]?.let { publicUrl(it) } ?: coverUrlFor(coverId)
            }),
            media = media,
            ownerDisplayName = row[UsersTable.displayName],
            contributors = contributors
        )
    }

    private fun countByMedia(
        mediaColumn: Column<EntityID<UUID>>,
        countExpr: Count,
        mediaIds: List<UUID>
    ): Map<UUID, Int> {
        if (mediaIds.isEmpty()) return emptyMap()
        return mediaColumn.table
            .select(mediaColumn, countExpr)
            .where { mediaColumn inList mediaIds }
            .groupBy(mediaColumn)
            .associate { it[mediaColumn].value to it[countExpr].toInt() }
    }

    /** Resolves cover URLs for a batch of recap rows, then maps them to DTOs. */
    private fun toRecapDtos(rows: List<ResultRow>): List<RecapDto> {
        val coverIds = rows.mapNotNull { it[RecapsTable.coverMediaId] }.distinct()
        val coverUrls: Map<UUID, String> = if (coverIds.isEmpty()) emptyMap() else {
            MediaTable.selectAll().where { MediaTable.id inList coverIds }
                .associate { it[MediaTable.id].value to publicUrl(it) }
        }
        return rows.map { row -> toRecapDto(row, coverUrl = row[RecapsTable.coverMediaId]?.let { coverUrls[it] }) }
    }

    private fun toRecapDto(row: ResultRow, coverUrl: String?): RecapDto = RecapDto(
        id = row[RecapsTable.id].value.toString(),
        albumId = row[RecapsTable.albumId].value.toString(),
        albumTitle = row[AlbumsTable.title],
        title = row[RecapsTable.title],
        coverMediaId = row[RecapsTable.coverMediaId]?.toString(),
        coverUrl = coverUrl,
        mediaIds = json.decodeFromString(row[RecapsTable.mediaIds]),
        periodStart = row[RecapsTable.periodStart],
        periodEnd = row[RecapsTable.periodEnd],
        status = RecapStatus.valueOf(row[RecapsTable.status]),
        shareToken = row[RecapsTable.shareToken],
        createdAt = row[RecapsTable.createdAt]
    )

    private fun coverUrlFor(mediaId: UUID): String? =
        MediaTable.selectAll().where { MediaTable.id eq mediaId }
            .singleOrNull()?.let { publicUrl(it) }

    private fun publicUrl(mediaRow: ResultRow): String =
        storageService.getPublicUrl(mediaRow[MediaTable.thumbnailKey] ?: mediaRow[MediaTable.storageKey])

    companion object {
        const val MIN_MEDIA = 12
        const val MAX_MEDIA = 24
        private val json = Json { ignoreUnknownKeys = true }
    }
}
