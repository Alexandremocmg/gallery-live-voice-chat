package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechReadinessRefreshPolicyTest {
  @Test
  fun refreshesImmediatelyWhenNeverRefreshed() {
    val decision =
      SpeechReadinessRefreshPolicy.decide(
        nowMs = 10_000L,
        lastRefreshMs = null,
      )

    assertTrue(decision.shouldRefresh)
    assertEquals(10_000L, decision.nextRefreshMs)
  }

  @Test
  fun suppressesRefreshInsideCooldownWindow() {
    val decision =
      SpeechReadinessRefreshPolicy.decide(
        nowMs = 11_000L,
        lastRefreshMs = 10_000L,
      )

    assertFalse(decision.shouldRefresh)
    assertEquals(10_000L, decision.nextRefreshMs)
  }

  @Test
  fun refreshesAfterCooldownWindow() {
    val decision =
      SpeechReadinessRefreshPolicy.decide(
        nowMs = 13_100L,
        lastRefreshMs = 10_000L,
      )

    assertTrue(decision.shouldRefresh)
    assertEquals(13_100L, decision.nextRefreshMs)
  }
}
