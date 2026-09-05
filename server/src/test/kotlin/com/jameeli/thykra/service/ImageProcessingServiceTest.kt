package com.jameeli.thykra.service

import com.jameeli.thykra.storage.PresignedUpload
import com.jameeli.thykra.storage.StorageService
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class InMemoryStorage : StorageService {
    val files = mutableMapOf<String, ByteArray>()
    override suspend fun generatePresignedUpload(key: String, contentType: String) =
        PresignedUpload(uploadUrl = "mem://$key")
    override fun getPublicUrl(key: String) = "mem://$key"
    override suspend fun delete(key: String) { files.remove(key) }
    override suspend fun readBytes(key: String): ByteArray? = files[key]
    override suspend fun writeBytes(key: String, bytes: ByteArray) { files[key] = bytes }
}

class ImageProcessingServiceTest {

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.ORANGE
        g.fillRect(0, 0, width, height)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    @Test
    fun extracts_dimensions_and_writes_bounded_thumbnail() = runBlocking {
        val storage = InMemoryStorage()
        storage.files["media/photo.png"] = pngBytes(800, 600)

        val meta = assertNotNull(ImageProcessingService(storage).processImage("media/photo.png"))
        assertEquals(800, meta.width)
        assertEquals(600, meta.height)
        assertNull(meta.takenAt, "synthetic PNG carries no EXIF date")
        assertEquals("media/photo-thumb.jpg", meta.thumbnailKey)

        val thumbBytes = assertNotNull(storage.files[meta.thumbnailKey])
        val thumb = assertNotNull(ImageIO.read(ByteArrayInputStream(thumbBytes)))
        // 800x600 downscaled into a 400x400 box keeping aspect ratio.
        assertEquals(400, thumb.width)
        assertEquals(300, thumb.height)
    }

    @Test
    fun small_images_still_produce_a_readable_thumbnail() = runBlocking {
        val storage = InMemoryStorage()
        storage.files["tiny.png"] = pngBytes(100, 50)

        val meta = assertNotNull(ImageProcessingService(storage).processImage("tiny.png"))
        assertEquals(100, meta.width)
        assertEquals(50, meta.height)
        val thumb = assertNotNull(ImageIO.read(ByteArrayInputStream(storage.files[meta.thumbnailKey])))
        assertTrue(maxOf(thumb.width, thumb.height) <= 400)
    }

    @Test
    fun missing_key_returns_null() = runBlocking {
        assertNull(ImageProcessingService(InMemoryStorage()).processImage("absent.png"))
    }

    @Test
    fun non_image_bytes_return_null() = runBlocking {
        val storage = InMemoryStorage()
        storage.files["media/junk.png"] = "definitely not an image".encodeToByteArray()
        assertNull(ImageProcessingService(storage).processImage("media/junk.png"))
        assertNull(storage.files["media/junk-thumb.jpg"], "no thumbnail must be written for junk input")
    }
}
