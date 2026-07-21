package com.google.ai.edge.gallery.voice.domain

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.language.RecognitionBackend
import com.google.ai.edge.gallery.voice.language.SpeechLocale

data class VoiceDiagnosticsSnapshot(
  val backend: RecognitionBackend = RecognitionBackend.UNAVAILABLE,
  val locale: SpeechLocale = SpeechLocale.PT_BR,
  val phase: String = "Idle",
  val sessionId: Long = 0L,
  val physicalAttempt: Int = 0,
  val lastError: String? = null,
  val privacyLabel: String = "Indisponivel",
)

object VoiceDiagnosticsPolicy {
  fun privacyLabel(
    connectivityMode: ConnectivityMode,
    backend: RecognitionBackend,
  ): String =
    when {
      backend == RecognitionBackend.ANDROID_ON_DEVICE -> "Local confirmado"
      connectivityMode == ConnectivityMode.PRIVATE_OFFLINE -> "Offline exigido"
      backend == RecognitionBackend.ANDROID_SYSTEM -> "Servico do sistema; rede possivel"
      else -> "Indisponivel"
    }
}
