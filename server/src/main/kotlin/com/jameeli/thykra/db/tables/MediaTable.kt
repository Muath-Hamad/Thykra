package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object MediaTable : UUIDTable("media") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val uploaderId = reference("uploader_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 20)
    val status = varchar("status", 20)
    val storageKey = varchar("storage_key", 1024).uniqueIndex()
    val filename = varchar("filename", 512)
    val contentType = varchar("content_type", 128)
    val fileSize = long("file_size")
    val width = integer("width").nullable()
    val height = integer("height").nullable()
    val durationMs = long("duration_ms").nullable()
    val takenAt = timestamp("taken_at").nullable()
    val uploadedAt = timestamp("uploaded_at")
    val updatedAt = timestamp("updated_at")
}
