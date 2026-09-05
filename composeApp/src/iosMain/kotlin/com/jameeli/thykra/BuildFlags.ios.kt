package com.jameeli.thykra

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual val KitGalleryEnabled: Boolean = Platform.isDebugBinary
