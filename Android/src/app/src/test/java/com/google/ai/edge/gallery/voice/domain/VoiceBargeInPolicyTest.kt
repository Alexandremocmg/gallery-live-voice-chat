package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceBargeInPolicyTest {
  @Test
  fun automaticBargeInIsDisabledUntilItCanShareMicWithSpeechRecognizer() {
    assertFalse(VoiceBargeInPolicy.AUTOMATIC_BARGE_IN_ENABLED_BY_DEFAULT)
  }
}
