package id.ilhamsholahuddin.hearlink.service.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    // All SpeechRecognizer operations MUST run on the Main thread
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isExplicitStop = false
    private var isRestarting = false

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _rmsdB = MutableStateFlow(0f)
    val rmsdB: StateFlow<Float> = _rmsdB.asStateFlow()

    private val recognitionIntent: Intent
        get() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    fun startListening() {
        // Ensure execution on Main thread - SpeechRecognizer requirement
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }
        // Reset explicit stop flag because we are manually starting it
        isExplicitStop = false
        
        if (_isListening.value) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Speech recognition tidak tersedia di perangkat ini."
            return
        }

        // Always create a fresh recognizer to avoid busy/stale state issues
        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        _isListening.value = true
        _error.value = null
        isRestarting = false
        speechRecognizer?.startListening(recognitionIntent)
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _error.value = null
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            _rmsdB.value = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            // Will be followed by onResults or onError - don't change isListening here
        }

        override fun onError(errorId: Int) {
            _isListening.value = false
            _rmsdB.value = 0f

            // Only show meaningful errors to the user
            if (errorId != SpeechRecognizer.ERROR_NO_MATCH &&
                errorId != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                errorId != SpeechRecognizer.ERROR_RECOGNIZER_BUSY
            ) {
                _error.value = "Error: $errorId"
            }

            // Schedule restart on Main thread with a small delay
            if (!isExplicitStop && !isRestarting) {
                isRestarting = true
                val delayMs = if (errorId == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 800L else 300L
                mainHandler.postDelayed({
                    if (!isExplicitStop) {
                        startListening()
                    }
                }, delayMs)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognized = matches[0].trim()
                if (recognized.isNotBlank()) {
                    val currentText = _transcription.value
                    _transcription.value = if (currentText.isBlank()) recognized else "$currentText $recognized"
                }
            }
            _isListening.value = false
            _rmsdB.value = 0f

            // Restart immediately for continuous listening
            if (!isExplicitStop && !isRestarting) {
                isRestarting = true
                mainHandler.postDelayed({
                    if (!isExplicitStop) {
                        startListening()
                    }
                }, 150L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun stopListening() {
        // Ensure execution on Main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }
        isExplicitStop = true
        isRestarting = false
        _isListening.value = false
        _rmsdB.value = 0f
        mainHandler.removeCallbacksAndMessages(null)
        // Panggil stop lalu destroy agar mic benar-benar dilepas
        // Tanpa destroy(), recognizer tetap memegang mic → ERROR_RECOGNIZER_BUSY saat restart
        speechRecognizer?.stopListening()
        destroyRecognizer()
    }

    fun clearTranscription() {
        _transcription.value = ""
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun destroy() {
        isExplicitStop = true
        isRestarting = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
    }
}
