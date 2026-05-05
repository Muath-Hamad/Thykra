package com.jameeli.thykra.routes

import com.jameeli.thykra.model.AddCommentRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.UpdateCommentRequest
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.CommentRepository
import com.jameeli.thykra.repository.MediaRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

private const val MAX_COMMENT_LENGTH = 2000

fun Route.commentRoutes(
    commentRepository: CommentRepository,
    mediaRepository: MediaRepository,
    albumMemberRepository: AlbumMemberRepository
) {
    authenticate("auth-jwt") {
        route("/albums/{albumId}/media/{mediaId}/comments") {
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
                val comments = commentRepository.listForMedia(mediaId)
                call.respond(ApiResponse(success = true, data = comments))
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
                val request = call.receive<AddCommentRequest>()
                val body = request.body.trim()
                if (body.isEmpty() || body.length > MAX_COMMENT_LENGTH) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Comment must be 1..$MAX_COMMENT_LENGTH characters")
                    )
                    return@post
                }
                val comment = commentRepository.create(mediaId, userId, body)
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = comment))
            }

            put("/{commentId}") {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])
                val commentId = UUID.fromString(call.parameters["commentId"])

                if (!albumMemberRepository.isMember(albumId, userId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@put
                }
                val existing = commentRepository.findById(commentId)
                if (existing == null || existing.mediaId != mediaId.toString()) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Comment not found")
                    )
                    return@put
                }
                if (existing.authorId != userId.toString()) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the author can edit this comment")
                    )
                    return@put
                }
                val request = call.receive<UpdateCommentRequest>()
                val body = request.body.trim()
                if (body.isEmpty() || body.length > MAX_COMMENT_LENGTH) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Comment must be 1..$MAX_COMMENT_LENGTH characters")
                    )
                    return@put
                }
                val updated = commentRepository.update(commentId, body)!!
                call.respond(ApiResponse(success = true, data = updated))
            }

            delete("/{commentId}") {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])
                val commentId = UUID.fromString(call.parameters["commentId"])

                if (!albumMemberRepository.isMember(albumId, userId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@delete
                }
                val existing = commentRepository.findById(commentId)
                if (existing == null || existing.mediaId != mediaId.toString()) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Comment not found")
                    )
                    return@delete
                }
                val role = albumMemberRepository.getMemberRole(albumId, userId)
                val isAuthor = existing.authorId == userId.toString()
                val isOwner = role == MemberRole.OWNER
                if (!isAuthor && !isOwner) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the author or album owner can delete this comment")
                    )
                    return@delete
                }
                commentRepository.delete(commentId)
                call.respond(ApiResponse(success = true, data = "Comment deleted"))
            }
        }
    }
}
