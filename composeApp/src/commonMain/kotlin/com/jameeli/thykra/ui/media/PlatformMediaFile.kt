package com.jameeli.thykra.ui.media

data class PlatformMediaFile(
    val name: String,
    val contentType: String,
    val size: Long,
    val readBytes: suspend () -> ByteArray,
    val width: Int? = null,
    val height: Int? = null
)
