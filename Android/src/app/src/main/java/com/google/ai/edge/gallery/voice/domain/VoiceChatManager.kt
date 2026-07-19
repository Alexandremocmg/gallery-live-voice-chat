package com.google.ai.edge.gallery.voice.domain

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import com.google.ai.edge.gallery.voice.language.LanguageConfidence
import com.google.ai.edge.gallery.voice.language.LanguageSwitchingSensitivity
import com.google.ai.edge.gallery.voice.language.RecognizedWord
import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import com.google.ai.edge.gallery.voice.language.SpeechBackendPolicy
import com.google.ai.edge.gallery.voice.language.SpeechCapability
import com.google.ai.edge.gallery.voice.language.SpeechChunk
import com.google.ai.edge.gallery.voice.language.SpeechChunkQueue
import com.google.ai.edge.gallery.voice.language.SpeechLanguageDetection
import com.google.ai.edge.gallery.voice.language.SpeechLocale
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionErrorRecoveryPolicy
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionRequest
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionResult
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionRetryPolicy
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionRuntimeError
import com.google.ai.edge.gallery.voice.language.SpeechRecognitionTransientError
import com.google.ai.edge.gallery.voice.language.TtsPlaybackErrorRecoveryPolicy
import com.google.ai.edge.gallery.voice.language.TtsPlaybackRuntimeError
import com.google.ai.edge.gallery.voice.language.TtsQueuePolicy
import com.google.ai.edge.gallery.voice.language.TtsVoiceSafetyPolicy
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
    private var activeRecognitionRequest = SpeechRecognitionRequest.single(SpeechLocale.PT_BR)
    @Volatile private var activeLanguageDetection: SpeechLanguageDetection? = null
    @Volatile private var recognitionSessionActive = false
    @Volatile private var discardCurrentRecognitionResult = false
    @Volatile private var speechStartedInSession = false
    @Volatile private var manualStopRequested = false
    private var recognitionRetryAttempt = 0
    private var recognitionSessionStartedAtMs = 0L
    private var recognitionSessionId = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val localVoices = mutableMapOf<SpeechLocale, Voice>()
    private val ttsQueue = SpeechChunkQueue()
    private val ttsQueueLock = Any()
    private var activeUtteranceId: String? = null
    private var activeQueuedSpeech: SpeechChunk? = null
    private var activeRangeStart = 0
    private var lastCompletedSpeech: SpeechChunk? = null
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

    private val _localTtsAvailabilityKnown = MutableStateFlow(false)
    val localTtsAvailabilityKnown: StateFlow<Boolean> = _localTtsAvailabilityKnown

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
            _localTtsAvailabilityKnown.value = true
            applyVoiceMode()

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val callbackBelongsToActiveUtterance = synchronized(ttsQueueLock) {
                        utteranceId != null && utteranceId == activeUtteranceId
                    }
                    val mayPublish =
                        SpeechStatePublicationPolicy.shouldPublishTtsState(
                            recognitionSessionActive = recognitionSessionActive,
                            callbackBelongsToActiveUtterance = callbackBelongsToActiveUtterance,
                        )
                    if (!mayPublish) return
                    _speechState.value = SpeechState.Speaking
                    if (onVoiceBargeIn != null) {
                        val activeChunk = synchronized(ttsQueueLock) { activeQueuedSpeech }
                        val bargeInConfig = activeChunk
                            ?.let(BargeInSensitivityPolicy::configFor)
                            ?: BargeInSensitivityPolicy.BALANCED
                        bargeInDetector.start(
                            scope = audioMonitorScope,
                            config = bargeInConfig,
                        ) {
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
            synchronized(ttsQueueLock) {
                if (ttsQueue.isNotEmpty() && activeUtteranceId == null) {
                    speakNextQueuedChunk()
                }
            }
        } else {
            Log.e(TAG, "Initialization of TTS failed.")
            publishTtsCapabilities()
            _localTtsAvailabilityKnown.value = true
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
        val installedVoices = engine.voices.orEmpty()
        SpeechLocale.entries.forEach { locale ->
            val bestVoice = installedVoices
                .filter { voice ->
                    TtsVoiceSafetyPolicy.isCompatibleLocalVoice(
                        requestedLocale = locale,
                        candidateLanguageTag = voice.locale.toLanguageTag(),
                        networkConnectionRequired = voice.isNetworkConnectionRequired,
                    )
                }
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

    fun startListening(request: SpeechRecognitionRequest) {
        startListeningInternal(request, resetRetry = true)
    }

    private fun startListeningInternal(
        request: SpeechRecognitionRequest,
        resetRetry: Boolean,
    ) {
        if (resetRetry) recognitionRetryAttempt = 0
        // ASR and TTS cannot share the microphone/speech state safely. A new recognition
        // session always owns the audio channel and cancels stale playback callbacks.
        stopSpeaking()
        discardCurrentRecognitionResult = false
        activeLanguageDetection = null
        recognitionSessionActive = false
        speechStartedInSession = false
        manualStopRequested = false
        recognitionSessionStartedAtMs = SystemClock.elapsedRealtime()
        recognitionSessionId += 1
        val localPackConfirmed =
            _speechCapabilities.value[request.primaryLocale]?.languagePackStatus ==
                LanguagePackStatus.INSTALLED
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
        activeListeningLocale = request.primaryLocale
        activeRecognitionRequest = request
        recognitionSessionActive = true
        _speechState.value = SpeechState.Listening
        _recognizedText.value = ""
        Log.d(
            TAG,
            "Recognition request #$recognitionSessionId attempt=$recognitionRetryAttempt: " +
                "primary=${request.primaryLocale.languageTag}, " +
                "detection=${request.detectionEnabled}, switching=${request.switchingEnabled}",
        )
        try {
            speechRecognizer?.startListening(buildRecognitionIntent(request))
        } catch (e: Exception) {
            clearRecognitionSession()
            Log.e(TAG, "Failed to start speech recognition", e)
            _speechState.value = SpeechState.Error("Nao foi possivel iniciar o reconhecimento de voz")
        }
    }

    private fun buildRecognitionIntent(request: SpeechRecognitionRequest): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, request.primaryLocale.languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                request.possiblyCompleteSilenceMs.toInt(),
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                request.completeSilenceMs.toInt(),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                putExtra(RecognizerIntent.EXTRA_REQUEST_WORD_CONFIDENCE, true)
                putExtra(RecognizerIntent.EXTRA_REQUEST_WORD_TIMING, true)
                if (request.detectionEnabled) {
                    putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                    putStringArrayListExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
                        ArrayList(request.allowedLocales.map(SpeechLocale::languageTag)),
                    )
                }
                if (request.switchingEnabled) {
                    putExtra(
                        RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                        request.switchingSensitivity.toRecognizerValue(),
                    )
                    putStringArrayListExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                        ArrayList(request.allowedLocales.map(SpeechLocale::languageTag)),
                    )
                }
            }
        }
    }

    fun refreshSpeechReadiness() {
        if (ttsInitialized) {
            catalogLocalVoices()
            _localTtsAvailabilityKnown.value = true
        }
        refreshLanguageCapabilities()
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
                SpeechCapability(
                    locale = locale,
                    backend = recognitionBackend,
                    languagePackStatus = status,
                    localTtsAvailable = localVoices.containsKey(locale),
                )
            }
            return
        }

        SpeechLocale.entries.forEach { locale ->
            val intent = buildRecognitionIntent(SpeechRecognitionRequest.single(locale))
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
        speechRecognizer?.triggerModelDownload(
            buildRecognitionIntent(SpeechRecognitionRequest.single(locale))
        )
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
        val capability =
            SpeechCapability(
                locale = locale,
                backend = recognitionBackend,
                languagePackStatus = status,
                localTtsAvailable = localVoices.containsKey(locale),
            )
        _speechCapabilities.value = _speechCapabilities.value + (locale to capability)
        Log.d(
            TAG,
            "Speech capability: locale=${locale.languageTag}, backend=$recognitionBackend, " +
                "pack=$status, localTts=${capability.localTtsAvailable}",
        )
    }

    private fun updateLocalTtsCapability(
        locale: SpeechLocale,
        localTtsAvailable: Boolean,
    ) {
        val current = _speechCapabilities.value[locale]
        val capability =
            current?.copy(localTtsAvailable = localTtsAvailable)
                ?: SpeechCapability(
                    locale = locale,
                    backend = recognitionBackend,
                    languagePackStatus = LanguagePackStatus.UNKNOWN,
                    localTtsAvailable = localTtsAvailable,
                )
        _speechCapabilities.value = _speechCapabilities.value + (locale to capability)
        Log.d(
            TAG,
            "TTS capability: locale=${locale.languageTag}, localTts=${capability.localTtsAvailable}",
        )
    }

    fun stopListening() {
        mainHandler.removeCallbacksAndMessages(null)
        recognitionRetryAttempt = 0
        manualStopRequested = true
        speechRecognizer?.stopListening()
    }

    fun cancelListening() {
        mainHandler.removeCallbacksAndMessages(null)
        recognitionRetryAttempt = 0
        discardCurrentRecognitionResult = true
        clearRecognitionSession()
        speechRecognizer?.cancel()
        _recognizedText.value = ""
        _speechState.value = SpeechState.Idle
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        speak(SpeechChunk(text = text, locale = SpeechLocale.PT_BR), flushQueue)
    }

    fun speak(chunk: SpeechChunk, flushQueue: Boolean = true) {
        if (chunk.text.isBlank()) return
        if (TtsQueuePolicy.shouldDefer(chunk.text, ttsInitialized)) {
            synchronized(ttsQueueLock) {
                if (flushQueue) {
                    textToSpeech?.stop()
                    ttsQueue.clear()
                    activeUtteranceId = null
                    activeQueuedSpeech = null
                    activeRangeStart = 0
                    _playbackCheckpoint.value = null
                }
                ttsQueue.addLast(chunk)
            }
            _voiceNotice.value = "A fala está sendo preparada; será reproduzida assim que o áudio estiver pronto."
            return
        }
        val engine = textToSpeech
        if (engine == null) {
            Log.e(TAG, "Speech skipped because the TTS engine is unavailable")
            _speechState.value = SpeechState.Error("O mecanismo de fala não está disponível")
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
            ttsQueue.addLast(chunk)
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

        val voice = localVoices[queued.locale]
        if (voice == null ||
            !TtsVoiceSafetyPolicy.isCompatibleLocalVoice(
                requestedLocale = queued.locale,
                candidateLanguageTag = voice.locale.toLanguageTag(),
                networkConnectionRequired = voice.isNetworkConnectionRequired,
            )
        ) {
            Log.e(TAG, "Cannot speak ${queued.locale.languageTag}: no compatible local voice installed")
            applyTtsPlaybackErrorRecovery(
                TtsPlaybackRuntimeError.MISSING_LOCAL_VOICE,
                queued.locale,
            )
            speakNextQueuedChunk()
            return
        }

        val utteranceId = "kabem-${System.nanoTime()}"
        synchronized(ttsQueueLock) {
            activeUtteranceId = utteranceId
            activeQueuedSpeech = queued
            activeRangeStart = 0
        }
        val languageResult = engine.setLanguage(queued.locale.locale)
        val voiceResult = engine.setVoice(voice)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED ||
                voiceResult == TextToSpeech.ERROR
        ) {
            applyTtsPlaybackErrorRecovery(
                TtsPlaybackRuntimeError.INCOMPLETE_LOCAL_VOICE_DATA,
                queued.locale,
            )
            failUtterance(utteranceId, TextToSpeech.ERROR, recoverTtsError = false)
            return
        }
        val speechRate =
            queued.speechRate
                ?: if (queued.locale.isEnglish) 0.91f else voiceMode.speechRate
        engine.setSpeechRate(speechRate)
        engine.setPitch(1.0f)
        Log.d(
            TAG,
            "TTS chunk: locale=${queued.locale.languageTag}, " +
                "chars=${queued.text.length}, rate=$speechRate",
        )
        val result = engine.speak(
            queued.text,
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

    private fun applyTtsPlaybackErrorRecovery(
        error: TtsPlaybackRuntimeError,
        locale: SpeechLocale,
    ) {
        val recovery =
            TtsPlaybackErrorRecoveryPolicy.recover(
                error = error,
                locale = locale,
            ) ?: return
        updateLocalTtsCapability(recovery.locale, recovery.localTtsAvailable)
        _voiceNotice.value = recovery.voiceNotice
    }

    private fun failUtterance(
        utteranceId: String?,
        errorCode: Int,
        recoverTtsError: Boolean = true,
    ) {
        Log.e(TAG, "TTS error $errorCode for $utteranceId")
        val failedLocale: SpeechLocale?
        val hasMore = synchronized(ttsQueueLock) {
            if (utteranceId != null && activeUtteranceId != utteranceId) return
            failedLocale = activeQueuedSpeech?.locale
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            ttsQueue.isNotEmpty()
        }
        if (recoverTtsError) {
            failedLocale?.let { locale ->
                applyTtsPlaybackErrorRecovery(TtsPlaybackRuntimeError.UTTERANCE_REJECTED, locale)
            }
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
            val start = activeRangeStart.coerceIn(0, current.text.length)
            val remaining = current.text.substring(start).trimStart()
            val pending = ttsQueue.snapshot().map { queued ->
                PlaybackChunk(queued.text, queued.locale, queued.speechRate)
            }
            ttsQueue.clear()
            activeUtteranceId = null
            activeQueuedSpeech = null
            activeRangeStart = 0
            SpeechPlaybackCheckpoint(
                remainingText = remaining,
                completeText = current.text,
                locale = current.locale,
                speechRate = current.speechRate,
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
            SpeechChunk(checkpoint.completeText, checkpoint.locale, checkpoint.speechRate)
        } ?: return false
        speak(last, flushQueue = true)
        return true
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        clearRecognitionSession()
        bargeInDetector.release()
        audioMonitorScope.cancel()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _playbackCheckpoint.value = null
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Recognition #$recognitionSessionId ready")
        if (discardCurrentRecognitionResult) return
        _speechState.value = SpeechState.Listening
    }

    override fun onBeginningOfSpeech() {
        speechStartedInSession = true
        Log.d(TAG, "Recognition #$recognitionSessionId beginning_of_speech")
    }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        Log.d(TAG, "Recognition #$recognitionSessionId end_of_speech")
        if (discardCurrentRecognitionResult) return
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        clearRecognitionSession()
        if (discardCurrentRecognitionResult) {
            discardCurrentRecognitionResult = false
            return
        }
        val elapsedMs = (SystemClock.elapsedRealtime() - recognitionSessionStartedAtMs).coerceAtLeast(0L)
        if (!manualStopRequested && retryTransientRecognitionError(error, elapsedMs)) {
            return
        }
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Falha ao acessar o audio do microfone"
            SpeechRecognizer.ERROR_CLIENT -> "O reconhecimento de voz foi interrompido"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissao de microfone ausente"
            SpeechRecognizer.ERROR_NETWORK -> "O servico tentou acessar a rede"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tempo de rede esgotado"
            SpeechRecognizer.ERROR_NO_MATCH -> "Nao consegui entender a fala"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecimento de voz ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Falha interna no servico de reconhecimento"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Servico de reconhecimento desconectado"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala foi detectada"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Muitas tentativas de reconhecimento"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> {
                applyRecognitionErrorRecovery(SpeechRecognitionRuntimeError.LANGUAGE_NOT_SUPPORTED)
                "Idioma nao suportado pelo reconhecimento local"
            }
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                applyRecognitionErrorRecovery(SpeechRecognitionRuntimeError.LANGUAGE_UNAVAILABLE)
                "Pacote offline do idioma ainda nao instalado"
            }
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> {
                applyRecognitionErrorRecovery(SpeechRecognitionRuntimeError.CANNOT_CHECK_SUPPORT)
                "Nao foi possivel verificar o suporte local deste idioma"
            }
            else -> "Nao consegui entender. Tente novamente."
        }
        Log.e("VoiceChatManager", "SpeechRecognizer error: $errorMessage")
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            recreateSpeechRecognizer()
        }
        manualStopRequested = false
        _speechState.value = SpeechState.Error(errorMessage)
    }

    private fun retryTransientRecognitionError(
        error: Int,
        elapsedMs: Long,
    ): Boolean {
        val transientError = error.toTransientRecognitionError() ?: return false
        val decision =
            SpeechRecognitionRetryPolicy.decide(
                error = transientError,
                attempt = recognitionRetryAttempt,
                speechStarted = speechStartedInSession,
                elapsedMs = elapsedMs,
            )
        if (!decision.shouldRetry) return false

        val request = activeRecognitionRequest
        val nextAttempt = decision.nextAttempt
        val previousSessionId = recognitionSessionId
        recognitionRetryAttempt = nextAttempt
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
            error == SpeechRecognizer.ERROR_CLIENT ||
            error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED
        ) {
            recreateSpeechRecognizer()
        }
        Log.w(
            TAG,
            "Recognition #$previousSessionId transient error=$error kind=$transientError " +
                "speechStarted=$speechStartedInSession elapsedMs=$elapsedMs; " +
                "retry=$nextAttempt delayMs=${decision.retryDelayMs}",
        )
        _voiceNotice.value = "Reconhecimento oscilou; vou continuar ouvindo."
        _speechState.value = SpeechState.Listening
        mainHandler.postDelayed(
            {
                startListeningInternal(request, resetRetry = false)
            },
            decision.retryDelayMs,
        )
        return true
    }

    private fun Int.toTransientRecognitionError(): SpeechRecognitionTransientError? =
        when (this) {
            SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionTransientError.CLIENT
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionTransientError.RECOGNIZER_BUSY
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> SpeechRecognitionTransientError.SERVER_DISCONNECTED
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionTransientError.SPEECH_TIMEOUT
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionTransientError.NO_MATCH
            else -> null
        }

    private fun applyRecognitionErrorRecovery(error: SpeechRecognitionRuntimeError) {
        val recovery =
            SpeechRecognitionErrorRecoveryPolicy.recover(
                error = error,
                locale = activeListeningLocale,
            ) ?: return
        updateCapability(activeListeningLocale, recovery.languagePackStatus)
        _voiceNotice.value = recovery.voiceNotice
    }

    private fun recreateSpeechRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        initSpeechRecognizer()
    }

    override fun onResults(results: Bundle?) {
        if (discardCurrentRecognitionResult) {
            discardCurrentRecognitionResult = false
            clearRecognitionSession()
            return
        }
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val languageDetection = activeLanguageDetection
            clearRecognitionSession()
            manualStopRequested = false
            val recognitionResult =
                SpeechRecognitionResult(
                    text = matches[0],
                    alternatives = matches.drop(1),
                    confidenceScores =
                        results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.toList().orEmpty(),
                    words = extractRecognizedWords(results),
                    locale = activeListeningLocale,
                    backend = recognitionBackend,
                    languageDetection = languageDetection,
                )
            _recognizedText.value = recognitionResult.text
            _speechState.value = SpeechState.ResultReady(recognitionResult)
        } else {
            clearRecognitionSession()
            manualStopRequested = false
            _speechState.value = SpeechState.Idle
        }
    }

    override fun onLanguageDetection(results: Bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            discardCurrentRecognitionResult ||
            !recognitionSessionActive ||
            !activeRecognitionRequest.detectionEnabled
        ) {
            return
        }

        val rawLocale = SpeechLocale.fromLanguageTag(results.getString(SpeechRecognizer.DETECTED_LANGUAGE))
        val detectedLocale = rawLocale?.toAllowedLocale(activeRecognitionRequest.allowedLocales)
        val androidConfidence =
            results.getInt(
                SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL,
                SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_UNKNOWN,
            )
        val languageConfidence = androidConfidence.toLanguageConfidence()
        if (detectedLocale != null) {
            activeLanguageDetection =
                SpeechLanguageDetection(
                    locale = detectedLocale,
                    confidence = languageConfidence,
                )
        }

        val switchResult =
            results.getInt(
                SpeechRecognizer.LANGUAGE_SWITCH_RESULT,
                SpeechRecognizer.LANGUAGE_SWITCH_RESULT_NOT_ATTEMPTED,
            )
        Log.d(
            TAG,
            "Language detection: locale=${detectedLocale?.languageTag ?: "unsupported"}, " +
                "confidence=$languageConfidence, switchResult=$switchResult",
        )
        when (switchResult) {
            SpeechRecognizer.LANGUAGE_SWITCH_RESULT_FAILED ->
                _voiceNotice.value =
                    "A troca automatica de idioma falhou; vou manter o idioma principal."
            SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL -> {
                activeRecognitionRequest.allowedLocales
                    .firstOrNull { locale ->
                        _speechCapabilities.value[locale]?.languagePackStatus !=
                            LanguagePackStatus.INSTALLED
                    }
                    ?.let { locale -> updateCapability(locale, LanguagePackStatus.DOWNLOAD_AVAILABLE) }
                _voiceNotice.value =
                    "A troca automatica precisa dos pacotes offline de portugues e ingles."
            }
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

    private fun clearRecognitionSession() {
        recognitionSessionActive = false
        activeLanguageDetection = null
    }
}

private fun LanguageSwitchingSensitivity.toRecognizerValue(): String =
    when (this) {
        LanguageSwitchingSensitivity.BALANCED -> RecognizerIntent.LANGUAGE_SWITCH_BALANCED
        LanguageSwitchingSensitivity.HIGH_PRECISION -> RecognizerIntent.LANGUAGE_SWITCH_HIGH_PRECISION
        LanguageSwitchingSensitivity.QUICK_RESPONSE -> RecognizerIntent.LANGUAGE_SWITCH_QUICK_RESPONSE
    }

private fun Int.toLanguageConfidence(): LanguageConfidence =
    when (this) {
        SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_HIGHLY_CONFIDENT ->
            LanguageConfidence.HIGH
        SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_CONFIDENT ->
            LanguageConfidence.MEDIUM
        else -> LanguageConfidence.LOW
    }

private fun SpeechLocale.toAllowedLocale(allowedLocales: List<SpeechLocale>): SpeechLocale? =
    when {
        this in allowedLocales -> this
        isEnglish -> allowedLocales.firstOrNull(SpeechLocale::isEnglish)
        else -> null
    }

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
