package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VoxDeckApp
import com.example.data.db.SoundCardEntity
import com.example.data.db.VoiceProfileEntity
import com.example.data.prefs.AudioRoutingMode
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SoundFilter {
    ALL,
    FAVORITES,
    TTS,
    IMPORTED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VoxDeckApp).container
    private val prefs = container.prefs
    private val repository = container.repository
    private val soundEngine = container.soundEngine
    val rootShizuku = container.rootShizukuController

    val language: StateFlow<AppLanguage> = prefs.language
    val darkMode: StateFlow<Boolean> = prefs.darkMode
    val cartesiaApiKey: StateFlow<String> = prefs.cartesiaApiKey
    val volume: StateFlow<Float> = prefs.volume
    val routingMode: StateFlow<AudioRoutingMode> = prefs.routingMode
    val customShellCommand: StateFlow<String> = prefs.customShellCommand

    val playingCardId: StateFlow<Long?> = soundEngine.playingCardId
    val isTtsSpeaking: StateFlow<Boolean> = soundEngine.isTtsSpeaking
    val isOverlayRunning: StateFlow<Boolean> = soundEngine.isOverlayRunning

    val rootStatus = rootShizuku.rootStatus
    val shizukuStatus = rootShizuku.shizukuStatus
    val commandOutput = rootShizuku.commandOutput

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SoundFilter.ALL)
    val selectedFilter: StateFlow<SoundFilter> = _selectedFilter.asStateFlow()

    private val _selectedGender = MutableStateFlow("FEMALE")
    val selectedGender: StateFlow<String> = _selectedGender.asStateFlow()

    private val _ttsText = MutableStateFlow("")
    val ttsText: StateFlow<String> = _ttsText.asStateFlow()

    private val _pitch = MutableStateFlow(1.06f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _editableVoiceId = MutableStateFlow("")
    val editableVoiceId: StateFlow<String> = _editableVoiceId.asStateFlow()

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    val allCards: StateFlow<List<SoundCardEntity>> = repository.cards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredCards: StateFlow<List<SoundCardEntity>> = combine(
        allCards,
        _searchQuery,
        _selectedFilter
    ) { cards, query, filter ->
        val q = query.trim().lowercase()
        cards.filter { card ->
            val matchesFilter = when (filter) {
                SoundFilter.ALL -> true
                SoundFilter.FAVORITES -> card.isFavorite
                SoundFilter.TTS -> card.category == "TTS"
                SoundFilter.IMPORTED -> card.category == "IMPORTED"
            }
            val matchesQuery = q.isEmpty() ||
                card.title.lowercase().contains(q) ||
                (card.ttsText?.lowercase()?.contains(q) == true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allVoiceProfiles: StateFlow<List<VoiceProfileEntity>> = repository.voiceProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedVoiceProfile: StateFlow<VoiceProfileEntity?> = combine(
        allVoiceProfiles,
        prefs.selectedVoiceProfileId,
        _selectedGender
    ) { profiles, savedId, gender ->
        val saved = profiles.firstOrNull { it.id == savedId }
        if (saved != null && saved.gender == gender) saved
        else profiles.firstOrNull { it.gender == gender } ?: profiles.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
        rootShizuku.registerShizukuListeners()
        viewModelScope.launch {
            selectedVoiceProfile.collect { profile ->
                if (profile != null) {
                    _pitch.value = profile.pitch
                    _speed.value = profile.speed
                    _editableVoiceId.value = profile.cartesiaVoiceId
                }
            }
        }
    }

    override fun onCleared() {
        rootShizuku.unregisterShizukuListeners()
        super.onCleared()
    }

    private fun strings() = stringsFor(language.value)

    fun toggleLanguage() = prefs.toggleLanguage()

    fun setLanguage(lang: AppLanguage) = prefs.setLanguage(lang)

    fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

    fun updateVolume(vol: Float) = soundEngine.updateVolume(vol)

    fun stopAllSounds() = soundEngine.stopAll()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: SoundFilter) {
        _selectedFilter.value = filter
    }

    fun togglePlayCard(card: SoundCardEntity) {
        val voice = allVoiceProfiles.value.firstOrNull { it.id == card.voiceProfileId }
            ?: selectedVoiceProfile.value
        soundEngine.toggleCard(card, voice)
    }

    fun toggleFavorite(card: SoundCardEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(card)
        }
    }

    fun renameCard(cardId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameCard(cardId, newTitle)
        }
    }

    fun deleteCard(card: SoundCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun importMediaUri(uri: Uri) {
        viewModelScope.launch {
            val ok = repository.importMediaUri(uri)
            _uiMessages.tryEmit(if (ok) strings().importSuccess else strings().importFailed)
        }
    }

    // Voice Studio controls
    fun selectGender(gender: String) {
        _selectedGender.value = gender
        val matching = allVoiceProfiles.value.firstOrNull { it.gender == gender }
        if (matching != null) {
            prefs.setSelectedVoiceProfileId(matching.id)
        }
    }

    fun selectVoiceProfile(profile: VoiceProfileEntity) {
        _selectedGender.value = profile.gender
        prefs.setSelectedVoiceProfileId(profile.id)
        _pitch.value = profile.pitch
        _speed.value = profile.speed
        _editableVoiceId.value = profile.cartesiaVoiceId
    }

    fun setPitch(value: Float) {
        _pitch.value = value.coerceIn(0.5f, 2.0f)
    }

    fun setSpeed(value: Float) {
        _speed.value = value.coerceIn(0.5f, 2.0f)
    }

    fun setEditableVoiceId(id: String) {
        _editableVoiceId.value = id
    }

    fun persistCurrentVoiceParams() {
        val current = selectedVoiceProfile.value ?: return
        viewModelScope.launch {
            repository.updateVoiceProfile(
                current.copy(
                    pitch = _pitch.value,
                    speed = _speed.value,
                    cartesiaVoiceId = _editableVoiceId.value.trim()
                )
            )
        }
    }

    fun setTtsText(text: String) {
        _ttsText.value = text
    }

    fun previewSelectedVoice() {
        val profile = selectedVoiceProfile.value ?: return
        val sample = when (profile.languageCode) {
            "hi" -> if (profile.gender == "MALE") "नमस्ते, यह मेरी आवाज़ का प्रीव्यू है।" else "नमस्ते, मैं आपकी आवाज़ असिस्टेंट हूँ।"
            "en" -> if (profile.gender == "MALE") "Hello, this is a male studio voice preview." else "Hello, this is a female studio voice preview."
            else -> if (profile.gender == "MALE") "হ্যালো, এটি ছেলে ভয়েস প্রিভিউ।" else "হ্যালো, এটি মেয়ে ভয়েস প্রিভিউ।"
        }
        viewModelScope.launch {
            _isSynthesizing.value = true
            try {
                repository.generateAndPlayOrSaveTts(
                    text = _ttsText.value.trim().ifBlank { sample },
                    profile = profile,
                    pitch = _pitch.value,
                    speed = _speed.value,
                    voiceId = _editableVoiceId.value,
                    saveToCard = false
                )
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    fun playOrSaveTts(saveToCard: Boolean, cardTitle: String? = null) {
        val profile = selectedVoiceProfile.value ?: return
        val text = _ttsText.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _isSynthesizing.value = true
            try {
                val res = repository.generateAndPlayOrSaveTts(
                    text = text,
                    profile = profile,
                    pitch = _pitch.value,
                    speed = _speed.value,
                    voiceId = _editableVoiceId.value,
                    saveToCard = saveToCard,
                    customTitle = cardTitle
                )
                if (res.savedCard) {
                    _uiMessages.tryEmit("${strings().cardSavedMsg} (${res.message})")
                }
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    fun addCustomVoiceProfile(
        name: String,
        gender: String,
        languageCode: String,
        voiceId: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repository.saveCustomVoiceProfile(
                name = name,
                gender = gender,
                languageCode = languageCode,
                voiceId = voiceId,
                pitch = _pitch.value,
                speed = _speed.value
            )
            _selectedGender.value = gender
            prefs.setSelectedVoiceProfileId(newId)
        }
    }

    fun deleteCustomVoiceProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomVoiceProfile(id)
        }
    }

    // Settings & Backup
    fun saveCartesiaApiKey(key: String) {
        prefs.setCartesiaApiKey(key)
        val msg = if (key.isBlank()) strings().apiKeyClearedMsg else strings().apiKeySavedMsg
        _uiMessages.tryEmit(msg)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = repository.exportBackup(uri)
            _uiMessages.tryEmit(if (ok) strings().backupExportSuccess else strings().importFailed)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = repository.importBackup(uri)
            _uiMessages.tryEmit(if (ok) strings().backupImportSuccess else strings().importFailed)
        }
    }

    // System, Root, Shizuku & Routing
    fun setRoutingMode(mode: AudioRoutingMode) {
        soundEngine.applyRoutingMode(mode)
    }

    fun checkRootAccess() {
        viewModelScope.launch {
            rootShizuku.checkRootAccess()
        }
    }

    fun refreshShizuku() = rootShizuku.refreshShizukuStatus()

    fun requestShizukuPermission() = rootShizuku.requestShizukuPermission()

    fun executeAudioShellCommand(cmd: String) {
        viewModelScope.launch {
            rootShizuku.executeAudioControlCommand(cmd)
        }
    }
}
