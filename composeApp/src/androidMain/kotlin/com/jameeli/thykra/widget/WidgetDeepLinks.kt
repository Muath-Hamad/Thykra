package com.jameeli.thykra.widget

import android.content.Context
import android.content.Intent
import com.jameeli.thykra.MainActivity

/**
 * Intent extras the main activity reads to deep-link from a widget tap into a specific
 * destination. Both keys are optional — if [EXTRA_ALBUM_ID] is missing the app falls back to
 * its normal startup flow.
 */
object WidgetDeepLinks {
    const val EXTRA_ALBUM_ID = "com.jameeli.thykra.widget.EXTRA_ALBUM_ID"
    const val EXTRA_MEDIA_ID = "com.jameeli.thykra.widget.EXTRA_MEDIA_ID"

    /**
     * Builds an [Intent] targeting [MainActivity] that, when launched, will navigate to the
     * supplied album (and optionally media item).
     *
     * The widget host launches this via `actionStartActivity`; the activity must be brought to
     * the front and re-deliver the intent to the running task, so we set
     * `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`.
     */
    fun openAlbum(context: Context, albumId: String, mediaId: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            // Unique action so the system doesn't dedupe between distinct widget taps
            action = "com.jameeli.thykra.widget.OPEN_ALBUM"
            // Include album id in the data URI so PendingIntents with different extras stay distinct
            val mediaPart = mediaId?.let { "/media/$it" } ?: ""
            data = android.net.Uri.parse("thykra://album/$albumId$mediaPart")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALBUM_ID, albumId)
            if (mediaId != null) putExtra(EXTRA_MEDIA_ID, mediaId)
        }
    }
}
