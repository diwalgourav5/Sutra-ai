package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AvatarPreset
import com.example.data.model.UserProfileEntity
import com.example.data.model.WebSource
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyWebBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileAvatarBadge(
    profile: UserProfileEntity,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val preset = profile.parsedAvatarPreset
    val customUri = profile.customAvatarUri
    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), CircleShape)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Box(
        modifier = baseModifier.background( Color(preset.primaryHex).copy(alpha = 0.22f) ),
        contentAlignment = Alignment.Center
    ) {
        when {
            preset == AvatarPreset.CUSTOM_PHOTO && !customUri.isNullOrBlank() -> {
                AsyncImage(
                    model = customUri,
                    contentDescription = "${profile.username} profile avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            preset.usesGeneratedAsset -> {
                Image(
                    painter = painterResource(id = R.drawable.img_profile_avatar),
                    contentDescription = "${profile.username} avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = preset.emojiBadge,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }
    }
}

@Composable
fun SutraBrandAvatar(size: Dp = 32.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_app_icon),
            contentDescription = "Sutra AI Assistant Icon",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun Base64ThumbnailImage(
    base64Data: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(base64Data) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

fun formatMessageTimestamp(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}

fun formatConversationDate(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}

private sealed class MarkdownSegment {
    data class TextParagraph(val text: String) : MarkdownSegment()
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment()
}

private fun parseMarkdownSegments(raw: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val regex = Regex("```([a-zA-Z0-9_+\\-]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val preceding = raw.substring(lastIndex, match.range.first).trim('\n')
            if (preceding.isNotBlank()) {
                segments.add(MarkdownSegment.TextParagraph(preceding))
            }
        }
        val lang = match.groupValues[1].trim().ifEmpty { "code / math" }
        val code = match.groupValues[2].trimEnd()
        segments.add(MarkdownSegment.CodeBlock(language = lang, code = code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim('\n')
        if (tail.isNotBlank()) {
            segments.add(MarkdownSegment.TextParagraph(tail))
        }
    }
    if (segments.isEmpty() && raw.isNotBlank()) {
        segments.add(MarkdownSegment.TextParagraph(raw))
    }
    return segments
}

@Composable
fun FormattedMarkdownContent(
    content: String,
    textColor: Color,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(content) { parseMarkdownSegments(content) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { segment ->
                when (segment) {
                    is MarkdownSegment.CodeBlock -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0A0E17),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF151C2C))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = segment.language.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onCopyCode(segment.code) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy code block",
                                            tint = Color(0xFF2DD4BF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2DD4BF)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = segment.code,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownSegment.TextParagraph -> {
                        val lines = segment.text.lines()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            lines.forEach { rawLine ->
                                val line = rawLine.trimEnd()
                                when {
                                    line.isBlank() -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    line.startsWith("### ") -> {
                                        Text(
                                            text = formatInlineMarkdown(
                                                line.removePrefix("### "),
                                                primaryColor,
                                                secondaryColor
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    line.startsWith("## ") -> {
                                        Text(
                                            text = formatInlineMarkdown(
                                                line.removePrefix("## "),
                                                primaryColor,
                                                secondaryColor
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    line.startsWith("# ") -> {
                                        Text(
                                            text = formatInlineMarkdown(
                                                line.removePrefix("# "),
                                                primaryColor,
                                                secondaryColor
                                            ),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    line == "---" || line == "***" -> {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = formatInlineMarkdown(
                                                line,
                                                primaryColor,
                                                secondaryColor
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatInlineMarkdown(
    text: String,
    primaryColor: Color,
    secondaryColor: Color
) = buildAnnotatedString {
    var i = 0
    val n = text.length
    while (i < n) {
        when {
            text.startsWith("**", i) -> {
                val closeIdx = text.indexOf("**", i + 2)
                if (closeIdx != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor)) {
                        append(text.substring(i + 2, closeIdx))
                    }
                    i = closeIdx + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val closeIdx = text.indexOf('`', i + 1)
                if (closeIdx != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    ) {
                        append(text.substring(i + 1, closeIdx))
                    }
                    i = closeIdx + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebSourcesStrip(
    usedWebSearch: Boolean,
    sources: List<WebSource>,
    modifier: Modifier = Modifier
) {
    if (!usedWebSearch && sources.isEmpty()) return
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SkyWebBadge.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyWebBadge.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Web Search Grounding",
                    tint = SkyWebBadge,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (sources.isNotEmpty()) {
                        "Web Grounded Response • ${sources.size} Verified Source(s)"
                    } else {
                        "Web Search Mode Active • Distinguished from general training knowledge"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyWebBadge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sources.forEach { source ->
                        AssistChip(
                            onClick = {
                                runCatching { uriHandler.openUri(source.uri) }
                            },
                            label = {
                                Text(
                                    text = source.title.take(32),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open source link",
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInteractionBottomSheet(
    isSpeaking: Boolean,
    languageName: String,
    onStartSpeechRecognition: () -> Unit,
    onStopSpeaking: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking) 1.22f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sutra Voice Assistant",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active Language: $languageName • Hands-free Speech-to-Text & Read Aloud",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onStartSpeechRecognition() }
                    .testTag("voice_modal_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Tap to speak",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSpeaking) {
                    "Sutra AI is reading the response aloud…"
                } else {
                    "Tap the microphone orb to dictate your question in English, Hindi, or Hinglish"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartSpeechRecognition,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("start_voice_stt_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speak Now")
                }

                if (isSpeaking) {
                    FilledTonalButton(
                        onClick = onStopSpeaking,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("stop_tts_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Audio")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Done")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
