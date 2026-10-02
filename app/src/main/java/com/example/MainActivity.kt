package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppThemeMode
import com.example.data.model.AvatarPreset
import com.example.data.model.ConversationEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.SutraBrandAvatar
import com.example.ui.components.VoiceInteractionBottomSheet
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MathSolverScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SutraAITheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.SutraViewModel
import com.example.util.ExportFormat
import com.example.util.VoiceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sutraViewModel: SutraViewModel = viewModel(
                factory = SutraViewModel.Factory(context)
            )
            val activeProfile by sutraViewModel.activeProfile.collectAsStateWithLifecycle()

            val isDark = when (activeProfile.parsedThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            SutraAITheme(
                darkTheme = isDark,
                dynamicColor = activeProfile.useDynamicColor,
                textScale = activeProfile.parsedTextSize.scaleFactor
            ) {
                SutraAssistantRootApp(viewModel = sutraViewModel)
            }
        }
    }
}

@Composable
fun SutraAssistantRootApp(viewModel: SutraViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val currentMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeAssistantMode.collectAsStateWithLifecycle()
    val webSearchEnabled by viewModel.webSearchEnabled.collectAsStateWithLifecycle()
    val pendingAttachment by viewModel.pendingAttachment.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val streamingPartialText by viewModel.streamingPartialText.collectAsStateWithLifecycle()
    val streamingModelName by viewModel.streamingModelName.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isVoiceOverlayOpen by viewModel.isVoiceConversationOverlayOpen.collectAsStateWithLifecycle()
    val lastExportResult by viewModel.lastExportResult.collectAsStateWithLifecycle()

    // Math Solver States
    val selectedMathCategory by viewModel.selectedMathCategory.collectAsStateWithLifecycle()
    val mathInputText by viewModel.mathInputText.collectAsStateWithLifecycle()
    val mathAttachment by viewModel.mathAttachment.collectAsStateWithLifecycle()
    val isSolvingMath by viewModel.isSolvingMath.collectAsStateWithLifecycle()
    val mathStreamingText by viewModel.mathStreamingText.collectAsStateWithLifecycle()
    val mathSolutionResult by viewModel.mathSolutionResult.collectAsStateWithLifecycle()

    // API Diagnostics
    val apiDiagnostics by viewModel.apiDiagnostics.collectAsStateWithLifecycle()

    // Voice TTS State
    val isSpeaking by (viewModel.voiceManager?.isSpeaking ?: remember { MutableStateFlow(false) })
        .collectAsStateWithLifecycle()
    val speakingMessageId by (viewModel.voiceManager?.speakingMessageId ?: remember { MutableStateFlow(null) })
        .collectAsStateWithLifecycle()

    // Track whether the photo/camera launch is for MathSolver or Chat
    var mediaTargetForMath by rememberSaveable { mutableStateOf(false) }

    // 1. Zero-Permission Photo Picker for Chat / Math Solver Image Understanding
    val galleryImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFromUri(context, uri, forMathSolver = mediaTargetForMath)
        }
    }

    // 2. Zero-Permission Photo Picker for Custom User Profile Avatar
    val avatarImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.updateProfileDetails(
                username = activeProfile.username,
                roleOrFocus = activeProfile.roleOrFocus,
                bioOrContext = activeProfile.bioOrContext,
                avatarPreset = AvatarPreset.CUSTOM_PHOTO,
                customAvatarUri = uri.toString()
            )
        }
    }

    // 3. Camera Capture Launcher + Runtime Permission
    val cameraTakePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.attachCameraBitmap(bitmap, forMathSolver = mediaTargetForMath)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { cameraTakePictureLauncher.launch(null) }
                .onFailure { viewModel.showMessage("Camera unavailable on this device.") }
        } else {
            viewModel.showMessage("Camera permission is required to snap photos.")
        }
    }

    // 4. Document / File Picker (PDF, TXT, Code, JSON, CSV, Images)
    val documentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachFromUri(context, uri, forMathSolver = false)
        }
    }

    // 5. Custom Save-As Export Launcher (JSON or TXT)
    var pendingSafExportFormat by remember { mutableStateOf(ExportFormat.JSON) }
    val createBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConversationHistory(
                context = context,
                format = pendingSafExportFormat,
                targetUri = uri
            )
        }
    }

    // 6. Speech-to-Text Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) {
                val combined = if (composerText.isBlank()) recognized else "$composerText $recognized"
                viewModel.updateComposerText(combined)
                if (isVoiceOverlayOpen) {
                    viewModel.sendMessage(recognized)
                }
            }
        }
    }

    val launchSpeechToText = {
        try {
            val intent = VoiceManager.createSpeechRecognizerIntent(activeProfile.parsedLanguage)
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            viewModel.showMessage("Speech recognition service is not available on this emulator/device.")
        }
    }

    // Show Snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // BackHandler on secondary screens to return to CHAT
    BackHandler(enabled = destination != AppDestination.CHAT) {
        viewModel.navigateTo(AppDestination.CHAT)
    }

    val activeConversationTitle = remember(conversations, currentConversationId) {
        conversations.find { it.id == currentConversationId }?.title ?: "Sutra AI"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SutraNavigationDrawerSheet(
                profile = activeProfile,
                conversations = conversations.take(20),
                activeConversationId = currentConversationId,
                onStartNewChat = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.startNewChat()
                },
                onSelectConversation = { id ->
                    coroutineScope.launch { drawerState.close() }
                    viewModel.selectConversation(id)
                },
                onOpenAllHistory = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.navigateTo(AppDestination.HISTORY)
                },
                onOpenProfile = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.navigateTo(AppDestination.PROFILE)
                },
                onQuickExportJson = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.exportConversationHistory(context, ExportFormat.JSON)
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                SutraBottomNavigationBar(
                    currentDestination = destination,
                    onSelectDestination = { dest -> viewModel.navigateTo(dest) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (destination) {
                    AppDestination.CHAT -> {
                        ChatScreen(
                            profile = activeProfile,
                            conversationTitle = activeConversationTitle,
                            messages = currentMessages,
                            composerText = composerText,
                            activeMode = activeMode,
                            webSearchEnabled = webSearchEnabled,
                            pendingAttachment = pendingAttachment,
                            isGenerating = isGenerating,
                            streamingPartialText = streamingPartialText,
                            streamingModelName = streamingModelName,
                            isSpeaking = isSpeaking,
                            speakingMessageId = speakingMessageId,
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onStartNewChat = { viewModel.startNewChat() },
                            onOpenProfile = { viewModel.navigateTo(AppDestination.PROFILE) },
                            onOpenMathSolver = { viewModel.navigateTo(AppDestination.MATH_SOLVER) },
                            onUpdateComposerText = viewModel::updateComposerText,
                            onSendMessage = { overridePrompt ->
                                viewModel.sendMessage(overridePrompt = overridePrompt)
                            },
                            onStopGenerating = viewModel::stopGenerating,
                            onSelectAssistantMode = viewModel::setAssistantMode,
                            onToggleWebSearch = viewModel::toggleWebSearch,
                            onCycleLanguage = viewModel::updatePreferredLanguage,
                            onPickGalleryImage = {
                                mediaTargetForMath = false
                                galleryImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onTakeCameraPhoto = {
                                mediaTargetForMath = false
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onPickDocumentFile = {
                                documentFilePicker.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/*",
                                        "application/json",
                                        "image/*"
                                    )
                                )
                            },
                            onClearAttachment = { viewModel.clearPendingAttachment(forMathSolver = false) },
                            onStartVoiceInput = launchSpeechToText,
                            onOpenVoiceOverlay = { viewModel.setVoiceConversationOverlayOpen(true) },
                            onSpeakMessage = { msg ->
                                viewModel.voiceManager?.speakText(
                                    text = msg.content,
                                    messageId = msg.id,
                                    language = activeProfile.parsedLanguage,
                                    rate = activeProfile.speechRate,
                                    pitch = activeProfile.speechPitch
                                )
                            },
                            onRegenerateMessage = viewModel::regenerateResponse,
                            onEditUserMessage = viewModel::editUserMessage,
                            onShowToast = viewModel::showMessage
                        )
                    }

                    AppDestination.MATH_SOLVER -> {
                        MathSolverScreen(
                            profile = activeProfile,
                            selectedCategory = selectedMathCategory,
                            mathInputText = mathInputText,
                            mathAttachment = mathAttachment,
                            isSolving = isSolvingMath,
                            streamingText = mathStreamingText,
                            solutionResult = mathSolutionResult,
                            onSelectCategory = viewModel::selectMathCategory,
                            onUpdateInput = viewModel::updateMathInput,
                            onInsertSymbol = viewModel::insertMathSymbol,
                            onPickMathImage = {
                                mediaTargetForMath = true
                                galleryImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onTakeMathCamera = {
                                mediaTargetForMath = true
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onClearAttachment = { viewModel.clearPendingAttachment(forMathSolver = true) },
                            onSolveProblem = viewModel::solveDedicatedMathProblem,
                            onStopSolving = viewModel::stopGenerating,
                            onContinueInChat = viewModel::continueMathSolutionInChat,
                            onSpeakSolution = { solutionText ->
                                viewModel.voiceManager?.speakText(
                                    text = solutionText,
                                    messageId = -99L,
                                    language = activeProfile.parsedLanguage,
                                    rate = activeProfile.speechRate,
                                    pitch = activeProfile.speechPitch
                                )
                            },
                            onShowToast = viewModel::showMessage
                        )
                    }

                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            conversations = conversations,
                            activeConversationId = currentConversationId,
                            searchQuery = historySearchQuery,
                            lastExportResult = lastExportResult,
                            onUpdateSearchQuery = viewModel::updateHistorySearchQuery,
                            onSelectConversation = viewModel::selectConversation,
                            onStartNewChat = { viewModel.startNewChat() },
                            onRenameConversation = viewModel::renameConversation,
                            onTogglePin = viewModel::togglePinConversation,
                            onDeleteConversation = viewModel::deleteConversation,
                            onClearAllHistory = viewModel::clearAllConversations,
                            onOneClickExport = { format ->
                                viewModel.exportConversationHistory(context, format)
                            },
                            onDismissExportBanner = viewModel::clearLastExportResult,
                            onShowToast = viewModel::showMessage
                        )
                    }

                    AppDestination.PROFILE -> {
                        ProfileScreen(
                            activeProfile = activeProfile,
                            allProfiles = allProfiles,
                            onSaveProfileDetails = viewModel::updateProfileDetails,
                            onCreateNewProfile = viewModel::createNewUserProfile,
                            onSwitchProfile = viewModel::switchUserProfile,
                            onDeleteProfile = viewModel::deleteUserProfile,
                            onPickCustomAvatarPhoto = {
                                avatarImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onUpdateTheme = viewModel::updateThemeMode,
                            onUpdateLanguage = viewModel::updatePreferredLanguage,
                            onUpdateTextSize = viewModel::updateTextSize,
                            onUpsertExtensibleAttribute = viewModel::upsertProfileExtensibleAttribute,
                            onRemoveExtensibleAttribute = viewModel::removeProfileExtensibleAttribute
                        )
                    }

                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            profile = activeProfile,
                            diagnostics = apiDiagnostics,
                            lastExportResult = lastExportResult,
                            onUpdateTheme = viewModel::updateThemeMode,
                            onUpdateDynamicColor = viewModel::updateDynamicColor,
                            onUpdateLanguage = viewModel::updatePreferredLanguage,
                            onUpdateTextSize = viewModel::updateTextSize,
                            onUpdateVoiceSettings = viewModel::updateVoiceSettings,
                            onTestVoiceOutput = {
                                val sample = when (activeProfile.parsedLanguage) {
                                    com.example.data.model.AppLanguage.HINDI ->
                                        "नमस्ते ${activeProfile.username}, सूत्र एआई आपकी सहायता के लिए तैयार है।"
                                    com.example.data.model.AppLanguage.HINGLISH ->
                                        "Namaste ${activeProfile.username}, Sutra AI aapke sawaalon ka jawab dene ke liye taiyaar hai."
                                    com.example.data.model.AppLanguage.ENGLISH ->
                                        "Hello ${activeProfile.username}, Sutra AI voice output is ready."
                                }
                                viewModel.voiceManager?.speakText(
                                    text = sample,
                                    messageId = -1L,
                                    language = activeProfile.parsedLanguage,
                                    rate = activeProfile.speechRate,
                                    pitch = activeProfile.speechPitch
                                )
                            },
                            onUpdatePrivacySettings = viewModel::updatePrivacySettings,
                            onOneClickExport = { format ->
                                viewModel.exportConversationHistory(context, format)
                            },
                            onSaveBackupToCustomLocation = { format ->
                                pendingSafExportFormat = format
                                val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                createBackupDocumentLauncher.launch(
                                    "sutra_ai_backup_$ts.${format.extension}"
                                )
                            },
                            onClearAllHistory = viewModel::clearAllConversations,
                            onVerifyApiConnection = viewModel::verifyApiConnection
                        )
                    }
                }
            }
        }
    }

    if (isVoiceOverlayOpen) {
        VoiceInteractionBottomSheet(
            isSpeaking = isSpeaking,
            languageName = activeProfile.parsedLanguage.displayName,
            onStartSpeechRecognition = launchSpeechToText,
            onStopSpeaking = { viewModel.voiceManager?.stopSpeaking() },
            onDismiss = { viewModel.setVoiceConversationOverlayOpen(false) }
        )
    }
}

@Composable
private fun SutraNavigationDrawerSheet(
    profile: UserProfileEntity,
    conversations: List<ConversationEntity>,
    activeConversationId: Long?,
    onStartNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onOpenAllHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onQuickExportJson: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Brand + Active Profile Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SutraBrandAvatar(size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sutra AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Conversational & Math Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active User Profile Card in Drawer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenProfile)
                    .testTag("drawer_profile_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatarBadge(profile = profile, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${profile.roleOrFocus} • ${profile.parsedLanguage.nativeBadge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("drawer_new_chat_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Conversation", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Conversations",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Manage All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenAllHistory)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(conversations, key = { it.id }) { convo ->
                    val isSelected = convo.id == activeConversationId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectConversation(convo.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (convo.isPinned) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = convo.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onQuickExportJson,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_quick_export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("1-Click Backup History")
            }
        }
    }
}

@Composable
private fun SutraBottomNavigationBar(
    currentDestination: AppDestination,
    onSelectDestination: (AppDestination) -> Unit
) {
    NavigationBar {
        AppDestination.entries.forEach { dest ->
            val selected = dest == currentDestination
            val (selectedIcon, unselectedIcon) = when (dest) {
                AppDestination.CHAT -> Icons.AutoMirrored.Filled.Chat to Icons.AutoMirrored.Outlined.Chat
                AppDestination.MATH_SOLVER -> Icons.Default.Calculate to Icons.Outlined.Calculate
                AppDestination.HISTORY -> Icons.Default.History to Icons.Outlined.History
                AppDestination.PROFILE -> Icons.Default.Person to Icons.Outlined.Person
                AppDestination.SETTINGS -> Icons.Default.Settings to Icons.Outlined.Settings
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("nav_tab_${dest.route}")
            )
        }
    }
}
