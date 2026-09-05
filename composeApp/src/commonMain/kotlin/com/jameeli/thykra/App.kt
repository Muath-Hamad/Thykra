package com.jameeli.thykra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.AppNavHost
import com.jameeli.thykra.ui.theme.LocalThemeMode
import com.jameeli.thykra.ui.theme.ThykraTheme
import com.jameeli.thykra.ui.theme.rememberThemeModeState

@Composable
fun App(
    authViewModel: AuthViewModel,
    albumApi: AlbumApi,
    mediaApi: MediaApi,
    reactionApi: ReactionApi,
    commentApi: CommentApi,
    profileApi: ProfileApi,
    uploadQueueManager: UploadQueueManager
) {
    // Read synchronously on first composition so a Darkroom phone never flashes Paper.
    val themeMode by rememberThemeModeState()
    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        ThykraTheme {
            AppNavHost(
                authViewModel = authViewModel,
                albumApi = albumApi,
                mediaApi = mediaApi,
                reactionApi = reactionApi,
                commentApi = commentApi,
                profileApi = profileApi,
                uploadQueueManager = uploadQueueManager,
            )
        }
    }
}
