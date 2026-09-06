package com.jameeli.thykra

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual val API_BASE_URL: String = "http://localhost:$SERVER_PORT"

actual val WEB_BASE_URL: String = "http://localhost:8080"