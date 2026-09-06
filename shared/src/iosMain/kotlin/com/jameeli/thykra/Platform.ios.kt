package com.jameeli.thykra

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

/**
 * Read from `Info.plist`, which `Configuration/Config.xcconfig` fills from
 * `THYKRA_API_BASE_URL`. A simulator build leaves it empty and falls back to localhost;
 * a device or TestFlight build sets it, because on a phone `localhost` is the phone.
 */
actual val API_BASE_URL: String =
    infoPlistString("ThykraApiBaseUrl") ?: "http://localhost:$SERVER_PORT"

actual val WEB_BASE_URL: String =
    infoPlistString("ThykraWebBaseUrl") ?: "http://localhost:8080"

private fun infoPlistString(key: String): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String)?.takeIf { it.isNotBlank() }