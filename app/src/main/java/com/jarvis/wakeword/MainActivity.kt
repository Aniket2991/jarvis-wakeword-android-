package com.jarvis.wakeword

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
            text = "Experimental voice trigger • Keep the persistent notification visible while listening."
            textSize = 15f
            setPadding(0, 16, 0, 24)
        })

        layout.addView(TextView(this).apply {
            text = "MICROPHONE & PRIVACY"
            textSize = 16f
        })

        layout.addView(TextView(this).apply {
            text = "When you start listening, Android SpeechRecognizer checks recognized speech for “Jarvis”. If your phone supports Android on-device recognition, the app prefers it; otherwise, your speech provider may process audio online. This is not a dedicated always-on keyword detector, so detection can be delayed or missed and listening may stop under Android battery or background limits. Saying “Jarvis” does not automatically launch the assistant; use the Open JARVIS action in the notification or the button below. You can stop the microphone listener at any time."
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
            text = "Stop listening"
            setOnClickListener {
                stopService(Intent(this@MainActivity, WakeWordService::class.java))
                Toast.makeText(this@MainActivity, "Stop requested.", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(layout)
    }

    private fun openAssistant() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://jarvis-web-assistant-five.vercel.app")
                )
            )
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
        Toast.makeText(this, "Wake listener started. Use the notification's Stop button to end listening.", Toast.LENGTH_LONG).show()
    }
}
