package com.google.ai.edge.gallery.voice.language

enum class ResponseLanguageMode {
  SINGLE_PORTUGUESE,
  SINGLE_ENGLISH,
  BILINGUAL_TEACHING,
}

enum class LanguageConfidence {
  LOW,
  MEDIUM,
  HIGH,
}

enum class LanguageDecisionSource {
  EXPLICIT_COMMAND,
  ENGLISH_TEACHER,
  ANDROID_DETECTION,
  TEXT_ANALYSIS,
  CONVERSATION_CONTEXT,
  DEFAULT,
}

data class LanguageTurnDecision(
  val inputLocale: SpeechLocale,
  val responseMode: ResponseLanguageMode,
  val responseLocale: SpeechLocale,
  val confidence: LanguageConfidence,
  val source: LanguageDecisionSource,
)
