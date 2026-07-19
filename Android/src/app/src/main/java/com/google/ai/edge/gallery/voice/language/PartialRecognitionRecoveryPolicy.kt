package com.google.ai.edge.gallery.voice.language

enum class PartialRecognitionTerminalError {
  NO_MATCH,
  SPEECH_TIMEOUT,
  CLIENT,
  SERVER_DISCONNECTED,
  EMPTY_RESULTS,
  NON_RECOVERABLE,
}

object PartialRecognitionRecoveryPolicy {
  fun shouldRecover(
    partialText: String,
    error: PartialRecognitionTerminalError,
    manualStopRequested: Boolean,
  ): Boolean {
    if (manualStopRequested || partialText.isBlank()) return false
    return error != PartialRecognitionTerminalError.NON_RECOVERABLE
  }
}
