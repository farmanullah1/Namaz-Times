package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_profiles")
data class MosqueProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val addressLabel: String,
    val latitude: Double?,
    val longitude: Double?,
    val timezoneId: String,
    val isEnabled: Boolean,
    val isArchived: Boolean,
    val userPriority: Int,
    val notes: String,
    val scheduleRevision: Long,
    val defaultReminderProfileId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "mosque_schedules",
    indices = [Index("mosqueId")],
    foreignKeys = [
        ForeignKey(
            entity = MosqueProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["mosqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MosqueScheduleEntity(
    @PrimaryKey val id: String,
    val mosqueId: String,
    val kind: String,
    val label: String,
    val weekday: Int?,
    val effectiveFrom: String?,
    val effectiveUntil: String?,
    val revision: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "mosque_schedule_entries",
    primaryKeys = ["scheduleId", "prayer"],
    indices = [Index("scheduleId")],
    foreignKeys = [
        ForeignKey(
            entity = MosqueScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleEntryEntity(
    val scheduleId: String,
    val prayer: String,
    val adhan: String?,
    val iqamah: String?,
    val adhanReminder: Boolean,
    val iqamahReminder: Boolean,
    val note: String,
    val khutbah: String?,
    val jumuah: String?,
    val secondJumuah: String?,
    val fridayNote: String
)

@Entity(
    tableName = "manual_overrides",
    primaryKeys = ["mosqueId", "date", "prayer"],
    indices = [Index("mosqueId")],
    foreignKeys = [
        ForeignKey(
            entity = MosqueProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["mosqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ManualOverrideEntity(
    val mosqueId: String,
    val date: String,
    val prayer: String,
    val adhan: String?,
    val iqamah: String?
)

@Entity(
    tableName = "reminder_preferences",
    primaryKeys = ["mosqueId", "prayer"],
    indices = [Index("mosqueId")],
    foreignKeys = [
        ForeignKey(
            entity = MosqueProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["mosqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderPreferenceEntity(
    val mosqueId: String,
    val prayer: String,
    val adhanEnabled: Boolean,
    val iqamahEnabled: Boolean,
    val preAdhanMinutes: Int?,
    val preIqamahMinutes: Int?,
    val soundMode: String,
    val customSoundUri: String?
)
