package com.jameeli.thykra.plugins

import com.jameeli.thykra.model.ApiResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = cause.message)
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception on ${call.request.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, error = "Internal server error")
            )
        }
    }
}
