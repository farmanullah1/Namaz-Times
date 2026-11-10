package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueDao {
    @Query("SELECT * FROM mosque_locations ORDER BY isActive DESC, id ASC")
    fun getAllMosques(): Flow<List<MosqueLocation>>

    @Query("SELECT * FROM mosque_locations WHERE id = :id LIMIT 1")
    suspend fun getMosqueById(id: Long): MosqueLocation?

    @Query("SELECT * FROM mosque_locations WHERE isActive = 1 LIMIT 1")
    fun getActiveMosqueFlow(): Flow<MosqueLocation?>

    @Query("SELECT * FROM mosque_locations WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveMosque(): MosqueLocation?

    @Query("SELECT COUNT(*) FROM mosque_locations")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMosque(mosque: MosqueLocation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mosques: List<MosqueLocation>)

    @Update
    suspend fun updateMosque(mosque: MosqueLocation)

    @Query("DELETE FROM mosque_locations WHERE id = :id")
    suspend fun deleteMosqueById(id: Long)

    @Query("UPDATE mosque_locations SET isActive = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE mosque_locations SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Long)
}
