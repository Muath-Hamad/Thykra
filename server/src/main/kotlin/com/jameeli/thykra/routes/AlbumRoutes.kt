package com.jameeli.thykra.routes

import com.jameeli.thykra.model.AddMemberRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.CreateAlbumRequest
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.UpdateAlbumRequest
import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
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
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration.Companion.days

fun Route.albumRoutes(
    albumRepository: AlbumRepository,
    albumMemberRepository: AlbumMemberRepository,
    albumInviteRepository: AlbumInviteRepository
) {
    authenticate("auth-jwt") {
        route("/albums") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albums = albumRepository.findAllForUser(UUID.fromString(userId))
                call.respond(ApiResponse(success = true, data = albums))
            }

            post {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val request = call.receive<CreateAlbumRequest>()
                val album = albumRepository.create(
                    ownerId = UUID.fromString(userId),
                    title = request.title,
                    description = request.description
                )
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = album))
            }

            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                if (!albumMemberRepository.isMember(albumId, UUID.fromString(userId))) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val album = albumRepository.findById(albumId)
                if (album != null) {
                    call.respond(ApiResponse(success = true, data = album))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Album not found")
                    )
                }
            }

            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can update the album")
                    )
                    return@put
                }
                val request = call.receive<UpdateAlbumRequest>()
                val album = albumRepository.update(
                    albumId, request.title, request.description, request.coverUrl, request.visibility
                )
                if (album != null) {
                    call.respond(ApiResponse(success = true, data = album))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Album not found")
                    )
                }
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can delete the album")
                    )
                    return@delete
                }
                albumRepository.delete(albumId)
                call.respond(ApiResponse(success = true, data = "Album deleted"))
            }

            get("/{id}/members") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                if (!albumMemberRepository.isMember(albumId, UUID.fromString(userId))) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Not a member of this album")
                    )
                    return@get
                }
                val members = albumMemberRepository.findMembers(albumId)
                call.respond(ApiResponse(success = true, data = members))
            }

            post("/{id}/members") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can add members")
                    )
                    return@post
                }
                val request = call.receive<AddMemberRequest>()
                val member = albumMemberRepository.addMember(
                    albumId, UUID.fromString(request.userId), request.role
                )
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = member))
            }

            delete("/{id}/members/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val targetUserId = call.parameters["userId"]!!
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(currentUserId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can remove members")
                    )
                    return@delete
                }
                albumMemberRepository.removeMember(albumId, UUID.fromString(targetUserId))
                call.respond(ApiResponse(success = true, data = "Member removed"))
            }

            post("/{id}/invite") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER && role != MemberRole.CONTRIBUTOR) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only owners and contributors can create invite links")
                    )
                    return@post
                }
                val token = UUID.randomUUID().toString()
                val expiresAt = Clock.System.now() + 7.days
                val invite = albumInviteRepository.createInvite(
                    albumId, UUID.fromString(userId), token, expiresAt
                )
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = invite))
            }

            post("/join/{token}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val token = call.parameters["token"]!!
                val invite = albumInviteRepository.findValidByToken(token)
                if (invite == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid or expired invite link")
                    )
                    return@post
                }
                val userUuid = UUID.fromString(userId)
                if (albumMemberRepository.isMember(invite.albumId, userUuid)) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, error = "Already a member of this album")
                    )
                    return@post
                }
                albumMemberRepository.addMember(invite.albumId, userUuid, MemberRole.CONTRIBUTOR)
                albumInviteRepository.markUsed(token, userUuid)
                val album = albumRepository.findById(invite.albumId)
                call.respond(ApiResponse(success = true, data = album))
            }
        }
    }
}
