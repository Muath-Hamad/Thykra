package com.jameeli.thykra.api

import com.jameeli.thykra.model.ApiResponse
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.model.PresignedUploadDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Drives the queue against a mock HTTP engine — no real IO. Canned responses
 * are produced by serialising the real DTOs so a wire-format change surfaces
 * here instead of silently diverging.
 */
class UploadQueueManagerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun presignedBody(mediaId: String) = json.encodeToString(
        ApiResponse.serializer(PresignedUploadDto.serializer()),
        ApiResponse(
            success = true,
            data = PresignedUploadDto(
                mediaId = mediaId,
                storageKey = "albums/a1/$mediaId.jpg",
                // Deliberately not localhost: uploadFile rewrites localhost to the
                // platform API host; a neutral host keeps that rewrite a no-op.
                uploadUrl = "http://uploads.test/put/$mediaId"
            )
        )
    )

    private fun errorBody(message: String) = json.encodeToString(
        ApiResponse.serializer(PresignedUploadDto.serializer()),
        ApiResponse(success = false, error = message)
    )

    private fun mediaBody(mediaId: String) = json.encodeToString(
        ApiResponse.serializer(MediaDto.serializer()),
        ApiResponse(
            success = true,
            data = MediaDto(
                id = mediaId,
                albumId = "a1",
                uploaderId = "u1",
                type = MediaType.PHOTO,
                status = MediaStatus.ACTIVE,
                storageKey = "albums/a1/$mediaId.jpg",
                url = "http://cdn.test/$mediaId.jpg",
                filename = "photo.jpg",
                contentType = "image/jpeg",
                fileSize = 3,
                uploadedAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )
    )

    /** MediaApi whose request-upload endpoint fails the first [failFirstN] calls. */
    private fun mediaApi(
        mediaId: String = "m1",
        failFirstN: Int = 0,
        capturedUploads: MutableList<ByteArray>? = null
    ): MediaApi {
        var requestUploadCalls = 0
        val apiEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/media/request-upload") -> {
                    requestUploadCalls++
                    if (requestUploadCalls <= failFirstN) {
                        respond(errorBody("boom"), HttpStatusCode.OK, jsonHeaders)
                    } else {
                        respond(presignedBody(mediaId), HttpStatusCode.OK, jsonHeaders)
                    }
                }
                path.endsWith("/confirm") -> respond(mediaBody(mediaId), HttpStatusCode.OK, jsonHeaders)
                else -> respond("unexpected call: $path", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(apiEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val rawClient = HttpClient(MockEngine { request ->
            capturedUploads?.add(request.body.toByteArray())
            respond("", HttpStatusCode.OK)
        })
        return MediaApi(client, isDebug = false, rawClient = rawClient)
    }

    private fun request(filename: String = "photo.jpg", bytes: ByteArray = byteArrayOf(1, 2, 3)) =
        UploadRequest(
            albumId = "a1",
            filename = filename,
            contentType = "image/jpeg",
            fileSize = bytes.size.toLong(),
            readBytes = { bytes }
        )

    private suspend fun awaitTerminal(uploads: StateFlow<List<UploadState>>, id: String): UploadState =
        uploads.first { list ->
            list.any { it.id == id && (it.status == UploadStatus.DONE || it.status == UploadStatus.FAILED) }
        }.first { it.id == id }

    @Test
    fun happy_path_reaches_done_with_media_id() = runTest {
        val captured = mutableListOf<ByteArray>()
        val manager = UploadQueueManager(mediaApi(capturedUploads = captured), backgroundScope)

        val id = manager.enqueue(request())
        val terminal = awaitTerminal(manager.uploads, id)

        assertEquals(UploadStatus.DONE, terminal.status)
        assertEquals("m1", terminal.mediaId)
        assertEquals(1, terminal.attempt)
        assertEquals(1, captured.size, "exactly one raw upload expected")
        assertContentEquals(byteArrayOf(1, 2, 3), captured.single())
    }

    @Test
    fun transient_failure_retries_then_succeeds() = runTest {
        val manager = UploadQueueManager(mediaApi(failFirstN = 1), backgroundScope)

        val id = manager.enqueue(request())
        val terminal = awaitTerminal(manager.uploads, id)

        assertEquals(UploadStatus.DONE, terminal.status)
        assertEquals(2, terminal.attempt, "first attempt fails, second succeeds")
    }

    @Test
    fun permanent_failure_gives_up_after_three_attempts() = runTest {
        val manager = UploadQueueManager(mediaApi(failFirstN = Int.MAX_VALUE), backgroundScope)

        val id = manager.enqueue(request())
        val terminal = awaitTerminal(manager.uploads, id)

        assertEquals(UploadStatus.FAILED, terminal.status)
        assertEquals(3, terminal.attempt)
        assertEquals("boom", terminal.error)
    }

    @Test
    fun clear_completed_drops_terminal_states_only() = runTest {
        val doneManager = UploadQueueManager(mediaApi(), backgroundScope)
        val doneId = doneManager.enqueue(request())
        awaitTerminal(doneManager.uploads, doneId)
        doneManager.clearCompleted()
        assertTrue(doneManager.uploads.value.isEmpty(), "DONE entries must be cleared")

        val failedManager = UploadQueueManager(mediaApi(failFirstN = Int.MAX_VALUE), backgroundScope)
        val failedId = failedManager.enqueue(request())
        awaitTerminal(failedManager.uploads, failedId)
        failedManager.clearCompleted()
        assertTrue(failedManager.uploads.value.isEmpty(), "FAILED entries must be cleared")

        // A queued (network-gated) upload survives clearCompleted.
        val offline = object : NetworkMonitor {
            override val isConnected = MutableStateFlow(false)
        }
        val gatedManager = UploadQueueManager(mediaApi(), backgroundScope, networkMonitor = offline)
        val gatedId = gatedManager.enqueue(request())
        advanceUntilIdle()
        gatedManager.clearCompleted()
        assertEquals(gatedId, gatedManager.uploads.value.single().id)
    }

    @Test
    fun uploads_wait_for_connectivity() = runTest {
        val network = object : NetworkMonitor {
            override val isConnected = MutableStateFlow(false)
        }
        val manager = UploadQueueManager(mediaApi(), backgroundScope, networkMonitor = network)

        val id = manager.enqueue(request())
        advanceUntilIdle()
        assertEquals(UploadStatus.QUEUED, manager.uploads.value.single { it.id == id }.status)

        network.isConnected.value = true
        val terminal = awaitTerminal(manager.uploads, id)
        assertEquals(UploadStatus.DONE, terminal.status)
    }

    private class FakePersistence : UploadPersistence {
        val records = mutableMapOf<String, PendingUploadRecord>()
        val bytes = mutableMapOf<String, ByteArray>()
        override suspend fun loadAll(): List<PendingUploadRecord> = records.values.toList()
        override suspend fun save(record: PendingUploadRecord) { records[record.id] = record }
        override suspend fun remove(id: String) { records.remove(id) }
        override suspend fun saveBytes(id: String, bytes: ByteArray) { this.bytes[id] = bytes }
        override suspend fun loadBytes(id: String): ByteArray? = bytes[id]
        override suspend fun removeBytes(id: String) { bytes.remove(id) }
    }

    @Test
    fun persisted_uploads_are_saved_then_cleaned_up_after_done() = runTest {
        val persistence = FakePersistence()
        val manager = UploadQueueManager(mediaApi(), backgroundScope, persistence = persistence)

        val id = manager.enqueueWithPersistence(request(bytes = byteArrayOf(9, 9)))
        // Nothing has yielded to the queue worker yet, so the durable copy must exist.
        assertNotNull(persistence.records[id], "record persisted before processing")
        assertNotNull(persistence.bytes[id], "bytes persisted before processing")

        val terminal = awaitTerminal(manager.uploads, id)
        assertEquals(UploadStatus.DONE, terminal.status)
        assertTrue(persistence.records.isEmpty(), "record removed after DONE")
        assertTrue(persistence.bytes.isEmpty(), "bytes removed after DONE")
    }

    @Test
    fun pending_uploads_resume_from_persistence_on_startup() = runTest {
        val persistence = FakePersistence()
        val seeded = byteArrayOf(4, 5, 6)
        persistence.records["upload_restored"] = PendingUploadRecord(
            id = "upload_restored",
            albumId = "a1",
            filename = "restored.jpg",
            contentType = "image/jpeg",
            fileSize = seeded.size.toLong()
        )
        persistence.bytes["upload_restored"] = seeded

        val captured = mutableListOf<ByteArray>()
        val manager = UploadQueueManager(
            mediaApi(capturedUploads = captured),
            backgroundScope,
            persistence = persistence
        )

        val terminal = awaitTerminal(manager.uploads, "upload_restored")
        assertEquals(UploadStatus.DONE, terminal.status)
        assertContentEquals(seeded, captured.single(), "resumed upload must send the persisted bytes")
        assertTrue(persistence.records.isEmpty() && persistence.bytes.isEmpty())
    }
}
