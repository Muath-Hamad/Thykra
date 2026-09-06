package com.jameeli.thykra.ui.kit

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.PlateShape

private const val SweepPeriodMs = 1400

/**
 * Part 2 §4.7. A shape holding a place, with one sweep of light travelling start to end.
 *
 * Skeletons copy the real layout, so the swap to content moves nothing. They appear only
 * after 200 ms of loading — never a flash — and cross-fade out. Spinners exist only inside
 * buttons and the dock.
 *
 * Under reduced motion the sweep becomes an alpha pulse on the same 1400 ms clock.
 */
@Composable
fun Modifier.skeleton(shape: Shape = PlateShape): Modifier {
    val scheme = MaterialTheme.colorScheme
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SweepPeriodMs, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonSweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(SweepPeriodMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonPulse",
    )

    return this
        .clip(shape)
        .background(scheme.surfaceVariant)
        .semantics { invisibleToUser() }
        .drawWithContent {
            drawContent()
            if (reduced) {
                drawRect(scheme.surfaceVariant.copy(alpha = 1f - pulse))
                return@drawWithContent
            }
            val bandWidth = size.width * 0.4f
            val travel = (size.width + bandWidth) * progress - bandWidth
            val start = if (rtl) size.width - travel - bandWidth else travel
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to scheme.surfaceVariant.copy(alpha = 0f),
                    0.5f to scheme.surface.copy(alpha = 0.6f),
                    1f to scheme.surfaceVariant.copy(alpha = 0f),
                    startX = start,
                    endX = start + bandWidth,
                ),
                topLeft = Offset(start, 0f),
                size = Size(bandWidth, size.height),
            )
        }
}

/** A skeleton shaped like a [TripCard], so the swap to the real one moves nothing. */
@Composable
fun TripCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .skeleton(),
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .skeleton(MaterialTheme.shapes.extraSmall),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .skeleton(MaterialTheme.shapes.extraSmall),
                )
            }
    }
}

/**
 * A skeleton shaped like a chapter: the numeral block, the 2 dp rule, the lead plate and
 * a two-column grid.
 */
@Composable
fun ChapterSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 54.dp, height = 40.dp)
                    .skeleton(MaterialTheme.shapes.extraSmall),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(width = 120.dp, height = 18.dp)
                        .skeleton(MaterialTheme.shapes.extraSmall),
                )
                Box(
                    Modifier
                        .size(width = 90.dp, height = 12.dp)
                        .skeleton(MaterialTheme.shapes.extraSmall),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onSurface),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .skeleton(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(2) {
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .skeleton(),
                )
            }
        }
    }
}
