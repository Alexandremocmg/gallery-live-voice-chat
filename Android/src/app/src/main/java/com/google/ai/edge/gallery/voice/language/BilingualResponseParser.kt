package com.google.ai.edge.gallery.voice.language

class BilingualResponseParser(
  defaultLocale: SpeechLocale,
) {
  private var currentLocale = defaultLocale
  private var pending = ""

  fun append(fragment: String): BilingualParseResult {
    if (fragment.isEmpty()) return BilingualParseResult(emptyList())
    pending += fragment
    return BilingualParseResult(parse(final = false))
  }

  fun finish(): BilingualParseResult {
    val chunks = parse(final = true)
    pending = ""
    return BilingualParseResult(chunks)
  }

  private fun parse(final: Boolean): List<SpeechChunk> {
    val output = mutableListOf<SpeechChunk>()
    while (pending.isNotEmpty()) {
      val markerStart = pending.indexOf(MARKER_PREFIX)
      if (markerStart < 0) {
        val heldSuffix = if (final) 0 else possibleMarkerSuffixLength(pending)
        val emitLength = pending.length - heldSuffix
        if (emitLength > 0) {
          addChunk(output, pending.take(emitLength), currentLocale)
          pending = pending.drop(emitLength)
        }
        break
      }

      if (markerStart > 0) {
        addChunk(output, pending.take(markerStart), currentLocale)
        pending = pending.drop(markerStart)
        continue
      }

      val markerEnd = pending.indexOf(MARKER_SUFFIX, startIndex = MARKER_PREFIX.length)
      if (markerEnd < 0) {
        if (final) {
          recoverTextAfterIncompleteMarker(pending)?.let { recovered ->
            recovered.locale?.let { currentLocale = it }
            addChunk(output, recovered.text, currentLocale)
          }
          pending = ""
        }
        break
      }

      val marker = pending.substring(MARKER_PREFIX.length, markerEnd)
      SpeechLocale.fromLanguageTag(marker)?.let { currentLocale = it }
      pending = pending.drop(markerEnd + MARKER_SUFFIX.length)
    }
    return output
  }

  private fun recoverTextAfterIncompleteMarker(value: String): RecoveredMarkerText? {
    val markerBody = value.removePrefix(MARKER_PREFIX)
    val textStart = markerBody.indexOfFirst(Char::isWhitespace)
    if (textStart < 0) return null
    val text = markerBody.substring(textStart)
    if (text.isBlank()) return null
    return RecoveredMarkerText(
      locale = SpeechLocale.fromLanguageTag(markerBody.take(textStart)),
      text = text,
    )
  }

  private fun possibleMarkerSuffixLength(text: String): Int {
    val max = minOf(text.length, MAX_MARKER_LENGTH)
    for (length in max downTo 1) {
      val suffix = text.takeLast(length)
      if (KNOWN_MARKERS.any { it.startsWith(suffix) } || MARKER_PREFIX.startsWith(suffix)) {
        return length
      }
    }
    return 0
  }

  private fun addChunk(
    output: MutableList<SpeechChunk>,
    text: String,
    locale: SpeechLocale,
  ) {
    if (text.isEmpty()) return
    val last = output.lastOrNull()
    if (last?.locale == locale && last.speechRate == null) {
      output[output.lastIndex] = last.copy(text = last.text + text)
    } else {
      output.add(SpeechChunk(text = text, locale = locale))
    }
  }
}

private data class RecoveredMarkerText(
  val locale: SpeechLocale?,
  val text: String,
)

private const val MARKER_PREFIX = "[["
private const val MARKER_SUFFIX = "]]"
private const val MAX_MARKER_LENGTH = 10
private val KNOWN_MARKERS = SpeechLocale.entries.map { "[[${it.languageTag}]]" }
