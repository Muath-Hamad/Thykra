package com.jameeli.thykra.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Optimistic always-connected monitor for iOS.
// NWPathMonitor is a pure Swift type and is not directly accessible from Kotlin/Native.
// Real NWPathMonitor integration requires a Swift bridge layer — TODO when building on macOS.
class IosNetworkMonitor : NetworkMonitor {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
}
