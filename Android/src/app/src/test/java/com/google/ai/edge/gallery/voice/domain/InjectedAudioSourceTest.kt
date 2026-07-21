package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class InjectedAudioSourceTest {
  @Test
  fun computesPeakFromLittleEndianPcm16() {
    val pcm = byteArrayOf(
      0x00, 0x00,
      0xE8.toByte(), 0x03,
      0x30, 0xF8.toByte(),
    )

    assertEquals(2_000, InjectedAudioSource().pcm16Peak(pcm, pcm.size))
  }

  @Test
  fun ignoresIncompleteTrailingByte() {
    val pcm = byteArrayOf(0xFF.toByte(), 0x7F, 0x55)

    assertEquals(32_767, InjectedAudioSource().pcm16Peak(pcm, pcm.size))
  }
}
