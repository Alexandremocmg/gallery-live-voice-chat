package com.google.ai.edge.gallery.voice.intelligence

import com.google.ai.edge.gallery.proto.ChatMessageProto
import com.google.ai.edge.gallery.proto.ChatSideProto
import com.google.ai.edge.litertlm.Message

object VoiceConversationMapper {
  fun toLiteRtMessages(
    messages: List<ChatMessageProto>,
    maxMessages: Int = 12,
    maxCharactersPerMessage: Int = 2_000,
  ): List<Message> {
    return messages
      .filter { it.messageType == "TEXT" && it.content.isNotBlank() }
      .takeLast(maxMessages)
      .mapNotNull { message ->
        val content = message.content.trim().take(maxCharactersPerMessage)
        when (message.side) {
          ChatSideProto.CHAT_SIDE_USER -> Message.user(content)
          ChatSideProto.CHAT_SIDE_MODEL -> Message.model(content)
          else -> null
        }
      }
      .toList()
  }
}
