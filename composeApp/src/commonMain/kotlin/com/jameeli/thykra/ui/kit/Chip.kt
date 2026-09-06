package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/** Part 2 §4.6. Read-only badges: a role, a state, never a tap target. */
enum class BadgeTone {
    /** Owner, Contributor — about a person, so clay. */
    People,

    /** A system state the accent owns. */
    Accent,

    /** Viewer, Revoked. */
    Muted,

    /** Expired. */
    Warning,
}

private val ChipShape = RoundedCornerShape(10.dp)

/**
 * The read-only badge. Role badges use the clay pair because a role is about a person;
 * Expired uses warning; Revoked and Viewer fall back to the muted surface pair.
 */
@Composable
fun ThykraBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Muted,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val (container, content) = when (tone) {
        BadgeTone.People -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        BadgeTone.Accent -> scheme.primaryContainer to scheme.onPrimaryContainer
        BadgeTone.Muted -> scheme.surfaceVariant to scheme.onSurfaceVariant
        BadgeTone.Warning -> extended.warningContainer to extended.onWarningContainer
    }

    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = content,
        modifier = modifier
            .background(container, ChipShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * The assist chip: it opens a picker, and says so with a Chevron rotated 90 degrees —
 * the set has no caret of its own, on purpose.
 */
@Composable
fun AssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(32.dp)
            .border(1.dp, scheme.outline, ChipShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
        )
        Icon(
            imageVector = ThykraIcons.Chevron,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                // A caret is a Chevron turned a quarter, and it still mirrors in RTL
                // because the Chevron underneath it does.
                .graphicsLayer { rotationZ = 90f },
        )
    }
}

/** The filter chip: it toggles, and shows a leading Check when it is on. */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(32.dp)
            .then(
                if (selected) {
                    Modifier.background(scheme.secondaryContainer, ChipShape)
                } else {
                    Modifier.border(1.dp, scheme.outline, ChipShape)
                },
            )
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = ThykraIcons.Check,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
        )
    }
}

/** The pill a reaction count sits in — shared by [ReactionBar] on both surfaces. */
@Composable
internal fun reactionChipColors(
    reactedByMe: Boolean,
    overMedia: Boolean,
): Triple<Color, Color, Color?> {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    return when {
        overMedia -> Triple(
            extended.scrimStrong.copy(alpha = com.jameeli.thykra.ui.theme.ScrimPillAlpha),
            extended.onScrim,
            if (reactedByMe) scheme.tertiary else null,
        )

        reactedByMe -> Triple(
            scheme.tertiaryContainer,
            scheme.onTertiaryContainer,
            scheme.tertiary,
        )

        else -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, null)
    }
}
