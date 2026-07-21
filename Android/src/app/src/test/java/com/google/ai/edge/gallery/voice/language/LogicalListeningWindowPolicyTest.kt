package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicalListeningWindowPolicyTest {
  private fun decide(
    logicalElapsedMs: Long = 2_000L,
    physicalAttempt: Int = 1,
    speechStarted: Boolean = false,
    partialAvailable: Boolean = false,
    manualStop: Boolean = false,
  ) = LogicalListeningWindowPolicy.shouldContinue(
    logicalElapsedMs = logicalElapsedMs,
    physicalAttempt = physicalAttempt,
    speechStarted = speechStarted,
    partialAvailable = partialAvailable,
    manualStopRequested = manualStop,
  )

  @Test
  fun initialNoMatchContinuesInsideLogicalWindow() {
    assertTrue(decide())
  }

  @Test
  fun logicalDeadlineStopsListening() {
    assertFalse(decide(logicalElapsedMs = 6_000L))
  }

  @Test
  fun attemptLimitStopsListening() {
    assertFalse(decide(physicalAttempt = 4))
  }

  @Test
  fun detectedSpeechNeverStartsUnrelatedAttempt() {
    assertFalse(decide(speechStarted = true))
  }

  @Test
  fun partialResultWinsOverContinuation() {
    assertFalse(decide(partialAvailable = true))
  }

  @Test
  fun manualStopNeverContinues() {
    assertFalse(decide(manualStop = true))
  }
}
