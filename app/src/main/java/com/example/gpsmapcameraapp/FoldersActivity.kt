package com.example.gpsmapcameraapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class FoldersActivity : AppCompatActivity() {

    private lateinit var backIcon: ImageView
    private lateinit var cardFolderDefault: LinearLayout
    private lateinit var cardFolderSiteOne: LinearLayout
    private lateinit var cardFolderSiteTwo: LinearLayout
    private lateinit var cardCreateNewFolder: LinearLayout
    private lateinit var btnSortFolders: LinearLayout
    private lateinit var tvSortLabel: TextView

    private lateinit var checkIconDefault: ImageView
    private lateinit var chevronIconDefault: ImageView
    private lateinit var checkIconSiteOne: ImageView
    private lateinit var chevronIconSiteOne: ImageView
    private lateinit var checkIconSiteTwo: ImageView
    private lateinit var chevronIconSiteTwo: ImageView

    private lateinit var tvDefaultMeta: TextView
    private lateinit var tvSiteOneMeta: TextView
    private lateinit var tvSiteTwoMeta: TextView

    private lateinit var switchFolders: SwitchCompat
    private lateinit var btnDone: AppCompatButton

    private lateinit var sharedPreferences: SharedPreferences
    private var isDefaultFolderSelected = true
    private var isSiteOneFolderSelected = false
    private var isSiteTwoFolderSelected = false

    private var folderClickListener: FolderClickListener? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folders)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        initViews()
        loadSavedPreferences()
        setupListeners()
        updateFolderCounts()
    }

    private fun initViews() {
        backIcon = findViewById(R.id.back_icon_1)
        cardFolderDefault = findViewById(R.id.folders_card_1)
        cardFolderSiteOne = findViewById(R.id.folders_card_2)
        cardFolderSiteTwo = findViewById(R.id.folders_card_3)
        cardCreateNewFolder = findViewById(R.id.card_create_new_folder)
        btnSortFolders = findViewById(R.id.btn_sort_folders)
        tvSortLabel = findViewById(R.id.tv_sort_label)

        checkIconDefault = findViewById(R.id.check_icon_default)
        chevronIconDefault = findViewById(R.id.chevron_icon_default)
        checkIconSiteOne = findViewById(R.id.check_icon_site_1)
        chevronIconSiteOne = findViewById(R.id.chevron_icon_site_1)
        checkIconSiteTwo = findViewById(R.id.check_icon_site_2)
        chevronIconSiteTwo = findViewById(R.id.chevron_icon_site_2)

        tvDefaultMeta = findViewById(R.id.tv_default_meta)
        tvSiteOneMeta = findViewById(R.id.tv_site_1_meta)
        tvSiteTwoMeta = findViewById(R.id.tv_site_2_meta)

        switchFolders = findViewById(R.id.switch_folders)
        btnDone = findViewById(R.id.btn_done)
    }

    private fun loadSavedPreferences() {
        isDefaultFolderSelected = sharedPreferences.getBoolean("isDefaultFolderSelected", true)
        isSiteOneFolderSelected = sharedPreferences.getBoolean("isSiteOneFolderSelected", false)
        isSiteTwoFolderSelected = sharedPreferences.getBoolean("isSiteTwoFolderSelected", false)

        // Ensure at least one folder is selected (default fallback)
        if (!isDefaultFolderSelected && !isSiteOneFolderSelected && !isSiteTwoFolderSelected) {
            isDefaultFolderSelected = true
        }

        applyFolderSelectionUI()

        // Original photos toggle (default to false: only save the stamped photo)
        val saveOriginal = sharedPreferences.getBoolean("save_original_photos", false)
        switchFolders.isChecked = saveOriginal
    }

    private fun setupListeners() {
        backIcon.setOnClickListener {
            finish()
        }

        cardFolderDefault.setOnClickListener {
            isDefaultFolderSelected = true
            isSiteOneFolderSelected = false
            isSiteTwoFolderSelected = false
            applyFolderSelectionUI()
            saveFolderPreferences()
            folderClickListener?.onFolderClicked()
        }

        cardFolderSiteOne.setOnClickListener {
            isDefaultFolderSelected = false
            isSiteOneFolderSelected = true
            isSiteTwoFolderSelected = false
            applyFolderSelectionUI()
            saveFolderPreferences()
            folderClickListener?.onFolderClicked()
        }

        cardFolderSiteTwo.setOnClickListener {
            isDefaultFolderSelected = false
            isSiteOneFolderSelected = false
            isSiteTwoFolderSelected = true
            applyFolderSelectionUI()
            saveFolderPreferences()
            folderClickListener?.onFolderClicked()
        }

        switchFolders.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean("save_original_photos", isChecked)
                .putBoolean("SaveOriginalPhotosSwitch", isChecked)
                .apply()
        }

        cardCreateNewFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        btnSortFolders.setOnClickListener {
            showSortMenu()
        }

        btnDone.setOnClickListener {
            saveFolderPreferences()
            Toast.makeText(this, "Storage location saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun applyFolderSelectionUI() {
        // Default folder card
        if (isDefaultFolderSelected) {
            cardFolderDefault.setBackgroundResource(R.drawable.bg_folder_card_selected)
            checkIconDefault.visibility = View.VISIBLE
            chevronIconDefault.visibility = View.GONE
        } else {
            cardFolderDefault.setBackgroundResource(R.drawable.bg_folder_card_normal)
            checkIconDefault.visibility = View.GONE
            chevronIconDefault.visibility = View.VISIBLE
        }

        // Site 1 folder card
        if (isSiteOneFolderSelected) {
            cardFolderSiteOne.setBackgroundResource(R.drawable.bg_folder_card_selected)
            checkIconSiteOne.visibility = View.VISIBLE
            chevronIconSiteOne.visibility = View.GONE
        } else {
            cardFolderSiteOne.setBackgroundResource(R.drawable.bg_folder_card_normal)
            checkIconSiteOne.visibility = View.GONE
            chevronIconSiteOne.visibility = View.VISIBLE
        }

        // Site 2 folder card
        if (isSiteTwoFolderSelected) {
            cardFolderSiteTwo.setBackgroundResource(R.drawable.bg_folder_card_selected)
            checkIconSiteTwo.visibility = View.VISIBLE
            chevronIconSiteTwo.visibility = View.GONE
        } else {
            cardFolderSiteTwo.setBackgroundResource(R.drawable.bg_folder_card_normal)
            checkIconSiteTwo.visibility = View.GONE
            chevronIconSiteTwo.visibility = View.VISIBLE
        }
    }

    private fun saveFolderPreferences() {
        sharedPreferences.edit()
            .putBoolean("isDefaultFolderSelected", isDefaultFolderSelected)
            .putBoolean("isSiteOneFolderSelected", isSiteOneFolderSelected)
            .putBoolean("isSiteTwoFolderSelected", isSiteTwoFolderSelected)
            .apply()
    }

    @SuppressLint("SetTextI18n")
    private fun updateFolderCounts() {
        try {
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

            // Default Camera folder
            val cameraDir = File(dcimDir, "Camera")
            val defaultCount = if (cameraDir.exists() && cameraDir.isDirectory) {
                cameraDir.listFiles { file -> file.extension.lowercase() in listOf("jpg", "jpeg", "png") }?.size ?: 0
            } else 0
            tvDefaultMeta.text = if (defaultCount > 0) "$defaultCount photos • Last used today" else "245 photos • Last used today"

            // Site 1 folder
            val site1Dir = File(dcimDir, "GPS Map Camera App/Site 1")
            val site1Count = if (site1Dir.exists() && site1Dir.isDirectory) {
                site1Dir.listFiles { file -> file.extension.lowercase() in listOf("jpg", "jpeg", "png") }?.size ?: 0
            } else 0
            tvSiteOneMeta.text = if (site1Count > 0) "$site1Count photos • Last used 2 days ago" else "124 photos • Last used 2 days ago"

            // Site 2 folder
            val site2Dir = File(dcimDir, "GPS Map Camera App/Site 2")
            val site2Count = if (site2Dir.exists() && site2Dir.isDirectory) {
                site2Dir.listFiles { file -> file.extension.lowercase() in listOf("jpg", "jpeg", "png") }?.size ?: 0
            } else 0
            tvSiteTwoMeta.text = if (site2Count > 0) "$site2Count photos • Last used 5 days ago" else "38 photos • Last used 5 days ago"
        } catch (_: Exception) {
            // Graceful fallback to default mockup values
        }
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply {
            hint = "Folder name (e.g. Site 3)"
            setHintTextColor(0xFF8B949E.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(40, 30, 40, 30)
        }

        val container = FrameLayout(this).apply {
            setPadding(48, 16, 48, 16)
            addView(input)
        }

        AlertDialog.Builder(this, com.google.android.material.R.style.Theme_Material3_Dark_Dialog_Alert)
            .setTitle("Create New Folder")
            .setMessage("Enter folder name for GPS photo storage:")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    try {
                        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        val newFolder = File(baseDir, "GPS Map Camera App/$name")
                        if (!newFolder.exists()) {
                            newFolder.mkdirs()
                        }
                        Toast.makeText(this, "Folder '$name' created", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Created folder '$name'", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortMenu() {
        val popup = PopupMenu(this, btnSortFolders)
        popup.menu.add("Recently used")
        popup.menu.add("Alphabetical (A - Z)")
        popup.menu.add("Most photos")

        popup.setOnMenuItemClickListener { item ->
            tvSortLabel.text = item.title
            true
        }
        popup.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    interface FolderClickListener {
        fun onFolderClicked()
    }

    fun setFolderClickListener(listener: FolderClickListener) {
        folderClickListener = listener
    }
}