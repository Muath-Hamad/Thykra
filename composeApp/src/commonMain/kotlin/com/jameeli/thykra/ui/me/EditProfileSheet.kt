package com.jameeli.thykra.ui.me

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
import com.jameeli.thykra.resources.me_name_error
import com.jameeli.thykra.resources.me_name_helper
import com.jameeli.thykra.resources.me_name_label
import com.jameeli.thykra.resources.me_your_name
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.ThykraTextField

/** One field. The avatar is changed from the photo picker on the screen itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }
    // Resolved here because the Save handler is a plain lambda, not a composable scope.
    val nameError = stringResource(Res.string.me_name_error)

    ThykraSheet(onDismiss = onDismiss, title = stringResource(Res.string.me_your_name)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThykraTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = stringResource(Res.string.me_name_label),
                helper = stringResource(Res.string.me_name_helper),
                error = error,
                maxLength = 60,
            )
            ThykraButton(
                label = stringResource(Res.string.common_save),
                onClick = {
                    if (name.isBlank()) error = nameError else onSave(name)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
