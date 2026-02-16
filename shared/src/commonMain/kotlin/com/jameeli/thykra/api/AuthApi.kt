package com.jameeli.thykra.api

import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.AuthResponse
import com.jameeli.thykra.model.OAuthRequest
import com.jameeli.thykra.model.RefreshTokenRequest
import com.jameeli.thykra.model.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {

    suspend fun loginWithOAuth(request: OAuthRequest): ApiResponse<AuthResponse> {
        return client.post("$API_BASE_URL/api/auth/oauth") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun refreshToken(request: RefreshTokenRequest): ApiResponse<TokenResponse> {
        return client.post("$API_BASE_URL/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun logout(): ApiResponse<Unit> {
        return client.post("$API_BASE_URL/api/auth/logout").body()
    }
}
