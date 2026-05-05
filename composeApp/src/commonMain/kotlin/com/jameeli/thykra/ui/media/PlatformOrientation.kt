package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable

/**
 * Returns true when the current window is in landscape orientation (width > height).
 * Implemented per platform because Compose Multiplatform does not yet expose a stable
 * cross-platform `LocalConfiguration`.
 */
@Composable
expect fun rememberIsLandscape(): Boolean
