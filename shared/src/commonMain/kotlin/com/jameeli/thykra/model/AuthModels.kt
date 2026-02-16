package com.jameeli.thykra.model

import kotlinx.serialization.Serializable

@Serializable
enum class OAuthProvider { GOOGLE, APPLE }

@Serializable
data class OAuthRequest(
    val provider: OAuthProvider,
    val idToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDto
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
