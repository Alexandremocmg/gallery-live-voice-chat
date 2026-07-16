package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.proto.ChatMessageProto
import java.nio.charset.StandardCharsets
import java.util.UUID

const val VOICE_CONVERSATION_SCHEMA_VERSION = 1

data class ConversationToken(
  val sessionId: String,
  val revision: Long,
  val generationEpoch: Long,
)

sealed interface ConversationMutationDecision {
  data object Allowed : ConversationMutationDecision
  data class Blocked(val reason: String) : ConversationMutationDecision
}

/** Pure rules shared by the coordinator and its unit tests. */
object ConversationMutationPolicy {
  fun validateRewrite(messages: List<ChatMessageProto>): ConversationMutationDecision {
    val unsafe = messages.firstOrNull {
      it.imageFilePathsCount > 0 || it.audioClipsCount > 0 || it.pdfPageNumbersCount > 0 ||
        it.voiceHadImages || it.voiceHadAudio
    } ?: return ConversationMutationDecision.Allowed
    val kind = when {
      unsafe.imageFilePathsCount > 0 || unsafe.voiceHadImages -> "imagem"
      unsafe.audioClipsCount > 0 || unsafe.voiceHadAudio -> "áudio"
      else -> "PDF"
    }
    return ConversationMutationDecision.Blocked(
      "Esta ação foi bloqueada porque o histórico inclui $kind que não pode ser reconstruído com segurança."
    )
  }

  fun migrateLegacyMessages(
    sessionId: String,
    sessionCreatedAtMs: Long,
    messages: List<ChatMessageProto>,
  ): List<ChatMessageProto> =
    messages.mapIndexed { index, message ->
      if (message.messageId.isNotBlank() && message.createdAtMs > 0L) message
      else {
        val seed = "$sessionId|$index|${message.side.number}|${message.content}"
        message.toBuilder()
          .setMessageId(UUID.nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8)).toString())
          .setCreatedAtMs((sessionCreatedAtMs.takeIf { it > 0L } ?: 1L) + index)
          .build()
      }
    }

  fun tokenStillCurrent(expected: ConversationToken, current: ConversationToken): Boolean =
    expected == current

  fun mayRollback(snapshot: ConversationToken, current: ConversationToken): Boolean =
    snapshot.sessionId == current.sessionId &&
      snapshot.generationEpoch == current.generationEpoch &&
      current.revision == snapshot.revision + 1
}
