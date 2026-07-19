package com.google.ai.edge.gallery.voice.domain

object RecognitionSessionPolicy {
  fun acceptCallback(
    callbackSessionId: Long,
    activeSessionId: Long,
    recognitionSessionActive: Boolean,
  ): Boolean =
    recognitionSessionActive && callbackSessionId == activeSessionId

  fun executeRetry(
    scheduledForSessionId: Long,
    activeSessionId: Long,
    recognitionSessionActive: Boolean,
    manualStopRequested: Boolean,
  ): Boolean =
    scheduledForSessionId == activeSessionId &&
      !recognitionSessionActive &&
      !manualStopRequested
}
