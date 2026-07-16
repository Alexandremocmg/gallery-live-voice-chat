package com.google.ai.edge.gallery.voice.domain

import kotlin.math.max

class AdaptiveVoiceActivityDetector(
  private val warmupMs: Long = 650L,
  private val sustainedSpeechMs: Long = 420L,
  private val minimumThreshold: Float = 900f,
  private val noiseMultiplier: Float = 3.2f,
) {
  private var startedAtMs = 0L
  private var speechStartedAtMs: Long? = null
  private var noiseFloor = 250f
  private var triggered = false

  fun reset(nowMs: Long = 0L) {
    startedAtMs = nowMs
    speechStartedAtMs = null
    noiseFloor = 250f
    triggered = false
  }

  fun observe(peak: Int, nowMs: Long): Boolean {
    if (triggered) return false
    if (startedAtMs == 0L) startedAtMs = nowMs
    val amplitude = peak.toFloat().coerceAtLeast(0f)
    if (nowMs - startedAtMs < warmupMs) {
      noiseFloor = noiseFloor * 0.88f + amplitude * 0.12f
      return false
    }

    val threshold = max(minimumThreshold, noiseFloor * noiseMultiplier)
    if (amplitude >= threshold) {
      val speechStart = speechStartedAtMs ?: nowMs.also { speechStartedAtMs = it }
      if (nowMs - speechStart >= sustainedSpeechMs) {
        triggered = true
        return true
      }
    } else {
      speechStartedAtMs = null
      noiseFloor = noiseFloor * 0.96f + amplitude * 0.04f
    }
    return false
  }
}
