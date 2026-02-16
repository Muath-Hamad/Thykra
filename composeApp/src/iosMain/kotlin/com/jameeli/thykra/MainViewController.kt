package com.jameeli.thykra

import androidx.compose.ui.window.ComposeUIViewController
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.InMemoryTokenProvider

fun MainViewController() = ComposeUIViewController {
    val tokenProvider = InMemoryTokenProvider()
    val httpClient = createApiClient(tokenProvider)
    val authApi = AuthApi(httpClient)
    val authViewModel = AuthViewModel(authApi, tokenProvider)
    App(authViewModel = authViewModel)
}
