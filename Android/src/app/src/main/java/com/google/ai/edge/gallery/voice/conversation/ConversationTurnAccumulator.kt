package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import com.google.ai.edge.gallery.voice.language.EnglishActivity

object ConversationTurnAccumulator {
  fun shouldWaitForContinuation(
    profile: ResponseDepthProfile,
    englishActivity: EnglishActivity,
    recognizedText: String,
  ): Boolean {
    if (englishActivity == EnglishActivity.REPEAT || englishActivity == EnglishActivity.PRONUNCIATION_FEEDBACK) {
      return false
    }
    val normalized = recognizedText.trim().lowercase()
    if (normalized.isBlank()) return false
    if (normalized.lastOrNull() in setOf('.', '?', '!', ';')) return false
    if (profile == ResponseDepthProfile.FLASH) return false
    return INCOMPLETE_PREFIXES.any { normalized.startsWith(it) } ||
      INCOMPLETE_SUFFIXES.any { normalized.endsWith(it) } ||
      normalized.split(Regex("\\s+")).size <= 5 && normalized.startsWith("eu ")
  }

  fun continuationWindowMs(timing: ConversationTurnTimingDecision): Long =
    maxOf(timing.submitDelayMs, timing.possiblyCompleteSilenceMs)

  fun recognizerRestartDelayMs(): Long = 300L

  fun merge(first: String, second: String): String =
    listOf(first.trim(), second.trim())
      .filter { it.isNotBlank() }
      .joinToString(" ")
      .replace(Regex("\\s+"), " ")
      .trim()

  private val INCOMPLETE_PREFIXES =
    listOf(
      "deixa eu",
      "eu queria",
      "eu quero",
      "como eu",
      "como se",
      "a frase",
      "pera",
      "espera",
    )

  private val INCOMPLETE_SUFFIXES =
    listOf(
      "que",
      "porque",
      "pra",
      "para",
      "tipo",
      "assim",
      "uma coisa",
    )
}
