package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object AlbumMembersTable : UUIDTable("album_members") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 50)
    val joinedAt = timestamp("joined_at")

    init {
        uniqueIndex("uq_album_user", albumId, userId)
    }
}
