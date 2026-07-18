package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationLanguagePersistencePolicyTest {
  @Test
  fun restoresPersistedPortuguese() {
    val state = activeEnglishState(nextInputLocale = SpeechLocale.EN_US)

    val restored = ConversationLanguagePersistencePolicy.restore("pt-BR", state)

    assertEquals(SpeechLocale.PT_BR, restored)
  }

  @Test
  fun restoresPersistedEnglishDialect() {
    val restored =
      ConversationLanguagePersistencePolicy.restore(
        persistedLanguageTag = "en-GB",
        englishLessonState = EnglishLessonState(),
      )

    assertEquals(SpeechLocale.EN_GB, restored)
  }

  @Test
  fun legacySessionDefaultsToPortuguese() {
    val restored =
      ConversationLanguagePersistencePolicy.restore(
        persistedLanguageTag = "",
        englishLessonState = EnglishLessonState(),
      )

    assertEquals(SpeechLocale.PT_BR, restored)
  }

  @Test
  fun legacyEnglishLessonRestoresItsEstablishedEnglishInput() {
    val restored =
      ConversationLanguagePersistencePolicy.restore(
        persistedLanguageTag = "",
        englishLessonState = activeEnglishState(nextInputLocale = SpeechLocale.EN_GB),
      )

    assertEquals(SpeechLocale.EN_GB, restored)
  }

  @Test
  fun legacyEnglishExplanationStillRestoresPortugueseInput() {
    val restored =
      ConversationLanguagePersistencePolicy.restore(
        persistedLanguageTag = "",
        englishLessonState = activeEnglishState(nextInputLocale = SpeechLocale.PT_BR),
      )

    assertEquals(SpeechLocale.PT_BR, restored)
  }

  @Test
  fun unsupportedPersistedTagUsesLegacyFallback() {
    val restored =
      ConversationLanguagePersistencePolicy.restore(
        persistedLanguageTag = "fr-FR",
        englishLessonState = activeEnglishState(nextInputLocale = SpeechLocale.EN_US),
      )

    assertEquals(SpeechLocale.EN_US, restored)
  }

  private fun activeEnglishState(nextInputLocale: SpeechLocale) =
    EnglishLessonState(
      active = true,
      activity = EnglishActivity.FREE_CONVERSATION,
      nextInputLocale = nextInputLocale,
    )
}
