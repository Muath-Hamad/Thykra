package com.jameeli.thykra.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

/**
 * Shared Preferences-backed Glance state definition for both Thykra widgets.
 *
 * Each widget instance ([GlanceId]) gets its own preferences file containing the bound album id
 * and refresh interval chosen during configuration.
 */
object WidgetPrefs {
    val SELECTED_ALBUM_ID = stringPreferencesKey("selected_album_id")
    val SELECTED_ALBUM_TITLE = stringPreferencesKey("selected_album_title")
    val REFRESH_INTERVAL_MS = longPreferencesKey("refresh_interval_ms")

    /** Default refresh interval when the user hasn't picked one (1 hour). */
    const val DEFAULT_REFRESH_INTERVAL_MS: Long = 60 * 60 * 1000L
}

/**
 * Helper for the configuration activity to persist the user's album selection into the
 * widget's per-instance state before [GlanceAppWidget.update] is called.
 */
suspend fun saveWidgetConfig(
    context: android.content.Context,
    glanceId: GlanceId,
    albumId: String,
    albumTitle: String,
    refreshIntervalMs: Long
) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[WidgetPrefs.SELECTED_ALBUM_ID] = albumId
            this[WidgetPrefs.SELECTED_ALBUM_TITLE] = albumTitle
            this[WidgetPrefs.REFRESH_INTERVAL_MS] = refreshIntervalMs
        }
    }
}

/** Reads the configured album id (or null if the widget hasn't been configured yet). */
fun Preferences.albumId(): String? = this[WidgetPrefs.SELECTED_ALBUM_ID]

/** Reads the cached album title shown in the widget chrome. */
fun Preferences.albumTitle(): String? = this[WidgetPrefs.SELECTED_ALBUM_TITLE]

/** Reads the configured refresh interval, falling back to the default. */
fun Preferences.refreshIntervalMs(): Long =
    this[WidgetPrefs.REFRESH_INTERVAL_MS] ?: WidgetPrefs.DEFAULT_REFRESH_INTERVAL_MS
