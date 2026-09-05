package com.jameeli.thykra.model

import com.jameeli.thykra.chapters.ChapterMedia
import com.jameeli.thykra.chapters.HasDimensions
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MediaType { PHOTO, VIDEO }

@Serializable
enum class MediaStatus { PENDING, ACTIVE }

@Serializable
data class MediaDto(
    override val id: String,
    val albumId: String,
    val uploaderId: String,
    override val type: MediaType,
    val status: MediaStatus,
    val storageKey: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val filename: String,
    val contentType: String,
    val fileSize: Long,
    override val width: Int? = null,
    override val height: Int? = null,
    val durationMs: Long? = null,
    override val takenAt: Instant? = null,
    override val uploadedAt: Instant
) : ChapterMedia, HasDimensions

@Serializable
data class RequestUploadUrlRequest(
    val filename: String,
    val contentType: String,
    val fileSize: Long
)

@Serializable
data class PresignedUploadDto(
    val mediaId: String,
    val storageKey: String,
    val uploadUrl: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    val expiresIn: Int = 3600
)

@Serializable
data class ConfirmUploadRequest(
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val takenAt: Instant? = null
)
