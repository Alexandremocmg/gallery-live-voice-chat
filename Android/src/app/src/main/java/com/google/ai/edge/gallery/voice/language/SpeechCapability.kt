package com.google.ai.edge.gallery.voice.language

enum class RecognitionBackend {
  ANDROID_ON_DEVICE,
  ANDROID_SYSTEM,
  LOCAL_LANGUAGE_PACK,
  GEMMA_AUDIO,
  UNAVAILABLE,
}

enum class LanguagePackStatus {
  INSTALLED,
  DOWNLOAD_AVAILABLE,
  DOWNLOAD_PENDING,
  UNSUPPORTED,
  UNKNOWN,
}

data class SpeechCapability(
  val locale: SpeechLocale,
  val backend: RecognitionBackend,
  val languagePackStatus: LanguagePackStatus,
  val localTtsAvailable: Boolean = false,
) {
  val isReadyOffline: Boolean
    get() = backend in setOf(RecognitionBackend.ANDROID_ON_DEVICE, RecognitionBackend.LOCAL_LANGUAGE_PACK) &&
      languagePackStatus != LanguagePackStatus.UNSUPPORTED
}
