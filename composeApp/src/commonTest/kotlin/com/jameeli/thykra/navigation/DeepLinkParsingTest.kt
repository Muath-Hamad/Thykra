package com.jameeli.thykra.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The invite loop's first hop. A link pasted into a group chat has to resolve to the same
 * place whether it arrived as a `thykra://` scheme from a widget or as an `https://`
 * App Link from a message, so both spellings are tested against the same targets.
 */
class DeepLinkParsingTest {

    @Test
    fun `custom scheme resolves every route`() {
        assertEquals(DeepLinkTarget.TripList, parseDeepLink("thykra://trips"))
        assertEquals(DeepLinkTarget.Trip("a1"), parseDeepLink("thykra://trips/a1"))
        assertEquals(
            DeepLinkTarget.Media("a1", "m2"),
            parseDeepLink("thykra://trips/a1/media/m2"),
        )
        assertEquals(DeepLinkTarget.Invite("tok"), parseDeepLink("thykra://invite/tok"))
        assertEquals(DeepLinkTarget.Recap("share"), parseDeepLink("thykra://r/share"))
    }

    @Test
    fun `web origin resolves to the same targets`() {
        assertEquals(DeepLinkTarget.TripList, parseDeepLink("https://thykra.com/trips"))
        assertEquals(DeepLinkTarget.Trip("a1"), parseDeepLink("https://thykra.com/trips/a1"))
        assertEquals(
            DeepLinkTarget.Media("a1", "m2"),
            parseDeepLink("https://thykra.com/trips/a1/media/m2"),
        )
        assertEquals(DeepLinkTarget.Invite("tok"), parseDeepLink("https://thykra.com/invite/tok"))
        assertEquals(DeepLinkTarget.Recap("share"), parseDeepLink("https://thykra.com/r/share"))
    }

    /**
     * The shipped widgets emit `album` and `albums`. Renaming the route must not break an
     * already-installed widget, so both spellings still resolve.
     */
    @Test
    fun `the widget spellings still resolve`() {
        assertEquals(DeepLinkTarget.TripList, parseDeepLink("thykra://albums"))
        assertEquals(DeepLinkTarget.Trip("a1"), parseDeepLink("thykra://album/a1"))
        assertEquals(
            DeepLinkTarget.Media("a1", "m2"),
            parseDeepLink("thykra://album/a1/media/m2"),
        )
    }

    @Test
    fun `query strings, fragments and trailing slashes are ignored`() {
        assertEquals(
            DeepLinkTarget.Invite("tok"),
            parseDeepLink("https://thykra.com/invite/tok?utm_source=whatsapp"),
        )
        assertEquals(DeepLinkTarget.Invite("tok"), parseDeepLink("https://thykra.com/invite/tok/"))
        assertEquals(DeepLinkTarget.Invite("tok"), parseDeepLink("thykra://invite/tok#top"))
    }

    @Test
    fun `shapes we do not own return null rather than throwing`() {
        assertNull(parseDeepLink("https://example.com/invite/tok"))
        assertNull(parseDeepLink("thykra://"))
        assertNull(parseDeepLink("thykra://nonsense"))
        assertNull(parseDeepLink("thykra://trips/a1/unknown/x"))
        assertNull(parseDeepLink(""))
        assertNull(parseDeepLink("not a url at all"))
    }

    /**
     * Only an invite and a public recap may be seen without a session. Everything else
     * waits behind sign-in — and is held, not dropped, so the round trip lands.
     */
    @Test
    fun `only invite and recap are reachable signed out`() {
        assertTrue(DeepLinkTarget.Invite("t").reachableSignedOut)
        assertTrue(DeepLinkTarget.Recap("t").reachableSignedOut)
        assertFalse(DeepLinkTarget.TripList.reachableSignedOut)
        assertFalse(DeepLinkTarget.Trip("a").reachableSignedOut)
        assertFalse(DeepLinkTarget.Media("a", "m").reachableSignedOut)
    }

    @Test
    fun `the bus holds a link until it is consumed`() {
        DeepLinkBus.clearPending()
        assertNull(DeepLinkBus.pending.value)

        val target = DeepLinkTarget.Invite("tok")
        DeepLinkBus.emit(target)
        assertEquals(target, DeepLinkBus.pending.value)

        // A different target must not clear someone else's pending link.
        DeepLinkBus.consume(DeepLinkTarget.Invite("other"))
        assertEquals(target, DeepLinkBus.pending.value)

        DeepLinkBus.consume(target)
        assertNull(DeepLinkBus.pending.value)
    }

    @Test
    fun `handleDeepLink reports whether it recognised the url`() {
        DeepLinkBus.clearPending()
        assertTrue(handleDeepLink("thykra://trips/a1"))
        assertFalse(handleDeepLink("https://example.com/whatever"))
        DeepLinkBus.clearPending()
    }
}
