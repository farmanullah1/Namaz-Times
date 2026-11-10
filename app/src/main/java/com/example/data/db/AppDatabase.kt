package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MosqueLocation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mosqueDao(): MosqueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noor_salah_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialMosques(database.mosqueDao())
                    }
                }
            }
        }

        suspend fun populateInitialMosques(dao: MosqueDao) {
            if (dao.getCount() > 0) return

            val defaultMosques = listOf(
                MosqueLocation(
                    name = "Defence Mosque",
                    area = "Defence Phase 5, Karachi",
                    latitude = 24.8016,
                    longitude = 67.0673,
                    radiusMeters = 5000.0,
                    isCustomTimes = true,
                    fajrAzan = "05:15",
                    fajrIqamah = "05:40",
                    sunriseTime = "06:38",
                    dhuhrAzan = "12:20",
                    dhuhrIqamah = "12:45",
                    asrAzan = "15:45", // Ahl al-Hadith standard (1x shadow)
                    asrIqamah = "16:05",
                    maghribAzan = "17:46",
                    maghribIqamah = "17:56",
                    ishaAzan = "19:05",
                    ishaIqamah = "19:25",
                    jummahIqamah = "13:15",
                    isActive = true, // Default active
                    isPreset = true,
                    note = "Masjid Usman Ghani / Jamia Masjid Ahl-e-Hadith Defence"
                ),
                MosqueLocation(
                    name = "Clifton Mosque",
                    area = "Clifton Block 2, Karachi",
                    latitude = 24.8183,
                    longitude = 67.0322,
                    radiusMeters = 4000.0,
                    isCustomTimes = true,
                    fajrAzan = "05:16",
                    fajrIqamah = "05:35",
                    sunriseTime = "06:39",
                    dhuhrAzan = "12:21",
                    dhuhrIqamah = "12:40",
                    asrAzan = "15:46",
                    asrIqamah = "16:00",
                    maghribAzan = "17:47",
                    maghribIqamah = "17:55",
                    ishaAzan = "19:05",
                    ishaIqamah = "19:20",
                    jummahIqamah = "13:15",
                    isActive = false,
                    isPreset = true,
                    note = "Jamia Masjid Clifton, Karachi"
                ),
                MosqueLocation(
                    name = "Hala New City Mosque",
                    area = "Hala New City, Matiari, Sindh",
                    latitude = 25.8117,
                    longitude = 68.4214,
                    radiusMeters = 8000.0,
                    isCustomTimes = true,
                    fajrAzan = "05:10",
                    fajrIqamah = "05:30",
                    sunriseTime = "06:32",
                    dhuhrAzan = "12:15",
                    dhuhrIqamah = "12:35",
                    asrAzan = "15:40",
                    asrIqamah = "15:55",
                    maghribAzan = "17:40",
                    maghribIqamah = "17:48",
                    ishaAzan = "18:58",
                    ishaIqamah = "19:15",
                    jummahIqamah = "13:00",
                    isActive = false,
                    isPreset = true,
                    note = "Jamia Masjid Ahl al-Hadith, Hala New City"
                )
            )
            dao.insertAll(defaultMosques)
        }
    }
}
