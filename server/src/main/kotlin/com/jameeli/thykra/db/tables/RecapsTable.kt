package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID

import org.jetbrains.exposed.v1.datetime.timestamp

object RecapsTable : UUIDTable("recaps") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val requestedBy = reference("requested_by", UsersTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val coverMediaId = javaUUID("cover_media_id").nullable()

    // JSON array of media UUID strings, in edition (chronological) order.
    val mediaIds = text("media_ids")
    val periodStart = timestamp("period_start").nullable()
    val periodEnd = timestamp("period_end").nullable()

    // BUILDING | READY | FAILED — builds are synchronous today, so rows are written READY;
    // BUILDING/FAILED exist for future async builders.
    val status = varchar("status", 32)
    val shareToken = varchar("share_token", 255).uniqueIndex().nullable()
    val createdAt = timestamp("created_at")
}
