package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sound_cards")
data class SoundCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "TTS", "IMPORTED", "SYNTH"
    val filePath: String = "",
    val ttsText: String? = null,
    val voiceProfileId: Long? = null,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val colorHex: Long = 0xFF22D3EE,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
