package com.jameeli.thykra.auth

import android.content.Context
import android.content.SharedPreferences
import com.jameeli.thykra.api.TokenProvider

class AndroidTokenStorage(context: Context) : TokenProvider {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("thykra_auth", Context.MODE_PRIVATE)

    override suspend fun getAccessToken(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null)

    override suspend fun getRefreshToken(): String? =
        prefs.getString(KEY_REFRESH_TOKEN, null)

    override suspend fun refreshTokens(): Pair<String, String>? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override suspend fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
