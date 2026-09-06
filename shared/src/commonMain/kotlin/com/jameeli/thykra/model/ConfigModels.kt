package com.jameeli.thykra.model

import kotlinx.serialization.Serializable

/**
 * Limits the server enforces, published so a client can apply them before it does work
 * that is going to be rejected anyway.
 *
 * The upload queue stages bytes to disk before it sends them, so a phone that learns the
 * ceiling first can refuse a 400 MB video at the picker instead of copying it, queueing
 * it, uploading it for two minutes on cellular and then failing. The server still
 * enforces every one of these — this is a courtesy, never a control.
 *
 * Defaults match the copy the web already shows ("up to 100 MB per file"), so the two
 * products cannot disagree about the number in front of someone.
 */
@Serializable
data class ClientConfigDto(
    /** Largest single file the server will accept, in bytes. */
    val maxUploadBytes: Long,
    /** Content types the upload endpoints accept. Empty means the client should not filter. */
    val allowedContentTypes: List<String> = emptyList(),
)
