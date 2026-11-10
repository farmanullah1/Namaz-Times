package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.NamazDatabase
import com.example.data.local.entity.ManualOverrideEntity
import com.example.data.local.entity.MosqueProfileEntity
import com.example.data.local.entity.MosqueScheduleEntity
import com.example.data.local.entity.PrayerHistoryEntity
import com.example.data.local.entity.QadaRecordEntity
import com.example.data.local.entity.QuranGoalEntity
import com.example.data.local.entity.QuranSessionEntity
import com.example.data.local.entity.ReminderPreferenceEntity
import com.example.data.local.entity.SavedPlaceEntity
import com.example.data.local.entity.ScheduleEntryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

data class BackupPreview(
    val valid: Boolean,
    val schemaVersion: Int,
    val places: Int = 0,
    val mosques: Int = 0,
    val schedules: Int = 0,
    val entries: Int = 0,
    val overrides: Int = 0,
    val reminderPreferences: Int = 0,
    val history: Int = 0,
    val qada: Int = 0,
    val quranSessions: Int = 0,
    val quranGoals: Int = 0,
    val hasSettings: Boolean = false,
    val errors: List<String> = emptyList()
) {
    val totalRecords: Int
        get() = places + mosques + schedules + entries + overrides + history + qada + quranSessions
}

/**
 * Local backup/restore. No code from a backup is ever executed, no audio binaries are imported, and
 * a Replace import writes a safety backup of the current data before touching live tables.
 */
class BackupRepository(
    private val context: Context,
    private val database: NamazDatabase,
    private val settingsRepository: SettingsRepository
) {

    suspend fun exportJson(): String {
        val snapshot = MosqueRepository(database).snapshot()
        val dao = database.mosqueDao()
        val prayerDao = database.prayerDao()
        val quranDao = database.quranDao()

        val json = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("app", "Namaz Times")
            put("exportedAt", Instant.now().toString())
            put("settings", JSONObject(settingsRepository.encode(settingsRepository.current())))
            put("places", JSONArray().apply {
                snapshot.places.forEach { place ->
                    put(JSONObject().apply {
                        put("id", place.id)
                        put("displayName", place.displayName)
                        putNullable("latitude", place.latitude)
                        putNullable("longitude", place.longitude)
                        put("geofenceRadiusMeters", place.geofenceRadiusMeters)
                        put("timezoneId", place.timezoneId)
                        putNullable("mosqueProfileId", place.mosqueProfileId)
                        put("priorityRank", place.priorityRank)
                        put("isEnabled", place.isEnabled)
                        put("isPriorityLocation", place.isPriorityLocation)
                        put("createdAt", place.createdAt)
                        put("updatedAt", place.updatedAt)
                    })
                }
            })
            put("mosques", JSONArray().apply {
                snapshot.mosques.forEach { mosque ->
                    put(JSONObject().apply {
                        put("id", mosque.id)
                        put("name", mosque.name)
                        put("addressLabel", mosque.addressLabel)
                        putNullable("latitude", mosque.latitude)
                        putNullable("longitude", mosque.longitude)
                        put("timezoneId", mosque.timezoneId)
                        put("isEnabled", mosque.isEnabled)
                        put("isArchived", mosque.isArchived)
                        put("userPriority", mosque.userPriority)
                        put("notes", mosque.notes)
                        put("scheduleRevision", mosque.scheduleRevision)
                        putNullable("defaultReminderProfileId", mosque.defaultReminderProfileId)
                        put("createdAt", mosque.createdAt)
                        put("updatedAt", mosque.updatedAt)
                    })
                }
            })
            put("schedules", JSONArray().apply {
                snapshot.schedules.forEach { schedule ->
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("mosqueId", schedule.mosqueId)
                        put("kind", schedule.kind.name)
                        put("label", schedule.label)
                        putNullable("weekday", schedule.weekday)
                        putNullable("effectiveFrom", schedule.effectiveFrom?.toString())
                        putNullable("effectiveUntil", schedule.effectiveUntil?.toString())
                        put("revision", schedule.revision)
                        put("isActive", schedule.isActive)
                        put("createdAt", schedule.createdAt)
                        put("updatedAt", schedule.updatedAt)
                    })
                }
            })
            put("entries", JSONArray().apply {
                snapshot.entries.forEach { entry ->
                    put(JSONObject().apply {
                        put("scheduleId", entry.scheduleId)
                        put("prayer", entry.prayer.name)
                        putNullable("adhan", entry.adhan)
                        putNullable("iqamah", entry.iqamah)
                        put("adhanReminder", entry.adhanReminder)
                        put("iqamahReminder", entry.iqamahReminder)
                        put("note", entry.note)
                        putNullable("khutbah", entry.khutbah)
                        putNullable("jumuah", entry.jumuah)
                        putNullable("secondJumuah", entry.secondJumuah)
                        put("fridayNote", entry.fridayNote)
                    })
                }
            })
            put("overrides", JSONArray().apply {
                snapshot.overrides.forEach { override ->
                    put(JSONObject().apply {
                        put("mosqueId", override.mosqueId)
                        put("date", override.date.toString())
                        put("prayer", override.prayer.name)
                        putNullable("adhan", override.adhan)
                        putNullable("iqamah", override.iqamah)
                    })
                }
            })
            put("reminderPreferences", JSONArray().apply {
                snapshot.reminderPreferences.forEach { preference ->
                    put(JSONObject().apply {
                        put("mosqueId", preference.mosqueId)
                        put("prayer", preference.prayer.name)
                        put("adhanEnabled", preference.adhanEnabled)
                        put("iqamahEnabled", preference.iqamahEnabled)
                        putNullable("preAdhanMinutes", preference.preAdhanMinutes)
                        putNullable("preIqamahMinutes", preference.preIqamahMinutes)
                        put("soundMode", preference.soundMode.name)
                        putNullable("customSoundUri", preference.customSoundUri)
                    })
                }
            })
            put("history", JSONArray().apply {
                prayerDao.getHistoryBetween("0001-01-01", "9999-12-31").forEach { record ->
                    put(JSONObject().apply {
                        put("id", record.id)
                        put("prayer", record.prayer)
                        put("trackingDate", record.trackingDate)
                        putNullable("scheduledAdhanMillis", record.scheduledAdhanMillis)
                        putNullable("scheduledIqamahMillis", record.scheduledIqamahMillis)
                        put("completedAtMillis", record.completedAtMillis)
                        putNullable("mosqueId", record.mosqueId)
                        putNullable("placeId", record.placeId)
                        putNullable("scheduleRevision", record.scheduleRevision)
                        put("context", record.context)
                        put("tags", record.tags)
                        put("note", record.note)
                        put("createdAt", record.createdAt)
                        put("updatedAt", record.updatedAt)
                    })
                }
            })
            put("qada", JSONArray().apply {
                prayerDao.getQada().forEach { record ->
                    put(JSONObject().apply {
                        put("id", record.id)
                        put("prayer", record.prayer)
                        put("performedDate", record.performedDate)
                        put("note", record.note)
                        put("createdAt", record.createdAt)
                    })
                }
            })
            put("quranSessions", JSONArray().apply {
                quranDao.getSessions().forEach { session ->
                    put(JSONObject().apply {
                        put("id", session.id)
                        put("dateKey", session.dateKey)
                        put("occurredAtMillis", session.occurredAtMillis)
                        put("rukus", session.rukus)
                        putNullable("surah", session.surah)
                        putNullable("rukuNumber", session.rukuNumber)
                        put("note", session.note)
                        put("isCorrection", session.isCorrection)
                        putNullable("correctionReason", session.correctionReason)
                        put("createdAt", session.createdAt)
                    })
                }
            })
            put("quranGoals", JSONArray().apply {
                quranDao.getGoals().forEach { goal ->
                    put(JSONObject().apply {
                        put("effectiveFrom", goal.effectiveFrom)
                        put("dailyGoal", goal.dailyGoal)
                    })
                }
            })
        }
        return json.toString(2)
    }

    fun preview(raw: String): BackupPreview {
        val json = try {
            JSONObject(raw)
        } catch (error: org.json.JSONException) {
            return BackupPreview(
                valid = false,
                schemaVersion = -1,
                errors = listOf("The selected file is not valid JSON.")
            )
        }
        val version = json.optInt("schemaVersion", -1)
        if (version != SCHEMA_VERSION) {
            return BackupPreview(
                valid = false,
                schemaVersion = version,
                errors = listOf("Unsupported backup version ($version). This app supports version $SCHEMA_VERSION.")
            )
        }
        val errors = mutableListOf<String>()
        if (json.optString("app") != "Namaz Times") {
            errors += "The file does not identify itself as a Namaz Times backup."
        }
        val arrayKeys = listOf(
            "places", "mosques", "schedules", "entries", "overrides",
            "reminderPreferences", "history", "qada", "quranSessions", "quranGoals"
        )
        if (arrayKeys.none { json.has(it) }) {
            errors += "The backup contains no recognizable app data."
        }
        return BackupPreview(
            valid = errors.isEmpty(),
            schemaVersion = version,
            places = json.optJSONArray("places")?.length() ?: 0,
            mosques = json.optJSONArray("mosques")?.length() ?: 0,
            schedules = json.optJSONArray("schedules")?.length() ?: 0,
            entries = json.optJSONArray("entries")?.length() ?: 0,
            overrides = json.optJSONArray("overrides")?.length() ?: 0,
            reminderPreferences = json.optJSONArray("reminderPreferences")?.length() ?: 0,
            history = json.optJSONArray("history")?.length() ?: 0,
            qada = json.optJSONArray("qada")?.length() ?: 0,
            quranSessions = json.optJSONArray("quranSessions")?.length() ?: 0,
            quranGoals = json.optJSONArray("quranGoals")?.length() ?: 0,
            hasSettings = json.optJSONObject("settings") != null,
            errors = errors
        )
    }

    suspend fun import(raw: String, replaceExisting: Boolean): Result<BackupPreview> {
        val preview = preview(raw)
        if (!preview.valid) {
            return Result.failure(IllegalArgumentException(preview.errors.firstOrNull() ?: "Invalid backup."))
        }
        val json = try {
            JSONObject(raw)
        } catch (error: org.json.JSONException) {
            return Result.failure(IllegalArgumentException("The selected file is not valid JSON."))
        }

        var safetyBackupPath: String? = null
        if (replaceExisting) {
            safetyBackupPath = writeSafetyBackup(exportJson())
        }

        try {
            database.withTransaction {
                if (replaceExisting) wipeAll()
                importPlaces(json.optJSONArray("places"))
                importMosques(json.optJSONArray("mosques"))
                importSchedules(json.optJSONArray("schedules"))
                importEntries(json.optJSONArray("entries"))
                importOverrides(json.optJSONArray("overrides"))
                importReminderPreferences(json.optJSONArray("reminderPreferences"))
                importHistory(json.optJSONArray("history"))
                importQada(json.optJSONArray("qada"))
                importQuranSessions(json.optJSONArray("quranSessions"))
                importQuranGoals(json.optJSONArray("quranGoals"))
            }
        } catch (error: Exception) {
            return Result.failure(
                IllegalArgumentException(
                    "The import was rolled back. Existing data is unchanged." +
                        (safetyBackupPath?.let { " Safety backup: $it" } ?: "")
                )
            )
        }

        if (replaceExisting) {
            json.optJSONObject("settings")?.let { settingsRepository.applyEncoded(it.toString()) }
        }
        return Result.success(preview)
    }

    /** Real snapshot of current data, written before a Replace import touches live tables. */
    private fun writeSafetyBackup(currentExport: String): String? = try {
        val file = File(context.cacheDir, "namaz-times-safety-${System.currentTimeMillis()}.json")
        file.writeText(currentExport)
        file.absolutePath
    } catch (error: Exception) {
        null
    }

    private suspend fun wipeAll() {
        database.prayerDao().deleteAllAlarms()
        database.prayerDao().deleteAllOccurrences()
        database.prayerDao().deleteAllHistory()
        database.prayerDao().deleteAllQada()
        database.quranDao().deleteAllSessions()
        database.quranDao().deleteAllGoals()
        database.mosqueDao().deleteAllReminderPreferences()
        database.mosqueDao().deleteAllOverrides()
        database.mosqueDao().deleteAllEntries()
        database.mosqueDao().deleteAllSchedules()
        database.mosqueDao().deleteAllProfiles()
        database.placeDao().deleteAll()
    }

    private fun importPlaces(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            database.placeDao().upsert(
                SavedPlaceEntity(
                    id = id,
                    displayName = row.optString("displayName", "Imported place"),
                    latitude = row.optDoubleOrNull("latitude"),
                    longitude = row.optDoubleOrNull("longitude"),
                    geofenceRadiusMeters = row.optInt("geofenceRadiusMeters", 250).coerceAtLeast(100),
                    timezoneId = row.optString("timezoneId", java.time.ZoneId.systemDefault().id),
                    mosqueProfileId = row.optStringOrNull("mosqueProfileId"),
                    priorityRank = row.optInt("priorityRank", 100),
                    isEnabled = row.optBoolean("isEnabled", true),
                    isPriorityLocation = row.optBoolean("isPriorityLocation", false),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun importMosques(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            database.mosqueDao().upsertProfile(
                MosqueProfileEntity(
                    id = id,
                    name = row.optString("name", "Imported mosque"),
                    addressLabel = row.optString("addressLabel", ""),
                    latitude = row.optDoubleOrNull("latitude"),
                    longitude = row.optDoubleOrNull("longitude"),
                    timezoneId = row.optString("timezoneId", java.time.ZoneId.systemDefault().id),
                    isEnabled = row.optBoolean("isEnabled", true),
                    isArchived = row.optBoolean("isArchived", false),
                    userPriority = row.optInt("userPriority", 100),
                    notes = row.optString("notes", ""),
                    scheduleRevision = row.optLong("scheduleRevision", 0L),
                    defaultReminderProfileId = row.optStringOrNull("defaultReminderProfileId"),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun importSchedules(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            val mosqueId = row.optString("mosqueId").takeIf { it.isNotBlank() } ?: continue
            database.mosqueDao().upsertSchedule(
                MosqueScheduleEntity(
                    id = id,
                    mosqueId = mosqueId,
                    kind = row.optString("kind", "FIXED"),
                    label = row.optString("label", ""),
                    weekday = row.optIntOrNull("weekday"),
                    effectiveFrom = row.optStringOrNull("effectiveFrom"),
                    effectiveUntil = row.optStringOrNull("effectiveUntil"),
                    revision = row.optLong("revision", 1L),
                    isActive = row.optBoolean("isActive", true),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun importEntries(array: JSONArray?) {
        val entries = mutableListOf<ScheduleEntryEntity>()
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val scheduleId = row.optString("scheduleId").takeIf { it.isNotBlank() } ?: continue
            val prayer = row.optString("prayer").takeIf { it.isNotBlank() } ?: continue
            entries += ScheduleEntryEntity(
                scheduleId = scheduleId,
                prayer = prayer,
                adhan = row.optStringOrNull("adhan"),
                iqamah = row.optStringOrNull("iqamah"),
                adhanReminder = row.optBoolean("adhanReminder", true),
                iqamahReminder = row.optBoolean("iqamahReminder", true),
                note = row.optString("note", ""),
                khutbah = row.optStringOrNull("khutbah"),
                jumuah = row.optStringOrNull("jumuah"),
                secondJumuah = row.optStringOrNull("secondJumuah"),
                fridayNote = row.optString("fridayNote", "")
            )
        }
        if (entries.isNotEmpty()) database.mosqueDao().upsertEntries(entries)
    }

    private suspend fun importOverrides(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val mosqueId = row.optString("mosqueId").takeIf { it.isNotBlank() } ?: continue
            val date = row.optString("date").takeIf { it.isNotBlank() } ?: continue
            val prayer = row.optString("prayer").takeIf { it.isNotBlank() } ?: continue
            database.mosqueDao().upsertOverride(
                ManualOverrideEntity(
                    mosqueId = mosqueId,
                    date = date,
                    prayer = prayer,
                    adhan = row.optStringOrNull("adhan"),
                    iqamah = row.optStringOrNull("iqamah")
                )
            )
        }
    }

    private suspend fun importReminderPreferences(array: JSONArray?) {
        val preferences = mutableListOf<ReminderPreferenceEntity>()
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val mosqueId = row.optString("mosqueId").takeIf { it.isNotBlank() } ?: continue
            val prayer = row.optString("prayer").takeIf { it.isNotBlank() } ?: continue
            preferences += ReminderPreferenceEntity(
                mosqueId = mosqueId,
                prayer = prayer,
                adhanEnabled = row.optBoolean("adhanEnabled", true),
                iqamahEnabled = row.optBoolean("iqamahEnabled", true),
                preAdhanMinutes = row.optIntOrNull("preAdhanMinutes"),
                preIqamahMinutes = row.optIntOrNull("preIqamahMinutes"),
                soundMode = row.optString("soundMode", "SYSTEM"),
                customSoundUri = row.optStringOrNull("customSoundUri")
            )
        }
        if (preferences.isNotEmpty()) database.mosqueDao().upsertReminderPreferences(preferences)
    }

    private suspend fun importHistory(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val prayer = row.optString("prayer").takeIf { it.isNotBlank() } ?: continue
            val trackingDate = row.optString("trackingDate").takeIf { it.isNotBlank() } ?: continue
            val existing = database.prayerDao().findHistory(prayer, trackingDate)
            database.prayerDao().upsertHistory(
                PrayerHistoryEntity(
                    id = existing?.id ?: row.optString("id", UUID.randomUUID().toString()),
                    prayer = prayer,
                    trackingDate = trackingDate,
                    scheduledAdhanMillis = row.optLongOrNull("scheduledAdhanMillis"),
                    scheduledIqamahMillis = row.optLongOrNull("scheduledIqamahMillis"),
                    completedAtMillis = row.optLong("completedAtMillis", System.currentTimeMillis()),
                    mosqueId = row.optStringOrNull("mosqueId"),
                    placeId = row.optStringOrNull("placeId"),
                    scheduleRevision = row.optLongOrNull("scheduleRevision"),
                    context = row.optString("context", ""),
                    tags = row.optString("tags", ""),
                    note = row.optString("note", ""),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun importQada(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val prayer = row.optString("prayer").takeIf { it.isNotBlank() } ?: continue
            val date = row.optString("performedDate").takeIf { it.isNotBlank() } ?: continue
            database.prayerDao().upsertQada(
                QadaRecordEntity(
                    id = row.optString("id", UUID.randomUUID().toString()),
                    prayer = prayer,
                    performedDate = date,
                    note = row.optString("note", ""),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private suspend fun importQuranSessions(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val dateKey = row.optString("dateKey").takeIf { it.isNotBlank() } ?: continue
            val rukus = row.optInt("rukus", 0)
            if (rukus == 0) continue
            database.quranDao().upsertSession(
                QuranSessionEntity(
                    id = row.optString("id", UUID.randomUUID().toString()),
                    dateKey = dateKey,
                    occurredAtMillis = row.optLong("occurredAtMillis", System.currentTimeMillis()),
                    rukus = rukus,
                    surah = row.optStringOrNull("surah"),
                    rukuNumber = row.optIntOrNull("rukuNumber"),
                    note = row.optString("note", ""),
                    isCorrection = row.optBoolean("isCorrection", false),
                    correctionReason = row.optStringOrNull("correctionReason"),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private suspend fun importQuranGoals(array: JSONArray?) {
        for (index in 0 until (array?.length() ?: 0)) {
            val row = array?.optJSONObject(index) ?: continue
            val effectiveFrom = row.optString("effectiveFrom").takeIf { it.isNotBlank() } ?: continue
            database.quranDao().upsertGoal(
                QuranGoalEntity(
                    effectiveFrom = effectiveFrom,
                    dailyGoal = row.optInt("dailyGoal", 1).coerceIn(0, 50)
                )
            )
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}
