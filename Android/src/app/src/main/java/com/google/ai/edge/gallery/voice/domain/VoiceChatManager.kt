package com.google.ai.edge.gallery.voice.domain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.google.ai.edge.gallery.data.TtsVoiceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class VoiceChatManager(
    private val context: Context,
    initialVoiceMode: TtsVoiceMode = TtsVoiceMode.NATURAL,
) : RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceChatManager"
        private const val GOOGLE_SPEECH_SERVICES_PACKAGE = "com.google.android.tts"
        private val PT_BR = Locale("pt", "BR")
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var offlineVoiceAvailable = false
    private var voiceMode = initialVoiceMode
    private val pendingUtterances = AtomicInteger(0)

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        } else {
            Log.e("VoiceChatManager", "Speech recognition is not available on this device.")
        }
    }

    private fun initTextToSpeech() {
        val googleSpeechServicesInstalled = try {
            context.packageManager.getApplicationInfo(GOOGLE_SPEECH_SERVICES_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }

        textToSpeech = if (googleSpeechServicesInstalled) {
            Log.d(TAG, "Using Google Speech Services TTS engine")
            TextToSpeech(context, this, GOOGLE_SPEECH_SERVICES_PACKAGE)
        } else {
            Log.w(TAG, "Google Speech Services not installed; using the device default TTS engine")
            TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            val result = textToSpeech?.setLanguage(PT_BR)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Portuguese (Brazil) is not supported for TTS on this device.")
            }

            offlineVoiceAvailable = applyBestLocalVoice()
            applyVoiceMode()

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speechState.value = SpeechState.Speaking
                }

                override fun onDone(utteranceId: String?) {
                    if (pendingUtterances.decrementAndGet().coerceAtLeast(0) == 0) {
                        pendingUtterances.set(0)
                        _speechState.value = SpeechState.Idle
                    }
                }

                @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                override fun onError(utteranceId: String?) {
                    pendingUtterances.set(0)
                    _speechState.value = SpeechState.Idle
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e("VoiceChatManager", "TTS Error: $errorCode")
                    pendingUtterances.set(0)
                    _speechState.value = SpeechState.Idle
                }
            })
        } else {
            Log.e(TAG, "Initialization of TTS failed.")
        }
    }

    fun setVoiceMode(mode: TtsVoiceMode) {
        voiceMode = mode
        if (ttsInitialized) {
            applyVoiceMode()
        }
    }

    private fun applyVoiceMode() {
        textToSpeech?.setSpeechRate(voiceMode.speechRate)
        textToSpeech?.setPitch(1.0f)
    }

    private fun applyBestLocalVoice(): Boolean {
        val engine = textToSpeech ?: return false
        val localPortugueseVoices = engine.voices
            .orEmpty()
            .filter { voice ->
                voice.locale.language == PT_BR.language &&
                    !voice.isNetworkConnectionRequired
            }

        val bestVoice = localPortugueseVoices.maxWithOrNull(
            compareBy<Voice> { if (it.locale.country == PT_BR.country) 1 else 0 }
                .thenBy { it.quality }
                .thenBy { -it.latency },
        )

        if (bestVoice != null) {
            val result = engine.setVoice(bestVoice)
            Log.d(
                TAG,
                "Selected local voice '${bestVoice.name}' quality=${bestVoice.quality} " +
                "latency=${bestVoice.latency} result=$result",
            )
            return result == TextToSpeech.SUCCESS
        } else {
            Log.w(TAG, "No local pt-BR voice found; keeping the engine language default")
            return false
        }
    }

    fun startListening() {
        _speechState.value = SpeechState.Listening
        _recognizedText.value = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        if (text.isBlank()) return

        val engine = textToSpeech
        if (!ttsInitialized || engine == null) {
            Log.e(TAG, "Speech skipped because the TTS engine is not initialized")
            _speechState.value = SpeechState.Idle
            return
        }

        if (flushQueue) {
            pendingUtterances.set(0)
        }
        val utteranceId = "${System.nanoTime()}"
        pendingUtterances.incrementAndGet()
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = engine.speak(text, queueMode, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            pendingUtterances.set(0)
            _speechState.value = SpeechState.Idle
            Log.e(TAG, "TTS rejected the utterance")
        } else if (!offlineVoiceAvailable) {
            Log.w(TAG, "No local pt-BR voice; using the engine default voice as fallback")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        pendingUtterances.set(0)
        _speechState.value = SpeechState.Idle
    }

    fun release() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _speechState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        Log.e("VoiceChatManager", "SpeechRecognizer error: $errorMessage")
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            recreateSpeechRecognizer()
        }
        _speechState.value = SpeechState.Error(errorMessage)
    }

    private fun recreateSpeechRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _recognizedText.value = matches[0]
            _speechState.value = SpeechState.ResultReady(matches[0])
        } else {
            _speechState.value = SpeechState.Idle
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _recognizedText.value = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object Processing : SpeechState()
    data class ResultReady(val text: String) : SpeechState()
    object Speaking : SpeechState()
    data class Error(val message: String) : SpeechState()
}
