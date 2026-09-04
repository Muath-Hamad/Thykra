package com.jameeli.thykra

import com.jameeli.thykra.model.AlbumMemberDto
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberLeaveTest {

    @Test
    fun aMemberCanLeaveAnAlbumThemselves() = thykraTestApp {
        val owner = devLogin("owner-l1@test.dev", "Muath")
        val friend = devLogin("b-l1@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Salt")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val leave = client.delete("/api/albums/${album.id}/members/${friend.user.id}") {
            bearer(friend.accessToken)
        }
        assertEquals(HttpStatusCode.OK, leave.status)

        val members = client.get("/api/albums/${album.id}/members") {
            bearer(owner.accessToken)
        }.apiData<List<AlbumMemberDto>>()
        assertTrue(members.none { it.userId == friend.user.id })
    }

    @Test
    fun theOwnerCannotLeaveTheirOwnAlbum() = thykraTestApp {
        val owner = devLogin("owner-l2@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Karak")

        val leave = client.delete("/api/albums/${album.id}/members/${owner.user.id}") {
            bearer(owner.accessToken)
        }
        assertEquals(HttpStatusCode.Forbidden, leave.status)
        assertEquals("The owner cannot leave the album", leave.api<Unit>().error)
    }

    @Test
    fun aNonOwnerStillCannotRemoveSomeoneElse() = thykraTestApp {
        val owner = devLogin("owner-l3@test.dev", "Muath")
        val friendB = devLogin("b-l3@test.dev", "Sara")
        val friendC = devLogin("c-l3@test.dev", "Lina")
        val album = createAlbum(owner.accessToken, "Ajloun")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friendB.accessToken, invite.token).status)
        assertEquals(HttpStatusCode.OK, joinAlbum(friendC.accessToken, invite.token).status)

        val remove = client.delete("/api/albums/${album.id}/members/${friendC.user.id}") {
            bearer(friendB.accessToken)
        }
        assertEquals(HttpStatusCode.Forbidden, remove.status)
        assertEquals("Only the owner can remove members", remove.api<Unit>().error)
    }

    @Test
    fun aNonMemberCannotLeave() = thykraTestApp {
        val owner = devLogin("owner-l4@test.dev", "Muath")
        val stranger = devLogin("b-l4@test.dev", "Omar")
        val album = createAlbum(owner.accessToken, "Umm Qais")

        val leave = client.delete("/api/albums/${album.id}/members/${stranger.user.id}") {
            bearer(stranger.accessToken)
        }
        assertEquals(HttpStatusCode.Forbidden, leave.status)
        assertEquals("Not a member of this album", leave.api<Unit>().error)
    }
}
