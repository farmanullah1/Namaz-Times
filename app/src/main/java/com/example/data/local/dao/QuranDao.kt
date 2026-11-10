package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.QuranGoalEntity
import com.example.data.local.entity.QuranSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM quran_sessions ORDER BY occurredAtMillis ASC")
    fun observeSessions(): Flow<List<QuranSessionEntity>>

    @Query("SELECT * FROM quran_sessions ORDER BY occurredAtMillis ASC")
    suspend fun getSessions(): List<QuranSessionEntity>

    @Query("SELECT * FROM quran_sessions WHERE dateKey = :dateKey ORDER BY occurredAtMillis ASC")
    suspend fun getSessionsForDate(dateKey: String): List<QuranSessionEntity>

    @Query("SELECT * FROM quran_sessions WHERE occurredAtMillis = :occurredAtMillis AND rukus = :rukus AND dateKey = :dateKey LIMIT 1")
    suspend fun findDuplicate(dateKey: String, occurredAtMillis: Long, rukus: Int): QuranSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: QuranSessionEntity)

    @Query("DELETE FROM quran_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM quran_sessions ORDER BY occurredAtMillis DESC LIMIT 1")
    suspend fun latestSession(): QuranSessionEntity?

    @Query("DELETE FROM quran_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM quran_goal_history ORDER BY effectiveFrom ASC")
    suspend fun getGoals(): List<QuranGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: QuranGoalEntity)

    @Query("DELETE FROM quran_goal_history")
    suspend fun deleteAllGoals()
}
