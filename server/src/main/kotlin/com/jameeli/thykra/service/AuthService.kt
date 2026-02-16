package com.jameeli.thykra.service

import com.jameeli.thykra.auth.AppleOAuthVerifier
import com.jameeli.thykra.auth.GoogleOAuthVerifier
import com.jameeli.thykra.auth.JwtService
import com.jameeli.thykra.model.AuthResponse
import com.jameeli.thykra.model.TokenResponse
import com.jameeli.thykra.repository.RefreshTokenRepository
import com.jameeli.thykra.repository.UserRepository
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val googleVerifier: GoogleOAuthVerifier,
    private val appleVerifier: AppleOAuthVerifier
) {

    suspend fun loginWithOAuth(provider: String, idToken: String): AuthResponse {
        val verifier = when (provider) {
            "google" -> googleVerifier
            "apple" -> appleVerifier
            else -> throw IllegalArgumentException("Unsupported OAuth provider: $provider")
        }

        val userInfo = verifier.verify(idToken)
            ?: throw IllegalArgumentException("Invalid ID token")

        val user = userRepository.findByOAuthSubject(provider, userInfo.subject)
            ?: userRepository.createUser(
                email = userInfo.email,
                displayName = userInfo.displayName,
                avatarUrl = userInfo.avatarUrl,
                provider = provider,
                subject = userInfo.subject
            )

        val accessToken = jwtService.generateAccessToken(user.id, user.email)
        val refreshToken = jwtService.generateRefreshToken()
        val refreshTokenHash = RefreshTokenRepository.hashToken(refreshToken)
        val expiresAt = Clock.System.now().plus(jwtService.refreshTokenExpiry.seconds)

        refreshTokenRepository.create(UUID.fromString(user.id), refreshTokenHash, expiresAt)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.accessTokenExpiry,
            user = user
        )
    }

    suspend fun refreshToken(refreshToken: String): TokenResponse? {
        val tokenHash = RefreshTokenRepository.hashToken(refreshToken)
        val record = refreshTokenRepository.findValidByTokenHash(tokenHash) ?: return null

        if (record.expiresAt < Clock.System.now()) {
            refreshTokenRepository.revokeByTokenHash(tokenHash)
            return null
        }

        // Rotate: revoke old, create new
        refreshTokenRepository.revokeByTokenHash(tokenHash)

        val user = userRepository.findById(record.userId) ?: return null
        val newAccessToken = jwtService.generateAccessToken(user.id, user.email)
        val newRefreshToken = jwtService.generateRefreshToken()
        val newHash = RefreshTokenRepository.hashToken(newRefreshToken)
        val expiresAt = Clock.System.now().plus(jwtService.refreshTokenExpiry.seconds)

        refreshTokenRepository.create(record.userId, newHash, expiresAt)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtService.accessTokenExpiry
        )
    }

    suspend fun logout(userId: String) {
        refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId))
    }
}
