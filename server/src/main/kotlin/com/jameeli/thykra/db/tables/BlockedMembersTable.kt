package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object BlockedMembersTable : UUIDTable("blocked_members") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val blockedBy = reference("blocked_by", UsersTable, onDelete = ReferenceOption.CASCADE)
    val blockedAt = timestamp("blocked_at")

    init {
        uniqueIndex("uq_blocked_album_user", albumId, userId)
    }
}
