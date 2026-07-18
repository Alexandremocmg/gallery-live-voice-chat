package com.google.ai.edge.gallery.voice.language

class EnglishResponseStateUpdater {
  fun updateAfterResponse(
    decision: EnglishLessonDecision,
    generatedEnglish: String,
  ): EnglishLessonState {
    val shouldCaptureTarget =
      decision.intent in
        setOf(
          EnglishLessonIntent.START,
          EnglishLessonIntent.PRONUNCIATION,
          EnglishLessonIntent.SLOWER,
          EnglishLessonIntent.REPEAT,
        )
    val cleanTarget =
      generatedEnglish
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(Regex("(?<=[.!?])\\s+"))
        .firstOrNull { it.isNotBlank() }
        ?.take(180)
    val target =
      decision.nextState.expectedPhrase
        ?: cleanTarget?.takeIf { shouldCaptureTarget && it.isNotBlank() }
    val stateWithTarget = decision.nextState.copy(expectedPhrase = target)
    return if (
      shouldCaptureTarget &&
        !target.isNullOrBlank() &&
        stateWithTarget.activity == EnglishActivity.LISTENING
    ) {
      stateWithTarget.copy(
        activity = EnglishActivity.REPEAT,
        nextInputLocale = stateWithTarget.dialect,
      )
    } else {
      stateWithTarget
    }
  }
}
