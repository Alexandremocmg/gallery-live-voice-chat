package com.google.ai.edge.gallery.voice.language

object TtsQueuePolicy {
  fun shouldDefer(text: String, ttsInitialized: Boolean): Boolean =
    text.isNotBlank() && !ttsInitialized
}
