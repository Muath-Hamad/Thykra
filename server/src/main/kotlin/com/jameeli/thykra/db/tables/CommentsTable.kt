package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object CommentsTable : UUIDTable("comments") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val authorId = reference("author_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val body = varchar("body", 2000)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
