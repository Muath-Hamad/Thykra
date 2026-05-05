package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object BlockedMembersTable : UUIDTable("blocked_members") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val blockedBy = reference("blocked_by", UsersTable, onDelete = ReferenceOption.CASCADE)
    val blockedAt = timestamp("blocked_at")

    init {
        uniqueIndex("uq_blocked_album_user", albumId, userId)
    }
}
