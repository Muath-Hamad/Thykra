package com.jameeli.thykra.ui.media

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

    // playWhenReady starts false — controlled explicitly via isActive below.
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }

    // Load and prepare the media item whenever the URL changes.
    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    // Play or pause based on whether this page is the current pager page.
    // Prevents multiple off-screen players from running simultaneously.
    LaunchedEffect(isActive) {
        if (isActive) player.play() else player.pause()
    }

    // Use rememberUpdatedState so the lifecycle observer always reads the
    // latest isActive value without being recreated on every recomposition.
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
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Release the player when this composable leaves the composition.
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = modifier
    )
}
