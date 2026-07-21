package com.google.ai.edge.gallery.voice.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInputGesturePolicyTest {
  @Test
  fun shortPressIsTapToToggle() {
    assertEquals(VoiceInputGesture.TAP, VoiceInputGesturePolicy.classify(120L))
  }

  @Test
  fun thresholdPressIsHoldToTalk() {
    assertEquals(VoiceInputGesture.HOLD, VoiceInputGesturePolicy.classify(350L))
  }
}
