package com.example.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AppLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    val speakingMessageId: StateFlow<Long?> = _speakingMessageId.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            _speakingMessageId.value = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            _speakingMessageId.value = null
                        }
                    })
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun speakText(
        text: String,
        messageId: Long? = null,
        language: AppLanguage = AppLanguage.ENGLISH,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        val engine = tts ?: return
        if (!isTtsReady) return

        // Toggle off if already speaking the same message
        if (_isSpeaking.value && messageId != null && _speakingMessageId.value == messageId) {
            stopSpeaking()
            return
        }

        val locale = when (language) {
            AppLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AppLanguage.HINGLISH -> Locale.forLanguageTag("en-IN")
            AppLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
        }
        engine.language = locale
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))

        // Strip markdown syntax for natural speech
        val cleanSpeech = text
            .replace(Regex("```[\\s\\S]*?```"), " Code block omitted. ")
            .replace(Regex("[*_#`~>]"), "")
            .take(3500)

        _speakingMessageId.value = messageId
        _isSpeaking.value = true
        engine.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "sutra_msg_${messageId ?: 0L}")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    fun shutdown() {
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun createSpeechRecognizerIntent(language: AppLanguage): Intent {
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    when (language) {
                        AppLanguage.HINDI -> "बोलिए (Speak in Hindi or English)…"
                        AppLanguage.HINGLISH -> "Boliye (Speak in Hinglish, Hindi, or English)…"
                        AppLanguage.ENGLISH -> "Speak your question or equation…"
                    }
                )
            }
        }
    }
}
