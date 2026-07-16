package com.google.ai.edge.gallery.voice.intelligence

import kotlin.math.ceil

fun interface TokenEstimator {
  fun estimate(text: String): Int
}

class HeuristicTokenEstimator : TokenEstimator {
  override fun estimate(text: String): Int {
    if (text.isBlank()) return 0
    val words = text.trim().split(Regex("\\s+")).size
    val punctuation = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
    val characterEstimate = ceil(text.length / 3.6).toInt()
    return maxOf(characterEstimate, words + punctuation / 3)
  }
}

enum class ContextPressure {
  NORMAL,
  TRIM_METADATA,
  COMPACT_HISTORY,
  ROLLING_SUMMARY,
  RESET_REQUIRED,
}

data class ContextBudgetSnapshot(
  val contextWindow: Int,
  val reservedOutput: Int,
  val fixedTokens: Int,
  val dynamicTokens: Int,
  val usedTokens: Int,
  val usageRatio: Float,
  val availableDynamicTokens: Int,
  val pressure: ContextPressure,
)

class ContextBudgetManager(
  private val estimator: TokenEstimator = HeuristicTokenEstimator(),
) {
  fun snapshot(
    contextWindow: Int,
    reservedOutput: Int,
    systemPrompt: String,
    toolCatalog: String,
    dynamicParts: List<String>,
  ): ContextBudgetSnapshot {
    val safeWindow = contextWindow.coerceAtLeast(MIN_CONTEXT_WINDOW)
    val output = reservedOutput.coerceIn(MIN_RESERVED_OUTPUT, safeWindow / 2)
    val fixed = estimator.estimate(systemPrompt) + estimator.estimate(toolCatalog)
    val dynamic = dynamicParts.sumOf(estimator::estimate)
    val used = fixed + dynamic + output
    val ratio = used.toFloat() / safeWindow
    val pressure = when {
      ratio < 0.65f -> ContextPressure.NORMAL
      ratio < 0.75f -> ContextPressure.TRIM_METADATA
      ratio < 0.85f -> ContextPressure.COMPACT_HISTORY
      ratio < 0.92f -> ContextPressure.ROLLING_SUMMARY
      else -> ContextPressure.RESET_REQUIRED
    }
    return ContextBudgetSnapshot(
      contextWindow = safeWindow,
      reservedOutput = output,
      fixedTokens = fixed,
      dynamicTokens = dynamic,
      usedTokens = used,
      usageRatio = ratio,
      availableDynamicTokens = (safeWindow - fixed - output).coerceAtLeast(0),
      pressure = pressure,
    )
  }

  fun fit(partsByPriority: List<String>, availableTokens: Int): List<String> {
    var remaining = availableTokens.coerceAtLeast(0)
    return buildList {
      partsByPriority.forEach { part ->
        if (part.isBlank() || remaining <= 0) return@forEach
        val estimate = estimator.estimate(part)
        if (estimate <= remaining) {
          add(part)
          remaining -= estimate
        } else {
          val proportion = remaining.toFloat() / estimate.coerceAtLeast(1)
          val chars = (part.length * proportion).toInt().coerceAtLeast(0)
          if (chars >= MIN_PART_CHARS) add(part.take(chars).trimEnd() + "\n[contexto compactado]")
          remaining = 0
        }
      }
    }
  }

  companion object {
    const val DEFAULT_CONTEXT_WINDOW = 4_096
    private const val MIN_CONTEXT_WINDOW = 2_048
    private const val MIN_RESERVED_OUTPUT = 256
    private const val MIN_PART_CHARS = 80
  }
}
