package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ActivityType { REACTION, COMMENT }

/**
 * Aggregated activity entry across all albums the current user is a member of.
 *
 * Either [reactionType] or [commentBody] is filled depending on [type] — the other is null.
 * Truncated server-side: [commentBody] is at most 200 chars.
 */
@Serializable
data class ActivityItemDto(
    val type: ActivityType,
    val createdAt: Instant,
    val albumId: String,
    val albumTitle: String,
    val mediaId: String,
    val mediaThumbnailUrl: String? = null,
    val actorId: String,
    val actorDisplayName: String,
    val actorAvatarUrl: String? = null,
    // For reactions: filled. For comments: null.
    val reactionType: ReactionType? = null,
    // For comments: filled (truncated server-side to 200 chars). For reactions: null.
    val commentBody: String? = null
)

@Serializable
data class RecentActivityDto(val items: List<ActivityItemDto>)
