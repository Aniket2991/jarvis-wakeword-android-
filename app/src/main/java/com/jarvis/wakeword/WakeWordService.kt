package com.jarvis.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.Toast
import com.openwakeword.OpenWakeWord

/**
 * Experimental on-device wake-word service.
 *
 * Wake-word inference is handled locally by openWakeWord Android. Opening the
 * assistant website still requires a browser/network connection. Android may
 * block launching an activity from a background service, so the notification
 * includes an explicit Open JARVIS action.
 */
class WakeWordService : Service() {
    private var detector: OpenWakeWord? = null
    private var lastAssistantLaunchAttempt = 0L
    private val channelId = "jarvis_wake_listener"
    private val notificationId = 71

    companion object {
        const val ACTION_STOP = "com.jarvis.wakeword.ACTION_STOP_LISTENING"
        private const val ASSISTANT_URL = "https://jarvis-web-assistant-five.vercel.app"
        private const val LAUNCH_DEBOUNCE_MS = 10_000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification("Preparing offline wake-word detector"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListeningAndService()
            return START_NOT_STICKY
        }
        if (detector == null) startOfflineDetection()
        // User must explicitly restart listening if Android stops the service.
        return START_NOT_STICKY
    }

    private fun startOfflineDetection() {
        try {
            val wakeWordDetector = OpenWakeWord.Builder(this)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(0.5f)
                .setDebounceMs(LAUNCH_DEBOUNCE_MS)
                .build()
            detector = wakeWordDetector
            updateNotification("Offline detector active • Say “Hey Jarvis”")
            wakeWordDetector.start {
                onWakeWordDetected()
            }
        } catch (_: SecurityException) {
            detector?.release()
            detector = null
            updateNotification("Microphone permission unavailable • Open the app to review permissions")
        } catch (_: Exception) {
            detector?.release()
            detector = null
            updateNotification("Offline detector could not start • Check microphone permission and reopen the app")
        }
    }

    private fun onWakeWordDetected() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAssistantLaunchAttempt < LAUNCH_DEBOUNCE_MS) return
        lastAssistantLaunchAttempt = now

        updateNotification("“Hey Jarvis” detected • Tap Open JARVIS if Android blocks automatic opening")
        try {
            Toast.makeText(
                this@WakeWordService,
                "Hey Jarvis detected. Trying to open the assistant.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            // Notification action remains available even if a toast cannot be shown.
        }

        try {
            val assistantIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ASSISTANT_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(assistantIntent)
        } catch (_: Exception) {
            updateNotification("“Hey Jarvis” detected • Tap Open JARVIS to open the assistant")
        }
    }

    private fun stopListeningAndService() {
        detector?.stop()
        detector?.release()
        detector = null
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
        detector?.stop()
        detector?.release()
        detector = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
