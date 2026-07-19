package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishPracticeInputPolicyTest {
  @Test
  fun `repeat uses speech recognizer when English recognition is available`() {
    val capability =
      SpeechCapability(
        locale = SpeechLocale.EN_US,
        backend = RecognitionBackend.ANDROID_ON_DEVICE,
        languagePackStatus = LanguagePackStatus.INSTALLED,
      )

    assertFalse(
      EnglishPracticeInputPolicy.shouldUseGemmaAudio(
        activity = EnglishActivity.REPEAT,
        modelSupportsAudio = true,
        recognitionCapability = capability,
      )
    )
  }

  @Test
  fun `repeat falls back to Gemma audio only when recognition is unavailable`() {
    val capability =
      SpeechCapability(
        locale = SpeechLocale.EN_US,
        backend = RecognitionBackend.UNAVAILABLE,
        languagePackStatus = LanguagePackStatus.UNSUPPORTED,
      )

    assertTrue(
      EnglishPracticeInputPolicy.shouldUseGemmaAudio(
        activity = EnglishActivity.REPEAT,
        modelSupportsAudio = true,
        recognitionCapability = capability,
      )
    )
  }

  @Test
  fun `non repeat activities never use pronunciation audio recorder`() {
    assertFalse(
      EnglishPracticeInputPolicy.shouldUseGemmaAudio(
        activity = EnglishActivity.LISTENING,
        modelSupportsAudio = true,
        recognitionCapability = null,
      )
    )
  }

  @Test
  fun `unknown recognition capability prefers speech recognizer instead of model audio`() {
    assertFalse(
      EnglishPracticeInputPolicy.shouldUseGemmaAudio(
        activity = EnglishActivity.REPEAT,
        modelSupportsAudio = true,
        recognitionCapability = null,
      )
    )
  }

  @Test
  fun `audio unsupported model never uses Gemma audio`() {
    assertFalse(
      EnglishPracticeInputPolicy.shouldUseGemmaAudio(
        activity = EnglishActivity.REPEAT,
        modelSupportsAudio = false,
        recognitionCapability = null,
      )
    )
  }
}
