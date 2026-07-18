# Kabem Voice Bilingual Conversation Implementation Plan

**Date:** 2026-07-18
**Status:** Ready for implementation
**Design:** `docs/superpowers/specs/2026-07-18-bilingual-conversation-design.md`

## Goal

Route every voice turn through one deterministic Portuguese/English language decision so recognition, prompting and TTS agree. Normal turns use one language; English Teacher responses may use separated Portuguese and English segments with the correct local voice.

## Baseline

Before editing, run:

```powershell
cd Android/src
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected baseline: 90 unit tests and a debug APK. Preserve the current continuous-conversation fix in `VoiceViewModel`: speech recognition `Processing` is not model generation.

## Phase 1: Language Decision Core

### Task 1.1: Add language decision models

**Create:**

- `Android/src/app/src/main/java/com/google/ai/edge/gallery/voice/language/LanguageTurnModels.kt`

Define:

```kotlin
enum class ResponseLanguageMode {
  SINGLE_PORTUGUESE,
  SINGLE_ENGLISH,
  BILINGUAL_TEACHING,
}

enum class LanguageConfidence { LOW, MEDIUM, HIGH }

enum class LanguageDecisionSource {
  EXPLICIT_COMMAND,
  ENGLISH_TEACHER,
  ANDROID_DETECTION,
  TEXT_ANALYSIS,
  CONVERSATION_CONTEXT,
  DEFAULT,
}

data class LanguageTurnDecision(
  val inputLocale: SpeechLocale,
  val responseMode: ResponseLanguageMode,
  val responseLocale: SpeechLocale,
  val confidence: LanguageConfidence,
  val source: LanguageDecisionSource,
)
```

Keep Android framework confidence constants outside these domain types.

### Task 1.2: Implement deterministic transcript analysis

**Create:**

- `voice/language/TextLanguageResolver.kt`
- `app/src/test/java/com/google/ai/edge/gallery/voice/language/TextLanguageResolverTest.kt`

The resolver scores Portuguese and English using normalized tokens, function words, contractions and high-signal forms. It returns `UNKNOWN` when evidence is weak.

Rules:

- fewer than two strong tokens cannot switch an established language;
- names, acronyms, URLs, code and isolated borrowed words are neutral;
- Portuguese diacritics add evidence but are not required;
- English apostrophe contractions add English evidence;
- score ties inherit the previous locale.

Test at least:

- `quero entender como isso funciona` -> Portuguese;
- `can you explain how this works` -> English;
- `okay`, `sim`, `API`, `Gemma` -> ambiguous;
- mixed technical sentence inherits context;
- accents and punctuation do not change the result.

### Task 1.3: Implement the turn resolver

**Create:**

- `voice/language/LanguageTurnResolver.kt`
- `app/src/test/java/com/google/ai/edge/gallery/voice/language/LanguageTurnResolverTest.kt`

Inputs:

- final transcript;
- Android detected locale and confidence;
- previous established locale;
- selected English dialect;
- `EnglishLessonDecision`;
- whether an explicit language command was detected.

Precedence must match the design specification. Add tests for every precedence edge, especially explicit commands overriding Android detection and English Teacher overriding normal single-language response mode.

**Gate:**

```powershell
./gradlew.bat :app:testDebugUnitTest --tests '*TextLanguageResolverTest' --tests '*LanguageTurnResolverTest'
```

## Phase 2: Android Recognition Routing

### Task 2.1: Capture language detection correctly

**Modify:**

- `voice/domain/VoiceChatManager.kt`
- `voice/language/SpeechRecognitionResult.kt`

Add domain-safe language detection metadata to `SpeechRecognitionResult`. In `VoiceChatManager`:

- implement `RecognitionListener.onLanguageDetection` on API 34+;
- retain detected locale and confidence only for the active recognition session;
- clear retained detection when listening starts, is cancelled or fails;
- stop reading `SpeechRecognizer.DETECTED_LANGUAGE` from `onResults`;
- attach the retained metadata to the final result.

Do not retain stale language data across turns.

### Task 2.2: Build recognition requests from capabilities

**Create:**

- `voice/language/RecognitionLanguagePolicy.kt`
- `app/src/test/java/com/google/ai/edge/gallery/voice/language/RecognitionLanguagePolicyTest.kt`

**Modify:**

- `voice/domain/VoiceChatManager.kt`
- `voice/presentation/VoiceViewModel.kt`

Replace the loose `locale + allowBilingualSwitch` pair with a request object containing:

- primary locale;
- allowed locales;
- detection enabled;
- switching enabled;
- switching sensitivity.

Policy:

- API 34+ and both offline packs ready: balanced PT/EN switching;
- API 34+ with one pack missing: single-locale recognition plus language detection when supported;
- API 31-33: established single locale;
- pronunciation repetition: forced English, no switching;
- English Teacher explanation input: established Portuguese unless the activity expects English.

Keep `EXTRA_PREFER_OFFLINE = true`. Never create two recognizers or microphone recorders concurrently.

### Task 2.3: Improve recognizer diagnostics

Map language-switch errors separately from audio, network and no-match errors. A missing English pack should lead to the existing local pack action, not a generic recognition failure.

**Gate:** compile unit tests and verify the generated recognition policy for API 31, 33 and 34.

## Phase 3: Integrate Language Decisions into Voice Turns

### Task 3.1: Resolve language once per turn

**Modify:**

- `voice/presentation/VoiceViewModel.kt`

Add one `LanguageTurnResolver` instance and one established conversation locale. In `generateResponse`:

1. obtain `EnglishLessonDecision`;
2. resolve `LanguageTurnDecision` once;
3. update `_activeSpeechLocale` from the decision;
4. pass the same decision to prompt construction and response segmentation;
5. update the established locale only with high-confidence or explicit decisions.

Do not let `EnglishLessonOrchestrator`, prompt builders and TTS independently guess the language.

### Task 3.2: Select the next microphone locale

Normal conversation listens in the established locale and requests PT/EN switching when supported. English Teacher activities retain their explicit next-input behavior. Short follow-ups inherit the established locale.

Add focused tests around a pure coordinator or policy rather than constructing the full Android `ViewModel` in unit tests.

## Phase 4: Centralize the Prompt Language Contract

### Task 4.1: Add a language instruction builder

**Create:**

- `voice/language/LanguageInstructionBuilder.kt`
- `app/src/test/java/com/google/ai/edge/gallery/voice/language/LanguageInstructionBuilderTest.kt`

Output exactly one internal language block:

- Portuguese: answer only in Brazilian Portuguese;
- English: answer only in the selected dialect;
- bilingual teaching: Portuguese explanations and marked English demonstrations.

### Task 4.2: Remove duplicate language authority

**Modify:**

- `voice/language/EnglishTeachingPromptBuilder.kt`
- `voice/presentation/VoiceViewModel.kt`

`EnglishTeachingPromptBuilder` remains responsible for pedagogy, expected phrases and pronunciation instructions. `LanguageInstructionBuilder` becomes the sole authority for response language and marker requirements.

Add the language block independently from response depth, cognitive mode, skills and PDF context.

## Phase 5: Validate Streaming Output Before TTS

### Task 5.1: Add a bilingual speech segmenter

**Create:**

- `voice/language/BilingualSpeechSegmenter.kt`
- `app/src/test/java/com/google/ai/edge/gallery/voice/language/BilingualSpeechSegmenterTest.kt`

Reuse `BilingualResponseParser` for marker parsing, then:

- buffer until a stable sentence or phrase boundary;
- validate the buffered text with `TextLanguageResolver`;
- retag only when evidence strongly contradicts the current locale;
- inherit the expected response locale for ambiguous chunks;
- merge adjacent chunks with the same locale;
- preserve every visible character while removing internal markers.

An isolated English word in Portuguese does not switch voice. A complete English example does.

### Task 5.2: Replace parser-only streaming integration

**Modify:**

- `voice/presentation/VoiceViewModel.kt`

Replace direct `BilingualResponseParser` consumption with `BilingualSpeechSegmenter`. Keep sentence-level streaming so the first spoken response does not wait for full generation.

Track generated English text from validated chunks for pronunciation exercises.

### Task 5.3: Preserve TTS voice safety

**Modify only if necessary:**

- `voice/domain/VoiceChatManager.kt`

The current local voice catalog and per-chunk `setVoice` behavior should remain. Add assertions or extracted policy tests proving:

- no network voice is selected;
- English never falls back to Portuguese;
- Portuguese never falls back to English;
- missing voice data skips synthesis and publishes an installation notice;
- queue order remains correct across PT -> EN -> PT.

## Phase 6: Persist Conversation Language

### Task 6.1: Extend the session schema

**Modify:**

- `Android/src/app/src/main/proto/chat_history.proto`
- `voice/presentation/VoiceViewModel.kt`
- relevant conversation restoration tests

Add a backward-compatible field:

```proto
string voice_conversation_locale = 22;
```

Persist the established locale with each voice session. Restore it before the next listening turn. Legacy sessions with an empty field default to Portuguese unless active English Teacher state already establishes English.

Starting a new session resets the field to Portuguese.

## Phase 7: UI Feedback and Offline Readiness

### Task 7.1: Clarify the existing language indicator

**Modify:**

- `voice/presentation/LiveChatScreen.kt`

Reuse the current locale chip. It should show the established listening language (`PT`, `EN-US` or `EN-GB`) without adding a permanent language selector. During automatic switching, a short secondary state may show `PT/EN automatico`.

### Task 7.2: Surface missing local resources

Keep the existing English pack action. Ensure notices distinguish:

- recognition pack missing;
- local TTS voice missing;
- automatic language switching unsupported;
- general speech recognition unavailable.

No notice should claim offline bilingual readiness unless both recognition packs and both local voices are available.

## Phase 8: Validation and Documentation

### Task 8.1: Run the complete unit suite

```powershell
cd Android/src
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected result: all existing tests plus the new language tests pass, and `app-debug.apk` is generated.

### Task 8.2: Device validation

Run the specification's device matrix. Capture logs for:

- `onLanguageDetection` locale and confidence;
- final resolver decision and source;
- recognition request mode;
- each validated TTS chunk locale;
- missing-pack and missing-voice paths.

Logs must not contain raw audio or persisted private transcript content in release builds.

### Task 8.3: Update documentation

Update:

- `README.md` bilingual conversation description;
- `docs/superpowers/plans/2026-07-15-bilingual-english-teacher-implementation.md` with the new shared language router;
- this plan with final test counts and device results.

## Phase 8 Results — 2026-07-18

### Automated validation

- `:app:testDebugUnitTest`: **149 tests, 0 failures, 0 errors, 0 skipped**.
- `:app:assembleDebug`: successful.
- APK: `Android/src/app/build/outputs/apk/debug/app-debug.apk`.

The suite covers language resolution, recognition policy, bilingual streaming segmentation, TTS safety and queue order, session language persistence, readiness notices and the existing voice features.

### Diagnostic and privacy audit

The required diagnostic events are available without logging conversation content:

| Event | Safe metadata |
|---|---|
| Recognition request | primary locale, detection enabled, switching enabled |
| Android language detection | detected locale, confidence, switching result |
| Final language decision | input locale, response locale, mode, source, confidence |
| Validated TTS chunk | locale, character count, speech rate |
| Missing resources | locale, pack status or local voice availability |

No diagnostic event writes raw audio, partial model output, recognized transcript, persisted message content or the text sent to TTS. Audio remains temporary and is released through the existing recorder lifecycle.

### Device validation

**Status: pending.** `adb devices -l` returned no connected devices on 2026-07-18, so no physical result was inferred or marked as passed. The specification's matrix must still be run on:

- Android 14+ with both offline recognition packs;
- Android 14+ without the English pack;
- Android 12 or 13 without platform language switching;
- separate Portuguese and English local TTS voice configurations;
- Portuguese/English explicit switches, English Teacher activities and barge-in in both languages.

## Post-Validation Fixes — 2026-07-18

### Marker-free inline English TTS

Additional regression tests were added for Portuguese teaching responses that contain English practice phrases without explicit `[[en-*]]` markers. `BilingualSpeechSegmenter` now splits common cues such as `repita:`, `diga:`, `fale`, `em inglês` and quoted examples so a response like `Agora repita: Good morning. Depois diga: Thank you.` is spoken as PT -> EN -> PT -> EN instead of entirely with the Portuguese voice.

Validation run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.google.ai.edge.gallery.voice.language.BilingualSpeechSegmenterTest
./gradlew.bat :app:testDebugUnitTest --tests *voice*
./gradlew.bat :app:assembleDebug
```

All commands completed with `BUILD SUCCESSFUL`.

### English-practice ASR routing

A second regression covered pronunciation/repetition turns where the assistant demonstrates an English phrase and then expects the learner to repeat it. `EnglishLessonOrchestrator` now moves pronunciation requests into `EnglishActivity.REPEAT`, and `EnglishResponseStateUpdater` promotes captured English targets after the response so the next listening locale is the selected English dialect (`EN-US` or `EN-GB`) rather than `pt-BR`.

Validation run:

```powershell
./gradlew.bat :app:testDebugUnitTest `
  --tests com.google.ai.edge.gallery.voice.language.EnglishResponseStateUpdaterTest `
  --tests com.google.ai.edge.gallery.voice.language.EnglishLessonOrchestratorTest `
  --tests com.google.ai.edge.gallery.voice.language.RecognitionLanguagePolicyTest
./gradlew.bat :app:testDebugUnitTest --tests *voice*
./gradlew.bat :app:assembleDebug
```

All commands completed with `BUILD SUCCESSFUL`. Physical-device validation is still required to confirm installed Android recognition packs and local TTS voices on the target phone.

## Implementation Order

1. Phase 1: pure language core and tests.
2. Phase 2: Android recognition metadata and policy.
3. Phase 3: single turn decision in `VoiceViewModel`.
4. Phase 4: centralized prompt contract.
5. Phase 5: validated streaming TTS segmentation.
6. Phase 6: persistence.
7. Phase 7: UI readiness notices.
8. Phase 8: complete validation and docs.

Do not combine phases into one large edit. Each phase should compile and pass its focused tests before the next begins.
