package com.jameeli.thykra.service

import com.jameeli.thykra.TestDatabase
import com.jameeli.thykra.auth.AppleOAuthVerifier
import com.jameeli.thykra.auth.GoogleOAuthVerifier
import com.jameeli.thykra.auth.JwtService
import com.jameeli.thykra.repository.RefreshTokenRepository
import com.jameeli.thykra.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.time.Clock
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

class AuthServiceTest {

    // Tests isolate via distinct emails on the shared test database — see TestDatabase.
    @BeforeTest
    fun setUpDatabase() {
        TestDatabase.connect()
    }

    private fun authConfig() = MapApplicationConfig(
        "jwt.issuer" to "test-issuer",
        "jwt.audience" to "test-audience",
        "jwt.secret" to "test-secret",
        "jwt.realm" to "test-realm",
        "jwt.accessTokenExpiry" to "3600",
        "jwt.refreshTokenExpiry" to "2592000",
        "oauth.google.clientId" to "",
        "oauth.apple.clientId" to ""
    )

    private fun withAuthService(
        block: suspend (AuthService, JwtService, RefreshTokenRepository) -> Unit
    ) = testApplication {
        environment { config = authConfig() }
        lateinit var authService: AuthService
        lateinit var jwtService: JwtService
        lateinit var refreshTokens: RefreshTokenRepository
        application {
            jwtService = JwtService(environment)
            refreshTokens = RefreshTokenRepository()
            // The OAuth verifiers are never invoked on the dev-login/refresh/logout
            // paths under test; real instances with an idle client are fine.
            val http = HttpClient(CIO)
            authService = AuthService(
                jwtService,
                UserRepository(),
                refreshTokens,
                GoogleOAuthVerifier(environment, http),
                AppleOAuthVerifier(environment, http)
            )
        }
        startApplication()
        block(authService, jwtService, refreshTokens)
    }

    @Test
    fun dev_login_creates_then_reuses_the_user() = withAuthService { auth, _, _ ->
        val first = auth.devLogin("alice@example.com", "Alice")
        val second = auth.devLogin("alice@example.com", "Alice")
        assertEquals(first.user.id, second.user.id)
        assertEquals("alice@example.com", first.user.email)
        assertEquals("Alice", first.user.displayName)
    }

    @Test
    fun dev_login_issues_verifiable_access_token() = withAuthService { auth, jwt, _ ->
        val response = auth.devLogin("bob@example.com", "Bob")
        val decoded = jwt.verifier.verify(response.accessToken)
        assertEquals(response.user.id, decoded.subject)
        assertEquals("bob@example.com", decoded.getClaim("email").asString())
        assertEquals(3600, response.expiresIn)
    }

    @Test
    fun refresh_rotates_and_revokes_the_old_token() = withAuthService { auth, _, _ ->
        val login = auth.devLogin("carol@example.com", "Carol")

        val rotated = assertNotNull(auth.refreshToken(login.refreshToken))
        assertNotEquals(login.refreshToken, rotated.refreshToken)

        // The consumed token must be dead, the rotated one must still work.
        assertNull(auth.refreshToken(login.refreshToken), "old refresh token must be revoked")
        assertNotNull(auth.refreshToken(rotated.refreshToken))
    }

    @Test
    fun unknown_refresh_token_returns_null() = withAuthService { auth, _, _ ->
        assertNull(auth.refreshToken("never-issued-token"))
    }

    @Test
    fun expired_refresh_token_returns_null() = withAuthService { auth, _, refreshTokens ->
        val login = auth.devLogin("dave@example.com", "Dave")
        val expired = "expired-token"
        refreshTokens.create(
            UUID.fromString(login.user.id),
            RefreshTokenRepository.hashToken(expired),
            Clock.System.now().minus(1.hours)
        )
        assertNull(auth.refreshToken(expired))
    }

    @Test
    fun logout_revokes_every_refresh_token() = withAuthService { auth, _, _ ->
        val first = auth.devLogin("erin@example.com", "Erin")
        val second = auth.devLogin("erin@example.com", "Erin")

        auth.logout(first.user.id)

        assertNull(auth.refreshToken(first.refreshToken))
        assertNull(auth.refreshToken(second.refreshToken))
    }
}
