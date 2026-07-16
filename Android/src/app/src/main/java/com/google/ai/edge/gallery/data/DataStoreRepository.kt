/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.data

import androidx.datastore.core.DataStore
import com.google.ai.edge.gallery.proto.AccessTokenData
import com.google.ai.edge.gallery.proto.BenchmarkResult
import com.google.ai.edge.gallery.proto.BenchmarkResults
import com.google.ai.edge.gallery.proto.ChatSessionProto
import com.google.ai.edge.gallery.proto.MemoryProto
import com.google.ai.edge.gallery.proto.MemoryStatusProto
import com.google.ai.edge.gallery.proto.Cutout
import com.google.ai.edge.gallery.proto.CutoutCollection
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.proto.Settings
import com.google.ai.edge.gallery.proto.Skill
import com.google.ai.edge.gallery.proto.Skills
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.proto.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.google.ai.edge.gallery.voice.intelligence.ConnectivityMode

const val VOICE_SESSION_TASK_ID = "live_voice"

// TODO(b/423700720): Change to async (suspend) functions
interface DataStoreRepository {
  fun saveTextInputHistory(history: List<String>)

  fun readTextInputHistory(): List<String>

  fun saveTheme(theme: Theme)

  fun readTheme(): Theme

  /**
   * Saves the user's preference for whether Firebase Analytics data collection is enabled (`true`)
   * or disabled (`false`).
   *
   * Private mode always overrides this preference and keeps collection disabled.
   *
   * @param enabled `true` to enable Firebase Analytics data collection; `false` to disable it.
   */
  fun saveFirebaseAnalytics(enabled: Boolean)

  /**
   * Reads that current setting for whether Firebase Analytics data collection is enabled.
   *
   * @return `true` only in connected mode when collection was explicitly enabled.
   */
  fun readFirebaseAnalytics(): Boolean

  fun saveTtsVoiceMode(mode: TtsVoiceMode)

  fun readTtsVoiceMode(): TtsVoiceMode

  fun saveEnglishDialect(dialect: EnglishDialect)

  fun readEnglishDialect(): EnglishDialect

  fun saveConnectivityMode(mode: ConnectivityMode)

  fun readConnectivityMode(): ConnectivityMode

  fun saveSelectedKabemSkillIds(skillIds: Set<String>)

  fun readSelectedKabemSkillIds(): Set<String>

  fun readVoiceSessions(): List<ChatSessionProto>

  fun saveVoiceSession(session: ChatSessionProto)

  fun deleteVoiceSession(sessionId: String)

  fun readMemories(): List<MemoryProto>

  fun saveMemory(memory: MemoryProto)

  fun deleteMemory(memoryId: String)

  fun clearMemories()

  fun readMemoryEnabled(): Boolean

  fun saveMemoryEnabled(enabled: Boolean)

  fun saveSecret(key: String, value: String)

  fun readSecret(key: String): String?

  fun deleteSecret(key: String)

  fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long)

  fun clearAccessTokenData()

  fun readAccessTokenData(): AccessTokenData?

  fun saveImportedModels(importedModels: List<ImportedModel>)

  fun readImportedModels(): List<ImportedModel>

  fun isTosAccepted(): Boolean

  fun acceptTos()

  fun isGemmaTermsOfUseAccepted(): Boolean

  fun acceptGemmaTermsOfUse()

  fun getHasRunTinyGarden(): Boolean

  fun setHasRunTinyGarden(hasRun: Boolean)

  fun addCutout(cutout: Cutout)

  fun getAllCutouts(): List<Cutout>

  fun setCutout(newCutout: Cutout)

  fun setCutouts(cutouts: List<Cutout>)

  fun setHasSeenBenchmarkComparisonHelp(seen: Boolean)

  fun getHasSeenBenchmarkComparisonHelp(): Boolean

  fun addBenchmarkResult(result: BenchmarkResult)

  fun getAllBenchmarkResults(): List<BenchmarkResult>

  fun deleteBenchmarkResult(index: Int)

  fun addSkill(skill: Skill)

  fun setSkills(skills: List<Skill>)

  fun setSkillSelected(skill: Skill, selected: Boolean)

  fun setAllSkillsSelected(selected: Boolean)

  fun getAllSkills(): List<Skill>

  fun deleteSkill(name: String)

  suspend fun deleteSkills(names: Set<String>)

  /** Records that a promo with the specified ID has been viewed. */
  fun addViewedPromoId(promoId: String)

  /** Removes a viewed promo record. */
  fun removeViewedPromoId(promoId: String)

  /** Returns whether a promo with the specified ID has been viewed. */
  fun hasViewedPromo(promoId: String): Boolean
}

/** Repository for managing data using Proto DataStore. */
class DefaultDataStoreRepository(
  private val dataStore: DataStore<Settings>,
  private val userDataDataStore: DataStore<UserData>,
  private val cutoutDataStore: DataStore<CutoutCollection>,
  private val benchmarkResultsDataStore: DataStore<BenchmarkResults>,
  private val skillsDataStore: DataStore<Skills>,
) : DataStoreRepository {
  override fun saveTextInputHistory(history: List<String>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearTextInputHistory().addAllTextInputHistory(history).build()
      }
    }
  }

  override fun readTextInputHistory(): List<String> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.textInputHistoryList
    }
  }

  override fun saveTheme(theme: Theme) {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setTheme(theme).build() }
    }
  }

  override fun readTheme(): Theme {
    return runBlocking {
      val settings = dataStore.data.first()
      val curTheme = settings.theme
      // Use "auto" as the default theme.
      if (curTheme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else curTheme
    }
  }

  /**
   * Persists the preference while enforcing private mode as a hard privacy boundary.
   */
  override fun saveFirebaseAnalytics(enabled: Boolean) {
    runBlocking {
      dataStore.updateData { settings ->
        val privateMode =
          ConnectivityMode.fromStorage(settings.connectivityMode) == ConnectivityMode.PRIVATE_OFFLINE
        settings.toBuilder().setDisableFirebaseAnalytics(privateMode || !enabled).build()
      }
    }
  }

  /**
   * Analytics is never reported as enabled while private mode is active.
   */
  override fun readFirebaseAnalytics(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      ConnectivityMode.fromStorage(settings.connectivityMode) == ConnectivityMode.CONNECTED &&
        !settings.disableFirebaseAnalytics
    }
  }

  override fun saveTtsVoiceMode(mode: TtsVoiceMode) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setTtsVoiceMode(mode.storageValue).build()
      }
    }
  }

  override fun readTtsVoiceMode(): TtsVoiceMode {
    return runBlocking {
      TtsVoiceMode.fromStorage(dataStore.data.first().ttsVoiceMode)
    }
  }

  override fun saveEnglishDialect(dialect: EnglishDialect) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setEnglishDialect(dialect.storageValue).build()
      }
    }
  }

  override fun readEnglishDialect(): EnglishDialect {
    return runBlocking {
      EnglishDialect.fromStorage(dataStore.data.first().englishDialect)
    }
  }

  override fun saveConnectivityMode(mode: ConnectivityMode) {
    runBlocking {
      dataStore.updateData { settings ->
        settings
          .toBuilder()
          .setConnectivityMode(mode.storageValue)
          .setDisableFirebaseAnalytics(
            if (mode == ConnectivityMode.PRIVATE_OFFLINE) true else settings.disableFirebaseAnalytics
          )
          .build()
      }
    }
  }

  override fun readConnectivityMode(): ConnectivityMode {
    return runBlocking {
      ConnectivityMode.fromStorage(dataStore.data.first().connectivityMode)
    }
  }

  override fun saveSelectedKabemSkillIds(skillIds: Set<String>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings
          .toBuilder()
          .clearSelectedKabemSkillIds()
          .addAllSelectedKabemSkillIds(skillIds.sorted())
          .setKabemSkillsConfigured(true)
          .build()
      }
    }
  }

  override fun readSelectedKabemSkillIds(): Set<String> {
    return runBlocking {
      val settings = dataStore.data.first()
      if (settings.kabemSkillsConfigured) {
        settings.selectedKabemSkillIdsList.toSet()
      } else {
        com.google.ai.edge.gallery.voice.skills.KabemBuiltInSkills.all.map { it.id }.toSet()
      }
    }
  }

  override fun readVoiceSessions(): List<ChatSessionProto> {
    return runBlocking {
      userDataDataStore.data.first().chatSessionsList
        .filter { it.isVoiceSession || it.taskId == VOICE_SESSION_TASK_ID }
        .sortedByDescending { if (it.updatedAtMs > 0L) it.updatedAtMs else it.timestampMs }
    }
  }

  override fun saveVoiceSession(session: ChatSessionProto) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        val sessions = userData.chatSessionsList
          .filterNot { it.sessionId == session.sessionId && (it.isVoiceSession || it.taskId == VOICE_SESSION_TASK_ID) }
          .toMutableList()
        sessions.add(session)
        userData.toBuilder().clearChatSessions().addAllChatSessions(sessions).build()
      }
    }
  }

  override fun deleteVoiceSession(sessionId: String) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        val sessions = userData.chatSessionsList.filterNot {
          it.sessionId == sessionId && (it.isVoiceSession || it.taskId == VOICE_SESSION_TASK_ID)
        }
        userData.toBuilder().clearChatSessions().addAllChatSessions(sessions).build()
      }
    }
  }

  override fun readMemories(): List<MemoryProto> {
    return runBlocking {
      userDataDataStore.data.first().memoriesList
        .filter { it.status == MemoryStatusProto.MEMORY_STATUS_CONFIRMED }
        .sortedByDescending { it.updatedAtMs }
    }
  }

  override fun saveMemory(memory: MemoryProto) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        val memories = userData.memoriesList
          .filterNot { it.id == memory.id }
          .toMutableList()
        memories.add(memory)
        userData.toBuilder().clearMemories().addAllMemories(memories).build()
      }
    }
  }

  override fun deleteMemory(memoryId: String) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        val memories = userData.memoriesList.filterNot { it.id == memoryId }
        userData.toBuilder().clearMemories().addAllMemories(memories).build()
      }
    }
  }

  override fun clearMemories() {
    runBlocking {
      userDataDataStore.updateData { userData -> userData.toBuilder().clearMemories().build() }
    }
  }

  override fun readMemoryEnabled(): Boolean {
    return runBlocking {
      val userData = userDataDataStore.data.first()
      if (userData.memoryEnabledConfigured) userData.memoryEnabled else true
    }
  }

  override fun saveMemoryEnabled(enabled: Boolean) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        userData.toBuilder()
          .setMemoryEnabled(enabled)
          .setMemoryEnabledConfigured(true)
          .build()
      }
    }
  }

  override fun saveSecret(key: String, value: String) {
    runBlocking {
      userDataDataStore.updateData { userData ->
        userData.toBuilder().putSecrets(key, value).build()
      }
    }
  }

  override fun readSecret(key: String): String? {
    return runBlocking { userDataDataStore.data.first().secretsMap[key] }
  }

  override fun deleteSecret(key: String) {
    runBlocking {
      userDataDataStore.updateData { userData -> userData.toBuilder().removeSecrets(key).build() }
    }
  }

  override fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long) {
    runBlocking {
      // Clear the entry in old data store.
      dataStore.updateData { settings ->
        settings.toBuilder().setAccessTokenData(AccessTokenData.getDefaultInstance()).build()
      }

      userDataDataStore.updateData { userData ->
        userData
          .toBuilder()
          .setAccessTokenData(
            AccessTokenData.newBuilder()
              .setAccessToken(accessToken)
              .setRefreshToken(refreshToken)
              .setExpiresAtMs(expiresAt)
              .build()
          )
          .build()
      }
    }
  }

  override fun clearAccessTokenData() {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().clearAccessTokenData().build() }
      userDataDataStore.updateData { userData ->
        userData.toBuilder().clearAccessTokenData().build()
      }
    }
  }

  override fun readAccessTokenData(): AccessTokenData? {
    return runBlocking {
      val userData = userDataDataStore.data.first()
      userData.accessTokenData
    }
  }

  override fun saveImportedModels(importedModels: List<ImportedModel>) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().clearImportedModel().addAllImportedModel(importedModels).build()
      }
    }
  }

  override fun readImportedModels(): List<ImportedModel> {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.importedModelList
    }
  }

  override fun isTosAccepted(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.isTosAccepted
    }
  }

  override fun acceptTos() {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setIsTosAccepted(true).build() }
    }
  }

  override fun isGemmaTermsOfUseAccepted(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.isGemmaTermsAccepted
    }
  }

  override fun acceptGemmaTermsOfUse() {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setIsGemmaTermsAccepted(true).build()
      }
    }
  }

  override fun getHasRunTinyGarden(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.hasRunTinyGarden
    }
  }

  override fun setHasRunTinyGarden(hasRun: Boolean) {
    runBlocking {
      dataStore.updateData { settings -> settings.toBuilder().setHasRunTinyGarden(hasRun).build() }
    }
  }

  override fun addCutout(cutout: Cutout) {
    runBlocking {
      cutoutDataStore.updateData { cutouts -> cutouts.toBuilder().addCutout(cutout).build() }
    }
  }

  override fun getAllCutouts(): List<Cutout> {
    return runBlocking { cutoutDataStore.data.first().cutoutList }
  }

  override fun setCutout(newCutout: Cutout) {
    runBlocking {
      cutoutDataStore.updateData { cutouts ->
        var index = -1
        for (i in 0..<cutouts.cutoutCount) {
          val cutout = cutouts.cutoutList.get(i)
          if (cutout.id == newCutout.id) {
            index = i
            break
          }
        }
        if (index >= 0) {
          cutouts.toBuilder().setCutout(index, newCutout).build()
        } else {
          cutouts
        }
      }
    }
  }

  override fun setCutouts(cutouts: List<Cutout>) {
    runBlocking {
      cutoutDataStore.updateData { CutoutCollection.newBuilder().addAllCutout(cutouts).build() }
    }
  }

  override fun setHasSeenBenchmarkComparisonHelp(seen: Boolean) {
    runBlocking {
      dataStore.updateData { settings ->
        settings.toBuilder().setHasSeenBenchmarkComparisonHelp(seen).build()
      }
    }
  }

  override fun getHasSeenBenchmarkComparisonHelp(): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.hasSeenBenchmarkComparisonHelp
    }
  }

  override fun addBenchmarkResult(result: BenchmarkResult) {
    runBlocking {
      benchmarkResultsDataStore.updateData { results ->
        results.toBuilder().addResult(0, result).build()
      }
    }
  }

  override fun getAllBenchmarkResults(): List<BenchmarkResult> {
    return runBlocking { benchmarkResultsDataStore.data.first().resultList }
  }

  override fun deleteBenchmarkResult(index: Int) {
    runBlocking {
      benchmarkResultsDataStore.updateData { results ->
        val newResults = results.toBuilder().removeResult(index).build()
        newResults
      }
    }
  }

  override fun addSkill(skill: Skill) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills = buildList {
          add(skill)
          addAll(skills.skillList)
        }
        skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
      }
    }
  }

  override fun setSkills(skills: List<Skill>) {
    runBlocking {
      skillsDataStore.updateData { curSkills ->
        curSkills.toBuilder().clearSkill().addAllSkill(skills).build()
      }
    }
  }

  override fun setSkillSelected(skill: Skill, selected: Boolean) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills =
          skills.skillList.map { curSkill ->
            if (curSkill.name == skill.name) {
              curSkill.toBuilder().setSelected(selected).setUserModifiedSelection(true).build()
            } else {
              curSkill
            }
          }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override fun setAllSkillsSelected(selected: Boolean) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills =
          skills.skillList.map { curSkill ->
            curSkill.toBuilder().setSelected(selected).setUserModifiedSelection(true).build()
          }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override fun getAllSkills(): List<Skill> {
    return runBlocking { skillsDataStore.data.first().skillList }
  }

  override fun deleteSkill(name: String) {
    runBlocking {
      skillsDataStore.updateData { skills ->
        val newSkills = skills.skillList.filter { it.name != name }
        Skills.newBuilder().addAllSkill(newSkills).build()
      }
    }
  }

  override suspend fun deleteSkills(names: Set<String>) {
    skillsDataStore.updateData { skills ->
      val newSkills = skills.skillList.filter { it.name !in names }
      skills.toBuilder().clearSkill().addAllSkill(newSkills).build()
    }
  }

  override fun addViewedPromoId(promoId: String) {
    runBlocking {
      dataStore.updateData { settings ->
        if (settings.viewedPromoIdList.contains(promoId)) {
          settings
        } else {
          settings.toBuilder().addViewedPromoId(promoId).build()
        }
      }
    }
  }

  override fun removeViewedPromoId(promoId: String) {
    runBlocking {
      dataStore.updateData { settings ->
        val newList = settings.viewedPromoIdList.filter { it != promoId }
        settings.toBuilder().clearViewedPromoId().addAllViewedPromoId(newList).build()
      }
    }
  }

  override fun hasViewedPromo(promoId: String): Boolean {
    return runBlocking {
      val settings = dataStore.data.first()
      settings.viewedPromoIdList.contains(promoId)
    }
  }
}
