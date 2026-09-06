package com.jameeli.thykra.repository

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import com.jameeli.thykra.db.tables.RefreshTokensTable
import kotlin.time.Instant
import kotlin.time.Clock

import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

import java.security.MessageDigest
import java.util.UUID

class RefreshTokenRepository {

    suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant) {
        newSuspendedTransaction {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.id] = UUID.randomUUID()
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[RefreshTokensTable.createdAt] = Clock.System.now()
            }
        }
    }

    suspend fun findValidByTokenHash(tokenHash: String): TokenRecord? =
        newSuspendedTransaction {
            RefreshTokensTable.selectAll().where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    (RefreshTokensTable.revoked eq false)
            }.singleOrNull()?.let {
                TokenRecord(
                    userId = it[RefreshTokensTable.userId].value,
                    expiresAt = it[RefreshTokensTable.expiresAt]
                )
            }
        }

    suspend fun revokeByTokenHash(tokenHash: String) {
        newSuspendedTransaction {
            RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
                it[revoked] = true
            }
        }
    }

    suspend fun revokeAllByUserId(userId: UUID) {
        newSuspendedTransaction {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and (RefreshTokensTable.revoked eq false)
            }) {
                it[revoked] = true
            }
        }
    }

    data class TokenRecord(val userId: UUID, val expiresAt: Instant)

    companion object {
        fun hashToken(token: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
