package com.google.ai.edge.gallery.voice.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.voice.domain.SpeechState
import com.google.ai.edge.gallery.voice.domain.VoiceChatManager
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ExperimentalApi
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalApi::class)
class VoiceViewModel(
  private val context: Context,
  private val modelManagerViewModel: ModelManagerViewModel,
) : ViewModel() {

  private val voiceChatManager = VoiceChatManager(context)

  private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
  val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

  private val _recognizedText = MutableStateFlow("")
  val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

  private val _lastResponse = MutableStateFlow("")
  val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

  private val _responseProfile = MutableStateFlow(ResponseDepthProfile.FLASH)
  val responseProfile: StateFlow<ResponseDepthProfile> = _responseProfile.asStateFlow()

  private var activeModel: Model? = null
  private var activeTask: Task? = null
  private var isConversationReset = false
  private var responseGenerationInProgress = false
  private var lastTurnContext: TurnContext? = null

  private val systemPrompt =
    """
      Voce e o Kabem, assistente de voz pessoal. Responda de forma natural e coloquial, como em uma conversa por telefone.
      Por padrao, seja breve. Quando a mensagem do usuario vier com uma INSTRUCAO INTERNA DE ESTILO, siga essa instrucao para ajustar profundidade, estrutura e tamanho da resposta.
      Evite Markdown pesado e emojis. Prefira frases faladas, claras e faceis de ouvir.
    """
      .trimIndent()

  init {
    viewModelScope.launch {
      modelManagerViewModel.uiState.collectLatest { managerState ->
        val task = managerState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
        activeTask = task
        if (task != null) {
          val downloaded =
            task.models.firstOrNull { model ->
              managerState.modelDownloadStatus[model.name]?.status ==
                ModelDownloadStatusType.SUCCEEDED
            }
          activeModel = downloaded

          if (downloaded == null) {
            _uiState.value = VoiceUiState.NoModel
          } else {
            val initStatus = managerState.modelInitializationStatus[downloaded.name]?.status
            when (initStatus) {
              ModelInitializationStatusType.INITIALIZED -> {
                if (_uiState.value is VoiceUiState.Loading || _uiState.value is VoiceUiState.NoModel) {
                  _uiState.value = VoiceUiState.Idle
                }
                ensureConversationReset(downloaded)
              }
              ModelInitializationStatusType.INITIALIZING -> {
                _uiState.value =
                  VoiceUiState.Loading("Inicializando o modelo '${downloaded.name}' no celular...")
              }
              ModelInitializationStatusType.ERROR -> {
                val errorMsg =
                  managerState.modelInitializationStatus[downloaded.name]?.error
                    ?: "Erro ao carregar o modelo."
                _uiState.value = VoiceUiState.Error(errorMsg)
              }
              else -> {
                _uiState.value = VoiceUiState.Loading("Carregando o modelo '${downloaded.name}'...")
                modelManagerViewModel.initializeModel(context, task, downloaded)
              }
            }
          }
        } else {
          _uiState.value = VoiceUiState.NoModel
        }
      }
    }

    viewModelScope.launch {
      voiceChatManager.speechState.collectLatest { state ->
        when (state) {
          is SpeechState.ResultReady -> {
            _recognizedText.value = state.text
            _uiState.value = VoiceUiState.Generating
            generateResponse(state.text)
          }
          is SpeechState.Listening -> {
            _uiState.value = VoiceUiState.Listening
          }
          is SpeechState.Processing -> {
            _uiState.value = VoiceUiState.Generating
          }
          is SpeechState.Speaking -> {
            _uiState.value = VoiceUiState.Speaking
          }
          is SpeechState.Error -> {
            Log.e("VoiceViewModel", "Speech Recognizer Error: ${state.message}")
            _uiState.value = VoiceUiState.Idle
          }
          is SpeechState.Idle -> {
            if (_uiState.value is VoiceUiState.Speaking && !responseGenerationInProgress) {
              _uiState.value = VoiceUiState.Idle
              kotlinx.coroutines.delay(800)
              startListening()
            } else if (_uiState.value is VoiceUiState.Speaking) {
              _uiState.value = VoiceUiState.Generating
            }
          }
        }
      }
    }

    viewModelScope.launch {
      voiceChatManager.recognizedText.collectLatest { text -> _recognizedText.value = text }
    }
  }

  private fun ensureConversationReset(model: Model) {
    if (!isConversationReset) {
      isConversationReset = true
      viewModelScope.launch(Dispatchers.Default) {
        try {
          model.runtimeHelper.resetConversation(
            model = model,
            systemInstruction = Contents.of(systemPrompt),
          )
          Log.d("VoiceViewModel", "Conversation turns reset for model ${model.name}")
        } catch (e: Exception) {
          Log.e("VoiceViewModel", "Failed to reset conversation", e)
        }
      }
    }
  }

  fun startListening() {
    val model = activeModel
    if (model != null && modelManagerViewModel.uiState.value.isModelInitialized(model)) {
      _recognizedText.value = ""
      voiceChatManager.startListening()
    }
  }

  fun stopListening() {
    voiceChatManager.stopListening()
  }

  fun toggleListening() {
    when (_uiState.value) {
      is VoiceUiState.Idle -> startListening()
      is VoiceUiState.Listening -> stopListening()
      is VoiceUiState.Speaking -> {
        voiceChatManager.stopSpeaking()
        _uiState.value = VoiceUiState.Idle
      }
      else -> {}
    }
  }

  private fun generateResponse(prompt: String) {
    val model = activeModel ?: return
    val profile = determineResponseProfile(lastTurnContext, prompt)
    val wrappedPrompt = buildPromptWithInternalInstruction(profile, prompt)
    var accumulatedText = ""
    var pendingSpeechText = ""
    var hasSpokenFirstSegment = false

    responseGenerationInProgress = true
    _responseProfile.value = profile

    viewModelScope.launch(Dispatchers.Default) {
      try {
        model.runtimeHelper.runInference(
          model = model,
          input = wrappedPrompt,
          resultListener = { partialResult, done, _ ->
            accumulatedText += partialResult
            pendingSpeechText += partialResult

            val completeSegments = drainCompleteSentences(pendingSpeechText)
            pendingSpeechText = completeSegments.remainingText
            for (segment in completeSegments.sentences) {
              hasSpokenFirstSegment =
                speakStreamingSegment(segment, isFirstSegment = !hasSpokenFirstSegment) ||
                  hasSpokenFirstSegment
            }

            if (done) {
              responseGenerationInProgress = false
              val finalSegment = pendingSpeechText.trim()
              if (finalSegment.isNotBlank()) {
                hasSpokenFirstSegment =
                  speakStreamingSegment(finalSegment, isFirstSegment = !hasSpokenFirstSegment) ||
                    hasSpokenFirstSegment
                pendingSpeechText = ""
              }

              viewModelScope.launch(Dispatchers.Main) {
                _lastResponse.value = accumulatedText
                lastTurnContext =
                  TurnContext(
                    profile = profile,
                    assistantSummary = summarizeForTurnContext(accumulatedText),
                  )
                if (!hasSpokenFirstSegment && accumulatedText.isNotBlank()) {
                  _uiState.value = VoiceUiState.Speaking
                  voiceChatManager.speak(accumulatedText)
                }
              }
            }
          },
          cleanUpListener = {},
          onError = { error ->
            responseGenerationInProgress = false
            viewModelScope.launch(Dispatchers.Main) {
              _uiState.value = VoiceUiState.Error("Erro na geracao da resposta: $error")
            }
          },
          coroutineScope = viewModelScope,
        )
      } catch (e: Exception) {
        responseGenerationInProgress = false
        Log.e("VoiceViewModel", "Failed to run local inference", e)
        withContext(Dispatchers.Main) {
          _uiState.value = VoiceUiState.Error("Excecao na inferencia: ${e.message}")
        }
      }
    }
  }

  private fun speakStreamingSegment(segment: String, isFirstSegment: Boolean): Boolean {
    val cleanSegment = segment.trim()
    if (cleanSegment.isBlank()) {
      return false
    }

    viewModelScope.launch(Dispatchers.Main) {
      _uiState.value = VoiceUiState.Speaking
      voiceChatManager.speak(cleanSegment, flushQueue = isFirstSegment)
    }
    return true
  }

  private fun determineResponseProfile(
    previousTurn: TurnContext?,
    text: String,
  ): ResponseDepthProfile {
    val normalized = normalizeForMatching(text)
    val explicitMatch = findLastProfileMatch(normalized)
    if (explicitMatch != null) {
      return explicitMatch.profile
    }

    if (isContinuationRequest(normalized) && previousTurn != null) {
      return when (previousTurn.profile) {
        ResponseDepthProfile.FLASH -> ResponseDepthProfile.DETAILED
        else -> previousTurn.profile
      }
    }

    return ResponseDepthProfile.FLASH
  }

  private fun buildPromptWithInternalInstruction(
    profile: ResponseDepthProfile,
    userText: String,
  ): String {
    val previousSummary =
      lastTurnContext?.assistantSummary
        ?.takeIf { it.isNotBlank() }
        ?.let { " Contexto anterior curto: $it" }
        ?: ""

    return """
      [INSTRUCAO INTERNA DE ESTILO: ${profile.internalInstruction}$previousSummary]
      Fala do usuario: $userText
    """
      .trimIndent()
  }

  private fun drainCompleteSentences(text: String): SentenceDrainResult {
    if (text.isBlank()) {
      return SentenceDrainResult(emptyList(), text)
    }

    val sentences = mutableListOf<String>()
    var sentenceStart = 0
    var index = 0
    while (index < text.length) {
      val char = text[index]
      if (char == '.' || char == '!' || char == '?') {
        val end = index + 1
        val sentence = text.substring(sentenceStart, end).trim()
        if (sentence.isNotBlank()) {
          sentences.add(sentence)
        }
        sentenceStart = end
      }
      index++
    }

    val remaining = text.substring(sentenceStart).trimStart()
    return SentenceDrainResult(sentences, remaining)
  }

  private fun findLastProfileMatch(normalized: String): ProfileMatch? {
    val matches = mutableListOf<ProfileMatch>()
    for ((profile, patterns) in PROFILE_PATTERNS) {
      for (pattern in patterns) {
        val match = pattern.findAll(normalized).lastOrNull()
        if (match != null) {
          matches.add(ProfileMatch(profile, match.range.first))
        }
      }
    }
    return matches.maxByOrNull { it.position }
  }

  private fun isContinuationRequest(normalized: String): Boolean {
    return CONTINUATION_PATTERNS.any { it.matches(normalized) || it.containsMatchIn(normalized) }
  }

  private fun summarizeForTurnContext(text: String): String {
    return text.replace(Regex("\\s+"), " ").trim().take(180)
  }

  private fun normalizeForMatching(text: String): String {
    return Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{Mn}+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  override fun onCleared() {
    super.onCleared()
    voiceChatManager.release()
  }

  class Factory(
    private val context: Context,
    private val modelManagerViewModel: ModelManagerViewModel,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      @Suppress("UNCHECKED_CAST")
      return VoiceViewModel(context.applicationContext, modelManagerViewModel) as T
    }
  }
}

sealed class VoiceUiState {
  object Idle : VoiceUiState()
  object Listening : VoiceUiState()
  object Generating : VoiceUiState()
  object Speaking : VoiceUiState()
  object NoModel : VoiceUiState()
  data class Loading(val message: String) : VoiceUiState()
  data class Error(val message: String) : VoiceUiState()
}

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

private data class TurnContext(
  val profile: ResponseDepthProfile,
  val assistantSummary: String,
)

private data class ProfileMatch(
  val profile: ResponseDepthProfile,
  val position: Int,
)

private data class SentenceDrainResult(
  val sentences: List<String>,
  val remainingText: String,
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
