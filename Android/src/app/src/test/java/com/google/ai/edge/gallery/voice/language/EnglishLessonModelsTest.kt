package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishLessonModelsTest {
  @Test
  fun `default state keeps normal conversation in Portuguese`() {
    val state = EnglishLessonState()

    assertFalse(state.active)
    assertEquals(SpeechLocale.PT_BR, state.nextInputLocale)
    assertEquals(SpeechLocale.EN_US, state.dialect)
  }

  @Test
  fun `repeat state expects English input`() {
    val state =
      EnglishLessonState(
        active = true,
        activity = EnglishActivity.REPEAT,
        expectedPhrase = "How are you?",
        nextInputLocale = SpeechLocale.EN_US,
      )

    assertTrue(state.active)
    assertTrue(state.nextInputLocale.isEnglish)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `dialect cannot use a Portuguese locale`() {
    EnglishLessonState(dialect = SpeechLocale.PT_BR)
  }
}
