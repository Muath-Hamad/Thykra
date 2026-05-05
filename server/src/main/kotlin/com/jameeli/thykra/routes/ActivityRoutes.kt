package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.RecentActivityDto
import com.jameeli.thykra.repository.ActivityRepository
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.activityRoutes(activityRepository: ActivityRepository) {
    authenticate("auth-jwt") {
        route("/activity") {
            get("/recent") {
                val userId = UUID.fromString(call.principal<JWTPrincipal>()!!.subject!!)
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT)
                    .coerceIn(MIN_LIMIT, MAX_LIMIT)
                val items = activityRepository.recentForUser(userId, limit)
                call.respond(ApiResponse(success = true, data = RecentActivityDto(items = items)))
            }
        }
    }
}

private const val DEFAULT_LIMIT = 25
private const val MIN_LIMIT = 1
private const val MAX_LIMIT = 50
