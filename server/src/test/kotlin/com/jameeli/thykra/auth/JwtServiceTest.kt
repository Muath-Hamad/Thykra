package com.jameeli.thykra.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JwtServiceTest {

    private fun jwtConfig() = MapApplicationConfig(
        "jwt.issuer" to "test-issuer",
        "jwt.audience" to "test-audience",
        "jwt.secret" to "test-secret",
        "jwt.realm" to "test-realm",
        "jwt.accessTokenExpiry" to "3600",
        "jwt.refreshTokenExpiry" to "2592000"
    )

    // JwtService reads its settings from ApplicationEnvironment, so build one
    // through testApplication instead of booting the real module.
    private fun withJwtService(block: suspend (JwtService) -> Unit) = testApplication {
        environment { config = jwtConfig() }
        lateinit var service: JwtService
        application { service = JwtService(environment) }
        startApplication()
        block(service)
    }

    @Test
    fun access_token_round_trips_claims() = withJwtService { service ->
        val token = service.generateAccessToken("user-123", "alice@example.com")
        val decoded = service.verifier.verify(token)
        assertEquals("user-123", decoded.subject)
        assertEquals("alice@example.com", decoded.getClaim("email").asString())
        assertEquals("test-issuer", decoded.issuer)
        assertTrue(decoded.audience.contains("test-audience"))
    }

    @Test
    fun access_token_lifetime_matches_configured_expiry() = withJwtService { service ->
        val token = service.generateAccessToken("user-123", "a@b.c")
        val decoded = service.verifier.verify(token)
        val lifetimeMs = decoded.expiresAt.time - decoded.issuedAt.time
        // JWT dates truncate to whole seconds, so allow a little slack around 3600s.
        assertTrue(lifetimeMs in 3_595_000..3_605_000, "unexpected lifetime: $lifetimeMs ms")
    }

    @Test
    fun verifier_rejects_token_signed_with_other_secret() = withJwtService { service ->
        val forged = JWT.create()
            .withIssuer("test-issuer")
            .withAudience("test-audience")
            .withSubject("user-123")
            .sign(Algorithm.HMAC256("other-secret"))
        assertFailsWith<JWTVerificationException> { service.verifier.verify(forged) }
    }

    @Test
    fun verifier_rejects_tampered_payload() = withJwtService { service ->
        val token = service.generateAccessToken("user-123", "a@b.c")
        val parts = token.split(".")
        val tampered = parts[0] + "." + parts[1].dropLast(4) + "AAAA" + "." + parts[2]
        assertFailsWith<JWTVerificationException> { service.verifier.verify(tampered) }
    }

    @Test
    fun refresh_tokens_are_unique_uuids() = withJwtService { service ->
        val tokens = List(100) { service.generateRefreshToken() }
        assertEquals(100, tokens.toSet().size, "refresh tokens must not repeat")
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        tokens.forEach { assertTrue(uuidRegex.matches(it), "not a UUID: $it") }
    }
}
