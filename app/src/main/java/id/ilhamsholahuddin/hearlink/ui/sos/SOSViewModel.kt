package id.ilhamsholahuddin.hearlink.ui.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.data.AppDatabase
import id.ilhamsholahuddin.hearlink.data.EmergencyContactRepository
import id.ilhamsholahuddin.hearlink.data.UserRepository
import id.ilhamsholahuddin.hearlink.service.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SOSViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao())
    private val contactRepository = EmergencyContactRepository(db.emergencyContactDao())
    private val locationHelper = LocationHelper(application)
    private val context: Context = application

    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _countdownValue = MutableStateFlow(5)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private var countdownJob: Job? = null

    fun startSOSCountdown() {
        if (_isCountdownActive.value) return

        _isCountdownActive.value = true
        _countdownValue.value = 5

        countdownJob = viewModelScope.launch {
            while (_countdownValue.value > 0) {
                delay(1000)
                _countdownValue.value -= 1
            }
            if (_countdownValue.value == 0) {
                executeSOS()
            }
        }
    }

    fun cancelSOS() {
        countdownJob?.cancel()
        _isCountdownActive.value = false
    }

    private suspend fun executeSOS() {
        _isCountdownActive.value = false
        
        val user = userRepository.user.firstOrNull()
        val contacts = contactRepository.allContacts.firstOrNull() ?: emptyList()
        
        if (contacts.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Tidak ada kontak darurat yang dikonfigurasi!", Toast.LENGTH_LONG).show()
            }
            return
        }
        
        val location = locationHelper.getCurrentLocation()

        val userName = user?.name ?: "Pengguna HearLink"
        val locationLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Lokasi tidak dapat diperoleh saat ini."
        }

        val message = """
            🚨 SOS HearLink

            Halo, saya $userName.
            Saya sedang dalam keadaan darurat dan membutuhkan bantuan segera.

            Lokasi saya saat ini:
            $locationLink

            Mohon segera datang atau hubungi saya jika memungkinkan.
            Pesan ini dikirim otomatis melalui aplikasi HearLink.
        """.trimIndent()

        val smsManager = context.getSystemService(SmsManager::class.java)

        contacts.forEach { contact ->
            try {
                // Send SMS automatically
                smsManager.sendTextMessage(contact.phone, null, message, null, null)

                // If WhatsApp is enabled, launch Intent
                if (contact.hasWhatsApp) {
                    val uri = Uri.parse("whatsapp://send?phone=${contact.phone}&text=${Uri.encode(message)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // Fallback to web if WhatsApp is not installed
                        val webUri = Uri.parse("https://api.whatsapp.com/send?phone=${contact.phone}&text=${Uri.encode(message)}")
                        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "SOS Berhasil Terkirim ke ${contacts.size} kontak!", Toast.LENGTH_LONG).show()
        }
    }
}
