package id.ilhamsholahuddin.hearlink.data

import kotlinx.coroutines.flow.Flow

// Repository untuk Transkrip
class TranscriptRepository(private val transcriptDao: TranscriptDao) {
    val allTranscripts: Flow<List<Transcript>> = transcriptDao.getAllTranscripts()

    suspend fun insert(transcript: Transcript) {
        transcriptDao.insertTranscript(transcript)
    }

    suspend fun delete(transcript: Transcript) {
        transcriptDao.deleteTranscript(transcript)
    }
}

// Repository untuk Kontak Darurat
class EmergencyContactRepository(private val contactDao: EmergencyContactDao) {
    val allContacts: Flow<List<EmergencyContact>> = contactDao.getAllContacts()

    suspend fun insert(contact: EmergencyContact) {
        contactDao.insertContact(contact)
    }

    suspend fun update(contact: EmergencyContact) {
        contactDao.updateContact(contact)
    }

    suspend fun delete(contact: EmergencyContact) {
        contactDao.deleteContact(contact)
    }
}

// Repository untuk Profil Pengguna
class UserRepository(private val userDao: UserDao) {
    val user: Flow<User?> = userDao.getUser()

    suspend fun insert(user: User) {
        userDao.insertUser(user)
    }
}

// Repository untuk Kalimat Favorit
class FavoritePhraseRepository(private val favoritePhraseDao: FavoritePhraseDao) {
    val allFavoritePhrases: Flow<List<FavoritePhrase>> = favoritePhraseDao.getAllFavoritePhrases()

    suspend fun insert(phrase: FavoritePhrase) {
        favoritePhraseDao.insertFavoritePhrase(phrase)
    }

    suspend fun update(phrase: FavoritePhrase) {
        favoritePhraseDao.updateFavoritePhrase(phrase)
    }

    suspend fun delete(phrase: FavoritePhrase) {
        favoritePhraseDao.deleteFavoritePhrase(phrase)
    }
}

// Repository untuk Kalimat Kustom
class CustomPhraseRepository(private val customPhraseDao: CustomPhraseDao) {
    val allCustomPhrases: Flow<List<CustomPhrase>> = customPhraseDao.getAllCustomPhrases()

    suspend fun insert(phrase: CustomPhrase) {
        customPhraseDao.insertCustomPhrase(phrase)
    }

    suspend fun update(phrase: CustomPhrase) {
        customPhraseDao.updateCustomPhrase(phrase)
    }

    suspend fun delete(phrase: CustomPhrase) {
        customPhraseDao.deleteCustomPhrase(phrase)
    }
}

// Repository untuk Riwayat Kalimat
class PhraseHistoryRepository(private val phraseHistoryDao: PhraseHistoryDao) {
    val allPhraseHistory: Flow<List<PhraseHistory>> = phraseHistoryDao.getAllPhraseHistory()

    suspend fun insert(history: PhraseHistory) {
        phraseHistoryDao.insertPhraseHistory(history)
    }

    suspend fun clearHistory() {
        phraseHistoryDao.clearHistory()
    }

    suspend fun delete(history: PhraseHistory) {
        phraseHistoryDao.deletePhraseHistory(history)
    }
}

// Repository untuk Smart Conversation Mode
class ConversationRepository(private val conversationDao: ConversationDao) {

    val allSessions: Flow<List<ConversationSession>> = conversationDao.getAllSessions()

    fun getTurnsForSession(sessionId: Int): Flow<List<ConversationTurn>> =
        conversationDao.getTurnsForSession(sessionId)

    /** Simpan sesi baru dan kembalikan ID yang di-generate */
    suspend fun insertSession(session: ConversationSession): Long =
        conversationDao.insertSession(session)

    suspend fun updateSession(session: ConversationSession) =
        conversationDao.updateSession(session)

    suspend fun insertTurn(turn: ConversationTurn): Long =
        conversationDao.insertTurn(turn)

    /** Hapus sesi beserta seluruh turn-nya */
    suspend fun deleteSessionWithTurns(sessionId: Int) {
        conversationDao.deleteTurnsForSession(sessionId)
        conversationDao.deleteSessionById(sessionId)
    }
}