package com.google.ai.edge.gallery.voice.language

enum class TtsPlaybackRuntimeError {
  MISSING_LOCAL_VOICE,
  INCOMPLETE_LOCAL_VOICE_DATA,
  UTTERANCE_REJECTED,
  INTERRUPTED,
}

data class TtsPlaybackErrorRecovery(
  val locale: SpeechLocale,
  val localTtsAvailable: Boolean,
  val voiceNotice: String,
)

object TtsPlaybackErrorRecoveryPolicy {
  fun recover(
    error: TtsPlaybackRuntimeError,
    locale: SpeechLocale,
  ): TtsPlaybackErrorRecovery? =
    when (error) {
      TtsPlaybackRuntimeError.MISSING_LOCAL_VOICE ->
        TtsPlaybackErrorRecovery(
          locale = locale,
          localTtsAvailable = false,
          voiceNotice =
            "A voz ${locale.shortLabel} offline precisa ser instalada nas configuracoes de voz do celular.",
        )
      TtsPlaybackRuntimeError.INCOMPLETE_LOCAL_VOICE_DATA ->
        TtsPlaybackErrorRecovery(
          locale = locale,
          localTtsAvailable = false,
          voiceNotice =
            "A voz ${locale.shortLabel} local esta incompleta. Instale os dados de voz nas configuracoes do celular.",
        )
      TtsPlaybackRuntimeError.UTTERANCE_REJECTED ->
        TtsPlaybackErrorRecovery(
          locale = locale,
          localTtsAvailable = false,
          voiceNotice =
            "Nao consegui reproduzir a voz ${locale.shortLabel} local. Verifique as vozes offline do celular.",
        )
      TtsPlaybackRuntimeError.INTERRUPTED -> null
    }
}
