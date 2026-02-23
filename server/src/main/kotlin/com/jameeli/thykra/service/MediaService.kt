package com.jameeli.thykra.service

import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.model.PresignedUploadDto
import com.jameeli.thykra.repository.MediaRepository
import com.jameeli.thykra.storage.StorageService
import kotlinx.datetime.Instant
import java.util.UUID

class MediaService(
    private val mediaRepository: MediaRepository,
    private val storageService: StorageService
) {

    suspend fun requestUploadUrl(
        albumId: UUID,
        uploaderId: UUID,
        filename: String,
        contentType: String,
        fileSize: Long
    ): PresignedUploadDto {
        val type = when {
            contentType.startsWith("image/") -> MediaType.PHOTO
            contentType.startsWith("video/") -> MediaType.VIDEO
            else -> throw IllegalArgumentException("Unsupported content type: $contentType")
        }
        val ext = filename.substringAfterLast('.', "")
        val storageKey = "$albumId/${UUID.randomUUID()}${if (ext.isNotBlank()) ".$ext" else ""}"
        val media = mediaRepository.create(albumId, uploaderId, type, storageKey, filename, contentType, fileSize)
        val presigned = storageService.generatePresignedUpload(storageKey, contentType)
        return PresignedUploadDto(
            mediaId = media.id,
            storageKey = storageKey,
            uploadUrl = presigned.uploadUrl,
            method = presigned.method,
            headers = presigned.headers,
            expiresIn = presigned.expiresIn
        )
    }

    suspend fun confirmUpload(
        mediaId: UUID,
        width: Int?,
        height: Int?,
        durationMs: Long?,
        takenAt: Instant?
    ): MediaDto? = mediaRepository.activate(mediaId, width, height, durationMs, takenAt)

    suspend fun listMedia(albumId: UUID): List<MediaDto> = mediaRepository.findAllForAlbum(albumId)

    suspend fun getMedia(mediaId: UUID): MediaDto? = mediaRepository.findById(mediaId)

    suspend fun deleteMedia(mediaId: UUID): Boolean {
        val media = mediaRepository.findById(mediaId) ?: return false
        storageService.delete(media.storageKey)
        mediaRepository.delete(mediaId)
        return true
    }
}
