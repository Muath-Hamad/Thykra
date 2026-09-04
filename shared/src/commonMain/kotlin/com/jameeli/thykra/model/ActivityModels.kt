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

// ---------------------------------------------------------------------------
// Aggregated activity feed (Wanderlust Editions) — served by GET /activity and
// GET /albums/{id}/activity. The legacy /activity/recent endpoint above stays as is.
// ---------------------------------------------------------------------------

@Serializable
enum class ActivityEventType { MEDIA_ADDED, MEMBER_JOINED, COMMENT, REACTION }

@Serializable
data class ActivityActorDto(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
data class ActivityAlbumDto(
    val id: String,
    val title: String
)

/**
 * One pre-aggregated activity event — "Sara added 12 photos", not twelve rows.
 * Events are merged server-side per actor per type per album within a 30-minute window.
 *
 * [count] is the number of raw actions merged into this event. [mediaIds] lists the
 * media involved (uploads, commented or reacted-to media). [thumbnailUrls] carries up
 * to 4 thumbnails for the row's cluster. [commentBody] (COMMENT only) is the newest
 * comment of the window, truncated to 200 chars. [reactionType] (REACTION only) is set
 * when all reactions in the window share one type.
 */
@Serializable
data class ActivityEventDto(
    val id: String,
    val type: ActivityEventType,
    val actor: ActivityActorDto,
    val album: ActivityAlbumDto,
    val createdAt: Instant,
    val count: Int? = null,
    val mediaIds: List<String> = emptyList(),
    val thumbnailUrls: List<String> = emptyList(),
    val commentBody: String? = null,
    val reactionType: ReactionType? = null
)

/**
 * A page of the aggregated feed. [nextCursor] is an opaque cursor (pass back as
 * ?cursor=) with "before" semantics; null when there is nothing older. [lastSeenAt]
 * is the caller's server-side read marker, if one was ever posted.
 */
@Serializable
data class ActivityFeedDto(
    val items: List<ActivityEventDto>,
    val nextCursor: String? = null,
    val lastSeenAt: Instant? = null
)

@Serializable
data class MarkActivitySeenRequest(
    val seenAt: Instant? = null
)

@Serializable
data class ActivitySeenDto(
    val seenAt: Instant
)
