package com.jameeli.thykra.ui.media

import android.provider.OpenableColumns
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
        val files = uris.mapNotNull { uri ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: return@mapNotNull null
            var name = "media_${System.currentTimeMillis()}"
            var size = 0L
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                }
            }
            PlatformMediaFile(
                name = name,
                contentType = mimeType,
                size = size,
                readBytes = {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                }
            )
        }
        onResult(files)
    }
    return { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)) }
}
