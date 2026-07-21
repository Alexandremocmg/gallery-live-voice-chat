package com.google.ai.edge.gallery.voice.presentation

enum class VoiceInputGesture { TAP, HOLD }

object VoiceInputGesturePolicy {
  const val HOLD_THRESHOLD_MS = 350L

  fun classify(durationMs: Long): VoiceInputGesture =
    if (durationMs >= HOLD_THRESHOLD_MS) VoiceInputGesture.HOLD else VoiceInputGesture.TAP
}
