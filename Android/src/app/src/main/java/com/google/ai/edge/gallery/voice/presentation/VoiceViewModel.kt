package com.google.ai.edge.gallery.voice.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.voice.data.DownloadState
import com.google.ai.edge.gallery.voice.data.ModelDownloader
import com.google.ai.edge.gallery.voice.domain.SpeechState
import com.google.ai.edge.gallery.voice.domain.VoiceChatManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoiceViewModel(private val context: Context) : ViewModel() {

    private val modelDownloader = ModelDownloader(context)
    private val voiceChatManager = VoiceChatManager(context)

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

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
                        // In this simplified version we just echo back
                        voiceChatManager.speak(state.text)
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
        // Model always "ready" in this version (no LiteRT engine needed for basic voice)
        _downloadState.value = DownloadState.Success(context.filesDir)
    }

    fun startDownload() {
        viewModelScope.launch {
            modelDownloader.downloadModel().collectLatest { state ->
                _downloadState.value = state
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

    override fun onCleared() {
        super.onCleared()
        voiceChatManager.release()
    }

    // Factory to create VoiceViewModel with context
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return VoiceViewModel(context.applicationContext) as T
        }
    }
}

sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object Listening : VoiceUiState()
    object Generating : VoiceUiState()
    object Speaking : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}
