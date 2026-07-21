package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.language.RecognitionBackend

object InjectedAudioRecognitionPolicy {
  private const val MIN_API_LEVEL = 33

  fun shouldUse(
    apiLevel: Int,
    featureEnabled: Boolean,
    backend: RecognitionBackend,
  ): Boolean =
    featureEnabled &&
      apiLevel >= MIN_API_LEVEL &&
      backend == RecognitionBackend.ANDROID_SYSTEM
}
