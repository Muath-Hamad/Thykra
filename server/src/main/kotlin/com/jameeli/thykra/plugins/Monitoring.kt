package com.jameeli.thykra.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) { level = Level.INFO }
    install(DefaultHeaders) { header("X-Engine", "Ktor") }
}
