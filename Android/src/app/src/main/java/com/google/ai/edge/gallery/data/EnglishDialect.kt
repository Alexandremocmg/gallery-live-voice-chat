package com.google.ai.edge.gallery.data

enum class EnglishDialect(val storageValue: String, val languageTag: String) {
  AMERICAN("american", "en-US"),
  BRITISH("british", "en-GB"),
  ;

  companion object {
    fun fromStorage(value: String): EnglishDialect =
      entries.firstOrNull { it.storageValue == value } ?: AMERICAN
  }
}
