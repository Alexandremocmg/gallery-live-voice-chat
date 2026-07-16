package com.google.ai.edge.gallery.voice.language

class EnglishTeachingPromptBuilder {
  fun build(
    decision: EnglishLessonDecision,
    intelligibility: IntelligibilityResult? = null,
    hasPronunciationAudio: Boolean = false,
  ): String {
    if (!decision.nextState.active && decision.intent == EnglishLessonIntent.NONE) return ""
    if (decision.intent == EnglishLessonIntent.EXIT) {
      return "[ORIENTACAO DE IDIOMA INTERNA] Confirme brevemente em portugues que a conversa voltou ao portugues."
    }

    val state = decision.nextState
    val dialectTag = state.dialect.languageTag
    return buildString {
      appendLine("[ORIENTACAO DE INGLES INTERNA]")
      appendLine("Voce e um professor de ingles natural, paciente e preciso. Dialeto: $dialectTag.")
      if (state.activity == EnglishActivity.FREE_CONVERSATION) {
        appendLine("Converse em ingles, com vocabulario adequado ao usuario. Responda somente em ingles.")
        appendLine("Nao use marcadores de idioma neste turno, pois toda a resposta e inglesa.")
      } else {
        appendLine("Explique em portugues e escreva exemplos ou demonstracoes exclusivamente em ingles.")
        appendLine("Marque CADA trecho, inclusive o primeiro, com [[pt-BR]] ou [[$dialectTag]].")
        appendLine("Nunca misture os dois idiomas dentro do mesmo trecho marcado e nunca explique os marcadores.")
      }
      when (decision.intent) {
        EnglishLessonIntent.START ->
          appendLine("Ensine apenas um pequeno bloco. Use um exemplo curto em ingles e confirme se fez sentido.")
        EnglishLessonIntent.PRONUNCIATION -> {
          state.expectedPhrase?.let { appendLine("Frase ou palavra solicitada: <ALVO>${sanitize(it)}</ALVO>.") }
          appendLine("Demonstre o alvo em ingles e explique em portugues apenas uma dica auditiva simples.")
        }
        EnglishLessonIntent.SLOWER -> {
          state.expectedPhrase?.let { appendLine("Repita este alvo sem altera-lo: <ALVO>${sanitize(it)}</ALVO>.") }
          appendLine("Demonstre novamente e mantenha a explicacao muito curta.")
        }
        EnglishLessonIntent.REPEAT -> {
          state.expectedPhrase?.let { appendLine("Prepare a repeticao deste alvo: <ALVO>${sanitize(it)}</ALVO>.") }
          appendLine("Demonstre uma vez em ingles e convide o usuario a repetir. A proxima entrada sera em ingles.")
        }
        EnglishLessonIntent.LEARNER_ATTEMPT -> {
          state.expectedPhrase?.let { appendLine("Frase esperada: <ALVO>${sanitize(it)}</ALVO>.") }
          intelligibility?.let {
            appendLine("Analise local de inteligibilidade: ${it.promptSummary()}.")
          }
          if (hasPronunciationAudio) {
            appendLine("A tentativa do usuario esta no audio anexado. Compare-a diretamente com a frase esperada.")
            appendLine("Avalie palavras compreensiveis e ritmo geral, sem inventar fonemas, precisao tecnica ou porcentagem.")
          }
          appendLine("Reconheca primeiro o que ficou claro e corrija no maximo um ponto. Avalie inteligibilidade, nao sotaque ou fonemas.")
          appendLine("Convide para tentar novamente apenas quando isso for util.")
        }
        else -> appendLine("Continue em um bloco curto e avance somente uma dificuldade por vez.")
      }
      append("Esta orientacao e invisivel e nunca deve ser citada.")
    }
  }

  private fun sanitize(text: String): String =
    text.replace('<', '(').replace('>', ')').replace('[', '(').replace(']', ')').take(180)
}
