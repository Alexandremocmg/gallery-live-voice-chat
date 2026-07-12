package com.google.ai.edge.gallery.voice.presentation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.edge.gallery.voice.data.DownloadState

@Composable
fun VoiceAppScreen(viewModel: VoiceViewModel = viewModel()) {
    val downloadState by viewModel.downloadState.collectAsState()

    when (downloadState) {
        is DownloadState.Success -> {
            LiveChatScreen(viewModel)
        }
        else -> {
            LoadingScreen(viewModel, downloadState)
        }
    }
}

@Composable
fun LoadingScreen(viewModel: VoiceViewModel, downloadState: DownloadState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Kabem Voice",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        when (downloadState) {
            is DownloadState.Idle -> {
                Text(
                    text = "O modelo de IA (2GB) precisa ser baixado para funcionar offline.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.startDownload() }) {
                    Text("Baixar Modelo (2GB)")
                }
            }
            is DownloadState.Downloading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Baixando modelo...")
            }
            is DownloadState.Error -> {
                Text(
                    text = "Erro: ${(downloadState as DownloadState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.startDownload() }) {
                    Text("Tentar Novamente")
                }
            }
            is DownloadState.Success -> {
                // Handled in VoiceAppScreen
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
        // Top Section: Status
        Text(
            text = when (uiState) {
                is VoiceUiState.Idle -> "Pronto"
                is VoiceUiState.Listening -> "Ouvindo..."
                is VoiceUiState.Generating -> "Pensando..."
                is VoiceUiState.Speaking -> "Falando..."
                is VoiceUiState.Error -> (uiState as VoiceUiState.Error).message
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (uiState is VoiceUiState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        // Middle Section: Recognized Text (Hidden/Subtle)
        Text(
            text = recognizedText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(vertical = 32.dp)
        )

        // Bottom Section: Waveform and PTT Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Waveform animation
            if (uiState is VoiceUiState.Listening || uiState is VoiceUiState.Speaking) {
                WaveformAnimation()
            }

            // Big Push To Talk Button
            val buttonColor = if (uiState is VoiceUiState.Listening) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(buttonColor, CircleShape)
                    .clickable { viewModel.toggleListening() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState is VoiceUiState.Listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Push to Talk",
                    modifier = Modifier.size(48.dp),
                    tint = if (uiState is VoiceUiState.Listening) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun WaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_alpha"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
    )
}
