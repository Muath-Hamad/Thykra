package com.jameeli.thykra.ui.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Turns a content URI into the platform-neutral [PlatformMediaFile] the upload queue
 * takes.
 *
 * Shared by the picker and the share target, which is the point: a photo shared in from
 * the gallery has to arrive as exactly the same thing as one chosen inside the app, or
 * the two paths drift and only one of them gets a fix.
 *
 * Returns null when the URI has no MIME type the resolver recognises — a file the app
 * cannot describe cannot be uploaded, and guessing from the extension would put junk in
 * the queue for the server to reject later.
 */
fun mediaFileFromUri(context: Context, uri: Uri): PlatformMediaFile? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: return null

    var name = "media_${System.currentTimeMillis()}"
    var size = 0L
    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null, null, null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            // getColumnIndex, not getColumnIndexOrThrow: a share can arrive from a
            // provider that publishes neither column, and losing the display name is not
            // a reason to drop the photo.
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }

    return PlatformMediaFile(
        name = name,
        contentType = mimeType,
        size = size,
        readBytes = {
            resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        },
    )
}
