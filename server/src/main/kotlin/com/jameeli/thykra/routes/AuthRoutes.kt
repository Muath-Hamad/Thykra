package com.jameeli.thykra.routes

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.OAuthRequest
import com.jameeli.thykra.model.RefreshTokenRequest
import com.jameeli.thykra.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/oauth") {
            val request = call.receive<OAuthRequest>()
            val result = authService.loginWithOAuth(
                provider = request.provider.name.lowercase(),
                idToken = request.idToken
            )
            call.respond(ApiResponse(success = true, data = result))
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val result = authService.refreshToken(request.refreshToken)
            if (result != null) {
                call.respond(ApiResponse(success = true, data = result))
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Invalid or expired refresh token")
                )
            }
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.subject!!
                authService.logout(userId)
                call.respond(ApiResponse(success = true, data = "Logged out"))
            }
        }
    }
}
