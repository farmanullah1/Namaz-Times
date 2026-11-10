package com.example.data.repository

import com.example.data.db.MosqueDao
import com.example.data.db.MosqueLocation
import kotlinx.coroutines.flow.Flow

class MosqueRepository(private val mosqueDao: MosqueDao) {
    val allMosques: Flow<List<MosqueLocation>> = mosqueDao.getAllMosques()
    val activeMosque: Flow<MosqueLocation?> = mosqueDao.getActiveMosqueFlow()

    suspend fun getActiveMosque(): MosqueLocation? = mosqueDao.getActiveMosque()

    suspend fun setActiveMosque(id: Long) {
        mosqueDao.clearActiveFlag()
        mosqueDao.setActiveFlag(id)
    }

    suspend fun insert(mosque: MosqueLocation): Long = mosqueDao.insertMosque(mosque)

    suspend fun update(mosque: MosqueLocation) = mosqueDao.updateMosque(mosque)

    suspend fun delete(id: Long) = mosqueDao.deleteMosqueById(id)

    suspend fun ensureDefaultMosquesExist() {
        if (mosqueDao.getCount() == 0) {
            com.example.data.db.AppDatabase.populateInitialMosques(mosqueDao)
        }
    }
}
