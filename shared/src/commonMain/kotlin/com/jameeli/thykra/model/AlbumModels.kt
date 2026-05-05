package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MemberRole { OWNER, CONTRIBUTOR, VIEWER }

@Serializable
enum class AlbumVisibility { PRIVATE, LINK_SHARED }

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
    val visibility: AlbumVisibility = AlbumVisibility.PRIVATE,
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
    val coverUrl: String? = null,
    val visibility: AlbumVisibility? = null
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

@Serializable
data class PublicAlbumDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val ownerDisplayName: String,
    val ownerAvatarUrl: String? = null,
    val mediaCount: Int,
    val createdAt: Instant
)

@Serializable
data class PublicMediaDto(
    val id: String,
    val type: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val takenAt: Instant? = null,
    val uploadedAt: Instant
)

@Serializable
data class PublicAlbumViewDto(
    val album: PublicAlbumDto,
    val media: List<PublicMediaDto>
)
