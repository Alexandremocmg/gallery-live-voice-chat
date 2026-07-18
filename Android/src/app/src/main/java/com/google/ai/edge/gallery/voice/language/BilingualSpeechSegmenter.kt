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
