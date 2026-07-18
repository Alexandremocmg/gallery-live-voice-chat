package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationLanguageCoordinatorTest {
  private val inactiveLesson =
    EnglishLessonDecision(
      nextState = EnglishLessonState(),
      intent = EnglishLessonIntent.NONE,
      baseResponseLocale = SpeechLocale.PT_BR,
    )

  @Test
  fun highConfidenceEnglishTurnEstablishesEnglish() {
    val coordinator = ConversationLanguageCoordinator()

    val decision =
      coordinator.resolveTurn(
        result =
          recognitionResult(
            text = "Can you explain how this works",
            detectedLocale = SpeechLocale.EN_US,
            confidence = LanguageConfidence.HIGH,
          ),
        englishDialect = SpeechLocale.EN_US,
        englishLessonDecision = inactiveLesson,
      )

    assertEquals(ResponseLanguageMode.SINGLE_ENGLISH, decision.responseMode)
    assertEquals(SpeechLocale.EN_US, coordinator.establishedLocale)
  }

  @Test
  fun weakContradictoryTurnDoesNotReplaceEstablishedLocale() {
    val coordinator = ConversationLanguageCoordinator(initialLocale = SpeechLocale.EN_GB)

    coordinator.resolveTurn(
      result =
        recognitionResult(
          text = "okay",
          detectedLocale = SpeechLocale.PT_BR,
          confidence = LanguageConfidence.LOW,
        ),
      englishDialect = SpeechLocale.EN_GB,
      englishLessonDecision = inactiveLesson,
    )

    assertEquals(SpeechLocale.EN_GB, coordinator.establishedLocale)
  }

  @Test
  fun explicitLanguageCommandEstablishesRequestedResponseLanguage() {
    val coordinator = ConversationLanguageCoordinator(initialLocale = SpeechLocale.PT_BR)

    val decision =
      coordinator.resolveTurn(
        result = recognitionResult("Agora responda em ingles"),
        englishDialect = SpeechLocale.EN_GB,
        englishLessonDecision = inactiveLesson,
      )

    assertEquals(LanguageDecisionSource.EXPLICIT_COMMAND, decision.source)
    assertEquals(SpeechLocale.EN_GB, coordinator.establishedLocale)
  }

  @Test
  fun normalConversationListensInEstablishedLocale() {
    val coordinator = ConversationLanguageCoordinator(initialLocale = SpeechLocale.EN_US)

    assertEquals(
      SpeechLocale.EN_US,
      coordinator.nextListeningLocale(EnglishLessonState()),
    )
  }

  @Test
  fun activeEnglishLessonKeepsItsExplicitNextInputLocale() {
    val coordinator = ConversationLanguageCoordinator(initialLocale = SpeechLocale.EN_US)
    val lesson =
      EnglishLessonState(
        active = true,
        activity = EnglishActivity.EXPLANATION,
        nextInputLocale = SpeechLocale.PT_BR,
      )

    assertEquals(SpeechLocale.PT_BR, coordinator.nextListeningLocale(lesson))
  }

  @Test
  fun resetReturnsConversationToPortuguese() {
    val coordinator = ConversationLanguageCoordinator(initialLocale = SpeechLocale.EN_US)

    coordinator.reset()

    assertEquals(SpeechLocale.PT_BR, coordinator.establishedLocale)
  }

  private fun recognitionResult(
    text: String,
    detectedLocale: SpeechLocale? = null,
    confidence: LanguageConfidence = LanguageConfidence.LOW,
  ) =
    SpeechRecognitionResult(
      text = text,
      locale = SpeechLocale.PT_BR,
      backend = RecognitionBackend.ANDROID_ON_DEVICE,
      languageDetection =
        detectedLocale?.let { locale -> SpeechLanguageDetection(locale, confidence) },
    )
}
