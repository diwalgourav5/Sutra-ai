package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.ChatMessageEntity
import com.example.data.model.PendingAttachment
import com.example.data.model.UserProfileEntity
import com.example.ui.components.Base64ThumbnailImage
import com.example.ui.components.FormattedMarkdownContent
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.SutraBrandAvatar
import com.example.ui.components.WebSourcesStrip
import com.example.ui.components.formatMessageTimestamp
import com.example.ui.theme.SkyWebBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    profile: UserProfileEntity,
    conversationTitle: String,
    messages: List<ChatMessageEntity>,
    composerText: String,
    activeMode: AssistantMode,
    webSearchEnabled: Boolean,
    pendingAttachment: PendingAttachment?,
    isGenerating: Boolean,
    streamingPartialText: String,
    streamingModelName: String,
    isSpeaking: Boolean,
    speakingMessageId: Long?,
    onOpenDrawer: () -> Unit,
    onStartNewChat: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMathSolver: () -> Unit,
    onUpdateComposerText: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onStopGenerating: () -> Unit,
    onSelectAssistantMode: (AssistantMode) -> Unit,
    onToggleWebSearch: () -> Unit,
    onCycleLanguage: (AppLanguage) -> Unit,
    onPickGalleryImage: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onPickDocumentFile: () -> Unit,
    onClearAttachment: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onOpenVoiceOverlay: () -> Unit,
    onSpeakMessage: (ChatMessageEntity) -> Unit,
    onRegenerateMessage: (ChatMessageEntity) -> Unit,
    onEditUserMessage: (ChatMessageEntity, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showAttachmentSheet by rememberSaveable { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editedMessageText by rememberSaveable { mutableStateOf("") }

    // Automatically scroll to bottom when new message or streaming text arrives
    LaunchedEffect(messages.size, streamingPartialText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            messages.size > 3 &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != messages.size - 1
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top Header Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_history_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open conversation history drawer"
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = conversationTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeMode.shortTitle} • ${profile.parsedLanguage.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Language Quick Cycle Pill (EN -> हिं -> HI-EN)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val next = when (profile.parsedLanguage) {
                                    AppLanguage.ENGLISH -> AppLanguage.HINDI
                                    AppLanguage.HINDI -> AppLanguage.HINGLISH
                                    AppLanguage.HINGLISH -> AppLanguage.ENGLISH
                                }
                                onCycleLanguage(next)
                            }
                            .testTag("language_cycle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Switch language",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = profile.parsedLanguage.nativeBadge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // New Chat Button
                    IconButton(
                        onClick = onStartNewChat,
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Start New Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // User Profile Avatar Badge
                    ProfileAvatarBadge(
                        profile = profile,
                        size = 36.dp,
                        modifier = Modifier.testTag("top_profile_avatar_button"),
                        onClick = onOpenProfile
                    )
                }

                // Assistant Mode Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistantMode.entries.forEach { mode ->
                        val selected = mode == activeMode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAssistantMode(mode) },
                            label = {
                                Text(
                                    text = mode.shortTitle,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                val icon = when (mode) {
                                    AssistantMode.GENERAL -> Icons.Default.AutoAwesome
                                    AssistantMode.STEP_BY_STEP_TUTOR -> Icons.Default.School
                                    AssistantMode.MATH_SOLVER -> Icons.Default.Calculate
                                    AssistantMode.CODE_ARCHITECT -> Icons.Default.Code
                                    AssistantMode.WRITER_TRANSLATOR -> Icons.Default.Translate
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // 2. Main Chat Area (Empty State OR Message List)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyChatWelcomeView(
                    profile = profile,
                    onSelectPrompt = { promptText ->
                        onSendMessage(promptText)
                    },
                    onOpenMathSolver = onOpenMathSolver,
                    onPickImage = onPickGalleryImage,
                    onOpenProfile = onOpenProfile
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list")
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        val isLastAiMessage = !message.isUser && message.id == messages.lastOrNull()?.id
                        val displayedContent = if (isLastAiMessage && isGenerating && streamingPartialText.isNotEmpty()) {
                            streamingPartialText
                        } else {
                            message.content
                        }
                        val displayedModel = if (isLastAiMessage && isGenerating) {
                            streamingModelName
                        } else {
                            message.modelUsed
                        }

                        ChatMessageItemCard(
                            message = message.copy(
                                content = displayedContent,
                                modelUsed = displayedModel
                            ),
                            profile = profile,
                            isStreamingThisMessage = isLastAiMessage && isGenerating,
                            isSpeakingThisMessage = isSpeaking && speakingMessageId == message.id,
                            onCopy = { textToCopy ->
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                onShowToast("Copied to clipboard")
                            },
                            onRegenerate = { onRegenerateMessage(message) },
                            onEditUserMessage = {
                                editingMessage = message
                                editedMessageText = message.content
                            },
                            onSpeakToggle = { onSpeakMessage(message.copy(content = displayedContent)) }
                        )
                    }
                }

                // Floating Scroll-to-Bottom Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            }
        }

        // 3. Bottom Message Composer
        BottomMessageComposer(
            composerText = composerText,
            pendingAttachment = pendingAttachment,
            webSearchEnabled = webSearchEnabled,
            voiceEnabled = profile.voiceEnabled,
            isGenerating = isGenerating,
            language = profile.parsedLanguage,
            onUpdateText = onUpdateComposerText,
            onSend = { onSendMessage(null) },
            onStop = onStopGenerating,
            onOpenAttachmentMenu = { showAttachmentSheet = true },
            onClearAttachment = onClearAttachment,
            onToggleWebSearch = onToggleWebSearch,
            onTakeCameraPhoto = onTakeCameraPhoto,
            onStartVoiceInput = onStartVoiceInput,
            onOpenVoiceOverlay = onOpenVoiceOverlay
        )
    }

    // Attachment Picker Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Attach Image or Study Document",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analyze handwritten math, diagrams, screenshots, PDFs, or source code files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AttachmentOptionCard(
                    icon = Icons.Default.Image,
                    title = "Photo Gallery",
                    subtitle = "Upload photos of questions, textbooks, diagrams, or charts",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        showAttachmentSheet = false
                        onPickGalleryImage()
                    }
                )

                AttachmentOptionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Take Photo with Camera",
                    subtitle = "Snap a picture of an equation, homework page, or whiteboard",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        showAttachmentSheet = false
                        onTakeCameraPhoto()
                    }
                )

                AttachmentOptionCard(
                    icon = Icons.Default.Description,
                    title = "Upload Document / Code / PDF",
                    subtitle = "Supports PDF, TXT, Markdown, CSV, JSON, Kotlin, Python & JS (up to 10 MB)",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        showAttachmentSheet = false
                        onPickDocumentFile()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Edit User Message Dialog
    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Your Message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Updating this message will regenerate the assistant's response from this point in the conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editedMessageText,
                        onValueChange = { editedMessageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_message_text_field"),
                        minLines = 3,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingMessage
                        if (target != null && editedMessageText.isNotBlank()) {
                            onEditUserMessage(target, editedMessageText)
                        }
                        editingMessage = null
                    },
                    modifier = Modifier.testTag("confirm_edit_message_button")
                ) {
                    Text("Save & Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyChatWelcomeView(
    profile: UserProfileEntity,
    onSelectPrompt: (String) -> Unit,
    onOpenMathSolver: () -> Unit,
    onPickImage: () -> Unit,
    onOpenProfile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Banner Card with generated art and Personalized User Greeting
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sutra_hero),
                        contentDescription = "Sutra AI Hero Illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x660B0F17),
                                        Color(0xE60B0F17)
                                    )
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { onOpenProfile() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                ProfileAvatarBadge(profile = profile, size = 22.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${profile.username} • ${profile.roleOrFocus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (profile.parsedLanguage) {
                                    AppLanguage.HINDI -> "नमस्ते, ${profile.username.substringBefore(' ')}! आज हम क्या सीखेंगे?"
                                    AppLanguage.HINGLISH -> "Namaste, ${profile.username.substringBefore(' ')}! Aaj kis topic ya problem ko solve karein?"
                                    AppLanguage.ENGLISH -> "Hello, ${profile.username.substringBefore(' ')}! How can Sutra AI help you today?"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step-by-step reasoning • Math & Logic • Vision • English, हिन्दी & Hinglish",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Feature Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenMathSolver() }
                        .testTag("quick_math_solver_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Open Math Solver",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Math Solver",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Algebra, Calculus, Matrices & Word Problems",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onPickImage() }
                        .testTag("quick_vision_upload_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Analyze Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Vision & Study Scan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyze photos, diagrams, charts & notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Curated Multilingual & Multi-Domain Starter Prompts
        item {
            Text(
                text = "Explore Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        val starterPrompts = listOf(
            Triple(
                "📐 Step-by-Step Calculus & Algebra",
                "Evaluate ∫ x · e^(2x) dx using integration by parts and explain each step clearly.",
                "Mathematics"
            ),
            Triple(
                "🇮🇳 Hinglish Concept Tutor",
                "Quantum Entanglement aur Superposition ko easy Hinglish mein real-life analogy ke saath step-by-step samjhao.",
                "Hinglish • Study"
            ),
            Triple(
                "💻 Code & Algorithm Debugger",
                "Write a thread-safe LRU Cache in Kotlin with O(1) get and put operations, and explain how the doubly linked list + HashMap work together.",
                "Programming"
            ),
            Triple(
                "🗣️ हिन्दी अनुवाद और सारांश (Hindi)",
                "कृत्रिम बुद्धिमत्ता (AI) और मशीन लर्निंग के बीच मुख्य अंतर को सरल हिन्दी में उदाहरण सहित समझाइए।",
                "Hindi • Education"
            ),
            Triple(
                "✍️ Essay, Rewrite & Idea Generator",
                "Generate 5 innovative final-year computer science project ideas combining mobile sensors and sustainability, with tech stack and impact.",
                "Ideas & Writing"
            )
        )

        items(starterPrompts) { (title, prompt, badge) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectPrompt(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItemCard(
    message: ChatMessageEntity,
    profile: UserProfileEntity,
    isStreamingThisMessage: Boolean,
    isSpeakingThisMessage: Boolean,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    onEditUserMessage: () -> Unit,
    onSpeakToggle: () -> Unit
) {
    val isUser = message.isUser
    val webSources = remember(message.webSourcesJson) { message.parseWebSources() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Header Row: Avatar + Sender Name + Timestamp + Model Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            if (!isUser) {
                SutraBrandAvatar(size = 24.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sutra AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = message.modelUsed.removePrefix("gemini-"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatMessageTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = formatMessageTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                ProfileAvatarBadge(profile = profile, size = 24.dp)
            }
        }

        // Message Bubble / Card
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (message.isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            },
            border = if (!isUser) {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            } else null,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Attached Image or Document Preview
                if (!message.attachmentName.isNullOrBlank()) {
                    if (!message.attachmentBase64.isNullOrBlank() &&
                        message.attachmentMimeType?.startsWith("image/") == true
                    ) {
                        Base64ThumbnailImage(
                            base64Data = message.attachmentBase64,
                            contentDescription = message.attachmentName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.attachmentName,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Message Content
                val contentTextColor = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (message.isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                FormattedMarkdownContent(
                    content = message.content,
                    textColor = contentTextColor,
                    onCopyCode = onCopy
                )

                if (isStreamingThisMessage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Streaming response…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Web Search Grounding Sources Strip
                if (!isUser && (message.usedWebSearch || webSources.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WebSourcesStrip(
                        usedWebSearch = message.usedWebSearch,
                        sources = webSources
                    )
                }

                // Action Bar (Copy, Edit, Regenerate, TTS)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUser) {
                        MessageActionChip(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            testTag = "edit_user_msg_${message.id}",
                            onClick = onEditUserMessage
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MessageActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            testTag = "copy_user_msg_${message.id}",
                            onClick = { onCopy(message.content) }
                        )
                    } else if (!isStreamingThisMessage) {
                        MessageActionChip(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            testTag = "copy_ai_msg_${message.id}",
                            onClick = { onCopy(message.content) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MessageActionChip(
                            icon = Icons.Default.Refresh,
                            label = "Regenerate",
                            testTag = "regenerate_ai_msg_${message.id}",
                            onClick = onRegenerate
                        )
                        if (profile.voiceEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            MessageActionChip(
                                icon = if (isSpeakingThisMessage) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                label = if (isSpeakingThisMessage) "Stop Audio" else "Listen",
                                testTag = "speak_ai_msg_${message.id}",
                                onClick = onSpeakToggle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomMessageComposer(
    composerText: String,
    pendingAttachment: PendingAttachment?,
    webSearchEnabled: Boolean,
    voiceEnabled: Boolean,
    isGenerating: Boolean,
    language: AppLanguage,
    onUpdateText: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenAttachmentMenu: () -> Unit,
    onClearAttachment: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onOpenVoiceOverlay: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 1. Staged Attachment Preview & Vision Quick Actions
            AnimatedVisibility(visible = pendingAttachment != null) {
                pendingAttachment?.let { att ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (att.isImage && !att.base64Data.isNullOrBlank()) {
                                        Base64ThumbnailImage(
                                            base64Data = att.base64Data,
                                            contentDescription = att.fileName,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.fileName,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (att.uploadProgress < 1f) {
                                                "Processing attachment (${(att.uploadProgress * 100).toInt()}%)…"
                                            } else {
                                                "${att.mimeType} • Ready for AI analysis"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = onClearAttachment) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove attachment"
                                        )
                                    }
                                }

                                if (att.uploadProgress < 1f) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { att.uploadProgress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Vision / Document Quick Prompts
                                if (att.isImage) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val visionPrompts = listOf(
                                            "Solve questions step-by-step",
                                            "Explain diagram / chart",
                                            "Extract & translate text",
                                            "Summarize study notes"
                                        )
                                        visionPrompts.forEach { preset ->
                                            AssistChip(
                                                onClick = { onUpdateText(preset) },
                                                label = {
                                                    Text(
                                                        text = preset,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Utility Bar: Web Search Toggle + Camera + Live Voice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = webSearchEnabled,
                    onClick = onToggleWebSearch,
                    label = {
                        Text(
                            text = if (webSearchEnabled) "Web Search: ON" else "Web Search",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Toggle Web Search Grounding",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyWebBadge.copy(alpha = 0.2f),
                        selectedLabelColor = SkyWebBadge,
                        selectedLeadingIconColor = SkyWebBadge
                    ),
                    modifier = Modifier.testTag("web_search_toggle_chip")
                )

                AssistChip(
                    onClick = onTakeCameraPhoto,
                    label = {
                        Text("Snap Photo", style = MaterialTheme.typography.labelSmall)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )

                if (voiceEnabled) {
                    AssistChip(
                        onClick = onOpenVoiceOverlay,
                        label = {
                            Text("Voice Mode", style = MaterialTheme.typography.labelSmall)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Interactive Voice Mode",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.testTag("open_voice_overlay_chip")
                    )
                }
            }

            // 3. Main Composer Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onOpenAttachmentMenu,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("attach_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Attach image or file",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = composerText,
                    onValueChange = onUpdateText,
                    placeholder = {
                        Text(
                            text = when (language) {
                                AppLanguage.HINDI -> "अपना प्रश्न पूछें (हिन्दी, English, या गणित)…"
                                AppLanguage.HINGLISH -> "Kuch bhi poochiye (Hinglish, English, Math, Code)…"
                                AppLanguage.ENGLISH -> "Ask anything in English, Hindi, or Hinglish…"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                if (voiceEnabled && composerText.isBlank() && !isGenerating) {
                    IconButton(
                        onClick = onStartVoiceInput,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Dictate with voice",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (isGenerating) {
                    // Stop Generating Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onStop)
                            .testTag("stop_generating_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop generating response",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                } else {
                    val canSend = composerText.isNotBlank() || pendingAttachment != null
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend, onClick = onSend)
                            .testTag("send_message_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (canSend) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
