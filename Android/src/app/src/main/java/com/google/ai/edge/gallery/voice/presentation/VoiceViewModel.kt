package com.google.ai.edge.gallery.voice.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.google.ai.edge.gallery.voice.data.DownloadState
import com.google.ai.edge.gallery.voice.data.ModelDownloader
import com.google.ai.edge.gallery.voice.domain.SpeechState
import com.google.ai.edge.gallery.voice.domain.VoiceChatManager
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalApi::class)
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val modelDownloader = ModelDownloader(context)
    private val voiceChatManager = VoiceChatManager(context)

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    // System prompt for Kabem
    private val systemPrompt = """
        Você é o Kabem, assistente pessoal. Respostas curtas, informais, diretas e naturais. 
        Como a interface é de voz, evite formatação Markdown. Responda em Português do Brasil 
        de forma coloquial e prestativa. Limite-se a 2 frases por resposta.
    """.trimIndent()

    init {
        checkModelStatus()

        viewModelScope.launch {
            voiceChatManager.speechState.collectLatest { state ->
                when (state) {
                    is SpeechState.ResultReady -> {
                        _uiState.value = VoiceUiState.Generating
                        generateResponse(state.text)
                    }
                    is SpeechState.Listening -> _uiState.value = VoiceUiState.Listening
                    is SpeechState.Processing -> _uiState.value = VoiceUiState.Generating
                    is SpeechState.Speaking -> _uiState.value = VoiceUiState.Speaking
                    is SpeechState.Error -> {
                        _uiState.value = VoiceUiState.Idle
                        Log.e("VoiceViewModel", "Speech Error: ${state.message}")
                    }
                    is SpeechState.Idle -> {
                        if (_uiState.value !is VoiceUiState.Generating) {
                            _uiState.value = VoiceUiState.Idle
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

    private fun checkModelStatus() {
        if (modelDownloader.isModelDownloaded()) {
            val file = modelDownloader.getModelFile()
            if (file != null) {
                initEngine(file.absolutePath)
            }
        } else {
            _downloadState.value = DownloadState.Idle
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            modelDownloader.downloadModel().collectLatest { state ->
                _downloadState.value = state
                if (state is DownloadState.Success) {
                    initEngine(state.file.absolutePath)
                }
            }
        }
    }

    private fun initEngine(modelPath: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU() // Try GPU for performance
                )
                engine = Engine(engineConfig)
                engine?.initialize()

                val config = ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt)
                )
                conversation = engine?.createConversation(config)
                _uiState.value = VoiceUiState.Idle
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Failed to initialize LiteRT Engine", e)
                _uiState.value = VoiceUiState.Error("Falha ao inicializar a IA.")
            }
        }
    }

    fun toggleListening() {
        when (_uiState.value) {
            is VoiceUiState.Idle -> {
                voiceChatManager.startListening()
            }
            is VoiceUiState.Listening -> {
                voiceChatManager.stopListening()
            }
            is VoiceUiState.Speaking -> {
                voiceChatManager.stopSpeaking()
                _uiState.value = VoiceUiState.Idle
            }
            else -> {}
        }
    }

    private fun generateResponse(prompt: String) {
        if (conversation == null) {
            _uiState.value = VoiceUiState.Error("A IA não está pronta.")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                var fullResponse = ""
                conversation?.sendMessageAsync(
                    Contents.of(prompt),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            fullResponse = message.toString()
                        }

                        override fun onDone() {
                            // Speak the full response
                            voiceChatManager.speak(fullResponse)
                        }

                        override fun onError(throwable: Throwable) {
                            Log.e("VoiceViewModel", "Inference Error", throwable)
                            _uiState.value = VoiceUiState.Error("Erro na geração da resposta.")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("VoiceViewModel", "Inference Exception", e)
                _uiState.value = VoiceUiState.Error("Exceção na geração da resposta.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceChatManager.release()
        conversation?.close()
        engine?.close()
    }
}

sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object Listening : VoiceUiState()
    object Generating : VoiceUiState()
    object Speaking : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}
