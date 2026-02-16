package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.UpdateProfileRequest
import com.jameeli.thykra.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID

fun Route.profileRoutes(userRepository: UserRepository) {
    authenticate("auth-jwt") {
        route("/profile") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val user = userRepository.findById(UUID.fromString(userId))
                if (user != null) {
                    call.respond(ApiResponse(success = true, data = user))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "User not found")
                    )
                }
            }
            put {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val request = call.receive<UpdateProfileRequest>()
                val user = userRepository.updateProfile(
                    UUID.fromString(userId),
                    request.displayName,
                    request.avatarUrl
                )
                if (user != null) {
                    call.respond(ApiResponse(success = true, data = user))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, error = "User not found")
                    )
                }
            }
        }
    }
}
