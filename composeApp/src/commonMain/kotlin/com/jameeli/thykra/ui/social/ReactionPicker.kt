package com.jameeli.thykra.ui.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jameeli.thykra.model.MediaReactionsDto
import com.jameeli.thykra.model.ReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.theme.thykra

/**
 * A horizontally laid-out reaction picker overlay. Shows all 8 reaction types with
 * a circular pill highlight on the user's current reaction, plus a small count badge
 * if other people have reacted with the same type.
 *
 * Designed to sit above [com.jameeli.thykra.ui.media.MediaViewerScreenContent] (or any
 * media surface) — uses translucent background so it works against arbitrary content.
 */
@Composable
fun ReactionPicker(
    reactions: MediaReactionsDto?,
    onToggle: (ReactionType) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ReactionEmoji.ordered.forEach { type ->
                val summary = reactions?.summary?.firstOrNull { it.type == type }
                ReactionPickerItem(
                    type = type,
                    summary = summary,
                    onClick = { onToggle(type) }
                )
            }
        }
    }
}

@Composable
private fun ReactionPickerItem(
    type: ReactionType,
    summary: ReactionSummaryDto?,
    onClick: () -> Unit
) {
    val reactedByMe = summary?.reactedByMe == true
    val count = summary?.count ?: 0

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (reactedByMe) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                else Modifier
            )
            .then(
                if (reactedByMe) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = ReactionEmoji.glyph(type),
                fontSize = 22.sp
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Compact one-line summary of total reactions, suitable for showing inline below the media.
 * Displays up to four most-used reaction emojis followed by the total count.
 */
@Composable
fun ReactionSummaryRow(
    reactions: MediaReactionsDto?,
    modifier: Modifier = Modifier
) {
    val summaries = reactions?.summary?.filter { it.count > 0 }?.sortedByDescending { it.count }.orEmpty()
    if (summaries.isEmpty()) return
    val totalCount = summaries.sumOf { it.count }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        summaries.take(4).forEach { s ->
            Text(text = ReactionEmoji.glyph(s.type), fontSize = 14.sp)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = totalCount.toString(),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
