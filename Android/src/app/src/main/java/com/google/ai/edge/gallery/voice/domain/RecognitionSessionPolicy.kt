package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.language.RecognitionBackend

object RecognitionSessionPolicy {
  fun invalidate(activeSessionId: Long): Long = activeSessionId + 1L

  fun invalidateForConnectivityMode(mode: ConnectivityMode): Boolean =
    mode == ConnectivityMode.PRIVATE_OFFLINE

  fun backendAllowed(
    mode: ConnectivityMode,
    backend: RecognitionBackend,
  ): Boolean =
    mode != ConnectivityMode.PRIVATE_OFFLINE ||
      backend == RecognitionBackend.ANDROID_ON_DEVICE

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
