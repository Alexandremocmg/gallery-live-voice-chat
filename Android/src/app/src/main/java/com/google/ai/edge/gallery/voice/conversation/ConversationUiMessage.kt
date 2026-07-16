package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.proto.ChatMessageProto
import com.google.ai.edge.gallery.proto.ChatSideProto

/** Origin of a message shown in the shared conversation timeline. */
enum class ConversationMessageSource {
  VOICE,
  TEXT,
  SYSTEM,
}

enum class ConversationMessageSide {
  USER,
  ASSISTANT,
  SYSTEM,
}

enum class ConversationMessageStatus {
  COMPLETE,
  STREAMING,
  ERROR,
  CANCELLED,
}

data class ConversationUiMessage(
  val id: String,
  val side: ConversationMessageSide,
  val text: String,
  val status: ConversationMessageStatus,
  val createdAtMs: Long,
  val source: ConversationMessageSource,
  val canEdit: Boolean,
  val isMarkdown: Boolean = true,
  val attachmentLabels: List<String> = emptyList(),
)

/** Maps the persisted voice message format to the UI contract. */
object ConversationUiMessageMapper {
  fun fromProto(message: ChatMessageProto, position: Int): ConversationUiMessage {
    val side =
      when (message.side) {
        ChatSideProto.CHAT_SIDE_USER -> ConversationMessageSide.USER
        ChatSideProto.CHAT_SIDE_MODEL -> ConversationMessageSide.ASSISTANT
        else -> ConversationMessageSide.SYSTEM
      }
    val status =
      when {
        message.inProgress -> ConversationMessageStatus.STREAMING
        message.messageType == "ERROR" -> ConversationMessageStatus.ERROR
        message.messageType == "CANCELLED" -> ConversationMessageStatus.CANCELLED
        else -> ConversationMessageStatus.COMPLETE
      }
    return ConversationUiMessage(
      id = message.messageId.ifBlank { "legacy-$position" },
      side = side,
      text = message.content,
      status = status,
      createdAtMs = message.createdAtMs,
      source = when {
        side == ConversationMessageSide.SYSTEM -> ConversationMessageSource.SYSTEM
        message.voiceMessageSource == ConversationMessageSource.TEXT.name -> ConversationMessageSource.TEXT
        else -> ConversationMessageSource.VOICE
      },
      canEdit = side == ConversationMessageSide.USER && status == ConversationMessageStatus.COMPLETE,
      isMarkdown = message.isMarkdown,
      attachmentLabels = buildList {
        if (message.imageFilePathsCount > 0) {
          add(if (message.imageFilePathsCount == 1) "1 imagem" else "${message.imageFilePathsCount} imagens")
        } else if (message.voiceHadImages) {
          add("Imagem usada no turno")
        }
        if (message.audioClipsCount > 0) {
          add(if (message.audioClipsCount == 1) "1 áudio" else "${message.audioClipsCount} áudios")
        } else if (message.voiceHadAudio) {
          add("Áudio usado no turno")
        }
        if (message.pdfPageNumbersCount > 0) {
          add("PDF · páginas ${message.pdfPageNumbersList.joinToString(", ")}")
        }
      },
    )
  }

  fun fromProto(messages: List<ChatMessageProto>): List<ConversationUiMessage> =
    messages.mapIndexed { position, message -> fromProto(message, position) }
}
