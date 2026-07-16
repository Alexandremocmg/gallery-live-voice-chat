package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishLessonOrchestratorTest {
  private val orchestrator = EnglishLessonOrchestrator()

  @Test fun `study request starts bilingual lesson`() {
    val decision = orchestrator.decide(EnglishLessonState(), "Quero aprender ingles do zero")
    assertTrue(decision.nextState.active)
    assertEquals(EnglishActivity.EXPLANATION, decision.nextState.activity)
    assertEquals(SpeechLocale.PT_BR, decision.nextState.nextInputLocale)
  }

  @Test fun `conversation request keeps microphone in English`() {
    val decision = orchestrator.decide(EnglishLessonState(), "Vamos conversar em ingles")
    assertEquals(EnglishActivity.FREE_CONVERSATION, decision.nextState.activity)
    assertEquals(SpeechLocale.EN_US, decision.nextState.nextInputLocale)
    assertEquals(SpeechLocale.EN_US, decision.baseResponseLocale)
  }

  @Test fun `repeat switches the next microphone to selected dialect`() {
    val active = EnglishLessonState(active = true, activity = EnglishActivity.LISTENING, dialect = SpeechLocale.EN_GB)
    val decision = orchestrator.decide(active, "Quero repetir")
    assertEquals(EnglishActivity.REPEAT, decision.nextState.activity)
    assertEquals(SpeechLocale.EN_GB, decision.nextState.nextInputLocale)
  }

  @Test fun `English input after repeat becomes learner attempt`() {
    val repeat = EnglishLessonState(active = true, activity = EnglishActivity.REPEAT, nextInputLocale = SpeechLocale.EN_US)
    val decision = orchestrator.decide(repeat, "How are you")
    assertEquals(EnglishLessonIntent.LEARNER_ATTEMPT, decision.intent)
    assertEquals(EnglishActivity.PRONUNCIATION_FEEDBACK, decision.nextState.activity)
    assertEquals(SpeechLocale.PT_BR, decision.nextState.nextInputLocale)
  }

  @Test fun `exit resets lesson to Portuguese`() {
    val active = EnglishLessonState(active = true, activity = EnglishActivity.FREE_CONVERSATION, nextInputLocale = SpeechLocale.EN_US)
    val decision = orchestrator.decide(active, "Volte para o portugues")
    assertFalse(decision.nextState.active)
    assertEquals(SpeechLocale.PT_BR, decision.nextState.nextInputLocale)
  }

  @Test fun `English voice commands work during conversation`() {
    val active = EnglishLessonState(active = true, activity = EnglishActivity.FREE_CONVERSATION, nextInputLocale = SpeechLocale.EN_US)
    val slower = orchestrator.decide(active, "Speak more slowly")
    assertEquals(EnglishLessonIntent.SLOWER, slower.intent)

    val exit = orchestrator.decide(active, "Back to Portuguese")
    assertEquals(EnglishLessonIntent.EXIT, exit.intent)
  }
}
