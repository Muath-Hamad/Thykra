package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revoked = bool("revoked").default(false)
}
