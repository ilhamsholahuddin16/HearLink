package id.ilhamsholahuddin.hearlink.service.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import id.ilhamsholahuddin.hearlink.service.vibration.HapticService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class SoundAlertService(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _decibelLevel = MutableStateFlow(0f)
    val decibelLevel: StateFlow<Float> = _decibelLevel.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    /** Level alert berdasarkan dB yang terukur — subscribe untuk reaksi UI / haptic */
    private val _alertLevel = MutableStateFlow(SoundAlertLevel.SILENT)
    val alertLevel: StateFlow<SoundAlertLevel> = _alertLevel.asStateFlow()

    // Null means no error, non-null string is an error description
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val hapticService = HapticService(context)
    private val prefs = context.getSharedPreferences("hearlink_settings", Context.MODE_PRIVATE)
    private var lastVibrationTime = 0L
    private val vibrationCooldownMs = 3000L // cooldown 3 detik — proteksi baterai

    fun startListening() {
        if (_isListening.value) return

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _error.value = "Izin RECORD_AUDIO belum diberikan."
            return
        }

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            _error.value = "Tidak dapat menghitung ukuran buffer mikrofon."
            return
        }
        // Gunakan minimal 2x min buffer untuk menghindari underrun
        val bufferSize = maxOf(minBuffer * 2, 4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            _error.value = "AudioRecord gagal diinisialisasi. Coba tutup aplikasi lain yang menggunakan mikrofon."
            return
        }

        _error.value = null
        audioRecord = record
        record.startRecording()
        _isListening.value = true

        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize)
            while (isActive && _isListening.value) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: break
                if (readResult > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until readResult) {
                        val sample = buffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / readResult)
                    // Map RMS ke dB: minimum 0 dB untuk keheningan
                    val db = if (rms > 1.0) (20.0 * log10(rms)).toFloat() else 0f
                    _decibelLevel.value = db

                    // Tentukan level alert dan picu haptic jika cooldown habis
                    val level = SoundAlertLevel.fromDecibel(db)
                    _alertLevel.value = level

                    if (level != SoundAlertLevel.SILENT && level.hapticPattern != null) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastVibrationTime >= vibrationCooldownMs) {
                            val selectedPatternName = prefs.getString("haptic_pattern", id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern.TRIPLE_TAP.name)
                            val pattern = runCatching { id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern.valueOf(selectedPatternName ?: "") }
                                .getOrDefault(id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern.TRIPLE_TAP)
                            
                            // Vibrator API HARUS dipanggil dari Main Thread
                            withContext(Dispatchers.Main) {
                                hapticService.vibratePattern(pattern)
                            }
                            lastVibrationTime = currentTime
                        }
                    }
                }
                delay(100)
            }
        }
    }

    fun stopListening() {
        _isListening.value = false
        _decibelLevel.value = 0f
        _alertLevel.value = SoundAlertLevel.SILENT
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        hapticService.stop()
    }
}
