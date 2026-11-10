package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved mosque or geographic prayer zone.
 * Supports both custom fixed prayer schedules (entered directly by user)
 * and astronomical Ahl al-Hadith calculated times with mosque Iqamah offsets.
 */
@Entity(tableName = "mosque_locations")
data class MosqueLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 4000.0,
    val isCustomTimes: Boolean = true, // User enters specific mosque schedule
    // Custom Azan and Iqamah times (HH:mm format)
    val fajrAzan: String = "05:15",
    val fajrIqamah: String = "05:40",
    val sunriseTime: String = "06:35",
    val dhuhrAzan: String = "12:20",
    val dhuhrIqamah: String = "12:45",
    val asrAzan: String = "15:45", // Ahl al-Hadith 1x shadow
    val asrIqamah: String = "16:05",
    val maghribAzan: String = "17:45",
    val maghribIqamah: String = "17:55",
    val ishaAzan: String = "19:00",
    val ishaIqamah: String = "19:20",
    val jummahIqamah: String = "13:15",
    // Iqamah offsets in minutes (if using calculated times)
    val fajrIqamahOffsetMin: Int = 25,
    val dhuhrIqamahOffsetMin: Int = 20,
    val asrIqamahOffsetMin: Int = 15,
    val maghribIqamahOffsetMin: Int = 10,
    val ishaIqamahOffsetMin: Int = 20,
    val isActive: Boolean = false,
    val isPreset: Boolean = false,
    val note: String = ""
)
