package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object ReactionsTable : UUIDTable("reactions") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 32)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex("uq_reaction_media_user_type", mediaId, userId, type)
    }
}
