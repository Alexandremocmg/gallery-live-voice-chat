package com.google.ai.edge.gallery.voice.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageSide
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageStatus
import com.google.ai.edge.gallery.voice.conversation.ConversationUiMessage

@Composable
fun VoiceConversationTimeline(
    messages: List<ConversationUiMessage>,
    onSpeakAgain: (ConversationUiMessage) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()
    val context = LocalContext.current
    var selectedMessage by remember { mutableStateOf<ConversationUiMessage?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            state.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            val isUser = message.side == ConversationMessageSide.USER
            val isSystem = message.side == ConversationMessageSide.SYSTEM
            val background = when {
                isSystem -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = horizontalAlignment,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = background,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = message.status == ConversationMessageStatus.COMPLETE) {
                            selectedMessage = message
                        },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = when {
                                isSystem -> "Sistema"
                                isUser -> "Você"
                                else -> "Kabem"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SelectionContainer {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        when (message.status) {
                            ConversationMessageStatus.STREAMING ->
                                Text(
                                    text = "Gerando...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            ConversationMessageStatus.ERROR ->
                                Text(
                                    text = "Não foi possível concluir esta resposta.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            ConversationMessageStatus.CANCELLED ->
                                Text(
                                    text = "Resposta interrompida.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            ConversationMessageStatus.COMPLETE -> Unit
                        }
                    }
                }
            }
        }
    }

    selectedMessage?.let { message ->
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (message.side == ConversationMessageSide.USER) "Mensagem enviada" else "Resposta do Kabem",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Kabem Voice", message.text))
                        Toast.makeText(context, "Mensagem copiada", Toast.LENGTH_SHORT).show()
                        selectedMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copiar") }
                TextButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar mensagem"))
                        selectedMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Compartilhar") }
                if (message.side == ConversationMessageSide.ASSISTANT) {
                    TextButton(
                        onClick = {
                            onSpeakAgain(message)
                            selectedMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ouvir novamente") }
                }
                TextButton(
                    onClick = { selectedMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Fechar") }
            }
        }
    }
}
