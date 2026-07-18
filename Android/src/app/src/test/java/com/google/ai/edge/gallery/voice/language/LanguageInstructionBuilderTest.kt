package com.google.ai.edge.gallery.voice.language

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageInstructionBuilderTest {
  private val builder = LanguageInstructionBuilder()

  @Test
  fun PortugueseModeRequiresOnlyBrazilianPortuguese() {
    val instruction =
      builder.build(
        decision = decision(ResponseLanguageMode.SINGLE_PORTUGUESE, SpeechLocale.PT_BR),
        englishDialect = SpeechLocale.EN_US,
      )

    assertTrue(instruction.contains("somente em portugues brasileiro"))
    assertFalse(instruction.contains("[["))
    assertSingleLanguageBlock(instruction)
  }

  @Test
  fun EnglishModeUsesTheResolvedDialectWithoutMarkers() {
    val instruction =
      builder.build(
        decision = decision(ResponseLanguageMode.SINGLE_ENGLISH, SpeechLocale.EN_GB),
        englishDialect = SpeechLocale.EN_GB,
      )

    assertTrue(instruction.contains("only in en-GB English"))
    assertFalse(instruction.contains("[["))
    assertSingleLanguageBlock(instruction)
  }

  @Test
  fun bilingualTeachingOwnsPortugueseAndEnglishMarkers() {
    val instruction =
      builder.build(
        decision = decision(ResponseLanguageMode.BILINGUAL_TEACHING, SpeechLocale.PT_BR),
        englishDialect = SpeechLocale.EN_US,
      )

    assertTrue(instruction.contains("[[pt-BR]]"))
    assertTrue(instruction.contains("[[en-US]]"))
    assertTrue(instruction.contains("Nunca misture"))
    assertSingleLanguageBlock(instruction)
  }

  private fun decision(
    mode: ResponseLanguageMode,
    responseLocale: SpeechLocale,
  ) =
    LanguageTurnDecision(
      inputLocale = SpeechLocale.PT_BR,
      responseMode = mode,
      responseLocale = responseLocale,
      confidence = LanguageConfidence.HIGH,
      source = LanguageDecisionSource.TEXT_ANALYSIS,
    )

  private fun assertSingleLanguageBlock(instruction: String) {
    val header = "[ORIENTACAO INTERNA DE IDIOMA]"
    assertTrue(instruction.contains(header))
    assertTrue(instruction.indexOf(header) == instruction.lastIndexOf(header))
  }
}
