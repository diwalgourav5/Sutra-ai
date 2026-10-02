package com.example.data.repository

import com.example.data.local.ChatMessageDao
import com.example.data.local.ConversationDao
import com.example.data.local.UserProfileDao
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MathCategory
import com.example.data.model.PendingAttachment
import com.example.data.model.UserProfileEntity
import com.example.data.model.WebSource
import com.example.data.remote.AiGenerationResult
import com.example.data.remote.ApiConfig
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.Flow

class SutraRepository(
    private val userProfileDao: UserProfileDao,
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val apiClient: GeminiApiClient = GeminiApiClient()
) {
    // --- User Profile Management ---

    val activeProfileFlow: Flow<UserProfileEntity?> = userProfileDao.observeActiveProfile()
    val allProfilesFlow: Flow<List<UserProfileEntity>> = userProfileDao.observeAllProfiles()

    suspend fun ensureDefaultProfileExists(): UserProfileEntity {
        val current = userProfileDao.getActiveProfileOnce()
        if (current != null) return current
        val defaultProfile = UserProfileEntity(
            username = "Aarav Sharma",
            roleOrFocus = "Student & Developer",
            bioOrContext = "Focused on computer science, university mathematics, and bilingual learning in English & Hinglish."
        )
        val newId = userProfileDao.insertProfile(defaultProfile)
        return defaultProfile.copy(id = newId)
    }

    suspend fun createNewProfile(profile: UserProfileEntity): Long {
        userProfileDao.deactivateAllProfiles()
        return userProfileDao.insertProfile(profile.copy(id = 0, isActive = true))
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        userProfileDao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun switchProfile(profileId: Long) {
        userProfileDao.switchActiveProfile(profileId)
    }

    suspend fun deleteProfile(profileId: Long) {
        userProfileDao.deleteProfileById(profileId)
        val active = userProfileDao.getActiveProfileOnce()
        if (active == null) {
            ensureDefaultProfileExists()
        }
    }

    // --- Conversations & History ---

    val allConversationsFlow: Flow<List<ConversationEntity>> =
        conversationDao.observeAllConversations()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        return if (query.isBlank()) {
            conversationDao.observeAllConversations()
        } else {
            conversationDao.searchConversations(query.trim())
        }
    }

    fun observeMessages(conversationId: Long): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeMessagesForConversation(conversationId)

    suspend fun getMessagesOnce(conversationId: Long): List<ChatMessageEntity> =
        chatMessageDao.getMessagesForConversationOnce(conversationId)

    suspend fun createConversation(
        title: String,
        profileId: Long,
        mode: AssistantMode
    ): Long {
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            profileId = profileId,
            title = title.take(70).ifBlank { "New Conversation" },
            assistantMode = mode.name,
            createdAt = now,
            updatedAt = now,
            lastMessagePreview = ""
        )
        return conversationDao.insertConversation(entity)
    }

    suspend fun renameConversation(conversationId: Long, newTitle: String) {
        val clean = newTitle.trim().ifBlank { "Untitled Chat" }
        conversationDao.renameConversation(conversationId, clean)
    }

    suspend fun togglePinConversation(conversation: ConversationEntity) {
        conversationDao.setConversationPinned(conversation.id, !conversation.isPinned)
    }

    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun clearAllHistory() {
        conversationDao.deleteAllConversations()
    }

    suspend fun getAllConversationsWithMessagesForBackup(): List<Pair<ConversationEntity, List<ChatMessageEntity>>> {
        val convos = conversationDao.getAllConversationsOnce()
        return convos.map { convo ->
            convo to chatMessageDao.getMessagesForConversationOnce(convo.id)
        }
    }

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        val id = chatMessageDao.insertMessage(message)
        val convo = conversationDao.getConversationById(message.conversationId)
        if (convo != null) {
            val preview = message.content.replace("\n", " ").take(100)
            conversationDao.updateConversation(
                convo.copy(
                    updatedAt = System.currentTimeMillis(),
                    lastMessagePreview = preview
                )
            )
        }
        return id
    }

    suspend fun updateMessageContent(
        messageId: Long,
        conversationId: Long,
        content: String,
        isError: Boolean = false,
        webSources: List<WebSource> = emptyList(),
        usedWebSearch: Boolean = false,
        modelUsed: String = ApiConfig.MODEL_FAST_MULTIMODAL
    ) {
        val sourcesJson = if (webSources.isNotEmpty()) WebSource.toJsonString(webSources) else null
        chatMessageDao.updateMessageContent(
            messageId = messageId,
            content = content,
            isError = isError,
            webSourcesJson = sourcesJson,
            usedWebSearch = usedWebSearch,
            modelUsed = modelUsed
        )
        val convo = conversationDao.getConversationById(conversationId)
        if (convo != null) {
            conversationDao.updateConversation(
                convo.copy(
                    updatedAt = System.currentTimeMillis(),
                    lastMessagePreview = content.replace("\n", " ").take(100)
                )
            )
        }
    }

    suspend fun deleteMessagesFromInclusive(conversationId: Long, fromMessageId: Long) {
        chatMessageDao.deleteMessagesFromIdInclusive(conversationId, fromMessageId)
    }

    suspend fun deleteMessagesAfter(conversationId: Long, afterMessageId: Long) {
        chatMessageDao.deleteMessagesAfterId(conversationId, afterMessageId)
    }

    // --- AI Generation & Diagnostics ---

    suspend fun streamAiReply(
        history: List<ChatMessageEntity>,
        userPrompt: String,
        pendingAttachment: PendingAttachment?,
        profile: UserProfileEntity?,
        language: AppLanguage,
        assistantMode: AssistantMode,
        mathCategory: MathCategory? = null,
        useWebSearch: Boolean = false,
        onChunk: suspend (accumulatedText: String, modelName: String) -> Unit
    ): AiGenerationResult {
        return apiClient.streamChatResponse(
            history = history,
            userPrompt = userPrompt,
            pendingAttachment = pendingAttachment,
            profile = profile,
            language = language,
            assistantMode = assistantMode,
            mathCategory = mathCategory,
            useWebSearch = useWebSearch,
            onChunk = onChunk
        )
    }

    suspend fun verifyBackendConnection(): Pair<Boolean, String> {
        return apiClient.verifyConnection()
    }
}
