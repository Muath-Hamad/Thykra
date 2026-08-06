package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class RecapStatus { BUILDING, READY, FAILED }

/**
 * A generated edition of a trip — a cover, a title and 12-24 chosen plates,
 * shareable as its own public link (/r/{shareToken}).
 */
@Serializable
data class RecapDto(
    val id: String,
    val albumId: String,
    val albumTitle: String,
    val title: String,
    val coverMediaId: String? = null,
    val coverUrl: String? = null,
    val mediaIds: List<String> = emptyList(),
    val periodStart: Instant? = null,
    val periodEnd: Instant? = null,
    val status: RecapStatus,
    val shareToken: String? = null,
    val createdAt: Instant
)

/**
 * Public, unauthenticated recap reader payload. [media] is the chosen items in
 * [RecapDto.mediaIds] order, with reaction counts (no identities). [contributors]
 * are only the uploaders of the chosen media — never the full member list.
 */
@Serializable
data class RecapViewDto(
    val recap: RecapDto,
    val media: List<PublicMediaDto> = emptyList(),
    val ownerDisplayName: String,
    val contributors: List<AlbumMemberSummary> = emptyList()
)
