package com.example.prayer

import com.example.domain.model.EffectiveTimetable
import com.example.domain.model.ManualOverride
import com.example.domain.model.MosqueProfile
import com.example.domain.model.MosqueSchedule
import com.example.domain.model.PrayerName
import com.example.domain.model.ScheduleEntry
import com.example.domain.model.ScheduleKind
import com.example.domain.model.TimetableSlot
import com.example.domain.model.WallClock
import java.time.LocalDate

/**
 * Deterministic timetable resolution.
 *
 * Precedence for a date: one-date override > active date-range/seasonal > weekday > fixed daily.
 * Each prayer's Adhan and Iqamah are resolved independently so a higher layer that omits one
 * field does not silently invent a value; it falls to the next layer for that field only.
 * A missing field everywhere stays missing (never fabricated). Manual per-date prayer overrides
 * are applied last and win above the timetable.
 */
class MosqueScheduleResolver {

    fun resolve(
        date: LocalDate,
        mosque: MosqueProfile,
        schedules: List<MosqueSchedule>,
        entries: List<ScheduleEntry>,
        overrides: List<ManualOverride> = emptyList()
    ): EffectiveTimetable {
        val active = schedules.filter { it.mosqueId == mosque.id && it.isActive }
        val layers = buildLayers(date, active)

        val bySchedule = entries.groupBy { it.scheduleId }
        val dateOverrides = overrides.filter { it.mosqueId == mosque.id && it.date == date }
            .associateBy { it.prayer }

        val slots = PrayerName.obligatories.associateWith { prayer ->
            var adhan = WallClock.parse(dateOverrides[prayer]?.adhan)
            var iqamah = WallClock.parse(dateOverrides[prayer]?.iqamah)
            var adhanKind: ScheduleKind? = if (adhan != null) ScheduleKind.DATE_OVERRIDE else null
            var iqamahKind: ScheduleKind? = if (iqamah != null) ScheduleKind.DATE_OVERRIDE else null
            var note = ""
            var adhanReminder = true
            var iqamahReminder = true

            for (layer in layers) {
                val entry = bySchedule[layer.id]?.firstOrNull { it.prayer == prayer } ?: continue
                if (adhan == null) {
                    val parsed = WallClock.parse(entry.adhan)
                    if (parsed != null) {
                        adhan = parsed
                        adhanKind = layer.kind
                        adhanReminder = entry.adhanReminder
                    }
                }
                if (iqamah == null) {
                    val parsed = WallClock.parse(entry.iqamah)
                    if (parsed != null) {
                        iqamah = parsed
                        iqamahKind = layer.kind
                        iqamahReminder = entry.iqamahReminder
                    }
                }
                if (note.isEmpty()) note = entry.note
            }

            TimetableSlot(
                adhan = adhan,
                adhanSourceKind = adhanKind,
                iqamah = iqamah,
                iqamahSourceKind = iqamahKind,
                note = note,
                adhanReminder = adhanReminder,
                iqamahReminder = iqamahReminder
            )
        }

        val fridayEntry = layers.asSequence()
            .mapNotNull { layer -> bySchedule[layer.id]?.firstOrNull { it.prayer == PrayerName.DHUHR } }
            .firstOrNull { it.khutbah != null || it.jumuah != null || it.secondJumuah != null || it.fridayNote.isNotEmpty() }

        val primary = layers.firstOrNull()

        return EffectiveTimetable(
            mosqueId = mosque.id,
            revision = mosque.scheduleRevision,
            primaryKind = primary?.kind,
            primaryLabel = primary?.label?.takeIf { it.isNotBlank() } ?: primary?.kind?.title.orEmpty(),
            slots = slots,
            khutbah = fridayEntry?.khutbah,
            jumuah = fridayEntry?.jumuah,
            secondJumuah = fridayEntry?.secondJumuah,
            fridayNote = fridayEntry?.fridayNote.orEmpty()
        )
    }

    private fun buildLayers(date: LocalDate, active: List<MosqueSchedule>): List<MosqueSchedule> {
        val oneDate = active.filter {
            it.kind == ScheduleKind.DATE_OVERRIDE && it.effectiveFrom == date
        }
        val seasonal = active.filter {
            it.kind == ScheduleKind.DATE_RANGE &&
                it.effectiveFrom != null &&
                it.effectiveUntil != null &&
                !date.isBefore(it.effectiveFrom) &&
                !date.isAfter(it.effectiveUntil)
        }
        val weekly = active.filter {
            it.kind == ScheduleKind.WEEKLY && it.weekday == date.dayOfWeek.value
        }
        val fixed = active.filter { it.kind == ScheduleKind.FIXED }

        // Within one layer, prefer the newest revision; never blend two conflicting records.
        fun newest(list: List<MosqueSchedule>) = list.sortedWith(
            compareByDescending<MosqueSchedule> { it.revision }.thenBy { it.id }
        )

        return newest(oneDate) + newest(seasonal) + newest(weekly) + newest(fixed)
    }

    /** Detects same-precedence conflicts before saving a new timetable revision. */
    fun validateNoConflict(
        candidate: MosqueSchedule,
        schedules: List<MosqueSchedule>
    ): String? {
        val others = schedules.filter {
            it.mosqueId == candidate.mosqueId && it.id != candidate.id && it.isActive &&
                it.kind == candidate.kind
        }
        return when (candidate.kind) {
            ScheduleKind.FIXED ->
                if (others.isNotEmpty()) {
                    "A fixed daily timetable already exists for this mosque. Edit the existing timetable instead of adding a second one."
                } else null

            ScheduleKind.WEEKLY -> {
                if (candidate.weekday == null) return "Choose a weekday for the weekly timetable."
                val clash = others.any { it.weekday == candidate.weekday }
                if (clash) "A weekly timetable already exists for that weekday. Edit it or choose another day." else null
            }

            ScheduleKind.DATE_RANGE -> {
                val from = candidate.effectiveFrom
                val until = candidate.effectiveUntil
                if (from == null || until == null) return "Choose both a start and end date for a seasonal timetable."
                if (until.isBefore(from)) return "The seasonal end date must be on or after the start date."
                val clash = others.any { other ->
                    val otherFrom = other.effectiveFrom ?: return@any false
                    val otherUntil = other.effectiveUntil ?: return@any false
                    !until.isBefore(otherFrom) && !from.isAfter(otherUntil)
                }
                if (clash) "This date range overlaps another seasonal timetable for this mosque. Adjust the dates so only one applies." else null
            }

            ScheduleKind.DATE_OVERRIDE -> {
                val date = candidate.effectiveFrom ?: return "Choose the date for the one-date override."
                val clash = others.any { it.effectiveFrom == date }
                if (clash) "A one-date override already exists for $date. Edit that entry instead." else null
            }
        }
    }
}

data class PrayerEntryValidation(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val ok: Boolean get() = errors.isEmpty()
}

fun validatePrayerEntries(
    entries: List<ScheduleEntry>,
    confirmIqamahBeforeAdhan: Boolean
): PrayerEntryValidation {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    entries.forEach { entry ->
        if (!entry.adhan.isNullOrBlank() && WallClock.parse(entry.adhan) == null) {
            errors += "${entry.prayer.title} Adhan must be a valid 24-hour time such as 05:30."
        }
        if (!entry.iqamah.isNullOrBlank() && WallClock.parse(entry.iqamah) == null) {
            errors += "${entry.prayer.title} Iqamah must be a valid 24-hour time such as 05:40."
        }
        val adhan = WallClock.parse(entry.adhan)
        val iqamah = WallClock.parse(entry.iqamah)
        if (adhan != null && iqamah != null && iqamah < adhan) {
            warnings += "${entry.prayer.title}: Iqamah is earlier than Adhan on the same date."
        }
        if (!entry.khutbah.isNullOrBlank() && WallClock.parse(entry.khutbah) == null) {
            errors += "Friday Khutbah must be a valid 24-hour time."
        }
        if (!entry.jumuah.isNullOrBlank() && WallClock.parse(entry.jumuah) == null) {
            errors += "Jumuah time must be a valid 24-hour time."
        }
        if (!entry.secondJumuah.isNullOrBlank() && WallClock.parse(entry.secondJumuah) == null) {
            errors += "Second Jumuah must be a valid 24-hour time."
        }
    }

    if (warnings.isNotEmpty() && !confirmIqamahBeforeAdhan) {
        errors += "Confirm the Iqamah-before-Adhan exception before saving this timetable."
    }
    return PrayerEntryValidation(errors, warnings)
}
