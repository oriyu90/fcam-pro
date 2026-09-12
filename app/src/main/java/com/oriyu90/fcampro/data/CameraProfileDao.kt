package com.oriyu90.fcampro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraProfileDao {
    @Query("SELECT * FROM camera_profiles ORDER BY sortOrder ASC, timestamp DESC")
    fun getAllProfiles(): Flow<List<CameraProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CameraProfile)

    @Update
    suspend fun updateProfile(profile: CameraProfile)

    @Update
    @Transaction
    suspend fun updateProfiles(profiles: List<CameraProfile>)

    @Query("DELETE FROM camera_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
