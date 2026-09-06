package com.jameeli.thykra

import com.jameeli.thykra.model.InvitePreviewDto
import com.jameeli.thykra.model.InviteStatus
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class InvitePreviewTest {

    @Test
    fun validInviteAnonymousPreview() = thykraTestApp {
        val owner = devLogin("owner-p1@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        TestData.insertMedia(album.id, owner.user.id, Clock.System.now() - 2.hours)
        TestData.insertMedia(album.id, owner.user.id, Clock.System.now() - 1.hours)
        val invite = createInvite(owner.accessToken, album.id)

        // No Authorization header at all — the endpoint must work unauthenticated.
        val response = client.get("/api/invites/${invite.token}/preview")
        assertEquals(HttpStatusCode.OK, response.status)
        val preview = response.apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.VALID, preview.status)
        assertEquals("Wadi Rum", preview.album?.title)
        assertEquals("Muath", preview.invitedBy?.displayName)
        assertEquals(2, preview.mediaCount)
        assertNotNull(preview.expiresAt)
        assertEquals(2, preview.previewMedia.size)
        assertTrue(preview.previewMedia.all { it.thumbnailUrl.isNotBlank() })
    }

    @Test
    fun previewMediaIsCappedAtThree() = thykraTestApp {
        val owner = devLogin("owner-p2@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Amman")
        repeat(5) { i ->
            TestData.insertMedia(album.id, owner.user.id, Clock.System.now() - i.hours)
        }
        val invite = createInvite(owner.accessToken, album.id)
        val preview = client.get("/api/invites/${invite.token}/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.VALID, preview.status)
        assertEquals(5, preview.mediaCount)
        assertEquals(3, preview.previewMedia.size)
    }

    @Test
    fun unknownTokenIsRevokedDeadEndWithNothingElse() = thykraTestApp {
        val response = client.get("/api/invites/definitely-not-a-token/preview")
        assertEquals(HttpStatusCode.OK, response.status)
        val preview = response.apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.REVOKED, preview.status)
        assertNull(preview.album)
        assertNull(preview.invitedBy)
        assertNull(preview.mediaCount)
        assertNull(preview.expiresAt)
        assertTrue(preview.previewMedia.isEmpty())
    }

    @Test
    fun revokedTokenIsRevokedDeadEnd() = thykraTestApp {
        val owner = devLogin("owner-p3@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Petra")
        val token = TestData.insertInvite(
            album.id, owner.user.id,
            expiresAt = Clock.System.now() + 7.days,
            revokedAt = Clock.System.now() - 1.hours
        )
        val preview = client.get("/api/invites/$token/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.REVOKED, preview.status)
        assertNull(preview.album)
        assertNull(preview.invitedBy)
    }

    @Test
    fun expiredInviteStillNamesTheTrip() = thykraTestApp {
        val owner = devLogin("owner-p4@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val token = TestData.insertInvite(
            album.id, owner.user.id,
            expiresAt = Clock.System.now() - 1.days
        )
        val preview = client.get("/api/invites/$token/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.EXPIRED, preview.status)
        // "It was for Wadi Rum" — album, inviter and expiry survive expiry.
        assertEquals("Wadi Rum", preview.album?.title)
        assertEquals("Muath", preview.invitedBy?.displayName)
        assertNotNull(preview.expiresAt)
    }

    @Test
    fun alreadyMemberSeesReEntryWithJoinedAt() = thykraTestApp {
        val owner = devLogin("owner-p5@test.dev", "Muath")
        val friend = devLogin("friend-p5@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val preview = client.get("/api/invites/${invite.token}/preview") {
            bearer(friend.accessToken)
        }.apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.ALREADY_MEMBER, preview.status)
        assertEquals("Wadi Rum", preview.album?.title)
        assertNotNull(preview.joinedAt)
    }

    @Test
    fun blockedUserGetsNoAlbumDetailsAtAll() = thykraTestApp {
        val owner = devLogin("owner-p6@test.dev", "Muath")
        val blocked = devLogin("blocked-p6@test.dev", "Omar")
        val album = createAlbum(owner.accessToken, "Secret Trip")
        val invite = createInvite(owner.accessToken, album.id)
        val blockResponse = client.post("/api/albums/${album.id}/blocks/${blocked.user.id}") {
            bearer(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, blockResponse.status)

        val preview = client.get("/api/invites/${invite.token}/preview") {
            bearer(blocked.accessToken)
        }.apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.BLOCKED, preview.status)
        // The design is explicit: no trip name, no cover, nothing.
        assertNull(preview.album)
        assertNull(preview.invitedBy)
        assertNull(preview.mediaCount)
        assertNull(preview.expiresAt)
        assertTrue(preview.previewMedia.isEmpty())

        // The same token stays VALID for an anonymous caller.
        val anonymous = client.get("/api/invites/${invite.token}/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.VALID, anonymous.status)
    }
}
