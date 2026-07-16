package com.google.ai.edge.gallery.voice.pedagogy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachingOrchestratorTest {
  private val orchestrator = TeachingOrchestrator()

  @Test
  fun `starts a beginner lesson with a short teaching block`() {
    val decision = orchestrator.decide(TeachingState(), "Quero aprender juros compostos do zero")

    assertEquals(TeachingAction.EXPLAIN, decision.action)
    assertEquals(TeachingMode.ACTIVE, decision.proposedNextState.mode)
    assertEquals(LearnerLevel.BEGINNER, decision.proposedNextState.estimatedLevel)
    assertTrue(decision.shouldCheckUnderstanding)
  }

  @Test
  fun `ordinary factual question stays outside teaching`() {
    val decision = orchestrator.decide(TeachingState(), "Qual e a capital do Chile?")

    assertEquals(TeachingAction.ANSWER_NORMALLY, decision.action)
    assertEquals(TeachingMode.OFF, decision.proposedNextState.mode)
    assertFalse(decision.shouldCheckUnderstanding)
  }

  @Test
  fun `understanding advances the active lesson`() {
    val decision = orchestrator.decide(activeState(), "Entendi")

    assertEquals(TeachingAction.ADVANCE, decision.action)
    assertEquals(0, decision.proposedNextState.recentConfusionCount)
    assertTrue(decision.shouldCheckUnderstanding)
  }

  @Test
  fun `repeated confusion changes strategy and lowers level`() {
    val first = orchestrator.decide(activeState(), "Nao entendi")
    val second = orchestrator.decide(first.proposedNextState, "Ainda nao entendi")

    assertEquals(TeachingAction.SIMPLIFY, first.action)
    assertEquals(TeachingAction.GIVE_ANALOGY, second.action)
    assertEquals(LearnerLevel.BEGINNER, second.proposedNextState.estimatedLevel)
  }

  @Test
  fun `partial understanding receives an example`() {
    val decision = orchestrator.decide(activeState(), "Mais ou menos")

    assertEquals(TeachingAction.GIVE_EXAMPLE, decision.action)
  }

  @Test
  fun `learner answer is evaluated in the next response`() {
    val state = activeState().copy(awaitingUnderstanding = true)
    val decision = orchestrator.decide(state, "Juros incidem tambem sobre os juros anteriores")

    assertEquals(TeachingAction.EVALUATE_ATTEMPT, decision.action)
    assertTrue(decision.shouldCheckUnderstanding)
  }

  @Test
  fun `explicit exit completes and clears teaching state`() {
    val decision = orchestrator.decide(activeState(), "Volte ao normal")

    assertEquals(TeachingAction.COMPLETE, decision.action)
    assertEquals(TeachingMode.OFF, decision.proposedNextState.mode)
  }

  @Test
  fun `explicit topic change clears the old lesson and answers normally`() {
    val decision = orchestrator.decide(activeState(), "Mudando de assunto, fale sobre clima")

    assertEquals(TeachingAction.ANSWER_NORMALLY, decision.action)
    assertEquals(TeachingMode.OFF, decision.proposedNextState.mode)
  }

  @Test
  fun `decision does not mutate the state received`() {
    val original = activeState()
    val decision = orchestrator.decide(original, "Nao entendi")

    assertNotSame(original, decision.proposedNextState)
    assertEquals(0, original.recentConfusionCount)
    assertEquals(1, decision.proposedNextState.recentConfusionCount)
  }

  private fun activeState() =
    TeachingState(
      mode = TeachingMode.ACTIVE,
      topic = "juros compostos",
      goal = "Aprender juros compostos",
      estimatedLevel = LearnerLevel.INTERMEDIATE,
      levelConfidence = 0.8f,
      currentConcept = "juros sobre juros",
      lastAction = TeachingAction.EXPLAIN,
      turnCount = 1,
      awaitingUnderstanding = true,
    )
}
