package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechBackendFailureRecoveryPolicyTest {
  private fun decide(
    backend: RecognitionBackend = RecognitionBackend.ANDROID_ON_DEVICE,
    error: SpeechRecognitionTransientError = SpeechRecognitionTransientError.NO_MATCH,
    retryScheduled: Boolean = false,
    partialAvailable: Boolean = false,
    systemAvailable: Boolean = true,
    manualStop: Boolean = false,
  ) = SpeechBackendFailureRecoveryPolicy.shouldFallback(
    currentBackend = backend,
    error = error,
    retryScheduled = retryScheduled,
    partialAvailable = partialAvailable,
    systemRecognizerAvailable = systemAvailable,
    manualStopRequested = manualStop,
  )

  @Test
  fun exhaustedNoMatchFallsBackFromBrokenOnDeviceBackend() {
    assertTrue(decide())
  }

  @Test
  fun scheduledRetryDoesNotFallbackEarly() {
    assertFalse(decide(retryScheduled = true))
  }

  @Test
  fun partialResultWinsInsteadOfChangingBackend() {
    assertFalse(decide(partialAvailable = true))
  }

  @Test
  fun manualStopNeverChangesBackend() {
    assertFalse(decide(manualStop = true))
  }

  @Test
  fun systemBackendCannotFallbackToItself() {
    assertFalse(decide(backend = RecognitionBackend.ANDROID_SYSTEM))
  }

  @Test
  fun unavailableSystemRecognizerCannotBeSelected() {
    assertFalse(decide(systemAvailable = false))
  }

  @Test
  fun busyErrorUsesExistingRecoveryInsteadOfBackendFallback() {
    assertFalse(decide(error = SpeechRecognitionTransientError.RECOGNIZER_BUSY))
  }
}
