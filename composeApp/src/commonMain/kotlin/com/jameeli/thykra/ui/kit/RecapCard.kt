package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.darkroomExtended
import com.jameeli.thykra.ui.theme.darkroomScheme
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.3. Always dark, in both themes — it previews the reader, and the reader is
 * always Darkroom.
 *
 * The play pill is the same action as the card, so it is hidden from TalkBack rather than
 * offered as a second target for the same thing.
 */
@Composable
fun RecapCard(
    title: String,
    /** "Recap · 18 photos". */
    eyebrow: String,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val inDarkroom = MaterialTheme.colorScheme.surface == darkroomScheme.surface
    val container = if (inDarkroom) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.inverseSurface
    }
    val shape = MaterialTheme.shapes.medium
    // The card is dark whatever the app theme is, so its ink comes from Darkroom.
    val onCover = darkroomExtended.onScrim
    val eyebrowColor = darkroomScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "Recap, $title, $eyebrow"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        ) {
            if (coverUrl.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, PlateShape),
                )
            } else {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Ink from nothing to 92% over the bottom 40%, so the title always holds.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.60f to Color.Transparent,
                            1f to Color(0xFF121114).copy(alpha = 0.92f),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(36.dp)
                    .background(
                        MaterialTheme.thykra.scrimStrong.copy(alpha = ScrimPillAlpha),
                        CircleShape,
                    )
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ThykraIcons.Play,
                    contentDescription = null,
                    tint = onCover,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = eyebrowColor,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = onCover,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
