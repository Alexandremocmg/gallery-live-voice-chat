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

  @Test
  fun pendingTranscriptIsSubmittedWhenContinuationNoMatchOccurs() {
    assertTrue(
      shouldSubmitPendingRecognitionAfterError(
        errorMessage = "Nao consegui entender a fala",
        hasPendingRecognition = true,
      ),
    )
  }

  @Test
  fun noMatchWithoutPendingTranscriptDoesNotSubmit() {
    assertFalse(
      shouldSubmitPendingRecognitionAfterError(
        errorMessage = "Nao consegui entender a fala",
        hasPendingRecognition = false,
      ),
    )
  }

  @Test
  fun permissionOrAudioErrorsDoNotSubmitPendingTranscript() {
    assertFalse(
      shouldSubmitPendingRecognitionAfterError(
        errorMessage = "Permissao de microfone ausente",
        hasPendingRecognition = true,
      ),
    )
  }

  @Test
  fun rejectedVoiceTurnExplainsWhyItDidNotRespond() {
    assertTrue(conversationTurnRejectionNotice("", false, true)!!.contains("vazio"))
    assertTrue(conversationTurnRejectionNotice("ola", true, true)!!.contains("resposta atual"))
    assertTrue(conversationTurnRejectionNotice("ola", false, false)!!.contains("modelo"))
  }
}
