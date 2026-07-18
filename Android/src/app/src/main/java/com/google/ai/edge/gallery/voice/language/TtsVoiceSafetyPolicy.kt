package com.google.ai.edge.gallery.voice.language

import java.util.Locale

object TtsVoiceSafetyPolicy {
  fun isCompatibleLocalVoice(
    requestedLocale: SpeechLocale,
    candidateLanguageTag: String,
    networkConnectionRequired: Boolean,
  ): Boolean {
    if (networkConnectionRequired) return false
    val candidateLanguage =
      Locale.forLanguageTag(candidateLanguageTag.replace('_', '-')).language
    return candidateLanguage.isNotBlank() &&
      candidateLanguage.equals(requestedLocale.locale.language, ignoreCase = true)
  }
}

class SpeechChunkQueue {
  private val chunks = ArrayDeque<SpeechChunk>()

  fun addLast(chunk: SpeechChunk) {
    chunks.addLast(chunk)
  }

  fun removeFirstOrNull(): SpeechChunk? = chunks.removeFirstOrNull()

  fun clear() {
    chunks.clear()
  }

  fun isNotEmpty(): Boolean = chunks.isNotEmpty()

  fun snapshot(): List<SpeechChunk> = chunks.toList()
}
