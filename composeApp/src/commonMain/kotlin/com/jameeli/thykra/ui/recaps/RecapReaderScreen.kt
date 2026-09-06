package com.jameeli.thykra.ui.recaps

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.model.PublicMediaDto
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.share.shareText
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.ThykraTheme
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraTween
import kotlinx.coroutines.delay

/** Four seconds a frame, and at most eight progress segments however many there are. */
private const val FrameMs = 4_000L
private const val MaxSegments = 8

/**
 * Design part 3 §10. The story reader.
 *
 * Always Darkroom and always full screen. Under reduced motion it stops auto-advancing
 * entirely and the segments become a plain count — a story that moves on its own is
 * exactly the kind of motion someone turns off.
 */
@Composable
fun RecapReaderScreen(
    shareToken: String,
    viewModel: RecapReaderViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThykraTheme(forceDark = true) {
        val view by viewModel.view.collectAsState()
        val error by viewModel.error.collectAsState()
        val reduced = LocalReducedMotion.current
        val motion = LocalMotion.current

        var index by remember { mutableStateOf(0) }
        var paused by remember { mutableStateOf(false) }

        LaunchedEffect(shareToken) { viewModel.load(shareToken) }

        val media = view?.media.orEmpty()

        // Auto-advance, unless someone is holding or has asked for less motion.
        LaunchedEffect(index, paused, media.size, reduced) {
            if (reduced || paused || media.isEmpty()) return@LaunchedEffect
            delay(FrameMs)
            if (index < media.lastIndex) index++ else onClose()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            when {
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        headline = clayPhrase("This recap ", "isn't available."),
                        body = error.orEmpty(),
                        glyph = EmptyGlyph.Plate,
                    )
                }

                media.isEmpty() -> Box(Modifier.fillMaxSize())

                else -> {
                    val current = media[index]

                    Crossfade(
                        targetState = current.id,
                        animationSpec = thykraTween(if (reduced) motion.dur1 else motion.dur3),
                        label = "recapFrame",
                        modifier = Modifier.fillMaxSize(),
                    ) { id ->
                        val frame = media.firstOrNull { it.id == id } ?: current
                        AsyncImage(
                            model = frame.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Tap the end half to go on, the start half to go back, hold to pause.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(media.size) {
                                detectTapGestures(
                                    onPress = {
                                        paused = true
                                        tryAwaitRelease()
                                        paused = false
                                    },
                                    onTap = { offset ->
                                        if (offset.x > size.width / 2) {
                                            if (index < media.lastIndex) index++ else onClose()
                                        } else if (index > 0) {
                                            index--
                                        }
                                    },
                                )
                            },
                    )

                    ReaderChrome(
                        index = index,
                        total = media.size,
                        reduced = reduced,
                        title = view?.recap?.title.orEmpty(),
                        caption = current.caption(view?.ownerDisplayName),
                        firstOrLast = index == 0 || index == media.lastIndex,
                        onClose = onClose,
                        onShare = { shareText("https://thykra.com/r/$shareToken") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderChrome(
    index: Int,
    total: Int,
    reduced: Boolean,
    title: String,
    caption: String,
    firstOrLast: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
) {
    val extended = MaterialTheme.thykra

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (reduced) {
            // No segments to fill when nothing advances on its own.
            Text(
                text = "${index + 1} of $total",
                style = MaterialTheme.typography.labelMedium,
                color = extended.onScrim,
            )
        } else {
            ProgressSegments(index = index, total = total)
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ThykraIcons.Close,
                    contentDescription = "Close",
                    tint = extended.onScrim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(role = Role.Button, onClick = onShare),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ThykraIcons.Share,
                    contentDescription = "Share recap",
                    tint = extended.onScrim,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                )
                .padding(12.dp),
        ) {
            // The title only appears on the first and last frames; in between the
            // photographs carry it.
            if (firstOrLast && title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    color = extended.onScrim,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = extended.onScrim,
            )
        }
    }
}

/** Eight at most; beyond that the frames are grouped so the bar stays readable. */
@Composable
private fun ProgressSegments(index: Int, total: Int) {
    val extended = MaterialTheme.thykra
    val segments = minOf(total, MaxSegments)
    val filledThrough = if (total <= MaxSegments) {
        index
    } else {
        (index.toFloat() / total * segments).toInt()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Photo ${index + 1} of $total" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        color = if (i <= filledThrough) {
                            extended.onScrim
                        } else {
                            extended.onScrim.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

private fun PublicMediaDto.caption(ownerName: String?): String {
    val who = ownerName?.let { "Photo by $it" }
    return who ?: "Photo"
}
