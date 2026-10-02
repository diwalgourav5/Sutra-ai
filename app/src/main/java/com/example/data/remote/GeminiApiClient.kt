package com.example.data.remote

import com.example.data.model.AppLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MathCategory
import com.example.data.model.PendingAttachment
import com.example.data.model.UserProfileEntity
import com.example.data.model.WebSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AiGenerationResult(
    val text: String,
    val modelUsed: String,
    val usedWebSearch: Boolean,
    val webSources: List<WebSource>,
    val isError: Boolean = false
)

class GeminiApiClient {

    // Mandatory 60-second timeouts per Gemini API guidelines
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Builds the comprehensive System Instruction tailored to:
     * - Sutra AI core principles (accuracy, step-by-step reasoning, clear math, honest uncertainty)
     * - User Profile (username, role/focus, bio, and extensible custom attributes)
     * - Preferred Language (English, Hindi, or Hinglish)
     * - Active AssistantMode or MathCategory
     */
    fun buildSystemInstruction(
        profile: UserProfileEntity?,
        language: AppLanguage,
        assistantMode: AssistantMode,
        mathCategory: MathCategory? = null,
        useWebSearch: Boolean = false
    ): String {
        val userName = profile?.username?.takeIf { it.isNotBlank() } ?: "Learner"
        val userRole = profile?.roleOrFocus?.takeIf { it.isNotBlank() } ?: "Student & Professional"
        val userBio = profile?.bioOrContext?.takeIf { it.isNotBlank() } ?: ""
        val customAttrs = profile?.getExtensibleAttributesMap() ?: emptyMap()
        val customAttrsSummary = if (customAttrs.isNotEmpty()) {
            customAttrs.entries.joinToString("; ") { "${it.key}: ${it.value}" }
        } else {
            "Standard"
        }

        val mathSection = if (mathCategory != null || assistantMode == AssistantMode.MATH_SOLVER) {
            """
            MATH & LOGIC SOLVER PROTOCOL:
            - Category Focus: ${mathCategory?.title ?: "General Mathematics & Logic"} (${mathCategory?.subtitle ?: "All branches"})
            - Always format mathematical solutions into clear sections:
              1. **Problem Understanding & Given Data**: Restate the equation or values clearly.
              2. **Key Formula / Concept**: State the exact formula, theorem, or rule being applied.
              3. **Step-by-Step Calculation**: Show every intermediate algebraic or numerical step clearly so a student can follow without guessing. Use clean Unicode math notation (√, π, ∫, ∑, θ, ≤, ≥, ≠, x², matrices) and monospace blocks for matrix alignments.
              4. **Final Answer & Verification**: Clearly highlight the final answer and briefly verify its correctness.
            """.trimIndent()
        } else {
            ""
        }

        val webSearchNotice = if (useWebSearch) {
            """
            WEB SEARCH & CURRENT INFORMATION PROTOCOL:
            - When using retrieved web search information, clearly distinguish verified real-time/web facts from general foundational knowledge.
            - State dates, figures, and sources accurately and never fabricate citations.
            """.trimIndent()
        } else {
            ""
        }

        return """
            You are Sutra AI, an original, modern, high-precision conversational AI assistant for Android.
            
            ACTIVE USER PROFILE:
            - User Name: $userName
            - Role / Focus: $userRole
            - Personal Context: $userBio
            - Profile Preferences & Attributes: $customAttrsSummary
            
            LANGUAGE DIRECTIVE:
            - Preferred Language Setting: ${language.displayName}
            - ${language.systemPromptDirective}
            - You are completely fluent in English, Hindi (हिन्दी), and Hinglish (conversational Hindi in Roman script mixed with English). Always match the user's requested language or the language they write in.
            
            ACTIVE MODE (${assistantMode.title}):
            - ${assistantMode.systemDirective}
            
            CORE QUALITY & HONESTY RULES:
            1. Understand the user's question deeply before answering.
            2. Break complicated problems, science/study topics, and code architectures into clear, numbered, understandable steps.
            3. Show calculations and code clearly using Markdown formatting.
            4. Ask brief, helpful clarifying questions when a prompt is genuinely ambiguous.
            5. NEVER confidently invent facts, fake citations, or hallucinate APIs. Clearly state when information is uncertain or outside verifiable knowledge.
            $mathSection
            $webSearchNotice
        """.trimIndent()
    }

    /**
     * Streams content from the Gemini REST API (`streamGenerateContent?alt=sse`),
     * with automatic fallback if a preview model or search tool parameter is rejected.
     */
    suspend fun streamChatResponse(
        history: List<ChatMessageEntity>,
        userPrompt: String,
        pendingAttachment: PendingAttachment?,
        profile: UserProfileEntity?,
        language: AppLanguage,
        assistantMode: AssistantMode,
        mathCategory: MathCategory? = null,
        useWebSearch: Boolean = false,
        onChunk: suspend (accumulatedText: String, modelName: String) -> Unit
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = ApiConfig.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext AiGenerationResult(
                text = "API Key is missing. Please open the **Secrets panel in AI Studio** and set `GEMINI_API_KEY` (or check **Settings → API & Backend Status** for setup instructions).",
                modelUsed = "none",
                usedWebSearch = false,
                webSources = emptyList(),
                isError = true
            )
        }

        val rateLimit = ApiConfig.checkRateLimitAndRecord()
        if (!rateLimit.allowed) {
            return@withContext AiGenerationResult(
                text = "Rate limit reached (${ApiConfig.MAX_REQUESTS_PER_MINUTE} requests/minute). Please wait ${rateLimit.retryAfterSeconds}s before sending the next request.",
                modelUsed = "rate-limiter",
                usedWebSearch = false,
                webSources = emptyList(),
                isError = true
            )
        }

        // Select optimal model per task type
        val hasImageOrFile = pendingAttachment != null
        val prefersPro = (assistantMode.prefersProModel || mathCategory != null) && !hasImageOrFile
        val candidateModels = if (prefersPro) {
            listOf(
                ApiConfig.MODEL_COMPLEX_REASONING,
                ApiConfig.MODEL_FAST_MULTIMODAL,
                ApiConfig.MODEL_FALLBACK
            )
        } else {
            listOf(
                ApiConfig.MODEL_FAST_MULTIMODAL,
                ApiConfig.MODEL_FALLBACK
            )
        }

        val systemInstruction = buildSystemInstruction(
            profile = profile,
            language = language,
            assistantMode = assistantMode,
            mathCategory = mathCategory,
            useWebSearch = useWebSearch
        )

        var lastError = "Unable to connect to the AI service."

        for (model in candidateModels) {
            coroutineContext.ensureActive()

            // Try with requested webSearch setting first; if webSearch causes HTTP 400 on a tier, retry without tool
            val searchAttempts = if (useWebSearch) listOf(true, false) else listOf(false)

            for (attemptSearch in searchAttempts) {
                coroutineContext.ensureActive()
                val requestJson = buildRequestJson(
                    history = history,
                    userPrompt = userPrompt,
                    pendingAttachment = pendingAttachment,
                    systemInstruction = systemInstruction,
                    useWebSearch = attemptSearch
                )

                val streamResult = executeSseStreamRequest(
                    model = model,
                    apiKey = apiKey,
                    requestBodyJson = requestJson,
                    attemptSearch = attemptSearch,
                    onChunk = onChunk
                )

                if (!streamResult.isError && streamResult.text.isNotBlank()) {
                    return@withContext streamResult
                }

                // If SSE stream returned empty or failed, try standard generateContent once for this model
                coroutineContext.ensureActive()
                val singleResult = executeSingleGenerateContent(
                    model = model,
                    apiKey = apiKey,
                    requestBodyJson = requestJson,
                    attemptSearch = attemptSearch,
                    onChunk = onChunk
                )
                if (!singleResult.isError && singleResult.text.isNotBlank()) {
                    return@withContext singleResult
                }

                lastError = singleResult.text
            }
        }

        AiGenerationResult(
            text = lastError,
            modelUsed = candidateModels.first(),
            usedWebSearch = false,
            webSources = emptyList(),
            isError = true
        )
    }

    /**
     * Lightweight diagnostic check used by Settings -> "Verify API Connection".
     */
    suspend fun verifyConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = ApiConfig.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext false to "GEMINI_API_KEY is empty. Configure it via the AI Studio Secrets panel."
        }
        val startMs = System.currentTimeMillis()
        return@withContext try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Reply with the single word: Connected"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 16)
                    put("temperature", 0.1)
                })
            }
            val url = "${ApiConfig.BACKEND_BASE_URL}v1beta/models/${ApiConfig.MODEL_FAST_MULTIMODAL}:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startMs
                if (response.isSuccessful) {
                    true to "Connected to ${ApiConfig.MODEL_FAST_MULTIMODAL} (${elapsed}ms latency)"
                } else {
                    val errBody = response.body?.string().orEmpty()
                    val parsedMsg = extractApiErrorMessage(errBody, response.code)
                    false to "HTTP ${response.code}: $parsedMsg"
                }
            }
        } catch (e: Exception) {
            false to "Network error: ${e.localizedMessage ?: "Connection failed"}"
        }
    }

    private fun buildRequestJson(
        history: List<ChatMessageEntity>,
        userPrompt: String,
        pendingAttachment: PendingAttachment?,
        systemInstruction: String,
        useWebSearch: Boolean
    ): JSONObject {
        val root = JSONObject()

        // 1. System Instruction
        root.put(
            "systemInstruction",
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", systemInstruction))
            )
        )

        // 2. Conversation History (capped to recent context window turns, excluding error messages)
        val contentsArray = JSONArray()
        val validHistory = history
            .filter { !it.isError && it.content.isNotBlank() }
            .takeLast(ApiConfig.MAX_CONVERSATION_CONTEXT_TURNS)

        for (msg in validHistory) {
            val role = if (msg.isUser) "user" else "model"
            val partsArray = JSONArray()

            // Include inline image data if message had an image within recent 3 turns
            if (msg.isUser && !msg.attachmentBase64.isNullOrBlank() && !msg.attachmentMimeType.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", msg.attachmentMimeType)
                    put("data", msg.attachmentBase64)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            partsArray.put(JSONObject().put("text", msg.content))
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", partsArray)
                }
            )
        }

        // 3. Current User Turn
        val currentParts = JSONArray()
        if (pendingAttachment != null) {
            if (!pendingAttachment.base64Data.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", pendingAttachment.mimeType)
                    put("data", pendingAttachment.base64Data)
                }
                currentParts.put(JSONObject().put("inlineData", inlineData))
            }
            if (!pendingAttachment.extractedText.isNullOrBlank()) {
                val docContext = buildString {
                    appendLine("--- ATTACHED DOCUMENT: ${pendingAttachment.fileName} (${pendingAttachment.mimeType}) ---")
                    appendLine(pendingAttachment.extractedText.take(16_000))
                    appendLine("--- END OF ATTACHED DOCUMENT ---")
                }
                currentParts.put(JSONObject().put("text", docContext))
            }
        }

        val finalPromptText = userPrompt.take(ApiConfig.MAX_PROMPT_LENGTH_CHARS).ifBlank {
            if (pendingAttachment?.isImage == true) {
                "Analyze this image thoroughly. If it contains questions, equations, diagrams, charts, or study notes, explain or solve them step-by-step."
            } else {
                "Analyze and summarize the attached document."
            }
        }
        currentParts.put(JSONObject().put("text", finalPromptText))

        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            }
        )

        root.put("contents", contentsArray)

        // 4. Generation Config
        root.put(
            "generationConfig",
            JSONObject().apply {
                put("temperature", 0.65)
                put("topP", 0.95)
                put("topK", 40)
            }
        )

        // 5. Optional Web Search Grounding Tool
        if (useWebSearch) {
            val toolsArray = JSONArray().apply {
                put(JSONObject().put("google_search", JSONObject()))
            }
            root.put("tools", toolsArray)
        }

        return root
    }

    private suspend fun executeSseStreamRequest(
        model: String,
        apiKey: String,
        requestBodyJson: JSONObject,
        attemptSearch: Boolean,
        onChunk: suspend (String, String) -> Unit
    ): AiGenerationResult {
        val url = "${ApiConfig.BACKEND_BASE_URL}v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val accumulated = StringBuilder()
        val discoveredSources = mutableListOf<WebSource>()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errStr = response.body?.string().orEmpty()
                    return AiGenerationResult(
                        text = extractApiErrorMessage(errStr, response.code),
                        modelUsed = model,
                        usedWebSearch = false,
                        webSources = emptyList(),
                        isError = true
                    )
                }

                val body = response.body ?: return AiGenerationResult(
                    text = "Empty response stream from server.",
                    modelUsed = model,
                    usedWebSearch = false,
                    webSources = emptyList(),
                    isError = true
                )

                BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        coroutineContext.ensureActive()
                        val trimmed = line?.trim() ?: continue
                        if (!trimmed.startsWith("data:")) continue
                        val payloadStr = trimmed.removePrefix("data:").trim()
                        if (payloadStr.isEmpty() || payloadStr == "[DONE]") continue

                        try {
                            val chunkJson = JSONObject(payloadStr)
                            val chunkText = extractCandidateText(chunkJson)
                            if (chunkText.isNotEmpty()) {
                                accumulated.append(chunkText)
                                onChunk(accumulated.toString(), model)
                            }
                            val chunkSources = extractGroundingSources(chunkJson)
                            for (src in chunkSources) {
                                if (discoveredSources.none { it.uri == src.uri }) {
                                    discoveredSources.add(src)
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore malformed intermediate SSE line
                        }
                    }
                }

                val finalStr = accumulated.toString().trim()
                if (finalStr.isEmpty()) {
                    AiGenerationResult(
                        text = "",
                        modelUsed = model,
                        usedWebSearch = attemptSearch,
                        webSources = discoveredSources,
                        isError = true
                    )
                } else {
                    AiGenerationResult(
                        text = finalStr,
                        modelUsed = model,
                        usedWebSearch = attemptSearch || discoveredSources.isNotEmpty(),
                        webSources = discoveredSources,
                        isError = false
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AiGenerationResult(
                text = "Network error while communicating with Gemini API: ${e.localizedMessage ?: "Check internet connection"}",
                modelUsed = model,
                usedWebSearch = false,
                webSources = emptyList(),
                isError = true
            )
        }
    }

    private suspend fun executeSingleGenerateContent(
        model: String,
        apiKey: String,
        requestBodyJson: JSONObject,
        attemptSearch: Boolean,
        onChunk: suspend (String, String) -> Unit
    ): AiGenerationResult {
        val url = "${ApiConfig.BACKEND_BASE_URL}v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return AiGenerationResult(
                        text = extractApiErrorMessage(rawBody, response.code),
                        modelUsed = model,
                        usedWebSearch = false,
                        webSources = emptyList(),
                        isError = true
                    )
                }

                val json = JSONObject(rawBody)
                val text = extractCandidateText(json).trim()
                val sources = extractGroundingSources(json)
                if (text.isNotEmpty()) {
                    onChunk(text, model)
                    AiGenerationResult(
                        text = text,
                        modelUsed = model,
                        usedWebSearch = attemptSearch || sources.isNotEmpty(),
                        webSources = sources,
                        isError = false
                    )
                } else {
                    AiGenerationResult(
                        text = "The model returned an empty response. Please try rephrasing your question.",
                        modelUsed = model,
                        usedWebSearch = false,
                        webSources = emptyList(),
                        isError = true
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AiGenerationResult(
                text = "Connection error: ${e.localizedMessage ?: "Unable to reach AI server"}",
                modelUsed = model,
                usedWebSearch = false,
                webSources = emptyList(),
                isError = true
            )
        }
    }

    private fun extractCandidateText(root: JSONObject): String {
        val candidates = root.optJSONArray("candidates") ?: return ""
        val firstCandidate = candidates.optJSONObject(0) ?: return ""
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            // Skip internal thought parts if marked
            if (part.optBoolean("thought", false)) continue
            val text = part.optString("text", "")
            if (text.isNotEmpty()) {
                sb.append(text)
            }
        }
        return sb.toString()
    }

    private fun extractGroundingSources(root: JSONObject): List<WebSource> {
        val sources = mutableListOf<WebSource>()
        val candidates = root.optJSONArray("candidates") ?: return sources
        val firstCandidate = candidates.optJSONObject(0) ?: return sources
        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata") ?: return sources
        val chunks = groundingMetadata.optJSONArray("groundingChunks") ?: return sources
        for (i in 0 until chunks.length()) {
            val chunk = chunks.optJSONObject(i) ?: continue
            val web = chunk.optJSONObject("web") ?: continue
            val uri = web.optString("uri", "")
            val title = web.optString("title", "Web Source")
            if (uri.isNotBlank() && sources.none { it.uri == uri }) {
                sources.add(WebSource(title = title, uri = uri))
            }
        }
        return sources
    }

    private fun extractApiErrorMessage(rawBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(rawBody)
            val errObj = json.optJSONObject("error")
            val message = errObj?.optString("message").orEmpty()
            if (message.isNotBlank()) {
                "API Error ($statusCode): $message"
            } else {
                "Server returned HTTP $statusCode."
            }
        } catch (_: Exception) {
            when (statusCode) {
                400 -> "Bad request (HTTP 400). Verify the prompt or attached file format."
                401, 403 -> "Authentication error (HTTP $statusCode). Please verify your GEMINI_API_KEY in the AI Studio Secrets panel."
                429 -> "Quota or rate limit exceeded (HTTP 429). Please wait a moment and try again."
                else -> "Request failed with HTTP $statusCode."
            }
        }
    }
}
