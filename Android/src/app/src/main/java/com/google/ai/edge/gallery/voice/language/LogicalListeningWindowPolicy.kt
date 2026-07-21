package com.google.ai.edge.gallery.voice.language

object LogicalListeningWindowPolicy {
  const val INITIAL_SPEECH_WINDOW_MS = 6_000L
  const val MAX_PHYSICAL_ATTEMPTS = 4

  fun shouldContinue(
    logicalElapsedMs: Long,
    physicalAttempt: Int,
    speechStarted: Boolean,
    partialAvailable: Boolean,
    manualStopRequested: Boolean,
    automaticHandsFree: Boolean = false,
  ): Boolean =
    logicalElapsedMs < INITIAL_SPEECH_WINDOW_MS &&
      physicalAttempt < MAX_PHYSICAL_ATTEMPTS &&
      (!speechStarted || automaticHandsFree) &&
      !partialAvailable &&
      !manualStopRequested

  fun shouldSuppressTerminalFailure(
    automaticHandsFree: Boolean,
    partialAvailable: Boolean,
    manualStopRequested: Boolean,
  ): Boolean =
    automaticHandsFree &&
      !partialAvailable &&
      !manualStopRequested
}
