package com.google.ai.edge.gallery.voice.language

enum class SpeechReadinessNoticeKind {
  READY,
  RECOGNITION_UNAVAILABLE,
  RECOGNITION_PACK_MISSING,
  LOCAL_TTS_VOICE_MISSING,
  AUTOMATIC_SWITCHING_UNSUPPORTED,
}

data class SpeechReadinessNotice(
  val kind: SpeechReadinessNoticeKind,
  val message: String,
)

data class SpeechReadinessAction(
  val locale: SpeechLocale,
  val label: String,
  val enabled: Boolean,
)

data class SpeechReadinessSettingsAction(
  val locales: List<SpeechLocale>,
  val label: String,
  val enabled: Boolean,
)

data class SpeechReadinessState(
  val isBilingualOfflineReady: Boolean,
  val notices: List<SpeechReadinessNotice>,
  val displayNotices: List<SpeechReadinessNotice> = notices,
  val languagePackDownloadActions: List<SpeechReadinessAction> = emptyList(),
  val localVoiceSettingsAction: SpeechReadinessSettingsAction? = null,
)

object SpeechReadinessPolicy {
  fun languageIndicatorLabel(
    locale: SpeechLocale,
    automaticSwitchingActive: Boolean,
  ): String =
    if (automaticSwitchingActive) {
      "PT/EN automatico"
    } else {
      locale.languageTag.uppercase()
        .replace("PT-BR", "PT")
    }

  fun evaluate(
    apiLevel: Int,
    capabilities: Map<SpeechLocale, SpeechCapability>,
    englishDialect: SpeechLocale,
    localTtsAvailabilityKnown: Boolean,
  ): SpeechReadinessState {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }
    val requiredLocales = listOf(SpeechLocale.PT_BR, englishDialect)
    if (requiredLocales.any { it !in capabilities }) {
      return SpeechReadinessState(isBilingualOfflineReady = false, notices = emptyList())
    }

    val requiredCapabilities = requiredLocales.associateWith { capabilities.getValue(it) }
    val recognitionReady = requiredCapabilities.values.all { it.isRecognitionReadyOffline }
    val localVoicesReady =
      localTtsAvailabilityKnown && requiredCapabilities.values.all { it.localTtsAvailable }
    val automaticSwitchingSupported = apiLevel >= ANDROID_14_API_LEVEL
    val isReady = recognitionReady && localVoicesReady && automaticSwitchingSupported
    if (isReady) {
      return SpeechReadinessState(
        isBilingualOfflineReady = true,
        notices =
          listOf(
            SpeechReadinessNotice(
              SpeechReadinessNoticeKind.READY,
              "PT/EN prontos para conversa offline.",
            )
          ),
      )
    }

    val notices = mutableListOf<SpeechReadinessNotice>()
    val languagePackDownloadActions = mutableListOf<SpeechReadinessAction>()
    var localVoiceSettingsAction: SpeechReadinessSettingsAction? = null
    if (requiredCapabilities.values.all { it.backend == RecognitionBackend.UNAVAILABLE }) {
      notices +=
        SpeechReadinessNotice(
          SpeechReadinessNoticeKind.RECOGNITION_UNAVAILABLE,
          "Reconhecimento de voz indisponivel neste celular.",
        )
    } else if (!recognitionReady) {
      val unavailableLocales =
        requiredCapabilities.filterValues { !it.isRecognitionReadyOffline }.keys
      val labels = unavailableLocales.joinToString(" e ", transform = ::localeReadinessLabel)
      val hasDownload = unavailableLocales.any { locale ->
        requiredCapabilities.getValue(locale).languagePackStatus ==
          LanguagePackStatus.DOWNLOAD_AVAILABLE
      }
      val hasPendingDownload = unavailableLocales.any { locale ->
        requiredCapabilities.getValue(locale).languagePackStatus ==
          LanguagePackStatus.DOWNLOAD_PENDING
      }
      notices +=
        SpeechReadinessNotice(
          SpeechReadinessNoticeKind.RECOGNITION_PACK_MISSING,
          when {
            hasDownload -> "Pacote de reconhecimento offline ausente: $labels."
            hasPendingDownload -> "Download do reconhecimento offline em andamento: $labels."
            else -> "Reconhecimento offline nao confirmado: $labels."
          },
        )
      languagePackDownloadActions +=
        unavailableLocales
          .filter { locale ->
            requiredCapabilities.getValue(locale).languagePackStatus in
              setOf(LanguagePackStatus.DOWNLOAD_AVAILABLE, LanguagePackStatus.DOWNLOAD_PENDING)
          }
          .map { locale ->
            val pending =
              requiredCapabilities.getValue(locale).languagePackStatus ==
                LanguagePackStatus.DOWNLOAD_PENDING
            SpeechReadinessAction(
              locale = locale,
              label =
                if (pending) {
                  "Baixando reconhecimento ${localeReadinessLabel(locale)}"
                } else {
                  "Baixar reconhecimento ${localeReadinessLabel(locale)}"
                },
              enabled = !pending,
            )
          }
    }

    if (localTtsAvailabilityKnown) {
      val missingVoiceLocales =
        requiredCapabilities.filterValues { !it.localTtsAvailable }.keys
      if (missingVoiceLocales.isNotEmpty()) {
        notices +=
          SpeechReadinessNotice(
            SpeechReadinessNoticeKind.LOCAL_TTS_VOICE_MISSING,
            "Voz local offline ausente: " +
              missingVoiceLocales.joinToString(" e ", transform = ::localeReadinessLabel) +
              ".",
          )
        localVoiceSettingsAction =
          SpeechReadinessSettingsAction(
            locales = missingVoiceLocales.toList(),
            label =
              "Configurar vozes offline: " +
                missingVoiceLocales.joinToString(" e ", transform = ::localeReadinessLabel),
            enabled = true,
          )
      }
    }

    if (!automaticSwitchingSupported) {
      notices +=
        SpeechReadinessNotice(
          SpeechReadinessNoticeKind.AUTOMATIC_SWITCHING_UNSUPPORTED,
          "Alternancia automatica PT/EN indisponivel neste Android.",
        )
    }
    return SpeechReadinessState(
      isBilingualOfflineReady = false,
      notices = notices,
      displayNotices = primaryDisplayNotices(notices),
      languagePackDownloadActions = languagePackDownloadActions,
      localVoiceSettingsAction = localVoiceSettingsAction,
    )
  }
}

private fun primaryDisplayNotices(
  notices: List<SpeechReadinessNotice>,
): List<SpeechReadinessNotice> {
  if (notices.isEmpty()) return emptyList()
  val priority =
    listOf(
      SpeechReadinessNoticeKind.READY,
      SpeechReadinessNoticeKind.RECOGNITION_UNAVAILABLE,
      SpeechReadinessNoticeKind.RECOGNITION_PACK_MISSING,
      SpeechReadinessNoticeKind.LOCAL_TTS_VOICE_MISSING,
      SpeechReadinessNoticeKind.AUTOMATIC_SWITCHING_UNSUPPORTED,
    )
  return priority.firstNotNullOfOrNull { kind -> notices.firstOrNull { it.kind == kind } }
    ?.let(::listOf)
    ?: notices.take(1)
}

private fun localeReadinessLabel(locale: SpeechLocale): String =
  when (locale) {
    SpeechLocale.PT_BR -> "PT"
    SpeechLocale.EN_US -> "EN-US"
    SpeechLocale.EN_GB -> "EN-GB"
  }

private const val ANDROID_14_API_LEVEL = 34
