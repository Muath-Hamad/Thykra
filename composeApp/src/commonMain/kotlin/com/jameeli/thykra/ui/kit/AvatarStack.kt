package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Part 2 §4.2. Faces, overlapping, with a ring in the parent's own container colour so
 * they separate on any background.
 *
 * The whole stack is one TalkBack node — "6 people: Sara, Omar, Lina and 3 more" — rather
 * than a run of unnamed images. In RTL the overlap runs from the end, which
 * [Arrangement.spacedBy] with a negative spacing gives for free.
 */
@Composable
fun AvatarStack(
    users: List<AvatarUser>,
    modifier: Modifier = Modifier,
    max: Int = 3,
    size: AvatarSize = AvatarSize.Sm,
    /** Pass the parent's container colour so the rings punch out of it. */
    ringColor: Color = MaterialTheme.colorScheme.surface,
    /** When larger than `users.size`, the overflow pill counts the ones not drawn. */
    totalCount: Int = users.size,
) {
    if (users.isEmpty()) return

    val shown = users.take(max)
    val overflow = (totalCount - shown.size).coerceAtLeast(0)
    val ringWidth = 2.dp

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = stackDescription(totalCount, shown, overflow)
        },
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { user ->
            Box(
                modifier = Modifier
                    .size(size.diameter.dp + ringWidth * 2)
                    .background(ringColor, CircleShape)
                    .border(ringWidth, ringColor, CircleShape)
                    .padding(ringWidth),
                contentAlignment = Alignment.Center,
            ) {
                Avatar(user = user, size = size)
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(size.diameter.dp + ringWidth * 2)
                    .background(ringColor, CircleShape)
                    .padding(ringWidth)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private fun stackDescription(
    totalCount: Int,
    shown: List<AvatarUser>,
    overflow: Int,
): String {
    val names = shown.joinToString(", ") { it.displayName.substringBefore(' ') }
    val people = if (totalCount == 1) "1 person" else "$totalCount people"
    return if (overflow > 0) "$people: $names and $overflow more" else "$people: $names"
}
