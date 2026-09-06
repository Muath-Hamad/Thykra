package com.jameeli.thykra

import com.jameeli.thykra.model.ActivityEventType
import com.jameeli.thykra.model.ActivityFeedDto
import com.jameeli.thykra.model.ActivitySeenDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.ReactionType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ActivityFeedTest {

    @Test
    fun uploadsWithinThirtyMinutesMergeIntoOneEventAndFartherOnesSplit() = thykraTestApp {
        val owner = devLogin("owner-a1@test.dev", "Muath")
        val friend = devLogin("friend-a1@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val base = Clock.System.now() - 1.days
        // Burst of 3 uploads within 30 minutes -> ONE event with count=3.
        val m1 = TestData.insertMedia(album.id, friend.user.id, base)
        val m2 = TestData.insertMedia(album.id, friend.user.id, base + 5.minutes)
        val m3 = TestData.insertMedia(album.id, friend.user.id, base + 10.minutes)
        // A fourth upload 2 hours earlier -> separate event.
        val earlier = TestData.insertMedia(album.id, friend.user.id, base - 2.hours)

        val feed = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
        val mediaEvents = feed.items.filter { it.type == ActivityEventType.MEDIA_ADDED }
        assertEquals(2, mediaEvents.size)

        val burst = mediaEvents.first()
        assertEquals(3, burst.count)
        assertEquals(setOf(m1, m2, m3), burst.mediaIds.toSet())
        assertEquals((base + 10.minutes).dbPrecision(), burst.createdAt)
        assertEquals(friend.user.id, burst.actor.userId)
        assertEquals(album.id, burst.album.id)
        assertEquals("Wadi Rum", burst.album.title)
        assertTrue(burst.thumbnailUrls.isNotEmpty())

        val single = mediaEvents.last()
        assertEquals(1, single.count)
        assertEquals(listOf(earlier), single.mediaIds)
    }

    @Test
    fun globalFeedExcludesSelfButPerAlbumIncludesEveryone() = thykraTestApp {
        val owner = devLogin("owner-a2@test.dev", "Muath")
        val friend = devLogin("friend-a2@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Petra")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val base = Clock.System.now() - 1.days
        TestData.insertMedia(album.id, owner.user.id, base)
        TestData.insertMedia(album.id, friend.user.id, base + 2.hours)

        // Global: the owner's own upload is not news to the owner.
        val global = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
        val globalMedia = global.items.filter { it.type == ActivityEventType.MEDIA_ADDED }
        assertEquals(1, globalMedia.size)
        assertEquals(friend.user.id, globalMedia.single().actor.userId)

        // Per-album: the trip log shows everyone, the caller included.
        val perAlbum = client.get("/api/albums/${album.id}/activity") {
            bearer(owner.accessToken)
        }.apiData<ActivityFeedDto>()
        val perAlbumMedia = perAlbum.items.filter { it.type == ActivityEventType.MEDIA_ADDED }
        assertEquals(setOf(owner.user.id, friend.user.id), perAlbumMedia.map { it.actor.userId }.toSet())
    }

    @Test
    fun memberJoinedAppearsButOwnersCreationJoinDoesNot() = thykraTestApp {
        val owner = devLogin("owner-a3@test.dev", "Muath")
        val friend = devLogin("friend-a3@test.dev", "Hala")
        val album = createAlbum(owner.accessToken, "Amman")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val feed = client.get("/api/albums/${album.id}/activity") {
            bearer(owner.accessToken)
        }.apiData<ActivityFeedDto>()
        val joins = feed.items.filter { it.type == ActivityEventType.MEMBER_JOINED }
        assertEquals(1, joins.size)
        assertEquals(friend.user.id, joins.single().actor.userId)
        assertEquals("Hala", joins.single().actor.displayName)
    }

    @Test
    fun commentsAndReactionsAggregateWithBodyTruncationAndMediaIds() = thykraTestApp {
        val owner = devLogin("owner-a4@test.dev", "Muath")
        val friend = devLogin("friend-a4@test.dev", "Lina")
        val album = createAlbum(owner.accessToken, "Aqaba")
        val invite = createInvite(owner.accessToken, album.id)
        assertEquals(HttpStatusCode.OK, joinAlbum(friend.accessToken, invite.token).status)

        val base = Clock.System.now() - 1.days
        val m1 = TestData.insertMedia(album.id, owner.user.id, base - 1.hours)
        val m2 = TestData.insertMedia(album.id, owner.user.id, base - 1.hours + 1.minutes)
        val m3 = TestData.insertMedia(album.id, owner.user.id, base - 1.hours + 2.minutes)

        val longBody = "x".repeat(300)
        TestData.insertComment(m1, friend.user.id, "first", base)
        TestData.insertComment(m2, friend.user.id, longBody, base + 3.minutes)

        TestData.insertReaction(m1, friend.user.id, ReactionType.BEACH, base + 10.minutes)
        TestData.insertReaction(m2, friend.user.id, ReactionType.BEACH, base + 12.minutes)
        TestData.insertReaction(m3, friend.user.id, ReactionType.BEACH, base + 14.minutes)

        val feed = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()

        val comment = feed.items.single { it.type == ActivityEventType.COMMENT }
        assertEquals(2, comment.count)
        assertEquals(setOf(m1, m2), comment.mediaIds.toSet())
        // The newest comment's body, truncated to 200 chars.
        assertEquals(200, comment.commentBody?.length)

        val reaction = feed.items.single { it.type == ActivityEventType.REACTION }
        assertEquals(3, reaction.count)
        assertEquals(setOf(m1, m2, m3), reaction.mediaIds.toSet())
        assertEquals(ReactionType.BEACH, reaction.reactionType)
    }

    @Test
    fun cursorPagingWalksOlderPagesWithoutOverlap() = thykraTestApp {
        val owner = devLogin("owner-a5@test.dev", "Muath")
        val friend = devLogin("friend-a5@test.dev", "Sara")
        val album = createAlbum(owner.accessToken, "Jerash")
        TestData.insertMember(album.id, friend.user.id, MemberRole.CONTRIBUTOR, Clock.System.now() - 30.days)

        val base = Clock.System.now() - 1.days
        // 5 single uploads, 2 hours apart -> 5 aggregates, plus 1 MEMBER_JOINED = 6 events.
        repeat(5) { i ->
            TestData.insertMedia(album.id, friend.user.id, base - (i * 2).hours)
        }

        val seenIds = mutableListOf<String>()
        val seenCreatedAt = mutableListOf<Instant>()
        var cursor: String? = null
        var pages = 0
        do {
            val url = buildString {
                append("/api/activity?limit=2")
                if (cursor != null) append("&cursor=$cursor")
            }
            val feed = client.get(url) { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
            assertTrue(feed.items.size <= 2)
            feed.items.forEach {
                seenIds.add(it.id)
                seenCreatedAt.add(it.createdAt)
            }
            cursor = feed.nextCursor
            pages++
        } while (cursor != null && pages < 10)

        assertEquals(6, seenIds.size)
        assertEquals(seenIds.toSet().size, seenIds.size, "pages must not overlap")
        assertEquals(seenCreatedAt.sortedDescending(), seenCreatedAt, "feed must be newest-first across pages")
        assertTrue(pages >= 3)
    }

    @Test
    fun perAlbumFeedIsMemberOnly() = thykraTestApp {
        val owner = devLogin("owner-a6@test.dev", "Muath")
        val stranger = devLogin("stranger-a6@test.dev", "Nadia")
        val album = createAlbum(owner.accessToken, "Dead Sea")

        val response = client.get("/api/albums/${album.id}/activity") { bearer(stranger.accessToken) }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun feedRequiresAuth() = thykraTestApp {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/activity").status)
    }

    @Test
    fun seenMarkerRoundTrip() = thykraTestApp {
        val owner = devLogin("owner-a7@test.dev", "Muath")
        createAlbum(owner.accessToken, "Azraq")

        // Before any marker, lastSeenAt is null.
        val before = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
        assertNull(before.lastSeenAt)

        val explicit = Instant.parse("2026-08-01T10:00:00Z")
        val marked = client.post("/api/activity/seen") {
            bearer(owner.accessToken)
            jsonBody("""{"seenAt":"$explicit"}""")
        }.apiData<ActivitySeenDto>()
        assertEquals(explicit, marked.seenAt)

        val after = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
        assertEquals(explicit, after.lastSeenAt)

        // Empty body defaults to "now" and upserts over the previous value.
        val remarked = client.post("/api/activity/seen") {
            bearer(owner.accessToken)
            jsonBody("{}")
        }.apiData<ActivitySeenDto>()
        assertTrue(remarked.seenAt > explicit)

        val latest = client.get("/api/activity") { bearer(owner.accessToken) }.apiData<ActivityFeedDto>()
        assertNotNull(latest.lastSeenAt)
        assertEquals(remarked.seenAt.dbPrecision(), latest.lastSeenAt)
    }

    @Test
    fun legacyRecentEndpointStillWorks() = thykraTestApp {
        val owner = devLogin("owner-a8@test.dev", "Muath")
        val response = client.get("/api/activity/recent") { bearer(owner.accessToken) }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
