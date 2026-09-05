package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.rememberHaptics

/**
 * Part 2 §4.5. The only [AlertDialog] in the app.
 *
 * Sheets are dismissable by an accidental swipe, and a decision that cannot be undone
 * should not be. So this is the one thing that is still a dialog: it is not dismissable
 * by the scrim, the title is a question naming the thing, and the body states the
 * consequence in numbers from the DTO.
 *
 * Confirm is drawn last so a TalkBack swipe hears the consequence before the verb.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    loading: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = rememberHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(max = 312.dp),
        properties = DialogProperties(
            // Back cancels; the scrim does not.
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
        containerColor = scheme.surfaceContainerHigh,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            // The only filled destructive button in the app.
            ThykraButton(
                label = confirmLabel,
                onClick = {
                    haptic(HapticKind.Reject)
                    onConfirm()
                },
                variant = ThykraButtonVariant.Filled,
                destructive = true,
                loading = loading,
            )
        },
        dismissButton = {
            ThykraButton(
                label = dismissLabel,
                onClick = onDismiss,
                variant = ThykraButtonVariant.Text,
                enabled = !loading,
            )
        },
    )
}
