package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Test

class SpeechEndpointOverridePolicyTest {
  @Test
  fun conversationRecognitionUsesServiceEndpointDefaults() {
    assertFalse(SpeechEndpointOverridePolicy.shouldOverrideSilenceThresholds())
  }
}
