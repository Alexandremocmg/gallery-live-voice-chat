package com.google.ai.edge.gallery.voice.language

enum class LanguageSwitchingSensitivity {
  BALANCED,
  HIGH_PRECISION,
  QUICK_RESPONSE,
}

data class SpeechRecognitionRequest(
  val primaryLocale: SpeechLocale,
  val allowedLocales: List<SpeechLocale> = listOf(primaryLocale),
  val detectionEnabled: Boolean = false,
  val switchingEnabled: Boolean = false,
  val switchingSensitivity: LanguageSwitchingSensitivity = LanguageSwitchingSensitivity.BALANCED,
  val possiblyCompleteSilenceMs: Long = DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS,
  val completeSilenceMs: Long = DEFAULT_COMPLETE_SILENCE_MS,
  val automaticHandsFree: Boolean = false,
) {
  init {
    require(primaryLocale in allowedLocales) { "Primary locale must be allowed" }
    require(allowedLocales.isNotEmpty()) { "At least one locale must be allowed" }
    require(allowedLocales.distinct().size == allowedLocales.size) { "Allowed locales must be unique" }
    require(!switchingEnabled || detectionEnabled) { "Language switching requires detection" }
    require(!switchingEnabled || allowedLocales.size > 1) {
      "Language switching requires multiple locales"
    }
  }

  companion object {
    fun single(locale: SpeechLocale): SpeechRecognitionRequest =
      SpeechRecognitionRequest(primaryLocale = locale)
  }
}

object RecognitionLanguagePolicy {
  fun create(
    apiLevel: Int,
    establishedLocale: SpeechLocale,
    englishDialect: SpeechLocale,
    capabilities: Map<SpeechLocale, SpeechCapability>,
    englishActivity: EnglishActivity = EnglishActivity.NONE,
    languageDetectionSupported: Boolean = apiLevel >= ANDROID_14_API_LEVEL,
  ): SpeechRecognitionRequest {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }

    if (englishActivity == EnglishActivity.REPEAT) {
      return SpeechRecognitionRequest.single(englishDialect)
    }

    val normalizedEstablishedLocale =
      if (establishedLocale.isEnglish) englishDialect else SpeechLocale.PT_BR
    val primaryLocale =
      when (englishActivity) {
        EnglishActivity.FREE_CONVERSATION -> englishDialect
        EnglishActivity.EXPLANATION,
        EnglishActivity.LISTENING,
        EnglishActivity.PRONUNCIATION_FEEDBACK,
        -> SpeechLocale.PT_BR
        EnglishActivity.NONE -> normalizedEstablishedLocale
        EnglishActivity.REPEAT -> englishDialect
      }

    // On-device multilingual detection/switching is not reliable enough to own the microphone.
    // Physical-device logcat showed it repeatedly closing a ready recognition session with
    // NO_MATCH after about 1.2 seconds, before onBeginningOfSpeech. The conversation state
    // already determines whether this turn expects Portuguese or the selected English dialect,
    // so use that locale directly and keep bilingual routing above the platform recognizer.
    return SpeechRecognitionRequest.single(primaryLocale)
  }
}

private const val ANDROID_14_API_LEVEL = 34
private const val DEFAULT_POSSIBLY_COMPLETE_SILENCE_MS = 1_000L
private const val DEFAULT_COMPLETE_SILENCE_MS = 1_500L
