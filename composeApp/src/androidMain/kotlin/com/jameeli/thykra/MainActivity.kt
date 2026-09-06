package com.jameeli.thykra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.jameeli.thykra.api.ActivityFeedApi
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.RecapApi
import com.jameeli.thykra.api.AndroidNetworkMonitor
import com.jameeli.thykra.api.AndroidUploadPersistence
import com.jameeli.thykra.api.AuthApi
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.InviteApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadWorker
import com.jameeli.thykra.api.createApiClient
import com.jameeli.thykra.auth.AndroidTokenStorage
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.DeepLinkBus
import com.jameeli.thykra.navigation.DeepLinkTarget
import com.jameeli.thykra.navigation.handleDeepLink
import com.jameeli.thykra.ui.media.mediaFileFromUri
import com.jameeli.thykra.ui.me.AndroidDevicePreferences
import com.jameeli.thykra.ui.upload.IncomingShareBus
import com.jameeli.thykra.ui.share.SharingHost
import com.jameeli.thykra.ui.theme.ThemePreference
import com.jameeli.thykra.widget.WidgetDeepLinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SharingHost.appContext = applicationContext
        // Must be set before setContent: the theme reads the preference synchronously.
        ThemePreference.appContext = applicationContext
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
        }

        val tokenProvider = AndroidTokenStorage(applicationContext)
        val httpClient = createApiClient(tokenProvider, BuildConfig.DEBUG)
        val authApi = AuthApi(httpClient)
        val albumApi = AlbumApi(httpClient)
        val mediaApi = MediaApi(httpClient, BuildConfig.DEBUG)
        val reactionApi = ReactionApi(httpClient)
        val commentApi = CommentApi(httpClient)
        val profileApi = ProfileApi(httpClient)
        val inviteApi = InviteApi(httpClient)
        val activityFeedApi = ActivityFeedApi(httpClient)
        val recapApi = RecapApi(httpClient)
        val devicePreferences = AndroidDevicePreferences(applicationContext)
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
                reactionApi = reactionApi,
                commentApi = commentApi,
                profileApi = profileApi,
                uploadQueueManager = uploadQueueManager,
                inviteApi = inviteApi,
                activityFeedApi = activityFeedApi,
                recapApi = recapApi,
                devicePreferences = devicePreferences,
                networkMonitor = networkMonitor,
            )
        }

        // Handle deep links delivered as part of the launching intent (cold start from widget).
        deliverDeepLinkFrom(intent)
        deliverSharedMediaFrom(intent)
    }

    /**
     * Re-delivers deep-link intents when the activity is brought to the front via
     * `FLAG_ACTIVITY_SINGLE_TOP` (i.e. tapping a widget while the app is already running).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deliverDeepLinkFrom(intent)
        deliverSharedMediaFrom(intent)
    }

    /**
     * Forwards photos shared into the app to [IncomingShareBus], which the shell drains
     * by asking which trip they belong to.
     *
     * Read permission on these URIs is granted to *this activity* and dies with it, so
     * the files are held as lazy readers and drained while it is alive rather than
     * copied here — staging every shared video to disk before anyone has said which trip
     * they are for would be a lot of I/O for a share that may be cancelled.
     */
    private fun deliverSharedMediaFrom(intent: Intent?) {
        if (intent == null) return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))

            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    .orEmpty()

            else -> return
        }
        if (uris.isEmpty()) return

        IncomingShareBus.offer(uris.mapNotNull { mediaFileFromUri(applicationContext, it) })

        // Consumed. Without this a configuration change re-delivers the same intent and
        // the trip picker reappears over a share that was already dealt with.
        intent.action = null
        intent.removeExtra(Intent.EXTRA_STREAM)
    }

    /**
     * Forwards a widget intent to [DeepLinkBus]. The widget builds intents two ways:
     *  - data URI like `thykra://album/<id>` / `thykra://album/<id>/media/<mediaId>` —
     *    parsed via the shared [handleDeepLink] helper.
     *  - explicit extras [WidgetDeepLinks.EXTRA_ALBUM_ID] / [WidgetDeepLinks.EXTRA_MEDIA_ID]
     *    — used as a fallback in case the data URI gets stripped somewhere.
     */
    private fun deliverDeepLinkFrom(intent: Intent?) {
        if (intent == null) return

        val uri = intent.dataString
        if (!uri.isNullOrBlank() && handleDeepLink(uri)) return

        val albumId = intent.getStringExtra(WidgetDeepLinks.EXTRA_ALBUM_ID) ?: return
        val mediaId = intent.getStringExtra(WidgetDeepLinks.EXTRA_MEDIA_ID)
        DeepLinkBus.emit(
            if (mediaId != null) DeepLinkTarget.Media(albumId, mediaId)
            else DeepLinkTarget.Trip(albumId)
        )
    }
}
