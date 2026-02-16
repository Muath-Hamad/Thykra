package com.jameeli.thykra.auth

data class OAuthUserInfo(
    val subject: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?
)

interface OAuthVerifier {
    suspend fun verify(idToken: String): OAuthUserInfo?
}
