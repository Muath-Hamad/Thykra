package com.jameeli.thykra.ui.kit

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * What the dock draws, per file.
 *
 * This is a UI model rather than `UploadState` from the shared queue: the dock needs
 * bytes and a thumbnail, the queue does not yet carry either, and the kit must not depend
 * on a screen or a view model to get them. Build step 06 maps the queue onto this.
 */
@Immutable
data class UploadRowState(
    val id: String,
    val filename: String,
    val status: UploadRowStatus,
    val thumbnailUrl: String? = null,
    val isVideo: Boolean = false,
    val bytesUploaded: Long = 0,
    val totalBytes: Long = 0,
    /** "Too large (2.1 GB)" — a local failure that offers Skip rather than Retry. */
    val failureReason: String? = null,
    val retryable: Boolean = true,
) {
    val fraction: Float
        get() = if (totalBytes > 0) (bytesUploaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

enum class UploadRowStatus { Queued, Uploading, Confirming, Done, Failed }

/**
 * Part 2 §4.8. One file in flight.
 *
 * Failed rows go full-bleed on the error container and move to the top of the list, so
 * the thing that needs a decision is the thing under the thumb.
 */
@Composable
fun UploadRow(
    state: UploadRowState,
    modifier: Modifier = Modifier,
    onRetry: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    /** Below 380 dp a failed row stacks the filename over the status instead. */
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val failed = state.status == UploadRowStatus.Failed
    val done = state.status == UploadRowStatus.Done

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (failed) Modifier.background(scheme.errorContainer) else Modifier)
            .padding(horizontal = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = state.announcement()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.size(36.dp)) {
            AsyncImage(
                model = state.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.surfaceVariant, PlateShape),
            )
            if (state.isVideo) {
                Icon(
                    imageVector = ThykraIcons.Video,
                    contentDescription = null,
                    tint = extended.onScrim,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                        .size(12.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (failed && compact) {
                Text(
                    text = state.filename,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done) extended.textMeta else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusLabel(state)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.filename,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (done) extended.textMeta else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StatusLabel(state)
                }
            }
            UploadTrack(state)
        }

        if (failed) {
            IconButton(
                onClick = {
                    if (state.retryable) onRetry(state.id) else onSkip(state.id)
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (state.retryable) ThykraIcons.Retry else ThykraIcons.Close,
                    contentDescription = if (state.retryable) "Retry" else "Skip",
                    tint = scheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(state: UploadRowState) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val (text, color) = when (state.status) {
        UploadRowStatus.Queued -> "Queued" to extended.textMeta
        UploadRowStatus.Uploading -> "${(state.fraction * 100).toInt()}%" to extended.textMeta
        UploadRowStatus.Confirming -> "Confirming" to extended.warning
        UploadRowStatus.Done -> "Done" to extended.success
        UploadRowStatus.Failed -> (state.failureReason ?: "Didn't send") to scheme.onErrorContainer
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (state.status) {
            UploadRowStatus.Done -> Icon(
                imageVector = ThykraIcons.Check,
                contentDescription = null,
                tint = extended.success,
                modifier = Modifier.size(14.dp),
            )

            UploadRowStatus.Failed -> Icon(
                imageVector = ThykraIcons.Alert,
                contentDescription = null,
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(14.dp),
            )

            else -> Unit
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/**
 * A 3 dp track. Determinate from bytes while uploading; a 35%-wide sweep while
 * confirming, which under reduced motion becomes a static 35% bar pulsing on the same
 * clock.
 */
@Composable
private fun UploadTrack(state: UploadRowState) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current

    val fraction by animateFloatAsState(
        targetValue = state.fraction,
        animationSpec = thykraTween(motion.dur2, motion.easeOut),
        label = "uploadFraction",
    )
    val transition = rememberInfiniteTransition(label = "confirming")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confirmingSweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
        label = "confirmingPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(scheme.primaryContainer),
    ) {
        when (state.status) {
            UploadRowStatus.Confirming -> {
                val bandFraction = 0.35f
                val offset = if (reduced) 0.325f else (sweep * (1f + bandFraction)) - bandFraction
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bandFraction)
                        .height(3.dp)
                        .offsetFraction(offset)
                        .background(
                            MaterialTheme.thykra.warning
                                .copy(alpha = if (reduced) pulse * 0.5f else 1f),
                        ),
                )
            }

            UploadRowStatus.Queued -> Unit

            UploadRowStatus.Done -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.thykra.success),
            )

            UploadRowStatus.Failed -> Box(
                Modifier
                    .fillMaxWidth(state.fraction)
                    .height(3.dp)
                    .background(scheme.error),
            )

            UploadRowStatus.Uploading -> Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(scheme.primary),
            )
        }
    }
}

/** Shifts a child by a fraction of the parent's width, mirroring in RTL for free. */
private fun Modifier.offsetFraction(fraction: Float): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative((constraints.maxWidth * fraction).toInt(), 0)
            }
        },
    )

private fun UploadRowState.announcement(): String = when (status) {
    UploadRowStatus.Queued -> "$filename, queued"
    UploadRowStatus.Uploading -> "$filename, ${(fraction * 100).toInt()} percent"
    UploadRowStatus.Confirming -> "$filename, confirming"
    UploadRowStatus.Done -> "$filename, done"
    UploadRowStatus.Failed ->
        "$filename, ${failureReason ?: "didn't send"}, ${if (retryable) "Retry" else "Skip"} button"
}
