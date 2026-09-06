package com.jameeli.thykra

import com.jameeli.thykra.shared.BuildConfig

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

/**
 * 10.0.2.2 is the emulator's loopback to the host machine. A build for a real device
 * overrides this: put `API_BASE_URL=https://…` in `local.properties`, or set
 * `THYKRA_API_BASE_URL` in the environment.
 */
actual val API_BASE_URL: String = BuildConfig.API_BASE_URL

// Emulator loopback; production builds should override via a gradle
// buildConfigField similar to GOOGLE_CLIENT_ID.
actual val WEB_BASE_URL: String = BuildConfig.WEB_BASE_URL