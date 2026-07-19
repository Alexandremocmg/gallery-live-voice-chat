package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionRetryPolicyTest {
  @Test
  fun clientErrorAfterSpeechBeginsRetriesInsteadOfFailing() {
    val decision =
      SpeechRecognitionRetryPolicy.decide(
        error = SpeechRecognitionTransientError.CLIENT,
        attempt = 0,
        speechStarted = true,
        elapsedMs = 420L,
      )

    assertTrue(decision.shouldRetry)
    assertEquals(450L, decision.retryDelayMs)
    assertEquals(1, decision.nextAttempt)
  }

  @Test
  fun recognizerBusyRetriesAfterLongerSettleDelay() {
    val decision =
      SpeechRecognitionRetryPolicy.decide(
        error = SpeechRecognitionTransientError.RECOGNIZER_BUSY,
        attempt = 0,
        speechStarted = false,
        elapsedMs = 80L,
      )

    assertTrue(decision.shouldRetry)
    assertEquals(650L, decision.retryDelayMs)
    assertEquals(1, decision.nextAttempt)
  }

  @Test
  fun speechTimeoutBeforeSpeechRetriesOnceForContinuousMic() {
    val decision =
      SpeechRecognitionRetryPolicy.decide(
        error = SpeechRecognitionTransientError.SPEECH_TIMEOUT,
        attempt = 0,
        speechStarted = false,
        elapsedMs = 1_000L,
      )

    assertTrue(decision.shouldRetry)
    assertEquals(300L, decision.retryDelayMs)
  }

  @Test
  fun repeatedTransientErrorStopsRetrying() {
    val decision =
      SpeechRecognitionRetryPolicy.decide(
        error = SpeechRecognitionTransientError.CLIENT,
        attempt = 2,
        speechStarted = true,
        elapsedMs = 300L,
      )

    assertFalse(decision.shouldRetry)
    assertEquals(2, decision.nextAttempt)
  }
}
