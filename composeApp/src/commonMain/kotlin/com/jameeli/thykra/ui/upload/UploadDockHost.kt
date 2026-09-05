package com.jameeli.thykra.ui.upload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadState
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.navigation.ProvideUploadDock
import com.jameeli.thykra.ui.kit.UploadBatch
import com.jameeli.thykra.ui.kit.UploadDock
import com.jameeli.thykra.ui.kit.UploadRowState
import com.jameeli.thykra.ui.kit.UploadRowStatus
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.LocalCompactWidth
import com.jameeli.thykra.ui.theme.rememberHaptics
import kotlinx.coroutines.delay
import com.jameeli.thykra.nowMillis

/** How long the celebration stays before the dock takes itself away. */
private const val CelebrationMs = 6_000L

/**
 * Design part 3 §06, J6.
 *
 * Hands the shell a dock for the batch that is currently in flight. Hosting it here —
 * rather than inside a screen — is what lets it survive navigation and re-mount from the
 * persisted queue on a cold start.
 *
 * @param albumId when set, only that trip's batch is shown and the trip name is dropped
 *   from the summary, because you are already looking at it.
 */
@Composable
fun UploadDockHost(
    uploadQueueManager: UploadQueueManager,
    tripTitleFor: (String) -> String,
    modifier: Modifier = Modifier,
    albumId: String? = null,
    networkMonitor: NetworkMonitor? = null,
    onSeeThem: (String) -> Unit = {},
    onBatchComplete: (String) -> Unit = {},
) {
    val uploads by uploadQueueManager.uploads.collectAsState()
    val connectedState = networkMonitor?.isConnected?.collectAsState()
    val connected = connectedState?.value ?: true

    var expanded by remember { mutableStateOf(false) }
    var celebratedBatch by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHaptics()
    val compact = LocalCompactWidth.current
    val rate = rememberUploadRate()

    val relevant = uploads.filter { albumId == null || it.albumId == albumId }
    val batchId = relevant.activeBatchId()
    val rows = relevant.filter { it.batchId == batchId }

    val batch = if (batchId == null || rows.isEmpty()) {
        null
    } else {
        val uploadedBytes = rows.sumOf { it.bytesUploaded }
        UploadBatch(
            id = batchId,
            tripId = rows.first().albumId,
            tripTitle = tripTitleFor(rows.first().albumId),
            rows = rows.map { it.toRowState() },
            connected = connected,
            secondsRemaining = rate.secondsRemaining(
                uploaded = uploadedBytes,
                total = rows.sumOf { it.totalBytes },
            ),
            celebrationDetail = null,
        )
    }

    // Time-left is a moving average, so one fast chunk does not promise a fast batch.
    LaunchedEffect(batch?.bytesUploaded) {
        batch?.let { rate.sample(it.bytesUploaded) }
    }

    // One celebration per batch, then the dock takes itself away.
    LaunchedEffect(batch?.id, batch?.complete) {
        val current = batch ?: return@LaunchedEffect
        if (!current.complete || celebratedBatch == current.id) return@LaunchedEffect
        celebratedBatch = current.id
        haptic(HapticKind.Confirm)
        onBatchComplete(current.tripId)
        delay(CelebrationMs)
        uploadQueueManager.dismissBatch(current.id)
        expanded = false
    }

    // Failures keep the dock up indefinitely, so Retry all is always reachable.
    LaunchedEffect(batch?.failedCount) {
        if ((batch?.failedCount ?: 0) > 0) haptic(HapticKind.Reject)
    }

    ProvideUploadDock(
        content = batch?.let { current ->
            {
                UploadDock(
                    batch = current,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    modifier = modifier,
                    onRetry = uploadQueueManager::retry,
                    onSkip = uploadQueueManager::skip,
                    onRetryAll = { uploadQueueManager.retryAll(current.id) },
                    onSeeThem = {
                        uploadQueueManager.dismissBatch(current.id)
                        onSeeThem(current.tripId)
                    },
                    showTripName = albumId == null,
                    compact = compact,
                )
            }
        },
    )
}

/**
 * The batch the dock should be showing: the one with work still in it, or failing that
 * the newest completed one that has not been dismissed.
 */
private fun List<UploadState>.activeBatchId(): String? {
    val live = firstOrNull { it.status != UploadStatus.DONE }
    if (live != null) return live.batchId
    return maxByOrNull { it.enqueuedAtMs }?.batchId
}

private fun UploadState.toRowState() = UploadRowState(
    id = id,
    filename = filename,
    status = when (status) {
        UploadStatus.QUEUED -> UploadRowStatus.Queued
        UploadStatus.UPLOADING -> UploadRowStatus.Uploading
        UploadStatus.CONFIRMING -> UploadRowStatus.Confirming
        UploadStatus.DONE -> UploadRowStatus.Done
        UploadStatus.FAILED -> UploadRowStatus.Failed
    },
    // A queued file has not been uploaded yet, so there is no server thumbnail to show.
    // The plate placeholder stands in until there is.
    thumbnailUrl = null,
    isVideo = contentType.startsWith("video/"),
    bytesUploaded = bytesUploaded,
    totalBytes = totalBytes,
    failureReason = error,
    // A local failure — a file the server would refuse anyway — offers Skip, not Retry.
    retryable = error?.contains("too large", ignoreCase = true) != true,
)

/**
 * A ten-sample moving average of bytes per second.
 *
 * Shown only after three seconds and once it is under an hour, both because a number
 * that swings wildly in the first second is worse than no number at all.
 */
private class UploadRate {
    private val samples = ArrayDeque<Pair<Long, Long>>()

    fun sample(uploadedBytes: Long) {
        val now = nowMillis()
        samples.addLast(now to uploadedBytes)
        while (samples.size > 10) samples.removeFirst()
    }

    fun secondsRemaining(uploaded: Long, total: Long): Int? {
        if (samples.size < 2 || total <= uploaded) return null
        val (firstMs, firstBytes) = samples.first()
        val (lastMs, lastBytes) = samples.last()
        val elapsedMs = lastMs - firstMs
        if (elapsedMs < 3_000) return null
        val bytesPerMs = (lastBytes - firstBytes).toDouble() / elapsedMs
        if (bytesPerMs <= 0.0) return null
        val seconds = ((total - uploaded) / bytesPerMs / 1000).toInt()
        return if (seconds in 1..3599) seconds else null
    }
}

@Composable
private fun rememberUploadRate(): UploadRate = remember { UploadRate() }
