package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.MediaReactionsDto
import com.jameeli.thykra.model.ReactionDto
import com.jameeli.thykra.model.ReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import kotlin.time.Clock

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ReactionRepository {

    /**
     * Toggle a reaction: if the user already has this reaction type on the media, remove it; else add it.
     * Returns true if the reaction was added, false if it was removed.
     */
    suspend fun toggle(mediaId: UUID, userId: UUID, type: ReactionType): Boolean = newSuspendedTransaction {
        val existing = ReactionsTable.selectAll().where {
            (ReactionsTable.mediaId eq mediaId) and
                (ReactionsTable.userId eq userId) and
                (ReactionsTable.type eq type.name)
        }.singleOrNull()

        if (existing != null) {
            ReactionsTable.deleteWhere {
                (ReactionsTable.mediaId eq mediaId) and
                    (ReactionsTable.userId eq userId) and
                    (ReactionsTable.type eq type.name)
            }
            false
        } else {
            ReactionsTable.insert {
                it[ReactionsTable.id] = UUID.randomUUID()
                it[ReactionsTable.mediaId] = mediaId
                it[ReactionsTable.userId] = userId
                it[ReactionsTable.type] = type.name
                it[ReactionsTable.createdAt] = Clock.System.now()
            }
            true
        }
    }

    suspend fun listForMedia(mediaId: UUID, viewerId: UUID): MediaReactionsDto = newSuspendedTransaction {
        val rows = (ReactionsTable innerJoin UsersTable)
            .selectAll()
            .where { ReactionsTable.mediaId eq mediaId }
            .orderBy(ReactionsTable.createdAt, SortOrder.DESC)
            .toList()

        val reactions = rows.map { row ->
            ReactionDto(
                id = row[ReactionsTable.id].value.toString(),
                mediaId = row[ReactionsTable.mediaId].value.toString(),
                userId = row[ReactionsTable.userId].value.toString(),
                userDisplayName = row[UsersTable.displayName],
                userAvatarUrl = row[UsersTable.avatarUrl],
                type = ReactionType.valueOf(row[ReactionsTable.type]),
                createdAt = row[ReactionsTable.createdAt]
            )
        }

        val summary = reactions
            .groupBy { it.type }
            .map { (type, list) ->
                ReactionSummaryDto(
                    type = type,
                    count = list.size,
                    reactedByMe = list.any { it.userId == viewerId.toString() }
                )
            }
            .sortedByDescending { it.count }

        MediaReactionsDto(summary = summary, reactions = reactions)
    }
}
