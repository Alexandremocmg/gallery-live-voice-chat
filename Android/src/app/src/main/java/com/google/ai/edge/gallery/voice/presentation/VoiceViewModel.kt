package com.google.ai.edge.gallery.voice.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.voice.domain.SpeechState
import com.google.ai.edge.gallery.voice.domain.VoiceChatManager
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ExperimentalApi
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
    private val modelManagerViewModel: ModelManagerViewModel
) : ViewModel() {

    private val voiceChatManager = VoiceChatManager(context)

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private var activeModel: Model? = null
    private var activeTask: Task? = null
    private var isConversationReset = false

    private val systemPrompt = """
        Você é o Kabem, assistente de voz pessoal. Responda de forma muito curta, natural e coloquial, como em uma conversa por telefone. 
        Evite formatação Markdown, listas ou emojis. Limite-se a no máximo uma ou duas frases curtas por resposta.
    """.trimIndent()

    init {
        // Observe modelManagerViewModel state to check for downloaded models and initialization status
        viewModelScope.launch {
            modelManagerViewModel.uiState.collectLatest { managerState ->
                val task = managerState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
                activeTask = task
                if (task != null) {
                    val downloaded = task.models.firstOrNull { model ->
                        managerState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
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
                                _uiState.value = VoiceUiState.Loading("Inicializando o modelo '${downloaded.name}' no celular...")
                            }
                            ModelInitializationStatusType.ERROR -> {
                                val errorMsg = managerState.modelInitializationStatus[downloaded.name]?.error ?: "Erro ao carregar o modelo."
                                _uiState.value = VoiceUiState.Error(errorMsg)
                            }
                            else -> {
                                // Trigger initialization
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

        // Listen to Speech Recognizer events
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
                        // In phone call mode, don't crash, just log and go idle so the user can speak again
                        Log.e("VoiceViewModel", "Speech Recognizer Error: ${state.message}")
                        _uiState.value = VoiceUiState.Idle
                    }
                    is SpeechState.Idle -> {
                        // If we finished speaking and are back to Idle, auto-restart listening (phone call conversation loop!)
                        if (_uiState.value is VoiceUiState.Speaking) {
                            _uiState.value = VoiceUiState.Idle
                            kotlinx.coroutines.delay(800) // Small pause between turns
                            startListening()
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            voiceChatManager.recognizedText.collectLatest { text ->
                _recognizedText.value = text
            }
        }
    }

    private fun ensureConversationReset(model: Model) {
        if (!isConversationReset) {
            isConversationReset = true
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    model.runtimeHelper.resetConversation(
                        model = model,
                        systemInstruction = Contents.of(systemPrompt)
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
        var accumulatedText = ""

        viewModelScope.launch(Dispatchers.Default) {
            try {
                model.runtimeHelper.runInference(
                    model = model,
                    input = prompt,
                    resultListener = { partialResult, done, thinking ->
                        accumulatedText += partialResult
                        if (done) {
                            viewModelScope.launch(Dispatchers.Main) {
                                _lastResponse.value = accumulatedText
                                _uiState.value = VoiceUiState.Speaking
                                voiceChatManager.speak(accumulatedText)
                            }
                        }
                    },
                    cleanUpListener = {
                        // Inference completed clean up
                    },
                    onError = { error ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.value = VoiceUiState.Error("Erro na geração da resposta: $error")
                        }
                    },
                    coroutineScope = viewModelScope
                )
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Failed to run local inference", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = VoiceUiState.Error("Exceção na inferência: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceChatManager.release()
    }

    class Factory(
        private val context: Context,
        private val modelManagerViewModel: ModelManagerViewModel
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
