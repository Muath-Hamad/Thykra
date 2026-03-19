package com.jameeli.thykra.ui.media

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }

    DisposableEffect(url) {
        val mc = MediaController(context)
        mc.setAnchorView(videoView)
        videoView.setMediaController(mc)
        videoView.setVideoURI(Uri.parse(url))
        videoView.requestFocus()
        videoView.start()
        onDispose { videoView.stopPlayback() }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier
    )
}
