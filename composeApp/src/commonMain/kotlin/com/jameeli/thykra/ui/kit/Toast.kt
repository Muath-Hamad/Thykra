package com.jameeli.thykra.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.elevation
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraShadow
import com.jameeli.thykra.ui.theme.thykraTween

enum class ToastTone { Neutral, Success, Error }

@Immutable
data class ToastAction(val label: String, val onClick: () -> Unit)

@Immutable
data class ToastMessage(
    val message: String,
    val tone: ToastTone = ToastTone.Neutral,
    val action: ToastAction? = null,
    /** Indefinite while offline: a Retry that disappears is a Retry you cannot take. */
    val indefinite: Boolean = false,
)

/**
 * Part 2 §4.5. One at a time; a new one replaces.
 *
 * This is the kit's replacement for Material's Snackbar, which paints itself on
 * `inverseSurface` — a colour the Editions scheme uses for something else.
 */
@Stable
class ToastState {
    internal var current by mutableStateOf<ToastMessage?>(null)
        private set

    fun show(
        message: String,
        tone: ToastTone = ToastTone.Neutral,
        action: ToastAction? = null,
        indefinite: Boolean = false,
    ) {
        current = ToastMessage(message, tone, action, indefinite)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/**
 * Sits 12 dp above whichever bottom chrome is topmost — the nav bar, the action bar or
 * the dock. The caller passes that offset as `bottomPadding`.
 */
@Composable
fun ToastHost(
    state: ToastState,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 12.dp,
    /** TalkBack stretches Undo toasts so there is time to reach the button. */
    accessibilityEnabled: Boolean = false,
) {
    val toast = state.current
    val motion = LocalMotion.current
    val haptic = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val shape = MaterialTheme.shapes.medium

    LaunchedEffect(toast) {
        val message = toast ?: return@LaunchedEffect
        if (message.tone == ToastTone.Error) haptic(HapticKind.Reject)
        if (message.indefinite) return@LaunchedEffect
        val base = if (message.action != null) 8_000L else 4_000L
        val duration = if (accessibilityEnabled && message.action != null) 10_000L else base
        kotlinx.coroutines.delay(duration)
        if (state.current === message) state.dismiss()
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(thykraTween(motion.dur3)) +
                slideInVertically(thykraTween(motion.dur3)) { it / 3 },
            exit = fadeOut(thykraTween(motion.dur2)),
        ) {
            val message = toast ?: return@AnimatedVisibility
            val bar = when (message.tone) {
                ToastTone.Neutral -> scheme.primary
                ToastTone.Success -> extended.success
                ToastTone.Error -> scheme.error
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomPadding)
                    .fillMaxWidth()
                    .thykraShadow(MaterialTheme.elevation.level3, shape)
                    .clip(shape)
                    .background(scheme.surfaceContainer)
                    .semantics {
                        liveRegion = if (message.tone == ToastTone.Error) {
                            LiveRegionMode.Assertive
                        } else {
                            LiveRegionMode.Polite
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(48.dp)
                        .background(bar),
                )
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                )
                message.action?.let { action ->
                    ThykraButton(
                        label = action.label,
                        onClick = {
                            action.onClick()
                            state.dismiss()
                        },
                        variant = ThykraButtonVariant.Text,
                        size = ThykraButtonSize.Compact,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        }
    }
}
