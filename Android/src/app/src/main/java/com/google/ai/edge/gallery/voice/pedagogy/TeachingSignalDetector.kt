package com.google.ai.edge.gallery.voice.pedagogy

import java.text.Normalizer

data class TeachingDetection(
  val normalizedText: String,
  val strongLearningIntent: Boolean,
  val exitRequested: Boolean,
  val signal: UnderstandingSignal,
  val levelEstimate: LearnerLevelEstimate,
  val topicHint: String?,
)

class TeachingSignalDetector {
  fun detect(text: String, state: TeachingState): TeachingDetection {
    val normalized = normalize(text)
    val strongLearningIntent = STRONG_LEARNING_PATTERNS.any { it.containsMatchIn(normalized) }
    val exitRequested = EXIT_PATTERNS.any { it.containsMatchIn(normalized) }
    val signal = detectSignal(normalized, state, exitRequested)
    val levelEstimate = estimateLevel(normalized)
    val topicHint =
      when {
        strongLearningIntent -> extractTopicHint(text)
        signal == UnderstandingSignal.TOPIC_CHANGE -> text.toTeachingHint()
        else -> null
      }

    return TeachingDetection(
      normalizedText = normalized,
      strongLearningIntent = strongLearningIntent,
      exitRequested = exitRequested,
      signal = signal,
      levelEstimate = levelEstimate,
      topicHint = topicHint,
    )
  }

  private fun detectSignal(
    normalized: String,
    state: TeachingState,
    exitRequested: Boolean,
  ): UnderstandingSignal {
    if (exitRequested) return UnderstandingSignal.TOPIC_CHANGE
    if (CONFUSED_PATTERNS.any { it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.CONFUSED
    }
    if (PARTIAL_PATTERNS.any { it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.PARTIAL
    }
    if (UNDERSTOOD_PATTERNS.any { it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.UNDERSTOOD
    }
    if (EXAMPLE_PATTERNS.any { it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.WANTS_EXAMPLE
    }
    if (DEPTH_PATTERNS.any { it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.WANTS_DEPTH
    }
    if (NEXT_PATTERNS.any { it.matches(normalized) || it.containsMatchIn(normalized) }) {
      return UnderstandingSignal.WANTS_NEXT
    }
    if (
      state.mode == TeachingMode.ACTIVE &&
        TOPIC_CHANGE_PATTERNS.any { it.containsMatchIn(normalized) }
    ) {
      return UnderstandingSignal.TOPIC_CHANGE
    }
    if (looksLikeLearnerAttempt(normalized, state)) {
      return UnderstandingSignal.LEARNER_ATTEMPT
    }
    return UnderstandingSignal.NONE
  }

  private fun looksLikeLearnerAttempt(normalized: String, state: TeachingState): Boolean {
    if (!state.awaitingUnderstanding || state.mode != TeachingMode.ACTIVE) return false
    if (normalized.endsWith("?") || QUESTION_START.matches(normalized)) return false
    val words = normalized.split(' ').filter { it.length > 1 }
    return words.size >= 3
  }

  private fun estimateLevel(normalized: String): LearnerLevelEstimate {
    return when {
      BEGINNER_PATTERNS.any { it.containsMatchIn(normalized) } ->
        LearnerLevelEstimate(LearnerLevel.BEGINNER, 0.9f)
      ADVANCED_PATTERNS.any { it.containsMatchIn(normalized) } ->
        LearnerLevelEstimate(LearnerLevel.ADVANCED, 0.82f)
      INTERMEDIATE_PATTERNS.any { it.containsMatchIn(normalized) } ->
        LearnerLevelEstimate(LearnerLevel.INTERMEDIATE, 0.8f)
      else -> LearnerLevelEstimate(LearnerLevel.UNKNOWN, 0f)
    }
  }

  private fun extractTopicHint(text: String): String {
    var topic = normalize(text)
    for (pattern in TOPIC_PREFIX_PATTERNS) {
      topic = topic.replace(pattern, " ")
    }
    topic = topic.replace(
      Regex(
        "\\b(do zero|desde o inicio|para iniciantes?|de forma profunda|profundo|profunda|profundamente|em detalhes|passo a passo|tutorial completo)\\b"
      ),
      " ",
    )
      .replace(Regex("\\s+"), " ")
      .trim()
    return (topic.ifBlank { text }).toTeachingHint()
  }

  private fun normalize(text: String): String {
    return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
  }
}

private val STRONG_LEARNING_PATTERNS =
  listOf(
    Regex("\\b(me ensine|me ensina|quero aprender|quero entender|quero estudar)\\b"),
    Regex("\\b(explique|explica|ensine|ensina)\\b.*\\b(do zero|desde o inicio|para iniciante)\\b"),
    Regex("\\b(explique|explica|me explique|me explica)\\b.*\\b(profundo|profunda|profundamente|em detalhes|passo a passo)\\b"),
    Regex("\\b(passo a passo|tutorial completo)\\b"),
    Regex("\\b(aula sobre|estou estudando|me ajude a aprender)\\b"),
  )

private val TOPIC_PREFIX_PATTERNS =
  listOf(
    Regex("\\b(me ensine|me ensina|quero aprender|quero entender|quero estudar)\\b"),
    Regex("\\b(me explique|me explica|explique|explica|ensine|ensina|mostre|mostra)\\b"),
    Regex("\\b(aula sobre|estou estudando|me ajude a aprender)\\b"),
  )

private val EXIT_PATTERNS =
  listOf(
    Regex("\\b(pare de ensinar|para de ensinar|encerre a aula|chega de aula|nao quero continuar)\\b"),
    Regex("\\b(volte ao normal|responda normalmente)\\b"),
  )

private val UNDERSTOOD_PATTERNS =
  listOf(
    Regex("\\b(entendi|compreendi|agora ficou claro|fez sentido|faz sentido|saquei)\\b"),
    Regex("^\\s*(sim|certo|beleza|ok)\\s*[.!]*\\s*$"),
  )

private val CONFUSED_PATTERNS =
  listOf(
    Regex("\\b(nao entendi|nao compreendi|nao ficou claro|estou confuso|fiquei confuso)\\b"),
    Regex("^\\s*(como assim|nao entendi)\\s*[?!]*\\s*$"),
    Regex("^\\s*(nao|ainda nao)\\s*[.!]*\\s*$"),
    Regex("\\b(muito dificil|muito complicado|complicado demais)\\b"),
    Regex("\\b(explica|explique)\\b.*\\b(outro jeito|outra forma|mais simples)\\b"),
  )

private val PARTIAL_PATTERNS =
  listOf(
    Regex("\\b(mais ou menos|acho que entendi|entendi uma parte|quase entendi)\\b"),
  )

private val EXAMPLE_PATTERNS =
  listOf(
    Regex("\\b(me da um exemplo|me de um exemplo|da um exemplo|tem um exemplo|por exemplo|exemplo pratico|outro exemplo)\\b"),
  )

private val DEPTH_PATTERNS =
  listOf(
    Regex("\\b(aprofunda|aprofundar|mais detalhes|por dentro|internamente|tecnicamente)\\b"),
  )

private val NEXT_PATTERNS =
  listOf(
    Regex("^\\s*(mais|continua|continue|proximo|pode avancar|vamos avancar|e depois|depois)\\s*[.!?]*\\s*$"),
    Regex("\\b(qual e o proximo|pode continuar|vamos para o proximo)\\b"),
  )

private val TOPIC_CHANGE_PATTERNS =
  listOf(
    Regex("\\b(mudando de assunto|sobre outra coisa|agora fale (sobre|do|da|de)|agora quero (saber|falar) (sobre|do|da|de))\\b"),
  )

private val BEGINNER_PATTERNS =
  listOf(
    Regex("\\b(do zero|desde o inicio|sou iniciante|para iniciante|nao sei nada|nunca estudei)\\b"),
  )

private val INTERMEDIATE_PATTERNS =
  listOf(
    Regex("\\b(nivel intermediario|ja sei o basico|tenho conhecimento basico|conheco o basico)\\b"),
  )

private val ADVANCED_PATTERNS =
  listOf(
    Regex("\\b(nivel avancado|sou avancado|detalhes tecnicos|arquitetura interna|trade-offs?|complexidade)\\b"),
  )

private val QUESTION_START =
  Regex("^(como|por que|porque|qual|quais|quando|onde|quem|o que)\\b.*")
