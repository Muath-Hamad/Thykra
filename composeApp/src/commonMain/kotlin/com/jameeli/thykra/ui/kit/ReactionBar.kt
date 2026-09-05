package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jameeli.thykra.model.ReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.social.ReactionEmoji
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.9.
 *
 * Only types with a count above zero are drawn, sorted by count and then by the enum's
 * own order so the bar does not reshuffle on every tick. Yours is marked in clay with a
 * 1 dp stroke — clay because it is you, and a stroke rather than a fill so the emoji
 * still reads on a photograph.
 */
@Composable
fun ReactionBar(
    reactions: List<ReactionSummaryDto>,
    onToggle: (ReactionType) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
    /** Over a photograph the chips become scrim pills instead of surface ones. */
    overMedia: Boolean = false,
) {
    val extended = MaterialTheme.thykra
    val visible = reactions
        .filter { it.count > 0 }
        .sortedWith(compareByDescending<ReactionSummaryDto> { it.count }.thenBy { it.type.ordinal })

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEach { summary ->
            ReactionChip(
                summary = summary,
                overMedia = overMedia,
                onClick = { onToggle(summary.type) },
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (overMedia) {
                        Modifier.background(
                            extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                            RoundedCornerShape(10.dp),
                        )
                    } else {
                        Modifier.background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(10.dp),
                        )
                    },
                )
                .clickable(role = Role.Button, onClick = onOpenPicker),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ThykraIcons.React,
                contentDescription = "React",
                tint = if (overMedia) {
                    extended.onScrim
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReactionChip(
    summary: ReactionSummaryDto,
    overMedia: Boolean,
    onClick: () -> Unit,
) {
    val (container, content, stroke) = reactionChipColors(summary.reactedByMe, overMedia)
    val shape = RoundedCornerShape(10.dp)
    val name = ReactionEmoji.label(summary.type)

    Row(
        modifier = Modifier
            .height(36.dp)
            .background(container, shape)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, shape) else Modifier)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics {
                contentDescription = if (summary.reactedByMe) {
                    "$name, ${summary.count}, you reacted"
                } else {
                    "$name, ${summary.count}"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // The system emoji font, at 18 sp. No bundled sheet.
        Text(text = ReactionEmoji.glyph(summary.type), fontSize = 18.sp)
        Text(
            text = summary.count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}
