package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import com.google.ai.edge.gallery.voice.language.EnglishActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTurnAccumulatorTest {
  @Test
  fun incompleteTeacherTurnCanWaitForContinuation() {
    assertTrue(
      ConversationTurnAccumulator.shouldWaitForContinuation(
        profile = ResponseDepthProfile.STEP_BY_STEP,
        englishActivity = EnglishActivity.NONE,
        recognizedText = "deixa eu perguntar uma coisa",
      ),
    )
  }

  @Test
  fun completeQuestionDoesNotWaitForContinuation() {
    assertFalse(
      ConversationTurnAccumulator.shouldWaitForContinuation(
        profile = ResponseDepthProfile.STEP_BY_STEP,
        englishActivity = EnglishActivity.NONE,
        recognizedText = "Como eu digo bom dia em inglês?",
      ),
    )
  }

  @Test
  fun englishRepeatDoesNotWaitForContinuation() {
    assertFalse(
      ConversationTurnAccumulator.shouldWaitForContinuation(
        profile = ResponseDepthProfile.DETAILED,
        englishActivity = EnglishActivity.REPEAT,
        recognizedText = "good morning",
      ),
    )
  }

  @Test
  fun continuationTextIsMergedWithOneSpace() {
    assertEquals(
      "Eu queria dizer good morning",
      ConversationTurnAccumulator.merge("Eu queria dizer", "good morning"),
    )
  }

  @Test
  fun continuationWindowUsesTimingGrace() {
    val timing =
      ConversationTurnTimingPolicy.decide(
        profile = ResponseDepthProfile.STEP_BY_STEP,
        englishActivity = EnglishActivity.NONE,
        recognizedText = "deixa eu perguntar uma coisa",
      )

    assertEquals(1_500L, ConversationTurnAccumulator.continuationWindowMs(timing))
  }

  @Test
  fun continuationRestartWaitsForRecognizerToSettle() {
    assertEquals(300L, ConversationTurnAccumulator.recognizerRestartDelayMs())
  }
}
