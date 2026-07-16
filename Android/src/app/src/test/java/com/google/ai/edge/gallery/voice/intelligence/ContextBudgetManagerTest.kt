package com.google.ai.edge.gallery.voice.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBudgetManagerTest {
  private val manager = ContextBudgetManager(TokenEstimator { text -> text.length })

  @Test
  fun snapshot_reportsNormalPressureForSmallContext() {
    val snapshot =
      manager.snapshot(
        contextWindow = 4_096,
        reservedOutput = 512,
        systemPrompt = "s".repeat(200),
        toolCatalog = "",
        dynamicParts = listOf("x".repeat(500)),
      )

    assertEquals(ContextPressure.NORMAL, snapshot.pressure)
  }

  @Test
  fun snapshot_requiresResetNearWindowLimit() {
    val snapshot =
      manager.snapshot(
        contextWindow = 2_048,
        reservedOutput = 512,
        systemPrompt = "s".repeat(400),
        toolCatalog = "t".repeat(200),
        dynamicParts = listOf("x".repeat(1_000)),
      )

    assertEquals(ContextPressure.RESET_REQUIRED, snapshot.pressure)
  }

  @Test
  fun fit_keepsPriorityAndTruncatesLastPart() {
    val fitted = manager.fit(listOf("a".repeat(100), "b".repeat(100)), 180)

    assertEquals(2, fitted.size)
    assertTrue(fitted.first().all { it == 'a' })
    assertTrue(fitted.last().startsWith("b"))
  }
}
