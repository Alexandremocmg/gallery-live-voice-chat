package com.google.ai.edge.gallery.voice.pedagogy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachingSignalDetectorTest {
  private val detector = TeachingSignalDetector()

  @Test
  fun `strong learning request activates intent and beginner estimate`() {
    val result = detector.detect("Me ensine fracao do zero", TeachingState())

    assertTrue(result.strongLearningIntent)
    assertEquals(LearnerLevel.BEGINNER, result.levelEstimate.level)
    assertEquals("fracao", result.topicHint)
  }

  @Test
  fun `isolated factual explanation is not automatically a lesson`() {
    val result = detector.detect("Como funciona o Bluetooth?", TeachingState())

    assertFalse(result.strongLearningIntent)
    assertEquals(UnderstandingSignal.NONE, result.signal)
  }

  @Test
  fun `deep and step by step requests activate teaching`() {
    val deep = detector.detect("Me explica IA de forma profunda", TeachingState())
    val steps = detector.detect("Mostre passo a passo como criar uma horta", TeachingState())

    assertTrue(deep.strongLearningIntent)
    assertEquals("ia", deep.topicHint)
    assertTrue(steps.strongLearningIntent)
    assertTrue(steps.topicHint.orEmpty().contains("horta"))
  }

  @Test
  fun `detects understanding signals in active teaching`() {
    val state = activeState()

    assertEquals(UnderstandingSignal.CONFUSED, detector.detect("Nao entendi", state).signal)
    assertEquals(UnderstandingSignal.CONFUSED, detector.detect("Ainda nao", state).signal)
    assertEquals(UnderstandingSignal.PARTIAL, detector.detect("Mais ou menos", state).signal)
    assertEquals(UnderstandingSignal.UNDERSTOOD, detector.detect("Agora ficou claro", state).signal)
    assertEquals(UnderstandingSignal.WANTS_EXAMPLE, detector.detect("Me da um exemplo", state).signal)
    assertEquals(UnderstandingSignal.WANTS_DEPTH, detector.detect("E por dentro como funciona?", state).signal)
    assertEquals(UnderstandingSignal.WANTS_NEXT, detector.detect("Continua", state).signal)
    assertEquals(UnderstandingSignal.WANTS_NEXT, detector.detect("Mais", state).signal)
  }

  @Test
  fun `requesting another explanation is treated as confusion`() {
    assertEquals(
      UnderstandingSignal.CONFUSED,
      detector.detect("Explique de outro jeito", activeState()).signal,
    )
  }

  @Test
  fun `detects learner attempt only while awaiting an answer`() {
    val awaiting = activeState().copy(awaitingUnderstanding = true)
    val notAwaiting = activeState().copy(awaitingUnderstanding = false)

    assertEquals(
      UnderstandingSignal.LEARNER_ATTEMPT,
      detector.detect("A variavel guarda um valor", awaiting).signal,
    )
    assertEquals(
      UnderstandingSignal.NONE,
      detector.detect("A variavel guarda um valor", notAwaiting).signal,
    )
  }

  @Test
  fun `question after a check is not mistaken for an answer`() {
    val state = activeState().copy(awaitingUnderstanding = true)

    assertEquals(
      UnderstandingSignal.NONE,
      detector.detect("Como isso funciona na pratica?", state).signal,
    )
  }

  @Test
  fun `explicit topic change and exit are conservative`() {
    val state = activeState()

    assertEquals(
      UnderstandingSignal.TOPIC_CHANGE,
      detector.detect("Mudando de assunto, fale sobre clima", state).signal,
    )
    assertTrue(detector.detect("Volte ao normal", state).exitRequested)
    assertEquals(UnderstandingSignal.NONE, detector.detect("E a funcao?", state).signal)
  }

  @Test
  fun `estimates explicit intermediate and advanced levels`() {
    assertEquals(
      LearnerLevel.INTERMEDIATE,
      detector.detect("Ja sei o basico de Kotlin", TeachingState()).levelEstimate.level,
    )
    assertEquals(
      LearnerLevel.ADVANCED,
      detector.detect("Quero detalhes tecnicos e trade-offs", TeachingState()).levelEstimate.level,
    )
  }

  private fun activeState() =
    TeachingState(
      mode = TeachingMode.ACTIVE,
      topic = "variaveis",
      currentConcept = "armazenamento de valores",
      lastAction = TeachingAction.EXPLAIN,
    )
}
