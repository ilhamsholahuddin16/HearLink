package id.ilhamsholahuddin.hearlink.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts ORDER BY createdAt DESC")
    fun getAllTranscripts(): Flow<List<Transcript>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: Transcript)

    @Delete
    suspend fun deleteTranscript(transcript: Transcript)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}

@Dao
interface FavoritePhraseDao {
    @Query("SELECT * FROM favorite_phrases")
    fun getAllFavoritePhrases(): Flow<List<FavoritePhrase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoritePhrase(phrase: FavoritePhrase)

    @Update
    suspend fun updateFavoritePhrase(phrase: FavoritePhrase)

    @Delete
    suspend fun deleteFavoritePhrase(phrase: FavoritePhrase)
}

@Dao
interface CustomPhraseDao {
    @Query("SELECT * FROM custom_phrases ORDER BY createdAt DESC")
    fun getAllCustomPhrases(): Flow<List<CustomPhrase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPhrase(phrase: CustomPhrase)

    @Update
    suspend fun updateCustomPhrase(phrase: CustomPhrase)

    @Delete
    suspend fun deleteCustomPhrase(phrase: CustomPhrase)
}

@Dao
interface PhraseHistoryDao {
    @Query("SELECT * FROM phrase_history ORDER BY usedAt DESC")
    fun getAllPhraseHistory(): Flow<List<PhraseHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhraseHistory(history: PhraseHistory)

    @Query("DELETE FROM phrase_history")
    suspend fun clearHistory()

    @Delete
    suspend fun deletePhraseHistory(history: PhraseHistory)
}

// ─── Smart Conversation Mode ──────────────────────────────────

@Dao
interface ConversationDao {
    // ─ Session ───────────────────────────────────────
    @Query("SELECT * FROM conversation_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession): Long

    @Update
    suspend fun updateSession(session: ConversationSession)

    @Delete
    suspend fun deleteSession(session: ConversationSession)

    @Query("DELETE FROM conversation_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    // ─ Turns ───────────────────────────────────────
    @Query("SELECT * FROM conversation_turns WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getTurnsForSession(sessionId: Int): Flow<List<ConversationTurn>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ConversationTurn): Long

    @Delete
    suspend fun deleteTurn(turn: ConversationTurn)

    @Query("DELETE FROM conversation_turns WHERE sessionId = :sessionId")
    suspend fun deleteTurnsForSession(sessionId: Int)
}