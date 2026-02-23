package com.jameeli.thykra.service

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.jameeli.thykra.storage.StorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

data class ProcessedImageMeta(
    val width: Int,
    val height: Int,
    val takenAt: Instant?,
    val thumbnailKey: String
)

class ImageProcessingService(private val storageService: StorageService) {

    suspend fun processImage(storageKey: String): ProcessedImageMeta? {
        val bytes = storageService.readBytes(storageKey) ?: return null

        return withContext(Dispatchers.IO) {
            try {
                val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
                val width = image.width
                val height = image.height

                val takenAt: Instant? = try {
                    val metadata = ImageMetadataReader.readMetadata(ByteArrayInputStream(bytes))
                    val exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                    exif?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                        ?.let { Instant.fromEpochMilliseconds(it.time) }
                } catch (_: Exception) {
                    null
                }

                val thumbnailOut = ByteArrayOutputStream()
                Thumbnails.of(ByteArrayInputStream(bytes))
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputFormat("jpeg")
                    .outputQuality(0.85)
                    .toOutputStream(thumbnailOut)

                val baseName = storageKey.substringBeforeLast('.')
                val thumbnailKey = "$baseName-thumb.jpg"
                storageService.writeBytes(thumbnailKey, thumbnailOut.toByteArray())

                ProcessedImageMeta(width, height, takenAt, thumbnailKey)
            } catch (_: Exception) {
                null
            }
        }
    }
}
