package com.jameeli.thykra.ui.share

import android.content.Context
import android.content.Intent

/**
 * Process-wide bridge that lets the cross-platform [shareText] entry point obtain
 * an Android [Context] without taking one as a parameter. [MainActivity] sets this
 * from its `onCreate` (using the application context — never the activity itself —
 * so we don't leak the activity).
 */
object SharingHost {
    @Volatile
    var appContext: Context? = null
}

/**
 * Android implementation of the cross-platform [shareText] entry point. Presents the
 * system share sheet via [Intent.ACTION_SEND] / [Intent.createChooser] so the user can
 * forward the text to messages, mail, clipboard, or any installed app that handles
 * `text/plain`. Silently no-ops when called before [SharingHost.appContext] is set
 * (e.g. unit tests) — failing the user's tap-to-share with a crash would be worse.
 */
actual fun shareText(content: String) {
    val context = SharingHost.appContext ?: return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    val chooser = Intent.createChooser(sendIntent, null).apply {
        // FLAG_ACTIVITY_NEW_TASK is required because we're launching from a non-Activity
        // Context (the application context).
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
