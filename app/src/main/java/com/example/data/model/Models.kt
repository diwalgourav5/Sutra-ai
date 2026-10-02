package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode");

    companion object {
        fun fromName(name: String?): AppThemeMode =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AppLanguage(
    val displayName: String,
    val nativeBadge: String,
    val localeTag: String,
    val systemPromptDirective: String
) {
    ENGLISH(
        displayName = "English",
        nativeBadge = "EN",
        localeTag = "en-IN",
        systemPromptDirective = "Respond clearly in English unless the user explicitly writes in or asks for another language."
    ),
    HINDI(
        displayName = "हिन्दी (Hindi)",
        nativeBadge = "हिं",
        localeTag = "hi-IN",
        systemPromptDirective = "Respond naturally in Hindi (Devanagari script: हिन्दी), keeping technical terms, code snippets, and mathematical formulas clear and accurate."
    ),
    HINGLISH(
        displayName = "Hinglish (हिंग्लिश)",
        nativeBadge = "HI-EN",
        localeTag = "en-IN",
        systemPromptDirective = "Respond naturally in conversational Hinglish (Hindi written in Roman/Latin script blended smoothly with English technical/academic terms, e.g., 'Chaliye is problem ko step-by-step solve karte hain')."
    );

    companion object {
        fun fromName(name: String?): AppLanguage =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: ENGLISH
    }
}

enum class TextSizeOption(val displayName: String, val scaleFactor: Float) {
    SMALL("Small (88%)", 0.88f),
    MEDIUM("Default (100%)", 1.0f),
    LARGE("Large (115%)", 1.15f),
    EXTRA_LARGE("Extra Large (130%)", 1.30f);

    companion object {
        fun fromName(name: String?): TextSizeOption =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}

enum class AssistantMode(
    val title: String,
    val shortTitle: String,
    val description: String,
    val systemDirective: String,
    val prefersProModel: Boolean
) {
    GENERAL(
        title = "General Assistant",
        shortTitle = "General",
        description = "Balanced conversational help for everyday questions, ideas & analysis",
        systemDirective = "Act as an intelligent, versatile conversational assistant. Understand the user's intent carefully, structure answers cleanly with headings and bullet points where helpful, and ask for clarification if a prompt is ambiguous.",
        prefersProModel = false
    ),
    STEP_BY_STEP_TUTOR(
        title = "Step-by-Step Study Tutor",
        shortTitle = "Study Tutor",
        description = "Breaks down school & college concepts into intuitive steps with examples",
        systemDirective = "Act as a patient, rigorous academic tutor for school and university students. Break down complex concepts into numbered steps, provide intuitive analogies, highlight key definitions/formulas, and include a quick comprehension check.",
        prefersProModel = true
    ),
    MATH_SOLVER(
        title = "Mathematics & Logic Solver",
        shortTitle = "Math Solver",
        description = "Shows complete derivations, formulas, matrices & verified calculations",
        systemDirective = "Act as an expert mathematician and logic solver. Always state the Given data, Formula/Theorem used, complete Step-by-Step Derivation showing every intermediate calculation clearly, and a clearly boxed Final Answer with verification.",
        prefersProModel = true
    ),
    CODE_ARCHITECT(
        title = "Programming & Debugger",
        shortTitle = "Code & Debug",
        description = "Writes clean code, explains algorithms, and fixes bugs with root-cause analysis",
        systemDirective = "Act as a staff software engineer. Provide clean, idiomatic, well-commented code blocks, explain time/space complexity, identify root causes when debugging, and point out edge cases.",
        prefersProModel = true
    ),
    WRITER_TRANSLATOR(
        title = "Writer, Summarizer & Translator",
        shortTitle = "Write & Translate",
        description = "Drafts essays, rewrites tone, summarizes documents & translates EN/HI/Hinglish",
        systemDirective = "Act as an expert editor, writer, and multilingual translator fluent in English, Hindi, and Hinglish. Preserve nuance, tone, and formatting when writing, summarizing, or translating.",
        prefersProModel = false
    );

    companion object {
        fun fromName(name: String?): AssistantMode =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: GENERAL
    }
}

enum class MathCategory(
    val title: String,
    val symbolBadge: String,
    val subtitle: String,
    val quickSymbols: List<String>,
    val sampleProblems: List<String>
) {
    ARITHMETIC(
        title = "Arithmetic",
        symbolBadge = "÷×",
        subtitle = "Fractions, percentages, ratios, number theory & order of operations",
        quickSymbols = listOf("+", "-", "×", "÷", "%", "^", "√(", "(", ")", "="),
        sampleProblems = listOf(
            "Simplify step-by-step: ((3/4 + 5/6) ÷ (7/12)) × 24% of 350",
            "Find the GCD and LCM of 144, 252, and 630 using prime factorization.",
            "A shopkeeper marks goods 40% above cost price and gives a 15% discount. Find the net profit percentage."
        )
    ),
    ALGEBRA(
        title = "Algebra",
        symbolBadge = "x²",
        subtitle = "Linear & quadratic equations, polynomials, inequalities & logarithms",
        quickSymbols = listOf("x", "y", "x²", "x³", "^", "√(", "≤", "≥", "≠", "log(", "ln("),
        sampleProblems = listOf(
            "Solve the quadratic equation 3x² - 14x + 8 = 0 by both factoring and the quadratic formula.",
            "Solve the system of equations: 2x + 3y - z = 12, x - 2y + 4z = -5, 3x + y + 2z = 11",
            "Solve for x: log₂(x - 3) + log₂(x + 1) = 5"
        )
    ),
    GEOMETRY(
        title = "Geometry",
        symbolBadge = "△○",
        subtitle = "Triangles, circles, coordinate geometry, 3D surface area & volume",
        quickSymbols = listOf("π", "°", "∠", "△", "⊥", "∥", "r²", "√(", "(x₁,y₁)"),
        sampleProblems = listOf(
            "Find the equation of the circle passing through (1, 2), (3, -4), and (5, -6), and determine its center and radius.",
            "A right circular cone has base radius 7 cm and slant height 25 cm. Find its height, curved surface area, total surface area, and volume.",
            "In triangle ABC, sides are a = 13 cm, b = 14 cm, c = 15 cm. Find the area using Heron's formula and the inradius."
        )
    ),
    TRIGONOMETRY(
        title = "Trigonometry",
        symbolBadge = "sin θ",
        subtitle = "Identities, inverse trig, heights & distances, and periodic equations",
        quickSymbols = listOf("θ", "π", "sin(", "cos(", "tan(", "sec(", "cot(", "°", "²"),
        sampleProblems = listOf(
            "Find all solutions in [0, 2π) for the equation: 2sin²(θ) - cos(θ) - 1 = 0",
            "Prove the identity step-by-step: (sin θ + cos θ - 1) / (sin θ - cos θ + 1) = (1 - sin θ) / cos θ",
            "From the top of a 60m high building, the angles of depression of the top and bottom of a tower are 30° and 60°. Find the height of the tower."
        )
    ),
    CALCULUS(
        title = "Calculus",
        symbolBadge = "∫dx",
        subtitle = "Limits, derivatives, integration by parts/substitution, differential equations",
        quickSymbols = listOf("∫", "d/dx", "lim", "→", "∞", "∂", "e^x", "ln(x)", "sin(x)", "dx"),
        sampleProblems = listOf(
            "Evaluate the definite integral step-by-step: ∫ from 0 to π/2 of (x · sin(x)) dx",
            "Find the first and second derivatives of f(x) = x² · e^(3x) · ln(x) and locate critical points.",
            "Evaluate the limit: lim (x → 0) [ (e^(2x) - 1 - 2x) / (x · sin(x)) ] using L'Hôpital's Rule."
        )
    ),
    MATRICES(
        title = "Matrices",
        symbolBadge = "[A]",
        subtitle = "Determinants, matrix multiplication, inverse, rank & eigenvalues",
        quickSymbols = listOf("[[", "],[", "]]", "det(A)", "A⁻¹", "Aᵀ", "λ", "×", "I"),
        sampleProblems = listOf(
            "Find the determinant, adjoint, and inverse A⁻¹ of matrix A = [[2, -1, 3], [1, 4, -2], [3, 1, 1]].",
            "Find the eigenvalues and eigenvectors of the matrix M = [[4, 1], [2, 3]].",
            "Multiply matrices A = [[1, 2, 3], [4, 5, 6]] and B = [[7, 8], [9, 1], [2, 3]] showing each dot product."
        )
    ),
    STATISTICS(
        title = "Statistics",
        symbolBadge = "∑σ",
        subtitle = "Mean, variance, standard deviation, Bayes' theorem & distributions",
        quickSymbols = listOf("∑", "μ", "σ", "σ²", "P(A|B)", "nCr", "!", "x̄"),
        sampleProblems = listOf(
            "Calculate the mean, median, variance, and standard deviation for the dataset: 12, 15, 18, 22, 22, 25, 30, 32.",
            "Box A has 4 red and 6 blue balls; Box B has 7 red and 3 blue balls. A box is chosen at random and a red ball is drawn. Using Bayes' theorem, find the probability it came from Box B.",
            "A fair coin is tossed 10 times. Find the binomial probability of getting: (a) exactly 6 heads, (b) at least 8 heads."
        )
    ),
    WORD_PROBLEMS(
        title = "Word Problems",
        symbolBadge = "📝",
        subtitle = "Time & work, speed-distance, mixtures, finance & logical puzzles",
        quickSymbols = listOf("=", "+", "-", "×", "÷", "%", "x", "y", "→"),
        sampleProblems = listOf(
            "Train A leaves Delhi at 6:00 AM at 80 km/h. Train B leaves the same station at 7:30 AM at 110 km/h on a parallel track. At what time and distance will Train B overtake Train A?",
            "Pipe A can fill a tank in 12 hours, Pipe B in 15 hours, while Pipe C can empty the full tank in 20 hours. If all three are opened together, how long will it take to fill the tank?",
            "Is question ko Hinglish mein step-by-step samjhao: ₹25,000 par 12% per annum compound interest (compounded half-yearly) se 1.5 saal mein kitna amount milega?"
        )
    )
}

enum class AvatarPreset(
    val title: String,
    val subtitle: String,
    val emojiBadge: String,
    val primaryHex: Long,
    val usesGeneratedAsset: Boolean = false
) {
    SUTRA_SCHOLAR(
        title = "Sutra Scholar",
        subtitle = "Default AI Studio Illustrated Portrait",
        emojiBadge = "🎓",
        primaryHex = 0xFF2DD4BF,
        usesGeneratedAsset = true
    ),
    QUANTUM_CODER(
        title = "Quantum Architect",
        subtitle = "Systems, Code & Algorithms",
        emojiBadge = "⚡",
        primaryHex = 0xFF818CF8
    ),
    VEDIC_MATHEMATICIAN(
        title = "Vedic Polymath",
        subtitle = "Calculus, Matrices & Logic",
        emojiBadge = "∞",
        primaryHex = 0xFFF59E0B
    ),
    COSMIC_EXPLORER(
        title = "Cosmic Researcher",
        subtitle = "Science, Physics & Deep Inquiry",
        emojiBadge = "🪐",
        primaryHex = 0xFF38BDF8
    ),
    CREATIVE_MUSE(
        title = "Multilingual Writer",
        subtitle = "Literature, Hindi, Hinglish & Ideas",
        emojiBadge = "✒️",
        primaryHex = 0xFFF472B6
    ),
    CUSTOM_PHOTO(
        title = "Custom Photo",
        subtitle = "Uploaded from your device gallery",
        emojiBadge = "📷",
        primaryHex = 0xFF10B981
    );

    companion object {
        fun fromName(name: String?): AvatarPreset =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SUTRA_SCHOLAR
    }
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String = "Aarav Sharma",
    val avatarPreset: String = AvatarPreset.SUTRA_SCHOLAR.name,
    val customAvatarUri: String? = null,
    val roleOrFocus: String = "Student & Developer",
    val bioOrContext: String = "Curious learner exploring mathematics, computer science, and clear step-by-step explanations.",
    val themeMode: String = AppThemeMode.DARK.name,
    val useDynamicColor: Boolean = false,
    val preferredLanguage: String = AppLanguage.ENGLISH.name,
    val textSize: String = TextSizeOption.MEDIUM.name,
    val voiceEnabled: Boolean = true,
    val autoSpeakResponses: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val saveHistoryEnabled: Boolean = true,
    val privacyMaskSensitiveData: Boolean = false,
    val defaultWebSearch: Boolean = false,
    val isActive: Boolean = true,
    /**
     * Extensible JSON key-value store for future profile expansion
     * (e.g., academic level, preferred programming language, exam goals, response depth).
     */
    val extensibleAttributesJson: String = """{"Explanation Style":"Step-by-step with examples","Preferred Code Language":"Kotlin & Python","Academic Level":"University / Competitive Prep"}""",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val parsedThemeMode: AppThemeMode
        get() = AppThemeMode.fromName(themeMode)

    val parsedLanguage: AppLanguage
        get() = AppLanguage.fromName(preferredLanguage)

    val parsedTextSize: TextSizeOption
        get() = TextSizeOption.fromName(textSize)

    val parsedAvatarPreset: AvatarPreset
        get() = AvatarPreset.fromName(avatarPreset)

    fun getExtensibleAttributesMap(): Map<String, String> {
        if (extensibleAttributesJson.isBlank()) return emptyMap()
        return try {
            val json = JSONObject(extensibleAttributesJson)
            val map = linkedMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key, "")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun encodeExtensibleAttributes(attributes: Map<String, String>): String {
            val json = JSONObject()
            attributes.forEach { (k, v) ->
                if (k.isNotBlank()) json.put(k.trim(), v.trim())
            }
            return json.toString()
        }
    }
}

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["profileId"]), Index(value = ["updatedAt"])]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1L,
    val title: String,
    val assistantMode: String = AssistantMode.GENERAL.name,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = ""
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentUri: String? = null,
    val attachmentBase64: String? = null,
    val isMathSolution: Boolean = false,
    val mathCategory: String? = null,
    val usedWebSearch: Boolean = false,
    val webSourcesJson: String? = null,
    val modelUsed: String = "gemini-3.5-flash",
    val isError: Boolean = false
) {
    val isUser: Boolean
        get() = role == "user"

    fun parseWebSources(): List<WebSource> {
        if (webSourcesJson.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(webSourcesJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "Web Reference")
                    val uri = obj.optString("uri", "")
                    if (uri.isNotBlank()) {
                        add(WebSource(title = title, uri = uri))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class WebSource(
    val title: String,
    val uri: String
) {
    companion object {
        fun toJsonString(sources: List<WebSource>): String {
            val array = JSONArray()
            sources.forEach { source ->
                val obj = JSONObject()
                obj.put("title", source.title)
                obj.put("uri", source.uri)
                array.put(obj)
            }
            return array.toString()
        }
    }
}

data class PendingAttachment(
    val uriString: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0L,
    val base64Data: String? = null,
    val extractedText: String? = null,
    val uploadProgress: Float = 1.0f,
    val isImage: Boolean = mimeType.startsWith("image/"),
    val errorMessage: String? = null
)

data class ApiDiagnosticsState(
    val isKeyConfigured: Boolean = false,
    val activeEndpoint: String = "https://generativelanguage.googleapis.com/",
    val primaryChatModel: String = "gemini-3.5-flash",
    val reasoningMathModel: String = "gemini-3.1-pro-preview",
    val requestsInLastMinute: Int = 0,
    val maxRequestsPerMinute: Int = 15,
    val lastPingLatencyMs: Long? = null,
    val lastPingStatus: String = "Ready — Click 'Verify Connection' to test live API",
    val isTestingConnection: Boolean = false
)
