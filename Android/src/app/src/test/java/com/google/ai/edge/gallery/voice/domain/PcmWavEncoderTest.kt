package com.google.ai.edge.gallery.voice.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmWavEncoderTest {
  @Test fun `creates a standard mono PCM wav header`() {
    val pcm = byteArrayOf(1, 2, 3, 4)
    val wav = PcmWavEncoder.encodeMono16Bit(pcm, 16_000)

    assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
    assertEquals("WAVE", wav.copyOfRange(8, 12).decodeToString())
    assertEquals("data", wav.copyOfRange(36, 40).decodeToString())
    assertEquals(48, wav.size)
    assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
  }
}
