package com.google.ai.edge.gallery.voice.pedagogy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachingModelsTest {
  @Test
  fun `initial state is safe and inactive`() {
    val state = TeachingState()

    assertEquals(TeachingMode.OFF, state.mode)
    assertEquals(LearnerLevel.UNKNOWN, state.estimatedLevel)
    assertEquals(TeachingAction.ANSWER_NORMALLY, state.lastAction)
    assertTrue(state.completedConcepts.isEmpty())
    assertFalse(state.awaitingUnderstanding)
  }

  @Test
  fun `teaching hint is normalized and bounded`() {
    val hint = ("conceito " + "muito ".repeat(80)).toTeachingHint(60)

    assertTrue(hint.length <= 60)
    assertFalse(hint.endsWith(" "))
    assertFalse(hint.contains("  "))
  }

  @Test
  fun `normal decision does not require a comprehension check`() {
    val decision =
      TeachingDecision(
        signal = UnderstandingSignal.NONE,
        action = TeachingAction.ANSWER_NORMALLY,
        proposedNextState = TeachingState(),
        shouldCheckUnderstanding = false,
      )

    assertFalse(decision.shouldCheckUnderstanding)
  }
}
