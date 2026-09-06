package com.jameeli.thykra.db.tables

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

import org.jetbrains.exposed.v1.datetime.timestamp

object AlbumInvitesTable : UUIDTable("album_invites") {
    val albumId = reference("album_id", AlbumsTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex()
    val createdBy = reference("created_by", UsersTable, onDelete = ReferenceOption.CASCADE)
    val expiresAt = timestamp("expires_at")

    // Legacy column from the single-use era. Links are multi-use now; usedBy is no longer
    // treated as consumption (joins are tracked in InviteJoinsTable) but stays for compat.
    val usedBy = reference("used_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()

    // A revoked link is invalid regardless of expiry.
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at")
}
