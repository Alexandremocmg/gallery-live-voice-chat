package com.google.ai.edge.gallery.voice.language

object SpeechBackendPolicy {
  fun select(
    apiLevel: Int,
    onDeviceAvailable: Boolean,
    systemRecognizerAvailable: Boolean,
    localLanguagePackAvailable: Boolean = false,
    gemmaAudioAvailable: Boolean = false,
  ): RecognitionBackend {
    return when {
      apiLevel >= 31 && onDeviceAvailable -> RecognitionBackend.ANDROID_ON_DEVICE
      localLanguagePackAvailable -> RecognitionBackend.LOCAL_LANGUAGE_PACK
      gemmaAudioAvailable -> RecognitionBackend.GEMMA_AUDIO
      systemRecognizerAvailable -> RecognitionBackend.ANDROID_SYSTEM
      else -> RecognitionBackend.UNAVAILABLE
    }
  }
}
