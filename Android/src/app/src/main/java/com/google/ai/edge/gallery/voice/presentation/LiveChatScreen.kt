package com.google.ai.edge.gallery.voice.presentation

import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VoiceAppScreen() {
    val context = LocalContext.current
    val viewModel: VoiceViewModel = viewModel(factory = VoiceViewModel.Factory(context))
    LiveChatScreen(viewModel)
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
                is VoiceUiState.Idle -> "Pronto para ouvir"
                is VoiceUiState.Listening -> "Ouvindo..."
                is VoiceUiState.Generating -> "Processando..."
                is VoiceUiState.Speaking -> "Falando..."
                is VoiceUiState.Error -> (uiState as VoiceUiState.Error).message
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (uiState is VoiceUiState.Error)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface
        )

        // Middle Section: Recognized Text
        Text(
            text = recognizedText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 32.dp)
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
                    imageVector = if (uiState is VoiceUiState.Listening)
                        Icons.Default.Stop
                    else
                        Icons.Default.Mic,
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
