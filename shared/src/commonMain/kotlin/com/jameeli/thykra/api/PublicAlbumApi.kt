package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.PublicAlbumViewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PublicAlbumApi(private val client: HttpClient) {
    suspend fun getAlbum(id: String): ApiResponse<PublicAlbumViewDto> {
        return client.get("$API_BASE_URL/api/public/albums/$id").body()
    }
}
