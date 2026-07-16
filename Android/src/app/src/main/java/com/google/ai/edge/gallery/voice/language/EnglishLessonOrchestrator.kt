package com.google.ai.edge.gallery.voice.language

import java.text.Normalizer

data class EnglishLessonDecision(
  val nextState: EnglishLessonState,
  val intent: EnglishLessonIntent,
  val baseResponseLocale: SpeechLocale,
)

enum class EnglishLessonIntent {
  NONE,
  START,
  FREE_CONVERSATION,
  PRONUNCIATION,
  SLOWER,
  REPEAT,
  LEARNER_ATTEMPT,
  EXIT,
}

class EnglishLessonOrchestrator {
  fun decide(state: EnglishLessonState, userText: String): EnglishLessonDecision {
    val normalized = normalize(userText)
    val dialect = detectDialect(normalized) ?: state.dialect

    if (EXIT_PATTERN.containsMatchIn(normalized)) {
      return EnglishLessonDecision(EnglishLessonState(dialect = dialect), EnglishLessonIntent.EXIT, SpeechLocale.PT_BR)
    }

    if (state.active && state.activity == EnglishActivity.REPEAT && state.nextInputLocale.isEnglish) {
      return EnglishLessonDecision(
        state.copy(
          activity = EnglishActivity.PRONUNCIATION_FEEDBACK,
          nextInputLocale = SpeechLocale.PT_BR,
          attemptCount = state.attemptCount + 1,
        ),
        EnglishLessonIntent.LEARNER_ATTEMPT,
        SpeechLocale.PT_BR,
      )
    }

    if (FREE_CONVERSATION_PATTERN.containsMatchIn(normalized)) {
      return EnglishLessonDecision(
        state.copy(
          active = true,
          dialect = dialect,
          activity = EnglishActivity.FREE_CONVERSATION,
          nextInputLocale = dialect,
          demonstrationRate = NORMAL_ENGLISH_RATE,
        ),
        EnglishLessonIntent.FREE_CONVERSATION,
        dialect,
      )
    }

    if (SLOWER_PATTERN.containsMatchIn(normalized) && state.active) {
      return EnglishLessonDecision(
        state.copy(
          dialect = dialect,
          activity = EnglishActivity.LISTENING,
          nextInputLocale = SpeechLocale.PT_BR,
          demonstrationRate = SLOW_ENGLISH_RATE,
        ),
        EnglishLessonIntent.SLOWER,
        SpeechLocale.PT_BR,
      )
    }

    if (REPEAT_PATTERN.containsMatchIn(normalized) && state.active) {
      return EnglishLessonDecision(
        state.copy(
          dialect = dialect,
          activity = EnglishActivity.REPEAT,
          nextInputLocale = dialect,
        ),
        EnglishLessonIntent.REPEAT,
        SpeechLocale.PT_BR,
      )
    }

    val pronunciationMatch = PRONUNCIATION_PATTERN.find(normalized)
    if (pronunciationMatch != null) {
      val phrase = pronunciationMatch.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
      return EnglishLessonDecision(
        state.copy(
          active = true,
          dialect = dialect,
          activity = EnglishActivity.LISTENING,
          expectedPhrase = phrase ?: state.expectedPhrase,
          nextInputLocale = SpeechLocale.PT_BR,
          demonstrationRate = NORMAL_ENGLISH_RATE,
        ),
        EnglishLessonIntent.PRONUNCIATION,
        SpeechLocale.PT_BR,
      )
    }

    if (START_PATTERN.containsMatchIn(normalized) || (state.active && ENGLISH_TOPIC_PATTERN.containsMatchIn(normalized))) {
      return EnglishLessonDecision(
        state.copy(
          active = true,
          dialect = dialect,
          activity = EnglishActivity.EXPLANATION,
          nextInputLocale = SpeechLocale.PT_BR,
          demonstrationRate = NORMAL_ENGLISH_RATE,
        ),
        EnglishLessonIntent.START,
        SpeechLocale.PT_BR,
      )
    }

    if (state.active) {
      val base = if (state.activity == EnglishActivity.FREE_CONVERSATION) dialect else SpeechLocale.PT_BR
      return EnglishLessonDecision(state.copy(dialect = dialect), EnglishLessonIntent.NONE, base)
    }
    return EnglishLessonDecision(state.copy(dialect = dialect), EnglishLessonIntent.NONE, SpeechLocale.PT_BR)
  }

  private fun detectDialect(text: String): SpeechLocale? = when {
    Regex("\\b(britanico|britanica|reino unido|en-gb)\\b").containsMatchIn(text) -> SpeechLocale.EN_GB
    Regex("\\b(americano|americana|estados unidos|en-us)\\b").containsMatchIn(text) -> SpeechLocale.EN_US
    else -> null
  }

  private fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
}

private val START_PATTERN =
  Regex("\\b(?:quero|vamos|preciso|gostaria de) (?:estudar|aprender|praticar|treinar|melhorar)(?: o)? ingles\\b|\\bme ensine ingles\\b|\\bteach me english\\b|\\bi want to learn english\\b")
private val ENGLISH_TOPIC_PATTERN = Regex("\\b(ingles|english|vocabulario|grammar|gramatica)\\b")
private val FREE_CONVERSATION_PATTERN =
  Regex("\\b(?:vamos|quero) (?:conversar|falar|praticar conversa)(?: so)? em ingles\\b|\\bconversacao em ingles\\b|\\blet'?s (?:speak|talk) (?:in )?english\\b")
private val SLOWER_PATTERN =
  Regex("\\b(fale|fala|repita|diga).{0,18}(devagar|lentamente|mais lento)\\b|\\b(slower|more slowly|speak slowly)\\b")
private val REPEAT_PATTERN =
  Regex("\\b(quero|vou|posso) repetir\\b|\\bme deixe repetir\\b|\\bcorrija (?:a )?minha pronuncia\\b|\\b(i want to repeat|let me repeat|repeat that)\\b")
private val PRONUNCIATION_PATTERN =
  Regex("\\b(?:como (?:se )?pronuncia|qual (?:e )?a pronuncia de|pronuncie|how do you pronounce)\\s+(.+?)[?.!]*$")
private val EXIT_PATTERN =
  Regex("\\b(volte|voltar|vamos voltar|responda|fale) (?:para o |ao )?portugues\\b|\\bpare (?:a aula|o ingles)\\b|\\b(back to portuguese|speak portuguese)\\b")
