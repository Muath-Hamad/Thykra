package com.jameeli.thykra.ui.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(url: String, isActive: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }

    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }

    // Track playback state to drive the loader. ExoPlayer calls listeners on the
    // main thread, so updating Compose state here is safe.
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { playbackState = state }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

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
            playerViewRef[0]?.player = null
            player.stop()
            player.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also { playerViewRef[0] = it }
            },
            update = { view ->
                view.player = if (isActive) player else null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show a spinner while buffering or preparing. Hidden once STATE_READY.
        // Only shown when this page is active — off-screen pages are paused and
        // their surface is detached, so showing a loader there makes no sense.
        if (isActive && playbackState != Player.STATE_READY && playbackState != Player.STATE_ENDED) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}
