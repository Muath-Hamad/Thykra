package com.jameeli.thykra

import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.AuthResponse
import com.jameeli.thykra.model.CreateAlbumRequest
import com.jameeli.thykra.model.RefreshTokenRequest
import com.jameeli.thykra.model.TokenResponse
import com.jameeli.thykra.routes.DevLoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Boots the real application module (routing, auth, H2 database, local storage)
 * and exercises the HTTP surface end to end — the closest CI gets to a running
 * server without Docker.
 */
class ApiIntegrationTest {

    private fun fullConfig() = MapApplicationConfig(
        // Shared named in-memory database (see TestDatabase) — tests isolate via
        // distinct emails, not separate databases.
        "database.url" to TestDatabase.URL,
        "database.driver" to "org.h2.Driver",
        "database.user" to "sa",
        "database.password" to "",
        "database.maxPoolSize" to "5",
        "jwt.issuer" to "thykra-test",
        "jwt.audience" to "thykra-test-users",
        "jwt.realm" to "thykra",
        "jwt.secret" to "integration-test-secret",
        "jwt.accessTokenExpiry" to "3600",
        "jwt.refreshTokenExpiry" to "2592000",
        "storage.type" to "local",
        "storage.baseUrl" to "http://localhost:8081",
        "storage.localPath" to createTempDirectory("thykra-int-media").toString(),
        "oauth.google.clientId" to "",
        "oauth.apple.clientId" to "",
        "auth.allowDevLogin" to "true"
    )

    private fun withApp(block: suspend (HttpClient) -> Unit) = testApplication {
        environment { config = fullConfig() }
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        block(client)
    }

    private suspend fun devLogin(client: HttpClient, email: String): AuthResponse {
        val response = client.post("/api/auth/dev-login") {
            contentType(ContentType.Application.Json)
            setBody(DevLoginRequest(email = email, displayName = "Integration Tester"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val envelope = response.body<ApiResponse<AuthResponse>>()
        assertTrue(envelope.success)
        return assertNotNull(envelope.data)
    }

    @Test
    fun root_and_health_endpoints_respond() = withApp { client ->
        assertEquals("Thykra API v1.0", client.get("/").bodyAsText())
        val health = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertEquals("OK", health.bodyAsText())
    }

    @Test
    fun dev_login_issues_working_credentials() = withApp { client ->
        val auth = devLogin(client, "login@example.com")
        assertTrue(auth.accessToken.isNotBlank())
        assertTrue(auth.refreshToken.isNotBlank())
        assertEquals("login@example.com", auth.user.email)
    }

    @Test
    fun dev_login_rejects_blank_email() = withApp { client ->
        val response = client.post("/api/auth/dev-login") {
            contentType(ContentType.Application.Json)
            setBody(DevLoginRequest(email = "   ", displayName = "X"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun protected_routes_reject_missing_and_invalid_tokens() = withApp { client ->
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/albums").status)
        val garbage = client.get("/api/albums") { bearerAuth("not-a-jwt") }
        assertEquals(HttpStatusCode.Unauthorized, garbage.status)
    }

    @Test
    fun album_create_list_fetch_flow_works_for_the_owner() = withApp { client ->
        val auth = devLogin(client, "albums@example.com")

        val created = client.post("/api/albums") {
            bearerAuth(auth.accessToken)
            contentType(ContentType.Application.Json)
            setBody(CreateAlbumRequest(title = "Trip to Petra", description = "Family trip"))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val album = assertNotNull(created.body<ApiResponse<AlbumDto>>().data)
        assertEquals("Trip to Petra", album.title)
        assertEquals(auth.user.id, album.ownerId)
        assertEquals(1, album.memberCount)

        val listed = client.get("/api/albums") { bearerAuth(auth.accessToken) }
        assertEquals(HttpStatusCode.OK, listed.status)
        val albums = assertNotNull(listed.body<ApiResponse<List<AlbumDto>>>().data)
        assertTrue(albums.any { it.id == album.id })

        val fetched = client.get("/api/albums/${album.id}") { bearerAuth(auth.accessToken) }
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(album.id, assertNotNull(fetched.body<ApiResponse<AlbumDto>>().data).id)
    }

    @Test
    fun refresh_endpoint_rotates_tokens_and_kills_the_old_one() = withApp { client ->
        val auth = devLogin(client, "refresh@example.com")

        val refreshed = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(auth.refreshToken))
        }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        val tokens = assertNotNull(refreshed.body<ApiResponse<TokenResponse>>().data)
        assertTrue(tokens.refreshToken != auth.refreshToken)

        val replay = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(auth.refreshToken))
        }
        assertEquals(HttpStatusCode.Unauthorized, replay.status)
    }

    @Test
    fun logout_revokes_refresh_tokens() = withApp { client ->
        val auth = devLogin(client, "logout@example.com")

        val logout = client.post("/api/auth/logout") { bearerAuth(auth.accessToken) }
        assertEquals(HttpStatusCode.OK, logout.status)

        val afterLogout = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(auth.refreshToken))
        }
        assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
    }
}
