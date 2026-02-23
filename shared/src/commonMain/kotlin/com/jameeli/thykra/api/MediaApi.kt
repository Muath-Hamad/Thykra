package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.ConfirmUploadRequest
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.PresignedUploadDto
import com.jameeli.thykra.model.RequestUploadUrlRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

class MediaApi(private val client: HttpClient) {

    // Uses a separate unauthenticated client for raw file uploads (presigned URLs are self-authorising)
    private val rawClient = createPlatformHttpClient()

    suspend fun requestUploadUrl(
        albumId: String,
        request: RequestUploadUrlRequest
    ): ApiResponse<PresignedUploadDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/media/request-upload") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun uploadFile(
        uploadUrl: String,
        method: String,
        headers: Map<String, String>,
        bytes: ByteArray,
        contentType: String
    ) {
        rawClient.request(uploadUrl) {
            this.method = HttpMethod.parse(method)
            headers.forEach { (key, value) -> this.header(key, value) }
            header(HttpHeaders.ContentType, contentType)
            setBody(bytes)
        }
    }

    suspend fun confirmUpload(
        albumId: String,
        mediaId: String,
        request: ConfirmUploadRequest
    ): ApiResponse<MediaDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/media/$mediaId/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getAlbumMedia(albumId: String): ApiResponse<List<MediaDto>> {
        return client.get("$API_BASE_URL/api/albums/$albumId/media").body()
    }

    suspend fun getMedia(albumId: String, mediaId: String): ApiResponse<MediaDto> {
        return client.get("$API_BASE_URL/api/albums/$albumId/media/$mediaId").body()
    }

    suspend fun deleteMedia(albumId: String, mediaId: String): ApiResponse<String> {
        return client.delete("$API_BASE_URL/api/albums/$albumId/media/$mediaId").body()
    }
}
