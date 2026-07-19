package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartialRecognitionRecoveryPolicyTest {
  @Test
  fun usefulPartialIsRecoveredWhenNoMatchArrives() {
    assertTrue(
      PartialRecognitionRecoveryPolicy.shouldRecover(
        partialText = "qual é a capital do Brasil",
        error = PartialRecognitionTerminalError.NO_MATCH,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun usefulPartialIsRecoveredWhenSpeechTimeoutArrives() {
    assertTrue(
      PartialRecognitionRecoveryPolicy.shouldRecover(
        partialText = "me explique gravidade",
        error = PartialRecognitionTerminalError.SPEECH_TIMEOUT,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun manualStopNeverSubmitsPartialSpeech() {
    assertFalse(
      PartialRecognitionRecoveryPolicy.shouldRecover(
        partialText = "texto parcial",
        error = PartialRecognitionTerminalError.NO_MATCH,
        manualStopRequested = true,
      ),
    )
  }

  @Test
  fun blankPartialIsNeverRecovered() {
    assertFalse(
      PartialRecognitionRecoveryPolicy.shouldRecover(
        partialText = "   ",
        error = PartialRecognitionTerminalError.NO_MATCH,
        manualStopRequested = false,
      ),
    )
  }

  @Test
  fun permissionFailureDoesNotRecoverPartialSpeech() {
    assertFalse(
      PartialRecognitionRecoveryPolicy.shouldRecover(
        partialText = "texto parcial",
        error = PartialRecognitionTerminalError.NON_RECOVERABLE,
        manualStopRequested = false,
      ),
    )
  }
}
