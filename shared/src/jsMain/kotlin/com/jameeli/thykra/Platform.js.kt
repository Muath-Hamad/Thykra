package com.jameeli.thykra

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual val API_BASE_URL: String = "http://localhost:$SERVER_PORT"

actual val WEB_BASE_URL: String = window.location.origin