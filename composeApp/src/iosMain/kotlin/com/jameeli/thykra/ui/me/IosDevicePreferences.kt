package com.jameeli.thykra.ui.me

import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication

/**
 * The iOS side of [DevicePreferences].
 *
 * Language is written to `AppleLanguages`, which iOS reads on next launch — unlike
 * Android there is no in-process locale swap, so the Me screen's language change takes
 * effect when the app is next opened. That difference is the platform's, not the design's.
 */
class IosDevicePreferences : DevicePreferences {

    private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

    override var wifiOnlyUploads: Boolean
        get() = defaults.boolForKey(KEY_WIFI_ONLY)
        set(value) {
            defaults.setBool(value, KEY_WIFI_ONLY)
        }

    override var applicationLanguage: String
        get() = defaults.stringForKey(KEY_LANGUAGE) ?: "en"
        set(value) {
            defaults.setObject(value, KEY_LANGUAGE)
            // Takes effect on next launch; iOS has no in-process equivalent of
            // AppCompatDelegate.setApplicationLocales.
            defaults.setObject(listOf(value), "AppleLanguages")
        }

    override val versionLabel: String
        get() {
            val info = NSBundle.mainBundle.infoDictionary
            val short = info?.get("CFBundleShortVersionString") as? String ?: "1.0"
            val build = info?.get("CFBundleVersion") as? String ?: "1"
            return "$short ($build) · Wanderlust Editions"
        }

    override fun describeCacheSize(): String {
        val bytes = SingletonImageLoader.get(PlatformContext.INSTANCE).diskCache?.size ?: 0L
        val mb = bytes / 1_000_000.0
        return when {
            mb >= 1000 -> "${((mb / 1000) * 10).toInt() / 10.0} GB of thumbnails and originals"
            mb >= 1 -> "${mb.toInt()} MB of thumbnails and originals"
            else -> "Nothing saved yet"
        }
    }

    override suspend fun clearImageCache() {
        withContext(Dispatchers.Default) {
            // Staged uploads are left alone here as on Android: they are work in
            // progress, not a cache.
            val loader = SingletonImageLoader.get(PlatformContext.INSTANCE)
            loader.diskCache?.clear()
            loader.memoryCache?.clear()
        }
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }

    private companion object {
        const val KEY_WIFI_ONLY = "wifi_only_uploads"
        const val KEY_LANGUAGE = "app_language"
    }
}
