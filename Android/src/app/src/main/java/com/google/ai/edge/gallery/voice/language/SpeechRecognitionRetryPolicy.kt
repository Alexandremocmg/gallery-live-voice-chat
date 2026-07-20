package com.google.ai.edge.gallery.voice.language

enum class SpeechRecognitionTransientError {
  CLIENT,
  RECOGNIZER_BUSY,
  SERVER_DISCONNECTED,
  SPEECH_TIMEOUT,
  NO_MATCH,
}

data class SpeechRecognitionRetryDecision(
  val shouldRetry: Boolean,
  val retryDelayMs: Long,
  val nextAttempt: Int,
)

object SpeechRecognitionRetryPolicy {
  private const val MAX_ATTEMPTS = 2

  fun decide(
    error: SpeechRecognitionTransientError,
    attempt: Int,
    speechStarted: Boolean,
    elapsedMs: Long,
  ): SpeechRecognitionRetryDecision {
    if (attempt >= MAX_ATTEMPTS) {
      return SpeechRecognitionRetryDecision(
        shouldRetry = false,
        retryDelayMs = 0L,
        nextAttempt = attempt,
      )
    }

    val retryDelay =
      when (error) {
        SpeechRecognitionTransientError.RECOGNIZER_BUSY -> 650L
        SpeechRecognitionTransientError.CLIENT,
        SpeechRecognitionTransientError.SERVER_DISCONNECTED -> 450L
        SpeechRecognitionTransientError.SPEECH_TIMEOUT,
        SpeechRecognitionTransientError.NO_MATCH -> 300L
      }

    return SpeechRecognitionRetryDecision(
      shouldRetry = retryDelay > 0L,
      retryDelayMs = retryDelay,
      nextAttempt = if (retryDelay > 0L) attempt + 1 else attempt,
    )
  }
}
