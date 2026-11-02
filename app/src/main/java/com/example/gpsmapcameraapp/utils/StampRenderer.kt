package com.example.gpsmapcameraapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.gpsmapcameraapp.R
import com.example.gpsmapcameraapp.models.StampData
import com.example.gpsmapcameraapp.models.StampTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StampRenderer {

    /**
     * Renders the chosen stamp template onto the captured bitmap.
     * Scale and positioning adapt dynamically to match any camera capture resolution.
     */
    fun applyStamp(
        context: Context,
        sourceBitmap: Bitmap,
        template: StampTemplate,
        data: StampData
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw base captured photo
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)

        // 2. Dynamic scale factor relative to a 1080p baseline width
        val baselineWidth = 1080f
        val scale = (sourceBitmap.width.toFloat() / baselineWidth).coerceAtLeast(1.0f)
        val density = context.resources.displayMetrics.density

        // Read StampSettings preferences
        val stampPref = context.getSharedPreferences("StampSettings", Context.MODE_PRIVATE)
        val showAddress = stampPref.getBoolean("stamp_address", true)
        val showMap = stampPref.getBoolean("stamp_map", true)
        val showTelemetry = stampPref.getBoolean("stamp_telemetry", true)
        val customTagline = stampPref.getString("stamp_tagline", "Simatrix GPS Camera") ?: "Simatrix GPS Camera"

        // 3. Render according to chosen template
        when (template) {
            StampTemplate.MODERN_CARD -> {
                drawWatermark(context, canvas, sourceBitmap.width, sourceBitmap.height, density, isTopLeft = true, tagline = customTagline)
                drawModernCard(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showMap, showTelemetry, customTagline)
            }
            StampTemplate.MINIMAL -> {
                drawMinimalScriptWatermark(context, canvas, sourceBitmap.width, sourceBitmap.height, density, tagline = customTagline)
                drawMinimal(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showTelemetry)
            }
            StampTemplate.VERTICAL_INFO -> {
                drawVerticalInfo(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showTelemetry)
            }
            StampTemplate.MAP_INFO -> {
                drawWatermark(context, canvas, sourceBitmap.width, sourceBitmap.height, density, isTopLeft = false, tagline = customTagline)
                drawSatelliteMap(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showMap, showTelemetry, customTagline)
            }
            StampTemplate.CLEAN_OVERLAY -> {
                drawCompassOverlay(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showTelemetry, customTagline)
            }
            StampTemplate.CUSTOM -> {
                drawWatermark(context, canvas, sourceBitmap.width, sourceBitmap.height, density, isTopLeft = true, tagline = customTagline)
                drawModernCard(context, canvas, sourceBitmap.width, sourceBitmap.height, density, data, showAddress, showMap, showTelemetry, customTagline)
            }
        }

        return resultBitmap
    }

    private fun drawWatermark(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        isTopLeft: Boolean = false,
        tagline: String = "Simatrix GPS Camera"
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val inflater = LayoutInflater.from(context)
        val watermarkView = inflater.inflate(R.layout.stamp_watermark_geotag, null)

        val titleTv = watermarkView.findViewById<TextView>(R.id.watermark_title_tv)
        if (titleTv != null && tagline.isNotBlank()) {
            titleTv.text = tagline
        }

        watermarkView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        watermarkView.layout(0, 0, watermarkView.measuredWidth, watermarkView.measuredHeight)

        val targetX = if (isTopLeft) {
            16f * density
        } else {
            screenWidth - watermarkView.measuredWidth - (16f * density)
        }
        val targetY = 96f * density

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(targetX, targetY)
        watermarkView.draw(canvas)
        canvas.restore()
    }

    private fun drawMinimalScriptWatermark(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        tagline: String = "Location Tells the Story"
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val displayText = if (tagline == "Simatrix GPS Camera") "Location Tells the Story" else tagline
        val textView = TextView(context).apply {
            text = displayText
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(4f * density, 1f * density, 1f * density, 0xCC000000.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD_ITALIC)
        }

        textView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)

        val targetX = (screenWidth - textView.measuredWidth) / 2f
        val targetY = 96f * density

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(targetX, targetY)
        textView.draw(canvas)
        canvas.restore()
    }

    private fun extractShortAndFullAddress(rawText: String): Pair<String, String> {
        val clean = rawText.trim()
        if (clean.isBlank() || clean.startsWith("Location is off") || clean.startsWith("Acquiring")) {
            return Pair("Location Acquired", clean)
        }
        if (clean.contains("\n")) {
            val parts = clean.split("\n", limit = 2)
            return Pair(parts[0].trim(), parts.getOrNull(1)?.trim() ?: parts[0].trim())
        }
        val parts = clean.split(",")
        return if (parts.size >= 3) {
            val short = "${parts[0].trim()}, ${parts[1].trim()}"
            val full = parts.drop(2).joinToString(", ") { it.trim() }
            Pair(short, full)
        } else {
            Pair(clean, clean)
        }
    }

    private fun getRoundedCornerBitmap(
        src: Bitmap,
        cornerRadiusPx: Float,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rect = RectF(0f, 0f, targetWidthPx.toFloat(), targetHeightPx.toFloat())

        val scale = maxOf(targetWidthPx.toFloat() / src.width, targetHeightPx.toFloat() / src.height)
        val scaledW = src.width * scale
        val scaledH = src.height * scale
        val left = (targetWidthPx - scaledW) / 2f
        val top = (targetHeightPx - scaledH) / 2f
        val destRect = RectF(left, top, left + scaledW, top + scaledH)

        val path = Path().apply {
            addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(src, null, destRect, paint)
        return output
    }

    private fun drawModernCard(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        data: StampData,
        showAddress: Boolean = true,
        showMap: Boolean = true,
        showTelemetry: Boolean = true,
        customTagline: String = "Simatrix GPS Camera"
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val inflater = LayoutInflater.from(context)
        val cardView = inflater.inflate(R.layout.stamp_template_1_modern, null)

        val addressTv = cardView.findViewById<TextView>(R.id.modern_address_tv)
        if (showAddress) {
            addressTv.visibility = View.VISIBLE
            val address = data.fullAddress.ifBlank { data.shortAddress }.ifBlank { "Acquiring Location..." }
            addressTv.text = address
        } else {
            addressTv.visibility = View.GONE
        }
        cardView.findViewById<TextView>(R.id.modern_lat_long_tv).text = data.latLongText

        val altTv = cardView.findViewById<TextView>(R.id.modern_altitude_tv)
        val accTv = cardView.findViewById<TextView>(R.id.modern_accuracy_tv)
        if (showTelemetry) {
            altTv.visibility = View.VISIBLE
            accTv.visibility = View.VISIBLE
            altTv.text = data.altitudeText.ifBlank { "14.7 m" }
            accTv.text = data.accuracyText.ifBlank { "±15.2 m" }
        } else {
            altTv.visibility = View.GONE
            accTv.visibility = View.GONE
        }
        cardView.findViewById<TextView>(R.id.modern_timezone_tv).text = data.timezoneText.ifBlank { "GMT +05:30" }

        val formattedDate = if (data.dateText.isNotBlank() && data.dateText.contains(",")) {
            data.dateText
        } else {
            SimpleDateFormat("dd/MM/yyyy, EEEE, hh:mm:ss a", Locale.US).format(Date()).lowercase()
        }
        cardView.findViewById<TextView>(R.id.modern_date_time_tv).text = formattedDate
        cardView.findViewById<TextView>(R.id.modern_branding_tv).text = "Built by : $customTagline"

        val mapCard = cardView.findViewById<View>(R.id.modern_map_card)
        val detailsCard = cardView.findViewById<View>(R.id.modern_details_card)
        if (showMap) {
            mapCard.visibility = View.VISIBLE
            // Produce a 100% anti-aliased rounded map bitmap matching the viewfinder
            val mapImageView = cardView.findViewById<ImageView>(R.id.modern_map_image)
            val mapPinView = cardView.findViewById<ImageView>(R.id.modern_map_pin)
            val mapCardSizePx = (86f * density).toInt()
            val cornerRadiusPx = 13f * density
            val mapSource = data.mapBitmap ?: createFallbackMapBitmap(mapCardSizePx, mapCardSizePx)
            val roundedMap = getRoundedCornerBitmap(mapSource, cornerRadiusPx, mapCardSizePx, mapCardSizePx)
            mapImageView.setImageBitmap(roundedMap)
            mapPinView.visibility = if (data.mapBitmap != null) View.GONE else View.VISIBLE
        } else {
            mapCard.visibility = View.GONE
            (detailsCard.layoutParams as? android.widget.LinearLayout.LayoutParams)?.marginStart = 0
        }

        // Measure layout using the device's actual screen width
        // Guarantees the details card receives the exact same width (~70% of screen)
        // and all 6 lines of text fit seamlessly without any truncation, matching preview 1:1
        cardView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        cardView.layout(0, 0, screenWidth, cardView.measuredHeight)

        val cardHeightOnScreen = cardView.measuredHeight.toFloat()
        val bottomOffsetScreen = 52f * density
        val targetY = (imageHeight.toFloat() / photoScale) - cardHeightOnScreen - bottomOffsetScreen

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(0f, targetY)
        cardView.draw(canvas)
        canvas.restore()
    }

    private fun drawMinimal(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        data: StampData,
        showAddress: Boolean = true,
        showTelemetry: Boolean = true
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val inflater = LayoutInflater.from(context)

        // Outer wrapper with 12dp side margins matching activity_main.xml minimal_info_container
        val wrapper = android.widget.FrameLayout(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(screenWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            val sideMarginPx = (12f * density).toInt()
            setPadding(sideMarginPx, 0, sideMarginPx, 0)
        }

        // Glass card container matching minimal_info_container in activity_main.xml
        val cardContainer = android.widget.FrameLayout(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_glass_card)
            setPadding(0, (6f * density).toInt(), 0, 0)
        }

        val view = inflater.inflate(R.layout.stamp_template_2_minimal, cardContainer, false)

        val shortTv = view.findViewById<TextView>(R.id.minimal_short_address_tv)
        val fullTv = view.findViewById<TextView>(R.id.minimal_full_address_tv)
        if (showAddress) {
            shortTv?.visibility = View.VISIBLE
            fullTv?.visibility = View.VISIBLE
            val (shortAddr, fullAddr) = extractShortAndFullAddress(data.fullAddress.ifBlank { data.shortAddress })
            shortTv?.text = if (shortAddr.isNotBlank()) shortAddr else data.shortAddress
            fullTv?.text = if (fullAddr.isNotBlank()) fullAddr else data.fullAddress
        } else {
            shortTv?.visibility = View.GONE
            fullTv?.visibility = View.GONE
        }
        view.findViewById<TextView>(R.id.minimal_lat_long_tv).text = data.latLongText

        view.findViewById<TextView>(R.id.minimal_date_tv).text = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        view.findViewById<TextView>(R.id.minimal_day_tv).text = SimpleDateFormat("EEEE", Locale.US).format(Date())
        view.findViewById<TextView>(R.id.minimal_time_tv).text = data.timeText

        val altTv = view.findViewById<TextView>(R.id.minimal_altitude_tv)
        if (showTelemetry) {
            altTv?.visibility = View.VISIBLE
            val altVal = data.altitudeText.ifBlank { "14.7 m" }
            altTv?.text = "Alt $altVal"
        } else {
            altTv?.visibility = View.GONE
        }

        cardContainer.addView(view)
        wrapper.addView(cardContainer)

        wrapper.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        wrapper.layout(0, 0, screenWidth, wrapper.measuredHeight)

        val targetX: Float
        val targetY: Float

        val maxTargetX = (screenWidth - wrapper.measuredWidth).toFloat().coerceAtLeast(0f)
        val maxTargetY = ((imageHeight.toFloat() / photoScale) - wrapper.measuredHeight).toFloat().coerceAtLeast(0f)

        if (data.relativeStampX != null && data.relativeStampY != null) {
            targetX = (data.relativeStampX * screenWidth).coerceIn(0f, maxTargetX)
            targetY = (data.relativeStampY * (imageHeight.toFloat() / photoScale)).coerceIn(0f, maxTargetY)
        } else {
            targetX = 0f
            val cardHeightOnScreen = wrapper.measuredHeight.toFloat()
            val bottomOffsetScreen = 52f * density
            targetY = ((imageHeight.toFloat() / photoScale) - cardHeightOnScreen - bottomOffsetScreen).coerceIn(0f, maxTargetY)
        }

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(targetX, targetY)
        wrapper.draw(canvas)
        canvas.restore()
    }

    private fun drawVerticalInfo(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        data: StampData,
        showAddress: Boolean = true,
        showTelemetry: Boolean = true
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.stamp_template_3_vertical, null)

        val shortTv = view.findViewById<TextView>(R.id.vertical_short_address_tv)
        val fullTv = view.findViewById<TextView>(R.id.vertical_full_address_tv)
        if (showAddress) {
            shortTv?.visibility = View.VISIBLE
            fullTv?.visibility = View.VISIBLE
            val (shortAddr, fullAddr) = extractShortAndFullAddress(data.fullAddress.ifBlank { data.shortAddress })
            shortTv?.text = if (shortAddr.isNotBlank()) shortAddr else data.shortAddress
            fullTv?.text = if (fullAddr.isNotBlank()) fullAddr else data.fullAddress
        } else {
            shortTv?.visibility = View.GONE
            fullTv?.visibility = View.GONE
        }
        view.findViewById<TextView>(R.id.vertical_lat_long_tv)?.text = data.latLongText

        val dateFormatted = SimpleDateFormat("dd MMM yyyy, EEEE", Locale.US).format(Date())
        val timeFormatted = if (data.timeText.isNotBlank()) data.timeText else SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        view.findViewById<TextView>(R.id.vertical_date_tv)?.text = dateFormatted
        view.findViewById<TextView>(R.id.vertical_time_tv)?.text = timeFormatted

        val altTv = view.findViewById<TextView>(R.id.vertical_altitude_tv)
        if (showTelemetry) {
            altTv?.visibility = View.VISIBLE
            val altText = if (data.altitudeText.startsWith("Alt", ignoreCase = true)) data.altitudeText else "Alt ${data.altitudeText.ifBlank { "14.7 m" }}"
            val accText = if (data.accuracyText.startsWith("±")) data.accuracyText else "±${data.accuracyText.ifBlank { "16.2 m" }}"
            altTv?.text = "$altText  |  $accText"
        } else {
            altTv?.visibility = View.GONE
        }

        val targetCardWidthPx = (170f * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(targetCardWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val targetX: Float
        val targetY: Float

        val maxTargetX = (screenWidth - view.measuredWidth).toFloat().coerceAtLeast(0f)
        val maxTargetY = ((imageHeight.toFloat() / photoScale) - view.measuredHeight).toFloat().coerceAtLeast(0f)

        if (data.relativeStampX != null && data.relativeStampY != null) {
            targetX = (data.relativeStampX * screenWidth).coerceIn(0f, maxTargetX)
            targetY = (data.relativeStampY * (imageHeight.toFloat() / photoScale)).coerceIn(0f, maxTargetY)
        } else {
            val cardHeightOnScreen = view.measuredHeight.toFloat()
            val cardWidthOnScreen = view.measuredWidth.toFloat()
            targetX = (screenWidth - cardWidthOnScreen - (12f * density)).coerceIn(0f, maxTargetX)
            val bottomOffsetScreen = 52f * density
            targetY = ((imageHeight.toFloat() / photoScale) - cardHeightOnScreen - bottomOffsetScreen).coerceIn(0f, maxTargetY)
        }

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(targetX, targetY)
        view.draw(canvas)
        canvas.restore()
    }

    private fun drawSatelliteMap(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        data: StampData,
        showAddress: Boolean = true,
        showMap: Boolean = true,
        showTelemetry: Boolean = true,
        customTagline: String = "Simatrix GPS Camera"
    ) {
        val screenWidth = minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        val photoScale = imageWidth.toFloat() / screenWidth.toFloat()

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.stamp_template_4_satellite, null)

        val (shortAddr, fullAddr) = extractShortAndFullAddress(data.fullAddress.ifBlank { data.shortAddress })
        val shortAddrTv = view.findViewById<TextView>(R.id.satellite_short_address_tv)
        val fullAddrTv = view.findViewById<TextView>(R.id.satellite_full_address_tv)
        if (showAddress) {
            shortAddrTv.text = if (shortAddr.isNotBlank()) shortAddr else data.shortAddress
            fullAddrTv.text = if (fullAddr.isNotBlank()) fullAddr else data.fullAddress
            shortAddrTv.visibility = View.VISIBLE
            fullAddrTv.visibility = View.VISIBLE
        } else {
            shortAddrTv.visibility = View.GONE
            fullAddrTv.visibility = View.GONE
        }

        val latLongTv = view.findViewById<TextView>(R.id.satellite_lat_long_tv)
        latLongTv.text = data.latLongText
        latLongTv.visibility = if (showTelemetry) View.VISIBLE else View.GONE

        val dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val timeFormatted = if (data.timeText.isNotBlank()) data.timeText else SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        view.findViewById<TextView>(R.id.satellite_date_tv).text = dateFormatted
        view.findViewById<TextView>(R.id.satellite_time_tv).text = timeFormatted

        val altClean = if (data.altitudeText.startsWith("Alt", ignoreCase = true)) data.altitudeText else "${data.altitudeText.ifBlank { "14.7 m" }}"
        val accClean = if (data.accuracyText.startsWith("±")) data.accuracyText else "±${data.accuracyText.ifBlank { "32.0 m" }}"
        val altTv = view.findViewById<TextView>(R.id.satellite_altitude_tv)
        val accTv = view.findViewById<TextView>(R.id.satellite_accuracy_tv)
        altTv.text = altClean
        accTv.text = accClean
        altTv.visibility = if (showTelemetry) View.VISIBLE else View.GONE
        accTv.visibility = if (showTelemetry) View.VISIBLE else View.GONE

        val brandingTv = view.findViewById<TextView>(R.id.satellite_branding_tv)
        if (brandingTv != null) {
            if (customTagline.isNotBlank()) {
                brandingTv.text = "Captured with $customTagline"
                brandingTv.visibility = View.VISIBLE
            } else {
                brandingTv.visibility = View.GONE
            }
        }

        val mapCard = view.findViewById<View>(R.id.satellite_map_card)
        val detailsCard = view.findViewById<View>(R.id.satellite_details_card)
        if (showMap) {
            mapCard.visibility = View.VISIBLE
            val mapCardSizePx = (96f * density).toInt()
            val cornerRadiusPx = 14f * density
            val mapSource = data.mapBitmap ?: createFallbackMapBitmap(mapCardSizePx, mapCardSizePx)
            val roundedMap = getRoundedCornerBitmap(mapSource, cornerRadiusPx, mapCardSizePx, mapCardSizePx)
            val mapImageView = view.findViewById<ImageView>(R.id.satellite_map_image)
            val mapPinView = view.findViewById<ImageView>(R.id.satellite_map_pin)
            mapImageView.setImageBitmap(roundedMap)
            mapPinView?.visibility = if (data.mapBitmap != null) View.GONE else View.VISIBLE
        } else {
            mapCard.visibility = View.GONE
            val lp = detailsCard?.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.marginStart = 0
                detailsCard.layoutParams = lp
            }
        }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, screenWidth, view.measuredHeight)

        val cardHeightOnScreen = view.measuredHeight.toFloat()
        val bottomOffsetScreen = 52f * density
        val maxTargetY = ((imageHeight.toFloat() / photoScale) - cardHeightOnScreen).coerceAtLeast(0f)

        val targetY = if (data.relativeStampY != null) {
            (data.relativeStampY * (imageHeight.toFloat() / photoScale)).coerceIn(0f, maxTargetY)
        } else {
            ((imageHeight.toFloat() / photoScale) - cardHeightOnScreen - bottomOffsetScreen).coerceIn(0f, maxTargetY)
        }

        canvas.save()
        canvas.scale(photoScale, photoScale)
        canvas.translate(0f, targetY)
        view.draw(canvas)
        canvas.restore()
    }

    private fun createFallbackMapBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = 0xFFF2EFE9.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val mainRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 8f
        }
        canvas.drawLine(0f, height * 0.40f, width.toFloat(), height * 0.45f, mainRoadPaint)
        canvas.drawLine(width * 0.45f, 0f, width * 0.50f, height.toFloat(), mainRoadPaint)

        val secRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFE082.toInt()
            strokeWidth = 5f
        }
        canvas.drawLine(0f, height * 0.40f, width.toFloat(), height * 0.45f, secRoadPaint)
        canvas.drawLine(0f, height * 0.70f, width.toFloat(), height * 0.65f, mainRoadPaint)

        return bitmap
    }

    private fun drawCompassOverlay(
        context: Context,
        canvas: Canvas,
        imageWidth: Int,
        imageHeight: Int,
        density: Float,
        data: StampData,
        showAddress: Boolean = true,
        showTelemetry: Boolean = true,
        customTagline: String = "Simatrix GPS Camera"
    ) {
        // True WYSIWYG calibration:
        // Measuring against a 680px baseline and scaling to the photo's native resolution
        // increases typography, compass dial, and telemetry sizes by ~1.6x, producing exact 1:1
        // visual prominence matching the live viewfinder camera preview!
        val designWidth = 680f
        val overlayScale = (imageWidth.toFloat() / designWidth)
        val targetWidthPx = designWidth.toInt()
        val targetHeightPx = (imageHeight / overlayScale).toInt()

        // Match the live viewfinder's framing:
        // In the live preview, the overlay sits below the top action bar (~64-70dp)
        // and above the bottom shutter deck (~95-105dp).
        val topPaddingPx = (targetHeightPx * 0.080f).toInt()
        val bottomPaddingPx = (targetHeightPx * 0.115f).toInt()
        val sidePaddingPx = (targetWidthPx * 0.050f).toInt()

        // 1. Soft bottom gradient scrim matching the live viewfinder bottom deck gradient (bg_bottom_deck_gradient)
        // Guarantees 100% text readability over light/white backgrounds, table reflections, and glare
        val bottomScrimHeight = (bottomPaddingPx * overlayScale) + (260f * density * overlayScale)
        val bottomScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f,
                imageHeight - bottomScrimHeight,
                0f,
                imageHeight.toFloat(),
                intArrayOf(
                    0x00000000,
                    0x30000000,
                    0x8A000000.toInt(),
                    0xCC000000.toInt()
                ),
                floatArrayOf(0f, 0.35f, 0.70f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(
            0f,
            imageHeight - bottomScrimHeight,
            imageWidth.toFloat(),
            imageHeight.toFloat(),
            bottomScrimPaint
        )

        // 2. Subtle top gradient scrim for date & compass dial over sky/ceilings
        val topScrimHeight = (topPaddingPx * overlayScale) + (140f * density * overlayScale)
        val topScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                0f,
                topScrimHeight,
                intArrayOf(
                    0x55000000,
                    0x20000000,
                    0x00000000
                ),
                floatArrayOf(0f, 0.60f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(
            0f,
            0f,
            imageWidth.toFloat(),
            topScrimHeight,
            topScrimPaint
        )

        // 3. Inflate and bind template view
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.stamp_template_5_compass, null)

        view.setPadding(sidePaddingPx, topPaddingPx, sidePaddingPx, bottomPaddingPx)

        view.findViewById<TextView>(R.id.compass_date_tv).text = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        view.findViewById<TextView>(R.id.compass_time_tv).text = data.timeText
        view.findViewById<TextView>(R.id.compass_day_tv).text = SimpleDateFormat("EEEE", Locale.US).format(Date())

        val locBlock = view.findViewById<View>(R.id.compass_location_block)
        if (showAddress) {
            locBlock?.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.compass_short_address_tv).text = data.shortAddress.ifBlank { "Location Acquired" }
            view.findViewById<TextView>(R.id.compass_full_address_tv).text = data.fullAddress.ifBlank { data.shortAddress }
        } else {
            locBlock?.visibility = View.GONE
        }

        val telBlock = view.findViewById<View>(R.id.compass_telemetry_block)
        if (showTelemetry) {
            telBlock?.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.compass_altitude_tv).text = data.altitudeText.ifBlank { "14.7 m" }
            view.findViewById<TextView>(R.id.compass_accuracy_tv).text = data.accuracyText.ifBlank { "±32.0 m" }
            view.findViewById<TextView>(R.id.compass_lat_long_tv).text = data.latLongText
        } else {
            telBlock?.visibility = View.GONE
        }

        val bottomBar = view.findViewById<ViewGroup>(R.id.compass_bottom_bar)
        if (bottomBar != null && bottomBar.childCount >= 3) {
            val badgeTv = bottomBar.getChildAt(2) as? TextView
            if (badgeTv != null && customTagline.isNotBlank() && customTagline != "Simatrix GPS Camera") {
                badgeTv.text = customTagline
            }
        }

        // Rotate needle if compass azimuth provided
        val compassDial = view.findViewById<ImageView>(R.id.compass_dial_image)
        compassDial.rotation = -data.compassAzimuth

        view.measure(
            View.MeasureSpec.makeMeasureSpec(targetWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(targetHeightPx, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        canvas.save()
        canvas.scale(overlayScale, overlayScale)
        view.draw(canvas)
        canvas.restore()
    }
}
