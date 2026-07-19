package com.google.ai.edge.gallery.voice.language

data class SpeechReadinessRefreshDecision(
  val shouldRefresh: Boolean,
  val nextRefreshMs: Long?,
)

object SpeechReadinessRefreshPolicy {
  private const val RESUME_REFRESH_COOLDOWN_MS = 3_000L

  fun decide(
    nowMs: Long,
    lastRefreshMs: Long?,
  ): SpeechReadinessRefreshDecision {
    if (lastRefreshMs == null) {
      return SpeechReadinessRefreshDecision(shouldRefresh = true, nextRefreshMs = nowMs)
    }
    val shouldRefresh = nowMs - lastRefreshMs >= RESUME_REFRESH_COOLDOWN_MS
    return SpeechReadinessRefreshDecision(
      shouldRefresh = shouldRefresh,
      nextRefreshMs = if (shouldRefresh) nowMs else lastRefreshMs,
    )
  }
}
