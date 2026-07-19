package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechRecognitionErrorRecoveryPolicyTest {
  @Test
  fun languageUnavailableMarksPackDownloadAvailableWithDownloadNotice() {
    val recovery =
      SpeechRecognitionErrorRecoveryPolicy.recover(
        error = SpeechRecognitionRuntimeError.LANGUAGE_UNAVAILABLE,
        locale = SpeechLocale.EN_US,
      )

    assertEquals(LanguagePackStatus.DOWNLOAD_AVAILABLE, recovery?.languagePackStatus)
    assertEquals(
      "Baixe o pacote offline de EN antes de falar.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun languageNotSupportedMarksPackUnsupportedWithSupportNotice() {
    val recovery =
      SpeechRecognitionErrorRecoveryPolicy.recover(
        error = SpeechRecognitionRuntimeError.LANGUAGE_NOT_SUPPORTED,
        locale = SpeechLocale.PT_BR,
      )

    assertEquals(LanguagePackStatus.UNSUPPORTED, recovery?.languagePackStatus)
    assertEquals(
      "Este idioma nao e suportado pelo reconhecimento local deste celular.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun cannotCheckSupportMarksPackUnknownWithoutPretendingDownloadIsAvailable() {
    val recovery =
      SpeechRecognitionErrorRecoveryPolicy.recover(
        error = SpeechRecognitionRuntimeError.CANNOT_CHECK_SUPPORT,
        locale = SpeechLocale.EN_GB,
      )

    assertEquals(LanguagePackStatus.UNKNOWN, recovery?.languagePackStatus)
    assertEquals(
      "Nao foi possivel verificar o suporte local de EN neste celular.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun transientErrorsDoNotChangeLanguageCapability() {
    assertNull(
      SpeechRecognitionErrorRecoveryPolicy.recover(
        error = SpeechRecognitionRuntimeError.TRANSIENT,
        locale = SpeechLocale.PT_BR,
      )
    )
  }
}
