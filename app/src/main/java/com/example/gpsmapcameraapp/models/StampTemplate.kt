package com.example.gpsmapcameraapp.models

enum class StampTemplate(
    val id: Int,
    val title: String,
    val subtitle: String,
    val cdnFileName: String,
    val category: String,
    val isComingSoon: Boolean = false
) {
    MODERN_CARD(1, "Modern Card", "Clean & Professional", "mountain.jpg", "Clean"),
    MINIMAL(2, "Minimal", "Simple & Elegant", "lake.jpg", "Minimal"),
    VERTICAL_INFO(3, "Vertical Info", "Detailed & Organized", "aerial-terrain.jpg", "Detailed"),
    MAP_INFO(4, "Map + Info", "With Satellite Map", "aerial-city.jpg", "Map"),
    CLEAN_OVERLAY(5, "Clean Overlay", "Date, Time & Compass", "sunset.jpg", "Clean"),
    CUSTOM(6, "Custom", "Design Your Own", "sunset.jpg", "Custom", isComingSoon = true);

    companion object {
        fun fromId(id: Int): StampTemplate {
            return entries.firstOrNull { it.id == id } ?: MODERN_CARD
        }
    }
}
