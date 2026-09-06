package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 255)
    val avatarUrl = varchar("avatar_url", 1024).nullable()
    val oauthProvider = varchar("oauth_provider", 50)
    val oauthSubject = varchar("oauth_subject", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("uq_provider_subject", oauthProvider, oauthSubject)
    }
}
