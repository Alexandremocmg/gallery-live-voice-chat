package com.google.ai.edge.gallery.voice.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ConversationInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onSwitchToVoice: () -> Unit,
    reviewMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val canSend = text.trim().isNotEmpty()
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            label = { Text(if (reviewMode) "Revise antes de enviar" else "Digite sua mensagem") },
            minLines = 1,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
        )
        if (reviewMode) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Descartar transcrição")
            }
        } else {
            IconButton(onClick = onSwitchToVoice) {
                Icon(Icons.Default.KeyboardVoice, contentDescription = "Usar microfone")
            }
        }
        IconButton(onClick = onSend, enabled = canSend) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Enviar mensagem",
                tint = if (canSend) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}
