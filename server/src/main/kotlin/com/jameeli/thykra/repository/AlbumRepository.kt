package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.AlbumsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberSummary
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.storage.StorageService
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AlbumRepository(private val storageService: StorageService) {

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
                previewMembers = emptyList(),
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

        // Latest active media thumbnail (falls back to stored coverUrl if no media)
        val latestCoverUrl = MediaTable
            .selectAll()
            .where { (MediaTable.albumId eq albumId) and (MediaTable.status eq "ACTIVE") }
            .orderBy(MediaTable.uploadedAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { row ->
                val thumbKey = row[MediaTable.thumbnailKey]
                val mainKey = row[MediaTable.storageKey]
                storageService.getPublicUrl(thumbKey ?: mainKey)
            }
            ?: this[AlbumsTable.coverUrl]

        // First 4 members with avatar info
        val previewMembers = (AlbumMembersTable innerJoin UsersTable)
            .selectAll()
            .where { AlbumMembersTable.albumId eq albumId }
            .limit(4)
            .map {
                AlbumMemberSummary(
                    userId = it[AlbumMembersTable.userId].value.toString(),
                    displayName = it[UsersTable.displayName],
                    avatarUrl = it[UsersTable.avatarUrl]
                )
            }

        return AlbumDto(
            id = albumId.toString(),
            ownerId = this[AlbumsTable.ownerId].value.toString(),
            title = this[AlbumsTable.title],
            description = this[AlbumsTable.description],
            coverUrl = latestCoverUrl,
            memberCount = memberCount,
            previewMembers = previewMembers,
            createdAt = this[AlbumsTable.createdAt]
        )
    }
}
