package com.google.ai.edge.gallery.voice.data

enum class MemoryCategory(val label: String) {
  USER_FACT("Sobre voce"),
  PREFERENCE("Preferencias"),
  PROJECT("Projetos"),
  STUDY("Estudos"),
  PERSON("Pessoas");
}

enum class MemoryStatus {
  CANDIDATE,
  CONFIRMED,
  REJECTED,
}

data class MemoryItem(
  val id: String,
  val category: MemoryCategory,
  val key: String,
  val value: String,
  val status: MemoryStatus,
  val confidence: Float,
  val sourceSessionId: String,
  val evidence: String,
  val sensitive: Boolean,
)

data class MemoryCandidate(
  val category: MemoryCategory,
  val key: String,
  val value: String,
  val confidence: Float,
  val sensitive: Boolean,
)
