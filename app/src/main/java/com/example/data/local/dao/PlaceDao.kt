package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY priorityRank ASC, displayName ASC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places ORDER BY priorityRank ASC, displayName ASC")
    suspend fun getAll(): List<SavedPlaceEntity>

    @Query("SELECT * FROM saved_places WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedPlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM saved_places")
    suspend fun deleteAll()

    @Query("UPDATE saved_places SET mosqueProfileId = NULL WHERE mosqueProfileId = :mosqueId")
    suspend fun clearMosqueLinks(mosqueId: String)
}
