# JARVIS Wake Word Android — prototype

Starter Android project for experimenting with saying “Jarvis” while another app is in use.

## Important limitations
- Uses Android SpeechRecognizer, not a dedicated local wake-word spotter.
- Not guaranteed to run continuously, survive force-stop, work on a locked screen, or work on every Android device.
- Speech recognition may use the device's online speech provider. This is not an offline/private wake-word detector.
- Detecting “Jarvis” currently updates the notification and shows a toast; it does not yet open the existing web assistant or capture a follow-up command.
- Android 12+ microphone foreground-service rules require the service to be started while the app is visible; battery optimization and OEM background limits can interrupt it.

## Build APK without Android Studio
1. Open the repository's Actions tab and run “Build JARVIS Wake Word APK”, or push to main.
2. Open the completed workflow run and download artifact “jarvis-wakeword-debug-apk”.
3. Extract app-debug.apk, install it on your Android phone, and grant microphone permission.

## Test
1. Open the app and tap Start wake listening.
2. Keep its ongoing notification visible.
3. Switch to another app and say “Jarvis”.
4. Check whether the notification changes to “Wake word detected”.
5. Test with screen locked, battery saver, and after reboot separately; failures are expected on some phones.

## Next engineering step
For reliable low-power detection, replace SpeechRecognizer with a dedicated on-device keyword-spotting engine (for example, a compatible sherpa-onnx KWS model), then add a deliberate user action to launch the assistant and a foreground command-capture flow. Review third-party model and SDK licenses before bundling.
