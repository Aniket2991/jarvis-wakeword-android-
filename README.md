# JARVIS Wake Word Android — prototype

This is an experimental Android microphone-listening prototype, not a finished hands-free assistant.

## Microphone and privacy
- Listening starts only after the user opens the app, checks the microphone disclosure, and grants the Android microphone permission.
- A persistent notification is displayed while the service runs. Use its **Stop listening** action or the app's Stop button to stop it.
- The app uses Android SpeechRecognizer. It is not a dedicated offline wake-word detector; depending on the speech provider and phone settings, audio may be processed by that provider.
- The app does not currently send recognised commands to the JARVIS website, automatically launch the assistant, or perform commands.
- Android and phone-vendor battery/background limits may interrupt listening. Force-stop, reboot, or a restricted speech provider can also stop it.

## Build debug APK
1. Open the repository's Actions tab.
2. Open the completed **Build JARVIS Wake Word APK** run.
3. Download the `jarvis-wakeword-debug-apk` artifact and extract `app-debug.apk`.

## Signed release build
The workflow builds a signed release APK only when these four GitHub repository Actions secrets are configured:
- `JARVIS_KEYSTORE_BASE64`: base64-encoded Android keystore file
- `JARVIS_KEYSTORE_PASSWORD`
- `JARVIS_KEY_ALIAS`
- `JARVIS_KEY_PASSWORD`

Create and retain your own keystore privately. Never commit or publish the keystore or its passwords. If these secrets are not configured, the workflow builds only the debug APK.

## Test plan
1. Install the APK and open the app.
2. Read and accept the microphone notice; grant the requested permissions.
3. Start listening and verify the ongoing notification appears.
4. Switch to another app and say “Jarvis”; check whether the notification updates.
5. Press **Stop listening** in the notification and verify the notification disappears.
6. Repeat with battery saver and screen locked, noting that reliability is not guaranteed.

## Play Protect
A successful build or a release signature does not guarantee that Google Play Protect will stop showing a warning. Do not disable Play Protect globally. If it continues to flag the app, stop installing it and review the warning/report it for review.

## Next engineering step
For reliable, low-power wake-word detection, replace SpeechRecognizer with a dedicated compatible on-device keyword-spotting engine and then implement an explicit wake-to-command flow. Review third-party model and SDK licenses before bundling.
