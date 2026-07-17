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

  fun shouldReview(result: SpeechRecognitionResult): Boolean {
    val confidence = result.confidenceScores.firstOrNull() ?: return false
    return confidence >= 0f && confidence < LOW_CONFIDENCE_THRESHOLD
  }
}
