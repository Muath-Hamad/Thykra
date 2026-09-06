package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object InviteJoinsTable : UUIDTable("invite_joins") {
    val inviteId = reference("invite_id", AlbumInvitesTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val joinedAt = timestamp("joined_at")

    init {
        uniqueIndex("uq_invite_user", inviteId, userId)
    }
}
