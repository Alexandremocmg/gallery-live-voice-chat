package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import org.junit.Assert.assertEquals
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

  @Test
  fun invalidatingGenerationRejectsPendingRetryFromEndedSession() {
    val scheduledSessionId = 11L
    val invalidatedSessionId = RecognitionSessionPolicy.invalidate(scheduledSessionId)

    assertEquals(12L, invalidatedSessionId)
    assertFalse(
      RecognitionSessionPolicy.executeRetry(
        scheduledForSessionId = scheduledSessionId,
        activeSessionId = invalidatedSessionId,
        recognitionSessionActive = false,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun enteringPrivateModeAlwaysRequiresRecognitionInvalidation() {
    assertTrue(
      RecognitionSessionPolicy.invalidateForConnectivityMode(
        ConnectivityMode.PRIVATE_OFFLINE
      )
    )
    assertFalse(
      RecognitionSessionPolicy.invalidateForConnectivityMode(
        ConnectivityMode.CONNECTED
      )
    )
  }

  @Test
  fun privateModeAllowsOnlyOnDeviceRecognizer() {
    assertTrue(
      RecognitionSessionPolicy.backendAllowed(
        mode = ConnectivityMode.PRIVATE_OFFLINE,
        backend = RecognitionBackend.ANDROID_ON_DEVICE,
      )
    )
    assertFalse(
      RecognitionSessionPolicy.backendAllowed(
        mode = ConnectivityMode.PRIVATE_OFFLINE,
        backend = RecognitionBackend.ANDROID_SYSTEM,
      )
    )
    assertTrue(
      RecognitionSessionPolicy.backendAllowed(
        mode = ConnectivityMode.CONNECTED,
        backend = RecognitionBackend.ANDROID_SYSTEM,
      )
    )
  }
}
