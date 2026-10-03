package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiDiagnosticsState
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.TextSizeOption
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.ExportFormat
import com.example.util.ExportResult

@Composable
fun SettingsScreen(
    profile: UserProfileEntity,
    diagnostics: ApiDiagnosticsState,
    lastExportResult: ExportResult?,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTextSize: (TextSizeOption) -> Unit,
    onUpdateVoiceSettings: (
        voiceEnabled: Boolean,
        autoSpeak: Boolean,
        rate: Float,
        pitch: Float
    ) -> Unit,
    onTestVoiceOutput: () -> Unit,
    onUpdatePrivacySettings: (
        saveHistory: Boolean,
        maskSensitiveData: Boolean,
        defaultWebSearch: Boolean
    ) -> Unit,
    onOneClickExport: (ExportFormat) -> Unit,
    onSaveBackupToCustomLocation: (ExportFormat) -> Unit,
    onClearAllHistory: () -> Unit,
    onVerifyApiConnection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings & System Status",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize theme, language, typography, voice, privacy & API diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Theme & Appearance
        item {
            SettingsSectionCard(
                icon = Icons.Default.Palette,
                title = "Theme & Appearance"
            ) {
                Text("Color Theme Mode", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.parsedThemeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                        )
                    }
                }

                SettingsToggleRow(
                    title = "Material You Dynamic Color",
                    subtitle = "Adapt palette to your Android 12+ wallpaper colors",
                    checked = profile.useDynamicColor,
                    onCheckedChange = onUpdateDynamicColor
                )
            }
        }

        // 2. Language & Typography Scale
        item {
            SettingsSectionCard(
                icon = Icons.Default.Translate,
                title = "Language & Text Size"
            ) {
                Text(
                    text = "Assistant Response & Speech Language",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = profile.parsedLanguage == lang,
                            onClick = { onUpdateLanguage(lang) },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("settings_lang_${lang.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message Text Size", style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextSizeOption.entries.forEach { sizeOption ->
                        FilterChip(
                            selected = profile.parsedTextSize == sizeOption,
                            onClick = { onUpdateTextSize(sizeOption) },
                            label = { Text(sizeOption.displayName) },
                            modifier = Modifier.testTag("settings_textsize_${sizeOption.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 3. Voice Settings (STT & TTS)
        item {
            SettingsSectionCard(
                icon = Icons.Default.RecordVoiceOver,
                title = "Voice Interaction (STT & TTS)"
            ) {
                SettingsToggleRow(
                    title = "Enable Voice Features",
                    subtitle = "Show microphone button for Speech-to-Text and Read Aloud button on responses",
                    checked = profile.voiceEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateVoiceSettings(
                            enabled,
                            profile.autoSpeakResponses,
                            profile.speechRate,
                            profile.speechPitch
                        )
                    }
                )

                if (profile.voiceEnabled) {
                    SettingsToggleRow(
                        title = "Auto-Speak AI Responses",
                        subtitle = "Automatically read aloud every completed assistant response",
                        checked = profile.autoSpeakResponses,
                        onCheckedChange = { autoSpeak ->
                            onUpdateVoiceSettings(
                                profile.voiceEnabled,
                                autoSpeak,
                                profile.speechRate,
                                profile.speechPitch
                            )
                        }
                    )

                    Column {
                        Text(
                            text = "Speech Rate: ${String.format("%.2fx", profile.speechRate)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = profile.speechRate,
                            onValueChange = { newRate ->
                                onUpdateVoiceSettings(
                                    profile.voiceEnabled,
                                    profile.autoSpeakResponses,
                                    newRate,
                                    profile.speechPitch
                                )
                            },
                            valueRange = 0.6f..1.8f
                        )
                    }

                    Column {
                        Text(
                            text = "Voice Pitch: ${String.format("%.2fx", profile.speechPitch)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = profile.speechPitch,
                            onValueChange = { newPitch ->
                                onUpdateVoiceSettings(
                                    profile.voiceEnabled,
                                    profile.autoSpeakResponses,
                                    profile.speechRate,
                                    newPitch
                                )
                            },
                            valueRange = 0.6f..1.6f
                        )
                    }

                    FilledTonalButton(
                        onClick = onTestVoiceOutput,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Text-to-Speech (${profile.parsedLanguage.displayName})")
                    }
                }
            }
        }

        // 4. Chat History & Privacy
        item {
            SettingsSectionCard(
                icon = Icons.Default.Security,
                title = "Chat History & Privacy Protection"
            ) {
                SettingsToggleRow(
                    title = "Store Chat History Locally",
                    subtitle = "Persist conversations on-device in encrypted-ready Room SQLite database",
                    checked = profile.saveHistoryEnabled,
                    onCheckedChange = { save ->
                        onUpdatePrivacySettings(
                            save,
                            profile.privacyMaskSensitiveData,
                            profile.defaultWebSearch
                        )
                    }
                )

                SettingsToggleRow(
                    title = "Redact Sensitive PII Before Sending",
                    subtitle = "Automatically mask email addresses, phone/card numbers, and secret keys",
                    checked = profile.privacyMaskSensitiveData,
                    onCheckedChange = { mask ->
                        onUpdatePrivacySettings(
                            profile.saveHistoryEnabled,
                            mask,
                            profile.defaultWebSearch
                        )
                    }
                )

                SettingsToggleRow(
                    title = "Enable Web Search Grounding by Default",
                    subtitle = "Distinguish real-time web sources from foundational AI knowledge",
                    checked = profile.defaultWebSearch,
                    onCheckedChange = { web ->
                        onUpdatePrivacySettings(
                            profile.saveHistoryEnabled,
                            profile.privacyMaskSensitiveData,
                            web
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Text(
                    text = "One-Click Local History Backup",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instantly export all conversations & messages to a local JSON or readable Text file for backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOneClickExport(ExportFormat.JSON) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_export_json_button")
                    ) {
                        Text("1-Click JSON")
                    }
                    FilledTonalButton(
                        onClick = { onOneClickExport(ExportFormat.TEXT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_export_txt_button")
                    ) {
                        Text("1-Click TXT")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveBackupToCustomLocation(ExportFormat.JSON) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save JSON As…", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onSaveBackupToCustomLocation(ExportFormat.TEXT) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save TXT As…", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (lastExportResult != null && lastExportResult.success) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Last Backup: ${lastExportResult.fileName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            Text(
                                text = "Saved at: ${lastExportResult.filePathOrUri}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onClearAllHistory,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Stored Conversations")
                }
            }
        }

        // 5. API & Backend Status + Developer Instructions
        item {
            SettingsSectionCard(
                icon = Icons.Default.CloudDone,
                title = "API / Backend Status & Configuration"
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (diagnostics.isKeyConfigured) {
                        EmeraldSuccess.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (diagnostics.isKeyConfigured) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = if (diagnostics.isKeyConfigured) {
                                EmeraldSuccess
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (diagnostics.isKeyConfigured) {
                                    "GEMINI_API_KEY Injected via BuildConfig"
                                } else {
                                    "API Key Placeholder Detected — Configure via Secrets Panel"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = diagnostics.lastPingStatus,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Technical Telemetry Rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    TelemetryRow("Backend Base URL", diagnostics.activeEndpoint)
                    TelemetryRow("Fast Multimodal Model", diagnostics.primaryChatModel)
                    TelemetryRow("Math & Reasoning Model", diagnostics.reasoningMathModel)
                    TelemetryRow("AI Image Model", diagnostics.imageModel)
                    TelemetryRow(
                        "Rate Limiter Window",
                        "${diagnostics.requestsInLastMinute} / ${diagnostics.maxRequestsPerMinute} req/min"
                    )
                }

                Button(
                    onClick = onVerifyApiConnection,
                    enabled = !diagnostics.isTestingConnection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_api_connection_button")
                ) {
                    if (diagnostics.isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Live Gemini API…")
                    } else {
                        Text("Verify Live API Connection")
                    }
                }

                // Developer Configuration & Build Instructions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Developer Credential & Backend Setup Guide",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. AI Studio Secrets Panel: Add `GEMINI_API_KEY` in the AI Studio Secrets UI. The Secrets Gradle Plugin injects `BuildConfig.GEMINI_API_KEY` from `.env` / `.env.example` at compile time without hardcoding secrets in source code.\n" +
                                "2. Custom Backend Proxy: To route traffic through your own backend server in production, update `ApiConfig.BACKEND_BASE_URL` in `data/remote/ApiConfig.kt`.\n" +
                                "3. Build APK: Run `gradle :app:assembleDebug` or export the APK from the AI Studio Settings menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 6. About Sutra AI
        item {
            SettingsSectionCard(
                icon = Icons.Default.Info,
                title = "About Sutra AI"
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sutra AI v1.0 • Original Android Conversational Assistant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Designed for students, developers, and lifelong learners. Supports multi-turn context, step-by-step mathematics across 8 branches, image & document analysis, speech-to-text, text-to-speech, web search grounding, and natural conversation in English, हिन्दी (Hindi), and Hinglish.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
