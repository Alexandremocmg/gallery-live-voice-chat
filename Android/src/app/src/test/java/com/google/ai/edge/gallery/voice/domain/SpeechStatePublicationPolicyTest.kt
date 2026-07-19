package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechStatePublicationPolicyTest {
  @Test
  fun staleTtsCallbackCannotOverwriteActiveRecognition() {
    assertFalse(
      SpeechStatePublicationPolicy.shouldPublishTtsState(
        recognitionSessionActive = true,
        callbackBelongsToActiveUtterance = true,
      ),
    )
  }

  @Test
  fun activeTtsCallbackPublishesWhenNoRecognitionIsActive() {
    assertTrue(
      SpeechStatePublicationPolicy.shouldPublishTtsState(
        recognitionSessionActive = false,
        callbackBelongsToActiveUtterance = true,
      ),
    )
  }

  @Test
  fun callbackForOldUtteranceIsIgnored() {
    assertFalse(
      SpeechStatePublicationPolicy.shouldPublishTtsState(
        recognitionSessionActive = false,
        callbackBelongsToActiveUtterance = false,
      ),
    )
  }
}
