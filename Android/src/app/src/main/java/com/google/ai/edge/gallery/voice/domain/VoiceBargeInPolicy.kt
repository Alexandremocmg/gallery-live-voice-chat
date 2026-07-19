package com.google.ai.edge.gallery.voice.domain

object VoiceBargeInPolicy {
  // The current AudioRecord-based detector cannot share the microphone with
  // SpeechRecognizer without losing the beginning of the interruption phrase.
  const val AUTOMATIC_BARGE_IN_ENABLED_BY_DEFAULT = false
}
