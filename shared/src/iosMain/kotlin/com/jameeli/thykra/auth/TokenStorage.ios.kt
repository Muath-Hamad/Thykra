package com.jameeli.thykra.auth

import com.jameeli.thykra.api.TokenProvider
import platform.Foundation.NSUserDefaults

class IosTokenStorage : TokenProvider {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun getAccessToken(): String? =
        defaults.stringForKey(KEY_ACCESS_TOKEN)

    override suspend fun getRefreshToken(): String? =
        defaults.stringForKey(KEY_REFRESH_TOKEN)

    override suspend fun refreshTokens(): Pair<String, String>? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        defaults.setObject(accessToken, KEY_ACCESS_TOKEN)
        defaults.setObject(refreshToken, KEY_REFRESH_TOKEN)
    }

    override suspend fun clearTokens() {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "thykra_access_token"
        private const val KEY_REFRESH_TOKEN = "thykra_refresh_token"
    }
}
