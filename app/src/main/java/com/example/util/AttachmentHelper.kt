package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.PendingAttachment
import com.example.data.remote.ApiConfig
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AttachmentHelper {

    private val supportedTextExtensions = setOf(
        "txt", "md", "csv", "json", "xml", "html", "kt", "java",
        "py", "js", "ts", "c", "cpp", "h", "sql", "yaml", "yml", "log"
    )

    private val supportedBinaryMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heic",
        "image/heif",
        "application/pdf"
    )

    suspend fun processCameraBitmap(
        bitmap: Bitmap,
        onProgress: suspend (Float) -> Unit
    ): PendingAttachment = withContext(Dispatchers.IO) {
        onProgress(0.25f)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        onProgress(0.65f)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        onProgress(1.0f)
        PendingAttachment(
            uriString = "camera://captured_${System.currentTimeMillis()}.jpg",
            fileName = "Camera_Capture_${System.currentTimeMillis() % 10000}.jpg",
            mimeType = "image/jpeg",
            fileSizeBytes = bytes.size.toLong(),
            base64Data = base64,
            extractedText = null,
            uploadProgress = 1.0f,
            isImage = true,
            errorMessage = null
        )
    }

    suspend fun processUriAttachment(
        context: Context,
        uri: Uri,
        onProgress: suspend (Float) -> Unit
    ): PendingAttachment = withContext(Dispatchers.IO) {
        onProgress(0.15f)
        val contentResolver = context.contentResolver
        val (fileName, fileSize) = queryFileNameAndSize(context, uri)
        val rawMime = contentResolver.getType(uri)?.lowercase() ?: guessMimeType(fileName)
        val ext = fileName.substringAfterLast('.', "").lowercase()

        if (fileSize > ApiConfig.MAX_ATTACHMENT_SIZE_BYTES) {
            val sizeMb = String.format("%.1f", fileSize / (1024f * 1024f))
            return@withContext PendingAttachment(
                uriString = uri.toString(),
                fileName = fileName,
                mimeType = rawMime,
                fileSizeBytes = fileSize,
                uploadProgress = 1.0f,
                isImage = rawMime.startsWith("image/"),
                errorMessage = "File '$fileName' ($sizeMb MB) exceeds the 10 MB maximum upload limit."
            )
        }

        delay(120)
        onProgress(0.45f)

        // 1. Image attachment
        if (rawMime.startsWith("image/")) {
            return@withContext try {
                val base64 = contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    if (originalBitmap != null) {
                        val scaled = scaleDownBitmap(originalBitmap, 1280)
                        val bos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                        Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                    } else {
                        null
                    }
                }
                onProgress(1.0f)
                if (base64.isNullOrBlank()) {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = rawMime,
                        fileSizeBytes = fileSize,
                        uploadProgress = 1.0f,
                        isImage = true,
                        errorMessage = "Could not decode image '$fileName'. Please select a valid JPEG, PNG, or WebP image."
                    )
                } else {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = "image/jpeg",
                        fileSizeBytes = fileSize,
                        base64Data = base64,
                        uploadProgress = 1.0f,
                        isImage = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                PendingAttachment(
                    uriString = uri.toString(),
                    fileName = fileName,
                    mimeType = rawMime,
                    fileSizeBytes = fileSize,
                    uploadProgress = 1.0f,
                    isImage = true,
                    errorMessage = "Failed to read image: ${e.localizedMessage ?: "Read error"}"
                )
            }
        }

        // 2. PDF attachment (supported inline via Gemini multimodal inlineData)
        if (rawMime == "application/pdf" || ext == "pdf") {
            return@withContext try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                onProgress(0.85f)
                if (bytes == null || bytes.isEmpty()) {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = "application/pdf",
                        fileSizeBytes = fileSize,
                        uploadProgress = 1.0f,
                        isImage = false,
                        errorMessage = "The selected PDF file appears to be empty."
                    )
                } else if (bytes.size > 6 * 1024 * 1024) {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = "application/pdf",
                        fileSizeBytes = bytes.size.toLong(),
                        uploadProgress = 1.0f,
                        isImage = false,
                        errorMessage = "PDF '$fileName' is too large for inline analysis (max 6 MB for PDFs)."
                    )
                } else {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onProgress(1.0f)
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = "application/pdf",
                        fileSizeBytes = bytes.size.toLong(),
                        base64Data = base64,
                        uploadProgress = 1.0f,
                        isImage = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                PendingAttachment(
                    uriString = uri.toString(),
                    fileName = fileName,
                    mimeType = "application/pdf",
                    fileSizeBytes = fileSize,
                    uploadProgress = 1.0f,
                    isImage = false,
                    errorMessage = "Error reading PDF: ${e.localizedMessage}"
                )
            }
        }

        // 3. Text / Code / Markdown / CSV / JSON documents
        if (rawMime.startsWith("text/") || rawMime == "application/json" || ext in supportedTextExtensions) {
            return@withContext try {
                val content = contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }.orEmpty()
                onProgress(1.0f)
                if (content.isBlank()) {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = rawMime,
                        fileSizeBytes = fileSize,
                        uploadProgress = 1.0f,
                        isImage = false,
                        errorMessage = "Document '$fileName' is empty."
                    )
                } else {
                    PendingAttachment(
                        uriString = uri.toString(),
                        fileName = fileName,
                        mimeType = rawMime.ifBlank { "text/plain" },
                        fileSizeBytes = fileSize,
                        extractedText = content.take(20_000),
                        uploadProgress = 1.0f,
                        isImage = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                PendingAttachment(
                    uriString = uri.toString(),
                    fileName = fileName,
                    mimeType = rawMime,
                    fileSizeBytes = fileSize,
                    uploadProgress = 1.0f,
                    isImage = false,
                    errorMessage = "Failed to read document '$fileName': ${e.localizedMessage}"
                )
            }
        }

        // 4. Unsupported file type
        onProgress(1.0f)
        PendingAttachment(
            uriString = uri.toString(),
            fileName = fileName,
            mimeType = rawMime,
            fileSizeBytes = fileSize,
            uploadProgress = 1.0f,
            isImage = false,
            errorMessage = "Unsupported file format ($rawMime). Supported formats: Images (JPG, PNG, WebP), PDF, and Text/Code files (.txt, .md, .csv, .json, .py, .kt, .js)."
        )
    }

    private fun queryFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "md", "txt", "csv", "kt", "py", "js", "java", "html", "xml" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (ratio > 1f) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
