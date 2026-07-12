package com.google.ai.edge.gallery.voice.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAppScreen(modelManagerViewModel: ModelManagerViewModel, onBackClicked: () -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: VoiceViewModel = viewModel(
        factory = VoiceViewModel.Factory(context, modelManagerViewModel)
    )

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
                title = { Text("Kabem Voice (Ao Vivo)") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
        }
    }
}

@Composable
fun LiveChatScreen(viewModel: VoiceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()

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

        // Conversation panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (recognizedText.isNotBlank()) {
                Text(
                    text = "Você disse:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (lastResponse.isNotBlank() && uiState !is VoiceUiState.Generating) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Kabem respondeu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastResponse,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
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

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(buttonColor, CircleShape)
                    .clickable { viewModel.toggleListening() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isActive) "Parar" else "Falar",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
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
