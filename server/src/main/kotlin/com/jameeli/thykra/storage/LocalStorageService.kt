package com.jameeli.thykra.storage

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalStorageService(
    val baseUrl: String,
    val storagePath: String
) : StorageService {

    init {
        File(storagePath).mkdirs()
    }

    override suspend fun generatePresignedUpload(key: String, contentType: String): PresignedUpload =
        PresignedUpload(uploadUrl = "$baseUrl/api/media/upload/$key")

    override fun getPublicUrl(key: String): String = "$baseUrl/api/media/files/$key"

    override suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            File(storagePath).resolve(key).delete()
        }
    }

    suspend fun saveFile(key: String, channel: ByteReadChannel) {
        val base = File(storagePath).canonicalFile
        val file = File(base, key).canonicalFile
        require(file.toPath().startsWith(base.toPath())) { "Invalid storage key" }
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.outputStream().use { out -> channel.copyTo(out) }
        }
    }

    override suspend fun readBytes(key: String): ByteArray? {
        val base = File(storagePath).canonicalFile
        val file = File(base, key).canonicalFile
        if (!file.toPath().startsWith(base.toPath()) || !file.exists()) return null
        return withContext(Dispatchers.IO) { file.readBytes() }
    }

    override suspend fun writeBytes(key: String, bytes: ByteArray) {
        val base = File(storagePath).canonicalFile
        val file = File(base, key).canonicalFile
        require(file.toPath().startsWith(base.toPath())) { "Invalid storage key" }
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
    }

    fun getFile(key: String): File? {
        val base = File(storagePath).canonicalFile
        val resolved = File(base, key).canonicalFile
        if (!resolved.toPath().startsWith(base.toPath())) return null
        return if (resolved.exists()) resolved else null
    }
}
