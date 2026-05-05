package com.jameeli.thykra.ui.share

/**
 * Platform-specific clipboard write. Used in place of the deprecated
 * `LocalClipboardManager` so commonMain code can copy strings without
 * depending on a Compose-only API whose multiplatform replacement
 * (`LocalClipboard`) is suspend-only.
 */
expect fun copyToClipboard(text: String)
