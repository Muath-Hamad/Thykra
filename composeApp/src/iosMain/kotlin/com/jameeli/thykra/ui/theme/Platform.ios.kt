package com.jameeli.thykra.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
actual fun rememberHaptics(): (HapticKind) -> Unit = remember {
    val selection = UISelectionFeedbackGenerator()
    val notification = UINotificationFeedbackGenerator()
    val impact = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    ({ kind: HapticKind ->
        when (kind) {
            HapticKind.Tick -> selection.selectionChanged()
            HapticKind.Confirm -> notification.notificationOccurred(
                UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
            )
            HapticKind.Reject -> notification.notificationOccurred(
                UINotificationFeedbackType.UINotificationFeedbackTypeError,
            )
            HapticKind.LongPress -> impact.impactOccurred()
            HapticKind.None -> Unit
        }
    })
}

@Composable
actual fun platformReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }

/**
 * iOS decides status-bar style from the view controller, and the viewer is already drawn
 * on a near-black backdrop, so the light icons come for free. The hook exists so the
 * viewer's call site is identical on both platforms.
 */
@Composable
actual fun LightStatusBarIcons(enabled: Boolean) {
    // No-op: handled by the hosting UIViewController's preferredStatusBarStyle.
}

actual object ThemePreference {

    private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

    actual fun load(): ThemeMode =
        ThemeMode.fromStorageKey(defaults.stringForKey(KEY_MODE))

    actual fun save(mode: ThemeMode) {
        defaults.setObject(mode.storageKey, KEY_MODE)
    }

    private const val KEY_MODE = "thykra_theme_mode"
}
