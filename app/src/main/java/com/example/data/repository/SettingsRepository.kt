package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AsrShadowFactor
import com.example.domain.model.CalculationPreset
import com.example.domain.model.CalculationSettings
import com.example.domain.model.HighLatitudeRuleOption
import com.example.domain.model.HijriSettings
import com.example.domain.model.LastLocation
import com.example.domain.model.LastSelectionDecision
import com.example.domain.model.LocationMode
import com.example.domain.model.PerPrayerAdjustments
import com.example.domain.model.QuranResetRule
import com.example.domain.model.QuranSettings
import com.example.domain.model.ReminderSoundMode
import com.example.domain.model.WallClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException
import java.time.LocalTime

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "namaz_settings")

/**
 * All non-relational preferences, persisted locally in DataStore as one versioned JSON document so
 * updates stay atomic. Every field is restored on app restart and survives backup/restore.
 */
class SettingsRepository(private val context: Context) {

    private val key = stringPreferencesKey("app_settings_v1")

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> parse(preferences[key]) }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        context.settingsDataStore.edit { preferences ->
            val existing = parse(preferences[key])
            result = transform(existing)
            preferences[key] = toJson(result).toString()
        }
        return result
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[key] = toJson(settings).toString()
        }
    }

    suspend fun setOnboardingComplete() = update { it.copy(onboardingComplete = true) }

    suspend fun setThemeMode(mode: String) = update { it.copy(themeMode = mode) }

    suspend fun setCalculation(calculation: CalculationSettings) = update { it.copy(calculation = calculation) }

    suspend fun setHijri(adjustmentDays: Int, sunsetBoundary: Boolean) = update {
        it.copy(
            hijriAdjustmentDays = adjustmentDays.coerceIn(-2, 2),
            hijriSunsetBoundary = sunsetBoundary,
            hijriSource = HijriSettings.SOURCE_UMM_AL_QURA
        )
    }

    suspend fun setLocationMode(mode: LocationMode) = update {
        it.copy(locationMode = mode, temporaryMosqueId = null, temporaryExpiresAtMillis = null)
    }

    suspend fun setManualSelection(mosqueId: String?, placeId: String?) = update {
        it.copy(
            locationMode = LocationMode.MANUAL,
            manualMosqueId = mosqueId,
            manualPlaceId = placeId,
            temporaryMosqueId = null,
            temporaryExpiresAtMillis = null,
            lastSelectionReason = "Manual selection"
        )
    }

    suspend fun setTemporarySelection(mosqueId: String, placeId: String?, durationHours: Int) = update {
        val now = System.currentTimeMillis()
        it.copy(
            locationMode = LocationMode.TEMPORARY,
            temporaryPreviousMode = it.locationMode,
            temporaryMosqueId = mosqueId,
            temporaryExpiresAtMillis = now + durationHours * 60L * 60L * 1000L,
            manualPlaceId = it.manualPlaceId,
            lastSelectionReason = "Temporary override",
            lastAutoPlaceId = placeId
        )
    }

    suspend fun clearTemporarySelection() = update {
        it.copy(
            locationMode = it.temporaryPreviousMode,
            temporaryMosqueId = null,
            temporaryExpiresAtMillis = null
        )
    }

    suspend fun setAutomaticLocationEnabled(enabled: Boolean) = update {
        it.copy(automaticLocationEnabled = enabled, locationMode = if (enabled) LocationMode.AUTOMATIC else it.locationMode)
    }

    suspend fun saveAutomaticDecision(
        mosqueId: String?,
        placeId: String?,
        reason: String,
        decision: LastSelectionDecision?
    ) = update {
        it.copy(
            lastAutoMosqueId = mosqueId,
            lastAutoPlaceId = placeId,
            lastSelectionReason = reason,
            lastDecision = decision
        )
    }

    suspend fun saveLastLocation(location: LastLocation) = update { it.copy(lastLocation = location) }

    suspend fun setRemindersEnabled(enabled: Boolean) = update { it.copy(remindersEnabled = enabled) }

    suspend fun setReminderSound(mode: ReminderSoundMode, customUri: String?) = update {
        it.copy(reminderSoundMode = mode, customAdhanUri = customUri)
    }

    suspend fun setSpokenAnnouncements(enabled: Boolean) = update { it.copy(spokenAnnouncementEnabled = enabled) }

    suspend fun setFridayReminders(enabled: Boolean) = update { it.copy(fridayRemindersEnabled = enabled) }

    suspend fun setQuranSettings(settings: QuranSettings) = update { it.copy(quran = settings) }

    suspend fun setQadaEnabled(enabled: Boolean) = update { it.copy(qadaEnabled = enabled) }

    suspend fun setSilenceSettings(enabled: Boolean, minutes: Int) = update {
        it.copy(prayerSilenceEnabled = enabled, prayerSilenceMinutes = minutes.coerceIn(1, 60))
    }

    suspend fun setDndState(previousFilter: Int?, silencedAt: Long?, restoreAt: Long?) = update {
        it.copy(
            previousDndFilter = previousFilter,
            dndSilencedAtMillis = silencedAt,
            dndRestoreAtMillis = restoreAt
        )
    }

    suspend fun setSelectionThresholds(
        maximumNearestDistanceMeters: Double,
        accuracyMeters: Float,
        dwellSeconds: Long,
        marginMeters: Double,
        maxFixAgeSeconds: Long
    ) = update {
        it.copy(
            maximumNearestDistanceMeters = maximumNearestDistanceMeters.coerceIn(100.0, 200_000.0),
            accuracyThresholdMeters = accuracyMeters.coerceIn(10f, 5_000f),
            dwellSeconds = dwellSeconds.coerceIn(0L, 600L),
            boundaryMarginMeters = marginMeters.coerceIn(0.0, 1_000.0),
            maxFixAgeSeconds = maxFixAgeSeconds.coerceIn(15L, 900L)
        )
    }

    suspend fun setRebuildState(
        successfulAt: Long?,
        reason: String,
        succeeded: Boolean,
        needsRepair: Boolean
    ) = update {
        it.copy(
            lastSuccessfulRebuildMillis = successfulAt,
            lastRebuildReason = reason,
            lastRebuildSucceeded = succeeded,
            remindersNeedRepair = needsRepair
        )
    }

    // ------------------------------------------------------------------ serialization

    private fun toJson(settings: AppSettings): JSONObject = JSONObject().apply {
        put("version", 1)
        put("onboardingComplete", settings.onboardingComplete)
        put("themeMode", settings.themeMode)
        put("calculation", calculationToJson(settings.calculation))
        put("hijriAdjustmentDays", settings.hijriAdjustmentDays)
        put("hijriSunsetBoundary", settings.hijriSunsetBoundary)
        put("hijriSource", settings.hijriSource)
        put("locationMode", settings.locationMode.name)
        putNullable("manualMosqueId", settings.manualMosqueId)
        putNullable("manualPlaceId", settings.manualPlaceId)
        putNullable("temporaryMosqueId", settings.temporaryMosqueId)
        putNullable("temporaryExpiresAtMillis", settings.temporaryExpiresAtMillis)
        put("temporaryPreviousMode", settings.temporaryPreviousMode.name)
        put("automaticLocationEnabled", settings.automaticLocationEnabled)
        putNullable("lastAutoMosqueId", settings.lastAutoMosqueId)
        putNullable("lastAutoPlaceId", settings.lastAutoPlaceId)
        put("lastSelectionReason", settings.lastSelectionReason)
        put("maximumNearestDistanceMeters", settings.maximumNearestDistanceMeters)
        put("accuracyThresholdMeters", settings.accuracyThresholdMeters.toDouble())
        put("maxFixAgeSeconds", settings.maxFixAgeSeconds)
        put("dwellSeconds", settings.dwellSeconds)
        put("boundaryMarginMeters", settings.boundaryMarginMeters)
        settings.lastLocation?.let { location ->
            put(
                "lastLocation",
                JSONObject().apply {
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                    put("accuracyMeters", location.accuracyMeters.toDouble())
                    put("timestampMillis", location.timestampMillis)
                    put("source", location.source)
                }
            )
        }
        settings.lastDecision?.let { decision ->
            put(
                "lastDecision",
                JSONObject().apply {
                    put("timestampMillis", decision.timestampMillis)
                    put("accuracyMeters", decision.accuracyMeters.toDouble())
                    put("mode", decision.mode.name)
                    putNullable("selectedMosqueId", decision.selectedMosqueId)
                    putNullable("selectedPlaceId", decision.selectedPlaceId)
                    put("reason", decision.reason)
                    put("confidence", decision.confidence)
                }
            )
        }
        put("remindersEnabled", settings.remindersEnabled)
        put("reminderSoundMode", settings.reminderSoundMode.name)
        putNullable("customAdhanUri", settings.customAdhanUri)
        put("spokenAnnouncementEnabled", settings.spokenAnnouncementEnabled)
        put("fridayRemindersEnabled", settings.fridayRemindersEnabled)
        putNullable("lastSuccessfulRebuildMillis", settings.lastSuccessfulRebuildMillis)
        put("lastRebuildReason", settings.lastRebuildReason)
        put("lastRebuildSucceeded", settings.lastRebuildSucceeded)
        put("remindersNeedRepair", settings.remindersNeedRepair)
        put("prayerSilenceEnabled", settings.prayerSilenceEnabled)
        put("prayerSilenceMinutes", settings.prayerSilenceMinutes)
        putNullable("previousDndFilter", settings.previousDndFilter)
        putNullable("dndSilencedAtMillis", settings.dndSilencedAtMillis)
        putNullable("dndRestoreAtMillis", settings.dndRestoreAtMillis)
        put("quran", quranToJson(settings.quran))
        put("qadaEnabled", settings.qadaEnabled)
    }

    private fun parse(raw: String?): AppSettings {
        if (raw.isNullOrBlank()) return AppSettings()
        return try {
            val json = JSONObject(raw)
            val calcJson = json.optJSONObject("calculation")
            val quranJson = json.optJSONObject("quran")
            AppSettings(
                onboardingComplete = json.optBoolean("onboardingComplete", false),
                themeMode = json.optString("themeMode", "system"),
                calculation = parseCalculation(calcJson),
                hijriAdjustmentDays = json.optInt("hijriAdjustmentDays", 0).coerceIn(-2, 2),
                hijriSunsetBoundary = json.optBoolean("hijriSunsetBoundary", false),
                hijriSource = json.optString("hijriSource", HijriSettings.SOURCE_UMM_AL_QURA),
                locationMode = enumOr(json.optString("locationMode"), LocationMode.MANUAL),
                manualMosqueId = json.optStringOrNull("manualMosqueId"),
                manualPlaceId = json.optStringOrNull("manualPlaceId"),
                temporaryMosqueId = json.optStringOrNull("temporaryMosqueId"),
                temporaryExpiresAtMillis = json.optLongOrNull("temporaryExpiresAtMillis"),
                temporaryPreviousMode = enumOr(json.optString("temporaryPreviousMode"), LocationMode.MANUAL),
                automaticLocationEnabled = json.optBoolean("automaticLocationEnabled", false),
                lastAutoMosqueId = json.optStringOrNull("lastAutoMosqueId"),
                lastAutoPlaceId = json.optStringOrNull("lastAutoPlaceId"),
                lastSelectionReason = json.optString("lastSelectionReason", ""),
                maximumNearestDistanceMeters = json.optDouble("maximumNearestDistanceMeters", 20_000.0),
                accuracyThresholdMeters = json.optDouble("accuracyThresholdMeters", 100.0).toFloat(),
                maxFixAgeSeconds = json.optLong("maxFixAgeSeconds", 120L),
                dwellSeconds = json.optLong("dwellSeconds", 120L),
                boundaryMarginMeters = json.optDouble("boundaryMarginMeters", 50.0),
                lastLocation = json.optJSONObject("lastLocation")?.let { loc ->
                    LastLocation(
                        latitude = loc.optDouble("latitude"),
                        longitude = loc.optDouble("longitude"),
                        accuracyMeters = loc.optDouble("accuracyMeters").toFloat(),
                        timestampMillis = loc.optLong("timestampMillis"),
                        source = loc.optString("source", "unknown")
                    )
                },
                lastDecision = json.optJSONObject("lastDecision")?.let { d ->
                    LastSelectionDecision(
                        timestampMillis = d.optLong("timestampMillis"),
                        accuracyMeters = d.optDouble("accuracyMeters").toFloat(),
                        mode = enumOr(d.optString("mode"), LocationMode.MANUAL),
                        selectedMosqueId = d.optStringOrNull("selectedMosqueId"),
                        selectedPlaceId = d.optStringOrNull("selectedPlaceId"),
                        reason = d.optString("reason", ""),
                        confidence = d.optString("confidence", "")
                    )
                },
                remindersEnabled = json.optBoolean("remindersEnabled", false),
                reminderSoundMode = enumOr(json.optString("reminderSoundMode"), ReminderSoundMode.SYSTEM),
                customAdhanUri = json.optStringOrNull("customAdhanUri"),
                spokenAnnouncementEnabled = json.optBoolean("spokenAnnouncementEnabled", false),
                fridayRemindersEnabled = json.optBoolean("fridayRemindersEnabled", true),
                lastSuccessfulRebuildMillis = json.optLongOrNull("lastSuccessfulRebuildMillis"),
                lastRebuildReason = json.optString("lastRebuildReason", ""),
                lastRebuildSucceeded = json.optBoolean("lastRebuildSucceeded", true),
                remindersNeedRepair = json.optBoolean("remindersNeedRepair", false),
                prayerSilenceEnabled = json.optBoolean("prayerSilenceEnabled", false),
                prayerSilenceMinutes = json.optInt("prayerSilenceMinutes", 10).coerceIn(1, 60),
                previousDndFilter = json.optIntOrNull("previousDndFilter"),
                dndSilencedAtMillis = json.optLongOrNull("dndSilencedAtMillis"),
                dndRestoreAtMillis = json.optLongOrNull("dndRestoreAtMillis"),
                quran = parseQuran(quranJson),
                qadaEnabled = json.optBoolean("qadaEnabled", false)
            )
        } catch (error: org.json.JSONException) {
            // Keep the app usable with safe defaults rather than crashing on a damaged preference blob.
            AppSettings()
        }
    }

    private fun calculationToJson(calculation: CalculationSettings): JSONObject = JSONObject().apply {
        put("preset", calculation.preset.name)
        put("fajrAngle", calculation.fajrAngle)
        put("ishaAngle", calculation.ishaAngle)
        putNullable("ishaIntervalMinutes", calculation.ishaIntervalMinutes)
        put("asr", calculation.asr.name)
        put("highLatitude", calculation.highLatitude.name)
        put("adjustments", JSONObject().apply {
            put("fajr", calculation.adjustments.fajr)
            put("dhuhr", calculation.adjustments.dhuhr)
            put("asr", calculation.adjustments.asr)
            put("maghrib", calculation.adjustments.maghrib)
            put("isha", calculation.adjustments.isha)
        })
        put("roundingToNearestMinute", calculation.roundingToNearestMinute)
    }

    private fun parseCalculation(json: JSONObject?): CalculationSettings {
        val base = CalculationSettings()
        if (json == null) return base
        val adjustments = json.optJSONObject("adjustments")
        return CalculationSettings(
            preset = enumOr(json.optString("preset"), CalculationPreset.KARACHI),
            fajrAngle = json.optDouble("fajrAngle", base.fajrAngle),
            ishaAngle = json.optDouble("ishaAngle", base.ishaAngle),
            ishaIntervalMinutes = json.optIntOrNull("ishaIntervalMinutes"),
            asr = enumOr(json.optString("asr"), AsrShadowFactor.STANDARD),
            highLatitude = enumOr(json.optString("highLatitude"), HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT),
            adjustments = PerPrayerAdjustments(
                fajr = adjustments?.optInt("fajr", 0) ?: 0,
                dhuhr = adjustments?.optInt("dhuhr", 0) ?: 0,
                asr = adjustments?.optInt("asr", 0) ?: 0,
                maghrib = adjustments?.optInt("maghrib", 0) ?: 0,
                isha = adjustments?.optInt("isha", 0) ?: 0
            ),
            roundingToNearestMinute = json.optBoolean("roundingToNearestMinute", true)
        )
    }

    private fun quranToJson(quran: QuranSettings): JSONObject = JSONObject().apply {
        put("dailyGoal", quran.dailyGoal)
        put("surplusCreditEnabled", quran.surplusCreditEnabled)
        put("resetRule", quran.resetRule.name)
        put("customResetTime", WallClock.format(quran.customResetTime) ?: "00:00")
        put("streakCountsSurplus", quran.streakCountsSurplus)
        putNullable("startedOn", quran.startedOn?.toString())
        putNullable("editionTotalRukus", quran.editionTotalRukus)
        put("editionLabel", quran.editionLabel)
    }

    private fun parseQuran(json: JSONObject?): QuranSettings {
        val base = QuranSettings()
        if (json == null) return base
        return QuranSettings(
            dailyGoal = json.optInt("dailyGoal", 1).coerceIn(0, 50),
            surplusCreditEnabled = json.optBoolean("surplusCreditEnabled", false),
            resetRule = enumOr(json.optString("resetRule"), QuranResetRule.MIDNIGHT),
            customResetTime = WallClock.parse(json.optString("customResetTime")) ?: LocalTime.MIDNIGHT,
            streakCountsSurplus = json.optBoolean("streakCountsSurplus", true),
            startedOn = json.optStringOrNull("startedOn")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
            editionTotalRukus = json.optIntOrNull("editionTotalRukus"),
            editionLabel = json.optString("editionLabel", "")
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key)
    return value.ifBlank { null }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}
