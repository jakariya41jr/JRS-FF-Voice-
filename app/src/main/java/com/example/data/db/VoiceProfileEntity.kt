package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameBn: String,
    val nameEn: String,
    val gender: String, // "FEMALE" or "MALE"
    val languageCode: String, // "bn", "hi", "en"
    val cartesiaVoiceId: String,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false
)
