package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AlbumInvitesTable : UUIDTable("album_invites") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex()
    val createdBy = reference("created_by", UsersTable, onDelete = ReferenceOption.CASCADE)
    val expiresAt = timestamp("expires_at")
    val usedBy = reference("used_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at")
}
