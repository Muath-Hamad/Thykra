package com.jameeli.thykra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AndroidTokenStorage
import com.jameeli.thykra.auth.AuthViewModel
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val tokenProvider = AndroidTokenStorage(applicationContext)
        val httpClient = createApiClient(tokenProvider, BuildConfig.DEBUG)
        val authApi = AuthApi(httpClient)
        val albumApi = AlbumApi(httpClient)
        val mediaApi = MediaApi(httpClient, BuildConfig.DEBUG)
        val uploadQueueManager = UploadQueueManager(mediaApi, lifecycleScope)
        val authViewModel = AuthViewModel(authApi, tokenProvider)

        setContent {
            App(
                authViewModel = authViewModel,
                albumApi = albumApi,
                mediaApi = mediaApi,
                uploadQueueManager = uploadQueueManager
            )
        }
    }
}
