package com.example.gpsmapcameraapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.gpsmapcameraapp.models.StampTemplate
import com.example.gpsmapcameraapp.utils.TemplateImageCache
import com.google.android.gms.maps.GoogleMap

class TemplateActivity : AppCompatActivity() {

    private lateinit var backIcon: ImageView
    private lateinit var applyButton: AppCompatButton
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedTemplateId = 1
    var selectedMapType = GoogleMap.MAP_TYPE_NORMAL

    private lateinit var chips: List<TextView>
    private var activeCategory = "All"

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template)

        sharedPreferences = getSharedPreferences("TemplatePreferences", Context.MODE_PRIVATE)
        selectedTemplateId = sharedPreferences.getInt("selected_template_id", 1)
        if (selectedTemplateId == 6) {
            selectedTemplateId = 1
            sharedPreferences.edit().putInt("selected_template_id", 1).apply()
        }

        initViews()
        loadTemplatePreviewImages()
        setupTemplateCards()
        setupCategoryChips()
    }

    private fun initViews() {
        backIcon = findViewById(R.id.back_icon_5)
        backIcon.setOnClickListener {
            finish()
        }

        applyButton = findViewById(R.id.btn_apply_template)
        applyButton.setOnClickListener {
            val template = StampTemplate.fromId(selectedTemplateId)
            sharedPreferences.edit().putInt("selected_template_id", selectedTemplateId).apply()
            Toast.makeText(this, "Template Applied: ${template.title}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Loads the 5 template preview images from disk cache or downloads & caches them from CDN
     */
    private fun loadTemplatePreviewImages() {
        val bgImageViews = listOf(
            findViewById<ImageView>(R.id.img_template_bg_1),
            findViewById<ImageView>(R.id.img_template_bg_2),
            findViewById<ImageView>(R.id.img_template_bg_3),
            findViewById<ImageView>(R.id.img_template_bg_4),
            findViewById<ImageView>(R.id.img_template_bg_5)
        )

        val templates = listOf(
            StampTemplate.MODERN_CARD,
            StampTemplate.MINIMAL,
            StampTemplate.VERTICAL_INFO,
            StampTemplate.MAP_INFO,
            StampTemplate.CLEAN_OVERLAY
        )

        for (i in bgImageViews.indices) {
            val imageView = bgImageViews[i] ?: continue
            val template = templates[i]
            // Load and cache with local disk cache
            TemplateImageCache.loadTemplateImage(this, template.cdnFileName, imageView)
        }

        // Trigger background pre-cache for all 5 images if net is on
        TemplateImageCache.preCacheAllTemplates(this)
    }

    @SuppressLint("SetTextI18n")
    private fun setupTemplateCards() {
        val cards = listOf(
            findViewById<View>(R.id.card_template_1),
            findViewById<View>(R.id.card_template_2),
            findViewById<View>(R.id.card_template_3),
            findViewById<View>(R.id.card_template_4),
            findViewById<View>(R.id.card_template_5)
        )

        val badges = listOf(
            findViewById<View>(R.id.check_badge_1),
            findViewById<View>(R.id.check_badge_2),
            findViewById<View>(R.id.check_badge_3),
            findViewById<View>(R.id.check_badge_4),
            findViewById<View>(R.id.check_badge_5)
        )

        val checkIcons = listOf(
            findViewById<ImageView>(R.id.check_icon_1),
            findViewById<ImageView>(R.id.check_icon_2),
            findViewById<ImageView>(R.id.check_icon_3),
            findViewById<ImageView>(R.id.check_icon_4),
            findViewById<ImageView>(R.id.check_icon_5)
        )

        fun updateUi(id: Int) {
            selectedTemplateId = id
            sharedPreferences.edit().putInt("selected_template_id", id).apply()

            for (i in 0 until 5) {
                val isSelected = (i + 1) == id
                cards[i]?.setBackgroundResource(
                    if (isSelected) R.drawable.bg_template_grid_card_selected
                    else R.drawable.bg_template_grid_card
                )
                badges[i]?.setBackgroundResource(
                    if (isSelected) R.drawable.bg_check_circle_active
                    else R.drawable.bg_check_circle_inactive
                )
                checkIcons[i]?.visibility = if (isSelected) View.VISIBLE else View.GONE
            }

            val template = StampTemplate.fromId(id)
            applyButton.text = "Apply ${template.title}"
        }

        updateUi(selectedTemplateId)

        cards.forEachIndexed { index, card ->
            val templateId = index + 1
            card?.setOnClickListener {
                updateUi(templateId)
            }
        }

        // Custom template card (Coming Soon & Disabled)
        val card6 = findViewById<View>(R.id.card_template_6)
        card6?.setOnClickListener {
            Toast.makeText(this, "Custom template is coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoryChips() {
        val chipAll = findViewById<TextView>(R.id.chip_all)
        val chipClean = findViewById<TextView>(R.id.chip_clean)
        val chipMap = findViewById<TextView>(R.id.chip_map)
        val chipDetailed = findViewById<TextView>(R.id.chip_detailed)
        val chipMinimal = findViewById<TextView>(R.id.chip_minimal)

        chips = listOf(chipAll, chipClean, chipMap, chipDetailed, chipMinimal)

        val cards = listOf(
            findViewById<View>(R.id.card_template_1),
            findViewById<View>(R.id.card_template_2),
            findViewById<View>(R.id.card_template_3),
            findViewById<View>(R.id.card_template_4),
            findViewById<View>(R.id.card_template_5)
        )

        val card6 = findViewById<View>(R.id.card_template_6)

        fun selectChip(selectedChip: TextView, category: String) {
            activeCategory = category
            chips.forEach { chip ->
                val isSelected = chip == selectedChip
                chip.setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                chip.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF8B949E.toInt())
            }

            // Filter template card visibility or opacity
            cards.forEachIndexed { index, card ->
                val template = StampTemplate.fromId(index + 1)
                val matches = category == "All" || template.category.equals(category, ignoreCase = true)
                card?.alpha = if (matches) 1.0f else 0.35f
            }

            if (category == "All") {
                card6?.visibility = View.VISIBLE
                card6?.alpha = 0.65f
            } else {
                card6?.visibility = View.GONE
            }
        }

        chipAll.setOnClickListener { selectChip(chipAll, "All") }
        chipClean.setOnClickListener { selectChip(chipClean, "Clean") }
        chipMap.setOnClickListener { selectChip(chipMap, "Map") }
        chipDetailed.setOnClickListener { selectChip(chipDetailed, "Detailed") }
        chipMinimal.setOnClickListener { selectChip(chipMinimal, "Minimal") }
    }

    // Compatibility stubs for legacy bottom sheet dialogs
    fun updateLatLongText(text: String) {}
    fun updatePlusCodeText(text: String) {}
    fun updateTimeZoneText(text: String) {}
    fun updateWeatherText(text: String) {}
    fun updateWindText(text: String) {}
    fun updatePressureText(text: String) {}
    fun updateAltitudeText(text: String) {}
    fun updateAccuracyText(text: String) {}
    fun toggleMapSatellite() { selectedMapType = GoogleMap.MAP_TYPE_SATELLITE }
    fun toggleMapNormal() { selectedMapType = GoogleMap.MAP_TYPE_NORMAL }
    fun toggleMapTerrain() { selectedMapType = GoogleMap.MAP_TYPE_TERRAIN }
    fun toggleMapHybrid() { selectedMapType = GoogleMap.MAP_TYPE_HYBRID }
    fun saveSelectedMapType(mapType: Int) {
        selectedMapType = mapType
        sharedPreferences.edit().putInt("SelectedMapType", mapType).apply()
    }
    fun updateCircleForMapTypeIcon(mapType: Int) {
        selectedMapType = mapType
    }
}