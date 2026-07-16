package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.proto.ChatMessageProto
import com.google.ai.edge.gallery.proto.ChatSideProto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationUiMessageTest {
  @Test
  fun `maps user and assistant messages in order`() {
    val messages =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_USER)
            .setContent("Olá")
            .build(),
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_MODEL)
            .setContent("Como posso ajudar?")
            .build(),
        )
      )

    assertEquals(listOf("Olá", "Como posso ajudar?"), messages.map { it.text })
    assertEquals(ConversationMessageSide.USER, messages[0].side)
    assertEquals(ConversationMessageSide.ASSISTANT, messages[1].side)
    assertEquals(listOf("legacy-0", "legacy-1"), messages.map { it.id })
  }

  @Test
  fun `only complete user messages can be edited`() {
    val messages =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_USER)
            .setContent("Pergunta")
            .build(),
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_MODEL)
            .setContent("Resposta")
            .setInProgress(true)
            .build(),
        )
      )

    assertTrue(messages[0].canEdit)
    assertFalse(messages[1].canEdit)
    assertEquals(ConversationMessageStatus.STREAMING, messages[1].status)
  }

  @Test
  fun `unknown side becomes a system message`() {
    val message =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setContent("Aviso")
            .setMessageType("ERROR")
            .build()
        )
      ).single()

    assertEquals(ConversationMessageSide.SYSTEM, message.side)
    assertEquals(ConversationMessageSource.SYSTEM, message.source)
    assertEquals(ConversationMessageStatus.ERROR, message.status)
    assertFalse(message.canEdit)
  }

  @Test
  fun `maps persisted media metadata to attachment labels`() {
    val message =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_USER)
            .setContent("Analise isso")
            .addImageFilePaths("image.png")
            .addAudioClips(com.google.ai.edge.gallery.proto.AudioMessageProto.getDefaultInstance())
            .addPdfPageNumbers(2)
            .build()
        )
      ).single()

    assertEquals(listOf("1 imagem", "1 áudio", "PDF · páginas 2"), message.attachmentLabels)
  }

  @Test
  fun `preserves markdown flag`() {
    val message =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setSide(ChatSideProto.CHAT_SIDE_MODEL)
            .setIsMarkdown(false)
            .build()
        )
      ).single()

    assertFalse(message.isMarkdown)
  }

  @Test
  fun `preserves stable id and typed source`() {
    val message =
      ConversationUiMessageMapper.fromProto(
        listOf(
          ChatMessageProto.newBuilder()
            .setMessageId("message-123")
            .setVoiceMessageSource(ConversationMessageSource.TEXT.name)
            .setSide(ChatSideProto.CHAT_SIDE_USER)
            .build()
        )
      ).single()

    assertEquals("message-123", message.id)
    assertEquals(ConversationMessageSource.TEXT, message.source)
  }
}
