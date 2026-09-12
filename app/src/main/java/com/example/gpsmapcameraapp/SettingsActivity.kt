package com.example.gpsmapcameraapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Apple-style clean status bar with dark icons
        window.statusBarColor = Color.parseColor("#F2F2F7")
        window.navigationBarColor = Color.parseColor("#F2F2F7")
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        // Top Back Button
        findViewById<View>(R.id.settings_back_btn).setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Shutter Sound Switch (Connected to sound_state shared preference used by camera)
        val soundPref = getSharedPreferences("sound_state", Context.MODE_PRIVATE)
        val soundSwitch = findViewById<SwitchCompat>(R.id.switch_shutter_sound)
        soundSwitch.isChecked = soundPref.getBoolean("is_sound_on", true)
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            soundPref.edit().putBoolean("is_sound_on", isChecked).apply()
            val stateText = if (isChecked) "enabled" else "muted"
            Toast.makeText(this, "Camera shutter sound $stateText", Toast.LENGTH_SHORT).show()
        }

        // Keep Screen On Switch
        val cameraSettingsPref = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        val keepAwakeSwitch = findViewById<SwitchCompat>(R.id.switch_keep_awake)
        keepAwakeSwitch.isChecked = cameraSettingsPref.getBoolean("keep_screen_on", true)
        keepAwakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            cameraSettingsPref.edit().putBoolean("keep_screen_on", isChecked).apply()
        }

        // Stamp Settings
        val stampPref = getSharedPreferences("StampSettings", Context.MODE_PRIVATE)
        val addressSwitch = findViewById<SwitchCompat>(R.id.switch_address_stamp)
        addressSwitch.isChecked = stampPref.getBoolean("stamp_address", true)
        addressSwitch.setOnCheckedChangeListener { _, isChecked ->
            stampPref.edit().putBoolean("stamp_address", isChecked).apply()
        }

        val mapSwitch = findViewById<SwitchCompat>(R.id.switch_map_stamp)
        mapSwitch.isChecked = stampPref.getBoolean("stamp_map", true)
        mapSwitch.setOnCheckedChangeListener { _, isChecked ->
            stampPref.edit().putBoolean("stamp_map", isChecked).apply()
        }

        val telemetrySwitch = findViewById<SwitchCompat>(R.id.switch_telemetry_stamp)
        telemetrySwitch.isChecked = stampPref.getBoolean("stamp_telemetry", true)
        telemetrySwitch.setOnCheckedChangeListener { _, isChecked ->
            stampPref.edit().putBoolean("stamp_telemetry", isChecked).apply()
        }

        // Tagline Row
        findViewById<View>(R.id.row_tagline).setOnClickListener {
            Toast.makeText(this, "Watermark Tagline: Simatrix GPS Camera", Toast.LENGTH_SHORT).show()
        }

        // Storage Folder Row
        findViewById<View>(R.id.row_storage_folder).setOnClickListener {
            val intent = Intent(this, FoldersActivity::class.java)
            startActivity(intent)
        }

        // Photo Gallery Row
        findViewById<View>(R.id.row_photo_collection).setOnClickListener {
            val intent = Intent(this, CollectionActivity::class.java)
            startActivity(intent)
        }

        // Share App Row
        findViewById<View>(R.id.row_share_app).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Simatrix GPS Camera - Professional geolocated photo evidence, engineering site surveys & field inspections."
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share Simatrix GPS Camera"))
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
