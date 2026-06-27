package id.ilhamsholahuddin.hearlink.service.conversation

import id.ilhamsholahuddin.hearlink.data.ConversationTurn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Engine Smart Conversation Mode.
 *
 * ### Alur Baru (v2):
 * - **Pengguna ("me")**: Hanya bisa mengirim teks yang diketik. Teks dikirim secara eksplisit
 *   melalui [addTypedTurn]. Teks ini akan di-TTS oleh ViewModel.
 * - **Lawan Bicara ("other")**: Mikrofon selalu aktif mendengarkan. Teks hasil transkripsi
 *   selalu dilabeli sebagai "other". Bila ada jeda [SILENCE_THRESHOLD_MS], turn diputus.
 */
class SmartConversationEngine {

    companion object {
        const val SILENCE_THRESHOLD_MS = 1500L
        const val SPEAKER_ME    = "me"
        const val SPEAKER_OTHER = "other"
    }

    // ── State publik ──────────────────────────────────────────────────────────

    private val _turns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val turns: StateFlow<List<ConversationTurn>> = _turns.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _sessionDurationMs = MutableStateFlow(0L)
    val sessionDurationMs: StateFlow<Long> = _sessionDurationMs.asStateFlow()

    /** true ketika engine sedang mengakumulasi suara dari lawan bicara */
    private val _isOtherSpeaking = MutableStateFlow(false)
    val isOtherSpeaking: StateFlow<Boolean> = _isOtherSpeaking.asStateFlow()

    // ── State internal ────────────────────────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.Main)
    private var silenceJob: Job? = null
    private var durationJob: Job? = null
    private var sessionStartTime = 0L

    private val currentTurnText = StringBuilder()
    private var currentTurnStartMs = 0L

    // ── Kontrol Sesi ──────────────────────────────────────────────────────────

    fun startSession() {
        _turns.value = emptyList()
        currentTurnText.clear()
        sessionStartTime = System.currentTimeMillis()
        currentTurnStartMs = sessionStartTime
        _isSessionActive.value = true
        _sessionDurationMs.value = 0L

        durationJob = scope.launch {
            while (_isSessionActive.value) {
                delay(1000L)
                _sessionDurationMs.value = System.currentTimeMillis() - sessionStartTime
            }
        }
    }

    fun stopSession() {
        _isSessionActive.value = false
        _isOtherSpeaking.value = false
        durationJob?.cancel()
        silenceJob?.cancel()
        flushOtherTurn()   // Flush sisa ucapan lawan bicara jika ada
    }

    // ── Input dari PENGGUNA (ketikan → TTS) ──────────────────────────────────

    /**
     * Dipanggil oleh ViewModel saat pengguna menekan "Kirim".
     * Teks langsung ditambahkan sebagai turn "me" tanpa melalui STT.
     */
    fun addTypedTurn(text: String) {
        if (!_isSessionActive.value || text.isBlank()) return
        val turn = ConversationTurn(
            sessionId = 0,
            speakerLabel = SPEAKER_ME,
            text = text.trim(),
            timestampMs = System.currentTimeMillis()
        )
        _turns.value = _turns.value + turn
    }

    // ── Input dari LAWAN BICARA (suara → STT) ────────────────────────────────

    /**
     * Dipanggil setiap kali ada transkripsi baru dari SpeechRecognizerHelper.
     * Selalu dilabeli sebagai "other".
     */
    fun onNewTranscription(text: String) {
        if (!_isSessionActive.value || text.isBlank()) return

        if (currentTurnText.isEmpty()) {
            currentTurnStartMs = System.currentTimeMillis()
        }
        _isOtherSpeaking.value = true

        if (currentTurnText.isNotEmpty()) currentTurnText.append(" ")
        currentTurnText.append(text.trim())

        // Reset timer keheningan
        silenceJob?.cancel()
        silenceJob = scope.launch {
            delay(SILENCE_THRESHOLD_MS)
            if (_isSessionActive.value) {
                flushOtherTurn()
            }
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    fun forceFlushOtherTurn() {
        silenceJob?.cancel()
        flushOtherTurn()
    }

    private fun flushOtherTurn() {
        val text = currentTurnText.toString().trim()
        _isOtherSpeaking.value = false
        if (text.isBlank()) return

        val turn = ConversationTurn(
            sessionId = 0,
            speakerLabel = SPEAKER_OTHER,
            text = text,
            timestampMs = currentTurnStartMs
        )
        _turns.value = _turns.value + turn
        currentTurnText.clear()
    }

    fun clearTurns() {
        silenceJob?.cancel()
        currentTurnText.clear()
        _turns.value = emptyList()
        _isOtherSpeaking.value = false
    }

    fun destroy() {
        silenceJob?.cancel()
        durationJob?.cancel()
        _isSessionActive.value = false
    }
}
