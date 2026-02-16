package com.jameeli.thykra.plugins

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Thykra API v1.0")
        }
        route("/api") {
            get("/health") {
                call.respondText("OK")
            }
        }
    }
}
