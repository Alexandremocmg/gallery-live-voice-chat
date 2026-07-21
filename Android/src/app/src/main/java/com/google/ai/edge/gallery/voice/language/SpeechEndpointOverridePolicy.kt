package com.google.ai.edge.gallery.voice.language

/**
 * Conversation ASR should use the recognition service's own endpoint defaults.
 * Vendor services can apply custom silence thresholds to initial silence as well as
 * post-speech silence, prematurely closing the microphone before a person starts talking.
 */
object SpeechEndpointOverridePolicy {
  fun shouldOverrideSilenceThresholds(): Boolean = false
}
