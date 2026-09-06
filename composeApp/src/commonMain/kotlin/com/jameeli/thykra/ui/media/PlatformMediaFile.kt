package com.jameeli.thykra.ui.media

data class PlatformMediaFile(
    val name: String,
    val contentType: String,
    val size: Long,
    val readBytes: suspend () -> ByteArray,
    val width: Int? = null,
    val height: Int? = null,
    /**
     * When the photo was taken, read locally from its own metadata.
     *
     * The server extracts this again on confirm and its answer is authoritative. This
     * copy exists so a queued file can be placed in the right day chapter immediately,
     * rather than sitting under today until the round trip finishes and then jumping.
     */
    val takenAt: kotlin.time.Instant? = null,
    val durationMs: Long? = null,
)
