package com.jameeli.thykra.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.social.ReactionEmoji
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.LocalCompactWidth
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.elevation
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykraShadow
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * Part 2 §4.9.
 *
 * A [Popup] anchored above the React button or the long-pressed plate, not a sheet: the
 * eight choices have to be one thumb-move away, and a sheet is a journey.
 *
 * Cells read their English name from the string table — the emoji glyph is never the
 * accessibility label.
 */
@Composable
fun ReactionPicker(
    current: ReactionType?,
    onPick: (ReactionType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current
    val haptic = rememberHaptics()
    // Below 380 dp eight 44 dp cells do not fit on one row, so they wrap to two of four.
    val compact = LocalCompactWidth.current

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        haptic(HapticKind.Tick)
        shown = true
    }

    val scale by animateFloatAsState(
        targetValue = if (shown || reduced) 1f else 0.8f,
        animationSpec = thykraTween(motion.dur2, motion.spring),
        label = "pickerScale",
    )

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .graphicsLayer {
                    if (!reduced) {
                        scaleX = scale
                        scaleY = scale
                    }
                    alpha = if (shown) 1f else 0f
                }
                .thykraShadow(MaterialTheme.elevation.level3, RoundedCornerShape(20.dp))
                .background(scheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val rows = if (compact) ReactionEmoji.ordered.chunked(4) else listOf(ReactionEmoji.ordered)
            rows.forEachIndexed { rowIndex, chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    chunk.forEachIndexed { index, type ->
                        PickerCell(
                            type = type,
                            selected = type == current,
                            index = rowIndex * 4 + index,
                            shown = shown,
                            onPick = {
                                haptic(HapticKind.Confirm)
                                onPick(type)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCell(
    type: ReactionType,
    selected: Boolean,
    index: Int,
    shown: Boolean,
    onPick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current
    val name = ReactionEmoji.label(type)

    val scale by animateFloatAsState(
        targetValue = if (shown || reduced) 1f else 0.6f,
        animationSpec = thykraTween(
            durationMillis = motion.dur2,
            easing = motion.spring,
            delayMillis = if (reduced) 0 else motion.staggerDelay(index),
        ),
        label = "pickerCell",
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                if (!reduced) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .then(
                if (selected) {
                    Modifier.border(2.dp, scheme.tertiary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onPick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = ReactionEmoji.glyph(type), fontSize = 24.sp)
    }
}
