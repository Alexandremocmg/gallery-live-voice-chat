@echo off
setlocal
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not exist "%ADB%" set "ADB=adb"
%ADB% wait-for-device
%ADB% logcat -c
%ADB% shell am force-stop com.kabem.voice
%ADB% shell monkey -p com.kabem.voice 1 >nul
%ADB% logcat -v threadtime -s VoiceChatManager:D VoiceViewModel:D VoiceBargeIn:D TTS:D *:S
