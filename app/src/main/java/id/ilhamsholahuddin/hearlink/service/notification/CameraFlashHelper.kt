package id.ilhamsholahuddin.hearlink.service.notification

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mengontrol LED flash kamera untuk sinyal visual saat level CRITICAL.
 * Perangkat tanpa flash akan di-skip secara graceful.
 */
class CameraFlashHelper(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val hasFlash: Boolean by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    private var flashJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /** ID kamera belakang pertama yang ditemukan, null jika tidak ada */
    private val cameraId: String? by lazy {
        runCatching {
            cameraManager.cameraIdList.firstOrNull()
        }.getOrNull()
    }

    /**
     * Kedipkan LED selama [durationMs] ms dengan pola on/off [intervalMs] ms.
     * Jika perangkat tidak punya flash, fungsi ini diabaikan secara diam.
     */
    fun triggerFlash(durationMs: Long = 3000L, intervalMs: Long = 200L) {
        if (!hasFlash || cameraId == null) return

        stopFlash() // Hentikan kedipan sebelumnya jika ada

        flashJob = scope.launch {
            val endTime = System.currentTimeMillis() + durationMs
            var torchOn = false
            while (System.currentTimeMillis() < endTime) {
                torchOn = !torchOn
                setTorch(torchOn)
                delay(intervalMs)
            }
            setTorch(false) // Selalu matikan di akhir
        }
    }

    fun stopFlash() {
        flashJob?.cancel()
        flashJob = null
        if (hasFlash && cameraId != null) {
            setTorch(false)
        }
    }

    private fun setTorch(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                cameraId?.let { id -> cameraManager.setTorchMode(id, enabled) }
            }
        }
    }
}
