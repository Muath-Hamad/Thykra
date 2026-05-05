package com.jameeli.thykra

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val API_HOST: String = "10.0.2.2"

// Emulator loopback; production builds should override via a gradle
// buildConfigField similar to GOOGLE_CLIENT_ID.
actual val WEB_BASE_URL: String = "http://10.0.2.2:8080"