package com.jameeli.thykra.model

import com.jameeli.thykra.chapters.ChapterMedia
import com.jameeli.thykra.chapters.HasDimensions
import kotlin.time.Instant
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
    // Active media in the trip, split by kind. Default 0 so an older client that never
    // sends them — and an older server that never sends them back — both keep working.
    val mediaCount: Int = 0,
    val videoCount: Int = 0,
    val previewMembers: List<AlbumMemberSummary> = emptyList(),
    val createdAt: Instant,
    // Newest activity in the trip, for sorting the list by what moved most recently.
    val lastActivityAt: Instant? = null
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
    val expiresAt: Instant,
    // Number of distinct users who joined through this link. Defaults to 0 so
    // existing mobile clients (which never send/expect it) keep working.
    val joinCount: Int = 0
)

@Serializable
data class CreateInviteRequest(
    val expiresInDays: Int? = null
)

@Serializable
data class BlockedMemberDto(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val blockedAt: Instant
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

// Reaction counts for a media item on public surfaces. Counts only — never identities.
@Serializable
data class PublicReactionSummaryDto(
    val type: ReactionType,
    val count: Int
)

@Serializable
data class PublicMediaDto(
    override val id: String,
    override val type: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    override val width: Int? = null,
    override val height: Int? = null,
    override val takenAt: Instant? = null,
    override val uploadedAt: Instant,
    val reactionSummary: List<PublicReactionSummaryDto> = emptyList()
) : ChapterMedia, HasDimensions

@Serializable
data class PublicAlbumViewDto(
    val album: PublicAlbumDto,
    val media: List<PublicMediaDto>
)
