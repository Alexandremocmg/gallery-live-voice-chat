package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.language.SpeechChunk
import com.google.ai.edge.gallery.voice.language.SpeechLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BargeInSensitivityPolicyTest {
  @Test
  fun normalPortugueseChunkUsesBalancedBargeIn() {
    val config =
      BargeInSensitivityPolicy.configFor(
        SpeechChunk("Claro, vamos conversar sobre isso.", SpeechLocale.PT_BR),
      )

    assertEquals(800L, config.warmupMs)
    assertEquals(650L, config.sustainedSpeechMs)
    assertEquals(1_100f, config.minimumThreshold)
    assertEquals(3.6f, config.noiseMultiplier)
  }

  @Test
  fun shortEnglishDemonstrationUsesConservativeBargeIn() {
    val config =
      BargeInSensitivityPolicy.configFor(
        SpeechChunk("good morning", SpeechLocale.EN_US, speechRate = 0.81f),
      )

    assertEquals(1_200L, config.warmupMs)
    assertEquals(900L, config.sustainedSpeechMs)
    assertEquals(1_400f, config.minimumThreshold)
    assertEquals(4.2f, config.noiseMultiplier)
  }

  @Test
  fun conservativeConfigDoesNotTriggerOnBriefSpeechThatBalancedWouldAccept() {
    val balanced = AdaptiveVoiceActivityDetector(BargeInSensitivityPolicy.BALANCED)
    val conservative = AdaptiveVoiceActivityDetector(BargeInSensitivityPolicy.CONSERVATIVE)
    balanced.reset(1_000)
    conservative.reset(1_000)

    assertFalse(balanced.observe(2_000, 1_900))
    assertTrue(balanced.observe(2_000, 2_600))

    assertFalse(conservative.observe(2_000, 2_300))
    assertFalse(conservative.observe(2_000, 2_600))
    assertTrue(conservative.observe(2_000, 3_300))
  }
}
