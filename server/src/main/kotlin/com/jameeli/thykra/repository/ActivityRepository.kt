package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.ActivityItemDto
import com.jameeli.thykra.model.ActivityType
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.storage.StorageService
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ActivityRepository(private val storageService: StorageService) {

    /**
     * Returns the most recent reactions + comments across all albums the user is a member of.
     *
     * Excludes activity by the current user, and excludes activity on media that is not ACTIVE.
     * Each table is queried with the same [limit], merged in memory, then re-sorted/sliced.
     */
    suspend fun recentForUser(userId: UUID, limit: Int): List<ActivityItemDto> = newSuspendedTransaction {
        val safeLimit = limit.coerceIn(1, 50)

        // Reactions: ReactionsTable -> MediaTable -> AlbumsTable -> AlbumMembersTable (filter by user)
        // and ReactionsTable.userId -> UsersTable.id (the actor).
        val reactionItems = ReactionsTable
            .innerJoin(MediaTable)
            .innerJoin(AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = ReactionsTable.userId,
                otherColumn = UsersTable.id
            )
            .join(
                otherTable = AlbumMembersTable,
                joinType = JoinType.INNER,
                onColumn = AlbumsTable.id,
                otherColumn = AlbumMembersTable.albumId
            )
            .selectAll()
            .where {
                (AlbumMembersTable.userId eq userId) and
                    (MediaTable.status eq MediaStatus.ACTIVE.name) and
                    (ReactionsTable.userId neq userId)
            }
            .orderBy(ReactionsTable.createdAt, SortOrder.DESC)
            .limit(safeLimit)
            .map { row ->
                val thumbKey = row[MediaTable.thumbnailKey] ?: row[MediaTable.storageKey]
                ActivityItemDto(
                    type = ActivityType.REACTION,
                    createdAt = row[ReactionsTable.createdAt],
                    albumId = row[AlbumsTable.id].value.toString(),
                    albumTitle = row[AlbumsTable.title],
                    mediaId = row[MediaTable.id].value.toString(),
                    mediaThumbnailUrl = storageService.getPublicUrl(thumbKey),
                    actorId = row[UsersTable.id].value.toString(),
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    reactionType = ReactionType.valueOf(row[ReactionsTable.type]),
                    commentBody = null
                )
            }

        // Comments: CommentsTable -> MediaTable -> AlbumsTable -> AlbumMembersTable
        // and CommentsTable.authorId -> UsersTable.id (the actor).
        val commentItems = CommentsTable
            .innerJoin(MediaTable)
            .innerJoin(AlbumsTable)
            .join(
                otherTable = UsersTable,
                joinType = JoinType.INNER,
                onColumn = CommentsTable.authorId,
                otherColumn = UsersTable.id
            )
            .join(
                otherTable = AlbumMembersTable,
                joinType = JoinType.INNER,
                onColumn = AlbumsTable.id,
                otherColumn = AlbumMembersTable.albumId
            )
            .selectAll()
            .where {
                (AlbumMembersTable.userId eq userId) and
                    (MediaTable.status eq MediaStatus.ACTIVE.name) and
                    (CommentsTable.authorId neq userId)
            }
            .orderBy(CommentsTable.createdAt, SortOrder.DESC)
            .limit(safeLimit)
            .map { row ->
                val thumbKey = row[MediaTable.thumbnailKey] ?: row[MediaTable.storageKey]
                ActivityItemDto(
                    type = ActivityType.COMMENT,
                    createdAt = row[CommentsTable.createdAt],
                    albumId = row[AlbumsTable.id].value.toString(),
                    albumTitle = row[AlbumsTable.title],
                    mediaId = row[MediaTable.id].value.toString(),
                    mediaThumbnailUrl = storageService.getPublicUrl(thumbKey),
                    actorId = row[UsersTable.id].value.toString(),
                    actorDisplayName = row[UsersTable.displayName],
                    actorAvatarUrl = row[UsersTable.avatarUrl],
                    reactionType = null,
                    commentBody = row[CommentsTable.body].take(200)
                )
            }

        (reactionItems + commentItems)
            .sortedByDescending { it.createdAt }
            .take(safeLimit)
    }
}
