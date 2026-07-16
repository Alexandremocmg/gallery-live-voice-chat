package com.google.ai.edge.gallery.voice.intelligence

import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelCapability
import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import java.text.Normalizer

enum class ConnectivityMode(val storageValue: String, val label: String) {
  PRIVATE_OFFLINE("private_offline", "Privado"),
  CONNECTED("connected", "Conectado");

  companion object {
    fun fromStorage(value: String): ConnectivityMode =
      entries.firstOrNull { it.storageValue == value } ?: PRIVATE_OFFLINE
  }
}

enum class VoiceTurnIntent {
  CONVERSATION,
  TEACHING,
  PERCEPTION,
  SKILL,
  MCP,
}

enum class CognitiveMode {
  FAST,
  DEEP,
}

data class RequiredCapabilities(
  val image: Boolean = false,
  val audio: Boolean = false,
  val tools: Boolean = false,
  val thinking: Boolean = false,
  val network: Boolean = false,
)

data class VoiceTurnDecision(
  val intent: VoiceTurnIntent,
  val cognitiveMode: CognitiveMode,
  val requiredCapabilities: RequiredCapabilities,
  val activeSkillId: String? = null,
)

class CognitivePolicy {
  fun decide(
    text: String,
    profile: ResponseDepthProfile,
    teachingDeepen: Boolean,
    documentPageCount: Int = 0,
  ): CognitiveMode {
    val normalized = normalize(text)
    val explicitDeep = DEEP_PATTERN.containsMatchIn(normalized)
    val complexDocument = documentPageCount > 1 && DOCUMENT_SYNTHESIS_PATTERN.containsMatchIn(normalized)
    return if (
      profile == ResponseDepthProfile.STRATEGIC || teachingDeepen || explicitDeep || complexDocument
    ) {
      CognitiveMode.DEEP
    } else {
      CognitiveMode.FAST
    }
  }
}

class VoiceCapabilityRouter(
  private val cognitivePolicy: CognitivePolicy = CognitivePolicy(),
) {
  fun route(
    text: String,
    profile: ResponseDepthProfile,
    teachingActive: Boolean,
    teachingDeepen: Boolean,
    activeSkillId: String?,
    hasImages: Boolean,
    hasAudio: Boolean,
    pdfPageCount: Int,
    connectivityMode: ConnectivityMode,
  ): VoiceTurnDecision {
    val normalized = normalize(text)
    val perception = hasImages || hasAudio || pdfPageCount > 0 || PERCEPTION_PATTERN.containsMatchIn(normalized)
    val mcpRequested = MCP_PATTERN.containsMatchIn(normalized)
    val intent = when {
      mcpRequested && connectivityMode == ConnectivityMode.CONNECTED -> VoiceTurnIntent.MCP
      activeSkillId != null -> VoiceTurnIntent.SKILL
      perception -> VoiceTurnIntent.PERCEPTION
      teachingActive -> VoiceTurnIntent.TEACHING
      else -> VoiceTurnIntent.CONVERSATION
    }
    val cognitiveMode =
      cognitivePolicy.decide(
        text = text,
        profile = profile,
        teachingDeepen = teachingDeepen,
        documentPageCount = pdfPageCount,
      )
    return VoiceTurnDecision(
      intent = intent,
      cognitiveMode = cognitiveMode,
      activeSkillId = activeSkillId,
      requiredCapabilities =
        RequiredCapabilities(
          image = hasImages,
          audio = hasAudio,
          tools = intent == VoiceTurnIntent.MCP,
          thinking = cognitiveMode == CognitiveMode.DEEP,
          network = intent == VoiceTurnIntent.MCP,
        ),
    )
  }
}

data class ModelSelection(
  val model: Model?,
  val score: Int,
  val reason: String,
)

class VoiceModelSelector {
  fun select(
    models: List<Model>,
    required: RequiredCapabilities,
    availableMemoryGb: Int = Int.MAX_VALUE,
  ): ModelSelection {
    val compatible = models.filter { model ->
      (!required.image || model.llmSupportImage) &&
        (!required.audio || model.llmSupportAudio) &&
        (!required.thinking || model.capabilities.contains(ModelCapability.LLM_THINKING)) &&
        ((model.minDeviceMemoryInGb ?: 0) <= availableMemoryGb)
    }
    if (compatible.isEmpty()) {
      return ModelSelection(null, Int.MIN_VALUE, "Nenhum modelo baixado oferece todas as capacidades do turno")
    }

    val ranked = compatible.map { it to score(it, required) }.sortedByDescending { it.second }
    val (selected, score) = ranked.first()
    val strengths = buildList {
      if (selected.llmSupportImage) add("visao")
      if (selected.llmSupportAudio) add("audio")
      if (selected.capabilities.contains(ModelCapability.LLM_THINKING)) add("thinking")
      if (selected.llmMaxContextLength != null) add("contexto ${selected.llmMaxContextLength}")
    }
    return ModelSelection(selected, score, strengths.joinToString().ifBlank { "conversa local" })
  }

  private fun score(model: Model, required: RequiredCapabilities): Int {
    var score = 0
    if (model.llmSupportImage) score += if (required.image) 80 else 16
    if (model.llmSupportAudio) score += if (required.audio) 80 else 18
    if (model.capabilities.contains(ModelCapability.LLM_THINKING)) {
      score += if (required.thinking) 70 else 14
    }
    score += ((model.llmMaxContextLength ?: model.llmMaxToken).coerceAtMost(32_000) / 1_000)
    score -= (model.minDeviceMemoryInGb ?: 0) * 2
    if (model.name.contains("E2B", ignoreCase = true)) score += 4
    return score
  }
}

fun Model.supportsThinkingFor(taskId: String): Boolean {
  if (!capabilities.contains(ModelCapability.LLM_THINKING)) return false
  val allowedTasks = capabilityToTaskTypes[ModelCapability.LLM_THINKING]
  return allowedTasks.isNullOrEmpty() || taskId in allowedTasks
}

internal fun normalize(value: String): String =
  Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace(Regex("\\s+"), " ")
    .trim()

private val DEEP_PATTERN =
  Regex("\\b(analise profundamente|pense com cuidado|pense profundamente|raciocine|trade-?offs?|pros e contras|plano completo|estrategia|compare em detalhes)\\b")
private val DOCUMENT_SYNTHESIS_PATTERN =
  Regex("\\b(compare|relacione|sintetize|analise|conclusao|documento inteiro|varias paginas)\\b")
private val PERCEPTION_PATTERN =
  Regex("\\b(o que voce (?:ve|ouve)|olhe|observe|leia (?:isto|isso)|nesta imagem|neste audio|neste pdf)\\b")
private val MCP_PATTERN = Regex("\\b(mcp|servidor conectado|ferramenta online)\\b")
