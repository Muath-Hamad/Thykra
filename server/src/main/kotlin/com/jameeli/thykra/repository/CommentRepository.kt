package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.CommentDto
import kotlin.time.Clock

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

import java.util.UUID

class CommentRepository {

    suspend fun create(mediaId: UUID, authorId: UUID, body: String): CommentDto = newSuspendedTransaction {
        val now = Clock.System.now()
        val commentId = UUID.randomUUID()
        CommentsTable.insert {
            it[CommentsTable.id] = commentId
            it[CommentsTable.mediaId] = mediaId
            it[CommentsTable.authorId] = authorId
            it[CommentsTable.body] = body
            it[CommentsTable.createdAt] = now
            it[CommentsTable.updatedAt] = now
        }
        loadById(commentId) ?: error("Insert succeeded but row missing")
    }

    suspend fun listForMedia(mediaId: UUID): List<CommentDto> = newSuspendedTransaction {
        (CommentsTable innerJoin UsersTable)
            .selectAll()
            .where { CommentsTable.mediaId eq mediaId }
            .orderBy(CommentsTable.createdAt, SortOrder.ASC)
            .map { it.toCommentDto() }
    }

    suspend fun findById(id: UUID): CommentDto? = newSuspendedTransaction {
        loadById(id)
    }

    suspend fun update(id: UUID, body: String): CommentDto? = newSuspendedTransaction {
        val now = Clock.System.now()
        CommentsTable.update({ CommentsTable.id eq id }) {
            it[CommentsTable.body] = body
            it[CommentsTable.updatedAt] = now
        }
        loadById(id)
    }

    suspend fun delete(id: UUID) {
        newSuspendedTransaction {
            CommentsTable.deleteWhere { CommentsTable.id eq id }
        }
    }

    private fun loadById(id: UUID): CommentDto? =
        (CommentsTable innerJoin UsersTable)
            .selectAll()
            .where { CommentsTable.id eq id }
            .singleOrNull()
            ?.toCommentDto()

    private fun ResultRow.toCommentDto(): CommentDto = CommentDto(
        id = this[CommentsTable.id].value.toString(),
        mediaId = this[CommentsTable.mediaId].value.toString(),
        authorId = this[CommentsTable.authorId].value.toString(),
        authorDisplayName = this[UsersTable.displayName],
        authorAvatarUrl = this[UsersTable.avatarUrl],
        body = this[CommentsTable.body],
        createdAt = this[CommentsTable.createdAt],
        updatedAt = this[CommentsTable.updatedAt]
    )
}
