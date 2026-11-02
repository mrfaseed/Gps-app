package com.example.gpsmapcameraapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
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
        }

        setupCameraSwitches()
        setupAspectRatioAndTimer()
        setupStampSettings()
        setupFolderAndGalleryRows()
        setupAboutAndPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateAspectRatioAndTimerDisplay()
        updateFolderSubtitle()
        updatePermissionsStatus()
    }

    private fun setupAspectRatioAndTimer() {
        val cameraSettingsPref = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)

        // Aspect Ratio Row
        findViewById<View>(R.id.row_aspect_ratio).setOnClickListener {
            val ratioOptions = arrayOf("1:1 (Square)", "4:3 (Standard)", "16:9 (Widescreen)", "Full (Fill Screen)")
            val ratioKeys = arrayOf("1:1", "4:3", "16:9", "Full")
            val currentRatio = cameraSettingsPref.getString("camera_aspect_ratio", "16:9") ?: "16:9"
            val selectedIndex = ratioKeys.indexOf(currentRatio).let { if (it >= 0) it else 2 }

            AlertDialog.Builder(this)
                .setTitle("Select Camera Aspect Ratio")
                .setSingleChoiceItems(ratioOptions, selectedIndex) { dialog, which ->
                    val chosenKey = ratioKeys[which]
                    cameraSettingsPref.edit().putString("camera_aspect_ratio", chosenKey).apply()
                    updateAspectRatioAndTimerDisplay()
                    dialog.dismiss()
                    Toast.makeText(this, "Aspect ratio set to $chosenKey", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Self-Timer Row
        findViewById<View>(R.id.row_camera_timer).setOnClickListener {
            val timerOptions = arrayOf("Off (Instant)", "3 Seconds", "5 Seconds", "10 Seconds")
            val timerKeys = arrayOf("Off", "3s", "5s", "10s")
            val currentTimer = cameraSettingsPref.getString("camera_timer_mode", "Off") ?: "Off"
            val selectedIndex = timerKeys.indexOf(currentTimer).let { if (it >= 0) it else 0 }

            AlertDialog.Builder(this)
                .setTitle("Select Self-Timer")
                .setSingleChoiceItems(timerOptions, selectedIndex) { dialog, which ->
                    val chosenKey = timerKeys[which]
                    cameraSettingsPref.edit().putString("camera_timer_mode", chosenKey).apply()
                    // Also sync legacy timer_state for backwards compatibility
                    val legacyTimerPref = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
                    val legacyText = when (chosenKey) {
                        "3s" -> "Timer 3sec"
                        "5s" -> "Timer 5sec"
                        "10s" -> "Timer 10sec"
                        else -> "Timer Off"
                    }
                    legacyTimerPref.edit().putString("timer_state_key", legacyText).apply()

                    updateAspectRatioAndTimerDisplay()
                    dialog.dismiss()
                    Toast.makeText(this, "Timer set to $chosenKey", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateAspectRatioAndTimerDisplay() {
        val cameraSettingsPref = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        val currentRatio = cameraSettingsPref.getString("camera_aspect_ratio", "16:9") ?: "16:9"
        val currentTimer = cameraSettingsPref.getString("camera_timer_mode", "Off") ?: "Off"

        findViewById<TextView>(R.id.tv_settings_aspect_ratio_value)?.text = currentRatio
        findViewById<TextView>(R.id.tv_settings_timer_value)?.text = currentTimer
    }

    private fun setupCameraSwitches() {
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
            val stateText = if (isChecked) "Screen will stay awake" else "Normal screen timeout"
            Toast.makeText(this, stateText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStampSettings() {
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

        // Watermark Tagline Editor Dialog
        val tvBranding = findViewById<TextView>(R.id.settings_branding_text)
        val currentTagline = stampPref.getString("stamp_tagline", "Simatrix GPS Camera") ?: "Simatrix GPS Camera"
        tvBranding?.text = currentTagline

        findViewById<View>(R.id.row_tagline).setOnClickListener {
            val input = EditText(this).apply {
                setText(stampPref.getString("stamp_tagline", "Simatrix GPS Camera"))
                setSelection(text.length)
                setSingleLine(true)
                setTextColor(Color.parseColor("#1C1C1E"))
                setPadding(50, 40, 50, 40)
            }

            AlertDialog.Builder(this)
                .setTitle("Stamp Watermark")
                .setMessage("Enter custom company, surveyor, or project name to imprint on photos:")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newText = input.text.toString().trim()
                    val finalTagline = if (newText.isNotBlank()) newText else "Simatrix GPS Camera"
                    stampPref.edit().putString("stamp_tagline", finalTagline).apply()
                    tvBranding?.text = finalTagline
                    Toast.makeText(this, "Watermark updated", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Reset") { _, _ ->
                    stampPref.edit().putString("stamp_tagline", "Simatrix GPS Camera").apply()
                    tvBranding?.text = "Simatrix GPS Camera"
                    Toast.makeText(this, "Reset to default watermark", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupFolderAndGalleryRows() {
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
    }

    private fun updateFolderSubtitle() {
        val folderPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val folderName = when {
            folderPrefs.getBoolean("isSiteOneFolderSelected", false) -> "GPS Map Camera App / Site 1"
            folderPrefs.getBoolean("isSiteTwoFolderSelected", false) -> "GPS Map Camera App / Site 2"
            else -> "DCIM / Camera"
        }
        findViewById<TextView>(R.id.tv_settings_folder_name)?.text = folderName
    }

    private fun setupAboutAndPermissions() {
        // Permissions Status Row (tap to open system app info)
        findViewById<View>(R.id.row_permissions_status)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
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

    private fun updatePermissionsStatus() {
        val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasFineLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val tvStatus = findViewById<TextView>(R.id.tv_permissions_status)

        if (hasCamera && hasFineLoc) {
            tvStatus?.text = "Camera & GPS Granted"
            tvStatus?.setTextColor(Color.parseColor("#34C759"))
        } else {
            tvStatus?.text = "Action Required"
            tvStatus?.setTextColor(Color.parseColor("#FF9500"))
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
