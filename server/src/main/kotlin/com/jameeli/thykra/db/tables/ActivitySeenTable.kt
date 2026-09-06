package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object ActivitySeenTable : UUIDTable("activity_seen") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val seenAt = timestamp("seen_at")
}
