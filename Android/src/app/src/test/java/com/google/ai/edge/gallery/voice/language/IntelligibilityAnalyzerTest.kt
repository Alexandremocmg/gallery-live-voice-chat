package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntelligibilityAnalyzerTest {
  private val analyzer = IntelligibilityAnalyzer()

  @Test fun `chooses closest recognition alternative`() {
    val result = analyzer.analyze("I would like coffee", listOf("I wood light", "I would like coffee"))!!
    assertTrue(result.isLikelyUnderstood)
    assertEquals("I would like coffee", result.bestTranscript)
  }

  @Test fun `reports missing target words without claiming phonetic accuracy`() {
    val result = analyzer.analyze("I would like a cup of coffee", listOf("I like coffee"))!!
    assertFalse(result.isLikelyUnderstood)
    assertTrue(result.missingWords.contains("cup"))
  }
}
