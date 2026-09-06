package com.jameeli.thykra

const val SERVER_PORT = 8081

/**
 * The full origin the client talks to — scheme, host and port together, e.g.
 * `http://10.0.2.2:8081` in an emulator or `https://thykra.example.com` on a real phone.
 *
 * It is the whole origin rather than a host because a deployed server is reached over
 * https on port 443, and a host-plus-fixed-port constant cannot express that. Each
 * platform supplies a development default and a way to override it at build time:
 *
 * - Android: `API_BASE_URL` in `local.properties`, or the `THYKRA_API_BASE_URL`
 *   environment variable, baked into `BuildConfig`.
 * - iOS: the `ThykraApiBaseUrl` key in `Info.plist`, fed from `THYKRA_API_BASE_URL`
 *   in `Configuration/Config.xcconfig`.
 *
 * A device build that still points at `localhost` reaches the phone itself and fails
 * every request, which is why this is configurable rather than a constant.
 */
expect val API_BASE_URL: String

/**
 * Origin where the web app is served. Used by clients that need to mint shareable URLs.
 * Each platform supplies a sensible dev default; production builds override at build time.
 */
expect val WEB_BASE_URL: String

/**
 * Rewrites a URL the server minted from its own point of view onto the origin this client
 * can actually reach.
 *
 * A presigned upload URL comes back as `http://localhost:8081/...` because that is what
 * the dev server calls itself. An emulator has to send it to `10.0.2.2`, and a phone on
 * the far side of a tunnel has to send it somewhere else again. Anything already pointing
 * at a real origin is returned untouched.
 */
fun resolveAgainstApiOrigin(url: String): String {
    LoopbackOrigins.forEach { origin ->
        if (url.startsWith(origin)) return API_BASE_URL + url.removePrefix(origin)
    }
    return url
}

private val LoopbackOrigins = listOf(
    "http://localhost:$SERVER_PORT",
    "http://127.0.0.1:$SERVER_PORT",
)
