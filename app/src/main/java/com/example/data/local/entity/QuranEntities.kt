package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reading entries are append-only. Corrections are recorded as correction sessions so the audit
 * trail stays intact and a corrected day is never double-counted.
 */
@Entity(tableName = "quran_sessions", indices = [Index("dateKey")])
data class QuranSessionEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val occurredAtMillis: Long,
    val rukus: Int,
    val surah: String?,
    val rukuNumber: Int?,
    val note: String,
    val isCorrection: Boolean,
    val correctionReason: String?,
    val createdAt: Long
)

@Entity(tableName = "quran_goal_history")
data class QuranGoalEntity(
    @PrimaryKey val effectiveFrom: String,
    val dailyGoal: Int
)
