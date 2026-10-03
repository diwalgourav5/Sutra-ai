package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        GeneratedImageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SutraDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun generatedImageDao(): GeneratedImageDao

    companion object {
        @Volatile
        private var INSTANCE: SutraDatabase? = null

        fun getInstance(context: Context): SutraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SutraDatabase::class.java,
                    "sutra_ai_assistant.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
