package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = modifier
    )
}
