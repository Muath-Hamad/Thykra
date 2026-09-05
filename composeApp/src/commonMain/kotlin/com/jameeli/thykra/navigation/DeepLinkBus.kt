package com.jameeli.thykra.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform deep-link router.
 *
 * Platforms with native URL delivery — Android through an Intent, iOS through
 * `onOpenURL` — parse the URL into a [DeepLinkTarget] and push it here; the shell
 * collects [events] and navigates.
 *
 * A cold start with a link arrives *before* anyone is listening, so the flow buffers one
 * event and replays it when the shell subscribes. A link that arrives while signed out is
 * held in [pending] instead, because signing in destroys and rebuilds the graph and an
 * event in flight would be lost — which is exactly the WhatsApp-invite case the design
 * cares most about.
 */
sealed interface DeepLinkTarget {
    data object TripList : DeepLinkTarget
    data class Trip(val albumId: String) : DeepLinkTarget
    data class Media(val albumId: String, val mediaId: String) : DeepLinkTarget
    data class Invite(val token: String) : DeepLinkTarget
    data class Recap(val shareToken: String) : DeepLinkTarget

    /** True for the links a signed-out person is allowed to land on. */
    val reachableSignedOut: Boolean get() = this is Invite || this is Recap
}

object DeepLinkBus {

    private val _events = MutableSharedFlow<DeepLinkTarget>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<DeepLinkTarget> = _events.asSharedFlow()

    /**
     * The link the app should land on once it can. Survives the sign-in round trip, so
     * "open invite → sign in → land in the trip" works from a cold start.
     */
    private val _pending = MutableStateFlow<DeepLinkTarget?>(null)
    val pending: StateFlow<DeepLinkTarget?> = _pending.asStateFlow()

    fun emit(target: DeepLinkTarget) {
        _pending.value = target
        _events.tryEmit(target)
    }

    /** Called by the shell once it has actually navigated. */
    fun consume(target: DeepLinkTarget) {
        if (_pending.value == target) _pending.value = null
    }

    fun clearPending() {
        _pending.value = null
    }
}

/**
 * Parses an incoming URL into a [DeepLinkTarget]. Returns null for shapes we do not own,
 * so a caller can pass anything and ignore the answer safely.
 *
 * Both the custom scheme and the web origin resolve to the same targets, because the same
 * link has to work whether it came from a widget or from a message:
 *
 * ```
 * thykra://trips                             https://thykra.com/trips
 * thykra://trips/<id>                        https://thykra.com/trips/<id>
 * thykra://trips/<id>/media/<mediaId>        https://thykra.com/trips/<id>/media/<mediaId>
 * thykra://invite/<token>                    https://thykra.com/invite/<token>
 * thykra://r/<shareToken>                    https://thykra.com/r/<shareToken>
 * ```
 *
 * The `album`/`albums` spellings are the ones the shipped widgets already emit. They are
 * kept as aliases so an installed widget keeps working after this rename.
 */
fun parseDeepLink(url: String): DeepLinkTarget? {
    val path = when {
        url.startsWith("thykra://") -> url.removePrefix("thykra://")
        url.startsWith("https://thykra.com/") -> url.removePrefix("https://thykra.com/")
        url.startsWith("http://thykra.com/") -> url.removePrefix("http://thykra.com/")
        else -> return null
    }.substringBefore('?').substringBefore('#').trim('/')

    if (path.isEmpty()) return null
    val parts = path.split('/').filter { it.isNotBlank() }

    return when {
        parts.size == 1 && (parts[0] == "trips" || parts[0] == "albums") ->
            DeepLinkTarget.TripList

        parts.size == 2 && (parts[0] == "trips" || parts[0] == "album") ->
            DeepLinkTarget.Trip(parts[1])

        parts.size == 4 &&
            (parts[0] == "trips" || parts[0] == "album") &&
            parts[2] == "media" ->
            DeepLinkTarget.Media(albumId = parts[1], mediaId = parts[3])

        parts.size == 2 && parts[0] == "invite" -> DeepLinkTarget.Invite(parts[1])

        parts.size == 2 && parts[0] == "r" -> DeepLinkTarget.Recap(parts[1])

        else -> null
    }
}

/** Parse and emit in one call. The entry point the host platform holds. */
fun handleDeepLink(url: String): Boolean {
    val target = parseDeepLink(url) ?: return false
    DeepLinkBus.emit(target)
    return true
}
