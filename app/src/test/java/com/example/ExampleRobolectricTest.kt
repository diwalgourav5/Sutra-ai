package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.UserProfileEntity
import com.example.util.BackupExportHelper
import com.example.util.ExportFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sutra AI", appName)
    }

    @Test
    fun `profile extensible attributes serialize and deserialize`() {
        val attrs = mapOf(
            "Target Exam" to "JEE Advanced",
            "Preferred Language" to "Hinglish"
        )
        val encoded = UserProfileEntity.encodeExtensibleAttributes(attrs)
        val profile = UserProfileEntity(
            username = "Rohan",
            extensibleAttributesJson = encoded
        )
        val decoded = profile.getExtensibleAttributesMap()
        assertEquals("JEE Advanced", decoded["Target Exam"])
        assertEquals("Hinglish", decoded["Preferred Language"])
    }

    @Test
    fun `one click export history generates valid json and text backup files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = UserProfileEntity(username = "Aarav Sharma")
        val convo = ConversationEntity(id = 1L, title = "Calculus Integral Test")
        val msgUser = ChatMessageEntity(
            id = 10L,
            conversationId = 1L,
            role = "user",
            content = "Solve integral of x * sin(x) dx"
        )
        val msgAi = ChatMessageEntity(
            id = 11L,
            conversationId = 1L,
            role = "model",
            content = "Step 1: Use integration by parts: sin(x) - x*cos(x) + C"
        )
        val data = listOf(convo to listOf(msgUser, msgAi))

        val jsonExport = BackupExportHelper.exportHistoryToFile(
            context = context,
            profile = profile,
            conversationsWithMessages = data,
            format = ExportFormat.JSON
        )
        assertTrue(jsonExport.success)
        assertEquals(1, jsonExport.conversationCount)
        assertEquals(2, jsonExport.messageCount)
        assertTrue(jsonExport.contentPreview.contains("Calculus Integral Test"))

        val txtExport = BackupExportHelper.exportHistoryToFile(
            context = context,
            profile = profile,
            conversationsWithMessages = data,
            format = ExportFormat.TEXT
        )
        assertTrue(txtExport.success)
        assertTrue(txtExport.contentPreview.contains("SUTRA AI"))
    }

    @Test
    fun `image aspect ratio options map correctly to Gemini image config values`() {
        val square = com.example.data.model.ImageAspectRatioOption.SQUARE_1_1
        val landscape = com.example.data.model.ImageAspectRatioOption.LANDSCAPE_16_9
        val portrait = com.example.data.model.ImageAspectRatioOption.PORTRAIT_9_16

        assertEquals("1:1", square.apiValue)
        assertEquals("16:9", landscape.apiValue)
        assertEquals("9:16", portrait.apiValue)

        assertEquals(square, com.example.data.model.ImageAspectRatioOption.fromApiValue("1:1"))
        assertEquals(landscape, com.example.data.model.ImageAspectRatioOption.fromApiValue("16:9"))
        assertEquals(portrait, com.example.data.model.ImageAspectRatioOption.fromApiValue("9:16"))
        assertEquals(square, com.example.data.model.ImageAspectRatioOption.fromApiValue("unsupported"))
    }

    @Test
    fun `generated image entity instantiates with Gemini model and default parameters`() {
        val image = com.example.data.model.GeneratedImageEntity(
            prompt = "A cute robotic puppy playing in flowers",
            aspectRatio = "1:1",
            modelUsed = "gemini-3.1-flash-image",
            imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            mimeType = "image/png"
        )
        assertEquals("gemini-3.1-flash-image", image.modelUsed)
        assertEquals(com.example.data.model.ImageAspectRatioOption.SQUARE_1_1, image.parsedAspectRatio)
        assertTrue(image.prompt.isNotBlank())
    }
}
