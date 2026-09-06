package com.jameeli.thykra.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.elevation
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraShadow
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * One batch: every file enqueued for one trip within 60 s of the last.
 *
 * The dock reads nothing but this. Build step 06 derives it from `UploadQueueManager`,
 * which keeps the kit free of the queue's threading and persistence concerns.
 */
@Immutable
data class UploadBatch(
    val id: String,
    val tripId: String,
    val tripTitle: String,
    val rows: List<UploadRowState>,
    val connected: Boolean = true,
    /** Shown only after 3 s and once under an hour, rounded to minutes. */
    val secondsRemaining: Int? = null,
    /** "Sorted into 3 days", from the chapters the new media landed in. */
    val celebrationDetail: String? = null,
) {
    val total: Int get() = rows.size
    val doneCount: Int get() = rows.count { it.status == UploadRowStatus.Done }
    val failedCount: Int get() = rows.count { it.status == UploadRowStatus.Failed }
    val complete: Boolean get() = rows.isNotEmpty() && doneCount == total
    val bytesUploaded: Long get() = rows.sumOf { it.bytesUploaded }
    val bytesTotal: Long get() = rows.sumOf { it.totalBytes }
    val inFlight: UploadRowState?
        get() = rows.firstOrNull { it.status == UploadRowStatus.Uploading }
            ?: rows.firstOrNull { it.status != UploadRowStatus.Done }

    /** Failed first, then active, then queued, then done — the order the design asks for. */
    val ordered: List<UploadRowState>
        get() = rows.sortedBy { row ->
            when (row.status) {
                UploadRowStatus.Failed -> 0
                UploadRowStatus.Uploading -> 1
                UploadRowStatus.Confirming -> 2
                UploadRowStatus.Queued -> 3
                UploadRowStatus.Done -> 4
            }
        }
}

/**
 * Part 2 §4.8 and part 3 §06. The mobile-only journey, and the one component the trip
 * screen is built around.
 *
 * It is hosted by the Scaffold rather than the screen, so it survives navigation, and it
 * re-mounts on cold start from the persisted queue.
 */
@Composable
fun UploadDock(
    batch: UploadBatch,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onRetryAll: () -> Unit = {},
    onSeeThem: () -> Unit = {},
    /** The trips-list variant names the trip; inside a trip it is already obvious. */
    showTripName: Boolean = false,
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current
    val shape = MaterialTheme.shapes.medium

    val container by animateColorAsState(
        targetValue = if (batch.complete) extended.successContainer else scheme.surfaceContainer,
        animationSpec = thykraTween(motion.dur2),
        label = "dockContainer",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .thykraShadow(MaterialTheme.elevation.level3, shape)
            .clip(shape)
            .background(container),
    ) {
        if (batch.complete) {
            CelebrationHeader(batch = batch, onSeeThem = onSeeThem, reduced = reduced)
        } else {
            DockHeader(
                batch = batch,
                expanded = expanded,
                onToggle = onToggle,
                onRetryAll = onRetryAll,
                showTripName = showTripName,
            )
            DockTrack(batch)
        }

        AnimatedVisibility(
            visible = expanded && !batch.complete,
            enter = fadeIn(thykraTween(motion.dur3)) + slideInVertically(thykraTween(motion.dur3)),
            exit = fadeOut(thykraTween(motion.dur3)) + slideOutVertically(thykraTween(motion.dur3)),
        ) {
            LazyColumn(
                // Capped so the dock never eats more than 40% of the window.
                modifier = Modifier.heightIn(max = 280.dp),
            ) {
                items(batch.ordered, key = { it.id }) { row ->
                    UploadRow(
                        state = row,
                        onRetry = onRetry,
                        onSkip = onSkip,
                        compact = compact,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockHeader(
    batch: UploadBatch,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRetryAll: () -> Unit,
    showTripName: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val motion = LocalMotion.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 270f else 90f,
        animationSpec = thykraTween(motion.dur2),
        label = "dockChevron",
    )

    val summary = when {
        !batch.connected -> "Waiting for a connection"
        showTripName -> "Uploading to ${batch.tripTitle}"
        else -> "Uploading ${batch.doneCount + 1} of ${batch.total}"
    }
    val detail = when {
        !batch.connected -> "${batch.total - batch.doneCount} left · they'll send on their own"
        batch.failedCount > 0 -> "${batch.failedCount} didn't send${batch.timeLeftSuffix()}"
        else -> "${formatBytes(batch.bytesUploaded)} of ${formatBytes(batch.bytesTotal)}" +
            batch.timeLeftSuffix()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$summary. $detail"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = batch.inFlight?.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .background(scheme.surfaceVariant, PlateShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = extended.textMeta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (batch.failedCount > 0) {
            ThykraButton(
                label = "Retry all",
                onClick = onRetryAll,
                variant = ThykraButtonVariant.Text,
                size = ThykraButtonSize.Compact,
            )
        }
        Icon(
            imageVector = ThykraIcons.Chevron,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = chevronRotation },
        )
    }
}

/** The 4 dp batch bar. Warning-coloured while offline, because it is paused, not broken. */
@Composable
private fun DockTrack(batch: UploadBatch) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val motion = LocalMotion.current
    val fraction by animateFloatAsState(
        targetValue = if (batch.bytesTotal > 0) {
            (batch.bytesUploaded.toFloat() / batch.bytesTotal).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = thykraTween(motion.dur2, motion.easeOut),
        label = "dockProgress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(scheme.primaryContainer),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxSize()
                .background(if (batch.connected) scheme.primary else extended.warning),
        )
    }
}

/** The first of the app's two celebrations. */
@Composable
private fun CelebrationHeader(
    batch: UploadBatch,
    onSeeThem: () -> Unit,
    reduced: Boolean,
) {
    val extended = MaterialTheme.thykra
    val motion = LocalMotion.current
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = thykraTween(motion.dur3, motion.spring),
        label = "celebrationDisc",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription =
                    "${batch.total} photos are in. ${batch.celebrationDetail.orEmpty()}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    if (!reduced) {
                        scaleX = scale
                        scaleY = scale
                    }
                }
                .background(extended.success, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ThykraIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${batch.total} photos are in.",
                style = MaterialTheme.typography.titleMedium,
                color = extended.onSuccessContainer,
            )
            batch.celebrationDetail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.onSuccessContainer,
                )
            }
        }
        ThykraButton(
            label = "See them",
            onClick = onSeeThem,
            variant = ThykraButtonVariant.Text,
            size = ThykraButtonSize.Compact,
        )
    }
}

private fun UploadBatch.timeLeftSuffix(): String {
    val seconds = secondsRemaining ?: return ""
    if (seconds >= 3600) return ""
    val minutes = (seconds / 60).coerceAtLeast(1)
    return " · about $minutes min left"
}

/** MB and GB, one decimal place, the way a phone talks about a photo library. */
fun formatBytes(bytes: Long): String {
    val mb = bytes / 1_000_000.0
    return when {
        mb >= 1000 -> "${((mb / 1000) * 10).toInt() / 10.0} GB"
        mb >= 10 -> "${mb.toInt()} MB"
        bytes <= 0 -> "0 MB"
        else -> "${(mb * 10).toInt() / 10.0} MB"
    }
}

/** The dock rides 8 dp above whichever bottom chrome is showing. */
val DockBottomGap = 8.dp

/** Slide-up entrance, so the dock arrives from behind the bar it sits above. */
@Composable
fun dockEnterTransition() = fadeIn(thykraTween(LocalMotion.current.dur3)) +
    slideInVertically(thykraTween(LocalMotion.current.dur3)) { it }
