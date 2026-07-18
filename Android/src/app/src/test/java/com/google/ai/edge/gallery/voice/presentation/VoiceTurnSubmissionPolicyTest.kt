package com.google.ai.edge.gallery.voice.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTurnSubmissionPolicyTest {
  @Test
  fun speechProcessingDoesNotBlockFinalTranscriptSubmission() {
    assertTrue(
      canSubmitConversationTurn(
        text = "ola",
        responseGenerationInProgress = false,
        hasActiveModel = true,
      ),
    )
  }

  @Test
  fun activeGenerationBlocksAnotherTurn() {
    assertFalse(
      canSubmitConversationTurn(
        text = "ola",
        responseGenerationInProgress = true,
        hasActiveModel = true,
      ),
    )
  }

  @Test
  fun blankTextAndMissingModelAreRejected() {
    assertFalse(canSubmitConversationTurn("", false, true))
    assertFalse(canSubmitConversationTurn("ola", false, false))
  }
}
