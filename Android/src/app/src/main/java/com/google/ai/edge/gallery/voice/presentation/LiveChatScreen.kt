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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
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

@Composable
fun VoiceAppScreen() {
    val context = LocalContext.current
    val viewModel: VoiceViewModel = viewModel(factory = VoiceViewModel.Factory(context))

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

    if (hasMicPermission) {
        LiveChatScreen(viewModel)
    } else {
        // Show permission request UI
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
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Permissão de Microfone",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "O Kabem Voice precisa acessar o microfone para ouvir você.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Conceder Permissão")
            }
        }
    }
}

@Composable
fun LiveChatScreen(viewModel: VoiceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Status label
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = when (uiState) {
                is VoiceUiState.Idle -> "Toque para falar"
                is VoiceUiState.Listening -> "Ouvindo..."
                is VoiceUiState.Generating -> "Processando..."
                is VoiceUiState.Speaking -> "Kabem está falando..."
                is VoiceUiState.Error -> (uiState as VoiceUiState.Error).message
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (uiState is VoiceUiState.Error)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )

        // Middle: Recognized text display
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (recognizedText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = recognizedText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom: Animated mic button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Pulse animation when active
            if (uiState is VoiceUiState.Listening || uiState is VoiceUiState.Speaking) {
                WaveformAnimation(isListening = uiState is VoiceUiState.Listening)
            }

            // PTT Button
            val isActive = uiState is VoiceUiState.Listening
            val buttonColor = if (isActive)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary

            val isClickable = uiState is VoiceUiState.Idle ||
                    uiState is VoiceUiState.Listening ||
                    uiState is VoiceUiState.Speaking

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(buttonColor, CircleShape)
                    .then(
                        if (isClickable) Modifier.clickable { viewModel.toggleListening() }
                        else Modifier
                    ),
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun WaveformAnimation(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val primaryScale by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val secondaryScale by infiniteTransition.animateFloat(
        initialValue = 1.3f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
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
                .scale(secondaryScale)
                .background(color.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(primaryScale)
                .background(color.copy(alpha = 0.25f), CircleShape)
        )
    }
}
