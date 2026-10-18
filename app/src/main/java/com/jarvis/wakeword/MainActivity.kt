package com.jarvis.wakeword

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCode = 7
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }
        layout.addView(TextView(this).apply {
            text = "JARVIS Wake Word\n\nStart listening, then say “Jarvis”. Keep the persistent notification visible."
            textSize = 20f
        })
        layout.addView(Button(this).apply {
            text = "Start wake listening"
            setOnClickListener { requestPermissionsThenStart() }
        })
        layout.addView(Button(this).apply {
            text = "Stop listening"
            setOnClickListener { stopService(Intent(this@MainActivity, WakeWordService::class.java)) }
        })
        setContentView(layout)
    }

    private fun requestPermissionsThenStart() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), requestCode)
        } else startWakeService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == this.requestCode && results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) {
            startWakeService()
        } else Toast.makeText(this, "Microphone permission is required.", Toast.LENGTH_LONG).show()
    }

    private fun startWakeService() {
        val intent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Wake listener started. Say “Jarvis”.", Toast.LENGTH_LONG).show()
    }
}
