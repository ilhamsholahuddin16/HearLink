package id.ilhamsholahuddin.hearlink.service.sound

import id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern

/**
 * Level alert suara yang terdeteksi oleh [SoundAlertService].
 *
 * @property thresholdDb Batas minimum dB untuk mencapai level ini.
 * @property hapticPattern Pola getaran yang dipicu secara otomatis.
 * @property flashColor Warna hex overlay layar (0 = tidak ada overlay).
 * @property shouldNotify Apakah level ini memicu heads-up notification.
 * @property shouldTorchFlash Apakah level ini memicu LED kamera berkedip.
 */
enum class SoundAlertLevel(
    val thresholdDb: Float,
    val hapticPattern: HapticPattern?,
    val flashColor: Long,
    val shouldNotify: Boolean,
    val shouldTorchFlash: Boolean
) {
    /** < 60 dB — lingkungan tenang, tidak ada aksi */
    SILENT(
        thresholdDb = 0f,
        hapticPattern = null,
        flashColor = 0x00000000L,
        shouldNotify = false,
        shouldTorchFlash = false
    ),

    /** 60–75 dB — suara percakapan keras / mesin ringan */
    MEDIUM(
        thresholdDb = 60f,
        hapticPattern = HapticPattern.DOUBLE_TAP,
        flashColor = 0x33FFD60AL,   // kuning transparan
        shouldNotify = false,
        shouldTorchFlash = false
    ),

    /** 75–90 dB — klakson, musik keras, teriakan */
    LOUD(
        thresholdDb = 75f,
        hapticPattern = HapticPattern.TRIPLE_TAP,
        flashColor = 0x55FF3B30L,   // merah sedang
        shouldNotify = true,
        shouldTorchFlash = false
    ),

    /** > 90 dB — alarm kebakaran, sirine darurat */
    CRITICAL(
        thresholdDb = 90f,
        hapticPattern = HapticPattern.SOS_PATTERN,
        flashColor = 0xCCFF3B30L,   // merah pekat
        shouldNotify = true,
        shouldTorchFlash = true
    );

    companion object {
        /**
         * Menentukan level alert dari nilai dB yang terukur.
         * Selalu mengembalikan level tertinggi yang ambangnya terpenuhi.
         */
        fun fromDecibel(db: Float): SoundAlertLevel = when {
            db >= CRITICAL.thresholdDb -> CRITICAL
            db >= LOUD.thresholdDb     -> LOUD
            db >= MEDIUM.thresholdDb   -> MEDIUM
            else                       -> SILENT
        }
    }
}
