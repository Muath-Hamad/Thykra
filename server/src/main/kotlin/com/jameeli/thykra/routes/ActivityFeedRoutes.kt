package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ActivitySeenDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MarkActivitySeenRequest
import com.jameeli.thykra.repository.ActivityFeedRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.time.Clock
import kotlin.time.Instant
import java.util.UUID

/**
 * Aggregated activity feed (Wanderlust Editions):
 * - GET  /activity?cursor=&limit=            — across all my albums, my own events excluded
 * - GET  /albums/{id}/activity?cursor=&limit= — one album, member-only, all actors included
 * - POST /activity/seen {seenAt?}             — server-side read marker (defaults to now)
 *
 * The cursor is opaque to clients: the ISO instant of the last item's createdAt, applied
 * with "before" semantics. The legacy GET /activity/recent stays untouched in ActivityRoutes.
 */
fun Route.activityFeedRoutes(
    activityFeedRepository: ActivityFeedRepository,
    albumMemberRepository: AlbumMemberRepository
) {
    authenticate("auth-jwt") {
        route("/activity") {
            get {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val feed = activityFeedRepository.feedForUser(
                    userId = userId,
                    before = call.parseCursor(),
                    limit = call.parseLimit()
                )
                call.respond(ApiResponse(success = true, data = feed))
            }

            post("/seen") {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val request = runCatching { call.receive<MarkActivitySeenRequest>() }
                    .getOrDefault(MarkActivitySeenRequest())
                val seenAt = request.seenAt ?: Clock.System.now()
                activityFeedRepository.markSeen(userId, seenAt)
                call.respond(ApiResponse(success = true, data = ActivitySeenDto(seenAt = seenAt)))
            }
        }

        get("/albums/{id}/activity") {
            val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
            val albumId = UUID.fromString(call.parameters["id"])
            if (!albumMemberRepository.isMember(albumId, userId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Not a member of this album")
                )
                return@get
            }
            val feed = activityFeedRepository.feedForAlbum(
                albumId = albumId,
                viewerId = userId,
                before = call.parseCursor(),
                limit = call.parseLimit()
            )
            call.respond(ApiResponse(success = true, data = feed))
        }
    }
}

// Instant.parse throws IllegalArgumentException on malformed cursors, which StatusPages maps to 400.
private fun ApplicationCall.parseCursor(): Instant? =
    request.queryParameters["cursor"]?.takeIf { it.isNotBlank() }?.let { Instant.parse(it) }

private fun ApplicationCall.parseLimit(): Int =
    (request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_FEED_LIMIT).coerceIn(1, MAX_FEED_LIMIT)

private const val DEFAULT_FEED_LIMIT = 30
private const val MAX_FEED_LIMIT = 100
