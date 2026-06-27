package id.ilhamsholahuddin.hearlink.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.data.AppDatabase
import id.ilhamsholahuddin.hearlink.data.ConversationRepository
import id.ilhamsholahuddin.hearlink.data.ConversationSession
import id.ilhamsholahuddin.hearlink.data.ConversationTurn
import id.ilhamsholahuddin.hearlink.service.conversation.SmartConversationEngine
import id.ilhamsholahuddin.hearlink.service.speech.SpeechRecognizerHelper
import id.ilhamsholahuddin.hearlink.service.tts.TTSServiceHelper
import id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern
import id.ilhamsholahuddin.hearlink.service.vibration.HapticService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val db               = AppDatabase.getDatabase(application)
    private val conversationRepo = ConversationRepository(db.conversationDao())
    private val speechHelper     = SpeechRecognizerHelper(application)
    private val ttsHelper        = TTSServiceHelper(application)
    private val engine           = SmartConversationEngine()
    private val hapticService    = HapticService(application)

    // ── State publik ──────────────────────────────────────────────────────────

    val turns: StateFlow<List<ConversationTurn>> = engine.turns
    val isSessionActive: StateFlow<Boolean>       = engine.isSessionActive
    val sessionDurationMs: StateFlow<Long>        = engine.sessionDurationMs
    val isOtherSpeaking: StateFlow<Boolean>       = engine.isOtherSpeaking
    val isListening: StateFlow<Boolean>           = speechHelper.isListening

    val allSessions: StateFlow<List<ConversationSession>> = conversationRepo.allSessions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // ── Anti-echo flag ────────────────────────────────────────────────────────
    /**
     * Saat TTS sedang berbicara, set true agar STT tidak memproses
     * suara yang berasal dari speaker (feedback/echo).
     */
    private var isTtsSpeaking = false

    // ── Feed STT → Engine ─────────────────────────────────────────────────────

    private var lastTranscription = ""

    init {
        // Kumpulkan transkripsi dari STT dan teruskan ke engine sebagai "other"
        viewModelScope.launch {
            speechHelper.transcription.collect { text ->
                if (text != lastTranscription && text.isNotBlank()) {
                    val newPart = if (text.startsWith(lastTranscription)) {
                        text.removePrefix(lastTranscription).trim()
                    } else text.trim()
                    if (newPart.isNotBlank()) engine.onNewTranscription(newPart)
                    lastTranscription = text
                }
            }
        }
    }

    // ── Kontrol Sesi ──────────────────────────────────────────────────────────

    fun startSession() {
        engine.startSession()
        lastTranscription = ""
        speechHelper.clearTranscription()
        hapticService.vibratePattern(HapticPattern.GENTLE_WAVE)
    }

    fun stopSession() {
        speechHelper.stopListening()
        engine.forceFlushOtherTurn()
        ttsHelper.stop()
        engine.stopSession()
    }

    /** Menyalakan / mematikan mikrofon untuk merekam suara lawan bicara secara manual */
    fun toggleListening() {
        if (speechHelper.isListening.value) {
            speechHelper.stopListening()
            engine.forceFlushOtherTurn()
        } else {
            speechHelper.clearTranscription()
            lastTranscription = ""
            speechHelper.startListening()
            hapticService.vibratePattern(HapticPattern.PULSE_ONCE)
        }
    }

    /** Hapus semua riwayat chat dan kembalikan ke state awal (placeholder) */
    fun clearChat() {
        engine.clearTurns()
    }

    // ── Pengguna mengetik → TTS ───────────────────────────────────────────────

    /**
     * Mengirim pesan ketikan pengguna:
     * 1. Tambahkan ke engine sebagai turn "me".
     * 2. Bacakan via TTS.
     */
    fun sendTypedMessage(text: String) {
        if (text.isBlank()) return
        
        // Hentikan rekaman lawan bicara jika sedang berjalan agar TTS tidak bocor
        if (speechHelper.isListening.value) {
            speechHelper.stopListening()
            engine.forceFlushOtherTurn()
        }

        engine.addTypedTurn(text)
        ttsHelper.speak(text)
        hapticService.vibratePattern(HapticPattern.PULSE_ONCE)
    }

    // ── Simpan ke Room ────────────────────────────────────────────────────────

    fun saveSession(title: String) {
        val currentTurns = engine.turns.value
        if (currentTurns.isEmpty()) {
            _saveStatus.value = SaveStatus.Error("Tidak ada percakapan untuk disimpan.")
            return
        }
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            try {
                val session = ConversationSession(
                    title = title.ifBlank {
                        "Percakapan ${
                            java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date())
                        }"
                    },
                    createdAt = System.currentTimeMillis(),
                    durationMs = engine.sessionDurationMs.value
                )
                val sessionId = conversationRepo.insertSession(session).toInt()
                currentTurns.forEach { turn ->
                    conversationRepo.insertTurn(turn.copy(sessionId = sessionId))
                }
                hapticService.vibratePattern(HapticPattern.PULSE_ONCE)
                _saveStatus.value = SaveStatus.Success
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "Gagal menyimpan sesi.")
            }
        }
    }

    fun resetSaveStatus() { _saveStatus.value = SaveStatus.Idle }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch { conversationRepo.deleteSessionWithTurns(sessionId) }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.destroy()
        ttsHelper.destroy()
        engine.destroy()
    }
}

sealed class SaveStatus {
    object Idle    : SaveStatus()
    object Saving  : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}
