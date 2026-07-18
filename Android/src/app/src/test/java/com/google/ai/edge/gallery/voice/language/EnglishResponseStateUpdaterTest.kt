package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishResponseStateUpdaterTest {
  private val updater = EnglishResponseStateUpdater()

  @Test fun `captured pronunciation target arms English microphone after response`() {
    val decision =
      EnglishLessonDecision(
        nextState = EnglishLessonState(
          active = true,
          dialect = SpeechLocale.EN_GB,
          activity = EnglishActivity.LISTENING,
          nextInputLocale = SpeechLocale.PT_BR,
        ),
        intent = EnglishLessonIntent.PRONUNCIATION,
        baseResponseLocale = SpeechLocale.PT_BR,
      )

    val state = updater.updateAfterResponse(decision, "Good morning. Repita agora.")

    assertEquals(EnglishActivity.REPEAT, state.activity)
    assertEquals(SpeechLocale.EN_GB, state.nextInputLocale)
    assertEquals("Good morning.", state.expectedPhrase)
  }

  @Test fun `plain explanation without English target keeps Portuguese microphone`() {
    val decision =
      EnglishLessonDecision(
        nextState = EnglishLessonState(
          active = true,
          dialect = SpeechLocale.EN_US,
          activity = EnglishActivity.EXPLANATION,
          nextInputLocale = SpeechLocale.PT_BR,
        ),
        intent = EnglishLessonIntent.START,
        baseResponseLocale = SpeechLocale.PT_BR,
      )

    val state = updater.updateAfterResponse(decision, "")

    assertEquals(EnglishActivity.EXPLANATION, state.activity)
    assertEquals(SpeechLocale.PT_BR, state.nextInputLocale)
  }
}
