package com.jameeli.thykra.ui.share

/**
 * Platform-specific share sheet entry point. Implementations present the
 * native OS share UI (e.g. iOS UIActivityViewController, Android Intent
 * chooser) so the user can send the supplied text to messages, mail,
 * other installed apps, the system clipboard, etc.
 *
 * Currently used by the album settings screen to share an album's public
 * link when its visibility is [com.jameeli.thykra.model.AlbumVisibility.LINK_SHARED].
 */
expect fun shareText(content: String)
