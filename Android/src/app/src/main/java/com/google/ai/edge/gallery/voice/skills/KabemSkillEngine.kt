package com.google.ai.edge.gallery.voice.skills

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.intelligence.RequiredCapabilities
import com.google.ai.edge.gallery.voice.language.EnglishLessonIntent
import java.text.Normalizer

enum class SkillNetworkAccess {
  NONE,
  OPTIONAL,
  REQUIRED,
}

enum class SkillMemoryScope {
  NONE,
  SESSION,
  LEARNING_PROGRESS,
}

data class KabemSkillManifest(
  val id: String,
  val version: Int,
  val name: String,
  val description: String,
  val activationPhrases: List<String>,
  val capabilities: RequiredCapabilities,
  val supportedLanguages: Set<String>,
  val networkAccess: SkillNetworkAccess,
  val memoryScope: SkillMemoryScope,
  val internalInstruction: String,
)

data class SkillActivation(
  val activeSkill: KabemSkillManifest?,
  val changed: Boolean,
  val blockedReason: String? = null,
)

object KabemBuiltInSkills {
  const val ENGLISH_TEACHER_ID = "kabem.english_teacher"
  const val PDF_TEACHER_ID = "kabem.pdf_teacher"
  const val VISUAL_READER_ID = "kabem.visual_reader"

  val englishTeacher =
    KabemSkillManifest(
      id = ENGLISH_TEACHER_ID,
      version = 1,
      name = "Professor de Inglês",
      description = "Aulas adaptativas, conversação e prática de pronúncia.",
      activationPhrases =
        listOf("estudar ingles", "aprender ingles", "praticar ingles", "conversacao em ingles", "minha pronuncia"),
      capabilities = RequiredCapabilities(audio = true),
      supportedLanguages = setOf("pt-BR", "en-US", "en-GB"),
      networkAccess = SkillNetworkAccess.NONE,
      memoryScope = SkillMemoryScope.LEARNING_PROGRESS,
      internalInstruction =
        "A Skill Professor de Ingles esta ativa. Ensine em blocos curtos, mantenha o nivel adaptativo, use ingles nos exemplos e PT-BR nas explicacoes quando isso ajudar. Termine cada bloco com uma verificacao breve e avance somente quando houver sinal de compreensao.",
    )

  val pdfTeacher =
    KabemSkillManifest(
      id = PDF_TEACHER_ID,
      version = 1,
      name = "Professor de PDF",
      description = "Ensina e revisa o documento aberto.",
      activationPhrases = listOf("estudar este pdf", "me ensine este documento", "professor de pdf"),
      capabilities = RequiredCapabilities(image = true),
      supportedLanguages = setOf("pt-BR"),
      networkAccess = SkillNetworkAccess.NONE,
      memoryScope = SkillMemoryScope.SESSION,
      internalInstruction =
        "A Skill Professor de PDF esta ativa. Use somente o documento fornecido, cite paginas e ensine um conceito por vez antes de propor uma pergunta curta de verificacao.",
    )

  val visualReader =
    KabemSkillManifest(
      id = VISUAL_READER_ID,
      version = 1,
      name = "Leitor Visual",
      description = "Explica imagens, textos e cenas capturados pela camera.",
      activationPhrases = listOf("o que voce esta vendo", "leia isto", "explique esta imagem"),
      capabilities = RequiredCapabilities(image = true),
      supportedLanguages = setOf("pt-BR"),
      networkAccess = SkillNetworkAccess.NONE,
      memoryScope = SkillMemoryScope.SESSION,
      internalInstruction =
        "A Skill Leitor Visual esta ativa. Descreva apenas elementos sustentados pela imagem, deixe incertezas claras e priorize o que o usuario perguntou.",
    )

  val all = listOf(englishTeacher, pdfTeacher, visualReader)
}

class KabemSkillEngine(
  private val installedSkills: List<KabemSkillManifest> = KabemBuiltInSkills.all,
) {
  fun resolve(
    text: String,
    currentSkillId: String?,
    englishIntent: EnglishLessonIntent,
    hasPdf: Boolean,
    hasImages: Boolean,
    connectivityMode: ConnectivityMode,
  ): SkillActivation {
    val normalized = normalize(text)
    if (EXIT_SKILL_PATTERN.containsMatchIn(normalized)) {
      return SkillActivation(activeSkill = null, changed = currentSkillId != null)
    }

    val explicit = installedSkills.firstOrNull { skill ->
      skill.activationPhrases.any { normalized.contains(normalize(it)) }
    }
    val inferred = when {
      englishIntent != EnglishLessonIntent.NONE -> installedSkills.firstOrNull { it.id == KabemBuiltInSkills.ENGLISH_TEACHER_ID }
      hasPdf && PDF_PATTERN.containsMatchIn(normalized) -> installedSkills.firstOrNull { it.id == KabemBuiltInSkills.PDF_TEACHER_ID }
      hasImages && VISUAL_PATTERN.containsMatchIn(normalized) -> installedSkills.firstOrNull { it.id == KabemBuiltInSkills.VISUAL_READER_ID }
      else -> null
    }
    val selected = explicit ?: inferred ?: installedSkills.firstOrNull { it.id == currentSkillId }
    if (selected?.networkAccess == SkillNetworkAccess.REQUIRED && connectivityMode == ConnectivityMode.PRIVATE_OFFLINE) {
      return SkillActivation(null, currentSkillId != null, "Esta Skill exige o modo conectado")
    }
    return SkillActivation(selected, selected?.id != currentSkillId)
  }

  fun get(skillId: String?): KabemSkillManifest? = installedSkills.firstOrNull { it.id == skillId }

  private fun normalize(value: String): String =
    Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
}

private val EXIT_SKILL_PATTERN = Regex("\\b(sair da skill|encerrar a aula|terminar a aula|modo normal|mudar de assunto)\\b")
private val PDF_PATTERN = Regex("\\b(pdf|documento|pagina|capitulo)\\b")
private val VISUAL_PATTERN = Regex("\\b(imagem|foto|camera|vendo|visual|leia isto)\\b")
