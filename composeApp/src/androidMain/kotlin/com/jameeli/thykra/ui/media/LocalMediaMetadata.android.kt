package com.jameeli.thykra.ui.media

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** Capture date and pixel size, read from the file itself. All fields are best-effort. */
internal data class LocalMediaMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val takenAt: Instant? = null,
    val durationMs: Long? = null,
)

/**
 * Reads what the file knows about itself, so a queued upload can be placed in the right
 * day chapter before the server has ever seen it.
 *
 * Entirely best-effort. The server extracts this again on confirm and its answer wins;
 * this only exists so a photo taken last Tuesday does not sit under today for the length
 * of the upload and then jump. Every failure path returns nulls rather than throwing —
 * an unreadable date is not a reason to refuse the photo.
 */
internal fun readLocalMetadata(
    resolver: ContentResolver,
    uri: Uri,
    contentType: String,
): LocalMediaMetadata = when {
    contentType.startsWith("image/") -> readImageMetadata(resolver, uri)
    contentType.startsWith("video/") -> readVideoMetadata(resolver, uri)
    else -> LocalMediaMetadata()
}

private fun readImageMetadata(resolver: ContentResolver, uri: Uri): LocalMediaMetadata {
    // Bounds first, from a decode that allocates no pixels.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }

    val exif = runCatching {
        resolver.openInputStream(uri)?.use { ExifInterface(it) }
    }.getOrNull()

    // A portrait photo is stored landscape with a rotation tag, so the raw bounds are
    // the wrong way round for anything that lays out by aspect ratio.
    val rotated = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE,
        -> true

        else -> false
    }

    val width = bounds.outWidth.takeIf { it > 0 }
    val height = bounds.outHeight.takeIf { it > 0 }

    return LocalMediaMetadata(
        width = if (rotated) height else width,
        height = if (rotated) width else height,
        takenAt = exif?.let(::exifTakenAt),
    )
}

/**
 * EXIF dates carry no timezone — "2026:04:12 09:30:00" is wall-clock time wherever the
 * photo was taken. Resolving it against the phone's current zone is the same guess every
 * gallery app makes, and it is the one that puts the photo on the day the person
 * remembers taking it.
 */
private fun exifTakenAt(exif: ExifInterface): Instant? {
    val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null

    val parts = raw.trim().split(' ')
    if (parts.size != 2) return null
    val date = parts[0].split(':')
    val time = parts[1].split(':')
    if (date.size != 3 || time.size < 2) return null

    return runCatching {
        LocalDateTime(
            year = date[0].toInt(),
            monthNumber = date[1].toInt(),
            dayOfMonth = date[2].toInt(),
            hour = time[0].toInt(),
            minute = time[1].toInt(),
            second = time.getOrNull(2)?.toIntOrNull() ?: 0,
        ).toInstant(TimeZone.currentSystemDefault())
    }.getOrNull()
}

private fun readVideoMetadata(resolver: ContentResolver, uri: Uri): LocalMediaMetadata =
    runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(resolver.openFileDescriptor(uri, "r")!!.fileDescriptor)
            fun tag(key: Int) = retriever.extractMetadata(key)

            // A rotated video reports its stored size, same trap as a portrait photo.
            val rotation = tag(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val width = tag(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = tag(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val swapped = rotation == 90 || rotation == 270

            LocalMediaMetadata(
                width = if (swapped) height else width,
                height = if (swapped) width else height,
                durationMs = tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                // METADATA_KEY_DATE is "yyyyMMdd'T'HHmmss.SSS'Z'" and genuinely UTC,
                // unlike EXIF's zone-less wall clock.
                takenAt = tag(MediaMetadataRetriever.METADATA_KEY_DATE)?.let(::videoTakenAt),
            )
        }
    }.getOrElse { LocalMediaMetadata() }

private fun videoTakenAt(raw: String): Instant? = runCatching {
    val trimmed = raw.trim()
    if (trimmed.length < 15 || trimmed[8] != 'T') return null
    LocalDateTime(
        year = trimmed.substring(0, 4).toInt(),
        monthNumber = trimmed.substring(4, 6).toInt(),
        dayOfMonth = trimmed.substring(6, 8).toInt(),
        hour = trimmed.substring(9, 11).toInt(),
        minute = trimmed.substring(11, 13).toInt(),
        second = trimmed.substring(13, 15).toInt(),
    ).toInstant(TimeZone.UTC)
}.getOrNull()
