package com.example.data.repository

import com.example.data.local.Mappers.toDomain
import com.example.data.local.Mappers.toEntity
import com.example.data.local.NamazDatabase
import com.example.data.local.entity.ManualOverrideEntity
import com.example.data.local.entity.ReminderPreferenceEntity
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.ReminderPreference
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.WallClock
import com.example.prayer.MosqueScheduleResolver
import com.example.prayer.validatePrayerEntries
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class MosqueSnapshot(
    val places: List<SavedPlace>,
    val mosques: List<MosqueProfile>,
    val schedules: List<MosqueSchedule>,
    val entries: List<ScheduleEntry>,
    val overrides: List<ManualOverride>,
    val reminderPreferences: List<ReminderPreference>
) {
    fun mosque(id: String?): MosqueProfile? = mosques.firstOrNull { it.id == id }
    fun place(id: String?): SavedPlace? = places.firstOrNull { it.id == id }
    fun entriesFor(scheduleId: String): List<ScheduleEntry> = entries.filter { it.scheduleId == scheduleId }
    fun schedulesFor(mosqueId: String): List<MosqueSchedule> = schedules.filter { it.mosqueId == mosqueId }
}

class MosqueRepository(private val database: NamazDatabase) {

    private val resolver = MosqueScheduleResolver()

    val places: Flow<List<SavedPlace>> = database.placeDao().observeAll()
        .map { rows -> rows.map { it.toDomain() } }

    val mosques: Flow<List<MosqueProfile>> = database.mosqueDao().observeProfiles()
        .map { rows -> rows.map { it.toDomain() } }

    suspend fun snapshot(): MosqueSnapshot {
        val mosqueDao = database.mosqueDao()
        return MosqueSnapshot(
            places = database.placeDao().getAll().map { it.toDomain() },
            mosques = mosqueDao.getProfiles().map { it.toDomain() },
            schedules = mosqueDao.getSchedules().map { it.toDomain() },
            entries = mosqueDao.getAllEntries().map { it.toDomain() },
            overrides = mosqueDao.getAllOverrides().map { it.toDomain() },
            reminderPreferences = mosqueDao.getReminderPreferences().map { it.toDomain() }
        )
    }

    suspend fun upsertMosque(mosque: MosqueProfile) {
        val now = System.currentTimeMillis()
        val existing = database.mosqueDao().getProfile(mosque.id)
        val entity = mosque.copy(
            createdAt = existing?.createdAt ?: if (mosque.createdAt == 0L) now else mosque.createdAt,
            updatedAt = now,
            scheduleRevision = existing?.scheduleRevision ?: mosque.scheduleRevision
        ).toEntity()
        database.mosqueDao().upsertProfile(entity)
    }

    suspend fun deleteMosque(mosqueId: String) = database.withTransaction {
        database.placeDao().clearMosqueLinks(mosqueId)
        database.mosqueDao().deleteProfile(mosqueId)
    }

    suspend fun upsertPlace(place: SavedPlace) {
        val now = System.currentTimeMillis()
        val existing = database.placeDao().getById(place.id)
        database.placeDao().upsert(
            place.copy(
                createdAt = existing?.createdAt ?: if (place.createdAt == 0L) now else place.createdAt,
                updatedAt = now
            ).toEntity()
        )
    }

    suspend fun deletePlace(placeId: String) = database.placeDao().deleteById(placeId)

    suspend fun deleteSchedule(schedule: MosqueSchedule) = database.withTransaction {
        database.mosqueDao().deleteSchedule(schedule.id)
        bumpRevision(schedule.mosqueId)
    }

    suspend fun saveSchedule(
        mosqueId: String,
        schedule: MosqueSchedule,
        entries: List<ScheduleEntry>,
        confirmIqamahBeforeAdhan: Boolean
    ): Result<Long> {
        val snapshot = snapshot()
        val mosque = snapshot.mosque(mosqueId)
            ?: return Result.failure(IllegalArgumentException("Save the mosque profile before adding a timetable."))
        val existingSchedules = snapshot.schedulesFor(mosqueId)

        val conflict = resolver.validateNoConflict(schedule.copy(mosqueId = mosqueId), existingSchedules)
        if (conflict != null) return Result.failure(IllegalArgumentException(conflict))

        val validation = validatePrayerEntries(entries, confirmIqamahBeforeAdhan)
        if (!validation.ok) {
            return Result.failure(IllegalArgumentException(validation.errors.first()))
        }

        var newRevision = mosque.scheduleRevision
        database.withTransaction {
            newRevision = mosque.scheduleRevision + 1
            val now = System.currentTimeMillis()
            val scheduleEntity = schedule.copy(
                mosqueId = mosqueId,
                revision = newRevision,
                createdAt = if (schedule.createdAt == 0L) now else schedule.createdAt,
                updatedAt = now
            ).toEntity()
            database.mosqueDao().upsertSchedule(scheduleEntity)
            database.mosqueDao().deleteEntries(schedule.id)
            if (entries.isNotEmpty()) {
                database.mosqueDao().upsertEntries(
                    entries.map { it.copy(scheduleId = schedule.id).toEntity() }
                )
            }
            database.mosqueDao().upsertProfile(
                mosque.copy(scheduleRevision = newRevision, updatedAt = now).toEntity()
            )
        }
        return Result.success(newRevision)
    }

    suspend fun saveManualOverride(override: ManualOverride) {
        if (override.adhan == null && override.iqamah == null) {
            database.mosqueDao().deleteOverride(
                override.mosqueId,
                override.date.toString(),
                override.prayer.name
            )
            bumpRevision(override.mosqueId)
            return
        }
        database.withTransaction {
            database.mosqueDao().upsertOverride(
                ManualOverrideEntity(
                    mosqueId = override.mosqueId,
                    date = override.date.toString(),
                    prayer = override.prayer.name,
                    adhan = override.adhan,
                    iqamah = override.iqamah
                )
            )
            bumpRevision(override.mosqueId)
        }
    }

    private suspend fun bumpRevision(mosqueId: String) {
        val mosque = database.mosqueDao().getProfile(mosqueId) ?: return
        database.mosqueDao().upsertProfile(
            mosque.copy(
                scheduleRevision = mosque.scheduleRevision + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveReminderPreferences(preferences: List<ReminderPreference>) {
        database.mosqueDao().upsertReminderPreferences(
            preferences.map {
                ReminderPreferenceEntity(
                    mosqueId = it.mosqueId,
                    prayer = it.prayer.name,
                    adhanEnabled = it.adhanEnabled,
                    iqamahEnabled = it.iqamahEnabled,
                    preAdhanMinutes = it.preAdhanMinutes,
                    preIqamahMinutes = it.preIqamahMinutes,
                    soundMode = it.soundMode.name,
                    customSoundUri = it.customSoundUri
                )
            }
        )
    }

    /** Export one mosque profile (no audio binaries, no history) for sharing or safekeeping. */
    suspend fun exportMosqueJson(mosqueId: String): String? {
        val snapshot = snapshot()
        val mosque = snapshot.mosque(mosqueId) ?: return null
        val schedules = snapshot.schedulesFor(mosqueId)
        val json = JSONObject().apply {
            put("schemaVersion", 1)
            put("type", "mosque-profile")
            put("mosque", JSONObject().apply {
                put("id", mosque.id)
                put("name", mosque.name)
                put("addressLabel", mosque.addressLabel)
                putNullableDouble("latitude", mosque.latitude)
                putNullableDouble("longitude", mosque.longitude)
                put("timezoneId", mosque.timezoneId)
                put("notes", mosque.notes)
                put("userPriority", mosque.userPriority)
            })
            put("schedules", JSONArray().apply {
                schedules.forEach { schedule ->
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("kind", schedule.kind.name)
                        put("label", schedule.label)
                        putNullableInt("weekday", schedule.weekday)
                        putNullableString("effectiveFrom", schedule.effectiveFrom?.toString())
                        putNullableString("effectiveUntil", schedule.effectiveUntil?.toString())
                        put("isActive", schedule.isActive)
                        put("entries", JSONArray().apply {
                            snapshot.entriesFor(schedule.id).forEach { entry ->
                                put(JSONObject().apply {
                                    put("prayer", entry.prayer.name)
                                    putNullableString("adhan", entry.adhan)
                                    putNullableString("iqamah", entry.iqamah)
                                    put("adhanReminder", entry.adhanReminder)
                                    put("iqamahReminder", entry.iqamahReminder)
                                    put("note", entry.note)
                                    putNullableString("khutbah", entry.khutbah)
                                    putNullableString("jumuah", entry.jumuah)
                                    putNullableString("secondJumuah", entry.secondJumuah)
                                    put("fridayNote", entry.fridayNote)
                                })
                            }
                        })
                    })
                }
            })
        }
        return json.toString(2)
    }

    /**
     * Import one previously exported mosque profile. The mosque receives a fresh UUID so an
     * imported profile can never silently overwrite an existing mosque with the same ID.
     */
    suspend fun importMosqueJson(raw: String): Result<String> {
        return try {
            val json = JSONObject(raw)
            if (json.optInt("schemaVersion", -1) != 1) {
                return Result.failure(IllegalArgumentException("Unsupported mosque file version."))
            }
            val mosqueJson = json.optJSONObject("mosque")
                ?: return Result.failure(IllegalArgumentException("The file has no mosque profile."))
            val newId = UUID.randomUUID().toString()
            val timezoneId = mosqueJson.optString("timezoneId", java.time.ZoneId.systemDefault().id)
            val mosque = MosqueProfile(
                id = newId,
                name = mosqueJson.optString("name", "Imported mosque"),
                addressLabel = mosqueJson.optString("addressLabel", ""),
                latitude = mosqueJson.optDoubleOrNull("latitude"),
                longitude = mosqueJson.optDoubleOrNull("longitude"),
                timezoneId = timezoneId,
                notes = mosqueJson.optString("notes", ""),
                userPriority = mosqueJson.optInt("userPriority", 100)
            )

            database.withTransaction {
                database.mosqueDao().upsertProfile(mosque.toEntity())
                val schedulesJson = json.optJSONArray("schedules") ?: JSONArray()
                for (index in 0 until schedulesJson.length()) {
                    val scheduleJson = schedulesJson.optJSONObject(index) ?: continue
                    val scheduleId = UUID.randomUUID().toString()
                    val kind = ScheduleKind.entries.firstOrNull { it.name == scheduleJson.optString("kind") }
                        ?: ScheduleKind.FIXED
                    val schedule = MosqueSchedule(
                        id = scheduleId,
                        mosqueId = newId,
                        kind = kind,
                        label = scheduleJson.optString("label", kind.title),
                        weekday = scheduleJson.optIntOrNull("weekday"),
                        effectiveFrom = scheduleJson.optString("effectiveFrom").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        effectiveUntil = scheduleJson.optString("effectiveUntil").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                        revision = 1,
                        isActive = scheduleJson.optBoolean("isActive", true)
                    )
                    database.mosqueDao().upsertSchedule(schedule.toEntity())
                    val entriesJson = scheduleJson.optJSONArray("entries") ?: JSONArray()
                    val entries = mutableListOf<com.example.data.local.entity.ScheduleEntryEntity>()
                    for (entryIndex in 0 until entriesJson.length()) {
                        val entryJson = entriesJson.optJSONObject(entryIndex) ?: continue
                        val prayer = com.example.domain.model.PrayerName.fromKey(entryJson.optString("prayer")) ?: continue
                        val adhan = entryJson.optString("adhan").takeIf { WallClock.isValid(it) }
                        val iqamah = entryJson.optString("iqamah").takeIf { WallClock.isValid(it) }
                        entries += ScheduleEntry(
                            scheduleId = scheduleId,
                            prayer = prayer,
                            adhan = adhan,
                            iqamah = iqamah,
                            adhanReminder = entryJson.optBoolean("adhanReminder", true),
                            iqamahReminder = entryJson.optBoolean("iqamahReminder", true),
                            note = entryJson.optString("note", ""),
                            khutbah = entryJson.optString("khutbah").takeIf { WallClock.isValid(it) },
                            jumuah = entryJson.optString("jumuah").takeIf { WallClock.isValid(it) },
                            secondJumuah = entryJson.optString("secondJumuah").takeIf { WallClock.isValid(it) },
                            fridayNote = entryJson.optString("fridayNote", "")
                        ).toEntity()
                    }
                    if (entries.isNotEmpty()) database.mosqueDao().upsertEntries(entries)
                }
            }
            Result.success(newId)
        } catch (error: org.json.JSONException) {
            Result.failure(IllegalArgumentException("The mosque file is not valid JSON."))
        } catch (error: java.time.format.DateTimeParseException) {
            Result.failure(IllegalArgumentException("The mosque file contains an invalid date."))
        }
    }

    suspend fun countPlaces(): Int = database.placeDao().getAll().size

    suspend fun deleteAll() = database.withTransaction {
        database.mosqueDao().deleteAllProfiles()
        database.placeDao().deleteAll()
    }
}

private fun JSONObject.putNullableString(key: String, value: String?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.putNullableInt(key: String, value: Int?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.putNullableDouble(key: String, value: Double?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

