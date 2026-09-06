package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.ConfirmUploadRequest
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.RequestUploadUrlRequest
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.MediaRepository
import com.jameeli.thykra.service.MediaService
import com.jameeli.thykra.storage.LocalStorageService
import com.jameeli.thykra.storage.StorageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.mediaRoutes(
    mediaService: MediaService,
    mediaRepository: MediaRepository,
    albumMemberRepository: AlbumMemberRepository,
    storageService: StorageService,
    /** Published at GET /api/config; enforced here so the limit is real, not advisory. */
    maxUploadBytes: Long = Long.MAX_VALUE
) {
    // Local dev upload and file-serve endpoints (no JWT — upload auth via DB record existence)
    if (storageService is LocalStorageService) {
        put("/media/upload/{key...}") {
            val key = call.parameters.getAll("key")?.joinToString("/")
                ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Missing storage key")
                )
            val media = mediaRepository.findByStorageKey(key)
                ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Upload slot not found")
                )
            if (media.status.name != "PENDING") {
                return@put call.respond(
                    HttpStatusCode.Conflict,
                    ApiResponse<Unit>(success = false, error = "Upload already completed")
                )
            }
            storageService.saveFile(key, call.receiveChannel())
            call.respond(HttpStatusCode.NoContent)
        }

        get("/media/files/{key...}") {
            val key = call.parameters.getAll("key")?.joinToString("/")
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = storageService.getFile(key)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondFile(file)
        }
    }

    authenticate("auth-jwt") {
        route("/albums/{albumId}/media") {
            post("/request-upload") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["albumId"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER && role != MemberRole.CONTRIBUTOR) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only owners and contributors can upload media")
                    )
                    return@post
                }
                val request = call.receive<RequestUploadUrlRequest>()
                if (request.fileSize > maxUploadBytes) {
                    // Refused before a storage key is minted, so an oversized file never
                    // leaves a pending row behind for the cleanup to find.
                    call.respond(
                        HttpStatusCode.PayloadTooLarge,
                        ApiResponse<Unit>(
                            success = false,
                            error = "That file is larger than the ${maxUploadBytes / 1_048_576} MB limit"
                        )
                    )
                    return@post
                }
                val result = mediaService.requestUploadUrl(
                    albumId, UUID.fromString(userId), request.filename, request.contentType, request.fileSize
                )
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result))
            }

            post("/{mediaId}/confirm") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER && role != MemberRole.CONTRIBUTOR) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not authorized")
                    )
                    return@post
                }
                val request = call.receive<ConfirmUploadRequest>()
                val media = mediaService.confirmUpload(mediaId, request.width, request.height, request.durationMs, request.takenAt)
                if (media != null) {
                    call.respond(ApiResponse(success = true, data = media))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Media not found"))
                }
            }

            get {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["albumId"])
                if (!albumMemberRepository.isMember(albumId, UUID.fromString(userId))) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val media = mediaService.listMedia(albumId)
                call.respond(ApiResponse(success = true, data = media))
            }

            get("/{mediaId}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])
                if (!albumMemberRepository.isMember(albumId, UUID.fromString(userId))) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val media = mediaService.getMedia(mediaId)
                if (media != null) {
                    call.respond(ApiResponse(success = true, data = media))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Media not found"))
                }
            }

            delete("/{mediaId}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["albumId"])
                val mediaId = UUID.fromString(call.parameters["mediaId"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                val media = mediaService.getMedia(mediaId)
                if (media == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Media not found"))
                    return@delete
                }
                if (role != MemberRole.OWNER && media.uploaderId != userId) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the album owner or the uploader can delete this media")
                    )
                    return@delete
                }
                mediaService.deleteMedia(mediaId)
                call.respond(ApiResponse(success = true, data = "Media deleted"))
            }
        }
    }
}
