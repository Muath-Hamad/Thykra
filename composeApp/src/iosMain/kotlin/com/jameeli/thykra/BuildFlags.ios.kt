package com.jameeli.thykra

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual val KitGalleryEnabled: Boolean = Platform.isDebugBinary
