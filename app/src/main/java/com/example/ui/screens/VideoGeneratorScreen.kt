package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.GeneratedVideoEntity
import com.example.data.model.PendingAttachment
import com.example.data.model.VideoAspectRatioOption
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoGenerationProgress
import com.example.data.model.VideoJobStatus
import com.example.data.model.VideoQualityOption
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SAMPLE_TEXT_TO_VIDEO_PROMPTS = listOf(
    "Create a cinematic realistic shot of a sports car driving through a mountain road during sunset, realistic lighting, smooth camera movement.",
    "Aerial drone shot gliding over ancient Himalayan temples at golden sunrise with mist rising through pine valleys, 4K photorealistic.",
    "Macro slow-motion shot of glowing bioluminescent raindrops falling onto a lotus leaf in a tranquil night garden, shallow depth of field.",
    "Futuristic high-speed train arriving at a neon-lit eco-station in Mumbai during monsoon rain, reflections on wet pavement, cinematic tracking shot."
)

private val SAMPLE_IMAGE_TO_VIDEO_PROMPTS = listOf(
    "Animate the clouds and trees with gentle wind and create a slow cinematic camera movement.",
    "Add subtle golden sunlight rays shifting across the scene with a smooth dolly-in camera motion.",
    "Bring the water ripples and atmospheric mist to life with natural physics and gentle parallax movement."
)

@Composable
fun VideoGeneratorScreen(
    promptText: String,
    selectedDuration: VideoDurationOption,
    selectedAspectRatio: VideoAspectRatioOption,
    selectedQuality: VideoQualityOption,
    sourceImage: PendingAttachment?,
    progress: VideoGenerationProgress,
    activePlayingVideo: GeneratedVideoEntity?,
    errorBanner: Pair<String, String>?,
    myVideos: List<GeneratedVideoEntity>,
    isApiKeyConfigured: Boolean,
    onUpdatePrompt: (String) -> Unit,
    onSelectDuration: (VideoDurationOption) -> Unit,
    onSelectAspectRatio: (VideoAspectRatioOption) -> Unit,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onPickSourceImage: () -> Unit,
    onTakeSourceCameraPhoto: () -> Unit,
    onClearSourceImage: () -> Unit,
    onGenerateVideo: () -> Unit,
    onCancelGeneration: () -> Unit,
    onRetryGeneration: (GeneratedVideoEntity?) -> Unit,
    onDismissErrorBanner: () -> Unit,
    onSelectVideoForPlayback: (GeneratedVideoEntity) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onQuickSaveVideo: (GeneratedVideoEntity) -> Unit,
    onExportVideoAs: (GeneratedVideoEntity) -> Unit
) {
    var showApiGuideCard by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_video_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MovieFilter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sutra AI Video Studio",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Text-to-Video & Image-to-Video • Powered by Google Veo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showApiGuideCard = !showApiGuideCard },
                            modifier = Modifier.testTag("video_api_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Video API Setup Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = showApiGuideCard || !isApiKeyConfigured) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Backend & API Credential Configuration",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• Uses `GEMINI_API_KEY` or optional `VIDEO_API_KEY` injected via `BuildConfig` from the AI Studio Secrets panel (`.env` / `.env.example`).\n" +
                                        "• Connects to Google Veo (`veo-3.1-fast-generate-preview` & `veo-3.1-generate-preview`).\n" +
                                        "• Modular provider architecture (`VideoGenerationProvider.kt`) allows swapping or adding custom video backend proxies without changing UI code.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Error Banner with Retry Support
        if (errorBanner != null) {
            item {
                val (errTitle, errMessage) = errorBanner
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_error_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = onDismissErrorBanner,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Text(
                            text = errMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onRetryGeneration(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.testTag("video_error_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Generation", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onDismissErrorBanner) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }

        // 3. Active Video Preview Player (shown when a generated video is selected or just finished)
        if (activePlayingVideo != null && activePlayingVideo.parsedStatus == VideoJobStatus.COMPLETED) {
            item {
                VideoPreviewPlayerCard(
                    video = activePlayingVideo,
                    onQuickSave = { onQuickSaveVideo(activePlayingVideo) },
                    onExportAs = { onExportVideoAs(activePlayingVideo) },
                    onDelete = { onDeleteVideo(activePlayingVideo.id) }
                )
            }
        }

        // 4. Live Generation Progress Card
        if (progress.isGenerating) {
            item {
                VideoGenerationProgressCard(
                    progress = progress,
                    quality = selectedQuality,
                    aspectRatio = selectedAspectRatio,
                    duration = selectedDuration,
                    onCancel = onCancelGeneration
                )
            }
        }

        // 5. Prompt & Image-to-Video Input Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sourceImage != null) {
                                    "Image-to-Video Animation Prompt"
                                } else {
                                    "Video Scene & Motion Prompt"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (promptText.isNotEmpty()) {
                            IconButton(
                                onClick = { onUpdatePrompt("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear prompt",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = onUpdatePrompt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        minLines = 4,
                        maxLines = 7,
                        placeholder = {
                            Text(
                                text = if (sourceImage != null) {
                                    "Animate the clouds and trees with gentle wind and create a slow cinematic camera movement…"
                                } else {
                                    "Create a cinematic realistic shot of a sports car driving through a mountain road during sunset, realistic lighting, smooth camera movement…"
                                }
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Quick Sample Prompts Strip
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (sourceImage != null) {
                                "Try an Image-to-Video Animation Prompt:"
                            } else {
                                "Try an Example Cinematic Prompt:"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val promptsToShow = if (sourceImage != null) {
                            SAMPLE_IMAGE_TO_VIDEO_PROMPTS
                        } else {
                            SAMPLE_TEXT_TO_VIDEO_PROMPTS
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            promptsToShow.forEachIndexed { idx, sample ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .width(250.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onUpdatePrompt(sample) }
                                        .testTag("video_sample_prompt_$idx")
                                ) {
                                    Text(
                                        text = sample,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // Optional Image Upload for Image-to-Video
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reference Image (Optional • Image-to-Video)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Upload a photo to animate it with your motion prompt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (sourceImage == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPickSourceImage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("video_upload_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Photo")
                                }

                                OutlinedButton(
                                    onClick = onTakeSourceCameraPhoto,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("video_camera_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera")
                                }
                            }
                        } else {
                            SourceImageAttachmentPreview(
                                attachment = sourceImage,
                                onRemove = onClearSourceImage
                            )
                        }
                    }
                }
            }
        }

        // 6. Video Generation Parameters Card (Duration, Aspect Ratio, Quality)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Duration Selector: 5s, 10s
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Duration (Veo API: 4s–8s)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoDurationOption.entries.forEach { option ->
                                FilterChip(
                                    selected = selectedDuration == option,
                                    onClick = { onSelectDuration(option) },
                                    label = {
                                        Text(
                                            text = "${option.label} Clip",
                                            fontWeight = if (selectedDuration == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("video_duration_${option.seconds}s")
                                )
                            }
                        }
                    }

                    // Aspect Ratio Selector: 16:9, 9:16, 1:1
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Aspect Ratio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoAspectRatioOption.entries.forEach { ratio ->
                                FilterChip(
                                    selected = selectedAspectRatio == ratio,
                                    onClick = { onSelectAspectRatio(ratio) },
                                    label = {
                                        Text(
                                            text = ratio.label,
                                            fontWeight = if (selectedAspectRatio == ratio) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("video_aspect_${ratio.apiValue}")
                                )
                            }
                        }
                    }

                    // Quality Selector: Standard / High
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Render Quality & Model",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VideoQualityOption.entries.forEach { quality ->
                                val selected = selectedQuality == quality
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            },
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectQuality(quality) }
                                        .testTag("video_quality_${quality.label.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = quality.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = quality.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Generate Video Action Button
                    Button(
                        onClick = onGenerateVideo,
                        enabled = !progress.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("generate_video_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (progress.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generating Video (${progress.elapsedSeconds}s)…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sourceImage != null) {
                                    "Generate Image-to-Video (${selectedDuration.label})"
                                } else {
                                    "Generate Video (${selectedDuration.label} • ${selectedAspectRatio.apiValue})"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 7. "My Videos" History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Videos (${myVideos.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (myVideos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Generated Videos Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter a text prompt or attach a reference photo above and tap 'Generate Video' to create your first AI video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(myVideos, key = { it.id }) { videoItem ->
                MyVideoHistoryItemCard(
                    video = videoItem,
                    isCurrentlySelected = activePlayingVideo?.id == videoItem.id,
                    onPlay = { onSelectVideoForPlayback(videoItem) },
                    onRetry = { onRetryGeneration(videoItem) },
                    onSave = { onQuickSaveVideo(videoItem) },
                    onDelete = { onDeleteVideo(videoItem.id) }
                )
            }
        }
    }
}

@Composable
private fun SourceImageAttachmentPreview(
    attachment: PendingAttachment,
    onRemove: () -> Unit
) {
    val decodedBitmap = remember(attachment.base64Data) {
        attachment.base64Data?.let { b64 ->
            runCatching {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (decodedBitmap != null) {
                        Image(
                            bitmap = decodedBitmap,
                            contentDescription = "Source image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Image-to-Video Reference Frame Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove reference image"
                    )
                }
            }

            if (attachment.uploadProgress < 1.0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { attachment.uploadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VideoGenerationProgressCard(
    progress: VideoGenerationProgress,
    quality: VideoQualityOption,
    aspectRatio: VideoAspectRatioOption,
    duration: VideoDurationOption,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "video_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_progress_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(pulseScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = progress.stageTitle.ifBlank { "Generating AI Video…" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = progress.statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${progress.elapsedSeconds}s",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progress.progressFraction.coerceIn(0.05f, 0.98f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${quality.modelId} • ${aspectRatio.apiValue} • ${duration.label}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_video_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewPlayerCard(
    video: GeneratedVideoEntity,
    onQuickSave: () -> Unit,
    onExportAs: () -> Unit,
    onDelete: () -> Unit
) {
    var isPlaying by remember(video.id) { mutableStateOf(true) }
    var replayTrigger by remember(video.id) { mutableStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(video.id) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_preview_player_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Generated Video Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${video.modelUsed} • ${video.aspectRatio} • ${video.durationSeconds}s • ${video.quality}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Video Surface Box
            val localFileExists = remember(video.videoLocalPath) {
                !video.videoLocalPath.isNullOrBlank() && File(video.videoLocalPath).exists()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(video.parsedAspectRatio.ratioFloat.coerceIn(0.65f, 1.78f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (localFileExists && video.videoLocalPath != null) {
                    key(video.id, replayTrigger) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    videoViewRef = this
                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    setVideoURI(Uri.fromFile(File(video.videoLocalPath)))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                        isPlaying = true
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = "Video file unavailable on disk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            Text(
                text = video.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Mobile-Friendly Playback & Export Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val vv = videoViewRef
                        if (vv != null) {
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }

                FilledTonalButton(
                    onClick = {
                        replayTrigger++
                        isPlaying = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_replay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replay")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_quick_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save MP4")
                }

                OutlinedButton(
                    onClick = onExportAs,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_export_as_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export As…")
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete video",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MyVideoHistoryItemCard(
    video: GeneratedVideoEntity,
    isCurrentlySelected: Boolean,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBitmap = remember(video.thumbnailBase64) {
        video.thumbnailBase64?.let { b64 ->
            runCatching {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val formattedDate = remember(video.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(video.createdAt))
    }

    val status = video.parsedStatus

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlySelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_video_item_${video.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Video Thumbnail Box
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .height(66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B)
                                )
                            )
                        )
                        .clickable(enabled = status == VideoJobStatus.COMPLETED) { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap,
                            contentDescription = "Video thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (status) {
                                    VideoJobStatus.COMPLETED -> Icons.Default.PlayArrow
                                    VideoJobStatus.GENERATING -> Icons.Default.MovieFilter
                                    VideoJobStatus.FAILED -> Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (status) {
                                VideoJobStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.16f)
                                VideoJobStatus.GENERATING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                VideoJobStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                text = status.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (status) {
                                    VideoJobStatus.COMPLETED -> EmeraldSuccess
                                    VideoJobStatus.GENERATING -> MaterialTheme.colorScheme.primary
                                    VideoJobStatus.FAILED -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${video.durationSeconds}s • ${video.aspectRatio} • ${video.quality}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (video.hasSourceImage) {
                            "$formattedDate • Image-to-Video"
                        } else {
                            "$formattedDate • Text-to-Video"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (status == VideoJobStatus.FAILED && !video.errorMessage.isNullOrBlank()) {
                Text(
                    text = "${video.errorCategory ?: "Error"}: ${video.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action Buttons Row (Play / Retry / Save / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status == VideoJobStatus.COMPLETED) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("my_video_play_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }

                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.testTag("my_video_save_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save video",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                } else if (status == VideoJobStatus.FAILED) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("my_video_retry_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Generation")
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("my_video_delete_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}
