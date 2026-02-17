package com.jameeli.thykra

import com.jameeli.thykra.plugins.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            configureSerialization()
            routing {
                get("/") {
                    call.respondText("Thykra API v1.0")
                }
            }
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Thykra API v1.0", response.bodyAsText())
    }

    @Test
    fun testHealth() = testApplication {
        application {
            configureSerialization()
            routing {
                route("/api") {
                    get("/health") {
                        call.respondText("OK")
                    }
                }
            }
        }
        val response = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }
}
