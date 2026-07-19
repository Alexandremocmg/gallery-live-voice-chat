package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSessionPolicyTest {
  @Test
  fun callbackFromPreviousSessionIsRejected() {
    assertFalse(
      RecognitionSessionPolicy.acceptCallback(
        callbackSessionId = 10L,
        activeSessionId = 11L,
        recognitionSessionActive = true,
      ),
    )
  }

  @Test
  fun callbackFromCurrentActiveSessionIsAccepted() {
    assertTrue(
      RecognitionSessionPolicy.acceptCallback(
        callbackSessionId = 11L,
        activeSessionId = 11L,
        recognitionSessionActive = true,
      ),
    )
  }

  @Test
  fun callbackAfterSessionWasClearedIsRejected() {
    assertFalse(
      RecognitionSessionPolicy.acceptCallback(
        callbackSessionId = 11L,
        activeSessionId = 11L,
        recognitionSessionActive = false,
      ),
    )
  }

  @Test
  fun delayedRetryFromPreviousSessionIsRejected() {
    assertFalse(
      RecognitionSessionPolicy.executeRetry(
        scheduledForSessionId = 10L,
        activeSessionId = 11L,
        recognitionSessionActive = true,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun retryForEndedCurrentSessionIsAccepted() {
    assertTrue(
      RecognitionSessionPolicy.executeRetry(
        scheduledForSessionId = 11L,
        activeSessionId = 11L,
        recognitionSessionActive = false,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun retryAfterManualStopIsRejected() {
    assertFalse(
      RecognitionSessionPolicy.executeRetry(
        scheduledForSessionId = 11L,
        activeSessionId = 11L,
        recognitionSessionActive = false,
        manualStopRequested = true,
      ),
    )
  }
}
