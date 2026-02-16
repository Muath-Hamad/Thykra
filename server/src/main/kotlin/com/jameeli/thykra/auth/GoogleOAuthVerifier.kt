package com.jameeli.thykra.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.application.ApplicationEnvironment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class GoogleOAuthVerifier(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient
) : OAuthVerifier {

    private val clientId = environment.config.property("oauth.google.clientId").getString()
    private var cachedKeys: Map<String, RSAPublicKey> = emptyMap()

    override suspend fun verify(idToken: String): OAuthUserInfo? {
        return try {
            val decoded = JWT.decode(idToken)
            val key = getPublicKey(decoded.keyId) ?: return null

            val algorithm = Algorithm.RSA256(key, null)
            val verifier = JWT.require(algorithm)
                .withIssuer("https://accounts.google.com", "accounts.google.com")
                .withAudience(clientId)
                .build()

            val verified = verifier.verify(idToken)
            OAuthUserInfo(
                subject = verified.subject,
                email = verified.getClaim("email").asString(),
                displayName = verified.getClaim("name").asString() ?: verified.getClaim("email").asString(),
                avatarUrl = verified.getClaim("picture").asString()
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getPublicKey(keyId: String?): RSAPublicKey? {
        if (keyId == null) return null
        cachedKeys[keyId]?.let { return it }

        val jwks: JsonObject = httpClient.get(GOOGLE_JWKS_URL).body()
        val keys = jwks["keys"]?.jsonArray ?: return null

        cachedKeys = keys.associate { keyJson ->
            val obj = keyJson.jsonObject
            val kid = obj["kid"]!!.jsonPrimitive.content
            val n = obj["n"]!!.jsonPrimitive.content
            val e = obj["e"]!!.jsonPrimitive.content
            kid to createRSAPublicKey(n, e)
        }

        return cachedKeys[keyId]
    }

    private fun createRSAPublicKey(n: String, e: String): RSAPublicKey {
        val decoder = Base64.getUrlDecoder()
        val modulus = BigInteger(1, decoder.decode(n))
        val exponent = BigInteger(1, decoder.decode(e))
        val spec = RSAPublicKeySpec(modulus, exponent)
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }

    companion object {
        private const val GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs"
    }
}
