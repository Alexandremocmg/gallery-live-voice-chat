package com.google.ai.edge.gallery.voice.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceConversationStateMachineTest {
  @Test
  fun supportsListeningThinkingSpeakingAndInterruption() {
    val machine = VoiceConversationStateMachine()

    assertTrue(machine.transitionTo(VoiceConversationPhase.LISTENING))
    assertTrue(machine.transitionTo(VoiceConversationPhase.THINKING))
    assertTrue(machine.transitionTo(VoiceConversationPhase.SPEAKING))
    assertTrue(machine.transitionTo(VoiceConversationPhase.INTERRUPTED))
    assertTrue(machine.transitionTo(VoiceConversationPhase.LISTENING))
  }

  @Test
  fun rejectsInvalidDirectTransition() {
    val machine = VoiceConversationStateMachine()

    assertFalse(machine.transitionTo(VoiceConversationPhase.SPEAKING))
    assertEquals(VoiceConversationPhase.IDLE, machine.phase)
  }
}
