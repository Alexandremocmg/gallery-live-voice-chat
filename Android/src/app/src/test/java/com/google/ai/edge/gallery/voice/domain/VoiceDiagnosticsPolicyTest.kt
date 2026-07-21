package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceDiagnosticsPolicyTest {
  @Test
  fun onDeviceRecognizerIsReportedAsConfirmedLocal() {
    assertEquals(
      "Local confirmado",
      VoiceDiagnosticsPolicy.privacyLabel(
        ConnectivityMode.CONNECTED,
        RecognitionBackend.ANDROID_ON_DEVICE,
      ),
    )
  }

  @Test
  fun systemRecognizerDoesNotClaimGuaranteedOffline() {
    assertEquals(
      "Servico do sistema; rede possivel",
      VoiceDiagnosticsPolicy.privacyLabel(
        ConnectivityMode.CONNECTED,
        RecognitionBackend.ANDROID_SYSTEM,
      ),
    )
  }
}
