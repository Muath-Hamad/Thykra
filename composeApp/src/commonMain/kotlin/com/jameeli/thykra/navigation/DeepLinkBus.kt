package com.jameeli.thykra.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-platform deep-link router.
 *
 * Platforms with native URL delivery (iOS via SceneDelegate / SwiftUI's
 * onOpenURL, Android via Intent) push parsed [DeepLinkTarget]s here; the
 * Compose [AppNavHost] collects from [events] and navigates accordingly.
 *
 * A SharedFlow with extraBufferCapacity = 1 lets us emit *before* anyone
 * subscribes (e.g. when a deep link arrives during cold launch); the buffered
 * event is replayed once AppNavHost wires up its collector.
 */
sealed interface DeepLinkTarget {
    data class Album(val albumId: String) : DeepLinkTarget
    data class Media(val albumId: String, val mediaId: String) : DeepLinkTarget
    object AlbumList : DeepLinkTarget
}

object DeepLinkBus {
    private val _events = MutableSharedFlow<DeepLinkTarget>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events: SharedFlow<DeepLinkTarget> = _events.asSharedFlow()

    fun emit(target: DeepLinkTarget) {
        _events.tryEmit(target)
    }
}

/**
 * Parses a `thykra://` URL into a [DeepLinkTarget]. Returns null for unknown
 * shapes so callers can ignore them safely.
 *
 * Recognised forms (matching the WidgetKit deep links):
 *   thykra://albums                              -> AlbumList
 *   thykra://album/<albumId>                     -> Album
 *   thykra://album/<albumId>/media/<mediaId>     -> Media
 */
fun parseDeepLink(url: String): DeepLinkTarget? {
    val prefix = "thykra://"
    if (!url.startsWith(prefix)) return null
    val path = url.removePrefix(prefix).trimEnd('/')
    if (path.isEmpty()) return null
    val parts = path.split('/')
    return when {
        parts.size == 1 && parts[0] == "albums" -> DeepLinkTarget.AlbumList
        parts.size == 2 && parts[0] == "album" -> DeepLinkTarget.Album(parts[1])
        parts.size == 4 && parts[0] == "album" && parts[2] == "media" ->
            DeepLinkTarget.Media(albumId = parts[1], mediaId = parts[3])
        else -> null
    }
}

/**
 * Top-level entry point exposed to the host platform (iOS Swift, Android
 * Activity). Parses + emits in one call.
 */
fun handleDeepLink(url: String): Boolean {
    val target = parseDeepLink(url) ?: return false
    DeepLinkBus.emit(target)
    return true
}
