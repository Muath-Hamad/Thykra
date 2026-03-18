package com.jameeli.thykra

import androidx.compose.ui.window.ComposeUIViewController
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.IosTokenStorage
import kotlin.native.Platform

fun MainViewController() = ComposeUIViewController {
    val tokenProvider = IosTokenStorage()
    val httpClient = createApiClient(tokenProvider, Platform.isDebugBinary)
    val authApi = AuthApi(httpClient)
    val albumApi = AlbumApi(httpClient)
    val authViewModel = AuthViewModel(authApi, tokenProvider)
    App(authViewModel = authViewModel, albumApi = albumApi)
}
