package com.google.ai.edge.gallery.voice.intelligence

import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelCapability
import com.google.ai.edge.gallery.voice.data.ResponseDepthProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceIntelligenceModelsTest {
  @Test
  fun cognitivePolicy_usesDeepOnlyForComplexRequest() {
    val policy = CognitivePolicy()

    assertEquals(
      CognitiveMode.FAST,
      policy.decide("que horas sao", ResponseDepthProfile.FLASH, teachingDeepen = false),
    )
    assertEquals(
      CognitiveMode.DEEP,
      policy.decide("crie uma estrategia completa", ResponseDepthProfile.STRATEGIC, false),
    )
  }

  @Test
  fun router_doesNotRouteMcpWhilePrivate() {
    val decision =
      VoiceCapabilityRouter().route(
        text = "use uma ferramenta MCP",
        profile = ResponseDepthProfile.FLASH,
        teachingActive = false,
        teachingDeepen = false,
        activeSkillId = null,
        hasImages = false,
        hasAudio = false,
        pdfPageCount = 0,
        connectivityMode = ConnectivityMode.PRIVATE_OFFLINE,
      )

    assertEquals(VoiceTurnIntent.CONVERSATION, decision.intent)
    assertEquals(false, decision.requiredCapabilities.network)
  }

  @Test
  fun selector_respectsCapabilitiesAndMemory() {
    val small =
      Model(
        name = "Gemma E2B",
        llmSupportImage = true,
        llmSupportAudio = true,
        llmMaxContextLength = 32_000,
        minDeviceMemoryInGb = 8,
        capabilities = listOf(ModelCapability.LLM_THINKING),
      )
    val large = small.copy(name = "Gemma E4B", minDeviceMemoryInGb = 12)
    val selector = VoiceModelSelector()

    assertEquals(
      "Gemma E2B",
      selector.select(listOf(large, small), RequiredCapabilities(audio = true), 8).model?.name,
    )
    assertNull(selector.select(listOf(small), RequiredCapabilities(image = true), 4).model)
  }
}
