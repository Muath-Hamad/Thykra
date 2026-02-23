package com.jameeli.thykra.repository

import com.jameeli.thykra.db.tables.MediaTable
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.storage.StorageService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class MediaRepository(private val storageService: StorageService) {

    suspend fun create(
        albumId: UUID,
        uploaderId: UUID,
        type: MediaType,
        storageKey: String,
        filename: String,
        contentType: String,
        fileSize: Long
    ): MediaDto = newSuspendedTransaction {
        val now = Clock.System.now()
        val mediaId = UUID.randomUUID()
        MediaTable.insert {
            it[MediaTable.id] = mediaId
            it[MediaTable.albumId] = albumId
            it[MediaTable.uploaderId] = uploaderId
            it[MediaTable.type] = type.name
            it[MediaTable.status] = MediaStatus.PENDING.name
            it[MediaTable.storageKey] = storageKey
            it[MediaTable.filename] = filename
            it[MediaTable.contentType] = contentType
            it[MediaTable.fileSize] = fileSize
            it[MediaTable.uploadedAt] = now
            it[MediaTable.updatedAt] = now
        }
        MediaDto(
            id = mediaId.toString(),
            albumId = albumId.toString(),
            uploaderId = uploaderId.toString(),
            type = type,
            status = MediaStatus.PENDING,
            storageKey = storageKey,
            url = storageService.getPublicUrl(storageKey),
            filename = filename,
            contentType = contentType,
            fileSize = fileSize,
            uploadedAt = now
        )
    }

    suspend fun findById(id: UUID): MediaDto? = newSuspendedTransaction {
        MediaTable.selectAll().where { MediaTable.id eq id }
            .singleOrNull()?.toMediaDto()
    }

    suspend fun findByStorageKey(key: String): MediaDto? = newSuspendedTransaction {
        MediaTable.selectAll().where { MediaTable.storageKey eq key }
            .singleOrNull()?.toMediaDto()
    }

    suspend fun findAllForAlbum(albumId: UUID): List<MediaDto> = newSuspendedTransaction {
        MediaTable.selectAll()
            .where { (MediaTable.albumId eq albumId) and (MediaTable.status eq MediaStatus.ACTIVE.name) }
            .orderBy(MediaTable.uploadedAt, SortOrder.DESC)
            .map { it.toMediaDto() }
    }

    suspend fun activate(
        id: UUID,
        width: Int?,
        height: Int?,
        durationMs: Long?,
        takenAt: Instant?
    ): MediaDto? = newSuspendedTransaction {
        val now = Clock.System.now()
        MediaTable.update({ MediaTable.id eq id }) {
            it[MediaTable.status] = MediaStatus.ACTIVE.name
            if (width != null) it[MediaTable.width] = width
            if (height != null) it[MediaTable.height] = height
            if (durationMs != null) it[MediaTable.durationMs] = durationMs
            if (takenAt != null) it[MediaTable.takenAt] = takenAt
            it[MediaTable.updatedAt] = now
        }
        MediaTable.selectAll().where { MediaTable.id eq id }.singleOrNull()?.toMediaDto()
    }

    suspend fun delete(id: UUID) {
        newSuspendedTransaction {
            MediaTable.deleteWhere { MediaTable.id eq id }
        }
    }

    private fun ResultRow.toMediaDto() = MediaDto(
        id = this[MediaTable.id].value.toString(),
        albumId = this[MediaTable.albumId].value.toString(),
        uploaderId = this[MediaTable.uploaderId].value.toString(),
        type = MediaType.valueOf(this[MediaTable.type]),
        status = MediaStatus.valueOf(this[MediaTable.status]),
        storageKey = this[MediaTable.storageKey],
        url = storageService.getPublicUrl(this[MediaTable.storageKey]),
        filename = this[MediaTable.filename],
        contentType = this[MediaTable.contentType],
        fileSize = this[MediaTable.fileSize],
        width = this[MediaTable.width],
        height = this[MediaTable.height],
        durationMs = this[MediaTable.durationMs],
        takenAt = this[MediaTable.takenAt],
        uploadedAt = this[MediaTable.uploadedAt]
    )
}
