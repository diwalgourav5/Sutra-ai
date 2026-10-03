package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun observeActiveProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY isActive DESC, updatedAt DESC")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE user_profiles SET isActive = 1, updatedAt = :timestamp WHERE id = :profileId")
    suspend fun activateProfileById(profileId: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun switchActiveProfile(profileId: Long) {
        deactivateAllProfiles()
        activateProfileById(profileId)
    }

    @Query("DELETE FROM user_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllConversationsOnce(): List<ConversationEntity>

    @Query(
        """
        SELECT DISTINCT c.* FROM conversations c
        LEFT JOIN chat_messages m ON c.id = m.conversationId
        WHERE c.title LIKE '%' || :query || '%'
           OR c.lastMessagePreview LIKE '%' || :query || '%'
           OR m.content LIKE '%' || :query || '%'
        ORDER BY c.isPinned DESC, c.updatedAt DESC
        """
    )
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :newTitle, updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun renameConversation(
        conversationId: Long,
        newTitle: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE conversations SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun setConversationPinned(
        conversationId: Long,
        isPinned: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getMessagesForConversationOnce(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET content = :content, isError = :isError, webSourcesJson = :webSourcesJson, usedWebSearch = :usedWebSearch, modelUsed = :modelUsed WHERE id = :messageId")
    suspend fun updateMessageContent(
        messageId: Long,
        content: String,
        isError: Boolean = false,
        webSourcesJson: String? = null,
        usedWebSearch: Boolean = false,
        modelUsed: String = "gemini-3.5-flash"
    )

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND id >= :fromMessageId")
    suspend fun deleteMessagesFromIdInclusive(conversationId: Long, fromMessageId: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND id > :afterMessageId")
    suspend fun deleteMessagesAfterId(conversationId: Long, afterMessageId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)
}

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images ORDER BY createdAt DESC")
    fun observeAllImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE id = :imageId LIMIT 1")
    suspend fun getImageById(imageId: Long): GeneratedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity): Long

    @Update
    suspend fun updateImage(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long)

    @Query("DELETE FROM generated_images")
    suspend fun deleteAllImages()
}
