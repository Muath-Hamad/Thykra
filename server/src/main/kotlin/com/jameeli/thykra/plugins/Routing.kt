package com.jameeli.thykra.plugins

import com.jameeli.thykra.repository.AlbumInviteRepository
import com.jameeli.thykra.repository.AlbumMemberRepository
import com.jameeli.thykra.repository.AlbumRepository
import com.jameeli.thykra.repository.MediaRepository
import com.jameeli.thykra.repository.ReactionRepository
import com.jameeli.thykra.repository.UserRepository
import com.jameeli.thykra.routes.albumRoutes
import com.jameeli.thykra.routes.authRoutes
import com.jameeli.thykra.routes.mediaRoutes
import com.jameeli.thykra.routes.profileRoutes
import com.jameeli.thykra.routes.reactionRoutes
import com.jameeli.thykra.service.AuthService
import com.jameeli.thykra.service.MediaService
import com.jameeli.thykra.storage.StorageService
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    userRepository: UserRepository,
    albumRepository: AlbumRepository,
    albumMemberRepository: AlbumMemberRepository,
    albumInviteRepository: AlbumInviteRepository,
    mediaService: MediaService,
    mediaRepository: MediaRepository,
    reactionRepository: ReactionRepository,
    storageService: StorageService
) {
    routing {
        get("/") {
            call.respondText("Thykra API v1.0")
        }
        route("/api") {
            get("/health") {
                call.respondText("OK")
            }
            authRoutes(authService)
            profileRoutes(userRepository)
            albumRoutes(albumRepository, albumMemberRepository, albumInviteRepository)
            mediaRoutes(mediaService, mediaRepository, albumMemberRepository, storageService)
            reactionRoutes(reactionRepository, mediaRepository, albumMemberRepository)
        }
    }
}
