package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioRoutingMode(val key: String) {
    STANDARD_MEDIA("standard_media"),
    VOICE_CALL_STREAM("voice_call_stream"),
    ROOT_HAL_INJECT("root_hal_inject"),
    SHIZUKU_SHELL("shizuku_shell");

    companion object {
        fun fromKey(key: String?): AudioRoutingMode =
            entries.firstOrNull { it.key == key } ?: STANDARD_MEDIA
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voxdeck_local_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        if (prefs.getString(KEY_LANGUAGE, "bn") == "en") AppLanguage.EN else AppLanguage.BN
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _cartesiaApiKey = MutableStateFlow(prefs.getString(KEY_CARTESIA_API_KEY, "") ?: "")
    val cartesiaApiKey: StateFlow<String> = _cartesiaApiKey.asStateFlow()

    private val _volume = MutableStateFlow(prefs.getFloat(KEY_VOLUME, 0.9f).coerceIn(0f, 1f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _selectedVoiceProfileId = MutableStateFlow(prefs.getLong(KEY_SELECTED_VOICE_ID, 1L))
    val selectedVoiceProfileId: StateFlow<Long> = _selectedVoiceProfileId.asStateFlow()

    private val _routingMode = MutableStateFlow(
        AudioRoutingMode.fromKey(prefs.getString(KEY_ROUTING_MODE, AudioRoutingMode.STANDARD_MEDIA.key))
    )
    val routingMode: StateFlow<AudioRoutingMode> = _routingMode.asStateFlow()

    private val _customShellCommand = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_SHELL_CMD, "dumpsys audio | head -n 25")
            ?: "dumpsys audio | head -n 25"
    )
    val customShellCommand: StateFlow<String> = _customShellCommand.asStateFlow()

    var hasSeededDefaults: Boolean
        get() = prefs.getBoolean(KEY_SEEDED_DEFAULTS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SEEDED_DEFAULTS, value).apply()
        }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _language.value = lang
    }

    fun toggleLanguage() {
        val next = if (_language.value == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
        setLanguage(next)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _darkMode.value = enabled
    }

    fun setCartesiaApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(KEY_CARTESIA_API_KEY, trimmed).apply()
        _cartesiaApiKey.value = trimmed
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_VOLUME, clamped).apply()
        _volume.value = clamped
    }

    fun setSelectedVoiceProfileId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED_VOICE_ID, id).apply()
        _selectedVoiceProfileId.value = id
    }

    fun setRoutingMode(mode: AudioRoutingMode) {
        prefs.edit().putString(KEY_ROUTING_MODE, mode.key).apply()
        _routingMode.value = mode
    }

    fun setCustomShellCommand(cmd: String) {
        prefs.edit().putString(KEY_CUSTOM_SHELL_CMD, cmd).apply()
        _customShellCommand.value = cmd
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CARTESIA_API_KEY = "cartesia_api_key"
        private const val KEY_VOLUME = "master_volume"
        private const val KEY_SELECTED_VOICE_ID = "selected_voice_profile_id"
        private const val KEY_ROUTING_MODE = "audio_routing_mode"
        private const val KEY_CUSTOM_SHELL_CMD = "custom_shell_cmd"
        private const val KEY_SEEDED_DEFAULTS = "seeded_defaults_v1"
    }
}
