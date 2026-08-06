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
import com.jameeli.thykra.repository.ActivityFeedRepository
import com.jameeli.thykra.repository.ActivityRepository
import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
import com.jameeli.thykra.repository.BlockedMemberRepository
import com.jameeli.thykra.repository.CommentRepository
import com.jameeli.thykra.repository.MediaRepository
import com.jameeli.thykra.repository.ReactionRepository
import com.jameeli.thykra.repository.RecapRepository
import com.jameeli.thykra.repository.RefreshTokenRepository
import com.jameeli.thykra.repository.UserRepository
import com.jameeli.thykra.service.AuthService
import com.jameeli.thykra.service.ImageProcessingService
import com.jameeli.thykra.service.MediaService
import com.jameeli.thykra.storage.LocalStorageService
import com.jameeli.thykra.storage.S3StorageService
import com.jameeli.thykra.storage.StorageService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment)

    val jwtService = JwtService(environment)
    val userRepository = UserRepository()
    val refreshTokenRepository = RefreshTokenRepository()
    val albumMemberRepository = AlbumMemberRepository()
    val albumInviteRepository = AlbumInviteRepository()
    val blockedMemberRepository = BlockedMemberRepository()

    val storageType = environment.config.propertyOrNull("storage.type")?.getString() ?: "local"
    val storageService: StorageService = when (storageType) {
        "s3" -> S3StorageService(
            bucket = environment.config.property("storage.s3.bucket").getString(),
            region = environment.config.property("storage.s3.region").getString(),
            endpoint = environment.config.propertyOrNull("storage.s3.endpoint")?.getString()?.takeIf { it.isNotBlank() },
            accessKey = environment.config.propertyOrNull("storage.s3.accessKey")?.getString()?.takeIf { it.isNotBlank() },
            secretKey = environment.config.propertyOrNull("storage.s3.secretKey")?.getString()?.takeIf { it.isNotBlank() },
            publicBaseUrl = environment.config.propertyOrNull("storage.s3.publicBaseUrl")?.getString()?.takeIf { it.isNotBlank() },
            pathStyleAccess = environment.config.propertyOrNull("storage.s3.pathStyleAccess")?.getString()?.toBoolean() ?: false,
            presignExpirySeconds = environment.config.propertyOrNull("storage.s3.presignExpirySeconds")?.getString()?.toIntOrNull() ?: 3600,
        )
        else -> LocalStorageService(
            baseUrl = environment.config.property("storage.baseUrl").getString(),
            storagePath = environment.config.property("storage.localPath").getString()
        )
    }
    val albumRepository = AlbumRepository(storageService)
    val mediaRepository = MediaRepository(storageService)
    val reactionRepository = ReactionRepository()
    val commentRepository = CommentRepository()
    val activityRepository = ActivityRepository(storageService)
    val activityFeedRepository = ActivityFeedRepository(storageService)
    val recapRepository = RecapRepository(storageService)
    val imageProcessingService = ImageProcessingService(storageService)
    val mediaService = MediaService(mediaRepository, storageService, imageProcessingService)

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
    val allowDevLogin = environment.config.propertyOrNull("auth.allowDevLogin")?.getString()?.toBoolean() ?: false

    configureRouting(
        authService, userRepository, albumRepository, albumMemberRepository, albumInviteRepository,
        blockedMemberRepository, mediaService, mediaRepository, reactionRepository, commentRepository,
        activityRepository, activityFeedRepository, recapRepository, storageService, allowDevLogin
    )
}
