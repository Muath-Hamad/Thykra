package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.AddReactionRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MediaReactionsDto
import com.jameeli.thykra.model.ReactionType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ReactionApi(private val client: HttpClient) {

    suspend fun list(albumId: String, mediaId: String): ApiResponse<MediaReactionsDto> {
        return client.get("$API_BASE_URL/api/albums/$albumId/media/$mediaId/reactions").body()
    }

    suspend fun toggle(albumId: String, mediaId: String, type: ReactionType): ApiResponse<MediaReactionsDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/media/$mediaId/reactions") {
            contentType(ContentType.Application.Json)
            setBody(AddReactionRequest(type))
        }.body()
    }
}
