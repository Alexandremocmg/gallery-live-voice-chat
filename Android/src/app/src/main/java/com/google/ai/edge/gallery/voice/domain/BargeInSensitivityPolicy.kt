package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.language.SpeechChunk

data class VoiceActivityDetectorConfig(
  val warmupMs: Long,
  val sustainedSpeechMs: Long,
  val minimumThreshold: Float,
  val noiseMultiplier: Float,
)

object BargeInSensitivityPolicy {
  val BALANCED =
    VoiceActivityDetectorConfig(
      warmupMs = 800L,
      sustainedSpeechMs = 650L,
      minimumThreshold = 1_100f,
      noiseMultiplier = 3.6f,
    )

  val CONSERVATIVE =
    VoiceActivityDetectorConfig(
      warmupMs = 1_200L,
      sustainedSpeechMs = 900L,
      minimumThreshold = 1_400f,
      noiseMultiplier = 4.2f,
    )

  fun configFor(chunk: SpeechChunk): VoiceActivityDetectorConfig {
    val wordCount = chunk.text.trim().split(Regex("\\s+")).filter(String::isNotBlank).size
    if (chunk.locale.isEnglish && wordCount in 1..4) return CONSERVATIVE
    return BALANCED
  }
}
