package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.UserProfileEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val label: String
) {
    JSON("json", "application/json", "JSON Backup (.json)"),
    TEXT("txt", "text/plain", "Readable Text (.txt)")
}

data class ExportResult(
    val success: Boolean,
    val format: ExportFormat,
    val fileName: String,
    val filePathOrUri: String,
    val conversationCount: Int,
    val messageCount: Int,
    val contentPreview: String,
    val errorMessage: String? = null
)

object BackupExportHelper {

    fun generateBackupPayload(
        profile: UserProfileEntity,
        conversationsWithMessages: List<Pair<ConversationEntity, List<ChatMessageEntity>>>,
        format: ExportFormat
    ): String {
        return when (format) {
            ExportFormat.JSON -> buildJsonExport(profile, conversationsWithMessages)
            ExportFormat.TEXT -> buildTextExport(profile, conversationsWithMessages)
        }
    }

    /**
     * One-click local file export to the app's dedicated local backups directory
     * (or to a user-selected SAF [targetUri] if provided).
     */
    suspend fun exportHistoryToFile(
        context: Context,
        profile: UserProfileEntity,
        conversationsWithMessages: List<Pair<ConversationEntity, List<ChatMessageEntity>>>,
        format: ExportFormat,
        targetUri: Uri? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "sutra_ai_history_$timestampStr.${format.extension}"
        val payload = generateBackupPayload(profile, conversationsWithMessages, format)
        val totalMessages = conversationsWithMessages.sumOf { it.second.size }

        try {
            if (targetUri != null) {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: return@withContext ExportResult(
                    success = false,
                    format = format,
                    fileName = fileName,
                    filePathOrUri = targetUri.toString(),
                    conversationCount = conversationsWithMessages.size,
                    messageCount = totalMessages,
                    contentPreview = "",
                    errorMessage = "Unable to open selected destination file."
                )

                return@withContext ExportResult(
                    success = true,
                    format = format,
                    fileName = fileName,
                    filePathOrUri = targetUri.toString(),
                    conversationCount = conversationsWithMessages.size,
                    messageCount = totalMessages,
                    contentPreview = payload.take(2000)
                )
            }

            // Default One-Click Local Export to device app-specific backup storage
            val baseDir = context.getExternalFilesDir("backups")
                ?: File(context.filesDir, "backups")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            val outFile = File(baseDir, fileName)
            outFile.writeText(payload, Charsets.UTF_8)

            ExportResult(
                success = true,
                format = format,
                fileName = fileName,
                filePathOrUri = outFile.absolutePath,
                conversationCount = conversationsWithMessages.size,
                messageCount = totalMessages,
                contentPreview = payload.take(2000)
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                format = format,
                fileName = fileName,
                filePathOrUri = "",
                conversationCount = conversationsWithMessages.size,
                messageCount = totalMessages,
                contentPreview = "",
                errorMessage = e.localizedMessage ?: "Export failed"
            )
        }
    }

    private fun buildJsonExport(
        profile: UserProfileEntity,
        conversationsWithMessages: List<Pair<ConversationEntity, List<ChatMessageEntity>>>
    ): String {
        val root = JSONObject()
        root.put("app", "Sutra AI")
        root.put("backupVersion", 1)
        root.put("exportedAtIso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        val profileJson = JSONObject().apply {
            put("id", profile.id)
            put("username", profile.username)
            put("roleOrFocus", profile.roleOrFocus)
            put("bioOrContext", profile.bioOrContext)
            put("preferredLanguage", profile.preferredLanguage)
            put("themeMode", profile.themeMode)
            put("textSize", profile.textSize)
            put("customAttributes", JSONObject(profile.getExtensibleAttributesMap()))
        }
        root.put("activeProfile", profileJson)

        val convosArray = JSONArray()
        for ((convo, messages) in conversationsWithMessages) {
            val convoObj = JSONObject().apply {
                put("id", convo.id)
                put("title", convo.title)
                put("assistantMode", convo.assistantMode)
                put("isPinned", convo.isPinned)
                put("createdAt", convo.createdAt)
                put("updatedAt", convo.updatedAt)

                val msgsArray = JSONArray()
                for (msg in messages) {
                    val msgObj = JSONObject().apply {
                        put("id", msg.id)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("timestamp", msg.timestamp)
                        put("modelUsed", msg.modelUsed)
                        put("isMathSolution", msg.isMathSolution)
                        put("usedWebSearch", msg.usedWebSearch)
                        if (!msg.attachmentName.isNullOrBlank()) {
                            put("attachmentName", msg.attachmentName)
                            put("attachmentMimeType", msg.attachmentMimeType)
                        }
                    }
                    msgsArray.put(msgObj)
                }
                put("messages", msgsArray)
            }
            convosArray.put(convoObj)
        }
        root.put("conversations", convosArray)

        return root.toString(2)
    }

    private fun buildTextExport(
        profile: UserProfileEntity,
        conversationsWithMessages: List<Pair<ConversationEntity, List<ChatMessageEntity>>>
    ): String {
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        return buildString {
            appendLine("======================================================================")
            appendLine("SUTRA AI — CONVERSATION HISTORY BACKUP")
            appendLine("======================================================================")
            appendLine("Exported On : ${dateFormatter.format(Date())}")
            appendLine("User Profile: ${profile.username} (${profile.roleOrFocus})")
            appendLine("Language    : ${profile.parsedLanguage.displayName}")
            appendLine("Total Chats : ${conversationsWithMessages.size}")
            appendLine("======================================================================")
            appendLine()

            if (conversationsWithMessages.isEmpty()) {
                appendLine("(No saved conversations at time of export.)")
            } else {
                conversationsWithMessages.forEachIndexed { index, (convo, messages) ->
                    appendLine("----------------------------------------------------------------------")
                    appendLine("CHAT #${index + 1}: ${convo.title}")
                    appendLine("Mode: ${convo.assistantMode} | Updated: ${dateFormatter.format(Date(convo.updatedAt))}")
                    appendLine("----------------------------------------------------------------------")
                    messages.forEach { msg ->
                        val speaker = if (msg.isUser) profile.username else "Sutra AI (${msg.modelUsed})"
                        val time = dateFormatter.format(Date(msg.timestamp))
                        appendLine("[$time] $speaker:")
                        if (!msg.attachmentName.isNullOrBlank()) {
                            appendLine("  [Attachment: ${msg.attachmentName}]")
                        }
                        appendLine(msg.content)
                        appendLine()
                    }
                    appendLine()
                }
            }
        }
    }
}
