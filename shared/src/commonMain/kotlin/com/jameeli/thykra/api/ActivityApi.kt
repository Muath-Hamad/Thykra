package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.RecentActivityDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ActivityApi(private val client: HttpClient) {

    /**
     * Returns the most recent reactions and comments across every album the caller is a
     * member of, newest first. [limit] is clamped server-side to 1..50 (default 25).
     */
    suspend fun recent(limit: Int? = null): ApiResponse<RecentActivityDto> {
        return client.get("$API_BASE_URL/api/activity/recent") {
            if (limit != null) parameter("limit", limit)
        }.body()
    }
}
