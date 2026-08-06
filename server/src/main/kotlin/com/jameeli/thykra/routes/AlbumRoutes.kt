package com.jameeli.thykra.routes

import com.jameeli.thykra.model.AddMemberRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.CreateAlbumRequest
import com.jameeli.thykra.model.CreateInviteRequest
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.UpdateAlbumRequest
import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
import com.jameeli.thykra.repository.BlockedMemberRepository
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
    albumInviteRepository: AlbumInviteRepository,
    blockedMemberRepository: BlockedMemberRepository
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

            get("/{id}/blocks") {
                val currentUserId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(currentUserId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can view blocked members")
                    )
                    return@get
                }
                val blocked = blockedMemberRepository.list(albumId)
                call.respond(ApiResponse(success = true, data = blocked))
            }

            post("/{id}/blocks/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val targetUserId = UUID.fromString(call.parameters["userId"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(currentUserId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can block members")
                    )
                    return@post
                }
                if (targetUserId.toString() == currentUserId) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Cannot block yourself")
                    )
                    return@post
                }
                albumMemberRepository.removeMember(albumId, targetUserId)
                blockedMemberRepository.block(albumId, targetUserId, UUID.fromString(currentUserId))
                call.respond(ApiResponse(success = true, data = "Member blocked"))
            }

            delete("/{id}/blocks/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val targetUserId = UUID.fromString(call.parameters["userId"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(currentUserId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can unblock members")
                    )
                    return@delete
                }
                blockedMemberRepository.unblock(albumId, targetUserId)
                call.respond(ApiResponse(success = true, data = "Member unblocked"))
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
                val request = runCatching { call.receive<CreateInviteRequest>() }.getOrDefault(CreateInviteRequest())
                val days = (request.expiresInDays ?: 7).coerceIn(1, 90)
                val token = UUID.randomUUID().toString()
                val expiresAt = Clock.System.now() + days.days
                val invite = albumInviteRepository.createInvite(
                    albumId, UUID.fromString(userId), token, expiresAt
                )
                call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = invite))
            }

            get("/{id}/invites") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER && role != MemberRole.CONTRIBUTOR) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only owners and contributors can view invite links")
                    )
                    return@get
                }
                val invites = albumInviteRepository.listActiveForAlbum(albumId)
                call.respond(ApiResponse(success = true, data = invites))
            }

            delete("/{id}/invites/{token}") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val albumId = UUID.fromString(call.parameters["id"])
                val token = call.parameters["token"]!!
                val role = albumMemberRepository.getMemberRole(albumId, UUID.fromString(userId))
                if (role != MemberRole.OWNER) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "Only the owner can revoke invite links")
                    )
                    return@delete
                }
                val revoked = albumInviteRepository.revoke(albumId, token)
                if (!revoked) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "Invite not found")
                    )
                    return@delete
                }
                call.respond(ApiResponse(success = true, data = "Invite revoked"))
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
                if (blockedMemberRepository.isBlocked(invite.albumId, userUuid)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ApiResponse<Unit>(success = false, error = "You have been blocked from this album")
                    )
                    return@post
                }
                if (albumMemberRepository.isMember(invite.albumId, userUuid)) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, error = "Already a member of this album")
                    )
                    return@post
                }
                albumMemberRepository.addMember(invite.albumId, userUuid, MemberRole.CONTRIBUTOR)
                // Links are multi-use: record the join instead of consuming the token.
                albumInviteRepository.recordJoin(invite.id, userUuid)
                val album = albumRepository.findById(invite.albumId)
                call.respond(ApiResponse(success = true, data = album))
            }
        }
    }
}
