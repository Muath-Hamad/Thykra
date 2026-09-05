package com.jameeli.thykra

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Regression cover for the bug that made sign-in fail on every deployed
 * environment: browsers attach an `Origin` header to every non-GET request,
 * including same-origin ones, and Ktor answers with a bare 403 when it thinks
 * that origin is foreign. Behind nginx it always did, because `request.origin`
 * reported the container's own http://…:8081 rather than the public URL.
 *
 * The web app cannot distinguish that 403 (empty body) from any other failure,
 * so it renders "Sign-in didn't finish. Try again." — no clue as to the cause.
 * These tests pin the behaviour at the layer where it actually broke.
 */
class CorsTest {

    private val proxiedOrigin = "https://thykra.example.com"

    /** What the browser sends on a same-origin POST through the tunnel. */
    @Test
    fun sameOriginPostBehindProxyIsNotRejected() = thykraTestApp {
        val response = client.post("/api/auth/oauth") {
            header(HttpHeaders.Origin, proxiedOrigin)
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-Host", "thykra.example.com")
            jsonBody("""{"provider":"GOOGLE","idToken":"not-a-real-token"}""")
        }
        // The route must be reached and reject the token on its own terms.
        // A 403 here means CORS ate the request before the handler ever ran.
        assertNotEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    /** The same call on a LAN deployment, where the public port is not 80. */
    @Test
    fun sameOriginPostBehindProxyOnNonDefaultPortIsNotRejected() = thykraTestApp {
        val response = client.post("/api/auth/oauth") {
            header(HttpHeaders.Origin, "http://tower.local:8088")
            header("X-Forwarded-Proto", "http")
            header("X-Forwarded-Host", "tower.local:8088")
            jsonBody("""{"provider":"GOOGLE","idToken":"not-a-real-token"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    /** GET carries no Origin from a same-origin fetch, but proxies may add one. */
    @Test
    fun sameOriginGetBehindProxyIsNotRejected() = thykraTestApp {
        val response = client.get("/api/health") {
            header(HttpHeaders.Origin, proxiedOrigin)
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-Host", "thykra.example.com")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /** The allowlist still does its job: an unrelated site stays locked out. */
    @Test
    fun genuinelyForeignOriginIsStillRejected() = thykraTestApp {
        val response = client.post("/api/auth/oauth") {
            header(HttpHeaders.Origin, "https://evil.example.com")
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-Host", "thykra.example.com")
            jsonBody("""{"provider":"GOOGLE","idToken":"not-a-real-token"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
