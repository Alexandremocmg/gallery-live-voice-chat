package com.google.ai.edge.gallery.voice.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.MessageBodyText
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageSide
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageStatus
import com.google.ai.edge.gallery.voice.conversation.ConversationUiMessage
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageAction
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageActionContext
import com.google.ai.edge.gallery.voice.conversation.ConversationMessageActionPolicy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceConversationTimeline(
    messages: List<ConversationUiMessage>,
    onSpeakAgain: (ConversationUiMessage) -> Unit,
    onEditAndResend: (ConversationUiMessage, String) -> Unit,
    onRegenerate: (ConversationUiMessage) -> Unit,
    onNotice: (String) -> Unit,
    isConversationBusy: Boolean,
    hasTextToSpeech: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
) {
    val state = listState ?: rememberLazyListState()
    val context = LocalContext.current
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var editDraft by rememberSaveable { mutableStateOf("") }
    val selectedMessage = messages.firstOrNull { it.id == selectedMessageId }
    val editingMessage = messages.firstOrNull { it.id == editingMessageId }

    LaunchedEffect(messages.lastOrNull()?.id) {
        val wasNearEnd = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            ?.let { it >= messages.lastIndex - 1 } ?: true
        if (messages.isNotEmpty() && wasNearEnd) {
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
                        .combinedClickable(
                            enabled = message.status == ConversationMessageStatus.COMPLETE &&
                                !isConversationBusy,
                            onClick = {},
                            onLongClick = { selectedMessageId = message.id },
                        ),
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
                        if (message.status == ConversationMessageStatus.COMPLETE &&
                            !isConversationBusy
                        ) {
                            IconButton(
                                onClick = { selectedMessageId = message.id },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ações da mensagem")
                            }
                        }
                        if (message.attachmentLabels.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                message.attachmentLabels.forEach { label ->
                                    Text(
                                        text = "📎 $label",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        if (isSystem) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            )
                        } else {
                            MessageBodyText(
                                message = ChatMessageText(
                                    content = message.text,
                                    side = if (isUser) ChatSide.USER else ChatSide.AGENT,
                                    isMarkdown = message.isMarkdown,
                                ),
                                inProgress = message.status == ConversationMessageStatus.STREAMING,
                                horizontalPadding = 0.dp,
                                userTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                userLinkColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onCopyClicked = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Kabem Voice", text))
                                    onNotice("Mensagem copiada")
                                },
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
        val associatedUserMessage = messages.takeWhile { it.id != message.id }
            .lastOrNull { it.side == ConversationMessageSide.USER }
        val actions = ConversationMessageActionPolicy.availableActions(
            ConversationMessageActionContext(
                message = message,
                hasTextToSpeech = hasTextToSpeech,
                hasAssociatedUserMessage = associatedUserMessage != null,
                isConversationBusy = isConversationBusy,
                isRewriteSafe = when (message.side) {
                    ConversationMessageSide.USER -> message.attachmentLabels.isEmpty()
                    ConversationMessageSide.ASSISTANT -> associatedUserMessage?.attachmentLabels?.isEmpty() == true
                    ConversationMessageSide.SYSTEM -> false
                },
            )
        )
        ModalBottomSheet(onDismissRequest = { selectedMessageId = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (message.side == ConversationMessageSide.USER) "Mensagem enviada" else "Resposta do Kabem",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (editingMessage?.id == message.id) {
                    OutlinedTextField(
                        value = editDraft,
                        onValueChange = { editDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Editar mensagem") },
                        minLines = 2,
                        maxLines = 6,
                    )
                    Button(
                        onClick = {
                            onEditAndResend(message, editDraft)
                            editingMessageId = null
                            selectedMessageId = null
                        },
                        enabled = editDraft.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enviar edição") }
                    TextButton(
                        onClick = { editingMessageId = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancelar edição") }
                } else {
                    if (ConversationMessageAction.EDIT_AND_RESEND in actions) {
                        TextButton(
                            onClick = {
                                editingMessageId = message.id
                                editDraft = message.text
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Editar e reenviar") }
                    }
                    if (ConversationMessageAction.REGENERATE in actions) {
                        TextButton(
                            onClick = {
                                onRegenerate(message)
                                selectedMessageId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Regenerar resposta") }
                    }
                }
                if (ConversationMessageAction.COPY in actions) Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Kabem Voice", message.text))
                        onNotice("Mensagem copiada")
                        selectedMessageId = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copiar") }
                if (ConversationMessageAction.SHARE in actions) TextButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar mensagem"))
                        selectedMessageId = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Compartilhar") }
                if (ConversationMessageAction.SPEAK_AGAIN in actions) {
                    TextButton(
                        onClick = {
                            onSpeakAgain(message)
                            selectedMessageId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ouvir novamente") }
                }
                TextButton(
                    onClick = { selectedMessageId = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Fechar") }
            }
        }
    }
}
