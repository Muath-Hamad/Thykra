package com.jameeli.thykra.api

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
class IosUploadPersistence : UploadPersistence {

    private val queueDir: String
    private val json = Json { ignoreUnknownKeys = true }

    init {
        val docDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: ""
        queueDir = "$docDir/upload_queue"
        NSFileManager.defaultManager.createDirectoryAtPath(
            "$queueDir/staged",
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    override suspend fun loadAll(): List<PendingUploadRecord> {
        val text = readTextFile("$queueDir/queue.json") ?: return emptyList()
        return try {
            json.decodeFromString(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun save(record: PendingUploadRecord) {
        val records = loadAll().toMutableList()
        records.removeAll { it.id == record.id }
        records.add(record)
        writeFile("$queueDir/queue.json", json.encodeToString(records).encodeToByteArray())
    }

    override suspend fun remove(id: String) {
        val records = loadAll().filter { it.id != id }
        writeFile("$queueDir/queue.json", json.encodeToString(records).encodeToByteArray())
    }

    override suspend fun saveBytes(id: String, bytes: ByteArray) {
        writeFile("$queueDir/staged/$id.bin", bytes)
    }

    override suspend fun loadBytes(id: String): ByteArray? {
        return readFile("$queueDir/staged/$id.bin")
    }

    override suspend fun removeBytes(id: String) {
        NSFileManager.defaultManager.removeItemAtPath("$queueDir/staged/$id.bin", error = null)
    }

    private fun readTextFile(path: String): String? {
        return readFile(path)?.decodeToString()
    }

    private fun readFile(path: String): ByteArray? {
        val file = fopen(path, "rb") ?: return null
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt()
        fseek(file, 0, SEEK_SET)
        if (size <= 0) {
            fclose(file)
            return ByteArray(0)
        }
        val bytes = ByteArray(size)
        memScoped {
            val buf = allocArray<ByteVar>(size)
            fread(buf, 1u, size.toULong(), file)
            fclose(file)
            buf.readBytes(size).copyInto(bytes)
        }
        return bytes
    }

    private fun writeFile(path: String, bytes: ByteArray) {
        val file = fopen(path, "wb") ?: return
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
        }
        fclose(file)
    }
}
