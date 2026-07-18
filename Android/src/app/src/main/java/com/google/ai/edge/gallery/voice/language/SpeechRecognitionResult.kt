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

object SpeechReviewPolicy {
  const val LOW_CONFIDENCE_THRESHOLD = 0.55f

  // Voice chat is continuous by default. Recognition mistakes are corrected later through
  // the persisted message action "Editar e reenviar", never through a blocking review step.
  fun shouldReview(@Suppress("UNUSED_PARAMETER") result: SpeechRecognitionResult): Boolean = false
}
