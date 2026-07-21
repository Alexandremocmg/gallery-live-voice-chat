package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.language.RecognitionBackend

object InjectedAudioRecognitionPolicy {
  private const val MIN_API_LEVEL = 33

  fun canEnable(
    apiLevel: Int,
    systemRecognizerAvailable: Boolean,
    privateOffline: Boolean = false,
  ): Boolean =
    apiLevel >= MIN_API_LEVEL &&
      systemRecognizerAvailable &&
      !privateOffline

  fun selectBackend(
    apiLevel: Int,
    featureEnabled: Boolean,
    preferredBackend: RecognitionBackend,
    systemRecognizerAvailable: Boolean,
    privateOffline: Boolean = false,
  ): RecognitionBackend =
    if (
      featureEnabled &&
        canEnable(
          apiLevel = apiLevel,
          systemRecognizerAvailable = systemRecognizerAvailable,
          privateOffline = privateOffline,
        )
    ) {
      RecognitionBackend.ANDROID_SYSTEM
    } else {
      preferredBackend
    }

  fun shouldUse(
    apiLevel: Int,
    featureEnabled: Boolean,
    backend: RecognitionBackend,
  ): Boolean =
    featureEnabled &&
      apiLevel >= MIN_API_LEVEL &&
      backend == RecognitionBackend.ANDROID_SYSTEM
}
