package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.ActivitySeenTable
import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.ActivityActorDto
import com.jameeli.thykra.model.ActivityAlbumDto
import com.jameeli.thykra.model.ActivityEventDto
import com.jameeli.thykra.model.ActivityEventType
import com.jameeli.thykra.model.ActivityFeedDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.storage.StorageService
import kotlin.time.Instant

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Pre-aggregated activity feed for the redesigned /activity surfaces.
 *
 * Aggregation approach (documented per the design spec):
 * - Raw events (media uploads, member joins, comments, reactions) are fetched per source
 *   with a "before cursor" filter and a fudged per-source limit (limit * RAW_FUDGE), merged
 *   and sorted newest-first.
 * - Sweeping newest -> oldest, a raw event joins the open window for its
 *   (type, album, actor) key while it is within [WINDOW] of the window's newest event;
 *   otherwise the window is emitted as one aggregate ("Sara added 12 photos") and a new
 *   window opens. An aggregate's createdAt is the newest raw timestamp in its window.
 * - The aggregates are sliced to the requested limit; nextCursor is the createdAt of the
 *   last emitted aggregate. Because aggregation happens on the queried slice, a window
 *   that straddles a page boundary may be split into two aggregates — accepted trade-off
 *   for stable, stateless paging.
 */
class ActivityFeedRepository(private val storageService: StorageService) {

    private data class RawEvent(
        val type: ActivityEventType,
        val albumId: UUID,
        val albumTitle: String,
        val actorId: UUID,
        val actorDisplayName: String,
        val actorAvatarUrl: String?,
        val createdAt: Instant,
        val mediaId: UUID? = null,
        val thumbnailKey: String? = null,
        val commentBody: String? = null,
        val reactionType: ReactionType? = null
    )

    /**
     * Feed across all albums the user is a member of. Excludes the caller's own events,
     * matching the behaviour of the legacy /activity/recent endpoint.
     */
    suspend fun feedForUser(userId: UUID, before: Instant?, limit: Int): ActivityFeedDto =
        newSuspendedTransaction {
            val albumIds = AlbumMembersTable
                .selectAll().where { AlbumMembersTable.userId eq userId }
                .map { it[AlbumMembersTable.albumId].value }
            if (albumIds.isEmpty()) {
                return@newSuspendedTransaction ActivityFeedDto(
                    items = emptyList(),
                    nextCursor = null,
                    lastSeenAt = seenAt(userId)
                )
            }
            buildFeed(
                raw = fetchRaw(albumIds, before, limit, excludeActor = userId),
                limit = limit,
                lastSeenAt = seenAt(userId)
            )
        }

    /**
     * Feed for one album — the trip log. Includes every actor, the caller too:
     * the per-trip strip shows "who has been here lately", self included.
     */
    suspend fun feedForAlbum(albumId: UUID, viewerId: UUID, before: Instant?, limit: Int): ActivityFeedDto =
        newSuspendedTransaction {
            buildFeed(
                raw = fetchRaw(listOf(albumId), before, limit, excludeActor = null),
                limit = limit,
                lastSeenAt = seenAt(viewerId)
            )
        }

    /** Upserts the caller's read marker. */
    suspend fun markSeen(userId: UUID, seenAt: Instant) {
        newSuspendedTransaction {
            val updated = ActivitySeenTable.update({ ActivitySeenTable.userId eq userId }) {
                it[ActivitySeenTable.seenAt] = seenAt
            }
            if (updated == 0) {
                ActivitySeenTable.insert {
                    it[ActivitySeenTable.id] = UUID.randomUUID()
                    it[ActivitySeenTable.userId] = userId
                    it[ActivitySeenTable.seenAt] = seenAt
                }
            }
        }
    }

    suspend fun getSeenAt(userId: UUID): Instant? = newSuspendedTransaction { seenAt(userId) }

    private fun seenAt(userId: UUID): Instant? =
        ActivitySeenTable.selectAll().where { ActivitySeenTable.userId eq userId }
            .singleOrNull()?.get(ActivitySeenTable.seenAt)

    private fun rawLimit(limit: Int): Int = (limit * RAW_FUDGE).coerceAtMost(MAX_RAW_ROWS)

    /** Raw events plus whether any single source hit its fetch cap (i.e. may be truncated). */
    private data class RawFetch(val events: List<RawEvent>, val truncated: Boolean)

    private fun fetchRaw(albumIds: List<UUID>, before: Instant?, limit: Int, excludeActor: UUID?): RawFetch {
        val rawLimit = rawLimit(limit)

        // MEDIA_ADDED — ACTIVE media, actor = uploader, timestamp = uploadedAt.
        val mediaEvents = MediaTable
            .innerJoin(AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = MediaTable.uploaderId,
                otherColumn = UsersTable.id
            )
            .selectAll()
            .where {
                var op: Op<Boolean> = (MediaTable.albumId inList albumIds) and
                    (MediaTable.status eq MediaStatus.ACTIVE.name)
                if (before != null) op = op and (MediaTable.uploadedAt less before)
                if (excludeActor != null) op = op and (MediaTable.uploaderId neq excludeActor)
                op
            }
            .orderBy(MediaTable.uploadedAt, SortOrder.DESC)
            .limit(rawLimit)
            .map { row ->
                RawEvent(
                    type = ActivityEventType.MEDIA_ADDED,
                    albumId = row[AlbumsTable.id].value,
                    albumTitle = row[AlbumsTable.title],
                    actorId = row[UsersTable.id].value,
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    createdAt = row[MediaTable.uploadedAt],
                    mediaId = row[MediaTable.id].value,
                    thumbnailKey = row[MediaTable.thumbnailKey] ?: row[MediaTable.storageKey]
                )
            }

        // MEMBER_JOINED — excludes OWNER rows: the owner's membership row is the album's
        // creation, not a join.
        val joinEvents = AlbumMembersTable
            .join(
                otherTable = AlbumsTable,
                joinType = JoinType.INNER,
                onColumn = AlbumMembersTable.albumId,
                otherColumn = AlbumsTable.id
            )
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = AlbumMembersTable.userId,
                otherColumn = UsersTable.id
            )
            .selectAll()
            .where {
                var op: Op<Boolean> = (AlbumMembersTable.albumId inList albumIds) and
                    (AlbumMembersTable.role neq MemberRole.OWNER.name)
                if (before != null) op = op and (AlbumMembersTable.joinedAt less before)
                if (excludeActor != null) op = op and (AlbumMembersTable.userId neq excludeActor)
                op
            }
            .orderBy(AlbumMembersTable.joinedAt, SortOrder.DESC)
            .limit(rawLimit)
            .map { row ->
                RawEvent(
                    type = ActivityEventType.MEMBER_JOINED,
                    albumId = row[AlbumsTable.id].value,
                    albumTitle = row[AlbumsTable.title],
                    actorId = row[UsersTable.id].value,
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    createdAt = row[AlbumMembersTable.joinedAt]
                )
            }

        // COMMENT — on ACTIVE media only.
        val commentEvents = CommentsTable
            .innerJoin(MediaTable)
            .innerJoin(AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = CommentsTable.authorId,
                otherColumn = UsersTable.id
            )
            .selectAll()
            .where {
                var op: Op<Boolean> = (MediaTable.albumId inList albumIds) and
                    (MediaTable.status eq MediaStatus.ACTIVE.name)
                if (before != null) op = op and (CommentsTable.createdAt less before)
                if (excludeActor != null) op = op and (CommentsTable.authorId neq excludeActor)
                op
            }
            .orderBy(CommentsTable.createdAt, SortOrder.DESC)
            .limit(rawLimit)
            .map { row ->
                RawEvent(
                    type = ActivityEventType.COMMENT,
                    albumId = row[AlbumsTable.id].value,
                    albumTitle = row[AlbumsTable.title],
                    actorId = row[UsersTable.id].value,
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    createdAt = row[CommentsTable.createdAt],
                    mediaId = row[MediaTable.id].value,
                    thumbnailKey = row[MediaTable.thumbnailKey] ?: row[MediaTable.storageKey],
                    commentBody = row[CommentsTable.body]
                )
            }

        // REACTION — on ACTIVE media only.
        val reactionEvents = ReactionsTable
            .innerJoin(MediaTable)
            .innerJoin(AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = ReactionsTable.userId,
                otherColumn = UsersTable.id
            )
            .selectAll()
            .where {
                var op: Op<Boolean> = (MediaTable.albumId inList albumIds) and
                    (MediaTable.status eq MediaStatus.ACTIVE.name)
                if (before != null) op = op and (ReactionsTable.createdAt less before)
                if (excludeActor != null) op = op and (ReactionsTable.userId neq excludeActor)
                op
            }
            .orderBy(ReactionsTable.createdAt, SortOrder.DESC)
            .limit(rawLimit)
            .map { row ->
                RawEvent(
                    type = ActivityEventType.REACTION,
                    albumId = row[AlbumsTable.id].value,
                    albumTitle = row[AlbumsTable.title],
                    actorId = row[UsersTable.id].value,
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    createdAt = row[ReactionsTable.createdAt],
                    mediaId = row[MediaTable.id].value,
                    thumbnailKey = row[MediaTable.thumbnailKey] ?: row[MediaTable.storageKey],
                    reactionType = ReactionType.valueOf(row[ReactionsTable.type])
                )
            }

        val truncated = mediaEvents.size >= rawLimit || joinEvents.size >= rawLimit ||
            commentEvents.size >= rawLimit || reactionEvents.size >= rawLimit
        return RawFetch(mediaEvents + joinEvents + commentEvents + reactionEvents, truncated)
    }

    private fun buildFeed(raw: RawFetch, limit: Int, lastSeenAt: Instant?): ActivityFeedDto {
        val sorted = raw.events.sortedByDescending { it.createdAt }

        data class WindowKey(val type: ActivityEventType, val albumId: UUID, val actorId: UUID)

        val open = LinkedHashMap<WindowKey, MutableList<RawEvent>>()
        val aggregates = mutableListOf<ActivityEventDto>()

        fun emit(window: List<RawEvent>) {
            aggregates.add(toEvent(window))
        }

        for (event in sorted) {
            val key = WindowKey(event.type, event.albumId, event.actorId)
            val window = open[key]
            // The window is anchored at its newest event; a row joins while within 30 minutes
            // of that anchor, otherwise the window closes and a new one starts.
            if (window != null && window.first().createdAt - event.createdAt <= WINDOW) {
                window.add(event)
            } else {
                if (window != null) emit(window)
                open[key] = mutableListOf(event)
            }
        }
        open.values.forEach { emit(it) }

        val sortedAggregates = aggregates.sortedByDescending { it.createdAt }
        val page = sortedAggregates.take(limit)
        // More pages exist when we aggregated more than one page's worth, or when any raw
        // source may have been truncated at its fudge limit.
        val hasMore = sortedAggregates.size > limit || raw.truncated
        val nextCursor = if (hasMore) page.lastOrNull()?.createdAt?.toString() else null

        return ActivityFeedDto(items = page, nextCursor = nextCursor, lastSeenAt = lastSeenAt)
    }

    private fun toEvent(window: List<RawEvent>): ActivityEventDto {
        val newest = window.first()
        val mediaIds = window.mapNotNull { it.mediaId }.distinct()
        val thumbnailUrls = window.mapNotNull { it.thumbnailKey }.distinct()
            .take(MAX_THUMBNAILS)
            .map { storageService.getPublicUrl(it) }
        val reactionType = if (newest.type == ActivityEventType.REACTION) {
            window.mapNotNull { it.reactionType }.distinct().singleOrNull()
        } else null
        return ActivityEventDto(
            id = "${newest.type.name}-${newest.albumId}-${newest.actorId}-${newest.createdAt.toEpochMilliseconds()}",
            type = newest.type,
            actor = ActivityActorDto(
                userId = newest.actorId.toString(),
                displayName = newest.actorDisplayName,
                avatarUrl = newest.actorAvatarUrl
            ),
            album = ActivityAlbumDto(id = newest.albumId.toString(), title = newest.albumTitle),
            createdAt = newest.createdAt,
            count = window.size,
            mediaIds = mediaIds.map { it.toString() },
            thumbnailUrls = thumbnailUrls,
            commentBody = newest.commentBody?.take(MAX_COMMENT_LENGTH),
            reactionType = reactionType
        )
    }

    companion object {
        private val WINDOW = 30.minutes
        private const val RAW_FUDGE = 5
        private const val MAX_RAW_ROWS = 500
        private const val MAX_THUMBNAILS = 4
        private const val MAX_COMMENT_LENGTH = 200
    }
}
