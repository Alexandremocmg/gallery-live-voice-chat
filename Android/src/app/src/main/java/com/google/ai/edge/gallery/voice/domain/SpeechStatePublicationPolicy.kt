package com.google.ai.edge.gallery.voice.domain

object SpeechStatePublicationPolicy {
  fun shouldPublishTtsState(
    recognitionSessionActive: Boolean,
    callbackBelongsToActiveUtterance: Boolean,
  ): Boolean =
    callbackBelongsToActiveUtterance && !recognitionSessionActive
}
