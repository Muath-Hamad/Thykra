package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.ClientConfigDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * `GET /api/config` — the limits a client should apply before it starts work.
 *
 * Unauthenticated on purpose. It carries no user data, and the share-to-Thykra flow
 * needs the ceiling while it is still deciding whether the incoming files are usable,
 * which can happen before a token is in hand.
 */
fun Route.configRoutes(config: ClientConfigDto) {
    get("/config") {
        call.respond(ApiResponse(success = true, data = config))
    }
}
