package com.jameeli.thykra.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `system | paper | darkroom`, persisted under the same three names the web stores in
 * `localStorage`, so a screenshot of the two platforms is the same page at two widths.
 */
enum class ThemeMode(val storageKey: String) {
    System("system"),
    Paper("paper"),
    Darkroom("darkroom");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: System
    }
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.System }

/**
 * The app-level theme mode, loaded synchronously on first composition and written back
 * whenever it changes. Hoisted so the Me screen's segmented control can drive it.
 */
@Composable
fun rememberThemeModeState(): MutableState<ThemeMode> {
    val state = remember { mutableStateOf(ThemePreference.load()) }
    LaunchedEffect(Unit) {
        snapshotFlow { state.value }.collect { ThemePreference.save(it) }
    }
    return state
}
