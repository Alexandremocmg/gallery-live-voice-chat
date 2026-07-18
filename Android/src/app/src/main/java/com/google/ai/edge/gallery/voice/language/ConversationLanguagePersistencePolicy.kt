package com.google.ai.edge.gallery.voice.language

object ConversationLanguagePersistencePolicy {
  fun restore(
    persistedLanguageTag: String?,
    englishLessonState: EnglishLessonState,
  ): SpeechLocale {
    SpeechLocale.fromLanguageTag(persistedLanguageTag)?.let { return it }
    return if (englishLessonState.active && englishLessonState.nextInputLocale.isEnglish) {
      englishLessonState.nextInputLocale
    } else {
      SpeechLocale.PT_BR
    }
  }
}
