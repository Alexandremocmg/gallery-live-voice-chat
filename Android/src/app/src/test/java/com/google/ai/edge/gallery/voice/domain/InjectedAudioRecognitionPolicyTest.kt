package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InjectedAudioRecognitionPolicyTest {
  @Test
  fun enablesControlledAudioForSystemRecognizerOnApi33() {
    assertTrue(
      InjectedAudioRecognitionPolicy.shouldUse(
        apiLevel = 33,
        featureEnabled = true,
        backend = RecognitionBackend.ANDROID_SYSTEM,
      )
    )
  }

  @Test
  fun disabledFlagNeverInjectsAudio() {
    assertFalse(
      InjectedAudioRecognitionPolicy.shouldUse(
        apiLevel = 35,
        featureEnabled = false,
        backend = RecognitionBackend.ANDROID_SYSTEM,
      )
    )
  }

  @Test
  fun api32CannotInjectAudio() {
    assertFalse(
      InjectedAudioRecognitionPolicy.shouldUse(
        apiLevel = 32,
        featureEnabled = true,
        backend = RecognitionBackend.ANDROID_SYSTEM,
      )
    )
  }

  @Test
  fun onDeviceRecognizerKeepsCertifiedMicrophonePath() {
    assertFalse(
      InjectedAudioRecognitionPolicy.shouldUse(
        apiLevel = 35,
        featureEnabled = true,
        backend = RecognitionBackend.ANDROID_ON_DEVICE,
      )
    )
  }

  @Test
  fun controlledAudioSelectsSystemRecognizerInsteadOfOnDeviceRecognizer() {
    assertEquals(
      RecognitionBackend.ANDROID_SYSTEM,
      InjectedAudioRecognitionPolicy.selectBackend(
        apiLevel = 35,
        featureEnabled = true,
        preferredBackend = RecognitionBackend.ANDROID_ON_DEVICE,
        systemRecognizerAvailable = true,
      ),
    )
  }

  @Test
  fun disablingControlledAudioRestoresPreferredBackend() {
    assertEquals(
      RecognitionBackend.ANDROID_ON_DEVICE,
      InjectedAudioRecognitionPolicy.selectBackend(
        apiLevel = 35,
        featureEnabled = false,
        preferredBackend = RecognitionBackend.ANDROID_ON_DEVICE,
        systemRecognizerAvailable = true,
      ),
    )
  }

  @Test
  fun controlledAudioFailsClosedWithoutCompatibleSystemRecognizer() {
    assertEquals(
      RecognitionBackend.ANDROID_ON_DEVICE,
      InjectedAudioRecognitionPolicy.selectBackend(
        apiLevel = 35,
        featureEnabled = true,
        preferredBackend = RecognitionBackend.ANDROID_ON_DEVICE,
        systemRecognizerAvailable = false,
      ),
    )
    assertFalse(
      InjectedAudioRecognitionPolicy.canEnable(
        apiLevel = 35,
        systemRecognizerAvailable = false,
      )
    )
  }

  @Test
  fun controlledAudioFailsClosedBeforeApi33() {
    assertEquals(
      RecognitionBackend.ANDROID_ON_DEVICE,
      InjectedAudioRecognitionPolicy.selectBackend(
        apiLevel = 32,
        featureEnabled = true,
        preferredBackend = RecognitionBackend.ANDROID_ON_DEVICE,
        systemRecognizerAvailable = true,
      ),
    )
    assertFalse(
      InjectedAudioRecognitionPolicy.canEnable(
        apiLevel = 32,
        systemRecognizerAvailable = true,
      )
    )
  }

  @Test
  fun controlledAudioFailsClosedInPrivateOfflineMode() {
    assertEquals(
      RecognitionBackend.ANDROID_ON_DEVICE,
      InjectedAudioRecognitionPolicy.selectBackend(
        apiLevel = 35,
        featureEnabled = true,
        preferredBackend = RecognitionBackend.ANDROID_ON_DEVICE,
        systemRecognizerAvailable = true,
        privateOffline = true,
      ),
    )
    assertFalse(
      InjectedAudioRecognitionPolicy.canEnable(
        apiLevel = 35,
        systemRecognizerAvailable = true,
        privateOffline = true,
      )
    )
  }
}
