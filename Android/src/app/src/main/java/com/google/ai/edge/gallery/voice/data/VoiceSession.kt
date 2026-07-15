package com.google.ai.edge.gallery.voice.data

data class VoiceSessionSummary(
  val id: String,
  val title: String,
  val updatedAtMs: Long,
  val messageCount: Int,
  val pdfName: String,
)
