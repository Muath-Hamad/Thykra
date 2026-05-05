package com.jameeli.thykra.routes

import com.jameeli.thykra.model.AddReactionRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.MediaRepository
import com.jameeli.thykra.repository.ReactionRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.reactionRoutes(
    reactionRepository: ReactionRepository,
    mediaRepository: MediaRepository,
    albumMemberRepository: AlbumMemberRepository
) {
    authenticate("auth-jwt") {
        route("/albums/{albumId}/media/{mediaId}/reactions") {
            get {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])

                if (!albumMemberRepository.isMember(albumId, userId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val media = mediaRepository.findById(mediaId)
                if (media == null || media.albumId != albumId.toString()) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Media not found in this album")
                    )
                    return@get
                }
                val result = reactionRepository.listForMedia(mediaId, userId)
                call.respond(ApiResponse(success = true, data = result))
            }

            post {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])

                if (!albumMemberRepository.isMember(albumId, userId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@post
                }
                val media = mediaRepository.findById(mediaId)
                if (media == null || media.albumId != albumId.toString()) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Media not found in this album")
                    )
                    return@post
                }
                val request = call.receive<AddReactionRequest>()
                reactionRepository.toggle(mediaId, userId, request.type)
                val result = reactionRepository.listForMedia(mediaId, userId)
                call.respond(ApiResponse(success = true, data = result))
            }
        }
    }
}
