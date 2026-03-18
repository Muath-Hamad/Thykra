@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.jameeli.thykra

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.IosNetworkMonitor
import com.jameeli.thykra.api.IosUploadPersistence
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.IosTokenStorage
import kotlin.native.Platform

fun MainViewController() = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val tokenProvider = remember { IosTokenStorage() }
    val httpClient = remember { createApiClient(tokenProvider, Platform.isDebugBinary) }
    val albumApi = remember { AlbumApi(httpClient) }
    val mediaApi = remember { MediaApi(httpClient, Platform.isDebugBinary) }
    val persistence = remember { IosUploadPersistence() }
    val networkMonitor = remember { IosNetworkMonitor() }
    val uploadQueueManager = remember(scope) { UploadQueueManager(mediaApi, scope, persistence, networkMonitor) }
    val authApi = remember { AuthApi(httpClient) }
    val authViewModel = remember { AuthViewModel(authApi, tokenProvider) }

    App(
        authViewModel = authViewModel,
        albumApi = albumApi,
        mediaApi = mediaApi,
        uploadQueueManager = uploadQueueManager
    )
}
