package com.jameeli.thykra.ui.me

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.SingletonImageLoader
import com.jameeli.thykra.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The Android side of [DevicePreferences].
 *
 * Language is a per-app locale rather than a restart: `setApplicationLocales` re-creates
 * the activity with the new configuration, which is all the theme root needs to pick up
 * `arabicTypography` and RTL.
 */
class AndroidDevicePreferences(private val context: Context) : DevicePreferences {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override var wifiOnlyUploads: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
        }

    override var applicationLanguage: String
        get() = AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
            ?.substringBefore('-')
            ?: prefs.getString(KEY_LANGUAGE, "en").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value))
        }

    override val versionLabel: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Wanderlust Editions"

    /** Coil's disk cache plus whatever the upload queue has staged but not yet sent. */
    override fun describeCacheSize(): String {
        val coilBytes = SingletonImageLoader.get(context).diskCache?.size ?: 0L
        val stagedBytes = File(context.filesDir, "upload_queue").walkBottomUp()
            .filter { it.isFile }
            .sumOf { it.length() }
        return formatBytes(coilBytes + stagedBytes)
    }

    override suspend fun clearImageCache() {
        withContext(Dispatchers.IO) {
            // Staged uploads are deliberately left alone: they are work in progress, not
            // a cache, and clearing them would silently drop someone's photographs.
            SingletonImageLoader.get(context).diskCache?.clear()
            SingletonImageLoader.get(context).memoryCache?.clear()
        }
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / 1_000_000.0
        return when {
            mb >= 1000 -> "${((mb / 1000) * 10).toInt() / 10.0} GB of thumbnails and originals"
            mb >= 1 -> "${mb.toInt()} MB of thumbnails and originals"
            else -> "Nothing saved yet"
        }
    }

    private companion object {
        const val PREFS = "thykra_device"
        const val KEY_WIFI_ONLY = "wifi_only_uploads"
        const val KEY_LANGUAGE = "app_language"
    }
}
