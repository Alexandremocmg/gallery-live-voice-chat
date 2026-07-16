package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BilingualResponseParserTest {
  @Test
  fun `plain response uses the default locale`() {
    val parser = BilingualResponseParser(SpeechLocale.PT_BR)
    val chunks = parser.append("Resposta simples.").chunks + parser.finish().chunks

    assertEquals(listOf(SpeechChunk("Resposta simples.", SpeechLocale.PT_BR)), chunks)
  }

  @Test
  fun `tagged response alternates languages and hides markers`() {
    val input =
      "[[pt-BR]]A expressao e: [[en-US]]How are you?[[pt-BR]] Agora repita."
    val parser = BilingualResponseParser(SpeechLocale.PT_BR)
    val chunks = parser.append(input).chunks + parser.finish().chunks

    assertEquals(
      listOf(
        SpeechChunk("A expressao e: ", SpeechLocale.PT_BR),
        SpeechChunk("How are you?", SpeechLocale.EN_US),
        SpeechChunk(" Agora repita.", SpeechLocale.PT_BR),
      ),
      chunks,
    )
    assertFalse(chunks.joinToString("") { it.text }.contains("[["))
  }

  @Test
  fun `parser handles every marker split position`() {
    val input = "[[pt-BR]]Ouça: [[en-GB]]Good morning![[pt-BR]] Agora você."
    val expectedText = "Ouça: Good morning! Agora você."

    for (split in 0..input.length) {
      val parser = BilingualResponseParser(SpeechLocale.PT_BR)
      val chunks =
        parser.append(input.take(split)).chunks +
          parser.append(input.drop(split)).chunks +
          parser.finish().chunks
      assertEquals("split=$split", expectedText, chunks.joinToString("") { it.text })
      assertEquals(
        "split=$split english text",
        "Good morning!",
        chunks.filter { it.locale == SpeechLocale.EN_GB }.joinToString("") { it.text },
      )
    }
  }

  @Test
  fun `parser handles input one character at a time`() {
    val input = "[[pt-BR]]Diga [[en-US]]I can't go.[[pt-BR]] Tente agora."
    val parser = BilingualResponseParser(SpeechLocale.PT_BR)
    val chunks = buildList {
      input.forEach { addAll(parser.append(it.toString()).chunks) }
      addAll(parser.finish().chunks)
    }

    assertEquals("Diga I can't go. Tente agora.", chunks.joinToString("") { it.text })
    assertEquals(
      "I can't go.",
      chunks.filter { it.locale == SpeechLocale.EN_US }.joinToString("") { it.text },
    )
  }

  @Test
  fun `unknown and incomplete markers are never spoken`() {
    val parser = BilingualResponseParser(SpeechLocale.PT_BR)
    val chunks = parser.append("Oi [[fr-FR]]bonjour [[en-").chunks + parser.finish().chunks

    assertEquals("Oi bonjour ", chunks.joinToString("") { it.text })
    assertFalse(chunks.joinToString("") { it.text }.contains("[["))
  }

  @Test
  fun `text after an incomplete locale marker is preserved`() {
    val parser = BilingualResponseParser(SpeechLocale.PT_BR)
    val chunks = parser.append("Diga [[en-US Good morning").chunks + parser.finish().chunks

    assertEquals("Diga  Good morning", chunks.joinToString("") { it.text })
    assertEquals(
      " Good morning",
      chunks.filter { it.locale == SpeechLocale.EN_US }.joinToString("") { it.text },
    )
  }
}
