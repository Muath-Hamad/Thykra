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
import kotlinx.serialization.Serializable

@Serializable
data class DevLoginRequest(
    val email: String,
    val displayName: String
)

fun Route.authRoutes(authService: AuthService, allowDevLogin: Boolean = false) {
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

        // E2E-only backdoor — only mounted when ALLOW_DEV_LOGIN=true. Issues a real AuthResponse
        // for an arbitrary { email, displayName } so Playwright can skip OAuth entirely. NEVER
        // enable in production.
        if (allowDevLogin) {
            post("/dev-login") {
                val request = call.receive<DevLoginRequest>()
                val email = request.email.trim()
                val displayName = request.displayName.trim()
                if (email.isEmpty() || displayName.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "email and displayName are required")
                    )
                    return@post
                }
                val result = authService.devLogin(email, displayName)
                call.respond(ApiResponse(success = true, data = result))
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
