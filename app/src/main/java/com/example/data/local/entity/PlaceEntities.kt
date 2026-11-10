package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val latitude: Double?,
    val longitude: Double?,
    val geofenceRadiusMeters: Int,
    val timezoneId: String,
    val mosqueProfileId: String?,
    val priorityRank: Int,
    val isEnabled: Boolean,
    val isPriorityLocation: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
