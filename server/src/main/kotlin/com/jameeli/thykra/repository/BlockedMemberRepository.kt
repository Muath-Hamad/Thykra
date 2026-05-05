package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.BlockedMembersTable
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.BlockedMemberDto
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class BlockedMemberRepository {

    suspend fun block(albumId: UUID, userId: UUID, blockedBy: UUID) {
        newSuspendedTransaction {
            val existing = BlockedMembersTable.selectAll().where {
                (BlockedMembersTable.albumId eq albumId) and (BlockedMembersTable.userId eq userId)
            }.singleOrNull()
            if (existing != null) return@newSuspendedTransaction
            BlockedMembersTable.insert {
                it[BlockedMembersTable.id] = UUID.randomUUID()
                it[BlockedMembersTable.albumId] = albumId
                it[BlockedMembersTable.userId] = userId
                it[BlockedMembersTable.blockedBy] = blockedBy
                it[BlockedMembersTable.blockedAt] = Clock.System.now()
            }
        }
    }

    suspend fun unblock(albumId: UUID, userId: UUID) {
        newSuspendedTransaction {
            BlockedMembersTable.deleteWhere {
                (BlockedMembersTable.albumId eq albumId) and (BlockedMembersTable.userId eq userId)
            }
        }
    }

    suspend fun isBlocked(albumId: UUID, userId: UUID): Boolean = newSuspendedTransaction {
        BlockedMembersTable.selectAll().where {
            (BlockedMembersTable.albumId eq albumId) and (BlockedMembersTable.userId eq userId)
        }.empty().not()
    }

    suspend fun list(albumId: UUID): List<BlockedMemberDto> = newSuspendedTransaction {
        (BlockedMembersTable innerJoin UsersTable)
            .selectAll()
            .where { BlockedMembersTable.albumId eq albumId }
            .orderBy(BlockedMembersTable.blockedAt, SortOrder.DESC)
            .map {
                BlockedMemberDto(
                    userId = it[BlockedMembersTable.userId].value.toString(),
                    displayName = it[UsersTable.displayName],
                    avatarUrl = it[UsersTable.avatarUrl],
                    blockedAt = it[BlockedMembersTable.blockedAt]
                )
            }
    }
}
