package com.jameeli.thykra.api

import com.jameeli.thykra.model.ConfirmUploadRequest
import com.jameeli.thykra.model.RequestUploadUrlRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class UploadStatus { QUEUED, UPLOADING, CONFIRMING, DONE, FAILED }

data class UploadState(
    val id: String,
    val albumId: String,
    val filename: String,
    val status: UploadStatus,
    val attempt: Int = 0,
    val mediaId: String? = null,
    val error: String? = null
)

data class UploadRequest(
    val albumId: String,
    val filename: String,
    val contentType: String,
    val fileSize: Long,
    val readBytes: suspend () -> ByteArray,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val takenAt: Instant? = null
)

class UploadQueueManager(
    private val mediaApi: MediaApi,
    scope: CoroutineScope,
    private val persistence: UploadPersistence? = null,
    private val networkMonitor: NetworkMonitor? = null
) {
    companion object {
        private const val MAX_ATTEMPTS = 3
    }

    private val _uploads = MutableStateFlow<List<UploadState>>(emptyList())
    val uploads: StateFlow<List<UploadState>> = _uploads.asStateFlow()

    private val queue = Channel<Pair<String, UploadRequest>>(Channel.UNLIMITED)
    private var nextId = Clock.System.now().toEpochMilliseconds()

    init {
        scope.launch {
            persistence?.loadAll()?.forEach { record ->
                val id = record.id
                _uploads.update { it + UploadState(id, record.albumId, record.filename, UploadStatus.QUEUED) }
                val persistedRequest = UploadRequest(
                    albumId = record.albumId,
                    filename = record.filename,
                    contentType = record.contentType,
                    fileSize = record.fileSize,
                    readBytes = { persistence.loadBytes(id) ?: ByteArray(0) },
                    width = record.width,
                    height = record.height,
                    durationMs = record.durationMs,
                    takenAt = record.takenAtMs?.let { Instant.fromEpochMilliseconds(it) }
                )
                queue.trySend(Pair(id, persistedRequest))
            }
            processQueue()
        }
    }

    fun enqueue(request: UploadRequest): String {
        val id = "upload_${nextId++}"
        _uploads.update { it + UploadState(id, request.albumId, request.filename, UploadStatus.QUEUED) }
        queue.trySend(Pair(id, request))
        return id
    }

    suspend fun enqueueWithPersistence(request: UploadRequest): String {
        val id = "upload_${nextId++}"
        if (persistence != null) {
            val bytes = request.readBytes()
            persistence.saveBytes(id, bytes)
            persistence.save(
                PendingUploadRecord(
                    id = id,
                    albumId = request.albumId,
                    filename = request.filename,
                    contentType = request.contentType,
                    fileSize = request.fileSize,
                    width = request.width,
                    height = request.height,
                    durationMs = request.durationMs,
                    takenAtMs = request.takenAt?.toEpochMilliseconds()
                )
            )
            val persistedRequest = request.copy(readBytes = { persistence.loadBytes(id) ?: ByteArray(0) })
            _uploads.update { it + UploadState(id, request.albumId, request.filename, UploadStatus.QUEUED) }
            queue.trySend(Pair(id, persistedRequest))
        } else {
            _uploads.update { it + UploadState(id, request.albumId, request.filename, UploadStatus.QUEUED) }
            queue.trySend(Pair(id, request))
        }
        return id
    }

    fun clearCompleted() {
        _uploads.update { list ->
            list.filter { it.status != UploadStatus.DONE && it.status != UploadStatus.FAILED }
        }
    }

    private suspend fun waitForConnectivity() {
        networkMonitor?.isConnected?.first { it }
    }

    private suspend fun processQueue() {
        for ((id, request) in queue) {
            waitForConnectivity()
            executeWithRetry(id, request)
        }
    }

    private suspend fun executeWithRetry(uploadId: String, request: UploadRequest) {
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                updateUpload(uploadId) { it.copy(status = UploadStatus.UPLOADING, attempt = attempt) }

                val presignedResponse = mediaApi.requestUploadUrl(
                    request.albumId,
                    RequestUploadUrlRequest(request.filename, request.contentType, request.fileSize)
                )
                val presigned = presignedResponse.data
                    ?: throw Exception(presignedResponse.error ?: "Failed to get upload URL")

                val bytes = request.readBytes()
                mediaApi.uploadFile(presigned.uploadUrl, presigned.method, presigned.headers, bytes, request.contentType)

                updateUpload(uploadId) { it.copy(status = UploadStatus.CONFIRMING) }

                val confirmResponse = mediaApi.confirmUpload(
                    request.albumId,
                    presigned.mediaId,
                    ConfirmUploadRequest(request.width, request.height, request.durationMs, request.takenAt)
                )
                val media = confirmResponse.data
                    ?: throw Exception(confirmResponse.error ?: "Failed to confirm upload")

                updateUpload(uploadId) { it.copy(status = UploadStatus.DONE, mediaId = media.id) }
                persistence?.remove(uploadId)
                persistence?.removeBytes(uploadId)
                return

            } catch (e: Exception) {
                if (attempt == MAX_ATTEMPTS) {
                    updateUpload(uploadId) { it.copy(status = UploadStatus.FAILED, error = e.message) }
                    persistence?.remove(uploadId)
                    persistence?.removeBytes(uploadId)
                } else {
                    delay(1000L * (1L shl (attempt - 1))) // 1s → 2s → 4s
                }
            }
        }
    }

    private fun updateUpload(id: String, transform: (UploadState) -> UploadState) {
        _uploads.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
