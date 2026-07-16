package com.google.ai.edge.gallery.voice.language

data class RecognizedWord(
  val text: String,
  val confidenceLevel: Int? = null,
  val timestampMs: Long? = null,
)

data class SpeechRecognitionResult(
  val text: String,
  val alternatives: List<String> = emptyList(),
  val confidenceScores: List<Float> = emptyList(),
  val words: List<RecognizedWord> = emptyList(),
  val locale: SpeechLocale,
  val backend: RecognitionBackend,
)
