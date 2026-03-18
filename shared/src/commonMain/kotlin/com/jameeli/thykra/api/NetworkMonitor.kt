package com.jameeli.thykra.api

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>
}
