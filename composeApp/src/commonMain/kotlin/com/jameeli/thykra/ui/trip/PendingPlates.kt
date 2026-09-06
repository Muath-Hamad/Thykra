package com.jameeli.thykra.ui.trip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.api.UploadState
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.chapters.localDateOf
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.trip_adding_file
import com.jameeli.thykra.ui.kit.skeleton
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/** Uploads still in flight for this trip, in the order they were queued. */
fun List<UploadState>.inFlightFor(albumId: String): List<UploadState> = filter {
    it.albumId == albumId &&
        (
            it.status == UploadStatus.QUEUED ||
                it.status == UploadStatus.UPLOADING ||
                it.status == UploadStatus.CONFIRMING
            )
}

/**
 * Groups in-flight uploads by the day their plate will land on.
 *
 * Uses the file's own capture date where it has one and the current time where it does
 * not, which is the same rule [com.jameeli.thykra.chapters.groupIntoChapters] applies to
 * media that has already arrived — so a placeholder sits in the chapter the real plate
 * will occupy, and nothing moves when the upload confirms.
 */
fun List<UploadState>.pendingByDay(timeZone: TimeZone = TimeZone.currentSystemDefault()): Map<LocalDate, List<UploadState>> {
    val now = Clock.System.now()
    return groupBy { localDateOf(it.takenAt ?: now, timeZone) }
}

/**
 * A plate that is not there yet.
 *
 * Deliberately the same skeleton the grid already uses while loading, at a fixed 4:3, so
 * a chapter mid-upload reads as "more is coming" rather than as a new kind of thing. The
 * real aspect is not known until the image is decoded, and guessing per file would make
 * the grid jump twice instead of once.
 */
@Composable
fun PendingPlate(upload: UploadState, modifier: Modifier = Modifier) {
    // Resolved before the semantics lambda, which is not a composable scope.
    val description = stringResource(Res.string.trip_adding_file, upload.filename)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PendingPlateHeight)
            .skeleton()
            // Named rather than silent: a screen reader on a half-uploaded trip should
            // hear that something is arriving, not skip an unlabelled box.
            //
            // mergeDescendants matters here even with nothing to merge. A plain Box that
            // is neither clickable nor focusable does not get promoted to its own
            // accessibility node just for carrying a contentDescription — verified on
            // device, where the plates rendered but never appeared in the tree. The
            // surrounding photo plates are exposed only because they are clickable.
            .semantics(mergeDescendants = true) { contentDescription = description },
    )
}

private val PendingPlateHeight = 180.dp
