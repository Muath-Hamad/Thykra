package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.ClientConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * The server's published limits.
 *
 * Fetched once and cached for the session: it changes only when the server is
 * redeployed, and a client that cannot reach it should still let someone try to upload
 * rather than block them on a courtesy check — hence [fallback] rather than an error.
 */
class ConfigApi(private val client: HttpClient) {

    suspend fun fetch(): ApiResponse<ClientConfigDto> =
        client.get("$API_BASE_URL/api/config").body()

    /**
     * The limits to work with, falling back to the documented defaults when the server
     * cannot be reached. An upload that exceeds the real ceiling still fails at
     * request-upload with a 413 that names the number, so guessing high is safe.
     */
    suspend fun fetchOrDefault(): ClientConfigDto =
        runCatching { fetch() }.getOrNull()?.takeIf { it.success }?.data ?: fallback

    companion object {
        /** Matches `upload.maxBytes` in the server's application.conf. */
        val fallback = ClientConfigDto(maxUploadBytes = 104_857_600L)
    }
}
