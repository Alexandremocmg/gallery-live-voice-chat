# Kabem Voice Bilingual Conversation Design

**Date:** 2026-07-18
**Status:** Implemented with post-validation fixes for mixed TTS and English-practice ASR

## Goal

Make Portuguese and English voice conversations sound natural and predictable while remaining offline. In normal conversation, Kabem answers in the language spoken in the current turn. Mixed output is allowed only for the English Teacher flow, where Portuguese explanations and English examples are separated and spoken with the correct local voice.

## Current Failure Modes

1. `VoiceChatManager` reads `SpeechRecognizer.DETECTED_LANGUAGE` from `onResults`, but Android provides language detection through `RecognitionListener.onLanguageDetection` when detection is enabled.
2. Automatic recognition switching is enabled only during `EnglishActivity.FREE_CONVERSATION`, so ordinary conversation remains locked to the previous locale.
3. `BilingualResponseParser` trusts model markers such as `[[pt-BR]]` and `[[en-US]]`. If the model omits or delays a marker, English text can be sent to a Portuguese TTS voice, or the reverse.
4. Short or ambiguous utterances can make the conversation oscillate between languages without considering the previous turn.
5. Marker-free Portuguese teaching output can contain short English examples such as `Good morning` or `Thank you`; without inline segmentation those examples are spoken with the Portuguese voice.
6. Pronunciation prompts can demonstrate an English phrase but leave the next listening state in Portuguese, causing the learner's English repetition to be transcribed as Portuguese.

Android references:

- https://developer.android.com/reference/android/speech/RecognizerIntent
- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/tts/TextToSpeech

## Product Rules

1. Normal conversation is single-language per turn.
2. A Portuguese user turn receives a Portuguese response.
3. An English user turn receives an English response in the selected dialect.
4. English Teacher explanations may combine languages, but every spoken segment has one explicit locale.
5. Explicit commands such as "fale ingles" and "volte ao portugues" override automatic detection.
6. Short ambiguous turns inherit the established conversation language.
7. Missing language packs or voices never cause text to be spoken with a voice from the wrong language.
8. When the assistant asks the learner to repeat or pronounce an English phrase, the next microphone request is forced to the selected English dialect.
9. Detection, routing and TTS remain local. No network service is introduced.

## Architecture

### 1. Turn Language Decision

Create a `LanguageTurnResolver` that returns:

```kotlin
data class LanguageTurnDecision(
  val inputLocale: SpeechLocale,
  val responseMode: ResponseLanguageMode,
  val responseLocale: SpeechLocale,
  val confidence: LanguageConfidence,
  val source: LanguageDecisionSource,
)
```

`ResponseLanguageMode` has three values:

- `SINGLE_PORTUGUESE`
- `SINGLE_ENGLISH`
- `BILINGUAL_TEACHING`

Signals are evaluated in this order:

1. explicit language command;
2. English Teacher state and activity;
3. Android language detection with sufficient confidence;
4. deterministic analysis of the final transcript;
5. previous established conversation locale;
6. Portuguese default for a new session.

The resolver uses hysteresis. A short turn or a weak text score does not change the established language. A language change requires either an explicit command or strong evidence from the current turn.

### 2. Recognition Routing

On Android 14 and later:

- enable language detection for Portuguese and the selected English dialect;
- enable balanced automatic language switching only when both offline recognition packs are available;
- implement `RecognitionListener.onLanguageDetection` and retain the latest detected locale and confidence for the active recognition session;
- stop reading `DETECTED_LANGUAGE` from `onResults`;
- attach the retained detection metadata to `SpeechRecognitionResult`.

On Android 12 and 13, or when the recognizer ignores switching:

- select the recognizer locale from the established conversation language;
- allow explicit commands and English Teacher state to change the next listening locale;
- use transcript analysis as a post-recognition correction signal;
- do not run two `SpeechRecognizer` instances or two microphone recorders concurrently.

The existing Gemma audio path remains dedicated to pronunciation and explicit audio analysis. General-turn audio fallback is outside this change because simultaneous recording can conflict with `SpeechRecognizer` and increases latency.

### 3. Transcript Language Analysis

Create a local `TextLanguageResolver` for Portuguese and English. It uses deterministic evidence rather than another model call:

- common function words and contractions;
- Portuguese diacritics and high-signal word forms;
- English contractions and high-signal word forms;
- token count and score margin;
- previous locale for ambiguous text.

Names, acronyms, code, borrowed words and isolated tokens do not switch the conversation language by themselves.

### 4. Prompt Contract

Every turn receives one internal language instruction derived from `LanguageTurnDecision`:

- Portuguese: answer only in Brazilian Portuguese.
- English: answer only in the selected English dialect.
- English Teacher: explain in Portuguese and place English demonstrations in explicitly marked English segments.

The language instruction is independent from response depth, teaching depth and skill routing. It must not be inferred again in multiple prompt builders.

Normal single-language responses do not require markers. `BILINGUAL_TEACHING` requires a marker before every locale change.

### 5. Output Validation and TTS Segmentation

Replace marker-only trust with a `BilingualSpeechSegmenter` pipeline:

1. parse explicit locale markers;
2. buffer enough text to reach a sentence or stable phrase boundary;
3. validate each segment with `TextLanguageResolver`;
4. retag a segment when its text strongly contradicts its marker or default locale;
5. merge adjacent segments with the same locale;
6. enqueue one `SpeechChunk` per validated locale.

An isolated foreign word inside a sentence does not change the voice. A complete English example inside a Portuguese lesson does. The segmenter also handles marker-free teaching cues such as `repita:`, `diga:`, `fale`, `em inglês` and quoted practice phrases so common learner prompts still produce PT -> EN -> PT chunks.

`VoiceChatManager` continues selecting a local `Voice` per `SpeechChunk`. If no offline voice exists for the resolved locale, it preserves the text, skips incorrect synthesis and shows a clear installation notice.

### 6. Conversation State

Persist the last established conversation locale in `ChatSessionProto` as a new backward-compatible field. Restoring a session restores that locale before the next listening turn. English Teacher state remains separate and continues using its existing persisted fields.

Starting a new session resets the established locale to Portuguese. An explicit command such as "vamos conversar em ingles" establishes English for that session and is then persisted.

## Error Handling

- If language detection is unavailable, use transcript analysis and conversation context.
- If language switching is unsupported, retain the selected single recognizer locale and expose no false claim of automatic switching.
- If an English recognition pack is missing, show the existing offline pack action before an English listening turn.
- If a TTS voice is missing, never substitute a voice from another language.
- If the resolver remains uncertain, inherit the previous locale rather than alternate unpredictably.
- Recognition and synthesis errors remain visible but do not erase the session or its established locale.

## Testing

### Unit Tests

- Portuguese and English transcript scoring.
- Ambiguous short turns inherit the previous locale.
- Explicit language commands override history.
- English Teacher selects `BILINGUAL_TEACHING`.
- Marker-free English response is retagged to English before TTS.
- Marker-free Portuguese response is retagged to Portuguese before TTS.
- Portuguese explanation plus English example produces separate chunks.
- Acronyms and isolated borrowed words do not switch voices.
- Missing or incomplete markers preserve all visible text.
- Session locale persistence and backward-compatible restoration.

### Device Matrix

- Android 14+ with Portuguese and English offline packs installed.
- Android 14+ with the English pack missing.
- Android 12 or 13 without platform language switching.
- Portuguese conversation followed by an explicit English switch.
- English conversation followed by an explicit Portuguese switch.
- English Teacher explanation, example, repetition, pronunciation and free conversation.
- Marker-free Portuguese response containing inline English practice phrases.
- Pronunciation request followed by learner repetition, verifying the active recognizer locale is `EN-US` or `EN-GB`.
- Local Portuguese and English TTS voices installed separately.
- Barge-in during Portuguese and English playback.

## Non-Goals

- Detecting more than Portuguese and English.
- Switching voices for individual foreign words inside one sentence.
- Cloud speech recognition or cloud TTS.
- Running a second model call only to classify language.
- Recording every ordinary turn for Gemma audio inference.

## Acceptance Criteria

1. Portuguese text is never intentionally synthesized with an English voice, and English text is never intentionally synthesized with a Portuguese voice.
2. Normal conversation answers in the language of the current user turn.
3. Short ambiguous turns do not cause language oscillation.
4. English Teacher output uses separate, correct voices for explanations and demonstrations, including marker-free inline examples.
5. English pronunciation/repetition exercises listen in the selected English dialect after the assistant demonstrates the target phrase.
6. The complete flow works offline when both language packs and local voices are installed.
7. Existing response-depth, teaching, session, media and barge-in behavior remains functional.
