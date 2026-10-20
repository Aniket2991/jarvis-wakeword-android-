# JARVIS Wake Word Android — experimental prototype (0.5.0)

This app listens for the word **“Jarvis”** after the user starts it and grants microphone permission. It is an experimental voice trigger, not a finished always-on assistant.

## How to use it
1. Install a tested build only if Play Protect allows it.
2. Open **JARVIS Wake Word**, read the microphone/privacy disclosure, and tick the acknowledgement box.
3. Grant microphone and notification permissions, then tap **Start wake listening**.
4. Use **Check phone readiness** to review microphone, notification, speech-recognition, on-device recognition, and battery-restriction status.
5. If background listening is interrupted, use **Battery and app settings** and select an unrestricted/allow-background option if your Android version offers it.
6. Leave the ongoing notification visible. When recognition detects “Jarvis”, the app attempts to open the existing assistant website. If Android blocks a background launch, tap **Open JARVIS** in the notification.
7. To stop microphone listening, tap **Stop listening** in the notification or the app.

## Important limitations
- This uses Android's SpeechRecognizer API, not a dedicated low-power wake-word model. It may miss or misrecognise the trigger.
- It prefers Android on-device speech recognition when the device reports it is available. Otherwise, the selected speech provider may process audio online.
- Automatic opening is best-effort. Android restricts launching activities from a background service; the notification action remains the reliable fallback.
- Android background execution and battery policies can interrupt listening, especially with the screen off. This build is not guaranteed to listen continuously or survive a force-stop/reboot.
- Repeated recognition errors pause listening instead of retrying indefinitely.
- The app does not forward spoken commands into the website or execute them.

## Version 0.5.0 changes
- Added a phone-readiness diagnostic for permissions, recognizer availability, on-device recognition availability, and battery exemption status.
- Added a shortcut to Android app settings to help the user review battery/background restrictions.
- Kept explicit microphone disclosure, persistent notification actions, cleartext network disabled, and a non-debuggable minified release variant.

## Build and verification
The GitHub Actions workflow runs Android lint and builds both debug and release variants. It verifies APK signatures and checks that the release variant is not debuggable.

- `jarvis-wakeword-debug-apk` is a development/testing build, not the recommended distribution build.
- If owner-controlled release-signing secrets are configured, the workflow uploads `jarvis-wakeword-release-apk`, signed with your own release keystore.
- Otherwise, it uploads `jarvis-wakeword-release-preview-apk`, a minified non-debuggable preview signed with the CI runner's temporary debug certificate. This preview is for first-install testing only. The certificate changes on future runs, so it is not suitable for in-place updates or distribution.
- For a permanent release, create and retain your own keystore privately, then add these repository Actions secrets: `JARVIS_KEYSTORE_BASE64`, `JARVIS_KEYSTORE_PASSWORD`, `JARVIS_KEY_ALIAS`, and `JARVIS_KEY_PASSWORD`. Never commit or publish the keystore or passwords.

## Play Protect
A successful build, a clean general scan, or a release signature does not guarantee that Google Play Protect will stop warning about this package. If it says **“Harmful app blocked”**, do not bypass the warning or turn Play Protect off. Review the exact APK and use Google's official developer-warning review process if the app is believed to be incorrectly classified.

## Next step for a genuinely reliable wake word
Replace SpeechRecognizer cycling with a dedicated on-device keyword-spotting engine and then implement an explicit wake-to-command flow. Review model/SDK licences and validate behaviour on the target phone before distribution.
