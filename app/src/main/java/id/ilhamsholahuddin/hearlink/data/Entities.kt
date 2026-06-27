package id.ilhamsholahuddin.hearlink.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val photoUri: String?,
    val bloodType: String?,
    val allergy: String?,
    val updatedAt: Long
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val hasWhatsApp: Boolean = false
)

@Entity(tableName = "transcripts")
data class Transcript(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long
)

@Entity(tableName = "favorite_phrases")
data class FavoritePhrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val source: String // "quick" atau "custom"
)

@Entity(tableName = "custom_phrases")
data class CustomPhrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "phrase_history")
data class PhraseHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val usedAt: Long
)

// ─── Smart Conversation Mode ──────────────────────────────────────────────────

/** Satu sesi percakapan Smart Conversation Mode */
@Entity(tableName = "conversation_sessions")
data class ConversationSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L
)

/** Satu giliran bicara (turn) dalam sebuah sesi percakapan */
@Entity(tableName = "conversation_turns")
data class ConversationTurn(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,          // Foreign key ke ConversationSession.id
    val speakerLabel: String,    // "me" = pengguna, "other" = lawan bicara
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)