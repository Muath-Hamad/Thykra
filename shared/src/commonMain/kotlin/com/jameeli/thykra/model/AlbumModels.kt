package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MemberRole { OWNER, CONTRIBUTOR, VIEWER }

@Serializable
data class AlbumMemberSummary(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class AlbumDto(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val memberCount: Int,
    val previewMembers: List<AlbumMemberSummary> = emptyList(),
    val createdAt: Instant
)

@Serializable
data class CreateAlbumRequest(
    val title: String,
    val description: String? = null
)

@Serializable
data class UpdateAlbumRequest(
    val title: String? = null,
    val description: String? = null,
    val coverUrl: String? = null
)

@Serializable
data class AlbumMemberDto(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: MemberRole,
    val joinedAt: Instant
)

@Serializable
data class InviteLinkDto(
    val albumId: String,
    val token: String,
    val expiresAt: Instant
)

@Serializable
data class AddMemberRequest(
    val userId: String,
    val role: MemberRole
)
