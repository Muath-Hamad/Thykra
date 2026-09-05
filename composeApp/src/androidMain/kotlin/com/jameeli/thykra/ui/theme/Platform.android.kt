package com.jameeli.thykra.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun rememberHaptics(): (HapticKind) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { kind ->
            val constant: Int? = when (kind) {
                HapticKind.Tick -> HapticFeedbackConstants.CLOCK_TICK
                HapticKind.LongPress -> HapticFeedbackConstants.LONG_PRESS
                // CONFIRM and REJECT landed in API 30. Below that, the nearest thing the
                // platform has is a keyboard tap, which is at least felt.
                HapticKind.Confirm ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                HapticKind.Reject ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                HapticKind.None -> null
            }
            if (constant != null) view.performHapticFeedback(constant)
        }
    }
}

@Composable
actual fun platformReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
actual fun LightStatusBarIcons(enabled: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(enabled, window) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = controller.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller.isAppearanceLightNavigationBars
        if (enabled) {
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose {
            controller.isAppearanceLightStatusBars = previousLightStatusBars
            controller.isAppearanceLightNavigationBars = previousLightNavigationBars
        }
    }
}

actual object ThemePreference {

    /**
     * Set once from `MainActivity.onCreate` before `setContent`, the same way
     * `SharingHost.appContext` is. Until it is set, [load] answers
     * [ThemeMode.System], which is also the correct answer.
     */
    var appContext: Context? = null

    private val prefs: SharedPreferences?
        get() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun load(): ThemeMode =
        ThemeMode.fromStorageKey(prefs?.getString(KEY_MODE, null))

    actual fun save(mode: ThemeMode) {
        prefs?.edit()?.putString(KEY_MODE, mode.storageKey)?.apply()
    }

    private const val PREFS = "thykra_appearance"
    private const val KEY_MODE = "theme_mode"
}
