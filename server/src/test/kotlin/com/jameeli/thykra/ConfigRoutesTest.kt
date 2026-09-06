package com.jameeli.thykra

import com.jameeli.thykra.model.ClientConfigDto
import com.jameeli.thykra.model.PresignedUploadDto
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val OneMegabyte = 1_048_576L

class ConfigRoutesTest {

    @Test
    fun configIsReadableWithoutAToken() = thykraTestApp {
        // No Authorization header: the share flow needs the ceiling while it is still
        // deciding whether the incoming files are usable, which can precede a token.
        val response = client.get("/api/config")

        assertEquals(HttpStatusCode.OK, response.status)
        val config = response.apiData<ClientConfigDto>()
        assertEquals(100 * OneMegabyte, config.maxUploadBytes)
    }

    @Test
    fun requestUploadRefusesAFileOverTheLimit() = thykraTestApp {
        val owner = devLogin("owner-cfg1@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val config = client.get("/api/config").apiData<ClientConfigDto>()

        val response = client.post("/api/albums/${album.id}/media/request-upload") {
            bearer(owner.accessToken)
            jsonBody(
                """{"filename":"huge.mov","contentType":"video/quicktime","fileSize":${config.maxUploadBytes + 1}}""",
            )
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        val envelope = response.api<Unit>()
        assertEquals(false, envelope.success)
        // The message has to name the limit, not just say no.
        assertTrue(envelope.error.orEmpty().contains("100 MB"), "was: ${envelope.error}")
    }

    @Test
    fun requestUploadAcceptsAFileAtTheLimit() = thykraTestApp {
        val owner = devLogin("owner-cfg2@test.dev", "Muath")
        val album = createAlbum(owner.accessToken, "Wadi Rum")
        val config = client.get("/api/config").apiData<ClientConfigDto>()

        // Exactly at the ceiling is allowed — the check is >, not >=.
        val response = client.post("/api/albums/${album.id}/media/request-upload") {
            bearer(owner.accessToken)
            jsonBody(
                """{"filename":"big.jpg","contentType":"image/jpeg","fileSize":${config.maxUploadBytes}}""",
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        response.apiData<PresignedUploadDto>()
    }
}
