package com.jameeli.thykra

const val SERVER_PORT = 8081
expect val API_HOST: String
val API_BASE_URL: String get() = "http://$API_HOST:$SERVER_PORT"

/**
 * Origin where the web app is served. Used by clients that need to mint
 * shareable URLs (e.g. public album links). Each platform supplies a
 * sensible dev default; production builds should override at build time
 * (see Platform.<target>.kt for instructions).
 */
expect val WEB_BASE_URL: String
