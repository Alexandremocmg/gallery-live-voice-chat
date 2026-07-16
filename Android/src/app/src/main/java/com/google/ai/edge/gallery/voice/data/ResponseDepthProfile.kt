package com.google.ai.edge.gallery.voice.data

enum class ResponseDepthProfile(
  val label: String,
  val internalInstruction: String,
) {
  FLASH(
    label = "Curto",
    internalInstruction =
      "O usuario quer uma conversa rapida. Responda direto, em ate duas frases curtas, sem lista.",
  ),
  DETAILED(
    label = "Detalhado",
    internalInstruction =
      "O usuario quer entender melhor. Responda com clareza, em um paragrafo conceitual limpo, com no maximo um exemplo curto.",
  ),
  STEP_BY_STEP(
    label = "Passo a passo",
    internalInstruction =
      "O usuario quer um tutorial pratico. Explique em etapas curtas, usando conectivos falados como Primeiro, Depois e Por fim.",
  ),
  STRATEGIC(
    label = "Estrategico",
    internalInstruction =
      "O usuario quer plano, analise, comparacao ou roteiro. Organize a resposta em blocos conceituais curtos e conclua com uma recomendacao objetiva.",
  ),
}
