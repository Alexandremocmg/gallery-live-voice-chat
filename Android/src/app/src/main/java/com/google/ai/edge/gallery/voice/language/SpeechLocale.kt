package com.google.ai.edge.gallery.voice.language

import java.util.Locale

enum class SpeechLocale(
  val languageTag: String,
  val shortLabel: String,
) {
  PT_BR("pt-BR", "PT"),
  EN_US("en-US", "EN"),
  EN_GB("en-GB", "EN"),
  ;

  val locale: Locale
    get() = Locale.forLanguageTag(languageTag)

  val isEnglish: Boolean
    get() = this == EN_US || this == EN_GB

  companion object {
    fun fromLanguageTag(tag: String?): SpeechLocale? {
      if (tag.isNullOrBlank()) return null
      val normalized = tag.replace('_', '-').lowercase()
      return entries.firstOrNull { it.languageTag.lowercase() == normalized }
        ?: when {
          normalized.startsWith("pt") -> PT_BR
          normalized.startsWith("en-gb") -> EN_GB
          normalized.startsWith("en") -> EN_US
          else -> null
        }
    }
  }
}
