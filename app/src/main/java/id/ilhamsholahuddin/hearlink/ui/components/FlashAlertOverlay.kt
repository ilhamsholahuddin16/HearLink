package id.ilhamsholahuddin.hearlink.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import id.ilhamsholahuddin.hearlink.service.sound.SoundAlertLevel

/**
 * Overlay layar berkedip yang muncul saat [SoundAlertService] mendeteksi suara
 * di level MEDIUM, LOUD, atau CRITICAL.
 *
 * Overlay ini harus dipasang di level Scaffold (di atas semua konten) di MainActivity.
 * Otomatis tidak terlihat saat level == SILENT.
 *
 * @param alertLevel Level alert saat ini dari SoundAlertService.
 */
@Composable
fun FlashAlertOverlay(alertLevel: SoundAlertLevel) {
    if (alertLevel == SoundAlertLevel.SILENT) return

    val overlayColor = when (alertLevel) {
        SoundAlertLevel.MEDIUM   -> Color(0x33FFD60A) // kuning transparan
        SoundAlertLevel.LOUD     -> Color(0x55FF3B30) // merah sedang
        SoundAlertLevel.CRITICAL -> Color(0xCCFF3B30) // merah pekat
        else -> return
    }

    when (alertLevel) {
        SoundAlertLevel.MEDIUM -> {
            // Fade halus tanpa kedipan — hanya indikasi visual ringan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }
        SoundAlertLevel.LOUD -> {
            // Pulse lambat — berkedip 1x per detik
            val infiniteTransition = rememberInfiniteTransition(label = "loud_pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loud_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF3B30).copy(alpha = alpha))
            )
        }
        SoundAlertLevel.CRITICAL -> {
            // Kedipan cepat — sinyal darurat
            val infiniteTransition = rememberInfiniteTransition(label = "critical_flash")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "critical_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF3B30).copy(alpha = alpha))
            )
        }
        else -> {}
    }
}
