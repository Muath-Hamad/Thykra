package com.jameeli.thykra.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jameeli.thykra.ui.theme.ThykraIcons
import kotlinx.coroutines.delay

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun VideoPlayer(url: String, isActive: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }
    val playerViewRef = remember { arrayOfNulls<PlayerView>(1) }

    // Playback state tracked via listener so the UI reacts to ExoPlayer events.
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Controls visibility: shown on entry, auto-hides after 3 s.
    var showControls by remember { mutableStateOf(true) }
    // True while the user is dragging the seek bar.
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    val d = player.duration
                    durationMs = if (d != C.TIME_UNSET) d else 0L
                }
                if (state == Player.STATE_ENDED) showControls = true
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Poll current position every 200 ms while playing.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = player.currentPosition
            delay(200)
        }
    }

    // Auto-hide controls 3 s after they become visible (unless user is seeking).
    LaunchedEffect(showControls, isSeeking) {
        if (showControls && !isSeeking) {
            delay(3_000)
            showControls = false
        }
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

    val isLoading = isActive &&
        playbackState != Player.STATE_READY &&
        playbackState != Player.STATE_ENDED

    // Fraction in [0,1] driving the seek slider.
    val sliderFraction = when {
        isSeeking -> seekFraction
        durationMs > 0L -> (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        // Video surface — surface attached only while isActive to avoid
        // blockUntilDelivered crash when the pager deactivates off-screen pages.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply { useController = false }
                    .also { playerViewRef[0] = it }
            },
            update = { view -> view.player = if (isActive) player else null },
            modifier = Modifier.fillMaxSize()
        )

        // Loading spinner — shown while buffering, replaces the center button.
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        // Controls overlay — fades in/out on tap, auto-hides after 3 s.
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Bottom gradient for legibility.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Centre play / pause / replay button (hidden while loading).
                if (!isLoading) {
                    val centerIcon = when {
                        playbackState == Player.STATE_ENDED -> ThykraIcons.Replay
                        isPlaying -> ThykraIcons.Pause
                        else -> ThykraIcons.PlayArrow
                    }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = true // reset auto-hide timer
                                if (playbackState == Player.STATE_ENDED) {
                                    player.seekTo(0)
                                    player.play()
                                } else if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = centerIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom controls: seek bar + time labels.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Slider(
                        value = sliderFraction,
                        onValueChange = { v ->
                            isSeeking = true
                            seekFraction = v
                        },
                        onValueChangeFinished = {
                            val target = (seekFraction * durationMs).toLong()
                            player.seekTo(target)
                            currentPositionMs = target
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(if (isSeeking) (seekFraction * durationMs).toLong() else currentPositionMs),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatMs(durationMs),
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
