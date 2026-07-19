package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import com.google.ai.edge.gallery.voice.language.EnglishActivity

data class ConversationTurnTimingDecision(
  val submitDelayMs: Long,
  val autoRestartDelayMs: Long,
  val possiblyCompleteSilenceMs: Long,
  val completeSilenceMs: Long,
)

object ConversationTurnTimingPolicy {
  fun decide(
    profile: ResponseDepthProfile,
    englishActivity: EnglishActivity,
    recognizedText: String,
  ): ConversationTurnTimingDecision {
    if (englishActivity == EnglishActivity.REPEAT || englishActivity == EnglishActivity.PRONUNCIATION_FEEDBACK) {
      return ConversationTurnTimingDecision(
        submitDelayMs = 250L,
        autoRestartDelayMs = 450L,
        possiblyCompleteSilenceMs = 700L,
        completeSilenceMs = 1_100L,
      )
    }
    if (profile == ResponseDepthProfile.FLASH) {
      return ConversationTurnTimingDecision(
        submitDelayMs = 250L,
        autoRestartDelayMs = 500L,
        possiblyCompleteSilenceMs = 800L,
        completeSilenceMs = 1_200L,
      )
    }
    val normalized = recognizedText.trim().lowercase()
    val incomplete = INCOMPLETE_PREFIXES.any { normalized.startsWith(it) }
    val teacherMode = englishActivity != EnglishActivity.NONE || profile == ResponseDepthProfile.STEP_BY_STEP
    val submitDelay = if (teacherMode || incomplete) 1_100L else 750L
    return ConversationTurnTimingDecision(
      submitDelayMs = submitDelay,
      autoRestartDelayMs = 900L,
      possiblyCompleteSilenceMs = 1_500L,
      completeSilenceMs = 2_400L,
    )
  }

  private val INCOMPLETE_PREFIXES =
    listOf(
      "deixa eu",
      "eu queria",
      "eu quero",
      "como eu",
      "como se",
      "a frase",
    )
}
