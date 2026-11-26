package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.audio.MediaImporter
import com.example.audio.SoundEngine
import com.example.audio.WavSynth
import com.example.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.db.SoundCardEntity
import com.example.data.db.VoiceProfileEntity
import com.example.data.prefs.AppPreferences
import com.example.network.CartesiaTtsClient
import com.example.ui.theme.PadAccents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class SoundboardRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: AppPreferences,
    val soundEngine: SoundEngine
) {
    private val soundDao = db.soundDao()
    private val voiceDao = db.voiceProfileDao()
    private val cartesiaClient = CartesiaTtsClient()
    private val mediaImporter = MediaImporter(context)
    private val backupManager = BackupManager(context)

    private val soundsDir: File
        get() = File(context.filesDir, "sounds").apply { mkdirs() }

    val cards: Flow<List<SoundCardEntity>> = soundDao.observeAllCards()
    val voiceProfiles: Flow<List<VoiceProfileEntity>> = voiceDao.observeAllProfiles()

    suspend fun ensureDefaultsSeeded() = withContext(Dispatchers.IO) {
        val existingVoices = voiceDao.getAllProfilesOnce()
        if (existingVoices.isEmpty()) {
            val defaults = listOf(
                VoiceProfileEntity(
                    nameBn = "বাংলা মেয়ে (নাবিলা)",
                    nameEn = "Bengali Female (Nabila)",
                    gender = "FEMALE",
                    languageCode = "bn",
                    cartesiaVoiceId = "5619d38c-cf51-4d8e-9575-48f61a280413",
                    pitch = 1.06f,
                    speed = 1.0f,
                    isCustom = false,
                    isDefault = true
                ),
                VoiceProfileEntity(
                    nameBn = "বাংলা ছেলে (রাফি)",
                    nameEn = "Bengali Male (Rafi)",
                    gender = "MALE",
                    languageCode = "bn",
                    cartesiaVoiceId = "694f9389-aac1-45b6-b726-9d9369183238",
                    pitch = 0.90f,
                    speed = 1.0f,
                    isCustom = false,
                    isDefault = true
                ),
                VoiceProfileEntity(
                    nameBn = "হিন্দি মেয়ে (আনন্যা)",
                    nameEn = "Hindi Female (Ananya)",
                    gender = "FEMALE",
                    languageCode = "hi",
                    cartesiaVoiceId = "faf0731e-dfb9-4cfc-8119-259a79b27e12",
                    pitch = 1.05f,
                    speed = 1.0f,
                    isCustom = false,
                    isDefault = true
                ),
                VoiceProfileEntity(
                    nameBn = "হিন্দি ছেলে (আরিয়ান)",
                    nameEn = "Hindi Male (Aryan)",
                    gender = "MALE",
                    languageCode = "hi",
                    cartesiaVoiceId = "79f8b5fb-2cc8-479a-80df-29f7a7cf1a3e",
                    pitch = 0.88f,
                    speed = 1.0f,
                    isCustom = false,
                    isDefault = true
                )
            )
            voiceDao.insertProfiles(defaults)
        }

        if (!prefs.hasSeededDefaults && soundDao.getAllCardsOnce().isEmpty()) {
            val chimeFile = File(soundsDir, "starter_chime.wav")
            val bassFile = File(soundsDir, "starter_bass.wav")
            val pingFile = File(soundsDir, "starter_ping.wav")

            val chimeDur = WavSynth.writeChimePad(chimeFile)
            val bassDur = WavSynth.writeBassHitPad(bassFile)
            val pingDur = WavSynth.writePingPad(pingFile)

            soundDao.insertCards(
                listOf(
                    SoundCardEntity(
                        title = "স্টুডিও ইন্ট্রো / Intro Chime",
                        category = "SYNTH",
                        filePath = chimeFile.absolutePath,
                        durationMs = chimeDur,
                        isFavorite = true,
                        colorHex = PadAccents[0]
                    ),
                    SoundCardEntity(
                        title = "বেস ড্রপ / Bass Hit",
                        category = "SYNTH",
                        filePath = bassFile.absolutePath,
                        durationMs = bassDur,
                        isFavorite = true,
                        colorHex = PadAccents[1]
                    ),
                    SoundCardEntity(
                        title = "অ্যালার্ট পিং / Alert Ping",
                        category = "SYNTH",
                        filePath = pingFile.absolutePath,
                        durationMs = pingDur,
                        isFavorite = false,
                        colorHex = PadAccents[2]
                    ),
                    SoundCardEntity(
                        title = "স্বাগতম ভয়েস / Welcome TTS",
                        category = "TTS",
                        filePath = "",
                        ttsText = "স্বাগতম! আপনার সাউন্ডবোর্ড এখন সম্পূর্ণ প্রস্তুত।",
                        durationMs = 2400L,
                        isFavorite = false,
                        colorHex = PadAccents[3]
                    )
                )
            )
            prefs.hasSeededDefaults = true
        }
    }

    suspend fun toggleFavorite(card: SoundCardEntity) {
        soundDao.setFavorite(card.id, !card.isFavorite)
    }

    suspend fun renameCard(cardId: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isNotEmpty()) {
            soundDao.renameCard(cardId, clean)
        }
    }

    suspend fun deleteCard(card: SoundCardEntity) = withContext(Dispatchers.IO) {
        if (soundEngine.playingCardId.value == card.id) {
            soundEngine.stopAll()
        }
        if (card.filePath.isNotBlank()) {
            try {
                File(card.filePath).delete()
            } catch (_: Throwable) {
            }
        }
        soundDao.deleteCardById(card.id)
    }

    suspend fun importMediaUri(uri: Uri): Boolean {
        val imported = mediaImporter.importFromUri(uri) ?: return false
        val accent = PadAccents[(System.currentTimeMillis() % PadAccents.size).toInt()]
        soundDao.insertCard(
            SoundCardEntity(
                title = imported.title,
                category = "IMPORTED",
                filePath = imported.filePath,
                durationMs = imported.durationMs,
                colorHex = accent
            )
        )
        return true
    }

    data class TtsPlaybackStatus(
        val usedCartesia: Boolean,
        val savedCard: Boolean,
        val message: String
    )

    suspend fun generateAndPlayOrSaveTts(
        text: String,
        profile: VoiceProfileEntity,
        pitch: Float,
        speed: Float,
        voiceId: String,
        saveToCard: Boolean,
        customTitle: String? = null
    ): TtsPlaybackStatus {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return TtsPlaybackStatus(usedCartesia = false, savedCard = false, message = "Empty text")
        }

        val apiKey = prefs.cartesiaApiKey.value
        val outFile = File(soundsDir, "tts_${System.currentTimeMillis()}.wav")

        if (apiKey.isNotBlank()) {
            val result = cartesiaClient.synthesizeToWav(
                apiKey = apiKey,
                text = cleanText,
                voiceId = voiceId,
                languageCode = profile.languageCode,
                speed = speed,
                outputFile = outFile
            )
            if (result is CartesiaTtsClient.SynthesisResult.Success) {
                val dur = mediaImporter.readDurationMs(outFile)
                if (saveToCard) {
                    val accent = PadAccents[(System.currentTimeMillis() % PadAccents.size).toInt()]
                    val id = soundDao.insertCard(
                        SoundCardEntity(
                            title = customTitle?.takeIf { it.isNotBlank() } ?: cleanText.take(28),
                            category = "TTS",
                            filePath = outFile.absolutePath,
                            ttsText = cleanText,
                            voiceProfileId = profile.id,
                            durationMs = dur,
                            colorHex = accent
                        )
                    )
                    soundEngine.playFile(outFile.absolutePath, id)
                    return TtsPlaybackStatus(true, true, "Cartesia AI")
                } else {
                    soundEngine.playFile(outFile.absolutePath, null)
                    return TtsPlaybackStatus(true, false, "Cartesia AI")
                }
            }
        }

        // Offline TTS fallback
        if (saveToCard) {
            val synthesizedOffline = soundEngine.synthesizeOfflineToFile(
                text = cleanText,
                languageCode = profile.languageCode,
                gender = profile.gender,
                pitch = pitch,
                speed = speed,
                outputFile = outFile
            )
            val dur = if (synthesizedOffline) mediaImporter.readDurationMs(outFile) else 2000L
            val accent = PadAccents[(System.currentTimeMillis() % PadAccents.size).toInt()]
            val id = soundDao.insertCard(
                SoundCardEntity(
                    title = customTitle?.takeIf { it.isNotBlank() } ?: cleanText.take(28),
                    category = "TTS",
                    filePath = if (synthesizedOffline) outFile.absolutePath else "",
                    ttsText = cleanText,
                    voiceProfileId = profile.id,
                    durationMs = dur,
                    colorHex = accent
                )
            )
            if (synthesizedOffline) {
                soundEngine.playFile(outFile.absolutePath, id)
            } else {
                soundEngine.speakOffline(cleanText, profile.languageCode, profile.gender, pitch, speed)
            }
            return TtsPlaybackStatus(false, true, "Offline TTS")
        } else {
            soundEngine.speakOffline(cleanText, profile.languageCode, profile.gender, pitch, speed)
            return TtsPlaybackStatus(false, false, "Offline TTS")
        }
    }

    suspend fun saveCustomVoiceProfile(
        name: String,
        gender: String,
        languageCode: String,
        voiceId: String,
        pitch: Float,
        speed: Float
    ): Long {
        return voiceDao.insertProfile(
            VoiceProfileEntity(
                nameBn = name.trim(),
                nameEn = name.trim(),
                gender = gender,
                languageCode = languageCode.trim().lowercase().ifBlank { "bn" },
                cartesiaVoiceId = voiceId.trim(),
                pitch = pitch,
                speed = speed,
                isCustom = true,
                isDefault = false
            )
        )
    }

    suspend fun updateVoiceProfile(profile: VoiceProfileEntity) {
        voiceDao.updateProfile(profile)
    }

    suspend fun deleteCustomVoiceProfile(id: Long) {
        voiceDao.deleteCustomProfile(id)
    }

    suspend fun exportBackup(uri: Uri): Boolean {
        val allCards = soundDao.getAllCardsOnce()
        val allVoices = voiceDao.getAllProfilesOnce()
        return backupManager.exportToZip(uri, allCards, allVoices)
    }

    suspend fun importBackup(uri: Uri): Boolean {
        val restored = backupManager.importFromZip(uri) ?: return false
        if (restored.second.isNotEmpty()) {
            voiceDao.insertProfiles(restored.second)
        }
        if (restored.first.isNotEmpty()) {
            soundDao.insertCards(restored.first)
        }
        return true
    }
}
