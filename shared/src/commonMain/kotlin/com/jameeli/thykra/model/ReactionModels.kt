package com.jameeli.thykra.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ReactionType {
    LOVE,
    WOW,
    LAUGH,
    WISH_I_WAS_THERE,
    WANDERLUST,
    BEACH,
    MOUNTAIN,
    FOOD
}

@Serializable
data class ReactionDto(
    val id: String,
    val mediaId: String,
    val userId: String,
    val userDisplayName: String,
    val userAvatarUrl: String? = null,
    val type: ReactionType,
    val createdAt: Instant
)

@Serializable
data class ReactionSummaryDto(
    val type: ReactionType,
    val count: Int,
    val reactedByMe: Boolean
)

@Serializable
data class MediaReactionsDto(
    val summary: List<ReactionSummaryDto>,
    val reactions: List<ReactionDto>
)

@Serializable
data class AddReactionRequest(
    val type: ReactionType
)
