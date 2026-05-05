package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.AddCommentRequest
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.CommentDto
import com.jameeli.thykra.model.UpdateCommentRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CommentApi(private val client: HttpClient) {

    suspend fun list(albumId: String, mediaId: String): ApiResponse<List<CommentDto>> {
        return client.get("$API_BASE_URL/api/albums/$albumId/media/$mediaId/comments").body()
    }

    suspend fun add(albumId: String, mediaId: String, body: String): ApiResponse<CommentDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/media/$mediaId/comments") {
            contentType(ContentType.Application.Json)
            setBody(AddCommentRequest(body))
        }.body()
    }

    suspend fun update(albumId: String, mediaId: String, commentId: String, body: String): ApiResponse<CommentDto> {
        return client.put("$API_BASE_URL/api/albums/$albumId/media/$mediaId/comments/$commentId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCommentRequest(body))
        }.body()
    }

    suspend fun delete(albumId: String, mediaId: String, commentId: String): ApiResponse<Unit> {
        return client.delete("$API_BASE_URL/api/albums/$albumId/media/$mediaId/comments/$commentId").body()
    }
}
