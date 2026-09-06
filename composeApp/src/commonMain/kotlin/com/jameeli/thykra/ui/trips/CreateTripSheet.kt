package com.jameeli.thykra.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.ThykraTextField
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/**
 * Design part 3 §08.
 *
 * A new trip is PRIVATE and the sheet says so as a line rather than a control — there is
 * exactly one decision here, and adding a second would make the first slower.
 *
 * Validation runs on submit, not as you type. The keyboard pushes the sheet up so the
 * button never ends up behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripSheet(
    viewModel: TripsViewModel,
    onDismiss: () -> Unit,
    onCreated: (AlbumDto) -> Unit,
) {
    val creating by viewModel.creating.collectAsState()
    val error by viewModel.createError.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    ThykraSheet(
        onDismiss = {
            viewModel.clearCreateError()
            onDismiss()
        },
        title = "Start a trip",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThykraTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (error != null) viewModel.clearCreateError()
                },
                label = "Trip title",
                placeholder = "Where did you go?",
                helper = "Friends will see this",
                error = error,
                maxLength = 60,
            )

            ThykraTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description · optional",
                placeholder = "A line about where and when",
                maxLength = 200,
                singleLine = false,
                minLines = 2,
            )

            PrivacyLine()

            ThykraButton(
                label = "Create trip",
                onClick = {
                    viewModel.createTrip(title, description.ifBlank { null }, onCreated)
                },
                loading = creating,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Read-only: new trips are private, and settings is where that changes. */
@Composable
private fun PrivacyLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = ThykraIcons.Lock,
            contentDescription = null,
            tint = MaterialTheme.thykra.textMeta,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Private · only people you invite",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.thykra.textMeta,
        )
    }
}
