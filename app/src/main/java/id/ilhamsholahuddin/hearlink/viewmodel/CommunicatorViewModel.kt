package id.ilhamsholahuddin.hearlink.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.ilhamsholahuddin.hearlink.data.AppDatabase
import id.ilhamsholahuddin.hearlink.data.CustomPhrase
import id.ilhamsholahuddin.hearlink.data.CustomPhraseRepository
import id.ilhamsholahuddin.hearlink.data.FavoritePhrase
import id.ilhamsholahuddin.hearlink.data.FavoritePhraseRepository
import id.ilhamsholahuddin.hearlink.data.PhraseHistory
import id.ilhamsholahuddin.hearlink.data.PhraseHistoryRepository
import id.ilhamsholahuddin.hearlink.service.tts.TTSServiceHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunicatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val favoritePhraseRepository = FavoritePhraseRepository(db.favoritePhraseDao())
    private val customPhraseRepository = CustomPhraseRepository(db.customPhraseDao())
    private val phraseHistoryRepository = PhraseHistoryRepository(db.phraseHistoryDao())
    private val ttsServiceHelper = TTSServiceHelper(application)
    private val sharedPrefs = application.getSharedPreferences("hearlink_prefs", android.content.Context.MODE_PRIVATE)

    init {
        checkAndInitializeDefaultPhrases()
    }

    private fun checkAndInitializeDefaultPhrases() {
        val isInitialized = sharedPrefs.getBoolean("default_phrases_initialized", false)
        if (!isInitialized) {
            val defaultPhrases = listOf(
                "Saya memiliki disabilitas pendengaran.",
                "Tolong ulangi.",
                "Tolong bicara lebih pelan.",
                "Tolong tuliskan pesan Anda.",
                "Halo! Apa kabar?",
                "Terima kasih.",
                "Sama-sama.",
                "Sampai jumpa.",
                "Di mana toilet?",
                "Berapa harganya?",
                "Saya ingin membayar.",
                "Bisakah Anda membantu saya?",
                "Tolong bantu saya!",
                "Hubungi keluarga saya."
            )
            viewModelScope.launch {
                defaultPhrases.forEach { phrase ->
                    customPhraseRepository.insert(CustomPhrase(phrase = phrase))
                }
                sharedPrefs.edit().putBoolean("default_phrases_initialized", true).apply()
            }
        }
    }

    val favoritePhrases: StateFlow<List<FavoritePhrase>> = favoritePhraseRepository.allFavoritePhrases
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customPhrases: StateFlow<List<CustomPhrase>> = customPhraseRepository.allCustomPhrases
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val phraseHistory: StateFlow<List<PhraseHistory>> = phraseHistoryRepository.allPhraseHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun speak(text: String, speed: Float = 1.0f) {
        if (text.isNotBlank()) {
            ttsServiceHelper.speak(text, speed)
            saveToHistory(text)
        }
    }

    private fun saveToHistory(text: String) {
        viewModelScope.launch {
            phraseHistoryRepository.insert(
                PhraseHistory(phrase = text, usedAt = System.currentTimeMillis())
            )
        }
    }

    // --- Favorite Phrases ---
    fun addFavorite(phrase: String, source: String) {
        viewModelScope.launch {
            favoritePhraseRepository.insert(FavoritePhrase(phrase = phrase, source = source))
        }
    }

    fun updateFavorite(favoritePhrase: FavoritePhrase, newPhrase: String) {
        viewModelScope.launch {
            favoritePhraseRepository.update(favoritePhrase.copy(phrase = newPhrase))
        }
    }

    fun removeFavorite(favoritePhrase: FavoritePhrase) {
        viewModelScope.launch {
            favoritePhraseRepository.delete(favoritePhrase)
        }
    }

    // --- Custom Phrases ---
    fun addCustomPhrase(phrase: String) {
        if (phrase.isBlank()) return
        viewModelScope.launch {
            customPhraseRepository.insert(CustomPhrase(phrase = phrase))
        }
    }

    fun updateCustomPhrase(customPhrase: CustomPhrase, newPhrase: String) {
        if (newPhrase.isBlank()) return
        viewModelScope.launch {
            customPhraseRepository.update(customPhrase.copy(phrase = newPhrase))
        }
    }

    fun deleteCustomPhrase(customPhrase: CustomPhrase) {
        viewModelScope.launch {
            customPhraseRepository.delete(customPhrase)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            phraseHistoryRepository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsServiceHelper.destroy()
    }
}
