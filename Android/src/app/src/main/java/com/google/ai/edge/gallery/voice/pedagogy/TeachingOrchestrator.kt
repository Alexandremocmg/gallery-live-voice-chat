package com.google.ai.edge.gallery.voice.pedagogy

class TeachingOrchestrator(
  private val detector: TeachingSignalDetector = TeachingSignalDetector(),
) {
  fun decide(currentState: TeachingState, userText: String): TeachingDecision {
    val detection = detector.detect(userText, currentState)

    if (detection.exitRequested) {
      return TeachingDecision(
        signal = UnderstandingSignal.TOPIC_CHANGE,
        action = TeachingAction.COMPLETE,
        proposedNextState = TeachingState(),
        shouldCheckUnderstanding = false,
      )
    }

    if (currentState.mode == TeachingMode.OFF) {
      return if (detection.strongLearningIntent) {
        startTeaching(detection, userText)
      } else {
        normalDecision(detection.signal)
      }
    }

    if (detection.signal == UnderstandingSignal.TOPIC_CHANGE) {
      return if (detection.strongLearningIntent) {
        startTeaching(detection, userText)
      } else {
        TeachingDecision(
          signal = UnderstandingSignal.TOPIC_CHANGE,
          action = TeachingAction.ANSWER_NORMALLY,
          proposedNextState = TeachingState(),
          shouldCheckUnderstanding = false,
          topicHint = detection.topicHint,
        )
      }
    }

    if (detection.strongLearningIntent && detection.topicHint != null) {
      val sameTopic = isSameTopic(currentState.topic, detection.topicHint)
      if (!sameTopic) return startTeaching(detection, userText)
    }

    return continueTeaching(currentState, detection)
  }

  private fun startTeaching(
    detection: TeachingDetection,
    userText: String,
  ): TeachingDecision {
    val topic = detection.topicHint?.takeIf { it.isNotBlank() }
    val needsGoal = topic == null || topic == detection.normalizedText && topic.split(' ').size <= 3
    val action = if (needsGoal) TeachingAction.ESTABLISH_GOAL else TeachingAction.EXPLAIN
    val nextState =
      TeachingState(
        mode = TeachingMode.ACTIVE,
        topic = topic ?: userText.toTeachingHint(),
        goal = topic?.let { "Aprender $it" },
        estimatedLevel = detection.levelEstimate.level,
        levelConfidence = detection.levelEstimate.confidence,
        currentConcept = topic,
        lastAction = action,
        turnCount = 1,
        awaitingUnderstanding = true,
      )
    return TeachingDecision(
      signal = detection.signal,
      action = action,
      proposedNextState = nextState,
      shouldCheckUnderstanding = true,
      topicHint = nextState.topic,
      goalHint = nextState.goal,
    )
  }

  private fun continueTeaching(
    currentState: TeachingState,
    detection: TeachingDetection,
  ): TeachingDecision {
    val (action, confusionCount, level) =
      when (detection.signal) {
        UnderstandingSignal.UNDERSTOOD ->
          Triple(TeachingAction.ADVANCE, 0, currentState.estimatedLevel)
        UnderstandingSignal.CONFUSED -> {
          val repeated = currentState.recentConfusionCount >= 1
          Triple(
            if (repeated) TeachingAction.GIVE_ANALOGY else TeachingAction.SIMPLIFY,
            currentState.recentConfusionCount + 1,
            if (repeated) lowerLevel(currentState.estimatedLevel) else currentState.estimatedLevel,
          )
        }
        UnderstandingSignal.PARTIAL ->
          Triple(TeachingAction.GIVE_EXAMPLE, 0, currentState.estimatedLevel)
        UnderstandingSignal.WANTS_EXAMPLE ->
          Triple(TeachingAction.GIVE_EXAMPLE, currentState.recentConfusionCount, currentState.estimatedLevel)
        UnderstandingSignal.WANTS_DEPTH ->
          Triple(TeachingAction.DEEPEN, 0, raiseLevel(currentState.estimatedLevel))
        UnderstandingSignal.WANTS_NEXT ->
          Triple(TeachingAction.ADVANCE, 0, currentState.estimatedLevel)
        UnderstandingSignal.LEARNER_ATTEMPT ->
          Triple(TeachingAction.EVALUATE_ATTEMPT, 0, currentState.estimatedLevel)
        UnderstandingSignal.NONE ->
          Triple(TeachingAction.EXPLAIN, currentState.recentConfusionCount, currentState.estimatedLevel)
        UnderstandingSignal.TOPIC_CHANGE ->
          Triple(TeachingAction.ANSWER_NORMALLY, 0, LearnerLevel.UNKNOWN)
      }

    val explicitEstimate = detection.levelEstimate
    val nextLevel =
      if (explicitEstimate.level != LearnerLevel.UNKNOWN) explicitEstimate.level else level
    val nextConfidence =
      if (explicitEstimate.level != LearnerLevel.UNKNOWN) {
        explicitEstimate.confidence
      } else {
        currentState.levelConfidence
      }
    val nextState =
      currentState.copy(
        estimatedLevel = nextLevel,
        levelConfidence = nextConfidence,
        lastAction = action,
        recentConfusionCount = confusionCount,
        turnCount = currentState.turnCount + 1,
        awaitingUnderstanding = true,
      )
    return TeachingDecision(
      signal = detection.signal,
      action = action,
      proposedNextState = nextState,
      shouldCheckUnderstanding = true,
      topicHint = nextState.topic,
      goalHint = nextState.goal,
    )
  }

  private fun normalDecision(signal: UnderstandingSignal): TeachingDecision {
    return TeachingDecision(
      signal = signal,
      action = TeachingAction.ANSWER_NORMALLY,
      proposedNextState = TeachingState(),
      shouldCheckUnderstanding = false,
    )
  }

  private fun lowerLevel(level: LearnerLevel): LearnerLevel {
    return when (level) {
      LearnerLevel.ADVANCED -> LearnerLevel.INTERMEDIATE
      LearnerLevel.INTERMEDIATE -> LearnerLevel.BEGINNER
      LearnerLevel.UNKNOWN -> LearnerLevel.BEGINNER
      LearnerLevel.BEGINNER -> LearnerLevel.BEGINNER
    }
  }

  private fun raiseLevel(level: LearnerLevel): LearnerLevel {
    return when (level) {
      LearnerLevel.UNKNOWN -> LearnerLevel.INTERMEDIATE
      LearnerLevel.BEGINNER -> LearnerLevel.INTERMEDIATE
      LearnerLevel.INTERMEDIATE -> LearnerLevel.ADVANCED
      LearnerLevel.ADVANCED -> LearnerLevel.ADVANCED
    }
  }

  private fun isSameTopic(current: String?, candidate: String): Boolean {
    if (current.isNullOrBlank()) return false
    val currentTerms = topicTerms(current)
    val candidateTerms = topicTerms(candidate)
    if (currentTerms.isEmpty() || candidateTerms.isEmpty()) return false
    return currentTerms.intersect(candidateTerms).isNotEmpty()
  }

  private fun topicTerms(value: String): Set<String> {
    return value.lowercase()
      .split(Regex("[^a-z0-9]+"))
      .filter { it.length >= 4 && it !in TOPIC_STOP_WORDS }
      .toSet()
  }
}

private val TOPIC_STOP_WORDS =
  setOf("quero", "aprender", "entender", "sobre", "como", "isso", "este", "essa", "para")
