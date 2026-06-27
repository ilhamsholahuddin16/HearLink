package id.ilhamsholahuddin.hearlink.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * TTS helper yang secara eksplisit menargetkan Google TTS engine
 * (com.google.android.tts) untuk mendapatkan suara Indonesia terbaik.
 * Fallback ke engine default jika Google TTS tidak terinstall.
 */
class TTSServiceHelper(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"
        private const val TAG = "TTSServiceHelper"
    }

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Callback yang dipanggil saat TTS mulai berbicara */
    var onSpeechStart: (() -> Unit)? = null
    /** Callback yang dipanggil saat TTS selesai berbicara */
    var onSpeechDone: (() -> Unit)? = null

    init {
        initTTS()
    }

    private fun isGoogleTTSAvailable(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo(GOOGLE_TTS_ENGINE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun initTTS() {
        tts = if (isGoogleTTSAvailable()) {
            Log.d(TAG, "Menggunakan Google TTS engine")
            TextToSpeech(appContext, this, GOOGLE_TTS_ENGINE)
        } else {
            Log.d(TAG, "Google TTS tidak tersedia, pakai engine default")
            TextToSpeech(appContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                _isInitialized.value = true
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)

                // Pasang listener untuk notifikasi mulai & selesai bicara
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onSpeechStart?.invoke() }
                    override fun onDone(utteranceId: String?)  { onSpeechDone?.invoke()  }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { onSpeechDone?.invoke()  }
                })
            } else {
                Log.w(TAG, "Bahasa Indonesia tidak didukung di engine ini")
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f) {
        if (_isInitialized.value) {
            tts?.setSpeechRate(speed)
            // Gunakan utteranceId agar UtteranceProgressListener aktif
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${System.currentTimeMillis()}")
        }
    }

    fun stop() { tts?.stop() }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}
