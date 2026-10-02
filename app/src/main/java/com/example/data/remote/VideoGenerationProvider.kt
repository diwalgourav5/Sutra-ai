package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import com.example.data.model.PendingAttachment
import com.example.data.model.VideoAspectRatioOption
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoErrorCategory
import com.example.data.model.VideoGenerationProgress
import com.example.data.model.VideoQualityOption
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class VideoGenerationRequest(
    val prompt: String,
    val duration: VideoDurationOption,
    val aspectRatio: VideoAspectRatioOption,
    val quality: VideoQualityOption,
    val sourceImage: PendingAttachment? = null
)

sealed class VideoGenerationOutcome {
    data class Success(
        val localVideoFilePath: String,
        val remoteVideoUri: String?,
        val thumbnailBase64: String?,
        val modelUsed: String
    ) : VideoGenerationOutcome()

    data class Failure(
        val category: VideoErrorCategory,
        val userMessage: String,
        val modelAttempted: String
    ) : VideoGenerationOutcome()
}

/**
 * Modular interface for AI Video Generation providers.
 * Allows plugging in additional video models or custom backend endpoints without
 * modifying the UI or ViewModel layers.
 */
interface VideoGenerationProvider {
    val providerName: String

    suspend fun generateVideo(
        context: Context,
        request: VideoGenerationRequest,
        onProgress: suspend (VideoGenerationProgress) -> Unit
    ): VideoGenerationOutcome
}

/**
 * Production implementation connecting to Google Veo (`veo-3.1-fast-generate-preview`
 * and `veo-3.1-generate-preview`) via the Gemini REST API.
 */
class GeminiVeoVideoProvider : VideoGenerationProvider {

    override val providerName: String = "Google Veo 3.1 Video Engine"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateVideo(
        context: Context,
        request: VideoGenerationRequest,
        onProgress: suspend (VideoGenerationProgress) -> Unit
    ): VideoGenerationOutcome = withContext(Dispatchers.IO) {
        val cleanPrompt = request.prompt.trim()

        // 1. Validate Prompt
        if (cleanPrompt.length < 4) {
            return@withContext VideoGenerationOutcome.Failure(
                category = VideoErrorCategory.INVALID_PROMPT,
                userMessage = "Please enter a descriptive video prompt (at least 4 characters) describing the scene, subject, and camera motion.",
                modelAttempted = request.quality.modelId
            )
        }

        // 2. Validate Duration (strictly integer between 4 and 8 inclusive)
        val durationSecondsInt: Int = request.duration.seconds
        if (durationSecondsInt !in 4..8) {
            return@withContext VideoGenerationOutcome.Failure(
                category = VideoErrorCategory.API_ERROR,
                userMessage = "Invalid duration ($durationSecondsInt seconds). Google Veo video generation requires durationSeconds to be an integer between 4 and 8 inclusive.",
                modelAttempted = request.quality.modelId
            )
        }

        // 3. Validate Optional Image Attachment for Image-to-Video
        val sourceImg = request.sourceImage
        if (sourceImg != null) {
            if (!sourceImg.isImage || sourceImg.base64Data.isNullOrBlank()) {
                return@withContext VideoGenerationOutcome.Failure(
                    category = VideoErrorCategory.UNSUPPORTED_IMAGE,
                    userMessage = "The selected attachment (${sourceImg.fileName}) is not a valid image for Image-to-Video generation. Please upload a JPEG, PNG, or WebP photo.",
                    modelAttempted = request.quality.modelId
                )
            }
        }

        // 3. Check API Credentials
        val apiKey = ApiConfig.getVideoApiKey()
        if (apiKey.isBlank() || !ApiConfig.isApiKeyConfigured()) {
            return@withContext VideoGenerationOutcome.Failure(
                category = VideoErrorCategory.API_ERROR,
                userMessage = "API Key is not configured. Open the Secrets panel in AI Studio and configure `GEMINI_API_KEY` (or `VIDEO_API_KEY`) to enable Veo video generation.",
                modelAttempted = request.quality.modelId
            )
        }

        // 4. Check Client-Side Rate Limit
        val rateLimit = ApiConfig.checkRateLimitAndRecord()
        if (!rateLimit.allowed) {
            return@withContext VideoGenerationOutcome.Failure(
                category = VideoErrorCategory.RATE_LIMIT,
                userMessage = "Usage rate limit reached (${ApiConfig.MAX_REQUESTS_PER_MINUTE} requests/min). Please wait ${rateLimit.retryAfterSeconds}s and retry.",
                modelAttempted = request.quality.modelId
            )
        }

        val primaryModel = request.quality.modelId
        val candidateModels = if (primaryModel == ApiConfig.MODEL_VIDEO_HIGH) {
            listOf(ApiConfig.MODEL_VIDEO_HIGH, ApiConfig.MODEL_VIDEO_FAST)
        } else {
            listOf(ApiConfig.MODEL_VIDEO_FAST, ApiConfig.MODEL_VIDEO_HIGH)
        }

        onProgress(
            VideoGenerationProgress(
                isGenerating = true,
                stageTitle = "Submitting Video Job",
                statusDetail = "Sending prompt to $primaryModel (${request.aspectRatio.apiValue}, ${request.duration.label})…",
                progressFraction = 0.08f,
                elapsedSeconds = 0
            )
        )

        var lastFailure: VideoGenerationOutcome.Failure? = null

        for (model in candidateModels) {
            coroutineContext.ensureActive()

            val startResult = startVeoGenerationJob(
                model = model,
                apiKey = apiKey,
                request = request
            )

            when (startResult) {
                is JobStartResult.DirectVideoData -> {
                    onProgress(
                        VideoGenerationProgress(
                            isGenerating = true,
                            stageTitle = "Saving Generated Video",
                            statusDetail = "Finalizing MP4 video stream…",
                            progressFraction = 0.92f,
                            elapsedSeconds = 2
                        )
                    )
                    return@withContext finalizeAndSaveVideo(
                        context = context,
                        apiKey = apiKey,
                        modelUsed = model,
                        videoUri = startResult.videoUri,
                        videoBase64 = startResult.videoBase64,
                        fallbackThumbBase64 = sourceImg?.base64Data
                    )
                }

                is JobStartResult.OperationStarted -> {
                    val opName = startResult.operationName
                    val pollOutcome = pollVideoOperationUntilComplete(
                        context = context,
                        apiKey = apiKey,
                        modelUsed = model,
                        operationName = opName,
                        fallbackThumbBase64 = sourceImg?.base64Data,
                        onProgress = onProgress
                    )
                    if (pollOutcome is VideoGenerationOutcome.Success) {
                        return@withContext pollOutcome
                    } else if (pollOutcome is VideoGenerationOutcome.Failure) {
                        lastFailure = pollOutcome
                    }
                }

                is JobStartResult.Error -> {
                    lastFailure = VideoGenerationOutcome.Failure(
                        category = startResult.category,
                        userMessage = startResult.message,
                        modelAttempted = model
                    )
                }
            }
        }

        lastFailure ?: VideoGenerationOutcome.Failure(
            category = VideoErrorCategory.GENERATION_FAILURE,
            userMessage = "Video generation could not be completed by the backend.",
            modelAttempted = primaryModel
        )
    }

    private sealed class JobStartResult {
        data class OperationStarted(val operationName: String) : JobStartResult()
        data class DirectVideoData(val videoUri: String?, val videoBase64: String?) : JobStartResult()
        data class Error(val category: VideoErrorCategory, val message: String) : JobStartResult()
    }

    private fun startVeoGenerationJob(
        model: String,
        apiKey: String,
        request: VideoGenerationRequest
    ): JobStartResult {
        // Endpoint 1: generateVideos (per Gemini API Veo specification)
        val generateVideosPayload = JSONObject().apply {
            put("prompt", request.prompt.trim())
            if (request.sourceImage != null && !request.sourceImage.base64Data.isNullOrBlank()) {
                put("image", JSONObject().apply {
                    put("bytesBase64Encoded", request.sourceImage.base64Data)
                    put("mimeType", request.sourceImage.mimeType.ifBlank { "image/jpeg" })
                })
            }
            put("config", JSONObject().apply {
                put("numberOfVideos", 1)
                put("resolution", request.quality.resolutionParam)
                put("aspectRatio", request.aspectRatio.apiValue)
                put("durationSeconds", request.duration.seconds)
            })
        }

        // Endpoint 2: predictLongRunning (Vertex / Generative Language LRO format)
        val predictLongRunningPayload = JSONObject().apply {
            val instance = JSONObject().apply {
                put("prompt", request.prompt.trim())
                if (request.sourceImage != null && !request.sourceImage.base64Data.isNullOrBlank()) {
                    put("image", JSONObject().apply {
                        put("bytesBase64Encoded", request.sourceImage.base64Data)
                        put("mimeType", request.sourceImage.mimeType.ifBlank { "image/jpeg" })
                    })
                }
            }
            put("instances", JSONArray().put(instance))
            put("parameters", JSONObject().apply {
                put("aspectRatio", request.aspectRatio.apiValue)
                put("sampleCount", 1)
                put("durationSeconds", request.duration.seconds)
            })
        }

        val candidateCalls = listOf(
            "${ApiConfig.VIDEO_BACKEND_BASE_URL}v1beta/models/$model:generateVideos?key=$apiKey" to generateVideosPayload,
            "${ApiConfig.VIDEO_BACKEND_BASE_URL}v1beta/models/$model:predictLongRunning?key=$apiKey" to predictLongRunningPayload
        )

        var lastErrorResult: JobStartResult.Error? = null

        for ((url, payload) in candidateCalls) {
            try {
                val httpReq = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(httpReq).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (response.isSuccessful && rawBody.isNotBlank()) {
                        val json = JSONObject(rawBody)

                        // Check if an immediate video URI or bytes were returned
                        val directUri = extractVideoUriFromResponse(json)
                        val directBase64 = extractVideoBase64FromResponse(json)
                        if (!directUri.isNullOrBlank() || !directBase64.isNullOrBlank()) {
                            return JobStartResult.DirectVideoData(directUri, directBase64)
                        }

                        // Otherwise extract long-running operation name
                        val opName = json.optString("name", "")
                        if (opName.isNotBlank()) {
                            return JobStartResult.OperationStarted(opName)
                        }
                    } else {
                        lastErrorResult = classifyHttpError(response.code, rawBody, model)
                    }
                }
            } catch (e: Exception) {
                lastErrorResult = JobStartResult.Error(
                    category = VideoErrorCategory.NETWORK_FAILURE,
                    message = "Network failure connecting to video backend: ${e.localizedMessage ?: "Check internet connection"}"
                )
            }
        }

        return lastErrorResult ?: JobStartResult.Error(
            category = VideoErrorCategory.API_ERROR,
            message = "Unable to start video generation job on model $model."
        )
    }

    private suspend fun pollVideoOperationUntilComplete(
        context: Context,
        apiKey: String,
        modelUsed: String,
        operationName: String,
        fallbackThumbBase64: String?,
        onProgress: suspend (VideoGenerationProgress) -> Unit
    ): VideoGenerationOutcome {
        val startTimeMs = System.currentTimeMillis()
        val cleanOpPath = operationName.removePrefix("/")
        val pollUrl = "${ApiConfig.VIDEO_BACKEND_BASE_URL}v1beta/$cleanOpPath?key=$apiKey"

        var pollAttempt = 0
        while (true) {
            coroutineContext.ensureActive()
            val elapsedMs = System.currentTimeMillis() - startTimeMs
            val elapsedSec = (elapsedMs / 1000L).toInt()

            if (elapsedMs > ApiConfig.VIDEO_GENERATION_TIMEOUT_MS) {
                return VideoGenerationOutcome.Failure(
                    category = VideoErrorCategory.TIMEOUT,
                    userMessage = "Video generation timed out after ${elapsedSec}s. The server may be under heavy load — please try again.",
                    modelAttempted = modelUsed
                )
            }

            val simulatedProgress = (0.15f + (elapsedMs.toFloat() / 90_000f) * 0.72f).coerceAtMost(0.88f)
            val stageLabel = when {
                pollAttempt < 2 -> "Initializing Veo 3.1 Diffusion Pipeline"
                pollAttempt < 6 -> "Synthesizing Temporal Motion & Keyframes"
                else -> "Rendering Lighting, Physics & High-Definition Frames"
            }

            onProgress(
                VideoGenerationProgress(
                    isGenerating = true,
                    stageTitle = stageLabel,
                    statusDetail = "Polling job status (${elapsedSec}s elapsed)…",
                    progressFraction = simulatedProgress,
                    elapsedSeconds = elapsedSec,
                    operationName = operationName
                )
            )

            delay(ApiConfig.VIDEO_POLL_INTERVAL_MS)
            coroutineContext.ensureActive()
            pollAttempt++

            try {
                val request = Request.Builder().url(pollUrl).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val err = classifyHttpError(response.code, bodyStr, modelUsed)
                        return VideoGenerationOutcome.Failure(
                            category = err.category,
                            userMessage = err.message,
                            modelAttempted = modelUsed
                        )
                    }

                    val json = JSONObject(bodyStr)
                    val errObj = json.optJSONObject("error")
                    if (errObj != null) {
                        val errMsg = errObj.optString("message", "Video operation failed on server.")
                        val errStatus = errObj.optString("status", "")
                        val fullDetail = if (errStatus.isNotBlank()) "[$errStatus] $errMsg" else errMsg
                        return VideoGenerationOutcome.Failure(
                            category = VideoErrorCategory.API_ERROR,
                            userMessage = "API Error: $fullDetail",
                            modelAttempted = modelUsed
                        )
                    }

                    val isDone = json.optBoolean("done", false)
                    if (isDone) {
                        val videoUri = extractVideoUriFromResponse(json)
                        val videoBase64 = extractVideoBase64FromResponse(json)

                        if (videoUri.isNullOrBlank() && videoBase64.isNullOrBlank()) {
                            return VideoGenerationOutcome.Failure(
                                category = VideoErrorCategory.GENERATION_FAILURE,
                                userMessage = "The video generation job finished, but no video stream was returned (it may have been filtered by safety policies). Try adjusting your prompt.",
                                modelAttempted = modelUsed
                            )
                        }

                        onProgress(
                            VideoGenerationProgress(
                                isGenerating = true,
                                stageTitle = "Downloading Generated MP4 Video",
                                statusDetail = "Saving video and generating preview thumbnail…",
                                progressFraction = 0.94f,
                                elapsedSeconds = elapsedSec,
                                operationName = operationName
                            )
                        )

                        return finalizeAndSaveVideo(
                            context = context,
                            apiKey = apiKey,
                            modelUsed = modelUsed,
                            videoUri = videoUri,
                            videoBase64 = videoBase64,
                            fallbackThumbBase64 = fallbackThumbBase64
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Transient network hiccup during polling; if past 3 attempts, fail gracefully
                if (pollAttempt >= 4) {
                    return VideoGenerationOutcome.Failure(
                        category = VideoErrorCategory.NETWORK_FAILURE,
                        userMessage = "Lost network connection while polling video status: ${e.localizedMessage ?: "Network error"}",
                        modelAttempted = modelUsed
                    )
                }
            }
        }
    }

    private fun finalizeAndSaveVideo(
        context: Context,
        apiKey: String,
        modelUsed: String,
        videoUri: String?,
        videoBase64: String?,
        fallbackThumbBase64: String?
    ): VideoGenerationOutcome {
        val videosDir = context.getExternalFilesDir("videos")
            ?: File(context.filesDir, "videos")
        if (!videosDir.exists()) {
            videosDir.mkdirs()
        }
        val outFile = File(videosDir, "sutra_veo_${System.currentTimeMillis()}.mp4")

        try {
            if (!videoBase64.isNullOrBlank()) {
                val bytes = Base64.decode(videoBase64, Base64.DEFAULT)
                FileOutputStream(outFile).use { it.write(bytes) }
            } else if (!videoUri.isNullOrBlank()) {
                val downloadUrl = if (videoUri.contains("generativelanguage.googleapis.com") && !videoUri.contains("key=")) {
                    val separator = if (videoUri.contains("?")) "&" else "?"
                    "$videoUri${separator}key=$apiKey"
                } else {
                    videoUri
                }

                val dlReq = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build()

                okHttpClient.newCall(dlReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        return VideoGenerationOutcome.Failure(
                            category = VideoErrorCategory.API_ERROR,
                            userMessage = "Generated video URI was created, but downloading the MP4 failed with HTTP ${response.code}.",
                            modelAttempted = modelUsed
                        )
                    }
                    val stream = response.body?.byteStream() ?: return VideoGenerationOutcome.Failure(
                        category = VideoErrorCategory.GENERATION_FAILURE,
                        userMessage = "Downloaded video stream was empty.",
                        modelAttempted = modelUsed
                    )
                    FileOutputStream(outFile).use { out ->
                        stream.copyTo(out)
                    }
                }
            } else {
                return VideoGenerationOutcome.Failure(
                    category = VideoErrorCategory.GENERATION_FAILURE,
                    userMessage = "No video data available to save.",
                    modelAttempted = modelUsed
                )
            }

            val extractedThumb = extractVideoFrameThumbnailBase64(outFile.absolutePath)
                ?: fallbackThumbBase64

            return VideoGenerationOutcome.Success(
                localVideoFilePath = outFile.absolutePath,
                remoteVideoUri = videoUri,
                thumbnailBase64 = extractedThumb,
                modelUsed = modelUsed
            )
        } catch (e: Exception) {
            return VideoGenerationOutcome.Failure(
                category = VideoErrorCategory.GENERATION_FAILURE,
                userMessage = "Failed to save generated video file: ${e.localizedMessage ?: "Storage error"}",
                modelAttempted = modelUsed
            )
        }
    }

    private fun extractVideoUriFromResponse(json: JSONObject): String? {
        val responseObj = json.optJSONObject("response") ?: json

        // Format A: response.generateVideoResponse.generatedSamples[0].video.uri
        val genVidResp = responseObj.optJSONObject("generateVideoResponse")
        if (genVidResp != null) {
            val samples = genVidResp.optJSONArray("generatedSamples")
            val firstSample = samples?.optJSONObject(0)
            val uri = firstSample?.optJSONObject("video")?.optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }

        // Format B: response.generatedVideos[0].video.uri
        val generatedVideos = responseObj.optJSONArray("generatedVideos")
        if (generatedVideos != null && generatedVideos.length() > 0) {
            val first = generatedVideos.optJSONObject(0)
            val uri = first?.optJSONObject("video")?.optString("uri")
                ?: first?.optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }

        // Format C: response.videos[0].uri
        val videos = responseObj.optJSONArray("videos")
        if (videos != null && videos.length() > 0) {
            val uri = videos.optJSONObject(0)?.optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }

        return null
    }

    private fun extractVideoBase64FromResponse(json: JSONObject): String? {
        val responseObj = json.optJSONObject("response") ?: json

        val predictions = responseObj.optJSONArray("predictions")
        if (predictions != null && predictions.length() > 0) {
            val first = predictions.optJSONObject(0)
            val b64 = first?.optString("bytesBase64Encoded")
                ?: first?.optJSONObject("video")?.optString("bytesBase64Encoded")
            if (!b64.isNullOrBlank()) return b64
        }

        val generatedVideos = responseObj.optJSONArray("generatedVideos")
        if (generatedVideos != null && generatedVideos.length() > 0) {
            val first = generatedVideos.optJSONObject(0)
            val b64 = first?.optJSONObject("video")?.optString("bytesBase64Encoded")
                ?: first?.optJSONObject("video")?.optString("videoBytes")
            if (!b64.isNullOrBlank()) return b64
        }

        return null
    }

    private fun extractVideoFrameThumbnailBase64(filePath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val bitmap = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
                ?: return null
            val scaled = Bitmap.createScaledBitmap(bitmap, 320, 180, true)
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, bos)
            Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun classifyHttpError(
        code: Int,
        rawBody: String,
        model: String
    ): JobStartResult.Error {
        var serverMsg = ""
        var serverStatus = ""
        var detailsText = ""

        try {
            val json = JSONObject(rawBody)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                serverMsg = errObj.optString("message", "")
                serverStatus = errObj.optString("status", "")
                val detailsArray = errObj.optJSONArray("details")
                if (detailsArray != null && detailsArray.length() > 0) {
                    val detailsList = mutableListOf<String>()
                    for (i in 0 until detailsArray.length()) {
                        val d = detailsArray.optJSONObject(i) ?: continue
                        val desc = d.optString("description", "")
                        if (desc.isNotBlank()) detailsList.add(desc)
                        val fieldViolations = d.optJSONArray("fieldViolations")
                        if (fieldViolations != null) {
                            for (j in 0 until fieldViolations.length()) {
                                val fv = fieldViolations.optJSONObject(j) ?: continue
                                val field = fv.optString("field", "")
                                val fvDesc = fv.optString("description", "")
                                detailsList.add(if (field.isNotBlank()) "$field: $fvDesc" else fvDesc)
                            }
                        }
                    }
                    if (detailsList.isNotEmpty()) {
                        detailsText = detailsList.joinToString("; ")
                    }
                }
            } else {
                serverMsg = rawBody
            }
        } catch (_: Exception) {
            serverMsg = rawBody
        }

        val detailBuilder = StringBuilder()
        if (serverMsg.isNotBlank()) detailBuilder.append(serverMsg)
        if (serverStatus.isNotBlank() && !serverMsg.contains(serverStatus)) {
            detailBuilder.append(" [Status: $serverStatus]")
        }
        if (detailsText.isNotBlank()) {
            detailBuilder.append(" ($detailsText)")
        }
        val detail = detailBuilder.toString().ifBlank { "HTTP $code from video API" }

        val isExplicitPromptSafety = detail.contains("safety", ignoreCase = true) ||
            detail.contains("prompt blocked", ignoreCase = true) ||
            detail.contains("content policy", ignoreCase = true) ||
            detail.contains("HARM_CATEGORY", ignoreCase = true)

        if (isExplicitPromptSafety) {
            return JobStartResult.Error(
                category = VideoErrorCategory.INVALID_PROMPT,
                message = "Prompt filtered by safety policy on $model: $detail"
            )
        }

        return when (code) {
            400 -> JobStartResult.Error(
                category = VideoErrorCategory.API_ERROR,
                message = "API Error (HTTP 400 - Bad Request): $detail"
            )
            401, 403 -> JobStartResult.Error(
                category = VideoErrorCategory.API_ERROR,
                message = "Authorization error on $model (HTTP $code): $detail. Verify that your GEMINI_API_KEY / VIDEO_API_KEY in the AI Studio Secrets panel has access to Veo video generation."
            )
            404 -> JobStartResult.Error(
                category = VideoErrorCategory.API_ERROR,
                message = "Video model '$model' endpoint was not found or is not enabled for this project (HTTP 404): $detail"
            )
            429 -> JobStartResult.Error(
                category = VideoErrorCategory.RATE_LIMIT,
                message = "Video API quota or rate limit reached (HTTP 429): $detail"
            )
            else -> JobStartResult.Error(
                category = VideoErrorCategory.API_ERROR,
                message = "Video API Error (HTTP $code): $detail"
            )
        }
    }

    companion object {
        suspend fun exportVideoFileToUri(
            context: Context,
            sourceFilePath: String,
            targetUri: Uri
        ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
            try {
                val srcFile = File(sourceFilePath)
                if (!srcFile.exists()) {
                    return@withContext false to "Source video file no longer exists on disk."
                }
                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    srcFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: return@withContext false to "Could not open target destination for writing."
                true to "Video exported successfully!"
            } catch (e: Exception) {
                false to "Failed to export video: ${e.localizedMessage ?: "Write error"}"
            }
        }
    }
}
