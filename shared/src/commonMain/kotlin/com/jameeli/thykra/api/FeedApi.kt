package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ActivityFeedDto
import com.jameeli.thykra.model.ActivitySeenDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MarkActivitySeenRequest
import com.jameeli.thykra.model.RecapDto
import com.jameeli.thykra.model.RecapViewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The aggregated activity feed. Events arrive pre-merged — "Sara added 12 photos", not
 * twelve rows — so the client never groups anything itself.
 */
class ActivityFeedApi(private val client: HttpClient) {

    suspend fun feed(cursor: String? = null, limit: Int? = null): ApiResponse<ActivityFeedDto> =
        client.get("$API_BASE_URL/api/activity") {
            cursor?.let { parameter("cursor", it) }
            limit?.let { parameter("limit", it) }
        }.body()

    suspend fun feedForAlbum(
        albumId: String,
        cursor: String? = null,
        limit: Int? = null,
    ): ApiResponse<ActivityFeedDto> =
        client.get("$API_BASE_URL/api/albums/$albumId/activity") {
            cursor?.let { parameter("cursor", it) }
            limit?.let { parameter("limit", it) }
        }.body()

    /**
     * The read marker. Posted when the Activity tab is left rather than when it is
     * opened, so a glance at the top of the list does not mark the bottom of it seen.
     */
    suspend fun markSeen(seenAt: kotlinx.datetime.Instant? = null): ApiResponse<ActivitySeenDto> =
        client.post("$API_BASE_URL/api/activity/seen") {
            contentType(ContentType.Application.Json)
            setBody(MarkActivitySeenRequest(seenAt))
        }.body()
}

/** Recaps: the generated editions, and the public reader behind a share token. */
class RecapApi(private val client: HttpClient) {

    suspend fun listForUser(): ApiResponse<List<RecapDto>> =
        client.get("$API_BASE_URL/api/recaps").body()

    suspend fun listForAlbum(albumId: String): ApiResponse<List<RecapDto>> =
        client.get("$API_BASE_URL/api/albums/$albumId/recaps").body()

    /** Server work, usually seconds. The card shows a skeleton for the round trip. */
    suspend fun build(albumId: String): ApiResponse<RecapDto> =
        client.post("$API_BASE_URL/api/albums/$albumId/recaps").body()

    /** Unauthenticated: the link works regardless of the trip's visibility. */
    suspend fun read(shareToken: String): ApiResponse<RecapViewDto> =
        client.get("$API_BASE_URL/api/r/$shareToken").body()
}
