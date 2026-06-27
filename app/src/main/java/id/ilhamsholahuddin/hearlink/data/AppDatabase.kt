package id.ilhamsholahuddin.hearlink.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `custom_phrases` " +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`phrase` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_phrases_new` " +
            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`phrase` TEXT NOT NULL, " +
            "`source` TEXT NOT NULL)"
        )
        database.execSQL("INSERT INTO `favorite_phrases_new` SELECT * FROM `favorite_phrases`")
        database.execSQL("DROP TABLE `favorite_phrases`")
        database.execSQL("ALTER TABLE `favorite_phrases_new` RENAME TO `favorite_phrases`")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Tabel sesi percakapan Smart Conversation Mode
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `conversation_sessions` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "`durationMs` INTEGER NOT NULL)"
        )
        // Tabel giliran bicara (turns) per sesi
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `conversation_turns` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`sessionId` INTEGER NOT NULL, " +
            "`speakerLabel` TEXT NOT NULL, " +
            "`text` TEXT NOT NULL, " +
            "`timestampMs` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [
        User::class,
        EmergencyContact::class,
        Transcript::class,
        FavoritePhrase::class,
        CustomPhrase::class,
        PhraseHistory::class,
        ConversationSession::class,
        ConversationTurn::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transcriptDao(): TranscriptDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun userDao(): UserDao
    abstract fun favoritePhraseDao(): FavoritePhraseDao
    abstract fun customPhraseDao(): CustomPhraseDao
    abstract fun phraseHistoryDao(): PhraseHistoryDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hearlink_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
