package com.google.ai.edge.gallery.voice.language

import java.text.Normalizer

class LanguageTurnResolver(
  private val textLanguageResolver: TextLanguageResolver = TextLanguageResolver(),
) {
  fun resolve(
    text: String,
    androidDetectedLocale: SpeechLocale? = null,
    androidConfidence: LanguageConfidence = LanguageConfidence.LOW,
    previousLocale: SpeechLocale? = null,
    englishDialect: SpeechLocale = SpeechLocale.EN_US,
    englishLessonDecision: EnglishLessonDecision? = null,
  ): LanguageTurnDecision {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }

    val textResolution =
      textLanguageResolver.resolve(
        text = text,
        previousLocale = previousLocale,
        englishDialect = englishDialect,
      )
    val inputEvidence =
      selectInputEvidence(
        androidDetectedLocale = androidDetectedLocale,
        androidConfidence = androidConfidence,
        textResolution = textResolution,
        previousLocale = previousLocale,
      )
    val explicitTarget = detectExplicitTarget(text, englishDialect)
    if (explicitTarget != null) {
      return singleLanguageDecision(
        inputLocale = inputEvidence.locale,
        responseLocale = explicitTarget,
        confidence = LanguageConfidence.HIGH,
        source = LanguageDecisionSource.EXPLICIT_COMMAND,
      )
    }

    englishLessonDecision?.let { lesson ->
      if (lesson.intent == EnglishLessonIntent.EXIT) {
        return singleLanguageDecision(
          inputLocale = inputEvidence.locale,
          responseLocale = SpeechLocale.PT_BR,
          confidence = LanguageConfidence.HIGH,
          source = LanguageDecisionSource.ENGLISH_TEACHER,
        )
      }
      if (lesson.nextState.active) {
        val freeConversation = lesson.nextState.activity == EnglishActivity.FREE_CONVERSATION
        return LanguageTurnDecision(
          inputLocale = inputEvidence.locale,
          responseMode =
            if (freeConversation) {
              ResponseLanguageMode.SINGLE_ENGLISH
            } else {
              ResponseLanguageMode.BILINGUAL_TEACHING
          },
          responseLocale = if (freeConversation) lesson.nextState.dialect else SpeechLocale.PT_BR,
          confidence = inputEvidence.confidence,
          source = LanguageDecisionSource.ENGLISH_TEACHER,
        )
      }
    }

    val responseLocale =
      if (inputEvidence.locale.isEnglish) englishDialect else SpeechLocale.PT_BR
    return singleLanguageDecision(
      inputLocale = inputEvidence.locale,
      responseLocale = responseLocale,
      confidence = inputEvidence.confidence,
      source = inputEvidence.source,
    )
  }

  private fun selectInputEvidence(
    androidDetectedLocale: SpeechLocale?,
    androidConfidence: LanguageConfidence,
    textResolution: TextLanguageResolution,
    previousLocale: SpeechLocale?,
  ): InputLanguageEvidence {
    if (androidDetectedLocale != null && androidConfidence != LanguageConfidence.LOW) {
      return InputLanguageEvidence(
        locale = androidDetectedLocale,
        confidence = androidConfidence,
        source = LanguageDecisionSource.ANDROID_DETECTION,
      )
    }
    textResolution.locale?.let { locale ->
      return InputLanguageEvidence(
        locale = locale,
        confidence = textResolution.confidence,
        source =
          if (textResolution.inheritedFromContext) {
            LanguageDecisionSource.CONVERSATION_CONTEXT
          } else {
            LanguageDecisionSource.TEXT_ANALYSIS
          },
      )
    }
    previousLocale?.let { locale ->
      return InputLanguageEvidence(
        locale = locale,
        confidence = LanguageConfidence.LOW,
        source = LanguageDecisionSource.CONVERSATION_CONTEXT,
      )
    }
    return InputLanguageEvidence(
      locale = SpeechLocale.PT_BR,
      confidence = LanguageConfidence.LOW,
      source = LanguageDecisionSource.DEFAULT,
    )
  }

  private fun singleLanguageDecision(
    inputLocale: SpeechLocale,
    responseLocale: SpeechLocale,
    confidence: LanguageConfidence,
    source: LanguageDecisionSource,
  ): LanguageTurnDecision =
    LanguageTurnDecision(
      inputLocale = inputLocale,
      responseMode =
        if (responseLocale.isEnglish) {
          ResponseLanguageMode.SINGLE_ENGLISH
        } else {
          ResponseLanguageMode.SINGLE_PORTUGUESE
        },
      responseLocale = responseLocale,
      confidence = confidence,
      source = source,
    )

  private fun detectExplicitTarget(
    text: String,
    englishDialect: SpeechLocale,
  ): SpeechLocale? {
    val normalized = normalize(text)
    return when {
      PORTUGUESE_COMMAND_PATTERN.containsMatchIn(normalized) -> SpeechLocale.PT_BR
      ENGLISH_COMMAND_PATTERN.containsMatchIn(normalized) -> englishDialect
      else -> null
    }
  }

  private fun normalize(value: String): String =
    Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
}

private data class InputLanguageEvidence(
  val locale: SpeechLocale,
  val confidence: LanguageConfidence,
  val source: LanguageDecisionSource,
)

private val ENGLISH_COMMAND_PATTERN =
  Regex(
    "\\b(?:fale|responda|converse)(?: comigo)?(?: somente)? (?:em )?ingles\\b" +
      "|\\b(?:vamos|quero) (?:conversar|falar)(?: somente)? em ingles\\b" +
      "|\\b(?:speak|answer|talk)(?: to me)? in english\\b" +
      "|\\bswitch to english\\b"
  )

private val PORTUGUESE_COMMAND_PATTERN =
  Regex(
    "\\b(?:fale|responda|converse)(?: comigo)?(?: somente)? (?:em )?portugues\\b" +
      "|\\b(?:volte|voltar)(?: para| ao)? portugues\\b" +
      "|\\b(?:speak|answer|talk)(?: to me)? in portuguese\\b" +
      "|\\b(?:back|switch) to portuguese\\b"
  )
