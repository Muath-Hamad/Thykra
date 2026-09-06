package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object AlbumsTable : UUIDTable("albums") {
    val ownerId = reference("owner_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val description = varchar("description", 1024).nullable()
    val coverUrl = varchar("cover_url", 1024).nullable()
    val visibility = varchar("visibility", 32).default("PRIVATE")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
