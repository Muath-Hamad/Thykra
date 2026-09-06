package com.jameeli.thykra.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jameeli.thykra.auth.AndroidTokenStorage
import com.jameeli.thykra.model.ConfirmUploadRequest
import com.jameeli.thykra.model.RequestUploadUrlRequest
import kotlin.time.Instant
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenProvider = AndroidTokenStorage(applicationContext)
        val httpClient = createApiClient(tokenProvider)
        val mediaApi = MediaApi(httpClient)
        val persistence = AndroidUploadPersistence(applicationContext)

        val records = persistence.loadAll()
        if (records.isEmpty()) return Result.success()

        for (record in records) {
            try {
                val bytes = persistence.loadBytes(record.id) ?: continue

                val presignedResponse = mediaApi.requestUploadUrl(
                    record.albumId,
                    RequestUploadUrlRequest(record.filename, record.contentType, record.fileSize)
                )
                val presigned = presignedResponse.data ?: continue

                mediaApi.uploadFile(
                    presigned.uploadUrl, presigned.method, presigned.headers,
                    bytes, record.contentType
                )

                val takenAt: Instant? = record.takenAtMs?.let { fromEpochMilliseconds(it) }
                mediaApi.confirmUpload(
                    record.albumId,
                    presigned.mediaId,
                    ConfirmUploadRequest(record.width, record.height, record.durationMs, takenAt)
                )

                persistence.remove(record.id)
                persistence.removeBytes(record.id)
            } catch (_: Exception) {
                // Continue to next; retry on next schedule
            }
        }

        return Result.success()
    }
}
