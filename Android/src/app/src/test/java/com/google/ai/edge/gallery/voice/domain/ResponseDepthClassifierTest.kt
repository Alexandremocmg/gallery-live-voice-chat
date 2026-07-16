package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseDepthClassifierTest {
  private val classifier = ResponseDepthClassifier()

  @Test
  fun `classifies explicit response profiles`() {
    assertSelection("resuma isso", ResponseDepthProfile.FLASH)
    assertSelection("me explica IA", ResponseDepthProfile.DETAILED)
    assertSelection("quero um passo a passo", ResponseDepthProfile.STEP_BY_STEP)
    assertSelection("crie um plano de estudos", ResponseDepthProfile.STRATEGIC)
  }

  @Test
  fun `last keyword wins when profiles conflict`() {
    val result = classifier.select(null, "me explica rapido")

    assertEquals(ResponseDepthProfile.FLASH, result.profile)
    assertTrue(result.isExplicit)
  }

  @Test
  fun `continuation escalates a flash response`() {
    val result = classifier.select(ResponseDepthProfile.FLASH, "mais")

    assertEquals(ResponseDepthProfile.DETAILED, result.profile)
    assertEquals(ResponseProfileSource.CONTINUATION, result.source)
  }

  @Test
  fun `default remains flash without a false positive`() {
    val result = classifier.select(null, "qual a temperatura ideal da geladeira")

    assertEquals(ResponseDepthProfile.FLASH, result.profile)
    assertFalse(result.isExplicit)
  }

  @Test
  fun `teaching raises only default flash`() {
    val defaultFlash = classifier.select(null, "certo")
    val explicitFlash = classifier.select(null, "responda rapido")

    assertEquals(
      ResponseDepthProfile.DETAILED,
      classifier.effectiveForTeaching(defaultFlash, teachingActive = true),
    )
    assertEquals(
      ResponseDepthProfile.FLASH,
      classifier.effectiveForTeaching(explicitFlash, teachingActive = true),
    )
  }

  @Test
  fun `teaching preserves explicit structured profiles`() {
    val stepByStep = classifier.select(null, "mostre passo a passo")
    val strategic = classifier.select(null, "monte uma estrategia")

    assertEquals(
      ResponseDepthProfile.STEP_BY_STEP,
      classifier.effectiveForTeaching(stepByStep, teachingActive = true),
    )
    assertEquals(
      ResponseDepthProfile.STRATEGIC,
      classifier.effectiveForTeaching(strategic, teachingActive = true),
    )
  }

  private fun assertSelection(text: String, expected: ResponseDepthProfile) {
    val result = classifier.select(null, text)
    assertEquals(expected, result.profile)
    assertTrue(result.isExplicit)
  }
}
