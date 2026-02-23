package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMediaPickerLauncher(
    onResult: (List<PlatformMediaFile>) -> Unit
): () -> Unit = { /* TODO: iOS PHPicker implementation */ }
