package com.jameeli.thykra.storage

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalStorageServiceTest {

    private lateinit var baseDir: File
    private lateinit var storage: LocalStorageService

    @BeforeTest
    fun setUp() {
        baseDir = createTempDirectory("thykra-storage-test").toFile()
        storage = LocalStorageService(baseUrl = "http://localhost:8081", storagePath = baseDir.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        baseDir.deleteRecursively()
    }

    @Test
    fun write_then_read_round_trips() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        storage.writeBytes("photo.jpg", bytes)
        assertContentEquals(bytes, storage.readBytes("photo.jpg"))
    }

    @Test
    fun nested_keys_create_parent_directories() = runBlocking {
        val bytes = "hello".encodeToByteArray()
        storage.writeBytes("albums/a1/media/m1.bin", bytes)
        assertContentEquals(bytes, storage.readBytes("albums/a1/media/m1.bin"))
        assertTrue(File(baseDir, "albums/a1/media/m1.bin").isFile)
    }

    @Test
    fun read_missing_key_returns_null() = runBlocking {
        assertNull(storage.readBytes("does-not-exist.bin"))
    }

    @Test
    fun path_traversal_keys_are_rejected() = runBlocking {
        val outside = File(baseDir.parentFile, "escape.bin")
        try {
            assertFailsWith<IllegalArgumentException> {
                storage.writeBytes("../escape.bin", byteArrayOf(1))
            }
            assertFalse(outside.exists(), "traversal write must not land outside the storage root")
            assertNull(storage.readBytes("../escape.bin"))
            assertNull(storage.getFile("../escape.bin"))
        } finally {
            outside.delete()
        }
    }

    @Test
    fun delete_removes_the_file() = runBlocking {
        storage.writeBytes("gone.bin", byteArrayOf(9))
        storage.delete("gone.bin")
        assertNull(storage.readBytes("gone.bin"))
    }

    @Test
    fun get_file_returns_existing_files_only() = runBlocking {
        assertNull(storage.getFile("missing.bin"))
        storage.writeBytes("present.bin", byteArrayOf(7))
        val file = storage.getFile("present.bin")
        assertTrue(file != null && file.isFile)
    }

    @Test
    fun urls_follow_the_local_upload_and_files_scheme() = runBlocking {
        assertEquals("http://localhost:8081/api/media/files/a/b.jpg", storage.getPublicUrl("a/b.jpg"))
        val presigned = storage.generatePresignedUpload("a/b.jpg", "image/jpeg")
        assertEquals("http://localhost:8081/api/media/upload/a/b.jpg", presigned.uploadUrl)
        assertEquals("PUT", presigned.method)
        assertTrue(presigned.headers.isEmpty())
    }
}
