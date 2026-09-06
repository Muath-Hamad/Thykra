package com.jameeli.thykra

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A presigned upload URL and a media URL both come back from the server describing
 * itself. Every client has to move them onto an origin it can actually reach, and
 * getting that wrong is invisible until an upload 404s on a real device.
 */
class ApiOriginTest {

    @Test
    fun `a loopback url is moved onto this client's origin`() {
        val minted = "http://localhost:$SERVER_PORT/api/media/upload/a1/photo.jpg"
        val resolved = resolveAgainstApiOrigin(minted)

        assertEquals("$API_BASE_URL/api/media/upload/a1/photo.jpg", resolved)
    }

    @Test
    fun `the 127 spelling of loopback is handled too`() {
        val minted = "http://127.0.0.1:$SERVER_PORT/api/media/files/a1/photo.jpg"
        assertEquals(
            "$API_BASE_URL/api/media/files/a1/photo.jpg",
            resolveAgainstApiOrigin(minted),
        )
    }

    @Test
    fun `a url already on a real origin is left alone`() {
        // S3, a CDN or a tunnel already points somewhere reachable; rewriting it would
        // break exactly the deployment this function exists to support.
        val s3 = "https://thykra-media.s3.eu-west-1.amazonaws.com/a1/photo.jpg?X-Amz-Signature=abc"
        assertEquals(s3, resolveAgainstApiOrigin(s3))

        val tunnel = "https://api.thykra.com/api/media/files/a1/photo.jpg"
        assertEquals(tunnel, resolveAgainstApiOrigin(tunnel))
    }

    @Test
    fun `a loopback on another port is not ours to rewrite`() {
        // The web dev server lives on 8080; only the API port is rewritten.
        val web = "http://localhost:8080/trips/a1"
        assertEquals(web, resolveAgainstApiOrigin(web))
    }

    @Test
    fun `the api origin carries a scheme so it survives https`() {
        // The old shape was host-only with a hardcoded http:// and :8081, which could
        // not express a deployed server on 443. This is the regression guard for that.
        assertEquals(true, API_BASE_URL.startsWith("http://") || API_BASE_URL.startsWith("https://"))
    }
}
