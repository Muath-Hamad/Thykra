package com.jameeli.thykra.api

import kotlinx.serialization.Serializable

@Serializable
data class PendingUploadRecord(
    val id: String,
    val albumId: String,
    val filename: String,
    val contentType: String,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val takenAtMs: Long? = null
)

interface UploadPersistence {
    suspend fun loadAll(): List<PendingUploadRecord>
    suspend fun save(record: PendingUploadRecord)
    suspend fun remove(id: String)
    suspend fun saveBytes(id: String, bytes: ByteArray)
    suspend fun loadBytes(id: String): ByteArray?
    suspend fun removeBytes(id: String)
}
