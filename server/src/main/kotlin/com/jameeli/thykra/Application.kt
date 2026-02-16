package com.jameeli.thykra

import com.jameeli.thykra.plugins.configureCors
import com.jameeli.thykra.plugins.configureMonitoring
import com.jameeli.thykra.plugins.configureRouting
import com.jameeli.thykra.plugins.configureSerialization
import com.jameeli.thykra.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureCors()
    configureStatusPages()
    configureRouting()
}
