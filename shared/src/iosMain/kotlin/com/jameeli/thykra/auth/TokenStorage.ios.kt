package com.jameeli.thykra.auth

import com.jameeli.thykra.api.TokenProvider
import platform.Foundation.NSUserDefaults

/**
 * iOS token provider backed by an App Group's shared `NSUserDefaults` suite.
 *
 * The suite name is intentionally shared with the WidgetKit extension target
 * (see `iosApp/ThykraWidgets/`) so widgets can read the JWT without us having
 * to re-implement auth there. If the App Group entitlement is not yet wired in
 * Xcode, `NSUserDefaults(suiteName:)` returns nil for that suite and we fall
 * back to `standardUserDefaults` so the main app keeps working.
 */
class IosTokenStorage : TokenProvider {

    // App Group identifier — must match `group.com.jameeli.thykra` entitlement
    // on BOTH the main app target and the widget extension target.
    private val appGroupSuite: NSUserDefaults? =
        NSUserDefaults(suiteName = APP_GROUP_SUITE)

    // Shared with widget extension — reads/writes go through the App Group
    // suite when available so the widget process can read the access token.
    private val defaults: NSUserDefaults =
        appGroupSuite ?: NSUserDefaults.standardUserDefaults

    override suspend fun getAccessToken(): String? =
        defaults.stringForKey(KEY_ACCESS_TOKEN)

    override suspend fun getRefreshToken(): String? =
        defaults.stringForKey(KEY_REFRESH_TOKEN)

    override suspend fun refreshTokens(): Pair<String, String>? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        defaults.setObject(accessToken, KEY_ACCESS_TOKEN)
        defaults.setObject(refreshToken, KEY_REFRESH_TOKEN)
        // Mirror under the widget-friendly key. Widgets read `auth_token`
        // straight from the App Group suite without going through Kotlin.
        defaults.setObject(accessToken, KEY_WIDGET_AUTH_TOKEN)
    }

    override suspend fun clearTokens() {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.removeObjectForKey(KEY_WIDGET_AUTH_TOKEN)
    }

    companion object {
        const val APP_GROUP_SUITE = "group.com.jameeli.thykra"
        private const val KEY_ACCESS_TOKEN = "thykra_access_token"
        private const val KEY_REFRESH_TOKEN = "thykra_refresh_token"
        // Convention key used by the WidgetKit extension. Kept short/stable
        // because it is hardcoded in Swift widget code as well.
        private const val KEY_WIDGET_AUTH_TOKEN = "auth_token"
    }
}
