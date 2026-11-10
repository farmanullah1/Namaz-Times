package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PrayerHistoryEntity
import com.example.data.local.entity.PrayerOccurrenceEntity
import com.example.data.local.entity.QadaRecordEntity
import com.example.data.local.entity.ScheduledAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_occurrences WHERE instantMillis >= :fromMillis ORDER BY instantMillis ASC")
    suspend fun occurrencesFrom(fromMillis: Long): List<PrayerOccurrenceEntity>

    @Query("SELECT * FROM prayer_occurrences WHERE id = :id LIMIT 1")
    suspend fun occurrenceById(id: String): PrayerOccurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOccurrences(occurrences: List<PrayerOccurrenceEntity>)

    @Query("DELETE FROM prayer_occurrences WHERE instantMillis < :beforeMillis")
    suspend fun deleteOccurrencesBefore(beforeMillis: Long)

    @Query("DELETE FROM prayer_occurrences WHERE localDate >= :date")
    suspend fun deleteOccurrencesFromDate(date: String)

    @Query("DELETE FROM prayer_occurrences")
    suspend fun deleteAllOccurrences()

    @Query("UPDATE prayer_occurrences SET deliveredAtMillis = :deliveredAt WHERE id = :id")
    suspend fun markDelivered(id: String, deliveredAt: Long)

    @Query("SELECT * FROM scheduled_alarms ORDER BY scheduledAtMillis ASC")
    suspend fun getScheduledAlarms(): List<ScheduledAlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlarm(alarm: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE eventId = :eventId")
    suspend fun deleteAlarm(eventId: String)

    @Query("DELETE FROM scheduled_alarms")
    suspend fun deleteAllAlarms()

    @Query("SELECT * FROM prayer_history WHERE prayer = :prayer AND trackingDate = :date LIMIT 1")
    suspend fun findHistory(prayer: String, date: String): PrayerHistoryEntity?

    @Query("SELECT * FROM prayer_history WHERE trackingDate = :date ORDER BY prayer ASC")
    fun observeHistoryForDate(date: String): Flow<List<PrayerHistoryEntity>>

    @Query("SELECT * FROM prayer_history WHERE trackingDate = :date ORDER BY prayer ASC")
    suspend fun getHistoryForDate(date: String): List<PrayerHistoryEntity>

    @Query("SELECT * FROM prayer_history WHERE trackingDate BETWEEN :from AND :to ORDER BY trackingDate DESC, prayer ASC")
    suspend fun getHistoryBetween(from: String, to: String): List<PrayerHistoryEntity>

    @Query("SELECT * FROM prayer_history ORDER BY completedAtMillis DESC")
    fun observeAllHistory(): Flow<List<PrayerHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(record: PrayerHistoryEntity)

    @Query("DELETE FROM prayer_history WHERE id = :id")
    suspend fun deleteHistory(id: String)

    @Query("DELETE FROM prayer_history")
    suspend fun deleteAllHistory()

    @Query("SELECT * FROM qada_records ORDER BY createdAt DESC")
    fun observeQada(): Flow<List<QadaRecordEntity>>

    @Query("SELECT * FROM qada_records ORDER BY createdAt DESC")
    suspend fun getQada(): List<QadaRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQada(record: QadaRecordEntity)

    @Query("DELETE FROM qada_records WHERE id = :id")
    suspend fun deleteQada(id: String)

    @Query("DELETE FROM qada_records")
    suspend fun deleteAllQada()
}
