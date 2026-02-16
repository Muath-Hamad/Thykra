package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.UpdateProfileRequest
import com.jameeli.thykra.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ProfileApi(private val client: HttpClient) {

    suspend fun getProfile(): ApiResponse<UserDto> {
        return client.get("$API_BASE_URL/api/profile").body()
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<UserDto> {
        return client.put("$API_BASE_URL/api/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
