package com.jarvis.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale

/**
 * Prototype only: Android SpeechRecognizer is not a true always-on, offline keyword spotter.
 * It can stop in background, depends on the device speech provider, and may send audio for recognition.
 */
class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var restarting = false
    private val channelId = "jarvis_wake_listener"
    private val notificationId = 71

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification("Listening service active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startRecognition()
        return START_STICKY
    }

    private fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable on this phone")
            stopSelf()
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { updateNotification("Listening for “Jarvis”") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { scheduleRestart(350) }
                override fun onError(error: Int) { scheduleRestart(900) }
                override fun onResults(results: Bundle?) {
                    val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any { it.lowercase(Locale.ROOT).contains("jarvis") }) {
                        updateNotification("Wake word detected — open JARVIS to continue")
                        handler.post { Toast.makeText(this@WakeWordService, "Jarvis detected. Open your JARVIS assistant.", Toast.LENGTH_LONG).show() }
                    }
                    scheduleRestart(500)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val phrases = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any { it.lowercase(Locale.ROOT).contains("jarvis") }) {
                        updateNotification("“Jarvis” detected — open JARVIS to continue")
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
        } catch (_: Exception) {
            scheduleRestart(1500)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "JARVIS wake listening", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("JARVIS Wake Word").setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(notificationId, buildNotification(message))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
