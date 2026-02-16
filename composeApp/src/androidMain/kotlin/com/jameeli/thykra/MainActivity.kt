package com.jameeli.thykra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AndroidTokenStorage
import com.jameeli.thykra.auth.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val tokenProvider = AndroidTokenStorage(applicationContext)
        val httpClient = createApiClient(tokenProvider)
        val authApi = AuthApi(httpClient)
        val authViewModel = AuthViewModel(authApi, tokenProvider)

        setContent {
            App(authViewModel = authViewModel)
        }
    }
}
