package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.AlbumInvitesTable
import com.jameeli.thykra.db.tables.InviteJoinsTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.InviteLinkDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class AlbumInviteRepository {

    data class InviteRecord(val id: UUID, val albumId: UUID, val token: String, val expiresAt: Instant)

    /** Full invite row plus inviter identity, used by the invite preview endpoint. */
    data class InviteDetails(
        val id: UUID,
        val albumId: UUID,
        val expiresAt: Instant,
        val revokedAt: Instant?,
        val inviterDisplayName: String,
        val inviterAvatarUrl: String?
    )

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

    /**
     * A link is valid until it expires or is revoked — it is multi-use, so a token pasted
     * in a group chat keeps working after the first friend joins. The legacy usedBy column
     * is deliberately NOT part of this check anymore; joins are tracked in InviteJoinsTable.
     */
    suspend fun findValidByToken(token: String): InviteRecord? =
        newSuspendedTransaction {
            val now = Clock.System.now()
            AlbumInvitesTable.selectAll().where {
                (AlbumInvitesTable.token eq token) and
                    (AlbumInvitesTable.expiresAt greater now) and
                    (AlbumInvitesTable.revokedAt.isNull())
            }.singleOrNull()?.let {
                InviteRecord(
                    id = it[AlbumInvitesTable.id].value,
                    albumId = it[AlbumInvitesTable.albumId].value,
                    token = it[AlbumInvitesTable.token],
                    expiresAt = it[AlbumInvitesTable.expiresAt]
                )
            }
        }

    suspend fun findDetailsByToken(token: String): InviteDetails? =
        newSuspendedTransaction {
            AlbumInvitesTable
                .join(
                    otherTable = UsersTable,
                    joinType = JoinType.INNER,
                    onColumn = AlbumInvitesTable.createdBy,
                    otherColumn = UsersTable.id
                )
                .selectAll()
                .where { AlbumInvitesTable.token eq token }
                .singleOrNull()?.let {
                    InviteDetails(
                        id = it[AlbumInvitesTable.id].value,
                        albumId = it[AlbumInvitesTable.albumId].value,
                        expiresAt = it[AlbumInvitesTable.expiresAt],
                        revokedAt = it[AlbumInvitesTable.revokedAt],
                        inviterDisplayName = it[UsersTable.displayName],
                        inviterAvatarUrl = it[UsersTable.avatarUrl]
                    )
                }
        }

    /** Records that a user joined through an invite. Idempotent per (invite, user). */
    suspend fun recordJoin(inviteId: UUID, userId: UUID) {
        newSuspendedTransaction {
            val exists = InviteJoinsTable.selectAll().where {
                (InviteJoinsTable.inviteId eq inviteId) and (InviteJoinsTable.userId eq userId)
            }.empty().not()
            if (!exists) {
                InviteJoinsTable.insert {
                    it[InviteJoinsTable.id] = UUID.randomUUID()
                    it[InviteJoinsTable.inviteId] = inviteId
                    it[InviteJoinsTable.userId] = userId
                    it[InviteJoinsTable.joinedAt] = Clock.System.now()
                }
            }
        }
    }

    /** Active (not expired, not revoked) invites for an album, newest first, with join counts. */
    suspend fun listActiveForAlbum(albumId: UUID): List<InviteLinkDto> =
        newSuspendedTransaction {
            val now = Clock.System.now()
            val rows = AlbumInvitesTable.selectAll().where {
                (AlbumInvitesTable.albumId eq albumId) and
                    (AlbumInvitesTable.expiresAt greater now) and
                    (AlbumInvitesTable.revokedAt.isNull())
            }.orderBy(AlbumInvitesTable.createdAt, SortOrder.DESC).toList()

            val inviteIds = rows.map { it[AlbumInvitesTable.id].value }
            val joinCounts: Map<UUID, Int> = if (inviteIds.isEmpty()) emptyMap() else {
                val countExpr = InviteJoinsTable.id.count()
                InviteJoinsTable
                    .select(InviteJoinsTable.inviteId, countExpr)
                    .where { InviteJoinsTable.inviteId inList inviteIds }
                    .groupBy(InviteJoinsTable.inviteId)
                    .associate { it[InviteJoinsTable.inviteId].value to it[countExpr].toInt() }
            }

            rows.map {
                val id = it[AlbumInvitesTable.id].value
                InviteLinkDto(
                    albumId = albumId.toString(),
                    token = it[AlbumInvitesTable.token],
                    expiresAt = it[AlbumInvitesTable.expiresAt],
                    joinCount = joinCounts[id] ?: 0
                )
            }
        }

    /**
     * Revokes an invite by token, scoped to an album. Returns false when no such invite
     * exists for that album; revoking an already-revoked invite is a no-op success.
     */
    suspend fun revoke(albumId: UUID, token: String): Boolean =
        newSuspendedTransaction {
            val exists = AlbumInvitesTable.selectAll().where {
                (AlbumInvitesTable.albumId eq albumId) and (AlbumInvitesTable.token eq token)
            }.empty().not()
            if (!exists) return@newSuspendedTransaction false
            AlbumInvitesTable.update({
                (AlbumInvitesTable.albumId eq albumId) and
                    (AlbumInvitesTable.token eq token) and
                    (AlbumInvitesTable.revokedAt.isNull())
            }) {
                it[revokedAt] = Clock.System.now()
            }
            true
        }

    /** Legacy: records the first user who used a link. No longer consumes the link. */
    suspend fun markUsed(token: String, userId: UUID) {
        newSuspendedTransaction {
            AlbumInvitesTable.update({
                (AlbumInvitesTable.token eq token) and (AlbumInvitesTable.usedBy.isNull())
            }) {
                it[usedBy] = userId
            }
        }
    }
}
