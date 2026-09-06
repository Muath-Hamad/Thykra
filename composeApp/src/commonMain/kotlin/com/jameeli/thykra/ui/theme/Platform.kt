package com.jameeli.thykra.ui.theme

import androidx.compose.runtime.Composable

/**
 * Five levels, and they never fire for anything the user did not just touch, nor more
 * than once per gesture.
 *
 * - [Tick] segmented and theme taps, picker open, selection toggles, predictive-back
 *   commit, zoom bounds.
 * - [Confirm] reaction pick, batch celebration, joined.
 * - [Reject] destructive confirm, reaction rollback, first failed upload in a batch,
 *   error toasts.
 * - [LongPress] entering selection mode.
 * - [None] everything else — button taps, tab switches, sheet opens, every scroll.
 *
 * There is no in-app haptics switch in v1; the system-wide toggle governs.
 */
enum class HapticKind { Tick, Confirm, Reject, LongPress, None }

/** `val haptic = rememberHaptics()`, then `haptic(HapticKind.Tick)`. */
@Composable
expect fun rememberHaptics(): (HapticKind) -> Unit

/**
 * Android reads `ANIMATOR_DURATION_SCALE == 0`; iOS reads
 * `UIAccessibility.isReduceMotionEnabled`. Read once at the theme root into
 * [LocalReducedMotion].
 */
@Composable
expect fun platformReducedMotion(): Boolean

/**
 * Asks the platform for light status-bar icons while the media viewer owns the screen.
 * Called with `true` on enter and `false` on exit, for the viewer's lifetime only.
 */
@Composable
expect fun LightStatusBarIcons(enabled: Boolean)

/**
 * The theme preference, cached synchronously.
 *
 * The value must be readable **before the first frame** — reading it as a suspended
 * default flashes Paper for one frame on a Darkroom phone, which is the one thing the
 * theme spec asks us not to do. So this is a plain synchronous key-value store
 * (SharedPreferences on Android, NSUserDefaults on iOS) and not DataStore's Flow.
 */
expect object ThemePreference {
    fun load(): ThemeMode
    fun save(mode: ThemeMode)
}
