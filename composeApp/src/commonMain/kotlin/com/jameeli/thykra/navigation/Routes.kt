package com.jameeli.thykra.navigation

import kotlinx.serialization.Serializable

/**
 * The route table from design part 3 §07 — twelve routes, four of them roots.
 *
 * "Trip", never "Album": the wire still says album because the server does, but nothing
 * the user can see or reach says it.
 */

// ── Roots. The four-tab bar shows on these and nowhere else. ───────────────────

/** The unauthenticated entry. Paper only, by rule. */
@Serializable
object Landing

@Serializable
object Trips

@Serializable
object ActivityFeed

@Serializable
object RecapsList

@Serializable
object Me

// ── Nested. The nav bar gives way to whatever chrome these bring. ──────────────

@Serializable
data class Trip(val albumId: String)

@Serializable
data class TripSettings(val albumId: String)

@Serializable
data class TripActivity(val albumId: String)

@Serializable
data class TripRecaps(val albumId: String)

@Serializable
data class Viewer(val albumId: String, val mediaId: String)

/** The story reader. Always Darkroom, always full screen. */
@Serializable
data class RecapReader(val recapId: String)

// ── Outside the shell entirely. ───────────────────────────────────────────────

/**
 * The invite preview. Full-screen, outside the tab bar, and reachable signed-out —
 * it is the growth loop, and asking someone to sign in before they can see what they
 * were invited to loses them.
 */
@Serializable
data class Invite(val token: String)

/**
 * The kit gallery — build step 02's checklist, drawn. Registered only when
 * [com.jameeli.thykra.KitGalleryEnabled] is true, which is a debug build. Nothing in the
 * app navigates to it and it never appears in a release binary.
 */
@Serializable
object KitGallery

/** The four roots, in bar order. */
val RootRoutes: List<Any> = listOf(Trips, ActivityFeed, RecapsList, Me)
