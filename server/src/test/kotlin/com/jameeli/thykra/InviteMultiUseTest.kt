package com.jameeli.thykra

import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.InvitePreviewDto
import com.jameeli.thykra.model.InviteStatus
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class InviteMultiUseTest {

    @Test
    fun twoDifferentUsersJoinTheSameLinkAndJoinCountIsTwo() = thykraTestApp {
        val owner = devLogin("owner-m1@test.dev", "Muath")
        val friendB = devLogin("b-m1@test.dev", "Sara")
        val friendC = devLogin("c-m1@test.dev", "Lina")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val invite = createInvite(owner.accessToken, album.id)

        // The same link works for many people — it must not die after the first join.
        val joinB = joinAlbum(friendB.accessToken, invite.token)
        assertEquals(HttpStatusCode.OK, joinB.status)
        assertEquals(album.id, joinB.apiData<AlbumDto>().id)

        val joinC = joinAlbum(friendC.accessToken, invite.token)
        assertEquals(HttpStatusCode.OK, joinC.status)

        val invites = client.get("/api/albums/${album.id}/invites") {
            bearer(owner.accessToken)
        }.apiData<List<InviteLinkDto>>()
        val listed = invites.single { it.token == invite.token }
        assertEquals(2, listed.joinCount)
    }

    @Test
    fun joiningTwiceWithTheSameUserConflicts() = thykraTestApp {
        val owner = devLogin("owner-m2@test.dev", "Muath")
        val friend = devLogin("b-m2@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Petra")
        val invite = createInvite(owner.accessToken, album.id)

        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)
        val second = joinAlbum(friend.accessToken, invite.token)
        assertEquals(HttpStatusCode.Conflict, second.status)
        assertEquals("Already a member of this album", second.api<Unit>().error)
    }

    @Test
    fun invitesListExcludesRevokedAndExpired() = thykraTestApp {
        val owner = devLogin("owner-m3@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Amman")
        val active = createInvite(owner.accessToken, album.id)
        val toRevoke = createInvite(owner.accessToken, album.id)
        TestData.insertInvite(album.id, owner.user.id, expiresAt = Clock.System.now() - 1.days)

        val revokeResponse = client.delete("/api/albums/${album.id}/invites/${toRevoke.token}") {
            bearer(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, revokeResponse.status)

        val invites = client.get("/api/albums/${album.id}/invites") {
            bearer(owner.accessToken)
        }.apiData<List<InviteLinkDto>>()
        assertEquals(listOf(active.token), invites.map { it.token })
        assertEquals(0, invites.single().joinCount)
    }

    @Test
    fun revokeIsOwnerOnly() = thykraTestApp {
        val owner = devLogin("owner-m4@test.dev", "Muath")
        val contributor = devLogin("b-m4@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Aqaba")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(contributor.accessToken, invite.token).status)

        // A contributor may list invites but not revoke them.
        val listAsContributor = client.get("/api/albums/${album.id}/invites") {
            bearer(contributor.accessToken)
        }
        assertEquals(HttpStatusCode.OK, listAsContributor.status)

        val revokeAsContributor = client.delete("/api/albums/${album.id}/invites/${invite.token}") {
            bearer(contributor.accessToken)
        }
        assertEquals(HttpStatusCode.Forbidden, revokeAsContributor.status)

        val revokeAsOwner = client.delete("/api/albums/${album.id}/invites/${invite.token}") {
            bearer(owner.accessToken)
        }
        assertEquals(HttpStatusCode.OK, revokeAsOwner.status)
    }

    @Test
    fun revokedLinkNoLongerJoinsAndPreviewsAsRevoked() = thykraTestApp {
        val owner = devLogin("owner-m5@test.dev", "Muath")
        val friend = devLogin("b-m5@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Dead Sea")
        val invite = createInvite(owner.accessToken, album.id)
        client.delete("/api/albums/${album.id}/invites/${invite.token}") {
            bearer(owner.accessToken)
        }.let { assertEquals(HttpStatusCode.OK, it.status) }

        val join = joinAlbum(friend.accessToken, invite.token)
        assertEquals(HttpStatusCode.BadRequest, join.status)
        assertEquals("Invalid or expired invite link", join.api<Unit>().error)

        val preview = client.get("/api/invites/${invite.token}/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.REVOKED, preview.status)
    }

    @Test
    fun revokeUnknownTokenIs404() = thykraTestApp {
        val owner = devLogin("owner-m6@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Jerash")
        val response = client.delete("/api/albums/${album.id}/invites/nope") {
            bearer(owner.accessToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun expiredLinkCannotBeJoined() = thykraTestApp {
        val owner = devLogin("owner-m7@test.dev", "Muath")
        val friend = devLogin("b-m7@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Madaba")
        val token = TestData.insertInvite(album.id, owner.user.id, expiresAt = Clock.System.now() - 1.days)

        val join = joinAlbum(friend.accessToken, token)
        assertEquals(HttpStatusCode.BadRequest, join.status)
        assertEquals("Invalid or expired invite link", join.api<Unit>().error)
    }

    @Test
    fun blockedUserCannotJoin() = thykraTestApp {
        val owner = devLogin("owner-m8@test.dev", "Muath")
        val blocked = devLogin("b-m8@test.dev", "Omar")
        val album = createAlbum(owner.accessToken, "Azraq")
        val invite = createInvite(owner.accessToken, album.id)
        client.post("/api/albums/${album.id}/blocks/${blocked.user.id}") {
            bearer(owner.accessToken)
        }.let { assertEquals(HttpStatusCode.OK, it.status) }

        val join = joinAlbum(blocked.accessToken, invite.token)
        assertEquals(HttpStatusCode.Forbidden, join.status)
        assertEquals("You have been blocked from this album", join.api<Unit>().error)
    }

    @Test
    fun joinCountSurvivesAndPreviewStaysValidAfterJoins() = thykraTestApp {
        val owner = devLogin("owner-m9@test.dev", "Muath")
        val friend = devLogin("b-m9@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Wadi Mujib")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        // Anonymous preview still VALID after someone joined — the link is not consumed.
        val preview = client.get("/api/invites/${invite.token}/preview").apiData<InvitePreviewDto>()
        assertEquals(InviteStatus.VALID, preview.status)
        assertNotNull(preview.album)

        val invites = client.get("/api/albums/${album.id}/invites") {
            bearer(owner.accessToken)
        }.apiData<List<InviteLinkDto>>()
        assertTrue(invites.single { it.token == invite.token }.joinCount == 1)
    }
}
