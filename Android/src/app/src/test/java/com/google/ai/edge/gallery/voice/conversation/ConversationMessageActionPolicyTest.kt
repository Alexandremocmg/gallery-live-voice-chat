package com.google.ai.edge.gallery.voice.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMessageActionPolicyTest {
  private val userMessage =
    ConversationUiMessage(
      id = "user-1",
      side = ConversationMessageSide.USER,
      text = "Pergunta",
      status = ConversationMessageStatus.COMPLETE,
      createdAtMs = 1,
      source = ConversationMessageSource.VOICE,
      canEdit = true,
    )

  private val assistantMessage =
    ConversationUiMessage(
      id = "assistant-1",
      side = ConversationMessageSide.ASSISTANT,
      text = "Resposta",
      status = ConversationMessageStatus.COMPLETE,
      createdAtMs = 2,
      source = ConversationMessageSource.VOICE,
      canEdit = false,
    )

  @Test
  fun `user message exposes copy select share and edit`() {
    val actions =
      ConversationMessageActionPolicy.availableActions(
        ConversationMessageActionContext(message = userMessage)
      )

    assertEquals(
      setOf(
        ConversationMessageAction.COPY,
        ConversationMessageAction.SHARE,
        ConversationMessageAction.EDIT_AND_RESEND,
      ),
      actions,
    )
  }

  @Test
  fun `assistant message exposes regenerate and speak when supported`() {
    val actions =
      ConversationMessageActionPolicy.availableActions(
        ConversationMessageActionContext(
          message = assistantMessage,
          hasTextToSpeech = true,
          hasAssociatedUserMessage = true,
        )
      )

    assertTrue(ConversationMessageAction.REGENERATE in actions)
    assertTrue(ConversationMessageAction.SPEAK_AGAIN in actions)

  }

  @Test
  fun `busy or streaming messages expose no content actions`() {
    val streaming = assistantMessage.copy(status = ConversationMessageStatus.STREAMING)
    val actions =
      ConversationMessageActionPolicy.availableActions(
        ConversationMessageActionContext(message = streaming, hasTextToSpeech = true)
      )

    assertTrue(actions.isEmpty())
  }

  @Test
  fun `unsafe multimodal rewrite is not offered`() {
    val user = userMessage.copy(attachmentLabels = listOf("Imagem 1"))
    val actions = ConversationMessageActionPolicy.availableActions(
      ConversationMessageActionContext(message = user, isRewriteSafe = false)
    )

    assertFalse(ConversationMessageAction.EDIT_AND_RESEND in actions)
    assertTrue(ConversationMessageAction.COPY in actions)
  }
}
