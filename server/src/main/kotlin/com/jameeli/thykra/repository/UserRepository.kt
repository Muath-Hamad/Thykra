package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.UsersTable
import com.jameeli.thykra.model.UserDto
import kotlin.time.Clock

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

import java.util.UUID

class UserRepository {

    suspend fun findByOAuthSubject(provider: String, subject: String): UserDto? =
        newSuspendedTransaction {
            UsersTable.selectAll().where {
                (UsersTable.oauthProvider eq provider) and (UsersTable.oauthSubject eq subject)
            }.singleOrNull()?.toUserDto()
        }

    suspend fun findById(id: UUID): UserDto? =
        newSuspendedTransaction {
            UsersTable.selectAll().where { UsersTable.id eq id }
                .singleOrNull()?.toUserDto()
        }

    suspend fun createUser(
        email: String,
        displayName: String,
        avatarUrl: String?,
        provider: String,
        subject: String
    ): UserDto = newSuspendedTransaction {
        val now = Clock.System.now()
        val id = UUID.randomUUID()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.displayName] = displayName
            it[UsersTable.avatarUrl] = avatarUrl
            it[UsersTable.oauthProvider] = provider
            it[UsersTable.oauthSubject] = subject
            it[UsersTable.createdAt] = now
            it[UsersTable.updatedAt] = now
        }
        UserDto(id.toString(), email, displayName, avatarUrl, now)
    }

    suspend fun updateProfile(id: UUID, displayName: String?, avatarUrl: String?): UserDto? =
        newSuspendedTransaction {
            val now = Clock.System.now()
            UsersTable.update({ UsersTable.id eq id }) {
                if (displayName != null) it[UsersTable.displayName] = displayName
                if (avatarUrl != null) it[UsersTable.avatarUrl] = avatarUrl
                it[UsersTable.updatedAt] = now
            }
            UsersTable.selectAll().where { UsersTable.id eq id }
                .singleOrNull()?.toUserDto()
        }

    private fun ResultRow.toUserDto() = UserDto(
        id = this[UsersTable.id].value.toString(),
        email = this[UsersTable.email],
        displayName = this[UsersTable.displayName],
        avatarUrl = this[UsersTable.avatarUrl],
        createdAt = this[UsersTable.createdAt]
    )
}
