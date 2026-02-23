package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MediaType { PHOTO, VIDEO }

@Serializable
enum class MediaStatus { PENDING, ACTIVE }

@Serializable
data class MediaDto(
    val id: String,
    val albumId: String,
    val uploaderId: String,
    val type: MediaType,
    val status: MediaStatus,
    val storageKey: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val filename: String,
    val contentType: String,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val takenAt: Instant? = null,
    val uploadedAt: Instant
)

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
