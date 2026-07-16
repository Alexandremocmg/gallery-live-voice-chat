package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechBackendPolicyTest {
  @Test
  fun `Android 12 prefers explicit on-device recognition`() {
    assertEquals(
      RecognitionBackend.ANDROID_ON_DEVICE,
      SpeechBackendPolicy.select(31, true, true),
    )
  }

  @Test
  fun `older Android prefers installed local pack before system service`() {
    assertEquals(
      RecognitionBackend.LOCAL_LANGUAGE_PACK,
      SpeechBackendPolicy.select(30, false, true, localLanguagePackAvailable = true),
    )
  }

  @Test
  fun `Gemma audio is a local fallback before generic system recognition`() {
    assertEquals(
      RecognitionBackend.GEMMA_AUDIO,
      SpeechBackendPolicy.select(30, false, true, gemmaAudioAvailable = true),
    )
  }

  @Test
  fun `generic system recognizer is identified separately`() {
    assertEquals(
      RecognitionBackend.ANDROID_SYSTEM,
      SpeechBackendPolicy.select(30, false, true),
    )
  }
}
