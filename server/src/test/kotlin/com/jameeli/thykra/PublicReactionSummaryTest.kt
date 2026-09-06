package com.jameeli.thykra

import com.jameeli.thykra.model.PublicAlbumViewDto
import com.jameeli.thykra.model.ReactionType
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PublicReactionSummaryTest {

    @Test
    fun publicViewCarriesReactionCountsWithoutIdentities() = thykraTestApp {
        val owner = devLogin("owner-r1@test.dev", "Muath")
        val friendA = devLogin("a-r1@test.dev", "Sara")
        val friendB = devLogin("b-r1@test.dev", "Lina")
        val album = createAlbum(owner.accessToken, "Wadi Rum")

        val share = client.put("/api/albums/${album.id}") {
            bearer(owner.accessToken)
            jsonBody("""{"visibility":"LINK_SHARED"}""")
        }
        assertEquals(HttpStatusCode.OK, share.status)

        val now = Clock.System.now()
        val m1 = TestData.insertMedia(album.id, owner.user.id, now - 2.hours)
        val m2 = TestData.insertMedia(album.id, owner.user.id, now - 1.hours)

        TestData.insertReaction(m1, friendA.user.id, ReactionType.LOVE, now - 30.minutes)
        TestData.insertReaction(m1, friendB.user.id, ReactionType.LOVE, now - 25.minutes)
        TestData.insertReaction(m1, friendA.user.id, ReactionType.WOW, now - 20.minutes)

        // Public route, no auth.
        val view = client.get("/api/public/albums/${album.id}").apiData<PublicAlbumViewDto>()
        assertEquals(2, view.media.size)

        val media1 = view.media.single { it.id == m1 }
        val summary = media1.reactionSummary.associate { it.type to it.count }
        assertEquals(mapOf(ReactionType.LOVE to 2, ReactionType.WOW to 1), summary)
        // Sorted most-reacted first.
        assertEquals(ReactionType.LOVE, media1.reactionSummary.first().type)

        val media2 = view.media.single { it.id == m2 }
        assertTrue(media2.reactionSummary.isEmpty())
    }

    @Test
    fun privateAlbumStaysHidden() = thykraTestApp {
        val owner = devLogin("owner-r2@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Private Trip")
        val response = client.get("/api/public/albums/${album.id}")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
