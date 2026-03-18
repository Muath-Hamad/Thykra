package com.jameeli.thykra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.AndroidNetworkMonitor
import com.jameeli.thykra.api.AndroidUploadPersistence
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadWorker
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AndroidTokenStorage
import com.jameeli.thykra.auth.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val tokenProvider = AndroidTokenStorage(applicationContext)
        val httpClient = createApiClient(tokenProvider, BuildConfig.DEBUG)
        val authApi = AuthApi(httpClient)
        val albumApi = AlbumApi(httpClient)
        val mediaApi = MediaApi(httpClient, BuildConfig.DEBUG)
        val persistence = AndroidUploadPersistence(applicationContext)
        val networkMonitor = AndroidNetworkMonitor(applicationContext)
        val uploadQueueManager = UploadQueueManager(mediaApi, lifecycleScope, persistence, networkMonitor)
        val authViewModel = AuthViewModel(authApi, tokenProvider)

        // Schedule background upload worker as a safety net for uploads pending after an app kill
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "upload_queue",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
        )

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
