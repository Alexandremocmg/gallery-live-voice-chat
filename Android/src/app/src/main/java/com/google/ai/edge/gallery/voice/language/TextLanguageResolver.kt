package com.google.ai.edge.gallery.voice.language

import java.text.Normalizer

data class TextLanguageResolution(
  val locale: SpeechLocale?,
  val confidence: LanguageConfidence,
  val inheritedFromContext: Boolean,
  val portugueseScore: Int,
  val englishScore: Int,
)

class TextLanguageResolver {
  fun resolve(
    text: String,
    previousLocale: SpeechLocale? = null,
    englishDialect: SpeechLocale = SpeechLocale.EN_US,
  ): TextLanguageResolution {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }

    val normalized = normalize(text)
    if (normalized.isBlank()) {
      return inheritedOrUnknown(previousLocale)
    }

    val tokens = TOKEN_PATTERN.findAll(normalized).map { it.value }.toSet()
    var portugueseScore = if (PORTUGUESE_DIACRITIC_PATTERN.containsMatchIn(text.lowercase())) 1 else 0
    var englishScore = 0
    var portugueseEvidence = 0
    var englishEvidence = 0

    tokens.forEach { token ->
      when {
        token in PORTUGUESE_HIGH_SIGNAL -> {
          portugueseScore += 2
          portugueseEvidence++
        }
        token in ENGLISH_HIGH_SIGNAL || ENGLISH_CONTRACTION_PATTERN.matches(token) -> {
          englishScore += 2
          englishEvidence++
        }
        token in PORTUGUESE_FUNCTION_WORDS -> {
          portugueseScore++
          portugueseEvidence++
        }
        token in ENGLISH_FUNCTION_WORDS -> {
          englishScore++
          englishEvidence++
        }
      }
    }

    val scoreDifference = kotlin.math.abs(portugueseScore - englishScore)
    val portugueseWins = portugueseScore > englishScore
    val winningScore = maxOf(portugueseScore, englishScore)
    val winningEvidence = if (portugueseWins) portugueseEvidence else englishEvidence

    if (scoreDifference < MIN_SCORE_MARGIN || winningScore < MIN_LANGUAGE_SCORE ||
      winningEvidence < MIN_EVIDENCE_TOKENS
    ) {
      return inheritedOrUnknown(previousLocale, portugueseScore, englishScore)
    }

    val winningLocale = if (portugueseWins) SpeechLocale.PT_BR else englishDialect
    val confidence =
      if (winningScore >= HIGH_CONFIDENCE_SCORE && scoreDifference >= HIGH_CONFIDENCE_MARGIN) {
        LanguageConfidence.HIGH
      } else {
        LanguageConfidence.MEDIUM
      }

    return TextLanguageResolution(
      locale = winningLocale,
      confidence = confidence,
      inheritedFromContext = false,
      portugueseScore = portugueseScore,
      englishScore = englishScore,
    )
  }

  private fun inheritedOrUnknown(
    previousLocale: SpeechLocale?,
    portugueseScore: Int = 0,
    englishScore: Int = 0,
  ): TextLanguageResolution =
    TextLanguageResolution(
      locale = previousLocale,
      confidence = LanguageConfidence.LOW,
      inheritedFromContext = previousLocale != null,
      portugueseScore = portugueseScore,
      englishScore = englishScore,
    )

  private fun normalize(value: String): String =
    Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace('’', '\'')
}

private const val MIN_SCORE_MARGIN = 2
private const val MIN_LANGUAGE_SCORE = 3
private const val MIN_EVIDENCE_TOKENS = 2
private const val HIGH_CONFIDENCE_SCORE = 7
private const val HIGH_CONFIDENCE_MARGIN = 4

private val TOKEN_PATTERN = Regex("[a-z]+(?:'[a-z]+)?")
private val PORTUGUESE_DIACRITIC_PATTERN = Regex("[áàâãéêíóôõúç]")
private val ENGLISH_CONTRACTION_PATTERN =
  Regex("[a-z]+(?:n't|'re|'ve|'ll|'d|'m|'s)")

private val PORTUGUESE_HIGH_SIGNAL =
  setOf(
    "agora",
    "entender",
    "explica",
    "explicar",
    "explique",
    "fale",
    "funciona",
    "obrigado",
    "porque",
    "portugues",
    "preciso",
    "quero",
    "responda",
    "voce",
  )

private val ENGLISH_HIGH_SIGNAL =
  setOf(
    "answer",
    "english",
    "explain",
    "please",
    "show",
    "speak",
    "thanks",
    "understand",
    "works",
    "why",
  )

private val PORTUGUESE_FUNCTION_WORDS =
  setOf(
    "ainda",
    "assim",
    "como",
    "com",
    "da",
    "das",
    "de",
    "do",
    "dos",
    "esta",
    "estao",
    "eu",
    "isso",
    "isto",
    "mais",
    "meu",
    "minha",
    "onde",
    "para",
    "pode",
    "podemos",
    "qual",
    "quando",
    "que",
    "sobre",
    "tambem",
    "tem",
    "uma",
    "vamos",
  )

private val ENGLISH_FUNCTION_WORDS =
  setOf(
    "about",
    "are",
    "can",
    "could",
    "does",
    "for",
    "from",
    "has",
    "have",
    "how",
    "into",
    "need",
    "that",
    "the",
    "this",
    "those",
    "want",
    "were",
    "what",
    "when",
    "where",
    "which",
    "with",
    "would",
    "you",
    "your",
  )
