package com.jameeli.thykra.api

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** What the notification needs to know, and nothing else. */
private data class QueueProgress(val running: Boolean, val done: Int, val total: Int)

/**
 * Watches the upload queue and keeps [UploadForegroundService] in step with it.
 *
 * Separated from the service so the queue is observed in exactly one place. The service
 * renders; this decides. That also means the notification and the in-app dock are reading
 * the same StateFlow and cannot drift apart.
 *
 * Distinct-until-changed matters more than it looks: byte progress updates the flow many
 * times a second, and starting a foreground service on every tick would be both wasteful
 * and, on newer Android, rate-limited.
 */
class UploadNotifier(private val context: Context) {

    fun observe(scope: CoroutineScope, uploads: StateFlow<List<UploadState>>) {
        scope.launch {
            uploads
                .map { list ->
                    val settled = list.count {
                        it.status == UploadStatus.DONE || it.status == UploadStatus.FAILED
                    }
                    QueueProgress(
                        running = list.any {
                            it.status == UploadStatus.QUEUED ||
                                it.status == UploadStatus.UPLOADING ||
                                it.status == UploadStatus.CONFIRMING
                        },
                        done = settled,
                        total = list.size,
                    )
                }
                // Only the counts matter here, so a flood of byte-level updates collapses
                // into one service call per file that actually finishes.
                .distinctUntilChanged()
                .collect { progress ->
                    if (progress.running && progress.total > 0) start(progress) else stop()
                }
        }
    }

    private fun start(progress: QueueProgress) {
        val intent = Intent(context, UploadForegroundService::class.java).apply {
            putExtra(UploadForegroundService.EXTRA_DONE, progress.done)
            putExtra(UploadForegroundService.EXTRA_TOTAL, progress.total)
        }
        // startForegroundService, not startService: from Android 8 a background start of a
        // plain service throws. The service calls startForeground immediately, inside the
        // few seconds the platform allows before it kills us for not doing so.
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    private fun stop() {
        runCatching { context.stopService(Intent(context, UploadForegroundService::class.java)) }
    }
}
