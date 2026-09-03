package com.oriyu90.fcampro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_profiles")
data class CameraProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iso: Int?,
    val shutterSpeedNs: Long?,
    val focusDistance: Float?,
    val whiteBalanceMode: Int?,
    val timestamp: Long = System.currentTimeMillis(),
)
