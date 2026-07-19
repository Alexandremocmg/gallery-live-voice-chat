package com.google.ai.edge.gallery.voice.language

enum class SpeechRecognitionRuntimeError {
  LANGUAGE_UNAVAILABLE,
  LANGUAGE_NOT_SUPPORTED,
  CANNOT_CHECK_SUPPORT,
  TRANSIENT,
}

data class SpeechRecognitionErrorRecovery(
  val languagePackStatus: LanguagePackStatus,
  val voiceNotice: String,
)

object SpeechRecognitionErrorRecoveryPolicy {
  fun recover(
    error: SpeechRecognitionRuntimeError,
    locale: SpeechLocale,
  ): SpeechRecognitionErrorRecovery? =
    when (error) {
      SpeechRecognitionRuntimeError.LANGUAGE_UNAVAILABLE ->
        SpeechRecognitionErrorRecovery(
          languagePackStatus = LanguagePackStatus.DOWNLOAD_AVAILABLE,
          voiceNotice = "Baixe o pacote offline de ${locale.shortLabel} antes de falar.",
        )
      SpeechRecognitionRuntimeError.LANGUAGE_NOT_SUPPORTED ->
        SpeechRecognitionErrorRecovery(
          languagePackStatus = LanguagePackStatus.UNSUPPORTED,
          voiceNotice = "Este idioma nao e suportado pelo reconhecimento local deste celular.",
        )
      SpeechRecognitionRuntimeError.CANNOT_CHECK_SUPPORT ->
        SpeechRecognitionErrorRecovery(
          languagePackStatus = LanguagePackStatus.UNKNOWN,
          voiceNotice =
            "Nao foi possivel verificar o suporte local de ${locale.shortLabel} neste celular.",
        )
      SpeechRecognitionRuntimeError.TRANSIENT -> null
    }
}
