package com.jameeli.thykra.ui.media

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(url: String, isActive: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // setPlaybackLooper(main looper): ExoPlayer's state machine runs on the main thread.
    // MediaCodec's EventHandler uses the same looper, so its native callbacks always
    // have a live thread to post to — eliminates "Handler on a dead thread" crashes.
    // Actual audio/video decoding still runs on ExoPlayer's internal codec threads.
    val player = remember {
        ExoPlayer.Builder(context)
            .setPlaybackLooper(Looper.getMainLooper())
            .build()
            .apply { playWhenReady = false }
    }

    // Prepare and play only when this page is the visible one.
    // Calling prepare() for off-screen pages initialises MediaCodec unnecessarily
    // and widens the window for the dead-thread race during disposal.
    LaunchedEffect(url, isActive) {
        if (isActive) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()
        } else {
            player.pause()
        }
    }

    val currentIsActive = rememberUpdatedState(isActive)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (currentIsActive.value) player.play()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.stop()
            player.release()
        }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = modifier
    )
}
