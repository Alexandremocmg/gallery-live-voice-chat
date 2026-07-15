package com.google.ai.edge.gallery.data

enum class TtsVoiceMode(
  val storageValue: String,
  val speechRate: Float,
) {
  NATURAL(storageValue = "natural", speechRate = 0.93f),
  ECONOMICAL(storageValue = "economical", speechRate = 1.02f),
  ;

  companion object {
    fun fromStorage(value: String): TtsVoiceMode =
      entries.firstOrNull { it.storageValue == value } ?: NATURAL
  }
}
