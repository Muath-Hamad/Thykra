package com.jameeli.thykra.ui.upload

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadRequest
import com.jameeli.thykra.ui.media.PlatformMediaFile
import kotlinx.coroutines.launch

/**
 * Drains [IncomingShareBus] by asking which trip the photos belong to.
 *
 * Lives beside the shell rather than inside a screen, because a share is a cold start:
 * whatever screen happens to be composed when the intent lands is not something to
 * depend on.
 *
 * Signed out, it shows nothing and holds the files. Sign-in rebuilds the graph, this
 * recomposes, and the sheet appears then — the same shape as the invite deep link, which
 * has the same problem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingShareHost(
    signedOut: Boolean,
    albumApi: AlbumApi,
    uploadQueueManager: UploadQueueManager,
    /** The trip currently on screen, if any. Null anywhere else. */
    currentTripId: String?,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
) {
    val pending by IncomingShareBus.pending.collectAsState()
    val scope = rememberCoroutineScope()

    // Set while the person is off making a trip to put these in. The sheet steps aside
    // so it is not competing with the create sheet, and the files stay held.
    var awaitingNewTrip by remember { mutableStateOf(false) }

    fun enqueueInto(albumId: String, files: List<PlatformMediaFile>) {
        scope.launch {
            files.forEach { file ->
                uploadQueueManager.enqueueWithPersistence(
                    UploadRequest(
                        albumId = albumId,
                        filename = file.name,
                        contentType = file.contentType,
                        fileSize = file.size,
                        readBytes = file.readBytes,
                        width = file.width,
                        height = file.height,
                        takenAt = file.takenAt,
                        durationMs = file.durationMs,
                    ),
                )
            }
            // Cleared only after staging, never before: readBytes reads through a content
            // URI whose permission dies with the activity that received the share.
            IncomingShareBus.clear()
        }
    }

    // The trip they went off to make now exists and they are standing in it. That is the
    // answer to the question the sheet was asking, so do not ask it again.
    LaunchedEffect(awaitingNewTrip, currentTripId, pending) {
        if (awaitingNewTrip && currentTripId != null && pending.isNotEmpty()) {
            awaitingNewTrip = false
            enqueueInto(currentTripId, pending)
        }
    }

    if (pending.isEmpty() || signedOut || awaitingNewTrip) return

    val trips = rememberShareTrips {
        val response = albumApi.getAlbums()
        if (response.success) response.data.orEmpty() else emptyList()
    }

    SharePickerSheet(
        fileCount = pending.size,
        trips = trips.trips,
        loading = trips.loading,
        onPick = { trip ->
            enqueueInto(trip.id, pending)
            onOpenTrip(trip.id)
        },
        onCreateTrip = {
            awaitingNewTrip = true
            onCreateTrip()
        },
        // Dismissing is a decision: the photos are dropped rather than left to reappear
        // over an unrelated screen later.
        onDismiss = { IncomingShareBus.clear() },
    )
}
