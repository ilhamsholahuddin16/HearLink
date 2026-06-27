package id.ilhamsholahuddin.hearlink.service.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Pola getaran semantik yang digunakan di seluruh aplikasi HearLink */
enum class HapticPattern {
    /** Satu ketukan pendek — konfirmasi UI ringan (misal: simpan sukses) */
    PULSE_ONCE,

    /** Dua ketukan — notifikasi baru / alert sedang */
    DOUBLE_TAP,

    /** Tiga ketukan — alert LOUD / suara keras terdeteksi */
    TRIPLE_TAP,

    /** Getaran panjang tunggal — notifikasi keras */
    LONG_BUZZ,

    /** Pola morse SOS — level CRITICAL / SOS terkirim */
    SOS_PATTERN,

    /** Detak jantung — pergantian giliran bicara di Smart Conversation */
    HEART_BEAT,

    /** Gelombang lembut — Smart Conversation Mode dimulai */
    GENTLE_WAVE
}

class HapticService(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val prefs = context.getSharedPreferences("hearlink_settings", Context.MODE_PRIVATE)
    private val isVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)

    fun vibrateShort() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    fun vibrateLong() {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 400, 200, 400)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
        }
    }

    /**
     * Memainkan pola getaran berdasarkan konteks semantik.
     * Gunakan ini untuk semua kasus baru — lebih ekspresif dan bermakna.
     */
    fun vibratePattern(pattern: HapticPattern) {
        if (!isVibrationEnabled || !vibrator.hasVibrator()) return

        val timings: LongArray = when (pattern) {
            HapticPattern.PULSE_ONCE   -> longArrayOf(0, 50)
            HapticPattern.DOUBLE_TAP  -> longArrayOf(0, 80, 60, 80)
            HapticPattern.TRIPLE_TAP  -> longArrayOf(0, 70, 50, 70, 50, 70)
            HapticPattern.LONG_BUZZ   -> longArrayOf(0, 600)
            HapticPattern.SOS_PATTERN -> longArrayOf(
                // S (tiga pendek)
                0, 150, 75, 150, 75, 150,
                // O (tiga panjang)
                225, 400, 225, 400, 225, 400,
                // S (tiga pendek)
                225, 150, 75, 150, 75, 150
            )
            HapticPattern.HEART_BEAT  -> longArrayOf(0, 100, 120, 200, 300, 100, 120, 200)
            HapticPattern.GENTLE_WAVE -> longArrayOf(0, 40, 30, 40, 30, 80, 30, 40, 30, 40)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    fun stop() {
        vibrator.cancel()
    }
}
