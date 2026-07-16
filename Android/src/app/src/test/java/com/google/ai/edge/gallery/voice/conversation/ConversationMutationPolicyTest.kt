package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.proto.ChatMessageProto
import com.google.ai.edge.gallery.proto.ChatSideProto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMutationPolicyTest {
  @Test
  fun `legacy migration creates deterministic stable ids and timestamps`() {
    val legacy = ChatMessageProto.newBuilder()
      .setSide(ChatSideProto.CHAT_SIDE_USER)
      .setContent("Olá")
      .build()

    val first = ConversationMutationPolicy.migrateLegacyMessages("session", 100, listOf(legacy)).single()
    val second = ConversationMutationPolicy.migrateLegacyMessages("session", 100, listOf(legacy)).single()

    assertEquals(first.messageId, second.messageId)
    assertNotEquals("", first.messageId)
    assertEquals(100, first.createdAtMs)
  }

  @Test
  fun `multimodal rewrite is blocked with a reason`() {
    val media = ChatMessageProto.newBuilder().addImageFilePaths("local.png").build()
    val decision = ConversationMutationPolicy.validateRewrite(listOf(media))

    assertTrue(decision is ConversationMutationDecision.Blocked)
    assertTrue((decision as ConversationMutationDecision.Blocked).reason.contains("imagem"))
  }

  @Test
  fun `rewrite is blocked when original image payload was not persisted`() {
    val media = ChatMessageProto.newBuilder().setVoiceHadImages(true).build()

    assertTrue(
      ConversationMutationPolicy.validateRewrite(listOf(media)) is ConversationMutationDecision.Blocked
    )
  }

  @Test
  fun `rollback requires same session epoch and exact next revision`() {
    val snapshot = ConversationToken("s", 4, 9)

    assertTrue(ConversationMutationPolicy.mayRollback(snapshot, ConversationToken("s", 5, 9)))
    assertTrue(!ConversationMutationPolicy.mayRollback(snapshot, ConversationToken("s", 6, 9)))
    assertTrue(!ConversationMutationPolicy.mayRollback(snapshot, ConversationToken("other", 5, 9)))
  }
}
