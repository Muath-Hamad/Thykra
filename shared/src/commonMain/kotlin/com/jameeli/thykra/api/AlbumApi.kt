package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.AddMemberRequest
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.CreateAlbumRequest
import com.jameeli.thykra.model.CreateInviteRequest
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.UpdateAlbumRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AlbumApi(private val client: HttpClient) {

    suspend fun getAlbums(): ApiResponse<List<AlbumDto>> {
        return client.get("$API_BASE_URL/api/albums").body()
    }

    suspend fun getAlbum(id: String): ApiResponse<AlbumDto> {
        return client.get("$API_BASE_URL/api/albums/$id").body()
    }

    suspend fun createAlbum(request: CreateAlbumRequest): ApiResponse<AlbumDto> {
        return client.post("$API_BASE_URL/api/albums") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateAlbum(id: String, request: UpdateAlbumRequest): ApiResponse<AlbumDto> {
        return client.put("$API_BASE_URL/api/albums/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteAlbum(id: String): ApiResponse<Unit> {
        return client.delete("$API_BASE_URL/api/albums/$id").body()
    }

    suspend fun getMembers(albumId: String): ApiResponse<List<AlbumMemberDto>> {
        return client.get("$API_BASE_URL/api/albums/$albumId/members").body()
    }

    suspend fun addMember(albumId: String, request: AddMemberRequest): ApiResponse<AlbumMemberDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/members") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun removeMember(albumId: String, userId: String): ApiResponse<Unit> {
        return client.delete("$API_BASE_URL/api/albums/$albumId/members/$userId").body()
    }

    suspend fun createInviteLink(albumId: String, expiresInDays: Int? = null): ApiResponse<InviteLinkDto> {
        return client.post("$API_BASE_URL/api/albums/$albumId/invite") {
            contentType(ContentType.Application.Json)
            setBody(CreateInviteRequest(expiresInDays))
        }.body()
    }

    suspend fun joinByInvite(token: String): ApiResponse<AlbumDto> {
        return client.post("$API_BASE_URL/api/albums/join/$token").body()
    }
}
