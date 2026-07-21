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
    automaticHandsFree: Boolean = false,
  ) = LogicalListeningWindowPolicy.shouldContinue(
    logicalElapsedMs = logicalElapsedMs,
    physicalAttempt = physicalAttempt,
    speechStarted = speechStarted,
    partialAvailable = partialAvailable,
    manualStopRequested = manualStop,
    automaticHandsFree = automaticHandsFree,
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
  fun handsFreeFalseStartWithoutPartialRearmsInsideLogicalWindow() {
    assertTrue(decide(speechStarted = true, automaticHandsFree = true))
  }

  @Test
  fun handsFreeFalseStartStillStopsAtAttemptLimit() {
    assertFalse(
      decide(
        physicalAttempt = LogicalListeningWindowPolicy.MAX_PHYSICAL_ATTEMPTS,
        speechStarted = true,
        automaticHandsFree = true,
      )
    )
  }

  @Test
  fun handsFreePartialAlwaysWinsOverRearming() {
    assertFalse(
      decide(
        speechStarted = true,
        partialAvailable = true,
        automaticHandsFree = true,
      )
    )
  }

  @Test
  fun exhaustedHandsFreeNoMatchEndsSilentlyWithoutPartial() {
    assertTrue(
      LogicalListeningWindowPolicy.shouldSuppressTerminalFailure(
        automaticHandsFree = true,
        partialAvailable = false,
        manualStopRequested = false,
      )
    )
  }

  @Test
  fun manualTurnNeverSuppressesTerminalFailure() {
    assertFalse(
      LogicalListeningWindowPolicy.shouldSuppressTerminalFailure(
        automaticHandsFree = false,
        partialAvailable = false,
        manualStopRequested = false,
      )
    )
  }

  @Test
  fun handsFreePartialNeverSuppressesTerminalFailure() {
    assertFalse(
      LogicalListeningWindowPolicy.shouldSuppressTerminalFailure(
        automaticHandsFree = true,
        partialAvailable = true,
        manualStopRequested = false,
      )
    )
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
