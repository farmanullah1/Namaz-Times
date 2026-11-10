package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_occurrences",
    indices = [Index("instantMillis"), Index("localDate"), Index("mosqueId")]
)
data class PrayerOccurrenceEntity(
    @PrimaryKey val id: String,
    val prayer: String,
    val eventType: String,
    val instantMillis: Long,
    val localDate: String,
    val mosqueId: String?,
    val placeId: String?,
    val source: String,
    val scheduleRevision: Long,
    val isEnabled: Boolean,
    val offsetMinutes: Int,
    val deliveredAtMillis: Long?
)

@Entity(tableName = "scheduled_alarms")
data class ScheduledAlarmEntity(
    @PrimaryKey val eventId: String,
    val requestCode: Int,
    val scheduledAtMillis: Long,
    val revision: Long,
    val createdAt: Long
)

@Entity(
    tableName = "prayer_history",
    indices = [
        Index(value = ["prayer", "trackingDate"], unique = true),
        Index("trackingDate")
    ]
)
data class PrayerHistoryEntity(
    @PrimaryKey val id: String,
    val prayer: String,
    val trackingDate: String,
    val scheduledAdhanMillis: Long?,
    val scheduledIqamahMillis: Long?,
    val completedAtMillis: Long,
    val mosqueId: String?,
    val placeId: String?,
    val scheduleRevision: Long?,
    val context: String,
    val tags: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "qada_records")
data class QadaRecordEntity(
    @PrimaryKey val id: String,
    val prayer: String,
    val performedDate: String,
    val note: String,
    val createdAt: Long
)
