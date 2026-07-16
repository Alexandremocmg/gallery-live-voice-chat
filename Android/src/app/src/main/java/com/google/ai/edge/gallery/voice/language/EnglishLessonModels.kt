package com.google.ai.edge.gallery.voice.language

enum class EnglishActivity {
  NONE,
  EXPLANATION,
  LISTENING,
  REPEAT,
  FREE_CONVERSATION,
  PRONUNCIATION_FEEDBACK,
}

data class EnglishLessonState(
  val active: Boolean = false,
  val dialect: SpeechLocale = SpeechLocale.EN_US,
  val activity: EnglishActivity = EnglishActivity.NONE,
  val expectedPhrase: String? = null,
  val nextInputLocale: SpeechLocale = SpeechLocale.PT_BR,
  val demonstrationRate: Float = NORMAL_ENGLISH_RATE,
  val attemptCount: Int = 0,
) {
  init {
    require(dialect.isEnglish) { "English dialect must use an English locale" }
  }
}

const val NORMAL_ENGLISH_RATE = 0.91f
const val SLOW_ENGLISH_RATE = 0.81f
