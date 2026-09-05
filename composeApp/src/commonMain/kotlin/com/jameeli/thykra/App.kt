package com.jameeli.thykra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.jameeli.thykra.api.ActivityFeedApi
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.RecapApi
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.InviteApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.AppNavHost
import com.jameeli.thykra.ui.me.DevicePreferences
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
    uploadQueueManager: UploadQueueManager,
    inviteApi: InviteApi,
    activityFeedApi: ActivityFeedApi,
    recapApi: RecapApi,
    devicePreferences: DevicePreferences,
    networkMonitor: NetworkMonitor? = null,
) {
    // Read synchronously on first composition so a Darkroom phone never flashes Paper.
    val themeModeState = rememberThemeModeState()
    val themeMode by themeModeState
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
                inviteApi = inviteApi,
                activityFeedApi = activityFeedApi,
                recapApi = recapApi,
                devicePreferences = devicePreferences,
                themeMode = themeMode,
                onThemeModeChange = { themeModeState.value = it },
                networkMonitor = networkMonitor,
            )
        }
    }
}
