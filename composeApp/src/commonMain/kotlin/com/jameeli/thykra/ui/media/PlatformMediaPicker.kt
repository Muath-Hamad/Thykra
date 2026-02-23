package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable

@Composable
expect fun rememberMediaPickerLauncher(
    onResult: (List<PlatformMediaFile>) -> Unit
): () -> Unit
