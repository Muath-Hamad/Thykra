package com.jameeli.thykra

import androidx.compose.runtime.Composable
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.AppNavHost
import com.jameeli.thykra.ui.theme.ThykraTheme

@Composable
fun App(
    authViewModel: AuthViewModel,
    albumApi: AlbumApi,
    mediaApi: MediaApi,
    uploadQueueManager: UploadQueueManager
) {
    ThykraTheme {
        AppNavHost(
            authViewModel = authViewModel,
            albumApi = albumApi,
            mediaApi = mediaApi,
            uploadQueueManager = uploadQueueManager
        )
    }
}
