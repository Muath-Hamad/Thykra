package com.jameeli.thykra

import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.model.RecapDto
import com.jameeli.thykra.model.RecapStatus
import com.jameeli.thykra.model.RecapViewDto
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RecapsTest {

    @Test
    fun buildIsGatedWithNeededCountBelowTwelveMedia() = thykraTestApp {
        val owner = devLogin("owner-c1@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Short Trip")
        val now = Clock.System.now()
        repeat(5) { i -> TestData.insertMedia(album.id, owner.user.id, now - i.hours) }

        val response = client.post("/api/albums/${album.id}/recaps") { bearer(owner.accessToken) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        // The client parses the count out of the error string for the honest gate copy.
        assertEquals("NOT_ENOUGH_MEDIA:7", response.api<Unit>().error)
    }

    @Test
    fun buildChoosesSpreadAcrossDaysWithCoverTitleAndPeriod() = thykraTestApp {
        val owner = devLogin("owner-c2@test.dev", "Muath")
        val friend = devLogin("friend-c2@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        // 30 media across 4 UTC calendar days (8+8+8+6), takenAt controls the day.
        val day1 = Instant.parse("2026-04-10T08:00:00Z")
        val day2 = Instant.parse("2026-04-11T08:00:00Z")
        val day3 = Instant.parse("2026-04-12T08:00:00Z")
        val day4 = Instant.parse("2026-04-13T08:00:00Z")
        val uploadedAt = Clock.System.now() - 2.hours
        val idsByDay = mutableMapOf<Instant, MutableList<String>>()
        listOf(day1 to 8, day2 to 8, day3 to 8, day4 to 6).forEach { (day, count) ->
            repeat(count) { i ->
                val uploader = if (i % 2 == 0) owner.user.id else friend.user.id
                val id = TestData.insertMedia(
                    album.id, uploader,
                    uploadedAt = uploadedAt,
                    takenAt = day + (i * 10).minutes
                )
                idsByDay.getOrPut(day) { mutableListOf() }.add(id)
            }
        }
        // Make one specific item the clear reaction winner -> cover.
        val star = idsByDay.getValue(day2)[3]
        val now = Clock.System.now()
        TestData.insertReaction(star, owner.user.id, ReactionType.LOVE, now - 30.minutes)
        TestData.insertReaction(star, friend.user.id, ReactionType.LOVE, now - 29.minutes)
        TestData.insertReaction(star, friend.user.id, ReactionType.WOW, now - 28.minutes)

        val response = client.post("/api/albums/${album.id}/recaps") { bearer(owner.accessToken) }
        assertEquals(HttpStatusCode.Created, response.status)
        val recap = response.apiData<RecapDto>()

        assertEquals(RecapStatus.READY, recap.status)
        assertEquals("4 days in Wadi Rum", recap.title)
        assertEquals(album.id, recap.albumId)
        assertEquals("Wadi Rum", recap.albumTitle)
        assertEquals(24, recap.mediaIds.size)
        assertEquals(star, recap.coverMediaId)
        assertNotNull(recap.coverUrl)
        assertNotNull(recap.shareToken)
        assertEquals(day1, recap.periodStart)
        assertEquals(day4 + 50.minutes, recap.periodEnd)
        assertTrue(recap.mediaIds.contains(star))

        // Every day of the trip is represented in the chosen plates.
        val chosen = recap.mediaIds.toSet()
        idsByDay.forEach { (_, dayIds) ->
            assertTrue(dayIds.any { it in chosen }, "each day must contribute at least one plate")
        }
    }

    @Test
    fun singleDayTripKeepsAlbumTitleAndAllMedia() = thykraTestApp {
        val owner = devLogin("owner-c3@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Beach Day")
        val day = Instant.parse("2026-05-01T09:00:00Z")
        repeat(12) { i ->
            TestData.insertMedia(
                album.id, owner.user.id,
                uploadedAt = Clock.System.now() - 1.hours,
                takenAt = day + (i * 5).minutes
            )
        }
        val recap = client.post("/api/albums/${album.id}/recaps") {
            bearer(owner.accessToken)
        }.apiData<RecapDto>()
        assertEquals("Beach Day", recap.title)
        assertEquals(12, recap.mediaIds.size)
    }

    @Test
    fun recapListsForUserAndAlbumWithAccessControl() = thykraTestApp {
        val owner = devLogin("owner-c4@test.dev", "Muath")
        val friend = devLogin("friend-c4@test.dev", "Sara")
        val stranger = devLogin("stranger-c4@test.dev", "Nadia")
        val album = createAlbum(owner.accessToken, "Petra")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)
        val day = Instant.parse("2026-06-01T09:00:00Z")
        repeat(12) { i ->
            TestData.insertMedia(album.id, owner.user.id, Clock.System.now() - 1.hours, takenAt = day + i.minutes)
        }
        val recap = client.post("/api/albums/${album.id}/recaps") {
            bearer(owner.accessToken)
        }.apiData<RecapDto>()

        // Across all my trips — both members see it, newest first.
        val mine = client.get("/api/recaps") { bearer(owner.accessToken) }.apiData<List<RecapDto>>()
        assertEquals(listOf(recap.id), mine.map { it.id })
        val friends = client.get("/api/recaps") { bearer(friend.accessToken) }.apiData<List<RecapDto>>()
        assertEquals(listOf(recap.id), friends.map { it.id })
        val strangers = client.get("/api/recaps") { bearer(stranger.accessToken) }.apiData<List<RecapDto>>()
        assertTrue(strangers.isEmpty())

        // Per-album list is member-only.
        val perAlbum = client.get("/api/albums/${album.id}/recaps") {
            bearer(friend.accessToken)
        }.apiData<List<RecapDto>>()
        assertEquals(listOf(recap.id), perAlbum.map { it.id })
        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/api/albums/${album.id}/recaps") { bearer(stranger.accessToken) }.status
        )
        // Building is member-only too.
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post("/api/albums/${album.id}/recaps") { bearer(stranger.accessToken) }.status
        )
    }

    @Test
    fun publicReaderReturnsMediaInOrderWithContributors() = thykraTestApp {
        val owner = devLogin("owner-c5@test.dev", "Muath")
        val friend = devLogin("friend-c5@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Amman")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val day1 = Instant.parse("2026-07-01T09:00:00Z")
        val day2 = Instant.parse("2026-07-02T09:00:00Z")
        val mediaIds = mutableListOf<String>()
        repeat(12) { i ->
            val uploader = if (i % 2 == 0) owner.user.id else friend.user.id
            val day = if (i < 6) day1 else day2
            mediaIds.add(
                TestData.insertMedia(album.id, uploader, Clock.System.now() - 1.hours, takenAt = day + i.minutes)
            )
        }
        TestData.insertReaction(mediaIds[0], friend.user.id, ReactionType.LOVE, Clock.System.now() - 10.minutes)

        val recap = client.post("/api/albums/${album.id}/recaps") {
            bearer(owner.accessToken)
        }.apiData<RecapDto>()

        // Public reader — no auth, works even though the album is PRIVATE.
        val view = client.get("/api/r/${recap.shareToken}").apiData<RecapViewDto>()
        assertEquals(recap.id, view.recap.id)
        assertEquals(recap.mediaIds, view.media.map { it.id })
        assertEquals("Muath", view.ownerDisplayName)
        assertEquals(
            setOf("Muath", "Sara"),
            view.contributors.map { it.displayName }.toSet()
        )
        // Reaction counts (no identities) ride along on the chosen media.
        val reacted = view.media.single { it.id == mediaIds[0] }
        assertEquals(1, reacted.reactionSummary.single().count)
        assertEquals(ReactionType.LOVE, reacted.reactionSummary.single().type)
    }

    @Test
    fun unknownShareTokenIs404() = thykraTestApp {
        val response = client.get("/api/r/not-a-real-token")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Recap not found", response.api<Unit>().error)
    }
}
