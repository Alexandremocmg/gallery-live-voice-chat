package com.google.ai.edge.gallery.voice.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.common.decodeSampledBitmapFromUri
import com.google.ai.edge.gallery.voice.data.MemoryCandidate
import com.google.ai.edge.gallery.voice.data.MemoryCategory
import com.google.ai.edge.gallery.voice.data.MemoryItem
import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import com.google.ai.edge.gallery.voice.language.EnglishActivity
import com.google.ai.edge.gallery.voice.language.LanguagePackStatus
import com.google.ai.edge.gallery.voice.intelligence.CognitiveMode
import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAppScreen(modelManagerViewModel: ModelManagerViewModel, onBackClicked: () -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: VoiceViewModel = viewModel(
        factory = VoiceViewModel.Factory(context, modelManagerViewModel)
    )
    var showSessions by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }

    // Request RECORD_AUDIO permission on entry
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) {
            Log.w("VoiceAppScreen", "Microphone permission denied")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(com.google.ai.edge.gallery.R.drawable.kabem_logo),
                            contentDescription = "Kabem Voice",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Kabem Voice")
                            Text(
                                text = viewModel.activeSessionTitle.collectAsState().value,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showSessions = true }) {
                        Icon(Icons.Default.History, contentDescription = "Abrir sessoes")
                    }
                    IconButton(onClick = { showMemory = true }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Minha memoria")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasMicPermission) {
                val uiState by viewModel.uiState.collectAsState()

                when (val state = uiState) {
                    is VoiceUiState.NoModel -> {
                        NoModelScreen(onBackClicked)
                    }
                    is VoiceUiState.Loading -> {
                        LoadingScreen(state.message)
                    }
                    is VoiceUiState.Error -> {
                        ErrorScreen(state.message, onBackClicked)
                    }
                    else -> {
                        LiveChatScreen(viewModel)
                    }
                }
            } else {
                PermissionRequestScreen {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            if (showSessions) {
                SessionListSheet(
                    viewModel = viewModel,
                    onDismiss = { showSessions = false },
                )
            }
            if (showMemory) {
                MemorySheet(
                    viewModel = viewModel,
                    onDismiss = { showMemory = false },
                )
            }
        }
    }
}

@Composable
fun LiveChatScreen(viewModel: VoiceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val conversationMessages by viewModel.conversationMessages.collectAsState()
    val responseProfile by viewModel.responseProfile.collectAsState()
    val cognitiveMode by viewModel.cognitiveMode.collectAsState()
    val activeSkill by viewModel.activeSkill.collectAsState()
    val connectivityMode by viewModel.connectivityMode.collectAsState()
    val englishLessonState by viewModel.englishLessonState.collectAsState()
    val activeSpeechLocale by viewModel.activeSpeechLocale.collectAsState()
    val voiceNotice by viewModel.voiceNotice.collectAsState()
    val speechCapabilities by viewModel.speechCapabilities.collectAsState()
    val attachedImages by viewModel.attachedImages.collectAsState()
    val attachedAudioName by viewModel.attachedAudioName.collectAsState()
    val imageSupport by viewModel.imageSupport.collectAsState()
    val pdfDocument by viewModel.pdfDocument.collectAsState()
    val pdfLoading by viewModel.pdfLoading.collectAsState()
    val pdfError by viewModel.pdfError.collectAsState()
    val pendingMemory by viewModel.pendingMemory.collectAsState()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val bitmaps = uris.mapNotNull { uri ->
                    decodeSampledBitmapFromUri(context, uri, 1024, 1024)
                }
                viewModel.attachImages(bitmaps)
            }
        }
    }
    var showCameraPreview by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    val isReviewingTranscript = uiState is VoiceUiState.ReviewingTranscript
    LaunchedEffect(isReviewingTranscript) {
        if (isReviewingTranscript) draftText = recognizedText
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCameraPreview = true
    }
    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                Log.w("LiveChatScreen", "PDF provider nao ofereceu permissao persistente", e)
            }
            viewModel.loadPdf(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "Documento PDF")
        }
    }
    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.attachAudio(
                uri = it,
                displayName = it.lastPathSegment?.substringAfterLast('/') ?: "Audio anexado",
            )
        }
    }

    if (showCameraPreview) {
        KabemCameraDialog(
            onDismiss = { showCameraPreview = false },
            onCaptured = { bitmap ->
                viewModel.attachImage(bitmap)
                showCameraPreview = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status indicator
        Text(
            text = when (uiState) {
                is VoiceUiState.Idle -> "Pronto para conversar"
                is VoiceUiState.Listening -> "Estou ouvindo..."
                is VoiceUiState.Generating -> "Pensando..."
                is VoiceUiState.Speaking -> "Falando..."
                else -> ""
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (uiState is VoiceUiState.Listening)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(responseProfile.label) },
                leadingIcon = {
                    Text(
                        text = when (responseProfile) {
                            ResponseDepthProfile.FLASH -> "F"
                            ResponseDepthProfile.DETAILED -> "D"
                            ResponseDepthProfile.STEP_BY_STEP -> "P"
                            ResponseDepthProfile.STRATEGIC -> "E"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (englishLessonState.activity == EnglishActivity.REPEAT) {
                            "Repeticao"
                        } else {
                            activeSpeechLocale.shortLabel
                        }
                    )
                },
            )
            if (cognitiveMode == CognitiveMode.DEEP) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Analise profunda") },
                )
            }
            activeSkill?.let { skill ->
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(skill.name) },
                )
            }
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (connectivityMode == ConnectivityMode.PRIVATE_OFFLINE) {
                            "Privado"
                        } else {
                            "Conectado"
                        }
                    )
                },
            )
        }

        if (!voiceNotice.isNullOrBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = voiceNotice.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (englishLessonState.active) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                            }
                        },
                    ) { Text("Configurar vozes offline") }
                }
            }
        }

        val englishCapability = speechCapabilities[englishLessonState.dialect]
        if (englishLessonState.active &&
            englishCapability?.languagePackStatus == LanguagePackStatus.DOWNLOAD_AVAILABLE
        ) {
            OutlinedButton(onClick = viewModel::requestEnglishLanguagePack) {
                Text("Baixar reconhecimento de ingles")
            }
        }

        if (pdfDocument != null || pdfLoading || pdfError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                pdfLoading -> "Lendo PDF..."
                                pdfDocument != null -> pdfDocument!!.displayName
                                else -> "Falha ao ler PDF"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        if (pdfDocument != null) {
                            Text(
                                text = buildString {
                                    append("${pdfDocument!!.pageCount} paginas para estudo")
                                    if (pdfDocument!!.scannedPageCount > 0) {
                                        append(" · ${pdfDocument!!.scannedPageCount} processadas por visao local")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (pdfError != null) {
                            Text(
                                text = pdfError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(onClick = viewModel::clearPdf, enabled = !pdfLoading) {
                        Icon(Icons.Default.Close, contentDescription = "Remover PDF")
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                enabled = !pdfLoading,
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir PDF para estudar")
            }
        }

        // Conversation panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (attachedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    attachedImages.forEachIndexed { index, bitmap ->
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Imagem anexada ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                TextButton(onClick = viewModel::clearAttachedImage) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remover imagens")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            attachedAudioName?.let { audioName ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(audioName, modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = viewModel::clearAttachedAudio) {
                            Icon(Icons.Default.Close, contentDescription = "Remover audio")
                        }
                    }
                }
            }

            if (conversationMessages.isNotEmpty()) {
                VoiceConversationTimeline(
                    messages = conversationMessages,
                    onSpeakAgain = { message -> viewModel.speakMessage(message.text) },
                    onEditAndResend = { message, text -> viewModel.editAndResendMessage(message.position, text) },
                    onRegenerate = { message -> viewModel.regenerateMessage(message.position) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else if (recognizedText.isNotBlank()) {
                Text(
                    text = "Você disse:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (conversationMessages.isEmpty() && lastResponse.isNotBlank() && uiState !is VoiceUiState.Generating) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Kabem respondeu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastResponse,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            pendingMemory?.let { candidate ->
                Spacer(modifier = Modifier.height(20.dp))
                MemoryConfirmationCard(
                    candidate = candidate,
                    onConfirm = viewModel::confirmPendingMemory,
                    onReject = viewModel::rejectPendingMemory,
                )
            }
        }

        // Bottom control section with waveform pulse and mic button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Ripple animation
            if (uiState is VoiceUiState.Listening || uiState is VoiceUiState.Speaking) {
                WaveformAnimation(isListening = uiState is VoiceUiState.Listening)
            }

            val isActive = uiState is VoiceUiState.Listening
            val buttonColor = if (isActive)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary

            if (showTextInput || isReviewingTranscript) {
                ConversationInputBar(
                    text = draftText,
                    onTextChanged = { draftText = it },
                    onSend = {
                        if (isReviewingTranscript) viewModel.submitReviewedTranscript(draftText)
                        else viewModel.submitText(draftText)
                        draftText = ""
                        showTextInput = false
                    },
                    onCancel = {
                        if (isReviewingTranscript) viewModel.discardTranscript()
                        draftText = ""
                        showTextInput = false
                    },
                    onSwitchToVoice = {
                        draftText = ""
                        showTextInput = false
                    },
                    reviewMode = isReviewingTranscript,
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(buttonColor, CircleShape)
                        .clickable { viewModel.toggleListening() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isActive) "Parar" else "Falar",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.prepareForMediaAttachment()
                            showTextInput = true
                        },
                        enabled = uiState is VoiceUiState.Idle || uiState is VoiceUiState.Listening,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            Icons.Default.Keyboard,
                            contentDescription = "Usar teclado",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.prepareForMediaAttachment()
                            pickImage.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        enabled = imageSupport &&
                            (uiState is VoiceUiState.Idle || uiState is VoiceUiState.Listening),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Escolher imagens",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.prepareForMediaAttachment()
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                showCameraPreview = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = imageSupport &&
                            (uiState is VoiceUiState.Idle || uiState is VoiceUiState.Listening),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Tirar foto",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.prepareForMediaAttachment()
                            pickAudio.launch(arrayOf("audio/*"))
                        },
                        enabled = uiState is VoiceUiState.Idle || uiState is VoiceUiState.Listening,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = "Anexar audio",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KabemCameraDialog(
    onDismiss: () -> Unit,
    onCaptured: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener(
            {
                runCatching {
                    provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider?.unbindAll()
                    provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                }.onFailure { error ->
                    Log.e("KabemCamera", "Failed to open CameraX preview", error)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose { provider?.unbindAll() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                ) {
                    Text(
                        "Camera ativa",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar camera")
                }
                FilledIconButton(
                    onClick = {
                        previewView.bitmap?.copy(Bitmap.Config.ARGB_8888, false)?.let(onCaptured)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).size(72.dp),
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Capturar", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MemoryConfirmationCard(
    candidate: MemoryCandidate,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Guardar na memoria?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${candidate.category.label}: ${candidate.value}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm) { Text("Guardar") }
                TextButton(onClick = onReject) { Text("Nao guardar") }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MemorySheet(viewModel: VoiceViewModel, onDismiss: () -> Unit) {
    val memories by viewModel.memories.collectAsState()
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()
    var editingMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var editText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Minha memoria", style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = memoryEnabled,
                    onCheckedChange = viewModel::setMemoryEnabled,
                )
            }
            Text(
                text = if (memoryEnabled) "Memorias confirmadas ficam somente neste celular."
                else "A memoria esta desativada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showClearDialog = true }, enabled = memories.isNotEmpty()) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apagar tudo")
            }
            if (memories.isEmpty()) {
                Text(
                    "Nenhuma memoria confirmada ainda.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                memories.groupBy { it.category }.forEach { (category, categoryMemories) ->
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                    categoryMemories.forEach { memory ->
                        ListItem(
                            headlineContent = { Text(memory.value, maxLines = 3) },
                            supportingContent = {
                                if (memory.sensitive) Text("Informacao sensivel")
                            },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingMemory = memory
                                            editText = memory.value
                                        },
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar memoria")
                                    }
                                    IconButton(onClick = { viewModel.deleteMemory(memory.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir memoria")
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    editingMemory?.let { memory ->
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("Editar memoria") },
            text = {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = false,
                    label = { Text(memory.category.label) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemory(memory.id, editText)
                        editingMemory = null
                    },
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) { Text("Cancelar") }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Apagar toda a memoria?") },
            text = { Text("Essa acao nao pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMemories()
                        showClearDialog = false
                    },
                ) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SessionListSheet(viewModel: VoiceViewModel, onDismiss: () -> Unit) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    var renameSessionId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sessoes locais", style = MaterialTheme.typography.titleLarge)
                FilledTonalIconButton(
                    onClick = {
                        viewModel.startNewSession()
                        onDismiss()
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova sessao")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (sessions.isEmpty()) {
                Text(
                    "As conversas serao salvas aqui automaticamente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    items(sessions, key = { it.id }) { session ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.resumeSession(session.id)
                                onDismiss()
                            },
                        headlineContent = {
                            Text(
                                text = session.title,
                                maxLines = 1,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = buildString {
                                    append("${session.messageCount} mensagens")
                                    if (session.pdfName.isNotBlank()) append(" • ${session.pdfName}")
                                },
                                maxLines = 1,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (session.id == activeSessionId) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "Sessao ativa",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        renameSessionId = session.id
                                        renameText = session.title
                                    },
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Renomear sessao")
                                }
                                IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir sessao")
                                }
                            }
                        },
                    )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    renameSessionId?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { renameSessionId = null },
            title = { Text("Renomear sessao") },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Nome") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameSession(sessionId, renameText)
                        renameSessionId = null
                    },
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameSessionId = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
fun WaveformAnimation(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.3f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    val color = if (isListening)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale2)
                .background(color.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale1)
                .background(color.copy(alpha = 0.22f), CircleShape)
        )
    }
}

@Composable
fun NoModelScreen(onBackClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Nenhum modelo baixado",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O Kabem Voice roda localmente no seu celular. Para começar, volte para a tela inicial, selecione a categoria 'AI Chat' e baixe pelo menos um modelo (ex: Gemma 2B).",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackClicked) {
            Text("Ir para Tela Inicial")
        }
    }
}

@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorScreen(message: String, onBackClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Erro no Carregamento",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackClicked) {
            Text("Voltar")
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permissão de Microfone Requerida",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O Kabem Voice precisa acessar o microfone para ouvir você em tempo real.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequest) {
            Text("Conceder Permissão")
        }
    }
}
