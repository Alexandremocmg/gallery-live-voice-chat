package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import java.text.Normalizer

enum class ResponseProfileSource {
  DEFAULT,
  EXPLICIT,
  CONTINUATION,
}

data class ResponseProfileSelection(
  val profile: ResponseDepthProfile,
  val source: ResponseProfileSource,
) {
  val isExplicit: Boolean
    get() = source == ResponseProfileSource.EXPLICIT
}

class ResponseDepthClassifier {
  fun select(previousProfile: ResponseDepthProfile?, text: String): ResponseProfileSelection {
    val normalized = normalize(text)
    val explicitMatch = findLastProfileMatch(normalized)
    if (explicitMatch != null) {
      return ResponseProfileSelection(explicitMatch.profile, ResponseProfileSource.EXPLICIT)
    }

    if (isContinuationRequest(normalized) && previousProfile != null) {
      val profile =
        when (previousProfile) {
          ResponseDepthProfile.FLASH -> ResponseDepthProfile.DETAILED
          else -> previousProfile
        }
      return ResponseProfileSelection(profile, ResponseProfileSource.CONTINUATION)
    }

    return ResponseProfileSelection(ResponseDepthProfile.FLASH, ResponseProfileSource.DEFAULT)
  }

  fun effectiveForTeaching(
    selection: ResponseProfileSelection,
    teachingActive: Boolean,
  ): ResponseDepthProfile {
    val isExplicitShort = selection.isExplicit && selection.profile == ResponseDepthProfile.FLASH
    return if (teachingActive && selection.profile == ResponseDepthProfile.FLASH && !isExplicitShort) {
      ResponseDepthProfile.DETAILED
    } else {
      selection.profile
    }
  }

  private fun findLastProfileMatch(normalized: String): ProfileMatch? {
    val matches = mutableListOf<ProfileMatch>()
    for ((profile, patterns) in PROFILE_PATTERNS) {
      for (pattern in patterns) {
        val match = pattern.findAll(normalized).lastOrNull() ?: continue
        matches.add(ProfileMatch(profile, match.range.first))
      }
    }
    return matches.maxByOrNull { it.position }
  }

  private fun isContinuationRequest(normalized: String): Boolean {
    return CONTINUATION_PATTERNS.any { it.matches(normalized) || it.containsMatchIn(normalized) }
  }

  private fun normalize(text: String): String {
    return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
  }
}

private data class ProfileMatch(
  val profile: ResponseDepthProfile,
  val position: Int,
)

private val PROFILE_PATTERNS: Map<ResponseDepthProfile, List<Regex>> =
  mapOf(
    ResponseDepthProfile.FLASH to
      listOf(
        Regex(
          "\\b(oi|ola|bom dia|boa tarde|boa noite|e ai|resuma|resumir|rapido|rapidinho|curto|em poucas palavras|que horas|onde fica|o que e)\\b"
        )
      ),
    ResponseDepthProfile.DETAILED to
      listOf(
        Regex(
          "\\b(me explica|explique|explica|como funciona|por que|porque|me fale mais|detalhe|detalhado|profundo|aprofundar|aprofunda|quero entender|entender melhor)\\b"
        )
      ),
    ResponseDepthProfile.STEP_BY_STEP to
      listOf(
        Regex(
          "\\b(passo a passo|tutorial|me ensine|ensina|do zero|como fazer|guia pratico|me mostra como|etapas|procedimento)\\b"
        )
      ),
    ResponseDepthProfile.STRATEGIC to
      listOf(
        Regex(
          "\\b(plano|estrategia|estrategico|analise|analisar|compare|comparar|comparacao|roteiro|planejamento|cronograma|decisao|vantagens|desvantagens)\\b"
        )
      ),
  )

private val CONTINUATION_PATTERNS =
  listOf(
    Regex("^\\s*(mais|continua|continue|e depois|depois|segue|prossiga|vai|pode continuar)\\s*[?.!]*\\s*$"),
    Regex("\\b(aprofunda nisso|explique melhor isso|fala mais disso|continua nisso|me da mais detalhes)\\b"),
  )
