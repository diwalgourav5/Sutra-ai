package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.data.model.ImageAspectRatioOption
import com.example.data.model.PendingAttachment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ImageGenerationRequest(
    val prompt: String,
    val aspectRatio: ImageAspectRatioOption,
    val sourceImage: PendingAttachment? = null
)

sealed class ImageGenerationOutcome {
    data class Success(
        val localImageFilePath: String,
        val imageBase64: String,
        val mimeType: String,
        val modelUsed: String
    ) : ImageGenerationOutcome()

    data class Failure(
        val errorTitle: String,
        val userMessage: String,
        val modelAttempted: String
    ) : ImageGenerationOutcome()
}

class GeminiImageGenerationProvider {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateImage(
        context: Context,
        request: ImageGenerationRequest
    ): ImageGenerationOutcome = withContext(Dispatchers.IO) {
        val cleanPrompt = request.prompt.trim()

        // 1. Validate prompt
        if (cleanPrompt.length < 3) {
            return@withContext ImageGenerationOutcome.Failure(
                errorTitle = "Invalid Prompt",
                userMessage = "Please enter an image prompt describing what you want to generate or edit (at least 3 characters).",
                modelAttempted = ApiConfig.MODEL_IMAGE_GENERATION
            )
        }

        // 2. Validate source image if image-to-image editing
        val sourceImg = request.sourceImage
        if (sourceImg != null) {
            if (!sourceImg.isImage || sourceImg.base64Data.isNullOrBlank()) {
                return@withContext ImageGenerationOutcome.Failure(
                    errorTitle = "Unsupported Image",
                    userMessage = "The reference image format is unsupported. Please upload a valid JPEG, PNG, or WebP photo.",
                    modelAttempted = ApiConfig.MODEL_IMAGE_GENERATION
                )
            }
        }

        // 3. Check API key
        val apiKey = ApiConfig.getApiKey()
        if (apiKey.isBlank() || !ApiConfig.isApiKeyConfigured()) {
            return@withContext ImageGenerationOutcome.Failure(
                errorTitle = "API Key Missing",
                userMessage = "Gemini API key is not configured. Open the Secrets panel in AI Studio and configure `GEMINI_API_KEY` to enable AI image generation.",
                modelAttempted = ApiConfig.MODEL_IMAGE_GENERATION
            )
        }

        // 4. Rate limiting check
        val rateLimit = ApiConfig.checkRateLimitAndRecord()
        if (!rateLimit.allowed) {
            return@withContext ImageGenerationOutcome.Failure(
                errorTitle = "Rate Limit Reached",
                userMessage = "Rate limit reached (${ApiConfig.MAX_REQUESTS_PER_MINUTE} req/min). Please wait ${rateLimit.retryAfterSeconds}s before generating another image.",
                modelAttempted = ApiConfig.MODEL_IMAGE_GENERATION
            )
        }

        // Candidate models in priority order (user preferred: gemini-3.1-flash-image)
        val candidateModels = listOf(
            ApiConfig.MODEL_IMAGE_GENERATION,        // gemini-3.1-flash-image
            "gemini-3.1-flash-image-preview",        // preview alias
            ApiConfig.MODEL_IMAGE_FALLBACK           // gemini-2.5-flash-image
        )

        var lastFailure: ImageGenerationOutcome.Failure? = null

        for (model in candidateModels) {
            val payload = buildGenerateImagePayload(cleanPrompt, request.aspectRatio, sourceImg)
            val url = "${ApiConfig.BACKEND_BASE_URL}v1beta/models/$model:generateContent?key=$apiKey"

            try {
                val httpReq = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                okHttpClient.newCall(httpReq).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (response.isSuccessful && rawBody.isNotBlank()) {
                        val parsedOutcome = parseAndSaveImageResponse(context, rawBody, model)
                        if (parsedOutcome is ImageGenerationOutcome.Success) {
                            return@withContext parsedOutcome
                        } else if (parsedOutcome is ImageGenerationOutcome.Failure) {
                            lastFailure = parsedOutcome
                        }
                    } else {
                        lastFailure = classifyErrorResponse(response.code, rawBody, model)
                    }
                }
            } catch (e: Exception) {
                lastFailure = ImageGenerationOutcome.Failure(
                    errorTitle = "Network Error",
                    userMessage = "Network failure connecting to Gemini Image API: ${e.localizedMessage ?: "Check your connection"}",
                    modelAttempted = model
                )
            }
        }

        lastFailure ?: ImageGenerationOutcome.Failure(
            errorTitle = "Image Generation Failed",
            userMessage = "Could not generate image. The model did not return any image data.",
            modelAttempted = ApiConfig.MODEL_IMAGE_GENERATION
        )
    }

    private fun buildGenerateImagePayload(
        prompt: String,
        aspectRatio: ImageAspectRatioOption,
        sourceImage: PendingAttachment?
    ): JSONObject {
        val partsArray = JSONArray()

        // If source image is provided for image-to-image editing, attach it first or with the prompt
        if (sourceImage != null && !sourceImage.base64Data.isNullOrBlank()) {
            val imagePart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", sourceImage.mimeType.ifBlank { "image/jpeg" })
                    put("data", sourceImage.base64Data)
                })
            }
            partsArray.put(imagePart)
        }

        // Text prompt
        partsArray.put(JSONObject().apply {
            put("text", prompt)
        })

        val contentObj = JSONObject().apply {
            put("parts", partsArray)
        }

        val generationConfig = JSONObject().apply {
            put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
            put("imageConfig", JSONObject().apply {
                put("aspectRatio", aspectRatio.apiValue)
                put("imageSize", "1K")
            })
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(contentObj))
            put("generationConfig", generationConfig)
        }
    }

    private fun parseAndSaveImageResponse(
        context: Context,
        rawBody: String,
        model: String
    ): ImageGenerationOutcome {
        val json = JSONObject(rawBody)

        val errObj = json.optJSONObject("error")
        if (errObj != null) {
            val msg = errObj.optString("message", "API Error occurred.")
            val status = errObj.optString("status", "")
            return ImageGenerationOutcome.Failure(
                errorTitle = "Image API Error",
                userMessage = if (status.isNotBlank()) "[$status] $msg" else msg,
                modelAttempted = model
            )
        }

        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val finishReason = firstCandidate?.optString("finishReason", "")
        if (finishReason == "SAFETY") {
            return ImageGenerationOutcome.Failure(
                errorTitle = "Prompt Safety Block",
                userMessage = "The image generation request was blocked by safety filters. Please adjust your prompt.",
                modelAttempted = model
            )
        }

        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        var foundBase64: String? = null
        var foundMimeType = "image/jpeg"

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val p = parts.optJSONObject(i) ?: continue
                val inlineData = p.optJSONObject("inlineData")
                if (inlineData != null) {
                    val data = inlineData.optString("data", "")
                    if (data.isNotBlank()) {
                        foundBase64 = data
                        foundMimeType = inlineData.optString("mimeType", "image/jpeg")
                        break
                    }
                }
            }
        }

        if (foundBase64.isNullOrBlank()) {
            val textMsg = parts?.optJSONObject(0)?.optString("text", "")
            val failureDetail = if (!textMsg.isNullOrBlank()) {
                "Model returned text instead of an image: \"$textMsg\""
            } else {
                "The selected model '$model' did not return an image. It may not support image generation on this account."
            }
            return ImageGenerationOutcome.Failure(
                errorTitle = "No Image Returned",
                userMessage = failureDetail,
                modelAttempted = model
            )
        }

        // Save image to local cache
        val imagesDir = context.getExternalFilesDir("generated_images")
            ?: File(context.filesDir, "generated_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val ext = if (foundMimeType.contains("png")) "png" else "jpg"
        val outFile = File(imagesDir, "sutra_img_${System.currentTimeMillis()}.$ext")

        return try {
            val bytes = Base64.decode(foundBase64, Base64.DEFAULT)
            FileOutputStream(outFile).use { it.write(bytes) }

            ImageGenerationOutcome.Success(
                localImageFilePath = outFile.absolutePath,
                imageBase64 = foundBase64,
                mimeType = foundMimeType,
                modelUsed = model
            )
        } catch (e: Exception) {
            ImageGenerationOutcome.Failure(
                errorTitle = "Storage Error",
                userMessage = "Failed to save generated image file: ${e.localizedMessage ?: "Unknown storage error"}",
                modelAttempted = model
            )
        }
    }

    private fun classifyErrorResponse(code: Int, rawBody: String, model: String): ImageGenerationOutcome.Failure {
        var serverMsg = ""
        var serverStatus = ""

        try {
            val json = JSONObject(rawBody)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                serverMsg = errObj.optString("message", "")
                serverStatus = errObj.optString("status", "")
            }
        } catch (_: Exception) {
            serverMsg = rawBody
        }

        val detail = if (serverMsg.isNotBlank()) {
            if (serverStatus.isNotBlank()) "[$serverStatus] $serverMsg" else serverMsg
        } else {
            "HTTP $code from Gemini API"
        }

        val isSafety = detail.contains("safety", ignoreCase = true) ||
            detail.contains("blocked", ignoreCase = true) ||
            detail.contains("content policy", ignoreCase = true)

        if (isSafety) {
            return ImageGenerationOutcome.Failure(
                errorTitle = "Prompt Safety Block",
                userMessage = "Image prompt was blocked by safety filters: $detail",
                modelAttempted = model
            )
        }

        return when (code) {
            400 -> ImageGenerationOutcome.Failure(
                errorTitle = "Image API Error",
                userMessage = "Request rejected by $model (HTTP 400): $detail",
                modelAttempted = model
            )
            401, 403 -> ImageGenerationOutcome.Failure(
                errorTitle = "API Key Error",
                userMessage = "Authorization failed for $model (HTTP $code): $detail. Verify that GEMINI_API_KEY in the Secrets panel is valid.",
                modelAttempted = model
            )
            404 -> ImageGenerationOutcome.Failure(
                errorTitle = "Model Unavailable",
                userMessage = "The image generation model '$model' was not found or is not enabled for your project (HTTP 404).",
                modelAttempted = model
            )
            429 -> ImageGenerationOutcome.Failure(
                errorTitle = "Quota Exceeded",
                userMessage = "Gemini Image API rate limit or quota exceeded (HTTP 429): $detail",
                modelAttempted = model
            )
            else -> ImageGenerationOutcome.Failure(
                errorTitle = "API Error",
                userMessage = "API returned HTTP $code: $detail",
                modelAttempted = model
            )
        }
    }

    companion object {
        suspend fun saveImageToGallery(context: Context, filePath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext false to "Source image file not found."

                val bitmap = BitmapFactory.decodeFile(filePath)
                    ?: return@withContext false to "Could not decode image bitmap."

                val filename = "SutraAI_${System.currentTimeMillis()}.jpg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SutraAI")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false to "Failed to create gallery entry."

                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val sutraDir = File(picturesDir, "SutraAI")
                    if (!sutraDir.exists()) sutraDir.mkdirs()
                    val target = File(sutraDir, filename)
                    FileOutputStream(target).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                true to "Image saved to Pictures/SutraAI!"
            } catch (e: Exception) {
                false to "Failed to save to gallery: ${e.localizedMessage ?: "Unknown error"}"
            }
        }

        suspend fun exportImageToUri(context: Context, filePath: String, targetUri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext false to "Source image file not found."

                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: return@withContext false to "Could not open target location for writing."

                true to "Image exported successfully!"
            } catch (e: Exception) {
                false to "Export failed: ${e.localizedMessage ?: "Write error"}"
            }
        }
    }
}
