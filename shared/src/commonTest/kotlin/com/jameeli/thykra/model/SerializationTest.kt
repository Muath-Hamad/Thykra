package com.jameeli.thykra.model

import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip tests for the wire DTOs. The server and clients all use this exact
 * serialised form; renaming a field here is a wire-protocol break. These tests
 * also pin the JSON shape so a refactor doesn't silently drop optional fields.
 */
class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun album_dto_round_trips_with_all_fields() {
        val album = AlbumDto(
            id = "alb-1",
            ownerId = "u-1",
            title = "Trip to Petra",
            description = "Family trip",
            coverUrl = "http://example/cover.jpg",
            visibility = AlbumVisibility.LINK_SHARED,
            memberCount = 3,
            previewMembers = listOf(
                AlbumMemberSummary("u-1", "Alice", "http://example/a.jpg"),
                AlbumMemberSummary("u-2", "Bob", null)
            ),
            createdAt = Instant.parse("2024-01-01T00:00:00Z")
        )
        val encoded = json.encodeToString(AlbumDto.serializer(), album)
        val decoded = json.decodeFromString(AlbumDto.serializer(), encoded)
        assertEquals(album, decoded)
    }

    @Test
    fun album_dto_visibility_defaults_to_private_when_missing() {
        val partial = """
            {"id":"a","ownerId":"u","title":"t","memberCount":1,"createdAt":"2024-01-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString(AlbumDto.serializer(), partial)
        assertEquals(AlbumVisibility.PRIVATE, decoded.visibility)
    }

    @Test
    fun reaction_summary_marks_user_reaction() {
        val summary = ReactionSummaryDto(ReactionType.WANDERLUST, count = 4, reactedByMe = true)
        val encoded = json.encodeToString(ReactionSummaryDto.serializer(), summary)
        assertTrue(encoded.contains("\"reactedByMe\":true"))
        assertTrue(encoded.contains("\"type\":\"WANDERLUST\""))
    }

    @Test
    fun comment_dto_preserves_avatar_optional() {
        val withAvatar = CommentDto(
            id = "c1", mediaId = "m1", authorId = "u1",
            authorDisplayName = "Alice", authorAvatarUrl = "http://x/a.jpg",
            body = "hi", createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T00:00:00Z")
        )
        val withoutAvatar = withAvatar.copy(authorAvatarUrl = null)
        val a = json.encodeToString(CommentDto.serializer(), withAvatar)
        val b = json.encodeToString(CommentDto.serializer(), withoutAvatar)
        assertEquals(withAvatar, json.decodeFromString(CommentDto.serializer(), a))
        assertEquals(withoutAvatar, json.decodeFromString(CommentDto.serializer(), b))
    }

    @Test
    fun create_invite_request_accepts_omitted_expiry() {
        val empty = json.decodeFromString(CreateInviteRequest.serializer(), "{}")
        assertEquals(null, empty.expiresInDays)
        val explicit = json.decodeFromString(CreateInviteRequest.serializer(), """{"expiresInDays":30}""")
        assertEquals(30, explicit.expiresInDays)
    }

    @Test
    fun api_response_round_trip_with_typed_payload() {
        val ok = ApiResponse(success = true, data = "hello")
        val encoded = json.encodeToString(ApiResponse.serializer(String.serializer()), ok)
        val decoded = json.decodeFromString(ApiResponse.serializer(String.serializer()), encoded)
        assertEquals(ok, decoded)
    }

    @Test
    fun activity_item_decodes_reaction_and_comment_variants() {
        val instant = "2024-01-01T00:00:00Z"
        val reaction = ActivityItemDto(
            type = ActivityType.REACTION,
            createdAt = Instant.parse(instant),
            albumId = "a", albumTitle = "T",
            mediaId = "m", mediaThumbnailUrl = null,
            actorId = "u", actorDisplayName = "Alice", actorAvatarUrl = null,
            reactionType = ReactionType.LOVE,
            commentBody = null
        )
        val comment = reaction.copy(
            type = ActivityType.COMMENT,
            reactionType = null,
            commentBody = "nice"
        )
        for (item in listOf(reaction, comment)) {
            val s = json.encodeToString(ActivityItemDto.serializer(), item)
            assertEquals(item, json.decodeFromString(ActivityItemDto.serializer(), s))
        }
    }
}

