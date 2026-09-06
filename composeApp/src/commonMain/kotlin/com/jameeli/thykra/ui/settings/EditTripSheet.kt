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

    ThykraSheet(onDismiss = onDismiss, title = "Edit trip") {
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
                label = "Trip title",
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
            ThykraButton(
                label = "Save",
                onClick = {
                    if (title.isBlank()) {
                        error = "Give the trip a name"
                    } else {
                        onSave(title, description.ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
