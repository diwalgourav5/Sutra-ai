package com.example.data.remote

import com.example.BuildConfig
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ============================================================================
 * SUTRA AI — BACKEND & API CONFIGURATION SYSTEM
 * ============================================================================
 *
 * HOW TO CONFIGURE CREDENTIALS & BACKEND CONNECTION:
 *
 * 1. AI STUDIO SECRETS PANEL (Default / Recommended for AI Studio):
 *    - Open the "Secrets" panel in the Google AI Studio UI.
 *    - Add or update the secret named `GEMINI_API_KEY`.
 *    - The Secrets Gradle Plugin reads `.env` / `.env.example` at build time
 *      and injects `BuildConfig.GEMINI_API_KEY` automatically.
 *    - NEVER hardcode raw API keys inside Kotlin source files or `strings.xml`.
 *
 * 2. CUSTOM SECURE BACKEND PROXY URL (For Production / Enterprise Deployment):
 *    - To route requests through your own backend server (e.g. Cloud Run,
 *      Firebase Functions, or Node/Go API gateway that holds the API key
 *      server-side and enforces server-side auth/quotas), update
 *      [BACKEND_BASE_URL] below to point to your proxy endpoint.
 * ============================================================================
 */
object ApiConfig {

    /**
     * Base URL for Gemini REST API or your custom backend proxy server.
     * Must end with a trailing slash `/`.
     */
    const val BACKEND_BASE_URL: String = "https://generativelanguage.googleapis.com/"

    /**
     * Approved Gemini Models per AI Studio guidelines:
     * - [MODEL_FAST_MULTIMODAL]: Default for fast chat, summarization, translation & vision.
     * - [MODEL_COMPLEX_REASONING]: Used for step-by-step Math Solver, Calculus, Matrices & Coding.
     * - [MODEL_FALLBACK]: Fallback alias if a preview model encounters temporary quota limits.
     */
    const val MODEL_FAST_MULTIMODAL: String = "gemini-3.5-flash"
    const val MODEL_COMPLEX_REASONING: String = "gemini-3.1-pro-preview"
    const val MODEL_FALLBACK: String = "gemini-flash-latest"
    const val MODEL_IMAGE_GENERATION: String = "gemini-3.1-flash-image"
    const val MODEL_IMAGE_FALLBACK: String = "gemini-2.5-flash-image"

    /**
     * Client-side request validation and rate limiting constants.
     */
    const val MAX_REQUESTS_PER_MINUTE: Int = 15
    const val MAX_PROMPT_LENGTH_CHARS: Int = 24_000
    const val MAX_CONVERSATION_CONTEXT_TURNS: Int = 16
    const val MAX_ATTACHMENT_SIZE_BYTES: Long = 10L * 1024L * 1024L // 10 MB

    /**
     * Safely retrieves the configured API key from BuildConfig.
     */
    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Checks whether a valid non-placeholder API key is present.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() &&
            key != "MY_GEMINI_API_KEY" &&
            key != "YOUR_API_KEY" &&
            !key.startsWith("PLACEHOLDER")
    }

    // --- Client-Side Sliding Window Rate Limiter ---
    private val requestTimestamps = ConcurrentLinkedQueue<Long>()

    @Synchronized
    fun checkRateLimitAndRecord(): RateLimitResult {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L
        while (true) {
            val head = requestTimestamps.peek() ?: break
            if (head < oneMinuteAgo) {
                requestTimestamps.poll()
            } else {
                break
            }
        }
        val currentCount = requestTimestamps.size
        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            val oldest = requestTimestamps.peek() ?: now
            val waitSeconds = ((oldest + 60_000L - now) / 1000L).coerceAtLeast(1L)
            return RateLimitResult(
                allowed = false,
                requestsInWindow = currentCount,
                retryAfterSeconds = waitSeconds
            )
        }
        requestTimestamps.add(now)
        return RateLimitResult(
            allowed = true,
            requestsInWindow = currentCount + 1,
            retryAfterSeconds = 0L
        )
    }

    fun getCurrentWindowRequestCount(): Int {
        val oneMinuteAgo = System.currentTimeMillis() - 60_000L
        return requestTimestamps.count { it >= oneMinuteAgo }
    }

    /**
     * Optional Privacy Filter: Redacts sensitive patterns (credit cards, phone numbers,
     * email addresses, or raw secret keys) when the user enables Privacy Masking in Settings.
     */
    fun maskSensitiveTextIfEnabled(input: String, maskEnabled: Boolean): String {
        if (!maskEnabled || input.isBlank()) return input
        var sanitized = input
        // Mask email addresses
        sanitized = sanitized.replace(
            Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),
            "[REDACTED_EMAIL]"
        )
        // Mask 10-to-16 digit phone / card sequences
        sanitized = sanitized.replace(
            Regex("\\b(?:\\+?\\d{1,3}[ -]?)?(?:\\d[ -]?){10,16}\\b"),
            "[REDACTED_NUMBER]"
        )
        // Mask Google API keys if accidentally pasted
        sanitized = sanitized.replace(
            Regex("AIza[0-9A-Za-z\\-_]{35}"),
            "[REDACTED_API_KEY]"
        )
        return sanitized
    }
}

data class RateLimitResult(
    val allowed: Boolean,
    val requestsInWindow: Int,
    val retryAfterSeconds: Long
)
