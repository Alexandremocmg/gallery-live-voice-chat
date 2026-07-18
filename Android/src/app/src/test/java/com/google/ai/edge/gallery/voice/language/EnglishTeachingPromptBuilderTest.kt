package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishTeachingPromptBuilderTest {
  private val builder = EnglishTeachingPromptBuilder()

  @Test
  fun bilingualLessonContainsPedagogyWithoutLanguageMarkers() {
    val decision = EnglishLessonOrchestrator().decide(EnglishLessonState(), "Quero aprender ingles")

    val prompt = builder.build(decision)

    assertTrue(prompt.contains("ORIENTACAO PEDAGOGICA"))
    assertTrue(prompt.contains("pequeno bloco"))
    assertFalse(prompt.contains("[["))
    assertFalse(prompt.contains("Responda somente"))
  }

  @Test
  fun freeConversationKeepsPedagogyAndSelectedDialect() {
    val state = EnglishLessonState(dialect = SpeechLocale.EN_GB)
    val decision = EnglishLessonOrchestrator().decide(state, "Vamos conversar em ingles")

    val prompt = builder.build(decision)

    assertTrue(prompt.contains("Dialeto de ensino: en-GB"))
    assertTrue(prompt.contains("conversa natural"))
    assertFalse(prompt.contains("marcador", ignoreCase = true))
    assertFalse(prompt.contains("Responda somente"))
  }
}
