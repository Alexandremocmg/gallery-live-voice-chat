package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishTeachingPromptBuilderTest {
  private val builder = EnglishTeachingPromptBuilder()

  @Test fun `bilingual lesson requires explicit language tags`() {
    val decision = EnglishLessonOrchestrator().decide(EnglishLessonState(), "Quero aprender ingles")
    val prompt = builder.build(decision)
    assertTrue(prompt.contains("[[pt-BR]]"))
    assertTrue(prompt.contains("[[en-US]]"))
  }

  @Test fun `free conversation stays entirely in selected English dialect`() {
    val state = EnglishLessonState(dialect = SpeechLocale.EN_GB)
    val decision = EnglishLessonOrchestrator().decide(state, "Vamos conversar em ingles")
    val prompt = builder.build(decision)
    assertTrue(prompt.contains("Responda somente em ingles"))
    assertFalse(prompt.contains("Marque CADA trecho"))
  }
}
