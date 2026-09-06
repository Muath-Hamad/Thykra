package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.InvitePreviewDto
import com.jameeli.thykra.model.InviteStatus
import com.jameeli.thykra.model.InviterDto
import com.jameeli.thykra.model.PreviewMediaDto
import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
import com.jameeli.thykra.repository.BlockedMemberRepository
import com.jameeli.thykra.repository.MediaRepository
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlin.time.Clock
import java.util.UUID

/**
 * Invite landing preview — GET /invites/{token}/preview.
 *
 * No auth required, but auth-aware: a signed-in caller additionally gets
 * ALREADY_MEMBER / BLOCKED statuses. Every outcome is HTTP 200 with success=true;
 * the status field IS the answer, so the landing page can render its designed
 * dead ends without a failed request.
 *
 * Status precedence: REVOKED (incl. unknown token) > BLOCKED > ALREADY_MEMBER >
 * EXPIRED > VALID. BLOCKED deliberately carries no album details — a blocked
 * person is shown no trip name and no cover.
 */
fun Route.inviteRoutes(
    albumInviteRepository: AlbumInviteRepository,
    albumRepository: AlbumRepository,
    albumMemberRepository: AlbumMemberRepository,
    blockedMemberRepository: BlockedMemberRepository,
    mediaRepository: MediaRepository
) {
    authenticate("auth-jwt", optional = true) {
        get("/invites/{token}/preview") {
            val token = call.parameters["token"]!!
            val callerId = call.principal<JWTPrincipal>()?.subject?.let { UUID.fromString(it) }

            val invite = albumInviteRepository.findDetailsByToken(token)
            if (invite == null || invite.revokedAt != null) {
                call.respond(ApiResponse(success = true, data = InvitePreviewDto(status = InviteStatus.REVOKED)))
                return@get
            }

            if (callerId != null && blockedMemberRepository.isBlocked(invite.albumId, callerId)) {
                call.respond(ApiResponse(success = true, data = InvitePreviewDto(status = InviteStatus.BLOCKED)))
                return@get
            }

            val album = albumRepository.findById(invite.albumId)
            if (album == null) {
                // Album deleted underneath the invite — same designed dead end as revoked.
                call.respond(ApiResponse(success = true, data = InvitePreviewDto(status = InviteStatus.REVOKED)))
                return@get
            }
            val invitedBy = InviterDto(
                displayName = invite.inviterDisplayName,
                avatarUrl = invite.inviterAvatarUrl
            )

            val joinedAt = callerId?.let { albumMemberRepository.getJoinedAt(invite.albumId, it) }
            if (joinedAt != null) {
                call.respond(
                    ApiResponse(
                        success = true,
                        data = InvitePreviewDto(
                            album = album,
                            invitedBy = invitedBy,
                            mediaCount = mediaRepository.countActiveForAlbum(invite.albumId),
                            expiresAt = invite.expiresAt,
                            status = InviteStatus.ALREADY_MEMBER,
                            joinedAt = joinedAt
                        )
                    )
                )
                return@get
            }

            if (invite.expiresAt <= Clock.System.now()) {
                // Expired links still name the trip ("It was for Wadi Rum") and the inviter.
                call.respond(
                    ApiResponse(
                        success = true,
                        data = InvitePreviewDto(
                            album = album,
                            invitedBy = invitedBy,
                            expiresAt = invite.expiresAt,
                            status = InviteStatus.EXPIRED
                        )
                    )
                )
                return@get
            }

            call.respond(
                ApiResponse(
                    success = true,
                    data = InvitePreviewDto(
                        album = album,
                        invitedBy = invitedBy,
                        mediaCount = mediaRepository.countActiveForAlbum(invite.albumId),
                        expiresAt = invite.expiresAt,
                        status = InviteStatus.VALID,
                        previewMedia = mediaRepository.latestActiveThumbnailUrls(invite.albumId, limit = 3)
                            .map { PreviewMediaDto(thumbnailUrl = it) }
                    )
                )
            )
        }
    }
}
