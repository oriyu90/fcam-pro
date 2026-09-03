package com.oriyu90.fcampro.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: CameraProfileDao) {
    val allProfiles: Flow<List<CameraProfile>> = dao.getAllProfiles()

    suspend fun insert(profile: CameraProfile) = dao.insertProfile(profile)

    suspend fun update(profile: CameraProfile) = dao.updateProfile(profile)

    suspend fun deleteById(id: Int) = dao.deleteProfileById(id)
}
