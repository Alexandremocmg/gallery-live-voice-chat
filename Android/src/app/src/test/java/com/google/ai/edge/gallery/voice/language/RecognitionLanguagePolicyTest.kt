package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionLanguagePolicyTest {
  @Test
  fun android31UsesEstablishedSingleLocale() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 31,
        establishedLocale = SpeechLocale.EN_GB,
        englishDialect = SpeechLocale.EN_GB,
        capabilities = installedCapabilities(SpeechLocale.EN_GB),
      )

    assertEquals(SpeechLocale.EN_GB, request.primaryLocale)
    assertEquals(listOf(SpeechLocale.EN_GB), request.allowedLocales)
    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun android33DoesNotRequestPlatformLanguageSwitching() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 33,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_US,
        capabilities = installedCapabilities(SpeechLocale.EN_US),
      )

    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun android34WithBothPacksUsesStableEstablishedLocale() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_GB,
        capabilities = installedCapabilities(SpeechLocale.EN_GB),
      )

    assertEquals(listOf(SpeechLocale.PT_BR), request.allowedLocales)
    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun android34WithMissingEnglishPackFallsBackToPrimaryLocaleOnly() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_US,
        capabilities =
          mapOf(
            SpeechLocale.PT_BR to capability(SpeechLocale.PT_BR, LanguagePackStatus.INSTALLED),
            SpeechLocale.EN_US to
              capability(SpeechLocale.EN_US, LanguagePackStatus.DOWNLOAD_AVAILABLE),
          ),
      )

    assertEquals(SpeechLocale.PT_BR, request.primaryLocale)
    assertEquals(listOf(SpeechLocale.PT_BR), request.allowedLocales)
    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun android34SystemRecognizerDoesNotEnableBilingualDetectionEvenIfStatusLooksInstalled() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_US,
        capabilities =
          mapOf(
            SpeechLocale.PT_BR to
              capability(
                SpeechLocale.PT_BR,
                LanguagePackStatus.INSTALLED,
                backend = RecognitionBackend.ANDROID_SYSTEM,
              ),
            SpeechLocale.EN_US to
              capability(
                SpeechLocale.EN_US,
                LanguagePackStatus.INSTALLED,
                backend = RecognitionBackend.ANDROID_SYSTEM,
              ),
          ),
      )

    assertEquals(SpeechLocale.PT_BR, request.primaryLocale)
    assertEquals(listOf(SpeechLocale.PT_BR), request.allowedLocales)
    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun pronunciationRepetitionForcesEnglishWithoutDetectionOrSwitching() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_GB,
        capabilities = installedCapabilities(SpeechLocale.EN_GB),
        englishActivity = EnglishActivity.REPEAT,
      )

    assertEquals(SpeechLocale.EN_GB, request.primaryLocale)
    assertEquals(listOf(SpeechLocale.EN_GB), request.allowedLocales)
    assertFalse(request.detectionEnabled)
    assertFalse(request.switchingEnabled)
  }

  @Test
  fun teacherExplanationKeepsPortugueseAsPrimaryInput() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.EN_US,
        englishDialect = SpeechLocale.EN_US,
        capabilities = installedCapabilities(SpeechLocale.EN_US),
        englishActivity = EnglishActivity.EXPLANATION,
      )

    assertEquals(SpeechLocale.PT_BR, request.primaryLocale)
  }

  @Test
  fun freeConversationKeepsSelectedEnglishDialectAsPrimaryInput() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.PT_BR,
        englishDialect = SpeechLocale.EN_GB,
        capabilities = installedCapabilities(SpeechLocale.EN_GB),
        englishActivity = EnglishActivity.FREE_CONVERSATION,
      )

    assertEquals(SpeechLocale.EN_GB, request.primaryLocale)
  }

  @Test
  fun establishedEnglishUsesTheCurrentlySelectedDialect() {
    val request =
      RecognitionLanguagePolicy.create(
        apiLevel = 34,
        establishedLocale = SpeechLocale.EN_GB,
        englishDialect = SpeechLocale.EN_US,
        capabilities = installedCapabilities(SpeechLocale.EN_US),
      )

    assertEquals(SpeechLocale.EN_US, request.primaryLocale)
    assertTrue(SpeechLocale.EN_US in request.allowedLocales)
  }

  private fun installedCapabilities(englishDialect: SpeechLocale) =
    mapOf(
      SpeechLocale.PT_BR to capability(SpeechLocale.PT_BR, LanguagePackStatus.INSTALLED),
      englishDialect to capability(englishDialect, LanguagePackStatus.INSTALLED),
    )

  private fun capability(
    locale: SpeechLocale,
    status: LanguagePackStatus,
    backend: RecognitionBackend = RecognitionBackend.ANDROID_ON_DEVICE,
  ) =
    SpeechCapability(
      locale = locale,
      backend = backend,
      languagePackStatus = status,
    )
}
