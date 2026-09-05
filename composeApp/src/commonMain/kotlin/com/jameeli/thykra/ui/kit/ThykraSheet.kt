package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Part 2 §4.4. Every decision that is not destructive arrives from the bottom edge, where
 * the thumb already is.
 *
 * Sheets never stack: a sheet that needs a [ConfirmDialog] closes first and then the
 * dialog opens, so there is never a scrim over a scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThykraSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    /** A member row, a trip cover — anything that stands in place of a plain title. */
    header: (@Composable () -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        // Large top corners, not Material's 28.
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 0.dp,
        scrimColor = scheme.scrim.copy(alpha = 0.32f),
        dragHandle = { SheetHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(
                    bottom = 20.dp +
                        WindowInsets.navigationBars.asBottomPadding(),
                ),
        ) {
            header?.invoke()
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        // Focus lands here on open.
                        .semantics { heading() },
                )
            }
            content()
        }
    }
}

/** The 32 x 4 rule the sheet draws for itself — there is no drag-handle icon in the set. */
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun WindowInsets.asBottomPadding() =
    with(androidx.compose.ui.platform.LocalDensity.current) {
        getBottom(this).toDp()
    }
