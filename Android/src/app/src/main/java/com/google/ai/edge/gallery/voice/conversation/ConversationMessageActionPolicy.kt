package com.google.ai.edge.gallery.voice.conversation

enum class ConversationMessageAction {
  COPY,
  SELECT,
  EDIT_AND_RESEND,
  REGENERATE,
  SHARE,
  SPEAK_AGAIN,
  SAVE_MEMORY,
}

data class ConversationMessageActionContext(
  val message: ConversationUiMessage,
  val hasTextToSpeech: Boolean = false,
  val hasAssociatedUserMessage: Boolean = false,
  val isConversationBusy: Boolean = false,
  val isRewriteSafe: Boolean = true,
)

object ConversationMessageActionPolicy {
  fun availableActions(context: ConversationMessageActionContext): Set<ConversationMessageAction> {
    val message = context.message
    if (context.isConversationBusy ||
      message.status != ConversationMessageStatus.COMPLETE ||
      message.text.isBlank()
    ) {
      return emptySet()
    }

    return buildSet {
      add(ConversationMessageAction.COPY)
      add(ConversationMessageAction.SHARE)
      if (message.side == ConversationMessageSide.USER && message.canEdit && context.isRewriteSafe) {
        add(ConversationMessageAction.EDIT_AND_RESEND)
      }
      if (message.side == ConversationMessageSide.ASSISTANT) {
        if (context.hasAssociatedUserMessage && context.isRewriteSafe) {
          add(ConversationMessageAction.REGENERATE)
        }
        if (context.hasTextToSpeech) add(ConversationMessageAction.SPEAK_AGAIN)

      }
    }
  }
}
