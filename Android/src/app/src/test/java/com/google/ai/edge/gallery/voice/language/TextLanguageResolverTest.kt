package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLanguageResolverTest {
  private val resolver = TextLanguageResolver()

  @Test
  fun detectsPortugueseSentence() {
    val result = resolver.resolve("Quero entender como isso funciona")

    assertEquals(SpeechLocale.PT_BR, result.locale)
    assertFalse(result.inheritedFromContext)
  }

  @Test
  fun detectsEnglishSentenceAndKeepsSelectedDialect() {
    val result =
      resolver.resolve(
        text = "Can you explain how this works",
        englishDialect = SpeechLocale.EN_GB,
      )

    assertEquals(SpeechLocale.EN_GB, result.locale)
    assertFalse(result.inheritedFromContext)
  }

  @Test
  fun ambiguousShortTurnsRemainUnknownWithoutContext() {
    listOf("okay", "sim", "API", "Gemma").forEach { text ->
      assertNull("text=$text", resolver.resolve(text).locale)
    }
  }

  @Test
  fun ambiguousTurnsInheritConversationLocale() {
    val result =
      resolver.resolve(
        text = "okay API",
        previousLocale = SpeechLocale.EN_US,
      )

    assertEquals(SpeechLocale.EN_US, result.locale)
    assertEquals(LanguageConfidence.LOW, result.confidence)
    assertTrue(result.inheritedFromContext)
  }

  @Test
  fun mixedTechnicalSentenceDoesNotOverridePortugueseContext() {
    val result =
      resolver.resolve(
        text = "endpoint returns JSON",
        previousLocale = SpeechLocale.PT_BR,
      )

    assertEquals(SpeechLocale.PT_BR, result.locale)
    assertTrue(result.inheritedFromContext)
  }

  @Test
  fun punctuationAndAccentsKeepPortugueseDetection() {
    val result = resolver.resolve("Você pode explicar, por favor, como estão as coisas?")

    assertEquals(SpeechLocale.PT_BR, result.locale)
    assertEquals(LanguageConfidence.HIGH, result.confidence)
  }

  @Test
  fun apostropheContractionsSupportEnglishDetection() {
    val result = resolver.resolve("I don't understand why it doesn't work")

    assertEquals(SpeechLocale.EN_US, result.locale)
  }
}
