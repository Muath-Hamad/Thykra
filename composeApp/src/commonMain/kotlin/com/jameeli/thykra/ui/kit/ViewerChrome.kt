package com.jameeli.thykra.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.ReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * Part 2 §4.11 and part 3 §07. The pills, gradients and filmstrip that float over a
 * photograph.
 *
 * Every pill sits on a scrim at 55% with a 12 dp gradient band behind its edge of the
 * screen, so it holds on a white sky as well as a night one. The chrome is always drawn
 * in Darkroom ink, because the viewer wraps itself in Darkroom regardless of the
 * preference.
 */
@Composable
fun ViewerChrome(
    /** "14 / 31 · Sat 12 Apr" — the index within the chapter, not the whole trip. */
    positionLabel: String,
    reactions: List<ReactionSummaryDto>,
    commentCount: Int,
    visible: Boolean,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onMore: () -> Unit,
    onToggleReaction: (ReactionType) -> Unit,
    onOpenPicker: () -> Unit,
    onComments: () -> Unit,
    modifier: Modifier = Modifier,
    /** "Sara Nasser · 4:12 pm". */
    attribution: String? = null,
    filmstrip: List<MediaDto> = emptyList(),
    currentIndex: Int = 0,
    onSelectIndex: (Int) -> Unit = {},
    /** Landscape moves the bottom cluster to the end edge and hides the filmstrip. */
    landscape: Boolean = false,
    filmstripState: LazyListState = rememberLazyListState(),
) {
    val motion = LocalMotion.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(thykraTween(motion.dur2)),
        exit = fadeOut(thykraTween(motion.dur2)),
    ) {
        Box(Modifier.fillMaxSize()) {
            TopBand(Modifier.align(Alignment.TopCenter))

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            androidx.compose.foundation.layout.WindowInsetsSides.Top +
                                androidx.compose.foundation.layout.WindowInsetsSides.Horizontal,
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChromePill(icon = ThykraIcons.Back, contentDescription = "Back", onClick = onBack)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ChromeLabel(positionLabel)
                }
                ChromePill(icon = ThykraIcons.Info, contentDescription = "Photo info", onClick = onInfo)
                ChromePill(icon = ThykraIcons.More, contentDescription = "More options", onClick = onMore)
            }

            if (landscape) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                androidx.compose.foundation.layout.WindowInsetsSides.Horizontal,
                            ),
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    ReactionBar(
                        reactions = reactions,
                        onToggle = onToggleReaction,
                        onOpenPicker = onOpenPicker,
                        overMedia = true,
                    )
                    CommentsPill(commentCount, onComments)
                }
            } else {
                BottomBand(Modifier.align(Alignment.BottomCenter))
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                androidx.compose.foundation.layout.WindowInsetsSides.Bottom +
                                    androidx.compose.foundation.layout.WindowInsetsSides.Horizontal,
                            ),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ReactionBar(
                            reactions = reactions,
                            onToggle = onToggleReaction,
                            onOpenPicker = onOpenPicker,
                            overMedia = true,
                        )
                        CommentsPill(commentCount, onComments)
                    }
                    attribution?.let { ChromeLabel(it) }
                    if (filmstrip.isNotEmpty()) {
                        Filmstrip(
                            media = filmstrip,
                            currentIndex = currentIndex,
                            onSelect = onSelectIndex,
                            state = filmstripState,
                        )
                    }
                }
            }
        }
    }
}

/** A 12 dp gradient band so a pill on a white sky still has an edge to sit against. */
@Composable
private fun TopBand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

@Composable
private fun BottomBand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.5f),
                ),
            ),
    )
}

@Composable
private fun ChromePill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val extended = MaterialTheme.thykra
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                RoundedCornerShape(12.dp),
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = extended.onScrim,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ChromeLabel(text: String) {
    val extended = MaterialTheme.thykra
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = extended.onScrim,
        modifier = Modifier
            .background(
                extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun CommentsPill(count: Int, onClick: () -> Unit) {
    val extended = MaterialTheme.thykra
    Row(
        modifier = Modifier
            .height(36.dp)
            .background(
                extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                RoundedCornerShape(10.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics {
                contentDescription = if (count == 1) "1 comment" else "$count comments"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = ThykraIcons.Comment,
            contentDescription = null,
            tint = extended.onScrim,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = extended.onScrim,
        )
    }
}

/**
 * 44 dp tall, 30 x 40 thumbs, and the current one grown to 34 x 44 with a bone stroke.
 * One TalkBack node, because thirty-one separate images is not navigation.
 */
@Composable
private fun Filmstrip(
    media: List<MediaDto>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    state: LazyListState,
) {
    val extended = MaterialTheme.thykra
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "Filmstrip, ${currentIndex + 1} of ${media.size}; swipe to move"
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(media, key = { _, item -> item.id }) { index, item ->
            val current = index == currentIndex
            Box(
                modifier = Modifier
                    .size(
                        width = if (current) 34.dp else 30.dp,
                        height = if (current) 44.dp else 40.dp,
                    )
                    .clickable { onSelect(index) },
            ) {
                AsyncImage(
                    model = item.thumbnailUrl ?: item.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, PlateShape),
                )
                if (current) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .border(2.dp, extended.onScrim, PlateShape),
                    )
                }
            }
        }
    }
}

/** Lets a caller drop extra chrome (a video scrub bar) into the same stack. */
@Composable
fun ViewerChromeSlot(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), content = content)
}
