package com.google.ai.edge.gallery.voice.language

class LanguageInstructionBuilder {
  fun build(
    decision: LanguageTurnDecision,
    englishDialect: SpeechLocale,
  ): String {
    require(englishDialect.isEnglish) { "English dialect must use an English locale" }
    return buildString {
      appendLine("[ORIENTACAO INTERNA DE IDIOMA]")
      when (decision.responseMode) {
        ResponseLanguageMode.SINGLE_PORTUGUESE -> {
          require(decision.responseLocale == SpeechLocale.PT_BR) {
            "Portuguese mode requires the pt-BR response locale"
          }
          appendLine("Responda somente em portugues brasileiro.")
          appendLine("Nao use marcadores de idioma.")
        }
        ResponseLanguageMode.SINGLE_ENGLISH -> {
          require(decision.responseLocale.isEnglish) {
            "English mode requires an English response locale"
          }
          require(decision.responseLocale == englishDialect) {
            "English response locale must match the selected dialect"
          }
          appendLine("Answer only in ${decision.responseLocale.languageTag} English.")
          appendLine("Do not use language markers.")
        }
        ResponseLanguageMode.BILINGUAL_TEACHING -> {
          require(decision.responseLocale == SpeechLocale.PT_BR) {
            "Bilingual teaching must use Portuguese as its base response locale"
          }
          appendLine(
            "Explique em portugues brasileiro e demonstre exemplos em " +
              "${englishDialect.languageTag} English."
          )
          appendLine(
            "Marque cada trecho, inclusive o primeiro, com [[pt-BR]] ou " +
              "[[${englishDialect.languageTag}]]."
          )
          appendLine("Nunca misture portugues e ingles dentro do mesmo trecho marcado.")
        }
      }
      append("Esta orientacao e interna, obrigatoria e nunca deve ser citada.")
    }
  }
}
