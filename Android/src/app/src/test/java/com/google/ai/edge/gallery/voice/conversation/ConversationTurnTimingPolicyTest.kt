package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import com.google.ai.edge.gallery.voice.language.EnglishActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTurnTimingPolicyTest {
  @Test
  fun englishTeacherConversationWaitsBeforeSubmittingFinalRecognition() {
    val decision =
      ConversationTurnTimingPolicy.decide(
        profile = ResponseDepthProfile.DETAILED,
        englishActivity = EnglishActivity.EXPLANATION,
        recognizedText = "Eu queria dizer",
      )

    assertEquals(1_100L, decision.submitDelayMs)
    assertEquals(900L, decision.autoRestartDelayMs)
    assertEquals(1_500L, decision.possiblyCompleteSilenceMs)
    assertEquals(2_400L, decision.completeSilenceMs)
  }

  @Test
  fun englishRepeatPracticeKeepsLowLatency() {
    val decision =
      ConversationTurnTimingPolicy.decide(
        profile = ResponseDepthProfile.DETAILED,
        englishActivity = EnglishActivity.REPEAT,
        recognizedText = "good morning",
      )

    assertEquals(250L, decision.submitDelayMs)
    assertEquals(450L, decision.autoRestartDelayMs)
    assertEquals(700L, decision.possiblyCompleteSilenceMs)
    assertEquals(1_100L, decision.completeSilenceMs)
  }

  @Test
  fun flashCommandRemainsFast() {
    val decision =
      ConversationTurnTimingPolicy.decide(
        profile = ResponseDepthProfile.FLASH,
        englishActivity = EnglishActivity.NONE,
        recognizedText = "resuma isso",
      )

    assertEquals(250L, decision.submitDelayMs)
    assertEquals(500L, decision.autoRestartDelayMs)
  }

  @Test
  fun incompleteShortUtteranceGetsExtraGrace() {
    val decision =
      ConversationTurnTimingPolicy.decide(
        profile = ResponseDepthProfile.STEP_BY_STEP,
        englishActivity = EnglishActivity.NONE,
        recognizedText = "deixa eu perguntar uma coisa",
      )

    assertEquals(1_100L, decision.submitDelayMs)
  }
}
