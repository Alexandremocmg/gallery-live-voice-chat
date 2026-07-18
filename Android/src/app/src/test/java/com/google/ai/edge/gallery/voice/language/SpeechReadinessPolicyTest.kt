package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechReadinessPolicyTest {
  @Test
  fun languageIndicatorShowsPortugueseAndEnglishDialects() {
    assertEquals("PT", SpeechReadinessPolicy.languageIndicatorLabel(SpeechLocale.PT_BR, false))
    assertEquals("EN-US", SpeechReadinessPolicy.languageIndicatorLabel(SpeechLocale.EN_US, false))
    assertEquals("EN-GB", SpeechReadinessPolicy.languageIndicatorLabel(SpeechLocale.EN_GB, false))
  }

  @Test
  fun languageIndicatorShowsAutomaticSwitchingOnlyWhileActive() {
    assertEquals(
      "PT/EN automatico",
      SpeechReadinessPolicy.languageIndicatorLabel(SpeechLocale.EN_US, true),
    )
  }

  @Test
  fun reportsGeneralRecognitionUnavailableSeparately() {
    val state =
      evaluate(
        capabilities = capabilities(RecognitionBackend.UNAVAILABLE, LanguagePackStatus.UNKNOWN),
        localTtsAvailabilityKnown = true,
      )

    assertTrue(state.hasNotice(SpeechReadinessNoticeKind.RECOGNITION_UNAVAILABLE))
    assertFalse(state.isBilingualOfflineReady)
  }

  @Test
  fun reportsMissingRecognitionPackSeparately() {
    val capabilities = readyCapabilities().toMutableMap()
    capabilities[SpeechLocale.EN_US] =
      capability(
        SpeechLocale.EN_US,
        RecognitionBackend.ANDROID_ON_DEVICE,
        LanguagePackStatus.DOWNLOAD_AVAILABLE,
      )

    val state = evaluate(capabilities)

    assertTrue(state.hasNotice(SpeechReadinessNoticeKind.RECOGNITION_PACK_MISSING))
    assertFalse(state.hasNotice(SpeechReadinessNoticeKind.LOCAL_TTS_VOICE_MISSING))
  }

  @Test
  fun reportsMissingLocalVoiceSeparately() {
    val capabilities = readyCapabilities().toMutableMap()
    capabilities[SpeechLocale.EN_US] =
      capability(
        SpeechLocale.EN_US,
        RecognitionBackend.ANDROID_ON_DEVICE,
        LanguagePackStatus.INSTALLED,
        localTtsAvailable = false,
      )

    val state = evaluate(capabilities)

    assertTrue(state.hasNotice(SpeechReadinessNoticeKind.LOCAL_TTS_VOICE_MISSING))
    assertFalse(state.isBilingualOfflineReady)
  }

  @Test
  fun reportsAutomaticSwitchingUnsupportedSeparately() {
    val state = evaluate(readyCapabilities(), apiLevel = 33)

    assertTrue(state.hasNotice(SpeechReadinessNoticeKind.AUTOMATIC_SWITCHING_UNSUPPORTED))
    assertFalse(state.isBilingualOfflineReady)
  }

  @Test
  fun claimsOfflineReadinessOnlyWithBothPacksAndBothLocalVoices() {
    val state = evaluate(readyCapabilities())

    assertTrue(state.isBilingualOfflineReady)
    assertEquals(
      listOf(SpeechReadinessNoticeKind.READY),
      state.notices.map(SpeechReadinessNotice::kind),
    )
  }

  @Test
  fun waitsForCapabilityAndTtsChecksBeforeShowingResourceNotices() {
    val missingCapability = evaluate(mapOf(SpeechLocale.PT_BR to readyCapability(SpeechLocale.PT_BR)))
    val ttsStillLoading = evaluate(readyCapabilities(), localTtsAvailabilityKnown = false)

    assertTrue(missingCapability.notices.isEmpty())
    assertFalse(ttsStillLoading.isBilingualOfflineReady)
    assertFalse(ttsStillLoading.hasNotice(SpeechReadinessNoticeKind.LOCAL_TTS_VOICE_MISSING))
  }

  private fun evaluate(
    capabilities: Map<SpeechLocale, SpeechCapability>,
    apiLevel: Int = 34,
    localTtsAvailabilityKnown: Boolean = true,
  ) =
    SpeechReadinessPolicy.evaluate(
      apiLevel = apiLevel,
      capabilities = capabilities,
      englishDialect = SpeechLocale.EN_US,
      localTtsAvailabilityKnown = localTtsAvailabilityKnown,
    )

  private fun readyCapabilities(): Map<SpeechLocale, SpeechCapability> =
    mapOf(
      SpeechLocale.PT_BR to readyCapability(SpeechLocale.PT_BR),
      SpeechLocale.EN_US to readyCapability(SpeechLocale.EN_US),
    )

  private fun capabilities(
    backend: RecognitionBackend,
    status: LanguagePackStatus,
  ): Map<SpeechLocale, SpeechCapability> =
    mapOf(
      SpeechLocale.PT_BR to capability(SpeechLocale.PT_BR, backend, status),
      SpeechLocale.EN_US to capability(SpeechLocale.EN_US, backend, status),
    )

  private fun readyCapability(locale: SpeechLocale) =
    capability(locale, RecognitionBackend.ANDROID_ON_DEVICE, LanguagePackStatus.INSTALLED)

  private fun capability(
    locale: SpeechLocale,
    backend: RecognitionBackend,
    status: LanguagePackStatus,
    localTtsAvailable: Boolean = true,
  ) =
    SpeechCapability(
      locale = locale,
      backend = backend,
      languagePackStatus = status,
      localTtsAvailable = localTtsAvailable,
    )

  private fun SpeechReadinessState.hasNotice(kind: SpeechReadinessNoticeKind): Boolean =
    notices.any { it.kind == kind }
}
