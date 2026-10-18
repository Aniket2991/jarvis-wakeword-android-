package com.jarvis.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale
import kotlin.math.min

/**
 * Experimental voice trigger, not a dedicated wake-word engine.
 *
 * SpeechRecognizer may use a remote provider unless on-device recognition is available.
 * A persistent notification provides an Open JARVIS fallback and a Stop action.
 * Android may block automatic activity launches while this service is in the background.
 */
class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var restarting = false
    private var consecutiveErrors = 0
    private var lastAssistantLaunchAttempt = 0L
    private val channelId = "jarvis_wake_listener"
    private val notificationId = 71

    companion object {
        const val ACTION_STOP = "com.jarvis.wakeword.ACTION_STOP_LISTENING"
        private const val ASSISTANT_URL = "https://jarvis-web-assistant-five.vercel.app"
        private const val MAX_CONSECUTIVE_ERRORS = 8
        private const val LAUNCH_DEBOUNCE_MS = 10_000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification("Starting microphone listener"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListeningAndService()
            return START_NOT_STICKY
        }
        if (recognizer == null && !restarting) startRecognition()
        // Never restart microphone listening automatically after Android stops the service.
        return START_NOT_STICKY
    }

    private fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            pauseRecognition("Speech recognition unavailable • Open the app to try again")
            return
        }

        try {
            recognizer?.destroy()
            recognizer = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateNotification("Microphone active • Listening for “Jarvis”")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        pauseRecognition("Microphone permission unavailable • Open the app to review permissions")
                        return
                    }

                    consecutiveErrors += 1
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        pauseRecognition("Listening paused after repeated recognition errors • Open the app to restart")
                        return
                    }

                    val exponent = (consecutiveErrors - 1).coerceAtMost(5)
                    val backoff = min(1_000L * (1L shl exponent), 30_000L)
                    scheduleRestart(backoff)
                }

                override fun onResults(results: Bundle?) {
                    consecutiveErrors = 0
                    val phrases = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    if (phrases.any { it.containsWakeWord() }) {
                        onWakeWordDetected()
                    } else {
                        updateNotification("Microphone active • Listening for “Jarvis”")
                    }
                    scheduleRestart(700)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val phrases = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    if (phrases.any { it.containsWakeWord() }) {
                        onWakeWordDetected()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            recognizer?.startListening(request)
        } catch (_: SecurityException) {
            pauseRecognition("Microphone permission unavailable • Open the app to review permissions")
        } catch (_: Exception) {
            consecutiveErrors += 1
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                pauseRecognition("Listening paused after repeated errors • Open the app to restart")
            } else {
                scheduleRestart(2_000L)
            }
        }
    }

    private fun String.containsWakeWord(): Boolean =
        lowercase(Locale.ROOT).contains("jarvis")

    private fun onWakeWordDetected() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAssistantLaunchAttempt < LAUNCH_DEBOUNCE_MS) return
        lastAssistantLaunchAttempt = now

        updateNotification("“Jarvis” detected • Opening assistant; tap Open JARVIS if Android blocks it")
        handler.post {
            Toast.makeText(
                this@WakeWordService,
                "Jarvis detected. Trying to open the assistant.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Best-effort only: Android can block activities launched from a background service.
        // The persistent notification's Open JARVIS action remains the supported fallback.
        try {
            val assistantIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ASSISTANT_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(assistantIntent)
        } catch (_: Exception) {
            updateNotification("“Jarvis” detected • Tap Open JARVIS to open the assistant")
        }
    }

    private fun scheduleRestart(delay: Long) {
        if (restarting) return
        restarting = true
        handler.postDelayed({
            restarting = false
            if (recognizer != null) startRecognition()
        }, delay)
    }

    private fun pauseRecognition(message: String) {
        handler.removeCallbacksAndMessages(null)
        restarting = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        updateNotification(message)
    }

    private fun stopListeningAndService() {
        handler.removeCallbacksAndMessages(null)
        restarting = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(notificationId)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JARVIS microphone listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible indicator and Stop control while JARVIS wake-word listening is active."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val stopIntent = Intent(this, WakeWordService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 72, stopIntent, flags)
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(this, 73, openIntent, flags)
        val assistantIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ASSISTANT_URL))
        val assistantPendingIntent = PendingIntent.getActivity(this, 74, assistantIntent, flags)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("JARVIS Wake Word")
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_view, "Open JARVIS", assistantPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopPendingIntent)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(notificationId, buildNotification(message))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
