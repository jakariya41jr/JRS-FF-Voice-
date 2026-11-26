package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY isCustom ASC, id ASC")
    fun observeAllProfiles(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles ORDER BY isCustom ASC, id ASC")
    suspend fun getAllProfilesOnce(): List<VoiceProfileEntity>

    @Query("SELECT * FROM voice_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): VoiceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<VoiceProfileEntity>)

    @Update
    suspend fun updateProfile(profile: VoiceProfileEntity)

    @Query("DELETE FROM voice_profiles WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomProfile(id: Long)
}
