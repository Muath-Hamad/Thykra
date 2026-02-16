package com.jameeli.thykra.plugins

import com.jameeli.thykra.repository.UserRepository
import com.jameeli.thykra.routes.authRoutes
import com.jameeli.thykra.routes.profileRoutes
import com.jameeli.thykra.service.AuthService
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(authService: AuthService, userRepository: UserRepository) {
    routing {
        get("/") {
            call.respondText("Thykra API v1.0")
        }
        route("/api") {
            get("/health") {
                call.respondText("OK")
            }
            authRoutes(authService)
            profileRoutes(userRepository)
        }
    }
}
