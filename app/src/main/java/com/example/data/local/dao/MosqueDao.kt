package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ManualOverrideEntity
import com.example.data.local.entity.MosqueProfileEntity
import com.example.data.local.entity.MosqueScheduleEntity
import com.example.data.local.entity.ReminderPreferenceEntity
import com.example.data.local.entity.ScheduleEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueDao {
    @Query("SELECT * FROM mosque_profiles ORDER BY isArchived ASC, userPriority ASC, name ASC")
    fun observeProfiles(): Flow<List<MosqueProfileEntity>>

    @Query("SELECT * FROM mosque_profiles ORDER BY isArchived ASC, userPriority ASC, name ASC")
    suspend fun getProfiles(): List<MosqueProfileEntity>

    @Query("SELECT * FROM mosque_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): MosqueProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: MosqueProfileEntity)

    @Query("DELETE FROM mosque_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM mosque_profiles")
    suspend fun deleteAllProfiles()

    @Query("SELECT * FROM mosque_schedules ORDER BY mosqueId ASC, kind ASC")
    suspend fun getSchedules(): List<MosqueScheduleEntity>

    @Query("SELECT * FROM mosque_schedules WHERE mosqueId = :mosqueId")
    suspend fun getSchedulesForMosque(mosqueId: String): List<MosqueScheduleEntity>

    @Query("SELECT * FROM mosque_schedules WHERE id = :id LIMIT 1")
    suspend fun getSchedule(id: String): MosqueScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: MosqueScheduleEntity)

    @Query("DELETE FROM mosque_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    @Query("DELETE FROM mosque_schedules")
    suspend fun deleteAllSchedules()

    @Query("SELECT * FROM mosque_schedule_entries")
    suspend fun getAllEntries(): List<ScheduleEntryEntity>

    @Query("SELECT * FROM mosque_schedule_entries WHERE scheduleId = :scheduleId")
    suspend fun getEntries(scheduleId: String): List<ScheduleEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<ScheduleEntryEntity>)

    @Query("DELETE FROM mosque_schedule_entries WHERE scheduleId = :scheduleId")
    suspend fun deleteEntries(scheduleId: String)

    @Query("DELETE FROM mosque_schedule_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM manual_overrides WHERE mosqueId = :mosqueId AND date = :date")
    suspend fun getOverrides(mosqueId: String, date: String): List<ManualOverrideEntity>

    @Query("SELECT * FROM manual_overrides")
    suspend fun getAllOverrides(): List<ManualOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: ManualOverrideEntity)

    @Query("DELETE FROM manual_overrides WHERE mosqueId = :mosqueId AND date = :date AND prayer = :prayer")
    suspend fun deleteOverride(mosqueId: String, date: String, prayer: String)

    @Query("DELETE FROM manual_overrides")
    suspend fun deleteAllOverrides()

    @Query("SELECT * FROM reminder_preferences")
    suspend fun getReminderPreferences(): List<ReminderPreferenceEntity>

    @Query("SELECT * FROM reminder_preferences WHERE mosqueId = :mosqueId")
    suspend fun getReminderPreferences(mosqueId: String): List<ReminderPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminderPreferences(preferences: List<ReminderPreferenceEntity>)

    @Query("DELETE FROM reminder_preferences")
    suspend fun deleteAllReminderPreferences()
}
