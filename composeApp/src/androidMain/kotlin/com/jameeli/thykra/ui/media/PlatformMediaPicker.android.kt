package com.jameeli.thykra.ui.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMediaPickerLauncher(
    onResult: (List<PlatformMediaFile>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { uris ->
        // Same conversion the share target uses, so a picked photo and a shared one are
        // indistinguishable by the time they reach the queue.
        onResult(uris.mapNotNull { mediaFileFromUri(context, it) })
    }
    return { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)) }
}
