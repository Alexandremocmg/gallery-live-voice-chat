# Kabem Voice Reliability Fixes

## Scope

This change hardens five existing voice flows without changing the assistant's product behavior:

1. A generated turn is only released after its in-memory conversation state is finalized.
2. The barge-in recorder releases microphone ownership before speech recognition starts.
3. Temporary PDF bitmaps are recycled on success, failure, cancellation, and capacity trimming.
4. Pronunciation recording owns and releases `AudioRecord` idempotently, including cancellation before its coroutine starts.
5. Media controls remain available while listening and cancel the current recognition without submitting partial speech.

## Design

Generation completion remains guarded by the existing generation epoch. The completion flag moves into the main-thread finalization block and is cleared in `finally`, after the assistant message and teaching state have been recorded.

Audio recorders use synchronized ownership helpers. Stopping barge-in detaches and releases its recorder synchronously; automatic voice detection invokes its callback only after cleanup. Recognition cancellation uses a discard flag so late `SpeechRecognizer` callbacks cannot submit a partial turn.

Rendered PDF pages are treated as temporary resources owned by the current inference. Every rendered page is tracked, unused pages are recycled immediately, and pages passed to inference are recycled exactly once when that inference reaches a terminal callback.

## Validation

- Existing bilingual, state-machine, VAD, and context-budget tests remain unchanged.
- Add focused tests for incomplete bilingual markers containing recoverable text where practical.
- Run voice unit tests and `assembleDebug`.
- Device validation should cover rapid barge-in, opening media while listening, repeated scanned-PDF questions, and repeated pronunciation start/cancel cycles.
