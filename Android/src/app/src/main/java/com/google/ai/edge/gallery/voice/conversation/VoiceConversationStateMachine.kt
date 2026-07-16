package com.google.ai.edge.gallery.voice.conversation

import com.google.ai.edge.gallery.voice.language.SpeechLocale

enum class VoiceConversationPhase {
  IDLE,
  LISTENING,
  THINKING,
  SPEAKING,
  INTERRUPTED,
  SKILL_ACTIVE,
  FAILED,
}

data class SpeechPlaybackCheckpoint(
  val remainingText: String,
  val completeText: String,
  val locale: SpeechLocale,
  val speechRate: Float?,
  val pendingChunks: List<PlaybackChunk> = emptyList(),
)

data class PlaybackChunk(
  val text: String,
  val locale: SpeechLocale,
  val speechRate: Float?,
)

class VoiceConversationStateMachine {
  @Volatile var phase: VoiceConversationPhase = VoiceConversationPhase.IDLE
    private set

  @Synchronized
  fun transitionTo(next: VoiceConversationPhase): Boolean {
    if (next == phase) return true
    val allowed = when (phase) {
      VoiceConversationPhase.IDLE -> setOf(VoiceConversationPhase.LISTENING, VoiceConversationPhase.THINKING, VoiceConversationPhase.SKILL_ACTIVE, VoiceConversationPhase.FAILED)
      VoiceConversationPhase.LISTENING -> setOf(VoiceConversationPhase.THINKING, VoiceConversationPhase.IDLE, VoiceConversationPhase.FAILED)
      VoiceConversationPhase.THINKING -> setOf(VoiceConversationPhase.SPEAKING, VoiceConversationPhase.IDLE, VoiceConversationPhase.FAILED)
      VoiceConversationPhase.SPEAKING -> setOf(VoiceConversationPhase.INTERRUPTED, VoiceConversationPhase.IDLE, VoiceConversationPhase.FAILED)
      VoiceConversationPhase.INTERRUPTED -> setOf(VoiceConversationPhase.LISTENING, VoiceConversationPhase.SPEAKING, VoiceConversationPhase.IDLE)
      VoiceConversationPhase.SKILL_ACTIVE -> setOf(VoiceConversationPhase.LISTENING, VoiceConversationPhase.THINKING, VoiceConversationPhase.IDLE, VoiceConversationPhase.FAILED)
      VoiceConversationPhase.FAILED -> setOf(VoiceConversationPhase.IDLE)
    }
    if (next !in allowed) return false
    phase = next
    return true
  }

  @Synchronized
  fun reset() {
    phase = VoiceConversationPhase.IDLE
  }
}
