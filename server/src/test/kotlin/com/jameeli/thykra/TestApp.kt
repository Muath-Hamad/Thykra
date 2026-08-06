package com.jameeli.thykra

import com.jameeli.thykra.db.DatabaseFactory
import com.jameeli.thykra.db.tables.AlbumInvitesTable
import com.jameeli.thykra.db.tables.AlbumMembersTable
import com.jameeli.thykra.db.tables.CommentsTable
import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.db.tables.ReactionsTable
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.AuthResponse
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.ReactionType
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.io.path.createTempDirectory

/**
 * Boots the full production module() against a unique in-memory H2 database and a
 * temp-dir local storage, with dev-login enabled so tests can mint real bearer tokens.
 */
fun thykraTestApp(block: suspend ApplicationTestBuilder.() -> Unit) {
    try {
        testApplication {
            val dbName = "thykra_test_" + UUID.randomUUID().toString().replace("-", "")
            val storageDir = createTempDirectory("thykra-test-storage").toString()
            environment {
                config = MapApplicationConfig(
                    "database.url" to "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "database.driver" to "org.h2.Driver",
                    "database.user" to "sa",
                    "database.password" to "",
                    "database.maxPoolSize" to "5",
                    "jwt.issuer" to "thykra-api",
                    "jwt.audience" to "thykra-users",
                    "jwt.realm" to "thykra",
                    "jwt.secret" to "test-secret",
                    "jwt.accessTokenExpiry" to "3600",
                    "jwt.refreshTokenExpiry" to "2592000",
                    "storage.type" to "local",
                    "storage.localPath" to storageDir,
                    "storage.baseUrl" to "http://localhost:8081",
                    "auth.allowDevLogin" to "true",
                    "oauth.google.clientId" to "",
                    "oauth.apple.clientId" to ""
                )
            }
            application { module() }
            // Force the app (and thus DatabaseFactory.init) to start before the test body runs,
            // so direct-DB helpers in TestData see the connected database.
            startApplication()
            block()
        }
    } finally {
        // Shut the per-test H2 database down — otherwise every test leaves a live
        // in-memory database and connection pool behind for the rest of the JVM.
        DatabaseFactory.close()
    }
}

/**
 * H2 (like Postgres) stores timestamps at microsecond precision, while
 * Clock.System.now() carries nanoseconds — truncate before comparing a value
 * that made a round-trip through the database.
 */
fun Instant.dbPrecision(): Instant =
    Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond / 1000 * 1000)

val testJson = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> HttpResponse.api(): ApiResponse<T> =
    testJson.decodeFromString<ApiResponse<T>>(bodyAsText())

suspend inline fun <reified T> HttpResponse.apiData(): T {
    val envelope = api<T>()
    check(envelope.success) { "Expected success response but got error=${envelope.error}" }
    return checkNotNull(envelope.data) { "Expected data in response" }
    }

fun HttpRequestBuilder.bearer(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}

fun HttpRequestBuilder.jsonBody(body: String) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

/** Creates (or logs in) a user through the dev-login backdoor and returns real tokens. */
suspend fun ApplicationTestBuilder.devLogin(email: String, displayName: String): AuthResponse {
    val response = client.post("/api/auth/dev-login") {
        jsonBody("""{"email":"$email","displayName":"$displayName"}""")
    }
    check(response.status == HttpStatusCode.OK) { "dev-login failed: ${response.bodyAsText()}" }
    return response.apiData<AuthResponse>()
}

suspend fun ApplicationTestBuilder.createAlbum(token: String, title: String): AlbumDto {
    val response = client.post("/api/albums") {
        bearer(token)
        jsonBody("""{"title":"$title"}""")
    }
    check(response.status == HttpStatusCode.Created) { "createAlbum failed: ${response.bodyAsText()}" }
    return response.apiData<AlbumDto>()
}

suspend fun ApplicationTestBuilder.createInvite(token: String, albumId: String): InviteLinkDto {
    val response = client.post("/api/albums/$albumId/invite") {
        bearer(token)
        jsonBody("{}")
    }
    check(response.status == HttpStatusCode.Created) { "createInvite failed: ${response.bodyAsText()}" }
    return response.apiData<InviteLinkDto>()
}

suspend fun ApplicationTestBuilder.joinAlbum(token: String, inviteToken: String): HttpResponse =
    client.post("/api/albums/join/$inviteToken") { bearer(token) }

/**
 * Direct-DB fixtures for rows the API cannot create with controlled timestamps
 * (media without going through presigned upload, backdated reactions/comments, etc.).
 * Runs against the app's connected database — call only inside thykraTestApp.
 */
object TestData {

    fun insertMedia(
        albumId: String,
        uploaderId: String,
        uploadedAt: Instant,
        takenAt: Instant? = null,
        status: String = "ACTIVE",
        type: String = "PHOTO"
    ): String = transaction {
        val id = UUID.randomUUID()
        MediaTable.insert {
            it[MediaTable.id] = id
            it[MediaTable.albumId] = UUID.fromString(albumId)
            it[MediaTable.uploaderId] = UUID.fromString(uploaderId)
            it[MediaTable.type] = type
            it[MediaTable.status] = status
            it[MediaTable.storageKey] = "test/$id.jpg"
            it[MediaTable.thumbnailKey] = "test/$id-thumb.jpg"
            it[MediaTable.filename] = "$id.jpg"
            it[MediaTable.contentType] = "image/jpeg"
            it[MediaTable.fileSize] = 1024L
            it[MediaTable.takenAt] = takenAt
            it[MediaTable.uploadedAt] = uploadedAt
            it[MediaTable.updatedAt] = uploadedAt
        }
        id.toString()
    }

    fun insertReaction(mediaId: String, userId: String, type: ReactionType, createdAt: Instant): String =
        transaction {
            val id = UUID.randomUUID()
            ReactionsTable.insert {
                it[ReactionsTable.id] = id
                it[ReactionsTable.mediaId] = UUID.fromString(mediaId)
                it[ReactionsTable.userId] = UUID.fromString(userId)
                it[ReactionsTable.type] = type.name
                it[ReactionsTable.createdAt] = createdAt
            }
            id.toString()
        }

    fun insertComment(mediaId: String, authorId: String, body: String, createdAt: Instant): String =
        transaction {
            val id = UUID.randomUUID()
            CommentsTable.insert {
                it[CommentsTable.id] = id
                it[CommentsTable.mediaId] = UUID.fromString(mediaId)
                it[CommentsTable.authorId] = UUID.fromString(authorId)
                it[CommentsTable.body] = body
                it[CommentsTable.createdAt] = createdAt
                it[CommentsTable.updatedAt] = createdAt
            }
            id.toString()
        }

    fun insertMember(albumId: String, userId: String, role: MemberRole, joinedAt: Instant) {
        transaction {
            AlbumMembersTable.insert {
                it[AlbumMembersTable.id] = UUID.randomUUID()
                it[AlbumMembersTable.albumId] = UUID.fromString(albumId)
                it[AlbumMembersTable.userId] = UUID.fromString(userId)
                it[AlbumMembersTable.role] = role.name
                it[AlbumMembersTable.joinedAt] = joinedAt
            }
        }
    }

    fun insertInvite(
        albumId: String,
        createdBy: String,
        expiresAt: Instant,
        revokedAt: Instant? = null
    ): String = transaction {
        val token = UUID.randomUUID().toString()
        AlbumInvitesTable.insert {
            it[AlbumInvitesTable.id] = UUID.randomUUID()
            it[AlbumInvitesTable.albumId] = UUID.fromString(albumId)
            it[AlbumInvitesTable.token] = token
            it[AlbumInvitesTable.createdBy] = UUID.fromString(createdBy)
            it[AlbumInvitesTable.expiresAt] = expiresAt
            it[AlbumInvitesTable.revokedAt] = revokedAt
            it[AlbumInvitesTable.createdAt] = Clock.System.now()
        }
        token
    }
}
