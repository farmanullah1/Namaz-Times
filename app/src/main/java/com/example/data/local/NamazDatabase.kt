package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MosqueDao
import com.example.data.local.dao.PlaceDao
import com.example.data.local.dao.PrayerDao
import com.example.data.local.dao.QuranDao
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
import com.example.data.local.entity.ScheduledAlarmEntity
import com.example.data.local.entity.ScheduleEntryEntity

/**
 * Local-only Room database. All data stays in app-private storage.
 *
 * Schema note: this is version 1 of the Namaz Times schema. The earlier AI Studio prototype used a
 * separate database file seeded with fabricated mosque timings, which must never be presented as
 * real data, so it is deliberately not migrated. Every future schema change must add an explicit
 * migration here; destructive fallback is never used.
 */
@Database(
    entities = [
        SavedPlaceEntity::class,
        MosqueProfileEntity::class,
        MosqueScheduleEntity::class,
        ScheduleEntryEntity::class,
        ManualOverrideEntity::class,
        ReminderPreferenceEntity::class,
        PrayerOccurrenceEntity::class,
        ScheduledAlarmEntity::class,
        PrayerHistoryEntity::class,
        QadaRecordEntity::class,
        QuranSessionEntity::class,
        QuranGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NamazDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun mosqueDao(): MosqueDao
    abstract fun prayerDao(): PrayerDao
    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: NamazDatabase? = null

        fun get(context: Context): NamazDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                NamazDatabase::class.java,
                "namaz_times.db"
            ).build().also { INSTANCE = it }
        }
    }
}
