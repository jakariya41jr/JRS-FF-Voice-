package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.db.SoundCardEntity
import com.example.data.db.VoiceProfileEntity
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.AudioRoutingMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class SoundEngine(
    private val context: Context,
    private val prefs: AppPreferences
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var activePlayer: MediaPlayer? = null
    private val _playingCardId = MutableStateFlow<Long?>(null)
    val playingCardId: StateFlow<Long?> = _playingCardId.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val pendingSynth = mutableMapOf<String, CompletableDeferred<Boolean>>()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isTtsSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isTtsSpeaking.value = false
                    utteranceId?.let { pendingSynth.remove(it)?.complete(true) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isTtsSpeaking.value = false
                    utteranceId?.let { pendingSynth.remove(it)?.complete(false) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isTtsSpeaking.value = false
                    utteranceId?.let { pendingSynth.remove(it)?.complete(false) }
                }
            })
        }
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun updateVolume(volume: Float) {
        prefs.setVolume(volume)
        val clamped = volume.coerceIn(0f, 1f)
        try {
            activePlayer?.setVolume(clamped, clamped)
        } catch (_: Throwable) {
        }
    }

    fun applyRoutingMode(mode: AudioRoutingMode) {
        prefs.setRoutingMode(mode)
        try {
            audioManager.mode = when (mode) {
                AudioRoutingMode.VOICE_CALL_STREAM -> AudioManager.MODE_IN_COMMUNICATION
                else -> AudioManager.MODE_NORMAL
            }
        } catch (_: Throwable) {
        }
    }

    fun toggleCard(card: SoundCardEntity, fallbackProfile: VoiceProfileEntity? = null) {
        if (_playingCardId.value == card.id) {
            stopAll()
            return
        }
        stopAll()

        val file = File(card.filePath)
        if (card.filePath.isNotBlank() && file.exists() && file.length() > 0L) {
            playFile(card.filePath, card.id)
        } else if (!card.ttsText.isNullOrBlank()) {
            val lang = fallbackProfile?.languageCode ?: "bn"
            val gender = fallbackProfile?.gender ?: "FEMALE"
            val pitch = fallbackProfile?.pitch ?: 1.0f
            val speed = fallbackProfile?.speed ?: 1.0f
            _playingCardId.value = card.id
            speakOffline(
                text = card.ttsText,
                languageCode = lang,
                gender = gender,
                pitch = pitch,
                speed = speed
            )
        }
    }

    fun playFile(path: String, cardId: Long? = null) {
        stopAll()
        val file = File(path)
        if (!file.exists()) return

        val volume = prefs.volume.value.coerceIn(0f, 1f)
        val usage = if (prefs.routingMode.value == AudioRoutingMode.VOICE_CALL_STREAM) {
            AudioAttributes.USAGE_VOICE_COMMUNICATION
        } else {
            AudioAttributes.USAGE_MEDIA
        }

        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setVolume(volume, volume)
            player.setOnCompletionListener {
                if (_playingCardId.value == cardId) {
                    _playingCardId.value = null
                }
                releasePlayer(player)
            }
            player.setOnErrorListener { mp, _, _ ->
                if (_playingCardId.value == cardId) {
                    _playingCardId.value = null
                }
                releasePlayer(mp)
                true
            }
            player.prepare()
            activePlayer = player
            _playingCardId.value = cardId
            player.start()
        } catch (_: Throwable) {
            _playingCardId.value = null
            releasePlayer(player)
        }
    }

    fun speakOffline(
        text: String,
        languageCode: String,
        gender: String,
        pitch: Float,
        speed: Float
    ) {
        if (!ttsReady || tts == null || text.isBlank()) return
        stopPlayerOnly()
        configureTtsVoice(languageCode, gender, pitch, speed)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, prefs.volume.value.coerceIn(0f, 1f))
        }
        val utteranceId = UUID.randomUUID().toString()
        _isTtsSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    suspend fun synthesizeOfflineToFile(
        text: String,
        languageCode: String,
        gender: String,
        pitch: Float,
        speed: Float,
        outputFile: File
    ): Boolean = withContext(Dispatchers.Main) {
        if (!ttsReady || tts == null || text.isBlank()) return@withContext false
        outputFile.parentFile?.mkdirs()
        configureTtsVoice(languageCode, gender, pitch, speed)
        val utteranceId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<Boolean>()
        pendingSynth[utteranceId] = deferred
        val res = tts?.synthesizeToFile(text, Bundle(), outputFile, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            pendingSynth.remove(utteranceId)
            return@withContext false
        }
        try {
            deferred.await() && outputFile.exists() && outputFile.length() > 44L
        } catch (_: Throwable) {
            false
        }
    }

    private fun configureTtsVoice(
        languageCode: String,
        gender: String,
        pitch: Float,
        speed: Float
    ) {
        val engine = tts ?: return
        val targetLocale = when (languageCode.lowercase()) {
            "bn" -> Locale("bn", "BD")
            "hi" -> Locale("hi", "IN")
            else -> Locale.US
        }
        val avail = engine.setLanguage(targetLocale)
        if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (languageCode.lowercase() == "bn") {
                engine.setLanguage(Locale("bn", "IN"))
            }
        }

        // Match matching voice by locale and gender hint if installed on device
        try {
            val voices = engine.voices.orEmpty()
            val matchingLang = voices.filter {
                it.locale.language.equals(targetLocale.language, ignoreCase = true)
            }
            val genderHint = if (gender == "MALE") listOf("male", "man", "bdm", "inm", "b-in", "d-in")
            else listOf("female", "woman", "bdf", "inf", "a-in", "c-in")

            val bestVoice = matchingLang.firstOrNull { v ->
                val lower = v.name.lowercase()
                genderHint.any { hint -> lower.contains(hint) }
            } ?: matchingLang.firstOrNull()

            if (bestVoice != null) {
                engine.voice = bestVoice
            }
        } catch (_: Throwable) {
        }

        val genderPitchBias = if (gender == "MALE") 0.88f else 1.10f
        val effectivePitch = (pitch * genderPitchBias).coerceIn(0.5f, 2.0f)
        engine.setPitch(effectivePitch)
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
    }

    fun stopAll() {
        stopPlayerOnly()
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        _isTtsSpeaking.value = false
        _playingCardId.value = null
        try {
            if (audioManager.mode == AudioManager.MODE_IN_COMMUNICATION) {
                audioManager.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Throwable) {
        }
    }

    private fun stopPlayerOnly() {
        val player = activePlayer
        activePlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Throwable) {
            }
            releasePlayer(player)
        }
    }

    private fun releasePlayer(player: MediaPlayer) {
        try {
            player.reset()
            player.release()
        } catch (_: Throwable) {
        }
        if (activePlayer === player) {
            activePlayer = null
        }
    }

    fun shutdown() {
        stopAll()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
    }
}
