package com.jameeli.thykra.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationEnvironment
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class JwtService(environment: ApplicationEnvironment) {

    private val issuer = environment.config.property("jwt.issuer").getString()
    private val audience = environment.config.property("jwt.audience").getString()
    private val secret = environment.config.property("jwt.secret").getString()
    val realm: String = environment.config.property("jwt.realm").getString()
    val accessTokenExpiry: Long = environment.config.property("jwt.accessTokenExpiry").getString().toLong()
    val refreshTokenExpiry: Long = environment.config.property("jwt.refreshTokenExpiry").getString().toLong()

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateAccessToken(userId: String, email: String): String {
        val now = Clock.System.now()
        val expiry = now.plus(accessTokenExpiry.seconds)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("email", email)
            .withIssuedAt(Date.from(now.toJavaInstant()))
            .withExpiresAt(Date.from(expiry.toJavaInstant()))
            .sign(algorithm)
    }

    fun generateRefreshToken(): String {
        return UUID.randomUUID().toString()
    }
}
