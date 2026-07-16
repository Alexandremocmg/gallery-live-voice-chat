package com.google.ai.edge.gallery.voice.domain

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionPart
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.google.ai.edge.gallery.data.TtsVoiceMode
import com.google.ai.edge.gallery.voice.language.LanguagePackStatus
import com.google.ai.edge.gallery.voice.language.RecognizedWord
import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import com.google.ai.edge.gallery.voice.language.SpeechBackendPolicy
import com.google.ai.edge.gallery.voice.language.SpeechCapability
import com.google.ai.edge.gallery.voice.language.SpeechChunk
import com.google.ai.edge.gallery.voice.language.SpeechLocale
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionResult
import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.conversation.PlaybackChunk
import com.google.ai.edge.gallery.voice.conversation.SpeechPlaybackCheckpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.Locale

class VoiceChatManager(
    private val context: Context,
    initialVoiceMode: TtsVoiceMode = TtsVoiceMode.NATURAL,
    initialConnectivityMode: ConnectivityMode = ConnectivityMode.PRIVATE_OFFLINE,
) : RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceChatManager"
        private const val GOOGLE_SPEECH_SERVICES_PACKAGE = "com.google.android.tts"
        private val PT_BR = SpeechLocale.PT_BR.locale
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var voiceMode = initialVoiceMode
    private var recognitionBackend = RecognitionBackend.UNAVAILABLE
    private var connectivityMode = initialConnectivityMode
    private var activeListeningLocale = SpeechLocale.PT_BR
    @Volatile private var discardCurrentRecognitionResult = false
    private val localVoices = mutableMapOf<SpeechLocale, Voice>()
    private val ttsQueue = ArrayDeque<QueuedSpeech>()
    private val ttsQueueLock = Any()
    private var activeUtteranceId: String? = null
    private var activeQueuedSpeech: QueuedSpeech? = null
    private var activeRangeStart = 0
    private var lastCompletedSpeech: QueuedSpeech? = null
    private val audioMonitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bargeInDetector = VoiceBargeInDetector()
    private var onVoiceBargeIn: (() -> Unit)? = null

    private val _playbackCheckpoint = MutableStateFlow<SpeechPlaybackCheckpoint?>(null)
    val playbackCheckpoint: StateFlow<SpeechPlaybackCheckpoint?> = _playbackCheckpoint

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _speechCapabilities = MutableStateFlow<Map<SpeechLocale, SpeechCapability>>(emptyMap())
    val speechCapabilities: StateFlow<Map<SpeechLocale, SpeechCapability>> = _speechCapabilities

    private val _voiceNotice = MutableStateFlow<String?>(null)
    val voiceNotice: StateFlow<String?> = _voiceNotice

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        val systemAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val onDeviceAvailable =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        recognitionBackend =
            SpeechBackendPolicy.select(
                apiLevel = Build.VERSION.SDK_INT,
                onDeviceAvailable = onDeviceAvailable,
                systemRecognizerAvailable = systemAvailable,
            )
        if (recognitionBackend == RecognitionBackend.ANDROID_SYSTEM) {
            _voiceNotice.value =
                "Este celular nao confirmou reconhecimento offline. Instale o pacote local do idioma antes de usar em modo aviao."
        }

        speechRecognizer = try {
            when (recognitionBackend) {
                RecognitionBackend.ANDROID_ON_DEVICE ->
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                RecognitionBackend.ANDROID_SYSTEM -> SpeechRecognizer.createSpeechRecognizer(context)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create preferred recognizer; trying system recognizer", e)
            recognitionBackend =
                if (systemAvailable) RecognitionBackend.ANDROID_SYSTEM
                else RecognitionBackend.UNAVAILABLE
            if (recognitionBackend == RecognitionBackend.ANDROID_SYSTEM) {
                _voiceNotice.value =
                    "O reconhecimento local falhou e o servico do sistema sera usado. Confirme o pacote offline antes do modo aviao."
            }
            if (systemAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
        }

        if (speechRecognizer != null) {
            speechRecognizer?.setRecognitionListener(this)
            refreshLanguageCapabilities()
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

            catalogLocalVoices()
            applyVoiceMode()

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speechState.value = SpeechState.Speaking
                    if (onVoiceBargeIn != null) {
                        bargeInDetector.start(audioMonitorScope) {
                            if (interruptSpeaking()) onVoiceBargeIn?.invoke()
                        }
                    }
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    synchronized(ttsQueueLock) {
                        if (utteranceId != null && utteranceId == activeUtteranceId) {
                            activeRangeStart = start.coerceAtLeast(0)
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    bargeInDetector.stop()
                    finishUtterance(utteranceId)
                }

                @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                override fun onError(utteranceId: String?) {
                    failUtterance(utteranceId, TextToSpeech.ERROR)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    bargeInDetector.stop()
                    failUtterance(utteranceId, errorCode)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (!interrupted) finishUtterance(utteranceId)
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

    fun setConnectivityMode(mode: ConnectivityMode) {
        connectivityMode = mode
    }

    fun setOnVoiceBargeIn(listener: (() -> Unit)?) {
        onVoiceBargeIn = listener
        if (listener == null) bargeInDetector.stop()
    }

    private fun applyVoiceMode() {
        textToSpeech?.setSpeechRate(voiceMode.speechRate)
        textToSpeech?.setPitch(1.0f)
    }

    private fun catalogLocalVoices() {
        val engine = textToSpeech ?: return
        localVoices.clear()
        val installedVoices = engine.voices.orEmpty().filterNot { it.isNetworkConnectionRequired }
        SpeechLocale.entries.forEach { locale ->
            val bestVoice = installedVoices
                .filter { it.locale.language.equals(locale.locale.language, ignoreCase = true) }
                .maxWithOrNull(
                    compareBy<Voice> { if (it.locale.country.equals(locale.locale.country, true)) 1 else 0 }
                        .thenBy { it.quality }
                        .thenBy { -it.latency },
                )
            if (bestVoice != null) {
                localVoices[locale] = bestVoice
                Log.d(TAG, "Local ${locale.languageTag} voice: ${bestVoice.name}")
            } else {
                Log.w(TAG, "No local ${locale.languageTag} TTS voice is installed")
            }
        }
        publishTtsCapabilities()
    }

    private fun publishTtsCapabilities() {
        _speechCapabilities.value = SpeechLocale.entries.associateWith { locale ->
            _speechCapabilities.value[locale]?.copy(localTtsAvailable = localVoices.containsKey(locale))
                ?: SpeechCapability(
                    locale = locale,
                    backend = recognitionBackend,
                    languagePackStatus = LanguagePackStatus.UNKNOWN,
                    localTtsAvailable = localVoices.containsKey(locale),
                )
        }
    }

    fun startListening(
        locale: SpeechLocale = SpeechLocale.PT_BR,
        allowBilingualSwitch: Boolean = false,
    ) {
        discardCurrentRecognitionResult = false
        val localPackConfirmed =
            _speechCapabilities.value[locale]?.languagePackStatus == LanguagePackStatus.INSTALLED
        if (
            connectivityMode == ConnectivityMode.PRIVATE_OFFLINE &&
                recognitionBackend != RecognitionBackend.ANDROID_ON_DEVICE &&
                !localPackConfirmed
        ) {
            _speechState.value =
                SpeechState.Error(
                    "Modo Privado: instale o reconhecimento offline deste idioma antes de falar."
                )
            return
        }
        if (speechRecognizer == null) {
            _speechState.value = SpeechState.Error("Reconhecimento de voz local indisponivel")
            return
        }
        activeListeningLocale = locale
        _speechState.value = SpeechState.Listening
        _recognizedText.value = ""
        speechRecognizer?.startListening(buildRecognitionIntent(locale, allowBilingualSwitch))
    }

    private fun buildRecognitionIntent(
        locale: SpeechLocale,
        allowBilingualSwitch: Boolean,
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                putExtra(RecognizerIntent.EXTRA_REQUEST_WORD_CONFIDENCE, true)
                putExtra(RecognizerIntent.EXTRA_REQUEST_WORD_TIMING, true)
                if (allowBilingualSwitch) {
                    putExtra(
                        RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                        RecognizerIntent.LANGUAGE_SWITCH_BALANCED,
                    )
                    putStringArrayListExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                        arrayListOf(SpeechLocale.PT_BR.languageTag, locale.languageTag),
                    )
                }
            }
        }
    }

    fun refreshLanguageCapabilities() {
        val recognizer = speechRecognizer ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val status =
                if (recognitionBackend == RecognitionBackend.ANDROID_ON_DEVICE) {
                    LanguagePackStatus.INSTALLED
                } else {
                    LanguagePackStatus.UNKNOWN
                }
            _speechCapabilities.value = SpeechLocale.entries.associateWith { locale ->
                SpeechCapability(locale, recognitionBackend, status)
            }
            return
        }

        SpeechLocale.entries.forEach { locale ->
            val intent = buildRecognitionIntent(locale, allowBilingualSwitch = false)
            recognizer.checkRecognitionSupport(
                intent,
                context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        val status = recognitionSupport.statusFor(locale)
                        updateCapability(locale, status)
                    }

                    override fun onError(error: Int) {
                        updateCapability(locale, LanguagePackStatus.UNKNOWN)
                    }
                },
            )
        }
    }

    fun requestLanguageModelDownload(locale: SpeechLocale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        speechRecognizer?.triggerModelDownload(buildRecognitionIntent(locale, false))
        updateCapability(locale, LanguagePackStatus.DOWNLOAD_PENDING)
    }

    private fun RecognitionSupport.statusFor(locale: SpeechLocale): LanguagePackStatus {
        fun List<String>.containsLocale(): Boolean =
            any { SpeechLocale.fromLanguageTag(it) == locale }
        return when {
            installedOnDeviceLanguages.containsLocale() -> LanguagePackStatus.INSTALLED
            pendingOnDeviceLanguages.containsLocale() -> LanguagePackStatus.DOWNLOAD_PENDING
            supportedOnDeviceLanguages.containsLocale() -> LanguagePackStatus.DOWNLOAD_AVAILABLE
            else -> LanguagePackStatus.UNSUPPORTED
        }
    }

    private fun updateCapability(locale: SpeechLocale, status: LanguagePackStatus) {
        _speechCapabilities.value = _speechCapabilities.value +
            (locale to SpeechCapability(locale, recognitionBackend, status, localVoices.containsKey(locale)))
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun cancelListening() {
        discardCurrentRecognitionResult = true
        speechRecognizer?.cancel()
        _recognizedText.value = ""
        _speechState.value = SpeechState.Idle
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        speak(SpeechChunk(text = text, locale = SpeechLocale.PT_BR), flushQueue)
    }

    fun speak(chunk: SpeechChunk, flushQueue: Boolean = true) {
        if (chunk.text.isBlank()) return

        val engine = textToSpeech
        if (!ttsInitialized || engine == null) {
            Log.e(TAG, "Speech skipped because the TTS engine is not initialized")
            _speechState.value = SpeechState.Idle
            return
        }

        var shouldStart = false
        synchronized(ttsQueueLock) {
            if (flushQueue) {
                engine.stop()
                ttsQueue.clear()
                activeUtteranceId = null
                activeQueuedSpeech = null
                activeRangeStart = 0
                _playbackCheckpoint.value = null
            }
            ttsQueue.addLast(QueuedSpeech(chunk))
            shouldStart = activeUtteranceId == null
        }
        if (shouldStart) speakNextQueuedChunk()
    }

    private fun speakNextQueuedChunk() {
        val engine = textToSpeech ?: return
        val queued = synchronized(ttsQueueLock) {
            if (activeUtteranceId != null) return
            ttsQueue.removeFirstOrNull()
        } ?: run {
            _speechState.value = SpeechState.Idle
            return
        }

        val voice = localVoices[queued.chunk.locale]
        if (voice == null) {
            Log.e(TAG, "Cannot speak ${queued.chunk.locale.languageTag}: no local voice installed")
            _voiceNotice.value =
                "A voz ${queued.chunk.locale.shortLabel} offline precisa ser instalada nas configuracoes de voz do celular."
            speakNextQueuedChunk()
            return
        }

        val utteranceId = "kabem-${System.nanoTime()}"
        synchronized(ttsQueueLock) {
            activeUtteranceId = utteranceId
            activeQueuedSpeech = queued
            activeRangeStart = 0
        }
        val languageResult = engine.setLanguage(queued.chunk.locale.locale)
        val voiceResult = engine.setVoice(voice)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED ||
                voiceResult == TextToSpeech.ERROR
        ) {
            _voiceNotice.value =
                "A voz ${queued.chunk.locale.shortLabel} local esta incompleta. Instale os dados de voz nas configuracoes do celular."
            failUtterance(utteranceId, TextToSpeech.ERROR)
            return
        }
        engine.setSpeechRate(
            queued.chunk.speechRate
                ?: if (queued.chunk.locale.isEnglish) 0.91f else voiceMode.speechRate,
        )
        engine.setPitch(1.0f)
        val result = engine.speak(
            queued.chunk.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId,
        )
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS rejected the utterance")
            failUtterance(utteranceId, result)
        }
    }

    private fun finishUtterance(utteranceId: String?) {
        val hasMore = synchronized(ttsQueueLock) {
            if (utteranceId == null || activeUtteranceId != utteranceId) return
            lastCompletedSpeech = activeQueuedSpeech
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            ttsQueue.isNotEmpty()
        }
        if (hasMore) speakNextQueuedChunk() else _speechState.value = SpeechState.Idle
    }

    private fun failUtterance(utteranceId: String?, errorCode: Int) {
        Log.e(TAG, "TTS error $errorCode for $utteranceId")
        val hasMore = synchronized(ttsQueueLock) {
            if (utteranceId != null && activeUtteranceId != utteranceId) return
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            ttsQueue.isNotEmpty()
        }
        if (hasMore) speakNextQueuedChunk() else _speechState.value = SpeechState.Idle
    }

    fun stopSpeaking() {
        bargeInDetector.stop()
        synchronized(ttsQueueLock) {
            ttsQueue.clear()
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            _playbackCheckpoint.value = null
        }
        textToSpeech?.stop()
        _speechState.value = SpeechState.Idle
    }

    fun interruptSpeaking(): Boolean {
        bargeInDetector.stop()
        val checkpoint = synchronized(ttsQueueLock) {
            val current = activeQueuedSpeech ?: return false
            val start = activeRangeStart.coerceIn(0, current.chunk.text.length)
            val remaining = current.chunk.text.substring(start).trimStart()
            val pending = ttsQueue.map { queued ->
                PlaybackChunk(queued.chunk.text, queued.chunk.locale, queued.chunk.speechRate)
            }
            ttsQueue.clear()
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            SpeechPlaybackCheckpoint(
                remainingText = remaining,
                completeText = current.chunk.text,
                locale = current.chunk.locale,
                speechRate = current.chunk.speechRate,
                pendingChunks = pending,
            )
        }
        _playbackCheckpoint.value = checkpoint
        textToSpeech?.stop()
        _speechState.value = SpeechState.Idle
        return true
    }

    fun resumeInterrupted(): Boolean {
        val checkpoint = _playbackCheckpoint.value ?: return false
        _playbackCheckpoint.value = null
        if (checkpoint.remainingText.isNotBlank()) {
            speak(
                SpeechChunk(checkpoint.remainingText, checkpoint.locale, checkpoint.speechRate),
                flushQueue = true,
            )
        }
        checkpoint.pendingChunks.forEach { pending ->
            speak(SpeechChunk(pending.text, pending.locale, pending.speechRate), flushQueue = false)
        }
        return checkpoint.remainingText.isNotBlank() || checkpoint.pendingChunks.isNotEmpty()
    }

    fun repeatLastComplete(): Boolean {
        val last = lastCompletedSpeech ?: _playbackCheckpoint.value?.let { checkpoint ->
            QueuedSpeech(SpeechChunk(checkpoint.completeText, checkpoint.locale, checkpoint.speechRate))
        } ?: return false
        speak(last.chunk, flushQueue = true)
        return true
    }

    fun release() {
        bargeInDetector.release()
        audioMonitorScope.cancel()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _playbackCheckpoint.value = null
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        if (discardCurrentRecognitionResult) return
        _speechState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        if (discardCurrentRecognitionResult) return
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        if (discardCurrentRecognitionResult) {
            discardCurrentRecognitionResult = false
            _speechState.value = SpeechState.Idle
            return
        }
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
        initSpeechRecognizer()
    }

    override fun onResults(results: Bundle?) {
        if (discardCurrentRecognitionResult) {
            discardCurrentRecognitionResult = false
            _speechState.value = SpeechState.Idle
            return
        }
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val detectedLocale =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SpeechLocale.fromLanguageTag(results.getString(SpeechRecognizer.DETECTED_LANGUAGE))
                } else {
                    null
                }
            val recognitionResult =
                SpeechRecognitionResult(
                    text = matches[0],
                    alternatives = matches.drop(1),
                    confidenceScores =
                        results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.toList().orEmpty(),
                    words = extractRecognizedWords(results),
                    locale = detectedLocale ?: activeListeningLocale,
                    backend = recognitionBackend,
                )
            _recognizedText.value = recognitionResult.text
            _speechState.value = SpeechState.ResultReady(recognitionResult)
        } else {
            _speechState.value = SpeechState.Idle
        }
    }

    @Suppress("DEPRECATION")
    private fun extractRecognizedWords(results: Bundle): List<RecognizedWord> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return emptyList()
        val parts = results.getParcelableArrayList<RecognitionPart>(SpeechRecognizer.RECOGNITION_PARTS)
            .orEmpty()
        return parts.map { part ->
            RecognizedWord(
                text = part.rawText,
                confidenceLevel = part.confidenceLevel,
                timestampMs = part.timestampMillis,
            )
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        if (discardCurrentRecognitionResult) return
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _recognizedText.value = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

private data class QueuedSpeech(val chunk: SpeechChunk)

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object Processing : SpeechState()
    data class ResultReady(val result: SpeechRecognitionResult) : SpeechState() {
        val text: String
            get() = result.text
    }
    object Speaking : SpeechState()
    data class Error(val message: String) : SpeechState()
}
