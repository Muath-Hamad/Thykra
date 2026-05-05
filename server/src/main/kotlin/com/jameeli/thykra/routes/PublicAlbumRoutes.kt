package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.repository.AlbumRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.publicAlbumRoutes(albumRepository: AlbumRepository) {
    route("/public/albums") {
        get("/{id}") {
            val albumId = try {
                UUID.fromString(call.parameters["id"])
            } catch (_: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Album not found")
                )
                return@get
            }
            val view = albumRepository.findPublicView(albumId)
            if (view == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(success = false, error = "Album not found or not publicly shared")
                )
                return@get
            }
            call.respond(ApiResponse(success = true, data = view))
        }
    }
}
