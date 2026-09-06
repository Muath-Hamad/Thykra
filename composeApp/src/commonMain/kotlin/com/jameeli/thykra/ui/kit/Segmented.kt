package com.jameeli.thykra.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.elevation
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykraShadow
import com.jameeli.thykra.ui.theme.thykraTween

@Immutable
data class SegmentedOption(val label: String, val icon: ImageVector? = null)

/**
 * Part 2 §4.6. Days / Sheet on the trip screen, and the appearance control on Me.
 *
 * Not `SingleChoiceSegmentedButtonRow`: its outline style fights the kit. This is a
 * sunken track with a raised thumb that slides between exactly two or three segments.
 */
@Composable
fun Segmented(
    options: List<SegmentedOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(options.size in 2..3) { "Segmented takes exactly two or three segments" }
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val elevation = MaterialTheme.elevation
    val haptic = rememberHaptics()

    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = thykraTween(motion.dur2, motion.easeOut),
        label = "segmentedThumb",
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .background(scheme.secondaryContainer, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .semantics { selectableGroup() },
    ) {
        // The thumb is laid out by hand so it can sit at a fractional index mid-slide.
        Layout(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .thykraShadow(elevation.level1, RoundedCornerShape(8.dp))
                        .background(scheme.surface, RoundedCornerShape(8.dp))
                        .then(
                            if (elevation.hairlineInsteadOfShadow) {
                                Modifier.border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            },
                        ),
                )
            },
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) { measurables, constraints ->
            val segmentWidth = constraints.maxWidth / options.size
            val placeable = measurables.first().measure(
                constraints.copy(minWidth = segmentWidth, maxWidth = segmentWidth),
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(x = (position * segmentWidth).toInt(), y = 0)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            role = Role.RadioButton,
                            onClick = {
                                if (index != selectedIndex) {
                                    haptic(HapticKind.Tick)
                                    onSelect(index)
                                }
                            },
                        )
                        .semantics {
                            // "Days, selected, 1 of 2"
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    option.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
