package com.jameeli.thykra.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * OkHttp rather than CIO, and the reason is TLS.
 *
 * The app ships a `network-security-config` with `<domain-config>` entries (the cleartext
 * exemptions for the emulator loopback and the LAN box). As soon as any domain-specific
 * config exists, the platform installs `NetworkSecurityTrustManager`, which rejects the
 * two-argument `checkServerTrusted(chain, authType)` outright:
 *
 *     Domain specific configurations require that hostname aware
 *     checkServerTrusted(X509Certificate[], String, String) is used
 *
 * CIO implements TLS itself in Kotlin and calls exactly that two-argument overload, so
 * every https request throws before a byte is exchanged. It went unnoticed while Android
 * only ever spoke cleartext to 10.0.2.2; pointing a build at an https origin surfaced it
 * immediately. OkHttp goes through the platform socket factory, which uses the
 * hostname-aware overload, so the domain config is honoured instead of fought.
 *
 * OkHttp is also the engine Ktor recommends on Android generally — it brings connection
 * pooling and HTTP/2, which the upload queue benefits from.
 */
actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp)
