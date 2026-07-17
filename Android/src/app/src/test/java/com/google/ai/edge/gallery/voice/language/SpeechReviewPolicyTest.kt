package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechReviewPolicyTest {
  private val result =
    SpeechRecognitionResult(
      text = "ola",
      locale = SpeechLocale.PT_BR,
      backend = RecognitionBackend.ANDROID_ON_DEVICE,
    )

  @Test
  fun unknownConfidenceUsesContinuousConversation() {
    assertFalse(SpeechReviewPolicy.shouldReview(result))
  }

  @Test
  fun lowConfidenceKeepsManualReview() {
    assertTrue(
      SpeechReviewPolicy.shouldReview(
        result.copy(confidenceScores = listOf(SpeechReviewPolicy.LOW_CONFIDENCE_THRESHOLD - 0.01f)),
      ),
    )
  }

  @Test
  fun confidentRecognitionIsSentAutomatically() {
    assertFalse(
      SpeechReviewPolicy.shouldReview(
        result.copy(confidenceScores = listOf(SpeechReviewPolicy.LOW_CONFIDENCE_THRESHOLD)),
      ),
    )
  }
}
