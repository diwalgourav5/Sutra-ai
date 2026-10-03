# Sutra AI — Intelligent Android AI Assistant & Studio

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-blue.svg)](https://developer.android.com/jetpack/compose)
[![AI Models](https://img.shields.io/badge/Google%20Gemini-3.5%20Flash%20%7C%203.1%20Pro%20%7C%203.1%20Flash%20Image-orange.svg)](https://ai.google.dev)
[![GitHub Repository](https://img.shields.io/badge/GitHub-diwalgourav5%2FSutra--ai-black.svg)](https://github.com/diwalgourav5/Sutra-ai)

**Sutra AI** is a native, modern Android application developed using **Kotlin**, **Jetpack Compose (Material Design 3)**, and **Google Gemini AI**. Designed for students, researchers, developers, and creators, it delivers conversational AI assistance, step-by-step mathematical problem solving, vision and document understanding, hands-free speech interactions, and a creative AI Image Studio.

---

## 🌟 Key Features

### 1. 💬 Conversational AI Assistant
- **Multi-turn Contextual Chat**: Context-aware conversations with automatic smart titling.
- **Multilingual Support**: First-class support for **English**, **हिन्दी (Hindi)**, and conversational **Hinglish** (Hindi written in Roman script blended with English terminology).
- **Web Search Grounding**: Real-time factual search grounding with clickable citation badges and source verification.
- **Specialized Assistant Modes**:
  - 🌐 General Assistant
  - 🎓 Study & Tutor
  - 💻 Coding & Architecture
  - 💡 Creative Writing & Brainstorming
- **Message Editing & Regeneration**: Edit previous messages or regenerate AI replies on the fly.

### 2. 🧮 Step-by-Step Math Solver
- **Dedicated Math Studio**: Specialized interface powered by `gemini-3.1-pro-preview` for complex logical reasoning.
- **8 Dedicated Mathematical Branches**:
  - Algebra & Polynomials
  - Calculus & Integrals
  - Linear Algebra & Matrices
  - Geometry & Trigonometry
  - Probability & Statistics
  - Discrete Mathematics & Graphs
  - Differential Equations
  - Word Problems & Financial Math
- **Interactive Math Symbol Toolbar**: Quick-insert symbols ($\int$, $\sum$, $\sqrt{}$, $\pi$, $\theta$, $\infty$, matrices, and superscripts).
- **Vision Solver**: Snap a photo or upload an image of a handwritten math problem to solve it step-by-step.
- **Continue in Chat**: Transition any solved math problem into conversational chat for follow-up explanations.

### 3. 🎨 AI Image Studio (Gemini 3.1 Flash Image)
- **ChatGPT / DALL-E Style Image Generation**:
  - **Text-to-Image**: High-fidelity image creation from text descriptions.
  - **Image-to-Image Editing**: Upload reference photos from gallery or camera and apply transformative prompts.
- **Aspect Ratio Control**:
  - `1:1` Square
  - `16:9` Landscape
  - `9:16` Portrait
- **Multilingual Prompt Chips**: Built-in inspiration prompts in English, Hindi, and Hinglish.
- **Image History ("My Images")**: Stored locally in Room database with instant preview, gallery save (`Pictures/SutraAI`), custom file export, regeneration, sharing, and deletion.
- **Model Engine**: Uses `gemini-3.1-flash-image` with fallback to `gemini-2.5-flash-image`.

### 4. 🎙️ Voice & Accessibility (STT & TTS)
- **Hands-Free Speech-to-Text**: In-chat microphone recording with automatic language adaptation for English and Hindi.
- **Text-to-Speech (TTS)**: Natural voice read-aloud with customizable pitch and speech rate.
- **Interactive Voice Overlay**: Full-screen hands-free conversational voice experience.

### 5. 💾 1-Click Backup & Local Data Persistence
- **Room Database**: Complete on-device database storing profiles, conversations, chat messages, and generated images.
- **1-Click Export**: Export conversation history into structured **JSON** or formatted **Text (TXT)** files for backup.
- **Custom Location Export**: Save backup files to any device folder using Android Storage Access Framework (SAF).

### 6. 🎨 Material Design 3 & Personalization
- **Theme Modes**: System Default, Light Mode, and Dark Mode.
- **Material You Dynamic Colors**: Adapts palette dynamically on Android 12+ devices.
- **Typography Sizing**: Small (88%), Default (100%), Large (115%), and Extra Large (130%).
- **User Profiles**: Custom avatars (photo upload or presets), custom username, and academic/work focus.
- **Privacy Masking**: Built-in filter that redacts sensitive information (emails, phone numbers, API keys) before sending requests.

---

## 🏗️ Architecture & Tech Stack

```
com.example/
├── MainActivity.kt                # Root ComponentActivity, Edge-to-Edge & Navigation
├── data/
│   ├── local/
│   │   ├── SutraDatabase.kt       # Room Database (v3)
│   │   └── SutraDao.kt            # DAOs for Profiles, Conversations, Messages, Images
│   ├── model/
│   │   └── Models.kt              # Domain Models, Enums, Room Entities, Diagnostics
│   ├── remote/
│   │   ├── ApiConfig.kt           # Endpoints, Sliding Window Rate Limiter & Masking
│   │   ├── GeminiApiClient.kt     # Retrofit & SSE Streaming client for Chat & Math
│   │   └── ImageGenerationProvider.kt # Gemini 3.1 Flash Image REST client & Storage
│   └── repository/
│       └── SutraRepository.kt     # Unified Single Source of Truth
├── ui/
│   ├── components/                # Modular UI widgets (Avatars, Strips, Markdown, Voice)
│   ├── screens/
│   │   ├── ChatScreen.kt          # Conversational Chat Screen
│   │   ├── MathSolverScreen.kt    # Dedicated Math Solver Studio
│   │   ├── ImageGeneratorScreen.kt# AI Image Studio & Gallery
│   │   ├── HistoryScreen.kt       # Conversation Management & Search
│   │   ├── ProfileScreen.kt       # User Profile & Preferences
│   │   └── SettingsScreen.kt      # Themes, API Diagnostics & Setup
│   ├── theme/                     # Material 3 Color Schemes, Shapes & Typography
│   └── viewmodel/
│       └── SutraViewModel.kt      # Central ViewModel, StateFlows & Coroutines
└── util/
    ├── AttachmentHelper.kt        # File & Bitmap processing, Base64 conversion
    ├── BackupExportHelper.kt      # JSON & TXT serialization & export
    └── VoiceManager.kt            # Android TextToSpeech & SpeechRecognizer helper
```

---

## 🔑 Setup & API Key Configuration

Sutra AI reads your API key securely via the **Secrets Gradle Plugin** and `BuildConfig`. API keys are never hardcoded in source code or committed to version control.

### Option A: AI Studio Secrets Panel (Recommended)
1. In Google AI Studio, open the **Secrets** panel in the left sidebar.
2. Add a secret named `GEMINI_API_KEY`.
3. Enter your Gemini API key from [Google AI Studio](https://aistudio.google.com/).

### Option B: Local `.env` File
Create a `.env` file in the root directory (based on `.env.example`):
```env
GEMINI_API_KEY=AIzaSy...Your_Actual_API_Key
```
*(Note: `.env` is listed in `.gitignore` and will never be committed to git).*

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- Android SDK 36 (compileSdk = 36, minSdk = 24)
- JDK 17 or JDK 21

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Local Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 📦 Pushing to GitHub (`diwalgourav5/Sutra-ai`)

If you are pushing this project to your repository [github.com/diwalgourav5/Sutra-ai](https://github.com/diwalgourav5/Sutra-ai):

```bash
# 1. Initialize git (if not already done)
git init

# 2. Add remote origin
git remote add origin https://github.com/diwalgourav5/Sutra-ai.git

# 3. Stage and commit all files
git add .
git commit -m "feat: complete Sutra AI Android Assistant & Image Studio with Gemini 3.5 & 3.1"

# 4. Push to main branch
git branch -M main
git push -u origin main
```

Alternatively, you can push directly to GitHub using the **Export / Push to GitHub** button in the Google AI Studio UI.

---

## 📄 License
This project is open-source and available under the [Apache 2.0 License](LICENSE).
