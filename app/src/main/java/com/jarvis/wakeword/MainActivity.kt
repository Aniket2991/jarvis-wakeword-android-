package com.jarvis.wakeword

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCode = 7
    private lateinit var privacyConsent: CheckBox

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
            text = "Prototype • Microphone use is visible and can be stopped at any time."
            textSize = 15f
            setPadding(0, 16, 0, 24)
        })

        layout.addView(TextView(this).apply {
            text = "MICROPHONE & PRIVACY"
            textSize = 16f
        })

        layout.addView(TextView(this).apply {
            text = "When you start listening, this app uses Android SpeechRecognizer to listen for the word “Jarvis”. This is not an offline keyword detector. Depending on your phone and speech-provider settings, audio may be sent to that provider for recognition. The app does not currently send commands to your JARVIS website or automatically execute them. A permanent notification stays visible while listening."
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
            text = "Stop listening"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WakeWordService::class.java))
                Toast.makeText(this@MainActivity, "Stop requested.", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(layout)
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
        Toast.makeText(this, "Wake listener started. Use the notification's Stop button to end listening.", Toast.LENGTH_LONG).show()
    }
}
