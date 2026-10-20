# JARVIS Wake Word Android — experimental prototype (0.6.0)

This app uses the openWakeWord Android library to detect the built-in **“Hey Jarvis”** wake phrase locally on the device. Wake-word inference does not require an API key or send audio to a remote speech-recognition service. Opening the JARVIS website still needs a browser and internet connection.

## How to use it
1. Install a tested build only if Play Protect allows it.
2. Open **JARVIS Wake Word**, read the microphone/privacy disclosure, and tick the acknowledgement box.
3. Grant microphone and notification permissions, then tap **Start wake listening**.
4. The ongoing notification should say **Offline detector active** when the detector starts.
5. Say **“Hey Jarvis”**. The app attempts to open the existing assistant website. If Android blocks a background launch, tap **Open JARVIS** in the notification.
6. To stop microphone listening, tap **Stop listening** in the notification or the app.
7. If listening stops in the background, use **Battery and app settings** to review battery/background restrictions if your Android version offers those options.

## Important limitations
- This is an experimental integration. A successful CI build does not prove wake-word accuracy or reliable behavior on every phone; real-device testing is still required.
- The bundled pre-trained openWakeWord models have separate **CC BY-NC-SA 4.0** licensing terms. Review those terms before commercial use or redistribution. The library code itself is Apache-2.0.
- Android background execution and battery policies can interrupt microphone listening, especially with the screen off. This build is not guaranteed to listen continuously or survive force-stop/reboot.
- Android may block opening a browser from a background service. The notification's **Open JARVIS** action is the fallback.
- Detecting “Hey Jarvis” only opens the assistant website; it does not pass the wake phrase or subsequent spoken commands into the website or execute commands.
- This prototype has not been verified as Play Protect-approved.

## Version 0.6.0 changes
- Integrated the openwakeword-android on-device detector using its built-in HEY_JARVIS model.
- Replaced repeated Android SpeechRecognizer cycling in the foreground service with local wake-word inference.
- Kept explicit microphone disclosure, foreground notification actions, stop control, and the existing assistant URL.
- Added the JitPack repository for the library dependency.

## Build and verification
The GitHub Actions workflow runs Android lint and builds both debug and release variants. It verifies APK signatures and checks that the release variant is not debuggable.

Without owner release-signing secrets, CI produces a debug APK and a minified non-debuggable release preview signed with the CI runner's temporary debug certificate. This preview is for first-install testing only; it is not suitable for distribution or in-place updates.

For permanent release signing, create and retain your own keystore privately, then add these repository Actions secrets: JARVIS_KEYSTORE_BASE64, JARVIS_KEYSTORE_PASSWORD, JARVIS_KEY_ALIAS, and JARVIS_KEY_PASSWORD. Never commit or publish the keystore or passwords.

## Play Protect
A successful build, a clean general scan, or a release signature does not guarantee that Google Play Protect will stop warning about this package. If it says **“Harmful app blocked”**, do not bypass the warning or turn Play Protect off. Review the exact APK and use Google's official developer-warning review process if the app is believed to be incorrectly classified.