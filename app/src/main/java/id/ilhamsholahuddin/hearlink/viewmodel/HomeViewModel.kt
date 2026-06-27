package id.ilhamsholahuddin.hearlink.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.service.sound.SoundAlertService
import id.ilhamsholahuddin.hearlink.service.vibration.HapticPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val soundAlertService = SoundAlertService(application)
    private val prefs = application.getSharedPreferences("hearlink_settings", android.content.Context.MODE_PRIVATE)

    private val _isSoundAlertEnabled = MutableStateFlow(false)
    val isSoundAlertEnabled: StateFlow<Boolean> = _isSoundAlertEnabled.asStateFlow()

    /** Real-time decibel level (0 = silence, ~80 = loud). */
    val decibelLevel: StateFlow<Float> = soundAlertService.decibelLevel

    /** Non-null when the service encountered an error (e.g. mic busy, permission missing). */
    val soundAlertError: StateFlow<String?> = soundAlertService.error

    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _isFlashEnabled = MutableStateFlow(prefs.getBoolean("flash_enabled", true))
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()

    private val _selectedHapticPattern = MutableStateFlow(
        runCatching { HapticPattern.valueOf(prefs.getString("haptic_pattern", HapticPattern.TRIPLE_TAP.name) ?: "") }
            .getOrDefault(HapticPattern.TRIPLE_TAP)
    )
    val selectedHapticPattern: StateFlow<HapticPattern> = _selectedHapticPattern.asStateFlow()

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _isVibrationEnabled.value = enabled
    }

    fun setFlashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("flash_enabled", enabled).apply()
        _isFlashEnabled.value = enabled
    }

    fun setSelectedHapticPattern(pattern: HapticPattern) {
        prefs.edit().putString("haptic_pattern", pattern.name).apply()
        _selectedHapticPattern.value = pattern
    }

    fun toggleSoundAlert() {
        viewModelScope.launch {
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _isSoundAlertEnabled.value = false
                return@launch
            }

            if (_isSoundAlertEnabled.value) {
                soundAlertService.stopListening()
                _isSoundAlertEnabled.value = false
            } else {
                soundAlertService.startListening()
                // Sync state with actual service state after attempting to start
                _isSoundAlertEnabled.value = soundAlertService.isListening.value
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            soundAlertService.startListening()
            _isSoundAlertEnabled.value = soundAlertService.isListening.value
        }
    }

    /**
     * Hentikan AudioRecord sementara tanpa mengubah state [isSoundAlertEnabled].
     * Panggil ini sebelum komponen lain (mis. SpeechRecognizer) menggunakan mikrofon.
     */
    fun pauseSoundAlertForExternalMicUse() {
        soundAlertService.stopListening()
    }

    /**
     * Nyalakan kembali Sound Alert jika sebelumnya memang aktif (isSoundAlertEnabled = true).
     * Panggil ini saat komponen lain selesai menggunakan mikrofon.
     */
    fun resumeSoundAlertIfEnabled() {
        if (_isSoundAlertEnabled.value) {
            viewModelScope.launch {
                soundAlertService.startListening()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundAlertService.stopListening()
    }
}
