package com.jameeli.thykra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import coil3.compose.AsyncImage
import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.ThykraIcons

@Composable
fun MediaViewerScreenContent(
    albumId: String,
    initialMediaId: String,
    viewModel: MediaViewerViewModel,
    onNavigateBack: () -> Unit
) {
    val media by viewModel.media.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.loadMedia(albumId)
    }

    val initialPage = remember(media, initialMediaId) {
        media.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = 0) { media.size }
    LaunchedEffect(initialPage) {
        if (media.isNotEmpty()) pagerState.scrollToPage(initialPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThykraColors.DeepNavy)
    ) {
        if (media.isEmpty()) {
            Text(
                "Loading...",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = media[page]
                val resolvedUrl = item.url.replace("http://localhost:8081", API_BASE_URL)
                if (item.type == MediaType.VIDEO) {
                    VideoPlayer(
                        url = resolvedUrl,
                        isActive = page == pagerState.currentPage,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ZoomableAsyncImage(
                        url = resolvedUrl,
                        contentDescription = item.filename,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Gradient scrim — tall enough to cover the status bar area + the row below it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Compact floating overlay.
        // safeDrawing.only(Top + Horizontal) covers status bar, display cutout, and
        // side nav bar (3-button nav moves to the right in landscape).
        // In landscape the screen is much shorter, so drop the extra vertical padding
        // so the buttons sit as close to the inset boundary as possible.
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(horizontal = 4.dp, vertical = if (isLandscape) 0.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = ThykraIcons.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            if (media.isNotEmpty()) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${media.size}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun ZoomableAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        // Only consume events during pinch-zoom or pan-while-zoomed.
                        // Single-finger swipes at 1x are left unconsumed so the pager handles them.
                        if (zoomChange != 1f || scale > 1f) {
                            event.changes.forEach { it.consume() }
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            offset = if (scale <= 1f) Offset.Zero else offset + panChange
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    )
}
