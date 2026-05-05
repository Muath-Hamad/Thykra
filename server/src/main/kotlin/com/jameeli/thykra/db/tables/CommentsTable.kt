package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CommentsTable : UUIDTable("comments") {
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val authorId = reference("author_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val body = varchar("body", 2000)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
