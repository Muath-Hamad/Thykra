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

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }

    // Holds a reference to the PlayerView so onDispose can detach the surface
    // before releasing the player (see explanation below).
    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }

    // Prepare and play only when this page is the visible one.
    // Never calling prepare() for off-screen pages means no MediaCodec
    // initialisation for pages the user hasn't visited.
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
            // Detach the surface BEFORE releasing the player.
            // PlayerView.surfaceDestroyed calls player.setVideoOutputInternal via
            // blockUntilDelivered, which needs the playback looper to be alive.
            // If release() runs first (looper.quit()), that call crashes.
            // Setting player = null removes ExoPlayer's SurfaceView listener so
            // surfaceDestroyed becomes a no-op when Compose removes the view.
            playerViewRef[0]?.player = null
            player.stop()
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).also { playerViewRef[0] = it }
        },
        update = { view ->
            // Attach the surface while active; detach while inactive.
            // The update lambda runs synchronously during the Compose layout phase,
            // before the HorizontalPager's SubcomposeLayout.deactivateOutOfFrame
            // can remove the view and trigger surfaceDestroyed. Without this,
            // deactivateOutOfFrame → AndroidViewHolder.onDeactivate → surfaceDestroyed
            // → blockUntilDelivered crashes because blockUntilDelivered asserts it
            // is not called from the playback looper thread.
            view.player = if (isActive) player else null
        },
        modifier = modifier
    )
}
