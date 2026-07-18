package com.google.ai.edge.gallery.voice.language

data class BilingualSegmentationResult(
  val chunks: List<SpeechChunk>,
)

class BilingualSpeechSegmenter(
  expectedResponseLocale: SpeechLocale,
  private val englishDialect: SpeechLocale,
  private val textLanguageResolver: TextLanguageResolver = TextLanguageResolver(),
) {
  private val parser = BilingualResponseParser(expectedResponseLocale)
  private val pendingText = StringBuilder()
  private var pendingLocale = expectedResponseLocale.normalizedTo(englishDialect)

  init {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }
  }

  fun append(fragment: String): BilingualSegmentationResult =
    BilingualSegmentationResult(process(parser.append(fragment).chunks, final = false))

  fun finish(): BilingualSegmentationResult =
    BilingualSegmentationResult(process(parser.finish().chunks, final = true))

  private fun process(
    parsedChunks: List<SpeechChunk>,
    final: Boolean,
  ): List<SpeechChunk> {
    val output = mutableListOf<SpeechChunk>()
    parsedChunks.forEach { chunk ->
      val parsedLocale = chunk.locale.normalizedTo(englishDialect)
      if (pendingText.isNotEmpty() && parsedLocale != pendingLocale) {
        emitValidated(output, pendingText.toString(), pendingLocale)
        pendingText.clear()
      }
      pendingLocale = parsedLocale
      pendingText.append(chunk.text)
      drainStableUnits(output)
    }
    if (final && pendingText.isNotEmpty()) {
      emitValidated(output, pendingText.toString(), pendingLocale)
      pendingText.clear()
    }
    return output
  }

  private fun drainStableUnits(output: MutableList<SpeechChunk>) {
    while (true) {
      val boundary = stableBoundaryEnd(pendingText)
      if (boundary < 0) return
      val text = pendingText.substring(0, boundary)
      pendingText.delete(0, boundary)
      emitValidated(output, text, pendingLocale)
    }
  }

  private fun emitValidated(
    output: MutableList<SpeechChunk>,
    text: String,
    expectedLocale: SpeechLocale,
  ) {
    if (text.isEmpty()) return
    val inlineChunks = splitInlineEnglishPractice(text, expectedLocale)
    if (inlineChunks.size > 1) {
      inlineChunks.forEach { chunk -> addMerged(output, chunk) }
      return
    }

    val resolution =
      textLanguageResolver.resolve(
        text = text,
        previousLocale = expectedLocale,
        englishDialect = englishDialect,
      )
    val validatedLocale =
      if (!resolution.inheritedFromContext &&
        resolution.confidence != LanguageConfidence.LOW &&
        resolution.locale != null
      ) {
        resolution.locale
      } else {
        expectedLocale
      }
    addMerged(output, SpeechChunk(text = text, locale = validatedLocale))
  }

  private fun splitInlineEnglishPractice(
    text: String,
    expectedLocale: SpeechLocale,
  ): List<SpeechChunk> {
    if (expectedLocale.isEnglish || text.isBlank()) return listOf(SpeechChunk(text, expectedLocale))

    val spans = mutableListOf<IntRange>()
    QUOTED_TEXT_PATTERN.findAll(text).forEach { match ->
      val quotedText = match.groupValues[1]
      if (isEnglishPracticeText(quotedText)) {
        spans.add((match.range.first + 1)..(match.range.last - 1))
      }
    }

    ENGLISH_CUE_QUOTE_PATTERN.findAll(text).forEach { match ->
      val start = match.range.last + 1
      val end = englishCueSpanEnd(text, start)
      if (end > start) {
        val candidate = text.substring(start, end)
        if (isEnglishPracticeText(candidate)) {
          spans.add(start until end)
        }
      }
    }

    ENGLISH_CUE_COLON_PATTERN.findAll(text).forEach { match ->
      val start = match.range.last + 1
      val end = englishCueSpanEnd(text, start)
      if (end > start) {
        val candidate = text.substring(start, end)
        if (isEnglishPracticeText(candidate)) {
          spans.add(start until end)
        }
      }
    }

    val mergedSpans = mergeRanges(spans).filter { range -> range.first <= range.last }
    if (mergedSpans.isEmpty()) return listOf(SpeechChunk(text, expectedLocale))

    val chunks = mutableListOf<SpeechChunk>()
    var cursor = 0
    mergedSpans.forEach { range ->
      if (range.first > cursor) {
        chunks.add(SpeechChunk(text.substring(cursor, range.first), expectedLocale))
      }
      chunks.add(SpeechChunk(text.substring(range), englishDialect))
      cursor = range.last + 1
    }
    if (cursor < text.length) {
      chunks.add(SpeechChunk(text.substring(cursor), expectedLocale))
    }
    return chunks.filter { it.text.isNotEmpty() }
  }

  private fun isEnglishPracticeText(candidate: String): Boolean {
    val trimmed = candidate.trim()
    if (trimmed.isBlank()) return false
    val resolution =
      textLanguageResolver.resolve(
        text = trimmed,
        previousLocale = null,
        englishDialect = englishDialect,
      )
    return !resolution.inheritedFromContext &&
      resolution.locale?.isEnglish == true &&
      resolution.confidence != LanguageConfidence.LOW
  }

  private fun englishCueSpanEnd(
    text: String,
    start: Int,
  ): Int {
    var index = start
    while (index < text.length && text[index].isWhitespace()) index++
    val contentStart = index
    while (index < text.length) {
      val char = text[index]
      if (char == '.' || char == '!' || char == '?' || char == '\n') {
        return index + 1
      }
      if (index > contentStart && ENGLISH_CUE_COLON_PATTERN.find(text, index)?.range?.first == index) {
        return index
      }
      index++
    }
    return text.length
  }

  private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
    if (ranges.isEmpty()) return emptyList()
    val sorted = ranges.sortedWith(compareBy<IntRange> { it.first }.thenBy { it.last })
    val merged = mutableListOf(sorted.first())
    sorted.drop(1).forEach { range ->
      val previous = merged.last()
      if (range.first <= previous.last + 1) {
        merged[merged.lastIndex] = previous.first..maxOf(previous.last, range.last)
      } else {
        merged.add(range)
      }
    }
    return merged
  }

  private fun addMerged(
    output: MutableList<SpeechChunk>,
    chunk: SpeechChunk,
  ) {
    val previous = output.lastOrNull()
    if (previous?.locale == chunk.locale && previous.speechRate == chunk.speechRate) {
      output[output.lastIndex] = previous.copy(text = previous.text + chunk.text)
    } else {
      output.add(chunk)
    }
  }

  private fun stableBoundaryEnd(text: CharSequence): Int {
    for (index in text.indices) {
      val char = text[index]
      val phraseLength = index + 1
      if (char == '.' || char == '!' || char == '?' || char == ';' || char == '\n' ||
        (char == ':' && phraseLength >= MIN_COLON_PHRASE_LENGTH)
      ) {
        return index + 1
      }
    }
    return -1
  }
}

private fun SpeechLocale.normalizedTo(englishDialect: SpeechLocale): SpeechLocale =
  if (isEnglish) englishDialect else SpeechLocale.PT_BR

private const val MIN_COLON_PHRASE_LENGTH = 40
private val QUOTED_TEXT_PATTERN = Regex("[\\\"“”']([^\\\"“”']+)[\\\"“”']")
private val ENGLISH_CUE_QUOTE_PATTERN =
  Regex(
    "(?i)\\b(?:repita|diga|fale|pronuncie|em ingles|em inglês|ingles|inglês|como se diz|significa)\\b[^\\\"“”']*[\\\"“”']",
  )
private val ENGLISH_CUE_COLON_PATTERN =
  Regex(
    "(?i)\\b(?:repita|diga|fale|pronuncie|em ingles|em inglês|ingles|inglês|como se diz|significa)\\s*:",
  )
