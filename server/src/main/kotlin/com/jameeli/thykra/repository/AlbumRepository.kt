package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.MemberRole
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AlbumRepository {

    suspend fun create(ownerId: UUID, title: String, description: String?): AlbumDto =
        newSuspendedTransaction {
            val now = Clock.System.now()
            val albumId = UUID.randomUUID()
            AlbumsTable.insert {
                it[AlbumsTable.id] = albumId
                it[AlbumsTable.ownerId] = ownerId
                it[AlbumsTable.title] = title
                it[AlbumsTable.description] = description
                it[AlbumsTable.createdAt] = now
                it[AlbumsTable.updatedAt] = now
            }
            AlbumMembersTable.insert {
                it[AlbumMembersTable.id] = UUID.randomUUID()
                it[AlbumMembersTable.albumId] = albumId
                it[AlbumMembersTable.userId] = ownerId
                it[AlbumMembersTable.role] = MemberRole.OWNER.name
                it[AlbumMembersTable.joinedAt] = now
            }
            AlbumDto(
                id = albumId.toString(),
                ownerId = ownerId.toString(),
                title = title,
                description = description,
                memberCount = 1,
                createdAt = now
            )
        }

    suspend fun findById(id: UUID): AlbumDto? =
        newSuspendedTransaction {
            AlbumsTable.selectAll().where { AlbumsTable.id eq id }
                .singleOrNull()?.toAlbumDto()
        }

    suspend fun findAllForUser(userId: UUID): List<AlbumDto> =
        newSuspendedTransaction {
            (AlbumsTable innerJoin AlbumMembersTable)
                .selectAll().where { AlbumMembersTable.userId eq userId }
                .map { it.toAlbumDto() }
        }

    suspend fun update(id: UUID, title: String?, description: String?, coverUrl: String?): AlbumDto? =
        newSuspendedTransaction {
            val now = Clock.System.now()
            AlbumsTable.update({ AlbumsTable.id eq id }) {
                if (title != null) it[AlbumsTable.title] = title
                if (description != null) it[AlbumsTable.description] = description
                if (coverUrl != null) it[AlbumsTable.coverUrl] = coverUrl
                it[AlbumsTable.updatedAt] = now
            }
            AlbumsTable.selectAll().where { AlbumsTable.id eq id }
                .singleOrNull()?.toAlbumDto()
        }

    suspend fun delete(id: UUID) {
        newSuspendedTransaction {
            AlbumsTable.deleteWhere { AlbumsTable.id eq id }
        }
    }

    private fun ResultRow.toAlbumDto(): AlbumDto {
        val albumId = this[AlbumsTable.id].value
        val memberCount = AlbumMembersTable.selectAll()
            .where { AlbumMembersTable.albumId eq albumId }
            .count().toInt()
        return AlbumDto(
            id = albumId.toString(),
            ownerId = this[AlbumsTable.ownerId].value.toString(),
            title = this[AlbumsTable.title],
            description = this[AlbumsTable.description],
            coverUrl = this[AlbumsTable.coverUrl],
            memberCount = memberCount,
            createdAt = this[AlbumsTable.createdAt]
        )
    }
}
