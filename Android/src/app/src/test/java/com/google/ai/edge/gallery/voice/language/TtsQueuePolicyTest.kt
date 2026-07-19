package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsQueuePolicyTest {
  @Test
  fun nonBlankSpeechIsDeferredUntilTtsInitializationCompletes() {
    assertTrue(TtsQueuePolicy.shouldDefer("Olá", ttsInitialized = false))
  }

  @Test
  fun initializedTtsDoesNotDeferSpeech() {
    assertFalse(TtsQueuePolicy.shouldDefer("Olá", ttsInitialized = true))
  }

  @Test
  fun blankSpeechIsNeverQueued() {
    assertFalse(TtsQueuePolicy.shouldDefer("   ", ttsInitialized = false))
  }
}
