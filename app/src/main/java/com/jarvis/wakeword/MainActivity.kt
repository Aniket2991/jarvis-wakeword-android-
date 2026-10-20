package com.jarvis.wakeword

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCode = 7
    private lateinit var privacyConsent: CheckBox
    private lateinit var diagnosticsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 40)
        }

        layout.addView(TextView(this).apply {
            text = "JARVIS Wake Word"
            textSize = 26f
        })

        layout.addView(TextView(this).apply {
            text = "Version 0.5.0 • Experimental voice trigger"
            textSize = 14f
            setPadding(0, 8, 0, 20)
        })

        layout.addView(TextView(this).apply {
            text = "MICROPHONE & PRIVACY"
            textSize = 16f
        })

        layout.addView(TextView(this).apply {
            text = "Android SpeechRecognizer checks recognized speech for “Jarvis”. On-device recognition is preferred when available; otherwise, your speech provider may process audio online. This is not a dedicated low-power wake-word engine. Detection can be delayed or missed, and Android may stop listening in the background. When detected, the app attempts to open your JARVIS assistant. If Android blocks that, tap Open JARVIS in the persistent notification."
            textSize = 15f
            setPadding(0, 12, 0, 12)
        })

        privacyConsent = CheckBox(this).apply {
            text = "I understand microphone listening and speech recognition may process audio."
            textSize = 15f
        }
        layout.addView(privacyConsent)

        layout.addView(Button(this).apply {
            text = "Start wake listening"
            setOnClickListener {
                if (!privacyConsent.isChecked) {
                    Toast.makeText(this@MainActivity, "Please read and accept the microphone notice first.", Toast.LENGTH_LONG).show()
                } else {
                    requestPermissionsThenStart()
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Open JARVIS assistant"
            setOnClickListener { openAssistant() }
        })

        layout.addView(Button(this).apply {
            text = "Check phone readiness"
            setOnClickListener { showDiagnostics() }
        })

        layout.addView(Button(this).apply {
            text = "Battery and app settings"
            setOnClickListener { openAppSettings() }
        })

        diagnosticsText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 12, 0, 12)
            text = "Phone readiness has not been checked yet."
        }
        layout.addView(diagnosticsText)

        layout.addView(Button(this).apply {
            text = "Stop listening"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WakeWordService::class.java))
                Toast.makeText(this@MainActivity, "Stop requested.", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (::diagnosticsText.isInitialized) showDiagnostics()
    }

    private fun showDiagnostics() {
        val microphoneGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(this)
        val onDeviceAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(packageName)

        diagnosticsText.text = buildString {
            appendLine("PHONE READINESS")
            appendLine("Microphone permission: ${if (microphoneGranted) "Granted" else "Needed"}")
            appendLine("Notifications: ${if (notificationsGranted) "Allowed" else "Not allowed"}")
            appendLine("Speech recognition: ${if (recognitionAvailable) "Available" else "Unavailable"}")
            appendLine("On-device speech recognition: ${if (onDeviceAvailable) "Available" else "Not reported as available"}")
            appendLine("Battery restriction exemption: ${if (batteryUnrestricted) "Already exempt" else "Not exempt"}")
            appendLine()
            append("Tip: if listening stops with the screen off, open Battery and app settings and choose an unrestricted/allow-background option if your phone offers it. This may improve reliability but cannot guarantee continuous listening.")
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Open Android Settings and find JARVIS Wake Word.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAssistant() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jarvis-web-assistant-five.vercel.app")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser is available to open JARVIS.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionsThenStart() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), requestCode)
        } else {
            startWakeService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == this.requestCode && results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) {
            startWakeService()
        } else {
            Toast.makeText(this, "Microphone and notification permissions are needed for visible listening.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startWakeService() {
        val intent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Wake listener started. Say Jarvis to try opening the assistant, or use the notification's Open JARVIS action.", Toast.LENGTH_LONG).show()
    }
}
