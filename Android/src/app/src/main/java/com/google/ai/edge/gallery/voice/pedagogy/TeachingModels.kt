package com.google.ai.edge.gallery.voice.pedagogy

enum class TeachingMode {
  OFF,
  ACTIVE,
}

enum class LearnerLevel {
  UNKNOWN,
  BEGINNER,
  INTERMEDIATE,
  ADVANCED,
}

enum class UnderstandingSignal {
  NONE,
  UNDERSTOOD,
  CONFUSED,
  PARTIAL,
  WANTS_EXAMPLE,
  WANTS_DEPTH,
  WANTS_NEXT,
  LEARNER_ATTEMPT,
  TOPIC_CHANGE,
}

enum class TeachingAction {
  ANSWER_NORMALLY,
  ESTABLISH_GOAL,
  EXPLAIN,
  SIMPLIFY,
  GIVE_EXAMPLE,
  GIVE_ANALOGY,
  REVIEW,
  DEEPEN,
  CHECK_UNDERSTANDING,
  EVALUATE_ATTEMPT,
  ADVANCE,
  CORRECT_GENTLY,
  COMPLETE,
}

data class TeachingState(
  val mode: TeachingMode = TeachingMode.OFF,
  val topic: String? = null,
  val goal: String? = null,
  val estimatedLevel: LearnerLevel = LearnerLevel.UNKNOWN,
  val levelConfidence: Float = 0f,
  val currentConcept: String? = null,
  val completedConcepts: List<String> = emptyList(),
  val lastAction: TeachingAction = TeachingAction.ANSWER_NORMALLY,
  val recentConfusionCount: Int = 0,
  val turnCount: Int = 0,
  val awaitingUnderstanding: Boolean = false,
)

data class TeachingDecision(
  val signal: UnderstandingSignal,
  val action: TeachingAction,
  val proposedNextState: TeachingState,
  val shouldCheckUnderstanding: Boolean,
  val topicHint: String? = null,
  val goalHint: String? = null,
)

data class LearnerLevelEstimate(
  val level: LearnerLevel,
  val confidence: Float,
)

internal fun String.toTeachingHint(maxLength: Int = MAX_TEACHING_HINT_LENGTH): String {
  val clean = replace(Regex("\\s+"), " ").trim().trim('.', '!', '?', ':', ';')
  if (clean.length <= maxLength) return clean
  val clipped = clean.take(maxLength + 1)
  val lastSpace = clipped.lastIndexOf(' ')
  return if (lastSpace >= maxLength / 2) clipped.take(lastSpace).trim() else clean.take(maxLength).trim()
}

internal const val MAX_TEACHING_HINT_LENGTH = 180
internal const val MAX_TEACHING_PROMPT_LENGTH = 1400
