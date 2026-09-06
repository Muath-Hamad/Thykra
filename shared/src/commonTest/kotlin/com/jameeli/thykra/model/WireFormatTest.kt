package com.jameeli.thykra.model

import com.jameeli.thykra.api.PendingUploadRecord
import kotlin.time.Instant
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Companion to SerializationTest: pins the wire shape of the DTOs it does not
 * already cover (media, auth, profile, upload persistence, enum names).
 */
class WireFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val instant = Instant.parse("2024-06-01T12:00:00Z")

    @Test
    fun media_dto_round_trips_with_and_without_optionals() {
        val full = MediaDto(
            id = "m1", albumId = "a1", uploaderId = "u1",
            type = MediaType.VIDEO, status = MediaStatus.ACTIVE,
            storageKey = "albums/a1/m1.mp4", url = "http://cdn/m1.mp4",
            thumbnailUrl = "http://cdn/m1-thumb.jpg",
            filename = "clip.mp4", contentType = "video/mp4", fileSize = 1234,
            width = 1920, height = 1080, durationMs = 12_000,
            takenAt = instant, uploadedAt = instant
        )
        val minimal = full.copy(
            thumbnailUrl = null, width = null, height = null, durationMs = null, takenAt = null
        )
        for (dto in listOf(full, minimal)) {
            val encoded = json.encodeToString(MediaDto.serializer(), dto)
            assertEquals(dto, json.decodeFromString(MediaDto.serializer(), encoded))
        }
    }

    @Test
    fun presigned_upload_dto_defaults_apply_when_fields_omitted() {
        val partial = """{"mediaId":"m1","storageKey":"k","uploadUrl":"http://u/x"}"""
        val decoded = json.decodeFromString(PresignedUploadDto.serializer(), partial)
        assertEquals("PUT", decoded.method)
        assertTrue(decoded.headers.isEmpty())
        assertEquals(3600, decoded.expiresIn)
    }

    @Test
    fun upload_request_dtos_tolerate_omitted_optionals() {
        val request = json.decodeFromString(
            RequestUploadUrlRequest.serializer(),
            """{"filename":"f.jpg","contentType":"image/jpeg","fileSize":10}"""
        )
        assertEquals("f.jpg", request.filename)

        val confirm = json.decodeFromString(ConfirmUploadRequest.serializer(), "{}")
        assertNull(confirm.width)
        assertNull(confirm.height)
        assertNull(confirm.durationMs)
        assertNull(confirm.takenAt)
    }

    @Test
    fun auth_response_and_token_response_round_trip() {
        val user = UserDto("u1", "a@b.c", "Alice", avatarUrl = null, createdAt = instant)
        val auth = AuthResponse("access", "refresh", expiresIn = 3600, user = user)
        val encodedAuth = json.encodeToString(AuthResponse.serializer(), auth)
        assertEquals(auth, json.decodeFromString(AuthResponse.serializer(), encodedAuth))

        val tokens = TokenResponse("access2", "refresh2", expiresIn = 3600)
        val encodedTokens = json.encodeToString(TokenResponse.serializer(), tokens)
        assertEquals(tokens, json.decodeFromString(TokenResponse.serializer(), encodedTokens))
    }

    @Test
    fun oauth_request_uses_enum_provider_names() {
        val request = OAuthRequest(OAuthProvider.GOOGLE, idToken = "tok")
        val encoded = json.encodeToString(OAuthRequest.serializer(), request)
        assertTrue(encoded.contains("\"provider\":\"GOOGLE\""))
        assertEquals(request, json.decodeFromString(OAuthRequest.serializer(), encoded))
    }

    @Test
    fun update_profile_request_supports_partial_updates() {
        val nameOnly = json.decodeFromString(UpdateProfileRequest.serializer(), """{"displayName":"New"}""")
        assertEquals("New", nameOnly.displayName)
        assertNull(nameOnly.avatarUrl)
        val empty = json.decodeFromString(UpdateProfileRequest.serializer(), "{}")
        assertNull(empty.displayName)
    }

    @Test
    fun pending_upload_record_round_trips() {
        val full = PendingUploadRecord(
            id = "upload_1", albumId = "a1", filename = "f.jpg",
            contentType = "image/jpeg", fileSize = 42,
            width = 800, height = 600, durationMs = null, takenAtMs = 1717243200000
        )
        val minimal = PendingUploadRecord(
            id = "upload_2", albumId = "a1", filename = "g.jpg",
            contentType = "image/jpeg", fileSize = 7
        )
        for (record in listOf(full, minimal)) {
            val encoded = json.encodeToString(PendingUploadRecord.serializer(), record)
            assertEquals(record, json.decodeFromString(PendingUploadRecord.serializer(), encoded))
        }
    }

    @Test
    fun api_response_error_envelope_round_trips() {
        val error = ApiResponse<String>(success = false, error = "Not a member of this album")
        val encoded = json.encodeToString(ApiResponse.serializer(String.serializer()), error)
        val decoded = json.decodeFromString(ApiResponse.serializer(String.serializer()), encoded)
        assertEquals(error, decoded)
        assertNull(decoded.data)
    }

    @Test
    fun enums_serialise_by_name() {
        assertEquals("\"PHOTO\"", json.encodeToString(MediaType.serializer(), MediaType.PHOTO))
        assertEquals("\"PENDING\"", json.encodeToString(MediaStatus.serializer(), MediaStatus.PENDING))
        assertEquals("\"OWNER\"", json.encodeToString(MemberRole.serializer(), MemberRole.OWNER))
        assertEquals("\"LINK_SHARED\"", json.encodeToString(AlbumVisibility.serializer(), AlbumVisibility.LINK_SHARED))
        assertEquals("\"WISH_I_WAS_THERE\"", json.encodeToString(ReactionType.serializer(), ReactionType.WISH_I_WAS_THERE))
    }

    @Test
    fun album_member_and_invite_dtos_round_trip() {
        val member = AlbumMemberDto("u1", "Alice", avatarUrl = null, role = MemberRole.CONTRIBUTOR, joinedAt = instant)
        val encodedMember = json.encodeToString(AlbumMemberDto.serializer(), member)
        assertEquals(member, json.decodeFromString(AlbumMemberDto.serializer(), encodedMember))

        val invite = InviteLinkDto("a1", token = "tok-123", expiresAt = instant)
        val encodedInvite = json.encodeToString(InviteLinkDto.serializer(), invite)
        assertEquals(invite, json.decodeFromString(InviteLinkDto.serializer(), encodedInvite))
    }
}
