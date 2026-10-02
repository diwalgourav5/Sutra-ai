package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SutraDatabase
import com.example.data.model.ApiDiagnosticsState
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.AssistantMode
import com.example.data.model.AvatarPreset
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MathCategory
import com.example.data.model.PendingAttachment
import com.example.data.model.TextSizeOption
import com.example.data.model.UserProfileEntity
import com.example.data.remote.AiGenerationResult
import com.example.data.remote.ApiConfig
import com.example.data.repository.SutraRepository
import com.example.util.AttachmentHelper
import com.example.util.BackupExportHelper
import com.example.util.ExportFormat
import com.example.util.ExportResult
import com.example.util.VoiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination(val route: String, val label: String) {
    CHAT("chat", "Chat"),
    MATH_SOLVER("math_solver", "Math Solver"),
    HISTORY("history", "History"),
    PROFILE("profile", "Profile"),
    SETTINGS("settings", "Settings")
}

@OptIn(ExperimentalCoroutinesApi::class)
class SutraViewModel(
    private val repository: SutraRepository,
    val voiceManager: VoiceManager? = null
) : ViewModel() {

    // --- Navigation ---
    private val _currentDestination = MutableStateFlow(AppDestination.CHAT)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    // --- User Profile State ---
    val activeProfile: StateFlow<UserProfileEntity> = repository.activeProfileFlow
        .map { it ?: UserProfileEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfileEntity()
        )

    val allProfiles: StateFlow<List<UserProfileEntity>> = repository.allProfilesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Conversation & History State ---
    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = _historySearchQuery
        .flatMapLatest { query -> repository.searchConversations(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentConversationId
        .flatMapLatest { convoId ->
            if (convoId == null) flowOf(emptyList())
            else repository.observeMessages(convoId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Composer & Generation State ---
    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _activeAssistantMode = MutableStateFlow(AssistantMode.GENERAL)
    val activeAssistantMode: StateFlow<AssistantMode> = _activeAssistantMode.asStateFlow()

    private val _webSearchEnabled = MutableStateFlow(false)
    val webSearchEnabled: StateFlow<Boolean> = _webSearchEnabled.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    val pendingAttachment: StateFlow<PendingAttachment?> = _pendingAttachment.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingPartialText = MutableStateFlow("")
    val streamingPartialText: StateFlow<String> = _streamingPartialText.asStateFlow()

    private val _streamingModelName = MutableStateFlow(ApiConfig.MODEL_FAST_MULTIMODAL)
    val streamingModelName: StateFlow<String> = _streamingModelName.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isVoiceConversationOverlayOpen = MutableStateFlow(false)
    val isVoiceConversationOverlayOpen: StateFlow<Boolean> = _isVoiceConversationOverlayOpen.asStateFlow()

    private val _lastExportResult = MutableStateFlow<ExportResult?>(null)
    val lastExportResult: StateFlow<ExportResult?> = _lastExportResult.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var activeStreamingMessageId: Long? = null

    // --- Math Solver Dedicated Workspace State ---
    private val _selectedMathCategory = MutableStateFlow(MathCategory.ALGEBRA)
    val selectedMathCategory: StateFlow<MathCategory> = _selectedMathCategory.asStateFlow()

    private val _mathInputText = MutableStateFlow("")
    val mathInputText: StateFlow<String> = _mathInputText.asStateFlow()

    private val _mathAttachment = MutableStateFlow<PendingAttachment?>(null)
    val mathAttachment: StateFlow<PendingAttachment?> = _mathAttachment.asStateFlow()

    private val _isSolvingMath = MutableStateFlow(false)
    val isSolvingMath: StateFlow<Boolean> = _isSolvingMath.asStateFlow()

    private val _mathStreamingText = MutableStateFlow("")
    val mathStreamingText: StateFlow<String> = _mathStreamingText.asStateFlow()

    private val _mathSolutionResult = MutableStateFlow<AiGenerationResult?>(null)
    val mathSolutionResult: StateFlow<AiGenerationResult?> = _mathSolutionResult.asStateFlow()

    private var mathSolverJob: Job? = null

    // --- API & Backend Diagnostics State ---
    private val _apiDiagnostics = MutableStateFlow(
        ApiDiagnosticsState(
            isKeyConfigured = ApiConfig.isApiKeyConfigured(),
            activeEndpoint = ApiConfig.BACKEND_BASE_URL,
            primaryChatModel = ApiConfig.MODEL_FAST_MULTIMODAL,
            reasoningMathModel = ApiConfig.MODEL_COMPLEX_REASONING,
            requestsInLastMinute = ApiConfig.getCurrentWindowRequestCount(),
            maxRequestsPerMinute = ApiConfig.MAX_REQUESTS_PER_MINUTE
        )
    )
    val apiDiagnostics: StateFlow<ApiDiagnosticsState> = _apiDiagnostics.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.ensureDefaultProfileExists()
            _webSearchEnabled.value = profile.defaultWebSearch
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Composer & Mode Actions ---

    fun updateComposerText(text: String) {
        _composerText.value = text
    }

    fun setAssistantMode(mode: AssistantMode) {
        _activeAssistantMode.value = mode
    }

    fun toggleWebSearch() {
        _webSearchEnabled.value = !_webSearchEnabled.value
    }

    fun setVoiceConversationOverlayOpen(open: Boolean) {
        _isVoiceConversationOverlayOpen.value = open
        if (!open) {
            voiceManager?.stopSpeaking()
        }
    }

    // --- Attachment Actions ---

    fun attachFromUri(context: Context, uri: Uri, forMathSolver: Boolean = false) {
        viewModelScope.launch {
            val placeholder = PendingAttachment(
                uriString = uri.toString(),
                fileName = "Loading attachment…",
                mimeType = "application/octet-stream",
                uploadProgress = 0.1f
            )
            if (forMathSolver) _mathAttachment.value = placeholder
            else _pendingAttachment.value = placeholder

            val processed = AttachmentHelper.processUriAttachment(context, uri) { progress ->
                if (forMathSolver) {
                    _mathAttachment.update { it?.copy(uploadProgress = progress) }
                } else {
                    _pendingAttachment.update { it?.copy(uploadProgress = progress) }
                }
            }

            if (processed.errorMessage != null) {
                _snackbarMessage.value = processed.errorMessage
                if (forMathSolver) _mathAttachment.value = null
                else _pendingAttachment.value = null
            } else {
                if (forMathSolver) _mathAttachment.value = processed
                else _pendingAttachment.value = processed
            }
        }
    }

    fun attachCameraBitmap(bitmap: Bitmap, forMathSolver: Boolean = false) {
        viewModelScope.launch {
            val processed = AttachmentHelper.processCameraBitmap(bitmap) { progress ->
                if (forMathSolver) {
                    _mathAttachment.update { it?.copy(uploadProgress = progress) }
                } else {
                    _pendingAttachment.update { it?.copy(uploadProgress = progress) }
                }
            }
            if (forMathSolver) _mathAttachment.value = processed
            else _pendingAttachment.value = processed
        }
    }

    fun clearPendingAttachment(forMathSolver: Boolean = false) {
        if (forMathSolver) _mathAttachment.value = null
        else _pendingAttachment.value = null
    }

    // --- Chat Lifecycle & Messaging Actions ---

    fun startNewChat(mode: AssistantMode = _activeAssistantMode.value) {
        stopGenerating()
        _currentConversationId.value = null
        _composerText.value = ""
        _pendingAttachment.value = null
        _activeAssistantMode.value = mode
        _currentDestination.value = AppDestination.CHAT
    }

    fun selectConversation(conversationId: Long) {
        stopGenerating()
        _currentConversationId.value = conversationId
        _currentDestination.value = AppDestination.CHAT
    }

    fun sendMessage(
        overridePrompt: String? = null,
        mathCategory: MathCategory? = null
    ) {
        val rawPrompt = (overridePrompt ?: _composerText.value).trim()
        val attachment = _pendingAttachment.value
        if (rawPrompt.isEmpty() && attachment == null) return
        if (_isGenerating.value) return

        val profile = activeProfile.value
        val maskedPrompt = ApiConfig.maskSensitiveTextIfEnabled(
            rawPrompt,
            profile.privacyMaskSensitiveData
        )

        _composerText.value = ""
        _pendingAttachment.value = null

        activeGenerationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingPartialText.value = ""

            var convoId = _currentConversationId.value
            if (convoId == null) {
                val smartTitle = buildSmartTitle(maskedPrompt, attachment, mathCategory)
                convoId = repository.createConversation(
                    title = smartTitle,
                    profileId = profile.id,
                    mode = if (mathCategory != null) AssistantMode.MATH_SOLVER else _activeAssistantMode.value
                )
                _currentConversationId.value = convoId
            }

            // Capture existing messages prior to inserting the new user message for context
            val priorHistory = repository.getMessagesOnce(convoId)

            // Insert User Message
            val userMsg = ChatMessageEntity(
                conversationId = convoId,
                role = "user",
                content = maskedPrompt.ifBlank {
                    if (attachment?.isImage == true) "Analyze attached image: ${attachment.fileName}"
                    else "Analyze attached file: ${attachment?.fileName ?: "Document"}"
                },
                timestamp = System.currentTimeMillis(),
                attachmentName = attachment?.fileName,
                attachmentMimeType = attachment?.mimeType,
                attachmentUri = attachment?.uriString,
                attachmentBase64 = attachment?.base64Data,
                isMathSolution = mathCategory != null || _activeAssistantMode.value == AssistantMode.MATH_SOLVER,
                mathCategory = mathCategory?.name
            )
            repository.insertMessage(userMsg)

            // Insert placeholder AI message for live updates
            val aiMsgId = repository.insertMessage(
                ChatMessageEntity(
                    conversationId = convoId,
                    role = "model",
                    content = "Thinking…",
                    timestamp = System.currentTimeMillis() + 1,
                    isMathSolution = mathCategory != null || _activeAssistantMode.value == AssistantMode.MATH_SOLVER,
                    mathCategory = mathCategory?.name,
                    modelUsed = if (mathCategory != null || _activeAssistantMode.value.prefersProModel) {
                        ApiConfig.MODEL_COMPLEX_REASONING
                    } else {
                        ApiConfig.MODEL_FAST_MULTIMODAL
                    }
                )
            )
            activeStreamingMessageId = aiMsgId

            try {
                val result = repository.streamAiReply(
                    history = priorHistory,
                    userPrompt = maskedPrompt,
                    pendingAttachment = attachment,
                    profile = profile,
                    language = profile.parsedLanguage,
                    assistantMode = if (mathCategory != null) AssistantMode.MATH_SOLVER else _activeAssistantMode.value,
                    mathCategory = mathCategory,
                    useWebSearch = _webSearchEnabled.value,
                    onChunk = { partial, modelName ->
                        _streamingPartialText.value = partial
                        _streamingModelName.value = modelName
                    }
                )

                repository.updateMessageContent(
                    messageId = aiMsgId,
                    conversationId = convoId,
                    content = result.text,
                    isError = result.isError,
                    webSources = result.webSources,
                    usedWebSearch = result.usedWebSearch,
                    modelUsed = result.modelUsed
                )

                // Refresh rate limiter count in diagnostics
                _apiDiagnostics.update {
                    it.copy(requestsInLastMinute = ApiConfig.getCurrentWindowRequestCount())
                }

                // Auto-speak if enabled in profile settings
                if (!result.isError && profile.voiceEnabled && profile.autoSpeakResponses) {
                    voiceManager?.speakText(
                        text = result.text,
                        messageId = aiMsgId,
                        language = profile.parsedLanguage,
                        rate = profile.speechRate,
                        pitch = profile.speechPitch
                    )
                }

                // If history saving is disabled in privacy settings, keep in memory only until session change
            } catch (_: CancellationException) {
                val partial = _streamingPartialText.value.trim()
                val stoppedContent = if (partial.isNotEmpty()) {
                    "$partial\n\n*(Generation stopped by user)*"
                } else {
                    "*(Generation stopped by user)*"
                }
                repository.updateMessageContent(
                    messageId = aiMsgId,
                    conversationId = convoId,
                    content = stoppedContent,
                    isError = false,
                    modelUsed = _streamingModelName.value
                )
            } finally {
                _isGenerating.value = false
                _streamingPartialText.value = ""
                activeStreamingMessageId = null
                activeGenerationJob = null
            }
        }
    }

    fun stopGenerating() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        mathSolverJob?.cancel()
        mathSolverJob = null
        _isGenerating.value = false
        _isSolvingMath.value = false
    }

    fun regenerateResponse(assistantMessage: ChatMessageEntity) {
        if (_isGenerating.value) return
        val convoId = assistantMessage.conversationId
        viewModelScope.launch {
            val allMsgs = repository.getMessagesOnce(convoId)
            val targetIdx = allMsgs.indexOfFirst { it.id == assistantMessage.id }
            if (targetIdx <= 0) return@launch
            val precedingUserMsg = allMsgs.subList(0, targetIdx).lastOrNull { it.isUser } ?: return@launch
            val historyBeforeUser = allMsgs.takeWhile { it.id != precedingUserMsg.id }

            // Delete from the assistant message onwards and re-run
            repository.deleteMessagesFromInclusive(convoId, assistantMessage.id)

            val reconstructedAttachment = if (!precedingUserMsg.attachmentBase64.isNullOrBlank()) {
                PendingAttachment(
                    uriString = precedingUserMsg.attachmentUri.orEmpty(),
                    fileName = precedingUserMsg.attachmentName ?: "Attachment",
                    mimeType = precedingUserMsg.attachmentMimeType ?: "image/jpeg",
                    base64Data = precedingUserMsg.attachmentBase64
                )
            } else {
                null
            }

            triggerAssistantTurnForExistingUserMessage(
                convoId = convoId,
                historyBeforeUser = historyBeforeUser,
                userMessage = precedingUserMsg,
                attachment = reconstructedAttachment
            )
        }
    }

    fun editUserMessage(userMessage: ChatMessageEntity, updatedText: String) {
        val cleanText = updatedText.trim()
        if (cleanText.isEmpty() || _isGenerating.value) return
        val convoId = userMessage.conversationId
        viewModelScope.launch {
            val allMsgs = repository.getMessagesOnce(convoId)
            val historyBeforeUser = allMsgs.takeWhile { it.id != userMessage.id }

            // Delete everything after this user message and update its content
            repository.deleteMessagesAfter(convoId, userMessage.id)
            repository.updateMessageContent(
                messageId = userMessage.id,
                conversationId = convoId,
                content = cleanText,
                isError = false,
                modelUsed = userMessage.modelUsed
            )

            val updatedUserMsg = userMessage.copy(content = cleanText)
            val reconstructedAttachment = if (!updatedUserMsg.attachmentBase64.isNullOrBlank()) {
                PendingAttachment(
                    uriString = updatedUserMsg.attachmentUri.orEmpty(),
                    fileName = updatedUserMsg.attachmentName ?: "Attachment",
                    mimeType = updatedUserMsg.attachmentMimeType ?: "image/jpeg",
                    base64Data = updatedUserMsg.attachmentBase64
                )
            } else {
                null
            }

            triggerAssistantTurnForExistingUserMessage(
                convoId = convoId,
                historyBeforeUser = historyBeforeUser,
                userMessage = updatedUserMsg,
                attachment = reconstructedAttachment
            )
        }
    }

    private fun triggerAssistantTurnForExistingUserMessage(
        convoId: Long,
        historyBeforeUser: List<ChatMessageEntity>,
        userMessage: ChatMessageEntity,
        attachment: PendingAttachment?
    ) {
        val profile = activeProfile.value
        activeGenerationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingPartialText.value = ""

            val aiMsgId = repository.insertMessage(
                ChatMessageEntity(
                    conversationId = convoId,
                    role = "model",
                    content = "Regenerating response…",
                    timestamp = System.currentTimeMillis(),
                    isMathSolution = userMessage.isMathSolution,
                    mathCategory = userMessage.mathCategory
                )
            )
            activeStreamingMessageId = aiMsgId

            try {
                val result = repository.streamAiReply(
                    history = historyBeforeUser,
                    userPrompt = userMessage.content,
                    pendingAttachment = attachment,
                    profile = profile,
                    language = profile.parsedLanguage,
                    assistantMode = _activeAssistantMode.value,
                    mathCategory = userMessage.mathCategory?.let {
                        runCatching { MathCategory.valueOf(it) }.getOrNull()
                    },
                    useWebSearch = _webSearchEnabled.value,
                    onChunk = { partial, modelName ->
                        _streamingPartialText.value = partial
                        _streamingModelName.value = modelName
                    }
                )

                repository.updateMessageContent(
                    messageId = aiMsgId,
                    conversationId = convoId,
                    content = result.text,
                    isError = result.isError,
                    webSources = result.webSources,
                    usedWebSearch = result.usedWebSearch,
                    modelUsed = result.modelUsed
                )
            } catch (_: CancellationException) {
                val partial = _streamingPartialText.value.trim()
                repository.updateMessageContent(
                    messageId = aiMsgId,
                    conversationId = convoId,
                    content = if (partial.isNotEmpty()) "$partial\n\n*(Stopped)*" else "*(Stopped)*",
                    isError = false
                )
            } finally {
                _isGenerating.value = false
                _streamingPartialText.value = ""
                activeStreamingMessageId = null
                activeGenerationJob = null
            }
        }
    }

    // --- Math Solver Dedicated Actions ---

    fun selectMathCategory(category: MathCategory) {
        _selectedMathCategory.value = category
    }

    fun updateMathInput(text: String) {
        _mathInputText.value = text
    }

    fun insertMathSymbol(symbol: String) {
        _mathInputText.update { current ->
            if (current.isEmpty()) symbol else "$current$symbol"
        }
    }

    fun solveDedicatedMathProblem() {
        val equation = _mathInputText.value.trim()
        val attachment = _mathAttachment.value
        if (equation.isEmpty() && attachment == null) {
            _snackbarMessage.value = "Please enter a math problem or attach a photo of an equation."
            return
        }
        if (_isSolvingMath.value) return

        val profile = activeProfile.value
        val category = _selectedMathCategory.value

        mathSolverJob = viewModelScope.launch {
            _isSolvingMath.value = true
            _mathStreamingText.value = ""
            _mathSolutionResult.value = null

            val prompt = buildString {
                appendLine("Category: ${category.title} (${category.subtitle})")
                if (equation.isNotBlank()) {
                    appendLine("Problem to solve step-by-step: $equation")
                } else {
                    appendLine("Extract the mathematics problem from the attached image and solve it completely step-by-step.")
                }
                appendLine("Show: 1) Given Data / Interpretation, 2) Formula / Theorem Used, 3) Complete Step-by-Step Calculation, 4) Final Boxed Answer & Verification.")
            }

            try {
                val result = repository.streamAiReply(
                    history = emptyList(),
                    userPrompt = prompt,
                    pendingAttachment = attachment,
                    profile = profile,
                    language = profile.parsedLanguage,
                    assistantMode = AssistantMode.MATH_SOLVER,
                    mathCategory = category,
                    useWebSearch = false,
                    onChunk = { partial, _ ->
                        _mathStreamingText.value = partial
                    }
                )
                _mathSolutionResult.value = result
            } catch (_: CancellationException) {
                val partial = _mathStreamingText.value.trim()
                if (partial.isNotEmpty()) {
                    _mathSolutionResult.value = AiGenerationResult(
                        text = "$partial\n\n*(Calculation stopped by user)*",
                        modelUsed = ApiConfig.MODEL_COMPLEX_REASONING,
                        usedWebSearch = false,
                        webSources = emptyList(),
                        isError = false
                    )
                }
            } finally {
                _isSolvingMath.value = false
                mathSolverJob = null
            }
        }
    }

    fun continueMathSolutionInChat() {
        val result = _mathSolutionResult.value ?: return
        val problemText = _mathInputText.value.ifBlank { "Solved ${selectedMathCategory.value.title} Problem" }
        val category = _selectedMathCategory.value
        val profile = activeProfile.value

        viewModelScope.launch {
            val convoId = repository.createConversation(
                title = "${category.symbolBadge} ${problemText.take(45)}",
                profileId = profile.id,
                mode = AssistantMode.MATH_SOLVER
            )
            repository.insertMessage(
                ChatMessageEntity(
                    conversationId = convoId,
                    role = "user",
                    content = problemText,
                    isMathSolution = true,
                    mathCategory = category.name
                )
            )
            repository.insertMessage(
                ChatMessageEntity(
                    conversationId = convoId,
                    role = "model",
                    content = result.text,
                    isMathSolution = true,
                    mathCategory = category.name,
                    modelUsed = result.modelUsed
                )
            )
            _activeAssistantMode.value = AssistantMode.MATH_SOLVER
            _currentConversationId.value = convoId
            _currentDestination.value = AppDestination.CHAT
        }
    }

    // --- History Management Actions ---

    fun updateHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun renameConversation(conversationId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(conversationId, newTitle)
            _snackbarMessage.value = "Conversation renamed"
        }
    }

    fun togglePinConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            repository.togglePinConversation(conversation)
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_currentConversationId.value == conversationId) {
                _currentConversationId.value = null
            }
            _snackbarMessage.value = "Conversation deleted"
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _currentConversationId.value = null
            _snackbarMessage.value = "All chat history cleared"
        }
    }

    fun exportConversationHistory(
        context: Context,
        format: ExportFormat,
        targetUri: Uri? = null
    ) {
        viewModelScope.launch {
            val profile = activeProfile.value
            val convosWithMessages = repository.getAllConversationsWithMessagesForBackup()
            val result = BackupExportHelper.exportHistoryToFile(
                context = context,
                profile = profile,
                conversationsWithMessages = convosWithMessages,
                format = format,
                targetUri = targetUri
            )
            _lastExportResult.value = result
            if (result.success) {
                _snackbarMessage.value = "Exported ${result.conversationCount} chat(s) to ${result.fileName}"
            } else {
                _snackbarMessage.value = "Export failed: ${result.errorMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearLastExportResult() {
        _lastExportResult.value = null
    }

    // --- User Profile & Settings Actions ---

    fun updateProfileDetails(
        username: String,
        roleOrFocus: String,
        bioOrContext: String,
        avatarPreset: AvatarPreset,
        customAvatarUri: String?
    ) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(
                current.copy(
                    username = username.trim().ifBlank { "Sutra User" },
                    roleOrFocus = roleOrFocus.trim(),
                    bioOrContext = bioOrContext.trim(),
                    avatarPreset = avatarPreset.name,
                    customAvatarUri = customAvatarUri
                )
            )
            _snackbarMessage.value = "Profile saved for ${username.trim().ifBlank { "Sutra User" }}"
        }
    }

    fun createNewUserProfile(
        username: String,
        roleOrFocus: String,
        bioOrContext: String,
        avatarPreset: AvatarPreset,
        language: AppLanguage,
        themeMode: AppThemeMode
    ) {
        viewModelScope.launch {
            val newProfile = UserProfileEntity(
                username = username.trim().ifBlank { "New User" },
                roleOrFocus = roleOrFocus.trim().ifBlank { "Explorer" },
                bioOrContext = bioOrContext.trim(),
                avatarPreset = avatarPreset.name,
                preferredLanguage = language.name,
                themeMode = themeMode.name,
                isActive = true
            )
            repository.createNewProfile(newProfile)
            _snackbarMessage.value = "Switched to new profile: ${newProfile.username}"
        }
    }

    fun switchUserProfile(profileId: Long) {
        viewModelScope.launch {
            repository.switchProfile(profileId)
            _snackbarMessage.value = "Switched active profile"
        }
    }

    fun deleteUserProfile(profileId: Long) {
        if (allProfiles.value.size <= 1) {
            _snackbarMessage.value = "At least one profile must remain active."
            return
        }
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            _snackbarMessage.value = "Profile removed"
        }
    }

    fun upsertProfileExtensibleAttribute(key: String, value: String) {
        if (key.isBlank()) return
        val current = activeProfile.value
        val updatedMap = current.getExtensibleAttributesMap().toMutableMap()
        updatedMap[key.trim()] = value.trim()
        viewModelScope.launch {
            repository.updateProfile(
                current.copy(
                    extensibleAttributesJson = UserProfileEntity.encodeExtensibleAttributes(updatedMap)
                )
            )
            _snackbarMessage.value = "Added preference attribute: ${key.trim()}"
        }
    }

    fun removeProfileExtensibleAttribute(key: String) {
        val current = activeProfile.value
        val updatedMap = current.getExtensibleAttributesMap().toMutableMap()
        updatedMap.remove(key)
        viewModelScope.launch {
            repository.updateProfile(
                current.copy(
                    extensibleAttributesJson = UserProfileEntity.encodeExtensibleAttributes(updatedMap)
                )
            )
        }
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(current.copy(themeMode = themeMode.name))
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(current.copy(useDynamicColor = enabled))
        }
    }

    fun updatePreferredLanguage(language: AppLanguage) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(current.copy(preferredLanguage = language.name))
            _snackbarMessage.value = "Assistant language set to ${language.displayName}"
        }
    }

    fun updateTextSize(option: TextSizeOption) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(current.copy(textSize = option.name))
        }
    }

    fun updateVoiceSettings(
        voiceEnabled: Boolean = activeProfile.value.voiceEnabled,
        autoSpeak: Boolean = activeProfile.value.autoSpeakResponses,
        rate: Float = activeProfile.value.speechRate,
        pitch: Float = activeProfile.value.speechPitch
    ) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(
                current.copy(
                    voiceEnabled = voiceEnabled,
                    autoSpeakResponses = autoSpeak,
                    speechRate = rate.coerceIn(0.5f, 2.0f),
                    speechPitch = pitch.coerceIn(0.5f, 2.0f)
                )
            )
        }
    }

    fun updatePrivacySettings(
        saveHistory: Boolean = activeProfile.value.saveHistoryEnabled,
        maskSensitiveData: Boolean = activeProfile.value.privacyMaskSensitiveData,
        defaultWebSearch: Boolean = activeProfile.value.defaultWebSearch
    ) {
        val current = activeProfile.value
        viewModelScope.launch {
            repository.updateProfile(
                current.copy(
                    saveHistoryEnabled = saveHistory,
                    privacyMaskSensitiveData = maskSensitiveData,
                    defaultWebSearch = defaultWebSearch
                )
            )
            _webSearchEnabled.value = defaultWebSearch
        }
    }

    // --- API Verification ---

    fun verifyApiConnection() {
        if (_apiDiagnostics.value.isTestingConnection) return
        viewModelScope.launch {
            _apiDiagnostics.update {
                it.copy(
                    isTestingConnection = true,
                    isKeyConfigured = ApiConfig.isApiKeyConfigured(),
                    lastPingStatus = "Testing live connection to Gemini API…"
                )
            }
            val start = System.currentTimeMillis()
            val (ok, statusText) = repository.verifyBackendConnection()
            val latency = if (ok) System.currentTimeMillis() - start else null
            _apiDiagnostics.update {
                it.copy(
                    isTestingConnection = false,
                    isKeyConfigured = ApiConfig.isApiKeyConfigured(),
                    requestsInLastMinute = ApiConfig.getCurrentWindowRequestCount(),
                    lastPingLatencyMs = latency,
                    lastPingStatus = statusText
                )
            }
        }
    }

    private fun buildSmartTitle(
        prompt: String,
        attachment: PendingAttachment?,
        mathCategory: MathCategory?
    ): String {
        if (mathCategory != null) {
            val snippet = prompt.take(38).ifBlank { mathCategory.title }
            return "${mathCategory.symbolBadge} $snippet"
        }
        if (prompt.isNotBlank()) {
            val firstLine = prompt.lines().first().trim()
            return if (firstLine.length <= 44) firstLine else "${firstLine.take(42)}…"
        }
        if (attachment != null) {
            return if (attachment.isImage) "📷 Vision: ${attachment.fileName}"
            else "📄 Doc: ${attachment.fileName}"
        }
        return "New Conversation"
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.shutdown()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = SutraDatabase.getInstance(context)
            val repository = SutraRepository(
                userProfileDao = db.userProfileDao(),
                conversationDao = db.conversationDao(),
                chatMessageDao = db.chatMessageDao()
            )
            val voiceManager = VoiceManager(context)
            return SutraViewModel(repository, voiceManager) as T
        }
    }
}
