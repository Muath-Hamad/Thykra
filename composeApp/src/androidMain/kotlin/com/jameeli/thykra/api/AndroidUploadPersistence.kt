package com.jameeli.thykra.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AndroidUploadPersistence(context: Context) : UploadPersistence {

    private val queueDir = File(context.filesDir, "upload_queue").also { it.mkdirs() }
    private val stagedDir = File(queueDir, "staged").also { it.mkdirs() }
    private val queueFile = File(queueDir, "queue.json")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadAll(): List<PendingUploadRecord> = withContext(Dispatchers.IO) {
        mutex.withLock { loadAllUnlocked() }
    }

    override suspend fun save(record: PendingUploadRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val records = loadAllUnlocked().toMutableList()
            records.removeAll { it.id == record.id }
            records.add(record)
            queueFile.writeText(json.encodeToString(records))
        }
    }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val records = loadAllUnlocked().filter { it.id != id }
            queueFile.writeText(json.encodeToString(records))
        }
    }

    override suspend fun saveBytes(id: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        File(stagedDir, "$id.bin").writeBytes(bytes)
    }

    override suspend fun loadBytes(id: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(stagedDir, "$id.bin")
        if (file.exists()) file.readBytes() else null
    }

    override suspend fun removeBytes(id: String) {
        withContext(Dispatchers.IO) {
            File(stagedDir, "$id.bin").delete()
        }
    }

    private fun loadAllUnlocked(): List<PendingUploadRecord> {
        if (!queueFile.exists()) return emptyList()
        return try {
            json.decodeFromString(queueFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }
}
