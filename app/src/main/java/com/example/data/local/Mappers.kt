package com.example.data.local

import com.example.data.local.entity.ManualOverrideEntity
import com.example.data.local.entity.MosqueProfileEntity
import com.example.data.local.entity.MosqueScheduleEntity
import com.example.data.local.entity.PrayerHistoryEntity
import com.example.data.local.entity.PrayerOccurrenceEntity
import com.example.data.local.entity.QadaRecordEntity
import com.example.data.local.entity.QuranGoalEntity
import com.example.data.local.entity.QuranSessionEntity
import com.example.data.local.entity.ReminderPreferenceEntity
import com.example.data.local.entity.SavedPlaceEntity
import com.example.data.local.entity.ScheduleEntryEntity
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerEventType
import com.example.domain.model.PrayerName
import com.example.domain.model.PrayerOccurrence
import com.example.domain.model.ReminderPreference
import com.example.domain.model.ReminderSoundMode
import com.example.domain.model.SavedPlace
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.TimeSource
import java.time.Instant
import java.time.LocalDate

object Mappers {

    fun SavedPlaceEntity.toDomain(): SavedPlace = SavedPlace(
        id = id,
        displayName = displayName,
        latitude = latitude,
        longitude = longitude,
        geofenceRadiusMeters = geofenceRadiusMeters,
        timezoneId = timezoneId,
        mosqueProfileId = mosqueProfileId,
        priorityRank = priorityRank,
        isEnabled = isEnabled,
        isPriorityLocation = isPriorityLocation,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun SavedPlace.toEntity(): SavedPlaceEntity = SavedPlaceEntity(
        id = id,
        displayName = displayName,
        latitude = latitude,
        longitude = longitude,
        geofenceRadiusMeters = geofenceRadiusMeters,
        timezoneId = timezoneId,
        mosqueProfileId = mosqueProfileId,
        priorityRank = priorityRank,
        isEnabled = isEnabled,
        isPriorityLocation = isPriorityLocation,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MosqueProfileEntity.toDomain(): MosqueProfile = MosqueProfile(
        id = id,
        name = name,
        addressLabel = addressLabel,
        latitude = latitude,
        longitude = longitude,
        timezoneId = timezoneId,
        isEnabled = isEnabled,
        isArchived = isArchived,
        userPriority = userPriority,
        notes = notes,
        scheduleRevision = scheduleRevision,
        defaultReminderProfileId = defaultReminderProfileId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MosqueProfile.toEntity(): MosqueProfileEntity = MosqueProfileEntity(
        id = id,
        name = name,
        addressLabel = addressLabel,
        latitude = latitude,
        longitude = longitude,
        timezoneId = timezoneId,
        isEnabled = isEnabled,
        isArchived = isArchived,
        userPriority = userPriority,
        notes = notes,
        scheduleRevision = scheduleRevision,
        defaultReminderProfileId = defaultReminderProfileId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MosqueScheduleEntity.toDomain(): MosqueSchedule = MosqueSchedule(
        id = id,
        mosqueId = mosqueId,
        kind = ScheduleKind.entries.firstOrNull { it.name == kind } ?: ScheduleKind.FIXED,
        label = label,
        weekday = weekday,
        effectiveFrom = effectiveFrom?.let { LocalDate.parse(it) },
        effectiveUntil = effectiveUntil?.let { LocalDate.parse(it) },
        revision = revision,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MosqueSchedule.toEntity(): MosqueScheduleEntity = MosqueScheduleEntity(
        id = id,
        mosqueId = mosqueId,
        kind = kind.name,
        label = label,
        weekday = weekday,
        effectiveFrom = effectiveFrom?.toString(),
        effectiveUntil = effectiveUntil?.toString(),
        revision = revision,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun ScheduleEntryEntity.toDomain(): ScheduleEntry = ScheduleEntry(
        scheduleId = scheduleId,
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        adhan = adhan,
        iqamah = iqamah,
        adhanReminder = adhanReminder,
        iqamahReminder = iqamahReminder,
        note = note,
        khutbah = khutbah,
        jumuah = jumuah,
        secondJumuah = secondJumuah,
        fridayNote = fridayNote
    )

    fun ScheduleEntry.toEntity(): ScheduleEntryEntity = ScheduleEntryEntity(
        scheduleId = scheduleId,
        prayer = prayer.name,
        adhan = adhan,
        iqamah = iqamah,
        adhanReminder = adhanReminder,
        iqamahReminder = iqamahReminder,
        note = note,
        khutbah = khutbah,
        jumuah = jumuah,
        secondJumuah = secondJumuah,
        fridayNote = fridayNote
    )

    fun ManualOverrideEntity.toDomain(): ManualOverride = ManualOverride(
        mosqueId = mosqueId,
        date = LocalDate.parse(date),
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        adhan = adhan,
        iqamah = iqamah
    )

    fun ReminderPreferenceEntity.toDomain(): ReminderPreference = ReminderPreference(
        mosqueId = mosqueId,
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        adhanEnabled = adhanEnabled,
        iqamahEnabled = iqamahEnabled,
        preAdhanMinutes = preAdhanMinutes,
        preIqamahMinutes = preIqamahMinutes,
        soundMode = ReminderSoundMode.entries.firstOrNull { it.name == soundMode } ?: ReminderSoundMode.SYSTEM,
        customSoundUri = customSoundUri
    )

    fun ReminderPreference.toEntity(): ReminderPreferenceEntity = ReminderPreferenceEntity(
        mosqueId = mosqueId,
        prayer = prayer.name,
        adhanEnabled = adhanEnabled,
        iqamahEnabled = iqamahEnabled,
        preAdhanMinutes = preAdhanMinutes,
        preIqamahMinutes = preIqamahMinutes,
        soundMode = soundMode.name,
        customSoundUri = customSoundUri
    )

    fun PrayerOccurrenceEntity.toDomain(): PrayerOccurrence = PrayerOccurrence(
        id = id,
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        eventType = PrayerEventType.entries.firstOrNull { it.name == eventType } ?: PrayerEventType.ADHAN,
        instant = Instant.ofEpochMilli(instantMillis),
        localDate = LocalDate.parse(localDate),
        mosqueId = mosqueId,
        placeId = placeId,
        source = TimeSource.entries.firstOrNull { it.name == source } ?: TimeSource.CALCULATED,
        scheduleRevision = scheduleRevision,
        isEnabled = isEnabled,
        offsetMinutes = offsetMinutes
    )

    fun PrayerOccurrence.toEntity(deliveredAtMillis: Long? = null): PrayerOccurrenceEntity = PrayerOccurrenceEntity(
        id = id,
        prayer = prayer.name,
        eventType = eventType.name,
        instantMillis = instant.toEpochMilli(),
        localDate = localDate.toString(),
        mosqueId = mosqueId,
        placeId = placeId,
        source = source.name,
        scheduleRevision = scheduleRevision,
        isEnabled = isEnabled,
        offsetMinutes = offsetMinutes,
        deliveredAtMillis = deliveredAtMillis
    )

    fun PrayerHistoryEntity.toDomain(): com.example.data.local.PrayerHistoryRecord = com.example.data.local.PrayerHistoryRecord(
        id = id,
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        trackingDate = LocalDate.parse(trackingDate),
        scheduledAdhan = scheduledAdhanMillis?.let { Instant.ofEpochMilli(it) },
        scheduledIqamah = scheduledIqamahMillis?.let { Instant.ofEpochMilli(it) },
        completedAt = Instant.ofEpochMilli(completedAtMillis),
        mosqueId = mosqueId,
        placeId = placeId,
        scheduleRevision = scheduleRevision,
        context = context,
        tags = tags,
        note = note
    )

    fun QadaRecordEntity.toDomain(): QadaRecord = QadaRecord(
        id = id,
        prayer = PrayerName.fromKey(prayer) ?: PrayerName.FAJR,
        performedDate = LocalDate.parse(performedDate),
        note = note,
        createdAt = createdAt
    )

    fun QuranSessionEntity.toDomain(): QuranSession = QuranSession(
        id = id,
        dateKey = LocalDate.parse(dateKey),
        occurredAt = Instant.ofEpochMilli(occurredAtMillis),
        rukus = rukus,
        surah = surah,
        rukuNumber = rukuNumber,
        note = note,
        isCorrection = isCorrection,
        correctionReason = correctionReason
    )

    fun QuranGoalEntity.toDomain(): QuranGoal = QuranGoal(
        effectiveFrom = LocalDate.parse(effectiveFrom),
        dailyGoal = dailyGoal
    )
}

data class PrayerHistoryRecord(
    val id: String,
    val prayer: PrayerName,
    val trackingDate: LocalDate,
    val scheduledAdhan: Instant?,
    val scheduledIqamah: Instant?,
    val completedAt: Instant,
    val mosqueId: String?,
    val placeId: String?,
    val scheduleRevision: Long?,
    val context: String,
    val tags: String,
    val note: String
)

data class QadaRecord(
    val id: String,
    val prayer: PrayerName,
    val performedDate: LocalDate,
    val note: String,
    val createdAt: Long
)

data class QuranSession(
    val id: String,
    val dateKey: LocalDate,
    val occurredAt: Instant,
    val rukus: Int,
    val surah: String?,
    val rukuNumber: Int?,
    val note: String,
    val isCorrection: Boolean,
    val correctionReason: String?
)

data class QuranGoal(
    val effectiveFrom: LocalDate,
    val dailyGoal: Int
)
