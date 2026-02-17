package com.jameeli.thykra

import com.jameeli.thykra.auth.AppleOAuthVerifier
import com.jameeli.thykra.auth.GoogleOAuthVerifier
import com.jameeli.thykra.auth.JwtService
import com.jameeli.thykra.db.DatabaseFactory
import com.jameeli.thykra.plugins.configureCors
import com.jameeli.thykra.plugins.configureMonitoring
import com.jameeli.thykra.plugins.configureRouting
import com.jameeli.thykra.plugins.configureSecurity
import com.jameeli.thykra.plugins.configureSerialization
import com.jameeli.thykra.plugins.configureStatusPages
import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
import com.jameeli.thykra.repository.RefreshTokenRepository
import com.jameeli.thykra.repository.UserRepository
import com.jameeli.thykra.service.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init(environment)

    val jwtService = JwtService(environment)
    val userRepository = UserRepository()
    val refreshTokenRepository = RefreshTokenRepository()
    val albumRepository = AlbumRepository()
    val albumMemberRepository = AlbumMemberRepository()
    val albumInviteRepository = AlbumInviteRepository()

    val oauthHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }
    val googleVerifier = GoogleOAuthVerifier(environment, oauthHttpClient)
    val appleVerifier = AppleOAuthVerifier(environment, oauthHttpClient)
    val authService = AuthService(jwtService, userRepository, refreshTokenRepository, googleVerifier, appleVerifier)

    configureSerialization()
    configureMonitoring()
    configureCors()
    configureStatusPages()
    configureSecurity(jwtService)
    configureRouting(authService, userRepository, albumRepository, albumMemberRepository, albumInviteRepository)
}
