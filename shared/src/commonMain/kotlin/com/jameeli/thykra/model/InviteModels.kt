package com.jameeli.thykra.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Outcome of resolving an invite token for the invite landing page.
 *
 * The status IS the answer: unknown/revoked/expired tokens still produce an HTTP 200
 * with success=true so the client can render a designed dead end instead of an error.
 */
@Serializable
enum class InviteStatus { VALID, EXPIRED, REVOKED, ALREADY_MEMBER, BLOCKED }

@Serializable
data class InviterDto(
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class PreviewMediaDto(
    val thumbnailUrl: String
)

/**
 * Unauthenticated (but auth-aware) preview of an invite link.
 *
 * Field population by [status]:
 * - REVOKED (also unknown token): everything null — nothing about the trip leaks.
 * - BLOCKED: everything null — a blocked person is shown no trip name and no cover.
 * - EXPIRED: [album], [invitedBy] and [expiresAt] are set so the page can say what
 *   the link was for; media details are omitted.
 * - ALREADY_MEMBER: album details plus the caller's [joinedAt].
 * - VALID: [album], [invitedBy], [mediaCount], [expiresAt] and up to 3 [previewMedia].
 */
@Serializable
data class InvitePreviewDto(
    val album: AlbumDto? = null,
    val invitedBy: InviterDto? = null,
    val mediaCount: Int? = null,
    val expiresAt: Instant? = null,
    val status: InviteStatus,
    val previewMedia: List<PreviewMediaDto> = emptyList(),
    val joinedAt: Instant? = null
)
