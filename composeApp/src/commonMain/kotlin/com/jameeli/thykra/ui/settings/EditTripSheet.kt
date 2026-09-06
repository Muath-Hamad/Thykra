package com.jameeli.thykra.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.common_save
import com.jameeli.thykra.resources.create_trip_desc_label
import com.jameeli.thykra.resources.create_trip_desc_placeholder
import com.jameeli.thykra.resources.create_trip_name_helper
import com.jameeli.thykra.resources.create_trip_name_label
import com.jameeli.thykra.resources.edit_trip_name_error
import com.jameeli.thykra.resources.edit_trip_title
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.ThykraTextField

/** Two inputs and a Save. The same two fields the create sheet asks for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripSheet(
    initialTitle: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var error by remember { mutableStateOf<String?>(null) }
    // Resolved here because the Save handler is a plain lambda, not a composable scope.
    val editTripNameError = stringResource(Res.string.edit_trip_name_error)

    ThykraSheet(onDismiss = onDismiss, title = stringResource(Res.string.edit_trip_title)) {
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
                    error = null
                },
                label = stringResource(Res.string.create_trip_name_label),
                helper = stringResource(Res.string.create_trip_name_helper),
                error = error,
                maxLength = 60,
            )
            ThykraTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(Res.string.create_trip_desc_label),
                placeholder = stringResource(Res.string.create_trip_desc_placeholder),
                maxLength = 200,
                singleLine = false,
                minLines = 2,
            )
            ThykraButton(
                label = stringResource(Res.string.common_save),
                onClick = {
                    if (title.isBlank()) {
                        error = editTripNameError
                    } else {
                        onSave(title, description.ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
