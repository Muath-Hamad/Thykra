package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumInvitesTable
import com.jameeli.thykra.model.InviteLinkDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AlbumInviteRepository {

    data class InviteRecord(val albumId: UUID, val token: String, val expiresAt: Instant)

    suspend fun createInvite(albumId: UUID, createdBy: UUID, token: String, expiresAt: Instant): InviteLinkDto =
        newSuspendedTransaction {
            val now = Clock.System.now()
            AlbumInvitesTable.insert {
                it[AlbumInvitesTable.id] = UUID.randomUUID()
                it[AlbumInvitesTable.albumId] = albumId
                it[AlbumInvitesTable.token] = token
                it[AlbumInvitesTable.createdBy] = createdBy
                it[AlbumInvitesTable.expiresAt] = expiresAt
                it[AlbumInvitesTable.createdAt] = now
            }
            InviteLinkDto(
                albumId = albumId.toString(),
                token = token,
                expiresAt = expiresAt
            )
        }

    suspend fun findValidByToken(token: String): InviteRecord? =
        newSuspendedTransaction {
            val now = Clock.System.now()
            AlbumInvitesTable.selectAll().where {
                (AlbumInvitesTable.token eq token) and
                    (AlbumInvitesTable.expiresAt greater now) and
                    (AlbumInvitesTable.usedBy.isNull())
            }.singleOrNull()?.let {
                InviteRecord(
                    albumId = it[AlbumInvitesTable.albumId].value,
                    token = it[AlbumInvitesTable.token],
                    expiresAt = it[AlbumInvitesTable.expiresAt]
                )
            }
        }

    suspend fun markUsed(token: String, userId: UUID) {
        newSuspendedTransaction {
            AlbumInvitesTable.update({ AlbumInvitesTable.token eq token }) {
                it[usedBy] = userId
            }
        }
    }
}
