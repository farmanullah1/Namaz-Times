package com.example.data.repository

import com.example.data.local.Mappers.toDomain
import com.example.data.local.NamazDatabase
import com.example.data.local.PrayerHistoryRecord
import com.example.data.local.QadaRecord
import com.example.data.local.entity.PrayerHistoryEntity
import com.example.data.local.entity.QadaRecordEntity
import com.example.domain.model.PrayerName
import com.example.domain.model.PrayerOccurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Prayer completion records. Nothing is ever inferred from the clock or from GPS. */
class PrayerHistoryRepository(private val database: NamazDatabase) {

    fun observeHistory(date: LocalDate): Flow<List<PrayerHistoryRecord>> =
        database.prayerDao().observeHistoryForDate(date.toString())
            .map { rows -> rows.map { it.toDomain() } }

    fun observeAllHistory(): Flow<List<PrayerHistoryRecord>> =
        database.prayerDao().observeAllHistory().map { rows -> rows.map { it.toDomain() } }

    fun observeQada(): Flow<List<QadaRecord>> =
        database.prayerDao().observeQada().map { rows -> rows.map { it.toDomain() } }

    suspend fun historyForDate(date: LocalDate): List<PrayerHistoryRecord> =
        database.prayerDao().getHistoryForDate(date.toString()).map { it.toDomain() }

    suspend fun historyBetween(from: LocalDate, to: LocalDate): List<PrayerHistoryRecord> =
        database.prayerDao().getHistoryBetween(from.toString(), to.toString()).map { it.toDomain() }

    /** Marks a prayer prayed, updating the existing record for that prayer/day rather than duplicating. */
    suspend fun markPrayed(
        prayer: PrayerName,
        trackingDate: LocalDate,
        occurrence: PrayerOccurrence?,
        mosqueId: String?,
        placeId: String?,
        context: String,
        tags: String,
        note: String,
        completedAt: Instant = Instant.now()
    ): PrayerHistoryRecord {
        val now = System.currentTimeMillis()
        val existing = database.prayerDao().findHistory(prayer.name, trackingDate.toString())
        val record = PrayerHistoryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            prayer = prayer.name,
            trackingDate = trackingDate.toString(),
            scheduledAdhanMillis = existing?.scheduledAdhanMillis ?: occurrence?.takeIf { it.eventType.name == "ADHAN" }?.instant?.toEpochMilli(),
            scheduledIqamahMillis = existing?.scheduledIqamahMillis,
            completedAtMillis = completedAt.toEpochMilli(),
            mosqueId = mosqueId ?: existing?.mosqueId,
            placeId = placeId ?: existing?.placeId,
            scheduleRevision = occurrence?.scheduleRevision ?: existing?.scheduleRevision,
            context = context,
            tags = tags,
            note = note,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        database.prayerDao().upsertHistory(record)
        return record.toDomain()
    }

    suspend fun updateRecord(record: PrayerHistoryRecord): PrayerHistoryRecord {
        val existing = database.prayerDao().findHistory(record.prayer.name, record.trackingDate.toString())
        val entity = PrayerHistoryEntity(
            id = existing?.id ?: record.id,
            prayer = record.prayer.name,
            trackingDate = record.trackingDate.toString(),
            scheduledAdhanMillis = record.scheduledAdhan?.toEpochMilli(),
            scheduledIqamahMillis = record.scheduledIqamah?.toEpochMilli(),
            completedAtMillis = record.completedAt.toEpochMilli(),
            mosqueId = record.mosqueId,
            placeId = record.placeId,
            scheduleRevision = record.scheduleRevision,
            context = record.context,
            tags = record.tags,
            note = record.note,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.prayerDao().upsertHistory(entity)
        return entity.toDomain()
    }

    suspend fun undo(recordId: String) = database.prayerDao().deleteHistory(recordId)

    suspend fun addQada(prayer: PrayerName, performedDate: LocalDate, note: String) {
        database.prayerDao().upsertQada(
            QadaRecordEntity(
                id = UUID.randomUUID().toString(),
                prayer = prayer.name,
                performedDate = performedDate.toString(),
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteQada(id: String) = database.prayerDao().deleteQada(id)

    suspend fun qadaRecords(): List<QadaRecord> = database.prayerDao().getQada().map { it.toDomain() }

    suspend fun deleteAll() = database.prayerDao().deleteAllHistory()
}
