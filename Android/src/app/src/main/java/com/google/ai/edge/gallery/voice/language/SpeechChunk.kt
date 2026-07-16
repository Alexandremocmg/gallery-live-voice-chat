package com.google.ai.edge.gallery.voice.language

data class SpeechChunk(
  val text: String,
  val locale: SpeechLocale,
  val speechRate: Float? = null,
)

data class BilingualParseResult(
  val chunks: List<SpeechChunk>,
) {
  val displayText: String
    get() = chunks.joinToString(separator = "") { it.text }
}
