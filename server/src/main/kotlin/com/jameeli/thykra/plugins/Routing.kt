package com.jameeli.thykra.plugins

import com.jameeli.thykra.model.ClientConfigDto
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
import com.jameeli.thykra.repository.UserRepository
import com.jameeli.thykra.routes.activityFeedRoutes
import com.jameeli.thykra.routes.activityRoutes
import com.jameeli.thykra.routes.albumRoutes
import com.jameeli.thykra.routes.authRoutes
import com.jameeli.thykra.routes.commentRoutes
import com.jameeli.thykra.routes.configRoutes
import com.jameeli.thykra.routes.inviteRoutes
import com.jameeli.thykra.routes.mediaRoutes
import com.jameeli.thykra.routes.profileRoutes
import com.jameeli.thykra.routes.publicAlbumRoutes
import com.jameeli.thykra.routes.reactionRoutes
import com.jameeli.thykra.routes.recapRoutes
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
    blockedMemberRepository: BlockedMemberRepository,
    mediaService: MediaService,
    mediaRepository: MediaRepository,
    reactionRepository: ReactionRepository,
    commentRepository: CommentRepository,
    activityRepository: ActivityRepository,
    activityFeedRepository: ActivityFeedRepository,
    recapRepository: RecapRepository,
    storageService: StorageService,
    allowDevLogin: Boolean = false,
    clientConfig: ClientConfigDto = ClientConfigDto(maxUploadBytes = Long.MAX_VALUE)
) {
    routing {
        get("/") {
            call.respondText("Thykra API v1.0")
        }
        route("/api") {
            get("/health") {
                call.respondText("OK")
            }
            configRoutes(clientConfig)
            authRoutes(authService, allowDevLogin = allowDevLogin)
            profileRoutes(userRepository)
            albumRoutes(albumRepository, albumMemberRepository, albumInviteRepository, blockedMemberRepository)
            mediaRoutes(
                mediaService, mediaRepository, albumMemberRepository, storageService,
                maxUploadBytes = clientConfig.maxUploadBytes
            )
            reactionRoutes(reactionRepository, mediaRepository, albumMemberRepository)
            commentRoutes(commentRepository, mediaRepository, albumMemberRepository)
            activityRoutes(activityRepository)
            activityFeedRoutes(activityFeedRepository, albumMemberRepository)
            inviteRoutes(
                albumInviteRepository, albumRepository, albumMemberRepository,
                blockedMemberRepository, mediaRepository
            )
            recapRoutes(recapRepository, albumMemberRepository)
            publicAlbumRoutes(albumRepository)
        }
    }
}
