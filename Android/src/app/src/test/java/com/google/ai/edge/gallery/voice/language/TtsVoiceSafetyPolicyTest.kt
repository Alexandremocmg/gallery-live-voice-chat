package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsVoiceSafetyPolicyTest {
  @Test
  fun localVoiceInTheRequestedLanguageIsAccepted() {
    assertTrue(
      TtsVoiceSafetyPolicy.isCompatibleLocalVoice(
        requestedLocale = SpeechLocale.PT_BR,
        candidateLanguageTag = "pt-BR",
        networkConnectionRequired = false,
      )
    )
  }

  @Test
  fun networkVoiceIsNeverAccepted() {
    assertFalse(
      TtsVoiceSafetyPolicy.isCompatibleLocalVoice(
        requestedLocale = SpeechLocale.EN_US,
        candidateLanguageTag = "en-US",
        networkConnectionRequired = true,
      )
    )
  }

  @Test
  fun PortugueseAndEnglishNeverFallbackToEachOther() {
    assertFalse(
      TtsVoiceSafetyPolicy.isCompatibleLocalVoice(SpeechLocale.PT_BR, "en-US", false)
    )
    assertFalse(
      TtsVoiceSafetyPolicy.isCompatibleLocalVoice(SpeechLocale.EN_US, "pt-BR", false)
    )
  }

  @Test
  fun EnglishDialectsRemainCompatibleWithinTheSameLanguage() {
    assertTrue(
      TtsVoiceSafetyPolicy.isCompatibleLocalVoice(SpeechLocale.EN_GB, "en-US", false)
    )
  }

  @Test
  fun speechQueuePreservesPortugueseEnglishPortugueseOrder() {
    val queue = SpeechChunkQueue()
    val expected =
      listOf(
        SpeechChunk("Primeiro.", SpeechLocale.PT_BR),
        SpeechChunk("Then.", SpeechLocale.EN_US),
        SpeechChunk("Por fim.", SpeechLocale.PT_BR),
      )

    expected.forEach(queue::addLast)

    assertEquals(expected, listOfNotNull(queue.removeFirstOrNull(), queue.removeFirstOrNull(), queue.removeFirstOrNull()))
  }
}
