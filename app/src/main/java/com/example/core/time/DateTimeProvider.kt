package com.example.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of "now" for the app so countdowns, date boundaries and Quran resets
 * are computed from one consistent clock and can be controlled in tests.
 */
interface DateTimeProvider {
    fun now(): Instant
    fun deviceZone(): ZoneId
    fun today(): LocalDate = now().atZone(deviceZone()).toLocalDate()
}

object SystemDateTimeProvider : DateTimeProvider {
    override fun now(): Instant = Instant.now()
    override fun deviceZone(): ZoneId = ZoneId.systemDefault()
}

/** Formats an instant as a 12-hour wall-clock label. */
fun formatClock(instant: Instant, zone: ZoneId): String {
    val time = instant.atZone(zone).toLocalTime()
    return formatClock(time)
}

fun formatClock(hour: Int, minute: Int): String {
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour12, minute, suffix)
}

fun formatClock(time: java.time.LocalTime): String = formatClock(time.hour, time.minute)

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
