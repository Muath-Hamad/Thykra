package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.MemberRole
import kotlin.time.Clock
import kotlin.time.Instant

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class AlbumMemberRepository {

    suspend fun findMembers(albumId: UUID): List<AlbumMemberDto> =
        newSuspendedTransaction {
            (AlbumMembersTable innerJoin UsersTable)
                .selectAll().where { AlbumMembersTable.albumId eq albumId }
                .map {
                    AlbumMemberDto(
                        userId = it[AlbumMembersTable.userId].value.toString(),
                        displayName = it[UsersTable.displayName],
                        avatarUrl = it[UsersTable.avatarUrl],
                        role = MemberRole.valueOf(it[AlbumMembersTable.role]),
                        joinedAt = it[AlbumMembersTable.joinedAt]
                    )
                }
        }

    suspend fun addMember(albumId: UUID, userId: UUID, role: MemberRole): AlbumMemberDto =
        newSuspendedTransaction {
            val now = Clock.System.now()
            AlbumMembersTable.insert {
                it[AlbumMembersTable.id] = UUID.randomUUID()
                it[AlbumMembersTable.albumId] = albumId
                it[AlbumMembersTable.userId] = userId
                it[AlbumMembersTable.role] = role.name
                it[AlbumMembersTable.joinedAt] = now
            }
            val user = UsersTable.selectAll().where { UsersTable.id eq userId }.single()
            AlbumMemberDto(
                userId = userId.toString(),
                displayName = user[UsersTable.displayName],
                avatarUrl = user[UsersTable.avatarUrl],
                role = role,
                joinedAt = now
            )
        }

    suspend fun removeMember(albumId: UUID, userId: UUID) {
        newSuspendedTransaction {
            AlbumMembersTable.deleteWhere {
                (AlbumMembersTable.albumId eq albumId) and (AlbumMembersTable.userId eq userId)
            }
        }
    }

    suspend fun getMemberRole(albumId: UUID, userId: UUID): MemberRole? =
        newSuspendedTransaction {
            AlbumMembersTable.selectAll().where {
                (AlbumMembersTable.albumId eq albumId) and (AlbumMembersTable.userId eq userId)
            }.singleOrNull()?.let { MemberRole.valueOf(it[AlbumMembersTable.role]) }
        }

    suspend fun isMember(albumId: UUID, userId: UUID): Boolean =
        getMemberRole(albumId, userId) != null

    suspend fun getJoinedAt(albumId: UUID, userId: UUID): Instant? =
        newSuspendedTransaction {
            AlbumMembersTable.selectAll().where {
                (AlbumMembersTable.albumId eq albumId) and (AlbumMembersTable.userId eq userId)
            }.singleOrNull()?.get(AlbumMembersTable.joinedAt)
        }
}
