package com.google.ai.edge.gallery.voice.language

class ConversationLanguageCoordinator(
  initialLocale: SpeechLocale = SpeechLocale.PT_BR,
  private val resolver: LanguageTurnResolver = LanguageTurnResolver(),
) {
  @Volatile var establishedLocale: SpeechLocale = initialLocale
    private set

  @Synchronized
  fun resolveTurn(
    result: SpeechRecognitionResult,
    englishDialect: SpeechLocale,
    englishLessonDecision: EnglishLessonDecision,
  ): LanguageTurnDecision {
    val detection = result.languageDetection
    val decision =
      resolver.resolve(
        text = result.text,
        androidDetectedLocale = detection?.locale,
        androidConfidence = detection?.confidence ?: LanguageConfidence.LOW,
        previousLocale = establishedLocale,
        englishDialect = englishDialect,
        englishLessonDecision = englishLessonDecision,
      )
    if (decision.source == LanguageDecisionSource.EXPLICIT_COMMAND) {
      establishedLocale = decision.responseLocale
    } else if (decision.confidence == LanguageConfidence.HIGH) {
      establishedLocale = decision.inputLocale
    }
    return decision
  }

  @Synchronized
  fun nextListeningLocale(englishLessonState: EnglishLessonState): SpeechLocale =
    if (englishLessonState.active) {
      englishLessonState.nextInputLocale
    } else {
      establishedLocale
    }

  @Synchronized
  fun reset(locale: SpeechLocale = SpeechLocale.PT_BR) {
    establishedLocale = locale
  }
}
