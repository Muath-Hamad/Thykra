package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.elevation
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraShadow

/**
 * Part 2 §4.3. One trip, one row at 393 dp, two-up from 600.
 *
 * The whole card is the tap target — there are no inner ones — so TalkBack hears one
 * node: the title, the counts, the last thing that happened, then "button".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripCard(
    album: AlbumDto,
    modifier: Modifier = Modifier,
    /** "84 photos · 6 people · Apr 2026", assembled by the screen from the DTO. */
    meta: String,
    /** The newest activity line for this trip. Hidden when there is none. */
    lastActivity: String? = null,
    /** Omitted until the server sends `videoCount` on AlbumDto. */
    videoCount: Int? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val elevation = MaterialTheme.elevation
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val members = album.previewMembers.map { it.toAvatarUser() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .thykraShadow(if (pressed) elevation.level2 else elevation.level1, shape)
            .clip(shape)
            .background(scheme.surfaceContainer)
            .then(
                if (elevation.hairlineInsteadOfShadow) {
                    Modifier.hairline(shape)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(album.title, meta, lastActivity)
                    .joinToString("; ")
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            if (album.coverUrl.isNullOrBlank()) {
                EmptyPlate(modifier = Modifier.fillMaxWidth())
            } else {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(scheme.surfaceVariant, PlateShape),
                )
            }
            if (videoCount != null && videoCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                            MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = ThykraIcons.Video,
                        contentDescription = null,
                        tint = extended.onScrim,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = videoCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.onScrim,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AvatarStack(
                    users = members,
                    max = 3,
                    size = AvatarSize.Xs,
                    ringColor = scheme.surfaceContainer,
                    totalCount = album.memberCount,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (lastActivity != null) {
                Text(
                    text = lastActivity,
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Darkroom's substitute for a shadow: a 1 dp outlineVariant edge. */
@Composable
internal fun Modifier.hairline(shape: Shape): Modifier =
    this.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
