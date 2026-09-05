package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.API_HOST
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

class MediaApi(
    private val client: HttpClient,
    isDebug: Boolean = false,
    // Separate unauthenticated client for raw file uploads (presigned URLs are
    // self-authorising). Injectable so tests can substitute a mock engine.
    private val rawClient: HttpClient = createRawHttpClient(isDebug)
) {

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
        // Replace localhost with the platform-specific API host so Android emulator
        // (which uses 10.0.2.2) and other platforms work without extra server config.
        val fixedUrl = uploadUrl.replace("://localhost:", "://$API_HOST:")
        val response = rawClient.request(fixedUrl) {
            this.method = HttpMethod.parse(method)
            headers.forEach { (key, value) -> this.header(key, value) }
            header(HttpHeaders.ContentType, contentType)
            setBody(bytes)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Upload failed: HTTP ${response.status.value}")
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
