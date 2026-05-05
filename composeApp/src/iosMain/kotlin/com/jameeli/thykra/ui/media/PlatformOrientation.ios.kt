package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * iOS uses [LocalWindowInfo.containerSize] to compare width/height, since Compose for iOS
 * does not surface an Android-style Configuration object.
 */
@Composable
actual fun rememberIsLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.width > size.height
}
