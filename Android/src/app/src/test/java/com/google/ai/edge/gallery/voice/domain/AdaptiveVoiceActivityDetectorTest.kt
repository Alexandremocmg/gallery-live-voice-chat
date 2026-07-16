package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveVoiceActivityDetectorTest {
  @Test
  fun ignoresNoiseAndTriggersOnSustainedSpeech() {
    val detector =
      AdaptiveVoiceActivityDetector(
        warmupMs = 100,
        sustainedSpeechMs = 200,
        minimumThreshold = 500f,
        noiseMultiplier = 2f,
      )
    detector.reset(1_000)

    assertFalse(detector.observe(100, 1_050))
    assertFalse(detector.observe(700, 1_150))
    assertFalse(detector.observe(700, 1_250))
    assertTrue(detector.observe(700, 1_360))
  }

  @Test
  fun shortPeakDoesNotTrigger() {
    val detector = AdaptiveVoiceActivityDetector(warmupMs = 0, sustainedSpeechMs = 200)
    detector.reset(1_000)

    assertFalse(detector.observe(2_000, 1_010))
    assertFalse(detector.observe(100, 1_050))
    assertFalse(detector.observe(2_000, 1_100))
  }
}
