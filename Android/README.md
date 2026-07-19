# Google AI Edge Gallery (Android)

## Kabem Voice Android notes

This Android app includes the Kabem Voice live conversation surface. The current `feat/live-voice-chat` work focuses on local, private Portuguese/English voice interaction:

- Voice and keyboard share the same conversation timeline.
- Portuguese and English turns are routed through one language decision so ASR, prompting and TTS stay aligned.
- English Teacher output may mix Portuguese explanations with English examples, but each spoken chunk is validated and sent to the correct local voice.
- Android 14+ automatic PT/EN recognition switching is enabled only when both offline recognition packs are confirmed ready.
- Missing recognition packs and missing local TTS voices are surfaced as readiness actions instead of silently falling back to the wrong language.
- The app refreshes readiness when returning from Android speech/TTS settings so newly installed packs or voices are detected without restarting.
- Barge-in and end-of-turn timing are conservative for short English demonstrations to avoid interrupting the teacher with echo or noise.

## Local validation

From `Android/src`:

```bat
gradlew.bat :app:testDebugUnitTest --tests *voice*
gradlew.bat :app:assembleDebug
```

A full local validation of the current voice package has completed with `BUILD SUCCESSFUL`. Physical-device validation is still required for installed Android recognition packs and local TTS voices because those are device settings, not repository state.
