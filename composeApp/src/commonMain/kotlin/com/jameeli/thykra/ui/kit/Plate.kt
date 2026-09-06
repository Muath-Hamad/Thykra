package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.11. A photograph, at 0 dp radius and its own true aspect, with a 1 dp
 * hairline drawn over it.
 *
 * The hairline is an alpha rather than a flat hex because it sits on a photograph and has
 * to hold on both a white sky and a night sky.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Plate(
    media: MediaDto,
    modifier: Modifier = Modifier,
    /** Overrides the media's own aspect — the masthead runs 16:9, the sheet runs 1:1. */
    aspectRatio: Float? = null,
    contentScale: ContentScale = ContentScale.Crop,
    selected: Boolean = false,
    /** "Photo by Sara, Saturday 12 April" — never the filename. */
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val ratio = aspectRatio ?: media.aspectRatio()

    Box(
        modifier = modifier
            .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier)
            .background(scheme.surfaceVariant, PlateShape)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                        role = Role.Button,
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            },
    ) {
        SubcomposeAsyncImage(
            model = media.thumbnailUrl ?: media.url,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ThykraIcons.Alert,
                        contentDescription = null,
                        tint = extended.textMeta,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        if (media.type == MediaType.VIDEO) {
            VideoBadge(
                durationMs = media.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }

        // Drawn over the photograph, last, so it is never covered by the image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, extended.plateOutline, PlateShape),
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, scheme.primary, PlateShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(scheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ThykraIcons.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        overlay?.invoke(this)
    }
}

/**
 * Play plus the duration, on a scrim pill. The pill sits at the bottom-start of the plate
 * in both directions — it labels the photograph, not the layout, so it does not mirror.
 */
@Composable
fun VideoBadge(
    durationMs: Long?,
    modifier: Modifier = Modifier,
) {
    val extended = MaterialTheme.thykra
    Row(
        modifier = modifier
            .background(
                extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ThykraIcons.Play,
            contentDescription = null,
            tint = extended.onScrim,
            modifier = Modifier.size(12.dp),
        )
        if (durationMs != null) {
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = extended.onScrim,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** A plate with nothing behind it yet — the cover fallback and the loading state. */
@Composable
fun EmptyPlate(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
) {
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .background(MaterialTheme.colorScheme.surfaceVariant, PlateShape)
            .border(1.dp, MaterialTheme.thykra.plateOutline, PlateShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ThykraIcons.Trips,
            contentDescription = null,
            tint = MaterialTheme.thykra.textMeta,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** True aspect from the media's own dimensions; null when the server never sent them. */
fun MediaDto.aspectRatio(): Float? {
    val w = width ?: return null
    val h = height ?: return null
    if (w <= 0 || h <= 0) return null
    return w.toFloat() / h.toFloat()
}

/** MM:SS, and H:MM:SS once a video runs past an hour. */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/** Coil needs a plain image in a few places where the plate's chrome would be noise. */
@Composable
internal fun PlainPlateImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, PlateShape)
            .border(1.dp, MaterialTheme.thykra.plateOutline, PlateShape),
    )
}
