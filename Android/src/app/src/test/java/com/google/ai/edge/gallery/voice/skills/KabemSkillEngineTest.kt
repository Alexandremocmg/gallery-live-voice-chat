package com.google.ai.edge.gallery.voice.skills

import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode
import com.google.ai.edge.gallery.voice.language.EnglishLessonIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KabemSkillEngineTest {
  private val engine = KabemSkillEngine()

  @Test
  fun activatesEnglishTeacherFromVoicePhrase() {
    val result =
      engine.resolve(
        text = "Quero estudar ingles",
        currentSkillId = null,
        englishIntent = EnglishLessonIntent.START,
        hasPdf = false,
        hasImages = false,
        connectivityMode = ConnectivityMode.PRIVATE_OFFLINE,
      )

    assertEquals(KabemBuiltInSkills.ENGLISH_TEACHER_ID, result.activeSkill?.id)
  }

  @Test
  fun explicitExitEndsCurrentSkill() {
    val result =
      engine.resolve(
        text = "encerrar a aula",
        currentSkillId = KabemBuiltInSkills.ENGLISH_TEACHER_ID,
        englishIntent = EnglishLessonIntent.NONE,
        hasPdf = false,
        hasImages = false,
        connectivityMode = ConnectivityMode.PRIVATE_OFFLINE,
      )

    assertNull(result.activeSkill)
  }
}
