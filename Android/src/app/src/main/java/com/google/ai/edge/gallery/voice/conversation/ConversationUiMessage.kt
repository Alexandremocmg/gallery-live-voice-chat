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
  val position: Int,
  val source: ConversationMessageSource,
  val canEdit: Boolean,
  val isMarkdown: Boolean = true,
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
      id = "voice-message-$position",
      side = side,
      text = message.content,
      status = status,
      position = position,
      source = if (side == ConversationMessageSide.SYSTEM) {
        ConversationMessageSource.SYSTEM
      } else {
        ConversationMessageSource.VOICE
      },
      canEdit = side == ConversationMessageSide.USER && status == ConversationMessageStatus.COMPLETE,
      isMarkdown = message.isMarkdown,
    )
  }

  fun fromProto(messages: List<ChatMessageProto>): List<ConversationUiMessage> =
    messages.mapIndexed(::fromProto)
}
