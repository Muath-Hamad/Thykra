package com.jameeli.thykra

import androidx.compose.runtime.Composable
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.AppNavHost
import com.jameeli.thykra.ui.theme.ThykraTheme

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
    ThykraTheme {
        AppNavHost(
            authViewModel = authViewModel,
            albumApi = albumApi,
            mediaApi = mediaApi,
            reactionApi = reactionApi,
            commentApi = commentApi,
            profileApi = profileApi,
            uploadQueueManager = uploadQueueManager
        )
    }
}
