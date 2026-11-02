package com.example.gpsmapcameraapp.models

import android.graphics.Bitmap

data class StampData(
    val shortAddress: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val latLongText: String,
    val altitudeText: String,
    val accuracyText: String,
    val dateText: String,
    val timeText: String,
    val timezoneText: String,
    val mapBitmap: Bitmap? = null,
    val compassAzimuth: Float = 0f,
    val relativeStampX: Float? = null,
    val relativeStampY: Float? = null
)
