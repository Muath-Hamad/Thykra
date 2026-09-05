package com.jameeli.thykra.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.CORSConfig
import io.ktor.server.plugins.cors.routing.CORS

/**
 * Cross-origin callers only.
 *
 * In every real deployment nginx serves the SPA and proxies `/api` under one
 * origin, so the browser's requests are same-origin and never consult this list
 * at all — provided `configureForwardedHeaders()` is installed, which is what
 * lets Ktor recognise them as same-origin in the first place.
 *
 * What is left for this list is traffic that genuinely crosses an origin: the
 * Vite dev server on :8080 proxying to a server started by `:server:run`, and
 * any future client served from a different host. Hostnames are deployment
 * facts, not source facts, so they come from `CORS_ALLOWED_ORIGINS` (comma
 * separated, scheme included) rather than being compiled in.
 */
fun Application.configureCors() {
    val allowedOrigins = environment.config
        .propertyOrNull("cors.allowedOrigins")?.getString()
        .orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowedOrigins.forEach(::allowOrigin)
        allowCredentials = true
    }
}

/**
 * `allowHost` wants the host and the scheme as separate arguments and rejects a
 * value containing "://", so split a normal origin string into the shape it
 * expects. A bare "host:port" with no scheme allows both http and https.
 */
private fun CORSConfig.allowOrigin(origin: String) {
    val trimmed = origin.trimEnd('/')
    val scheme = trimmed.substringBefore("://", missingDelimiterValue = "")
    val hostAndPort = if (scheme.isEmpty()) trimmed else trimmed.substringAfter("://")
    allowHost(
        hostAndPort,
        schemes = if (scheme.isEmpty()) listOf("http", "https") else listOf(scheme)
    )
}
