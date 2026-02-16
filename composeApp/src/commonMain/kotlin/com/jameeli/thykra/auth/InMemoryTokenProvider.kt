package com.jameeli.thykra.auth

import com.jameeli.thykra.api.TokenProvider

class InMemoryTokenProvider : TokenProvider {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override suspend fun getAccessToken(): String? = accessToken
    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun refreshTokens(): Pair<String, String>? {
        return null
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun clearTokens() {
        accessToken = null
        refreshToken = null
    }
}
