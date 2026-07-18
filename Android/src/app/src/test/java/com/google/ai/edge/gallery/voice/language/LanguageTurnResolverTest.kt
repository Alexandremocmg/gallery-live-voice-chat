package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTurnResolverTest {
  private val resolver = LanguageTurnResolver()

  @Test
  fun explicitEnglishCommandOverridesAndroidDetectionAndContext() {
    val decision =
      resolver.resolve(
        text = "Agora fale em ingles",
        androidDetectedLocale = SpeechLocale.PT_BR,
        androidConfidence = LanguageConfidence.HIGH,
        previousLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_GB,
      )

    assertEquals(SpeechLocale.PT_BR, decision.inputLocale)
    assertEquals(SpeechLocale.EN_GB, decision.responseLocale)
    assertEquals(ResponseLanguageMode.SINGLE_ENGLISH, decision.responseMode)
    assertEquals(LanguageDecisionSource.EXPLICIT_COMMAND, decision.source)
  }

  @Test
  fun explicitPortugueseCommandOverridesEnglishDetection() {
    val decision =
      resolver.resolve(
        text = "Back to Portuguese",
        androidDetectedLocale = SpeechLocale.EN_US,
        androidConfidence = LanguageConfidence.HIGH,
        previousLocale = SpeechLocale.EN_US,
      )

    assertEquals(SpeechLocale.PT_BR, decision.responseLocale)
    assertEquals(ResponseLanguageMode.SINGLE_PORTUGUESE, decision.responseMode)
  }

  @Test
  fun EnglishTeacherExplanationUsesBilingualMode() {
    val lesson =
      EnglishLessonDecision(
        nextState =
          EnglishLessonState(
            active = true,
            activity = EnglishActivity.EXPLANATION,
          ),
        intent = EnglishLessonIntent.START,
        baseResponseLocale = SpeechLocale.PT_BR,
      )

    val decision =
      resolver.resolve(
        text = "Quero aprender ingles",
        previousLocale = SpeechLocale.PT_BR,
        englishLessonDecision = lesson,
      )

    assertEquals(ResponseLanguageMode.BILINGUAL_TEACHING, decision.responseMode)
    assertEquals(SpeechLocale.PT_BR, decision.responseLocale)
    assertEquals(LanguageDecisionSource.ENGLISH_TEACHER, decision.source)
  }

  @Test
  fun EnglishTeacherFreeConversationUsesSelectedDialectOnly() {
    val lesson =
      EnglishLessonDecision(
        nextState =
          EnglishLessonState(
            active = true,
            dialect = SpeechLocale.EN_GB,
            activity = EnglishActivity.FREE_CONVERSATION,
            nextInputLocale = SpeechLocale.EN_GB,
          ),
        intent = EnglishLessonIntent.FREE_CONVERSATION,
        baseResponseLocale = SpeechLocale.EN_GB,
      )

    val decision =
      resolver.resolve(
        text = "Let's talk about music",
        previousLocale = SpeechLocale.EN_GB,
        englishDialect = SpeechLocale.EN_GB,
        englishLessonDecision = lesson,
      )

    assertEquals(ResponseLanguageMode.SINGLE_ENGLISH, decision.responseMode)
    assertEquals(SpeechLocale.EN_GB, decision.responseLocale)
  }

  @Test
  fun confidentAndroidDetectionPrecedesTranscriptAnalysis() {
    val decision =
      resolver.resolve(
        text = "Quero entender como isso funciona",
        androidDetectedLocale = SpeechLocale.EN_US,
        androidConfidence = LanguageConfidence.HIGH,
        previousLocale = SpeechLocale.PT_BR,
      )

    assertEquals(SpeechLocale.EN_US, decision.inputLocale)
    assertEquals(ResponseLanguageMode.SINGLE_ENGLISH, decision.responseMode)
    assertEquals(LanguageDecisionSource.ANDROID_DETECTION, decision.source)
  }

  @Test
  fun lowConfidenceAndroidDetectionFallsBackToText() {
    val decision =
      resolver.resolve(
        text = "Can you explain how this works",
        androidDetectedLocale = SpeechLocale.PT_BR,
        androidConfidence = LanguageConfidence.LOW,
        previousLocale = SpeechLocale.PT_BR,
      )

    assertEquals(SpeechLocale.EN_US, decision.inputLocale)
    assertEquals(LanguageDecisionSource.TEXT_ANALYSIS, decision.source)
  }

  @Test
  fun ambiguousFollowUpInheritsConversationLanguage() {
    val decision =
      resolver.resolve(
        text = "okay",
        previousLocale = SpeechLocale.EN_GB,
        englishDialect = SpeechLocale.EN_GB,
      )

    assertEquals(SpeechLocale.EN_GB, decision.inputLocale)
    assertEquals(SpeechLocale.EN_GB, decision.responseLocale)
    assertEquals(LanguageDecisionSource.CONVERSATION_CONTEXT, decision.source)
  }

  @Test
  fun newAmbiguousSessionDefaultsToPortuguese() {
    val decision = resolver.resolve(text = "Gemma")

    assertEquals(SpeechLocale.PT_BR, decision.inputLocale)
    assertEquals(SpeechLocale.PT_BR, decision.responseLocale)
    assertEquals(LanguageDecisionSource.DEFAULT, decision.source)
  }
}
