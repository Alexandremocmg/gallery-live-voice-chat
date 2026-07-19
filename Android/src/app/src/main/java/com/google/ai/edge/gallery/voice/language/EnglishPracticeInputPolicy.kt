package com.google.ai.edge.gallery.voice.language

object EnglishPracticeInputPolicy {
  fun shouldUseGemmaAudio(
    activity: EnglishActivity,
    modelSupportsAudio: Boolean,
    recognitionCapability: SpeechCapability?,
  ): Boolean {
    if (activity != EnglishActivity.REPEAT) return false
    if (!modelSupportsAudio) return false
    if (recognitionCapability == null) return false
    if (recognitionCapability.backend == RecognitionBackend.UNAVAILABLE) return true
    return false
  }
}
