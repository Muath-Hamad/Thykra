package com.jameeli.thykra

import kotlinx.browser.window

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual val API_HOST: String = "localhost"

actual val WEB_BASE_URL: String = window.location.origin