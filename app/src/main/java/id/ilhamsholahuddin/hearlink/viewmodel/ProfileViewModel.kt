package id.ilhamsholahuddin.hearlink.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.data.AppDatabase
import id.ilhamsholahuddin.hearlink.data.EmergencyContact
import id.ilhamsholahuddin.hearlink.data.EmergencyContactRepository
import id.ilhamsholahuddin.hearlink.data.User
import id.ilhamsholahuddin.hearlink.data.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao())
    private val contactRepository = EmergencyContactRepository(db.emergencyContactDao())
    private val prefs = application.getSharedPreferences("hearlink_settings", android.content.Context.MODE_PRIVATE)

    val userProfile: StateFlow<User?> = userRepository.user
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val emergencyContacts: StateFlow<List<EmergencyContact>> = contactRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isVibrationEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _isVibrationEnabled.value = enabled
    }

    fun saveUserProfile(name: String, bloodType: String, allergy: String, photoUri: String?) {
        viewModelScope.launch {
            val currentUser = userProfile.value
            val updatedUser = User(
                id = currentUser?.id ?: 0,
                name = name,
                bloodType = bloodType,
                allergy = allergy,
                photoUri = photoUri,
                updatedAt = System.currentTimeMillis()
            )
            userRepository.insert(updatedUser)
        }
    }

    fun savePhotoUri(uriStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val uri = android.net.Uri.parse(uriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(context.filesDir, "profile_photo.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val savedUriStr = file.absolutePath
                    val currentUser = userProfile.value
                    if (currentUser != null) {
                        userRepository.insert(currentUser.copy(photoUri = savedUriStr, updatedAt = System.currentTimeMillis()))
                    } else {
                        userRepository.insert(
                            User(name = "", photoUri = savedUriStr, bloodType = "", allergy = "", updatedAt = System.currentTimeMillis())
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addEmergencyContact(name: String, phone: String, hasWhatsApp: Boolean) {
        viewModelScope.launch {
            contactRepository.insert(
                EmergencyContact(name = name, phone = phone, hasWhatsApp = hasWhatsApp)
            )
        }
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            contactRepository.update(contact)
        }
    }

    fun removeEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            contactRepository.delete(contact)
        }
    }
}
