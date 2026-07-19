package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsPlaybackErrorRecoveryPolicyTest {
  @Test
  fun missingLocalVoiceMarksTtsUnavailableWithInstallNotice() {
    val recovery =
      TtsPlaybackErrorRecoveryPolicy.recover(
        error = TtsPlaybackRuntimeError.MISSING_LOCAL_VOICE,
        locale = SpeechLocale.EN_US,
      )

    assertEquals(SpeechLocale.EN_US, recovery?.locale)
    assertEquals(false, recovery?.localTtsAvailable)
    assertEquals(
      "A voz EN offline precisa ser instalada nas configuracoes de voz do celular.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun incompleteVoiceDataMarksTtsUnavailableWithSettingsNotice() {
    val recovery =
      TtsPlaybackErrorRecoveryPolicy.recover(
        error = TtsPlaybackRuntimeError.INCOMPLETE_LOCAL_VOICE_DATA,
        locale = SpeechLocale.PT_BR,
      )

    assertEquals(SpeechLocale.PT_BR, recovery?.locale)
    assertEquals(false, recovery?.localTtsAvailable)
    assertEquals(
      "A voz PT local esta incompleta. Instale os dados de voz nas configuracoes do celular.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun rejectedUtteranceMarksTtsUnavailableForActiveChunk() {
    val recovery =
      TtsPlaybackErrorRecoveryPolicy.recover(
        error = TtsPlaybackRuntimeError.UTTERANCE_REJECTED,
        locale = SpeechLocale.EN_GB,
      )

    assertEquals(SpeechLocale.EN_GB, recovery?.locale)
    assertEquals(false, recovery?.localTtsAvailable)
    assertEquals(
      "Nao consegui reproduzir a voz EN local. Verifique as vozes offline do celular.",
      recovery?.voiceNotice,
    )
  }

  @Test
  fun interruptedPlaybackDoesNotChangeTtsCapability() {
    assertNull(
      TtsPlaybackErrorRecoveryPolicy.recover(
        error = TtsPlaybackRuntimeError.INTERRUPTED,
        locale = SpeechLocale.PT_BR,
      )
    )
  }
}
