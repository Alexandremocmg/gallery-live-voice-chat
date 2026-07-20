package com.google.ai.edge.gallery.voice.language

object SpeechBackendFailureRecoveryPolicy {
  fun shouldFallback(
    currentBackend: RecognitionBackend,
    error: SpeechRecognitionTransientError,
    retryScheduled: Boolean,
    partialAvailable: Boolean,
    systemRecognizerAvailable: Boolean,
    manualStopRequested: Boolean,
  ): Boolean =
    currentBackend == RecognitionBackend.ANDROID_ON_DEVICE &&
      error in setOf(
        SpeechRecognitionTransientError.NO_MATCH,
        SpeechRecognitionTransientError.SPEECH_TIMEOUT,
      ) &&
      !retryScheduled &&
      !partialAvailable &&
      systemRecognizerAvailable &&
      !manualStopRequested
}
