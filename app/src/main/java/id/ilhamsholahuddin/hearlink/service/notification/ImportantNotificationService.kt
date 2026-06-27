package id.ilhamsholahuddin.hearlink.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import id.ilhamsholahuddin.hearlink.MainActivity
import id.ilhamsholahuddin.hearlink.R
import id.ilhamsholahuddin.hearlink.service.sound.SoundAlertLevel

/**
 * Mengelola pembuatan NotificationChannel dan pengiriman heads-up notification
 * saat [SoundAlertService] mendeteksi suara di level LOUD atau CRITICAL.
 */
class ImportantNotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "hearlink_sound_alert"
        const val NOTIFICATION_ID_LOUD = 1001
        const val NOTIFICATION_ID_CRITICAL = 1002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HearLink Sound Alert",
                NotificationManager.IMPORTANCE_HIGH  // Heads-up style
            ).apply {
                description = "Notifikasi saat suara keras terdeteksi di sekitar Anda"
                enableVibration(false) // Haptic dikelola oleh HapticService
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Kirim heads-up notification sesuai level alert.
     * Hanya dipanggil untuk level LOUD dan CRITICAL.
     */
    fun triggerAlert(level: SoundAlertLevel) {
        if (!level.shouldNotify) return

        val (title, body, notifId, priority) = when (level) {
            SoundAlertLevel.LOUD -> AlertContent(
                title = "⚠️ Suara Keras Terdeteksi",
                body = "Suara di sekitar Anda tergolong keras (75–90 dB). Harap waspada.",
                notifId = NOTIFICATION_ID_LOUD,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            SoundAlertLevel.CRITICAL -> AlertContent(
                title = "🚨 Peringatan Suara Kritis!",
                body = "Terdeteksi suara sangat keras (>90 dB). Mungkin alarm atau sirine darurat!",
                notifId = NOTIFICATION_ID_CRITICAL,
                priority = NotificationCompat.PRIORITY_MAX
            )
            else -> return
        }

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun cancelAll() {
        notificationManager.cancel(NOTIFICATION_ID_LOUD)
        notificationManager.cancel(NOTIFICATION_ID_CRITICAL)
    }

    private data class AlertContent(
        val title: String,
        val body: String,
        val notifId: Int,
        val priority: Int
    )
}
