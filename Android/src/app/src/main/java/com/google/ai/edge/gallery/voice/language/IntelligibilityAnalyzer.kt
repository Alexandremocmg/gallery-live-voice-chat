package com.google.ai.edge.gallery.voice.language

import java.text.Normalizer

data class IntelligibilityResult(
  val expected: String,
  val bestTranscript: String,
  val missingWords: List<String>,
  val extraWords: List<String>,
  val isLikelyUnderstood: Boolean,
) {
  fun promptSummary(): String = buildString {
    append(if (isLikelyUnderstood) "a frase parece compreensivel" else "a transcricao divergiu do alvo")
    if (missingWords.isNotEmpty()) append("; palavras nao reconhecidas: ${missingWords.joinToString(", ")}")
    if (extraWords.isNotEmpty()) append("; palavras adicionais: ${extraWords.joinToString(", ")}")
    append("; melhor transcricao: '$bestTranscript'")
  }
}

class IntelligibilityAnalyzer {
  fun analyze(expected: String, candidates: List<String>): IntelligibilityResult? {
    val expectedWords = words(expected)
    if (expectedWords.isEmpty() || candidates.isEmpty()) return null
    val ranked = candidates.filter { it.isNotBlank() }.map { it to editDistance(expectedWords, words(it)) }
    val best = ranked.minByOrNull { it.second } ?: return null
    val spokenWords = words(best.first)
    val missing = expectedWords.toMutableList().apply { spokenWords.forEach(::remove) }
    val extra = spokenWords.toMutableList().apply { expectedWords.forEach(::remove) }
    val threshold = maxOf(1, (expectedWords.size * 0.34f).toInt())
    return IntelligibilityResult(
      expected = expected,
      bestTranscript = best.first,
      missingWords = missing.distinct(),
      extraWords = extra.distinct(),
      isLikelyUnderstood = best.second <= threshold,
    )
  }

  private fun words(text: String): List<String> =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("[^a-z0-9']+"), " ")
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }

  private fun editDistance(left: List<String>, right: List<String>): Int {
    var previous = IntArray(right.size + 1) { it }
    left.forEachIndexed { leftIndex, leftWord ->
      val current = IntArray(right.size + 1)
      current[0] = leftIndex + 1
      right.forEachIndexed { rightIndex, rightWord ->
        current[rightIndex + 1] = minOf(
          current[rightIndex] + 1,
          previous[rightIndex + 1] + 1,
          previous[rightIndex] + if (leftWord == rightWord) 0 else 1,
        )
      }
      previous = current
    }
    return previous[right.size]
  }
}
