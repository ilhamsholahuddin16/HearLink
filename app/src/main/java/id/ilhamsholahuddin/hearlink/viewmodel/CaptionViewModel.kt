package id.ilhamsholahuddin.hearlink.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.data.AppDatabase
import id.ilhamsholahuddin.hearlink.data.Transcript
import id.ilhamsholahuddin.hearlink.data.TranscriptRepository
import id.ilhamsholahuddin.hearlink.service.sound.SoundAlertService
import id.ilhamsholahuddin.hearlink.service.speech.SpeechRecognizerHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val transcriptRepository = TranscriptRepository(db.transcriptDao())
    private val speechRecognizerHelper = SpeechRecognizerHelper(application)
    private val soundAlertService = SoundAlertService(application)

    val transcription: StateFlow<String> = speechRecognizerHelper.transcription
    val isListening: StateFlow<Boolean> = speechRecognizerHelper.isListening
    // Gunakan rmsdB dari SpeechRecognizer — tidak perlu AudioRecord terpisah
    // sehingga tidak ada konflik mikrofon antara SoundAlertService dan SpeechRecognizer
    val decibelLevel: StateFlow<Float> = speechRecognizerHelper.rmsdB
    val isSoundAlertActive: StateFlow<Boolean> = soundAlertService.isListening
    /** Level alert suara saat ini — digunakan oleh FlashAlertOverlay di MainActivity */
    val alertLevel = soundAlertService.alertLevel

    val allTranscripts: StateFlow<List<Transcript>> = transcriptRepository.allTranscripts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleListening() {
        if (isListening.value) {
            speechRecognizerHelper.stopListening()
        } else {
            speechRecognizerHelper.startListening()
        }
    }

    fun clearTranscription() {
        speechRecognizerHelper.clearTranscription()
    }

    fun saveTranscript(title: String) {
        val content = transcription.value
        if (content.isNotBlank()) {
            viewModelScope.launch {
                transcriptRepository.insert(
                    Transcript(
                        title = title.ifBlank { "Transcript ${System.currentTimeMillis()}" },
                        content = content,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteTranscript(transcript: Transcript) {
        viewModelScope.launch {
            transcriptRepository.delete(transcript)
        }
    }

    fun toggleSoundAlert(enable: Boolean) {
        if (enable) {
            soundAlertService.startListening()
        } else {
            soundAlertService.stopListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper.destroy()
        soundAlertService.stopListening()
    }
}
