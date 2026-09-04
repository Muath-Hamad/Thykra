package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.RecapRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Recaps — generated editions of a trip (Screen Specs doc 3, "Proposed contract"):
 * - GET  /recaps               — recaps across all my albums, newest first
 * - GET  /albums/{id}/recaps   — one album's recaps, member-only
 * - POST /albums/{id}/recaps   — build synchronously (OWNER or CONTRIBUTOR); returns READY.
 *   With fewer than 12 ACTIVE media, responds 400 with error "NOT_ENOUGH_MEDIA:{needed}"
 *   where needed = 12 - count; clients parse the count for the honest gate copy.
 * - GET  /r/{shareToken}       — public reader, no auth; 404 for unknown tokens.
 */
fun Route.recapRoutes(
    recapRepository: RecapRepository,
    albumMemberRepository: AlbumMemberRepository
) {
    authenticate("auth-jwt") {
        route("/recaps") {
            get {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val recaps = recapRepository.listForUser(userId)
                call.respond(ApiResponse(success = true, data = recaps))
            }
        }

        route("/albums/{id}/recaps") {
            get {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["id"])
                if (!albumMemberRepository.isMember(albumId, userId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val recaps = recapRepository.listForAlbum(albumId)
                call.respond(ApiResponse(success = true, data = recaps))
            }

            post {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, userId)
                if (role != MemberRole.OWNER && role != MemberRole.CONTRIBUTOR) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only owners and contributors can create recaps")
                    )
                    return@post
                }
                when (val result = recapRepository.build(albumId, userId)) {
                    is RecapRepository.BuildResult.NotEnoughMedia -> call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "NOT_ENOUGH_MEDIA:${result.needed}")
                    )
                    is RecapRepository.BuildResult.Built -> call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = result.recap)
                    )
                }
            }
        }
    }

    // Public recap reader. The link works regardless of album visibility — the owner
    // explicitly shared the recap. Never exposes the member list beyond contributors
    // of the chosen media.
    get("/r/{shareToken}") {
        val shareToken = call.parameters["shareToken"]!!
        val view = recapRepository.findViewByShareToken(shareToken)
        if (view == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(success = false, error = "Recap not found")
            )
            return@get
        }
        call.respond(ApiResponse(success = true, data = view))
    }
}
