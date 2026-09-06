package com.jameeli.thykra.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String,
    val mediaId: String,
    val authorId: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String? = null,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class AddCommentRequest(
    val body: String
)

@Serializable
data class UpdateCommentRequest(
    val body: String
)
