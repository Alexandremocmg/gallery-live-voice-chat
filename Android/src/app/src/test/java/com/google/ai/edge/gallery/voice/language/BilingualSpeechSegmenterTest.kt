package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilingualSpeechSegmenterTest {
  @Test
  fun markerFreeEnglishSentenceIsRetaggedBeforeSpeech() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val chunks = segmenter.append("Can you explain how this works?").chunks

    assertEquals(listOf(SpeechLocale.EN_US), chunks.map(SpeechChunk::locale))
  }

  @Test
  fun markerFreePortugueseSentenceIsRetaggedBeforeSpeech() {
    val segmenter = segmenter(expected = SpeechLocale.EN_US)

    val chunks = segmenter.append("Quero entender como isso funciona.").chunks

    assertEquals(listOf(SpeechLocale.PT_BR), chunks.map(SpeechChunk::locale))
  }

  @Test
  fun isolatedTechnicalEnglishDoesNotSwitchPortugueseVoice() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val chunks = segmenter.append("O endpoint funciona com JSON.").chunks

    assertEquals(listOf(SpeechLocale.PT_BR), chunks.map(SpeechChunk::locale))
  }

  @Test
  fun inlineEnglishPracticePhraseUsesEnglishVoiceInsidePortugueseResponse() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val chunks = segmenter.append("Agora repita: Good morning. Depois diga: Thank you.").chunks +
      segmenter.finish().chunks

    assertEquals(
      listOf(SpeechLocale.PT_BR, SpeechLocale.EN_US, SpeechLocale.PT_BR, SpeechLocale.EN_US),
      chunks.map(SpeechChunk::locale),
    )
    assertEquals(
      "Agora repita: Good morning. Depois diga: Thank you.",
      chunks.joinToString("") { it.text },
    )
  }

  @Test
  fun quotedInlineEnglishUsesSelectedDialectInsidePortugueseResponse() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR, dialect = SpeechLocale.EN_GB)

    val chunks = segmenter.append("Em inglês, fale \"How are you?\" e espere a resposta.").chunks +
      segmenter.finish().chunks

    assertEquals(
      listOf(SpeechLocale.PT_BR, SpeechLocale.EN_GB, SpeechLocale.PT_BR),
      chunks.map(SpeechChunk::locale),
    )
    assertEquals(
      "Em inglês, fale \"How are you?\" e espere a resposta.",
      chunks.joinToString("") { it.text },
    )
  }

  @Test
  fun taggedTeachingResponseProducesValidatedPortugueseAndEnglishChunks() {
    val input =
      "[[pt-BR]]Quero explicar como isso funciona. " +
        "[[en-US]]Can you explain how this works?" +
        "[[pt-BR]] Agora repita."
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val chunks = segmenter.append(input).chunks + segmenter.finish().chunks

    assertEquals(
      listOf(SpeechLocale.PT_BR, SpeechLocale.EN_US, SpeechLocale.PT_BR),
      chunks.map(SpeechChunk::locale),
    )
    assertEquals(
      "Quero explicar como isso funciona. Can you explain how this works? Agora repita.",
      chunks.joinToString("") { it.text },
    )
  }

  @Test
  fun everyStreamingSplitPreservesVisibleTextAndSelectedDialect() {
    val input = "[[pt-BR]]Vamos praticar. [[en-US]]Can you explain how this works?"
    val expectedText = "Vamos praticar. Can you explain how this works?"

    for (split in 0..input.length) {
      val segmenter = segmenter(expected = SpeechLocale.PT_BR, dialect = SpeechLocale.EN_GB)
      val chunks =
        segmenter.append(input.take(split)).chunks +
          segmenter.append(input.drop(split)).chunks +
          segmenter.finish().chunks

      assertEquals("split=$split", expectedText, chunks.joinToString("") { it.text })
      assertTrue("split=$split", chunks.any { it.locale == SpeechLocale.EN_GB })
      assertFalse("split=$split", chunks.joinToString("") { it.text }.contains("[["))
    }
  }

  @Test
  fun adjacentSentencesWithSameLanguageAreMerged() {
    val segmenter = segmenter(expected = SpeechLocale.EN_US)

    val chunks =
      segmenter.append("Can you explain how this works? This works because you can test it.").chunks

    assertEquals(1, chunks.size)
    assertEquals(SpeechLocale.EN_US, chunks.single().locale)
  }

  @Test
  fun completeSentenceIsAvailableBeforeGenerationFinishes() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val first = segmenter.append("Quero entender como isso funciona.").chunks
    val second = segmenter.append(" Agora explique mais").chunks

    assertEquals(1, first.size)
    assertTrue(second.isEmpty())
  }

  @Test
  fun textRecoveredFromIncompleteMarkerIsPreserved() {
    val segmenter = segmenter(expected = SpeechLocale.PT_BR)

    val chunks = segmenter.append("Diga [[en-US Good morning").chunks + segmenter.finish().chunks

    assertEquals("Diga  Good morning", chunks.joinToString("") { it.text })
  }

  private fun segmenter(
    expected: SpeechLocale,
    dialect: SpeechLocale = SpeechLocale.EN_US,
  ) =
    BilingualSpeechSegmenter(
      expectedResponseLocale = expected,
      englishDialect = dialect,
    )
}
