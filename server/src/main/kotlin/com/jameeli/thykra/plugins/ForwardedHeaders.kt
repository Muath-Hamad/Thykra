package com.jameeli.thykra.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

/**
 * Teaches Ktor that it sits behind a reverse proxy.
 *
 * Without this, `call.request.origin` reports the Netty connector's own view —
 * always `http` on port 8081 — no matter what the browser actually asked for.
 * That single fact broke sign-in on every deployed environment: browsers attach
 * an `Origin` header to every non-GET request, including same-origin ones, and
 * Ktor's CORS plugin decides "same origin" by comparing that header against
 * `request.origin`. Behind nginx the comparison was `http://host:8081` versus
 * `https://thykra.example.com`, so a plain same-origin `POST /api/auth/oauth`
 * was judged cross-origin and answered with an empty 403 — which the web app
 * surfaces as "Sign-in didn't finish. Try again."
 *
 * With `X-Forwarded-Proto`/`-Host` honoured, `request.origin` matches what the
 * browser sees, same-origin traffic skips CORS entirely, and the deployment
 * keeps working when the public hostname changes.
 *
 * These headers are client-supplied, so they are only trustworthy because the
 * proxy in front of us sets them (see webApp/nginx.conf). Nothing here grants
 * authority — `request.origin` feeds CORS comparison and logging, never authn.
 */
fun Application.configureForwardedHeaders() {
    install(XForwardedHeaders)
}
