package com.example.gpsmapcameraapp

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gpsmapcameraapp.focusview.FocusView
import com.example.gpsmapcameraapp.viewmodels.ImageSavingViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import android.app.Activity
import android.content.IntentSender
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import java.util.Locale

class MainActivity : AppCompatActivity(), FoldersActivity.FolderClickListener {
    private lateinit var previewView: PreviewView
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var flashLightIcon: ImageView
    private lateinit var cameraIcon: ImageView
    private var isBackCameraSelected = true // Flag to track the currently selected camera
    private lateinit var optionsIcon: ImageView
    private lateinit var cardView: CardView
    private var isOptionsIconSelected = false
    private lateinit var timerIcon: ImageView
    private lateinit var imageCaptureIcon: ImageView
    private var imageCapture: ImageCapture? = null
    private lateinit var collectionIcon: ImageView
    private lateinit var progressBarAnimation: LottieAnimationView
    private lateinit var threeToOneAnimationView: LottieAnimationView
    private lateinit var fiveToOneAnimationView: LottieAnimationView
    private lateinit var timerTv: TextView
    private lateinit var cityCountryTv: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var longLatTv: TextView
    private lateinit var cardViewGoogleMap: CardView
    private lateinit var soundOffIcon: ImageView
    private lateinit var soundOffTv: TextView
    private var isSoundOn = true
    private lateinit var focusManualIcon: ImageView
    private var isFocusManual = true
    private lateinit var focusView: FocusView
    private lateinit var focusTv: TextView
    private lateinit var focusAutoTv: TextView
    private val handler = Handler()
    private var isFirstClick = true
    private lateinit var whiteBalanceIcon: ImageView
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var crossIconScrollView: ImageView
    private lateinit var cardViewMainActivity: CardView
    private lateinit var autoEffectIcon: ImageView
    private lateinit var manualEffectIcon: ImageView
    private lateinit var incandescentEffectIcon: ImageView
    private lateinit var florescentEffectIcon: ImageView
    private lateinit var warmEffectIcon: ImageView
    private lateinit var dayLightEffectIcon: ImageView
    private lateinit var cloudyEffectIcon: ImageView
    private lateinit var twiiLightEffectIcon: ImageView
    private lateinit var shadeEffectIcon: ImageView
    private lateinit var foldersIcon: ImageView
    private lateinit var viewModel: ImageSavingViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var folderTv: TextView
    private lateinit var mapDataIcon: ImageView
    private lateinit var templateIcon: ImageView
    private lateinit var map: GoogleMap
    private lateinit var sharedPreferencesLatLong : SharedPreferences
    private lateinit var sharedPreferencesPlusCode: SharedPreferences
    private lateinit var sharedPreferencesTimeZone: SharedPreferences
    private lateinit var sharedPreferencesWeatherSelection: SharedPreferences
    private lateinit var cloudyIcon : ImageView
    private lateinit var windIcon : ImageView
    private lateinit var sharedPreferencesWindSelection: SharedPreferences
    private lateinit var pressureIcon : ImageView
    private lateinit var sharedPreferencesPressureSelection: SharedPreferences
    private lateinit var altitudeIcon : ImageView
    private lateinit var sharedPreferencesAltitudeText: SharedPreferences
    private lateinit var accuracyIcon : ImageView
    private lateinit var sharedPreferencesAccuracySelection: SharedPreferences
    private lateinit var locationDetailsCard : CardView
    private var locationCallback: LocationCallback? = null
    private var isResolvingLocation = false
    private var hasPromptedLocationOnResume = false
    private lateinit var gpsStatusTv: TextView
    private lateinit var gpsStatusDot: ImageView


    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        templateIcon = findViewById(R.id.template_icon)
        templateIcon.setOnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
            finish()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))


        previewView = findViewById(R.id.previewView)
        flashLightIcon = findViewById(R.id.flash_light_icon_main_activity)
        cameraIcon = findViewById(R.id.camera_change_icon_main_activity)
        optionsIcon = findViewById(R.id.options_icon_main_activity)
        cardView = findViewById(R.id.card_view_options_icon)
        timerIcon = findViewById(R.id.timer_off_icon)
        imageCaptureIcon = findViewById(R.id.image_capture_icon)
        collectionIcon = findViewById(R.id.collection_icon)
        progressBarAnimation = findViewById(R.id.progress_bar_animation)
        timerTv = findViewById(R.id.timer_off_tv)
        threeToOneAnimationView = findViewById(R.id.three_to_one_animation_view)
        fiveToOneAnimationView = findViewById(R.id.five_to_one_animation_view)
        cityCountryTv = findViewById(R.id.city_country_tv)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        longLatTv = findViewById(R.id.long_lat_tv)
        cardViewGoogleMap = findViewById(R.id.map_card_view)
        soundOffIcon = findViewById(R.id.sound_off_icon)
        soundOffTv = findViewById(R.id.sound_off_tv)
        focusManualIcon = findViewById(R.id.focus_manual_icon)
        focusView = findViewById(R.id.focusView)
        focusTv = findViewById(R.id.focus_manual_tv)
        focusAutoTv = findViewById(R.id.focus_auto_tv)
        whiteBalanceIcon = findViewById(R.id.white_balance_icon)
        horizontalScrollView = findViewById(R.id.horizontal_scroll_view_main)
        crossIconScrollView = findViewById(R.id.horizontal_scroll_view_main_cross_icon)
        cardViewMainActivity = findViewById(R.id.card_view_main_activity)
        autoEffectIcon = findViewById(R.id.auto_effect_icon)
        manualEffectIcon = findViewById(R.id.manual_effect_icon)
        incandescentEffectIcon = findViewById(R.id.incandescent_effect_icon)
        florescentEffectIcon = findViewById(R.id.flourescent_effect_icon)
        warmEffectIcon = findViewById(R.id.warm_effect_icon)
        dayLightEffectIcon = findViewById(R.id.day_light_effect_icon)
        cloudyEffectIcon = findViewById(R.id.cloudy_effect_icon)
        twiiLightEffectIcon = findViewById(R.id.day_light_effect_icon_2)
        shadeEffectIcon = findViewById(R.id.shade_effect_icon)
        foldersIcon = findViewById(R.id.default_icon)
        folderTv = findViewById(R.id.default_tv)
        mapDataIcon = findViewById(R.id.map_data_icon)
        cloudyIcon = findViewById(R.id.cloudy_icon_main)
        windIcon = findViewById(R.id.wind_icon_main)
        pressureIcon = findViewById(R.id.pressure_icon_main)
        altitudeIcon = findViewById(R.id.altitude_icon_main)
        accuracyIcon = findViewById(R.id.accuracy_icon_main)
        locationDetailsCard = findViewById(R.id.location_details_card_view)
        gpsStatusTv = findViewById(R.id.gps_status_tv)
        gpsStatusDot = findViewById(R.id.gps_status_dot)

        findViewById<View>(R.id.gps_status_pill)?.setOnClickListener {
            if (!isLocationServiceEnabled()) {
                promptEnableLocation()
            } else {
                startLocationUpdates()
                Toast.makeText(this, "Acquiring highest precision GPS fix...", Toast.LENGTH_SHORT).show()
            }
        }

        sharedPreferences = getSharedPreferences(PREF_NAME_MANUAL, Context.MODE_PRIVATE)
        sharedPreferencesLatLong = getSharedPreferences("LatLongSelection", Context.MODE_PRIVATE)
        sharedPreferencesPlusCode = getSharedPreferences("PLusCodeSelection", Context.MODE_PRIVATE)
        sharedPreferencesTimeZone = getSharedPreferences("TimeZoneSelection", Context.MODE_PRIVATE)
        sharedPreferencesWeatherSelection = getSharedPreferences("WeatherSelection", Context.MODE_PRIVATE)
        sharedPreferencesWindSelection = getSharedPreferences("WindSelection", Context.MODE_PRIVATE)
        sharedPreferencesPressureSelection = getSharedPreferences("PressureSelection", Context.MODE_PRIVATE)
        sharedPreferencesAltitudeText = getSharedPreferences("AltitudeSelection", Context.MODE_PRIVATE)
        sharedPreferencesAccuracySelection = getSharedPreferences("AccuracySelection", Context.MODE_PRIVATE)

        // Check if intent has extra data
        val line1 = intent.getStringExtra(KEY_LINE_1)
        val line2 = intent.getStringExtra(KEY_LINE_2)
        val line3 = intent.getStringExtra(KEY_LINE_3)

        if (line1 != null && line2 != null && line3 != null) {
            // Display the passed data from MapDataManualEntryActivity
            cityCountryTv.text = "$line1, $line2, $line3"

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {

                    findViewById<TextView>(R.id.long_lat_tv).text =
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"

                    val currentDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val currentTime =
                        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())

                    findViewById<TextView>(R.id.date_day_time_tv).text =
                        "$currentDate, $currentDay, $currentTime"
                }
                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                mapFragment.getMapAsync { googleMap ->
                    // Add a marker at the current location and move the camera
                    val latLng = location?.let { LatLng(it.latitude, location.longitude) }
                    latLng?.let { MarkerOptions().position(it).title("Marker") }
                        ?.let { googleMap.addMarker(it) }
                    latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15f) }
                        ?.let { googleMap.moveCamera(it) }
                }
            }
        }

        val openMapData = View.OnClickListener {
            val intent = Intent(this, MapDataAutomaticActivity::class.java)
            startActivity(intent)
        }
        mapDataIcon.setOnClickListener(openMapData)
        findViewById<TextView>(R.id.map_data_tv).setOnClickListener(openMapData)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        viewModel = ViewModelProvider(this)[ImageSavingViewModel::class.java]

        restoreEffectState("autoEffectIcon")
        restoreEffectState("manualEffectIcon")
        restoreEffectState("incandescentEffectIcon")
        restoreEffectState("florescentEffectIcon")
        restoreEffectState("warmEffectIcon")
        restoreEffectState("dayLightEffectIcon")
        restoreEffectState("cloudyEffectIcon")
        restoreEffectState("twiiLightEffectIcon")
        restoreEffectState("shadeEffectIcon")

        val openFolders = View.OnClickListener {
            val intent = Intent(this, FoldersActivity::class.java)
            startActivity(intent)
        }
        foldersIcon.setOnClickListener(openFolders)
        folderTv.setOnClickListener(openFolders)

        val openTemplate = View.OnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }
        templateIcon.setOnClickListener(openTemplate)
        findViewById<TextView>(R.id.template_tv).setOnClickListener(openTemplate)


        whiteBalanceIcon.setOnClickListener {
            cardViewMainActivity.visibility = View.INVISIBLE
            horizontalScrollView.visibility = View.VISIBLE
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            incandescentEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null
        }

        autoEffectIcon.setOnClickListener {

            autoEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            manualEffectIcon.colorFilter = null
            incandescentEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null

            // Remove any color effect from the preview view
            previewView.foreground = null
            // Save state
            saveEffectState("autoEffectIcon", Color.RED, null)

        }

        manualEffectIcon.setOnClickListener {

            manualEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            autoEffectIcon.colorFilter = null
            incandescentEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null

            // Apply grayscale effect to the preview view
            val overlayColor = ColorDrawable(Color.parseColor("#A0000020"))
            previewView.foreground = overlayColor

            // Save state
            saveEffectState("manualEffectIcon", Color.RED, Color.parseColor("#A0000020"))

        }

        incandescentEffectIcon.setOnClickListener {

            incandescentEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#90608080"))
            previewView.foreground = overlayColor

            saveEffectState("incandescentEffectIcon", Color.RED, Color.parseColor("#90608080"))
        }

        florescentEffectIcon.setOnClickListener {
            florescentEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            incandescentEffectIcon.colorFilter = null
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#40000000"))
            previewView.foreground = overlayColor

            saveEffectState("florescentEffectIcon", Color.RED, Color.parseColor("#40000000"))

        }

        warmEffectIcon.setOnClickListener {

            warmEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null
            incandescentEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#90608080"))
            previewView.foreground = overlayColor

            saveEffectState("warmEffectIcon", Color.RED, Color.parseColor("#90608080"))

        }

        dayLightEffectIcon.setOnClickListener {

            dayLightEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            incandescentEffectIcon.colorFilter = null
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null

            previewView.foreground = null

            saveEffectState("dayLightEffectIcon", Color.RED, null)

        }

        cloudyEffectIcon.setOnClickListener {

            cloudyEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            incandescentEffectIcon.colorFilter = null
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#40000000"))
            previewView.foreground = overlayColor

            saveEffectState("cloudyEffectIcon", Color.RED, Color.parseColor("#40000000"))

        }

        twiiLightEffectIcon.setOnClickListener {

            twiiLightEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            incandescentEffectIcon.colorFilter = null
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            shadeEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#40000030"))
            previewView.foreground = overlayColor

            saveEffectState("twiiLightEffectIcon", Color.RED, Color.parseColor("#40000030"))

        }

        shadeEffectIcon.setOnClickListener {

            shadeEffectIcon.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY)
            incandescentEffectIcon.colorFilter = null
            twiiLightEffectIcon.colorFilter = null
            autoEffectIcon.colorFilter = null
            manualEffectIcon.colorFilter = null
            florescentEffectIcon.colorFilter = null
            dayLightEffectIcon.colorFilter = null
            cloudyEffectIcon.colorFilter = null
            warmEffectIcon.colorFilter = null

            val overlayColor = ColorDrawable(Color.parseColor("#40000000"))
            previewView.foreground = overlayColor

            saveEffectState("shadeEffectIcon", Color.RED, Color.parseColor("#40000000"))

        }


        crossIconScrollView.setOnClickListener {
            horizontalScrollView.visibility = View.INVISIBLE
            cardViewMainActivity.visibility = View.VISIBLE
        }

        // Set the focus icon initially invisible
        // Set the focus icon initially invisible
        focusView.visibility = View.INVISIBLE

        // Track the last click time for auto focus
        System.currentTimeMillis()

        // Modify your onTouchListener to call this function when ACTION_DOWN is detected
        previewView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isFocusManual) {
                        focusView.x = event.x - focusView.width / 2
                        focusView.y = event.y - focusView.height / 2
                        showFocusIconFor3Seconds()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    // Hide the focus icon when touch is released
                    focusView.visibility = View.INVISIBLE
                }
            }
            true // Consume the touch event
        }

        // Start a timer to show the focus icon every 8 seconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isFocusManual) {
                    focusView.visibility = View.VISIBLE
                    // Add any zoom in and out animation here if needed
                    handler.postDelayed({
                        focusView.visibility = View.INVISIBLE
                    }, 3000) // Hide the focus icon after 3 seconds
                }

                // Repeat the timer every 8 seconds
                handler.postDelayed(this, 8000)
            }
        }, 8000) // Start the timer after 8 seconds


        focusManualIcon.setOnClickListener {
            isFocusManual = !isFocusManual
            if (isFocusManual) {
                isFirstClick = true
                focusManualIcon.setImageResource(R.drawable.focus_manual_icon)
                focusView.setFocusShape(FocusView.FOCUS_SHAPE_SQUARE)
                focusTv.visibility = View.VISIBLE
                focusAutoTv.visibility = View.INVISIBLE
                focusView.setManualFocus(true)
                // Show the focus icon for 3 seconds if in manual focus mode
                showFocusIconFor3Seconds()
            } else {
                focusView.visibility = View.VISIBLE
                isFirstClick = true
                focusManualIcon.setImageResource(R.drawable.focus_auto)
                focusView.setFocusShape(FocusView.FOCUS_SHAPE_CIRCLE)
                focusTv.visibility = View.INVISIBLE
                focusAutoTv.visibility = View.VISIBLE
                focusView.setManualFocus(false)
            }
        }
        restoreSoundState()

        soundOffIcon.setOnClickListener {
            isSoundOn = !isSoundOn
            if (isSoundOn) {
                soundOffIcon.setImageResource(R.drawable.sound_on_icon)
                soundOffTv.text = "Sound On"
            } else {
                soundOffIcon.setImageResource(R.drawable.sound_off_icon)
                soundOffTv.text = "Sound Off"
            }
            // Save the sound state
            saveSoundState(isSoundOn)
        }

        val capturedByTv = findViewById<TextView>(R.id.captured_by_tv)
        // Set the text
        capturedByTv.text = "Built by : Simatrix GPS Camera"

        // Request location updates
        if (isLocationServiceEnabled()) {
            startLocationUpdates()
        } else {
            cityCountryTv.text = "Location is off. Tap to enable."
            longLatTv.text = "GPS coordinates unavailable"
        }

        locationDetailsCard.setOnClickListener {
            if (!isLocationServiceEnabled()) {
                promptEnableLocation()
            } else {
                startLocationUpdates()
                Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
            }
        }

        cardViewGoogleMap.setOnClickListener {
            if (!isLocationServiceEnabled()) {
                promptEnableLocation()
            } else {
                startLocationUpdates()
            }
        }

        val rootLayout =
            findViewById<View>(R.id.root_layout_activity_main) // Assuming 'root_layout' is the ID of your root layout
        rootLayout.setOnClickListener {
            if (!isOptionsIconSelected) {
                optionsIcon.setImageResource(R.drawable.options_icon_main_activity)
                cardView.visibility = View.INVISIBLE
            }
            isOptionsIconSelected = false
        }

        val openCollection = View.OnClickListener {
            val intent = Intent(this, CollectionActivity::class.java)
            startActivity(intent)
        }
        collectionIcon.setOnClickListener(openCollection)
        findViewById<TextView>(R.id.collection_tv).setOnClickListener(openCollection)

        flashLightIcon.setOnClickListener {
            toggleFlashlight()
        }

        imageCaptureIcon.setOnClickListener {
            if (!isLocationServiceEnabled()) {
                AlertDialog.Builder(this)
                    .setTitle("Location Turned Off")
                    .setMessage("Location is turned off. Turn on location so GPS coordinates and address can be stamped on your photo?")
                    .setPositiveButton("Turn On") { _, _ ->
                        promptEnableLocation()
                    }
                    .setNegativeButton("Capture Anyway") { _, _ ->
                        executeImageCapture()
                    }
                    .show()
                return@setOnClickListener
            }
            executeImageCapture()
        }
        var clickCount = 0

        timerIcon.setOnClickListener {
            clickCount++
            when (clickCount % 3) {
                1 -> {
                    timerIcon.setImageResource(R.drawable.selected_timer_icon)
                    timerTv.text = "Timer 3sec"
                    saveTimerState("Timer 3sec", R.drawable.selected_timer_icon)
                }

                2 -> {
                    timerIcon.setImageResource(R.drawable.selected_timer_icon)
                    timerTv.text = "Timer 5sec"
                    saveTimerState("Timer 5sec", R.drawable.selected_timer_icon)
                }

                else -> {
                    timerIcon.setImageResource(R.drawable.timer_off_icon)
                    timerTv.text = "Timer Off"
                    saveTimerState("Timer Off", R.drawable.timer_off_icon)
                }
            }
        }


        optionsIcon.setOnClickListener {
            if (!isOptionsIconSelected) {
                optionsIcon.setImageResource(R.drawable.options_selected_icon)
                cardView.visibility = View.VISIBLE

            } else {
                optionsIcon.setImageResource(R.drawable.options_icon_main_activity)
                cardView.visibility = View.INVISIBLE
            }
            isOptionsIconSelected = !isOptionsIconSelected

        }


        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Check if all permissions are granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            checkLocationSettingsAndStart()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var cameraPermissionGranted = false
            var locationPermissionGranted = false

            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.CAMERA && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionGranted = true
                }
                if ((permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION || permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }

            if (cameraPermissionGranted && locationPermissionGranted) {
                startCamera()
                checkLocationSettingsAndStart()
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                flashLightIcon.setOnClickListener {
                    val camera = cameraProvider.bindToLifecycle(this, cameraSelector)

                    if (camera.cameraInfo.hasFlashUnit()) {
                        val cameraControl = camera.cameraControl
                        val torchState = camera.cameraInfo.torchState.value
                        val isTorchOn = torchState == TorchState.ON

                        // Check if the back camera is selected before changing the icon
                        if (isBackCameraSelected) {
                            if (isTorchOn) {
                                flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
                            } else {
                                flashLightIcon.setImageResource(R.drawable.flash_on_icon)
                            }

                            cameraControl.enableTorch(!isTorchOn)
                        }
                    }
                }

                cameraIcon.setOnClickListener {
                    val newCameraSelector = if (isBackCameraSelected) {
                        CameraSelector.DEFAULT_FRONT_CAMERA

                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        // Unbind all use cases before rebinding with the new camera selector
                        cameraProvider.unbindAll()

                        // Bind use cases to camera with the new camera selector
                        cameraProvider.bindToLifecycle(
                            this,
                            newCameraSelector,
                            preview,
                            imageCapture
                        )

                        if (!isBackCameraSelected) {
                            flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
                        } else {
                            flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
                        }

                        isBackCameraSelected =
                            !isBackCameraSelected // Toggle the camera selection flag

                        cameraIcon.animate().rotationBy(180f).setDuration(300).start()
                        // Update the camera icon based on the selected camera
                        cameraIcon.setImageResource(if (isBackCameraSelected) R.drawable.camera_change_icon_main_activity else R.drawable.front_cam_icon)
                    } catch (exc: Exception) {
                        Log.e(TAG, "Use case binding failed", exc)
                    }
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun executeImageCapture() {
        imageCaptureIcon.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction {
            imageCaptureIcon.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        }.start()

        // Check if the timer is set to 3 seconds
        when (timerTv.text) {
            "Timer 3sec" -> {
                threeToOneAnimationView.visibility = View.VISIBLE
                threeToOneAnimationView.playAnimation()
                threeToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun onAnimationEnd(animation: Animator) {
                        // Stop the animation
                        threeToOneAnimationView.clearAnimation()

                        // Play the sound of the camera if sound is on
                        if (isSoundOn) {
                            playCameraSound()
                        }

                        // Capture the image after stopping the animation
                        if (sharedPreferences.getBoolean("isDefaultFolderSelected", true)) {
                            val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                            viewModel.captureAndSaveImageDefault(this@MainActivity, previewView, cardView1, cardView2, mapFragment)

                        } else if (sharedPreferences.getBoolean(
                                "isSiteOneFolderSelected",
                                true
                            )
                        ) {
                            val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                            viewModel.captureAndSaveImageSiteOne(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                        } else if (sharedPreferences.getBoolean(
                                "isSiteTwoFolderSelected",
                                true
                            )
                        ) {
                            val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                            viewModel.captureAndSaveImageSiteTwo(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                        } else {
                            captureImage()
                        }
                    }
                })
            }

            "Timer 5sec" -> {
                fiveToOneAnimationView.visibility = View.VISIBLE
                fiveToOneAnimationView.playAnimation()
                fiveToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Stop the animation
                        fiveToOneAnimationView.clearAnimation()

                        // Play the sound of the camera if sound is on
                        if (isSoundOn) {
                            playCameraSound()
                        }
                        if (sharedPreferences.getBoolean("isDefaultFolderSelected", true)) {
                            val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                            viewModel.captureAndSaveImageDefault(this@MainActivity, previewView, cardView1, cardView2, mapFragment)

                        } else if (sharedPreferences.getBoolean(
                                "isSiteOneFolderSelected",
                                true
                            )
                        ) {
                            val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                            viewModel.captureAndSaveImageSiteOne(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                        } else if (sharedPreferences.getBoolean(
                                "isSiteTwoFolderSelected",
                                true
                            )
                        ) { val cardView1 = findViewById<View>(R.id.location_details_card_view)
                            val cardView2 = findViewById<View>(R.id.map_card_view)
                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

                            viewModel.captureAndSaveImageSiteTwo(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                        } else {
                            captureImage()
                        }
                    }
                })
            }

            else -> {
                // Play the sound of the camera if sound is on
                if (isSoundOn) {
                    playCameraSound()
                }
                val cardView1 = findViewById<View>(R.id.location_details_card_view)
                val cardView2 = findViewById<View>(R.id.map_card_view)
                val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                // Capture the image after stopping the animation
                if (sharedPreferences.getBoolean("isDefaultFolderSelected", true)) {
                    viewModel.captureAndSaveImageDefault(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                } else if (sharedPreferences.getBoolean("isSiteOneFolderSelected", true)) {
                    viewModel.captureAndSaveImageSiteOne(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                } else if (sharedPreferences.getBoolean("isSiteTwoFolderSelected", true)) {
                    viewModel.captureAndSaveImageSiteTwo(this@MainActivity, previewView, cardView1, cardView2, mapFragment)
                } else {
                    captureImage()
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationSettingsAndStart() {
        if (!hasLocationPermission()) return
        if (isLocationServiceEnabled()) {
            if (::gpsStatusTv.isInitialized) gpsStatusTv.text = "Acquiring GPS..."
            startLocationUpdates()
        } else {
            if (::gpsStatusTv.isInitialized) gpsStatusTv.text = "Location Off"
            cityCountryTv.text = "Location is off. Tap to enable."
            longLatTv.text = "GPS coordinates unavailable"
            promptEnableLocation()
        }
    }

    private fun promptEnableLocation() {
        if (!hasLocationPermission()) {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener(this) {
            startLocationUpdates()
        }

        task.addOnFailureListener(this) { exception ->
            if (exception is ResolvableApiException) {
                try {
                    isResolvingLocation = true
                    exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    showFallbackLocationSettingsDialog()
                }
            } else {
                showFallbackLocationSettingsDialog()
            }
        }
    }

    private fun showFallbackLocationSettingsDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Turn On Location")
            .setMessage("Location services are required to show your address and stamp GPS coordinates on photos. Please turn on Location in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            isResolvingLocation = false
            if (resultCode == Activity.RESULT_OK) {
                if (::gpsStatusTv.isInitialized) gpsStatusTv.text = "Acquiring GPS..."
                Toast.makeText(this, "Location enabled. Acquiring GPS...", Toast.LENGTH_SHORT).show()
                startLocationUpdates()
            } else {
                if (::gpsStatusTv.isInitialized) gpsStatusTv.text = "Location Off"
                Toast.makeText(this, "Location is disabled. Tap location card to enable.", Toast.LENGTH_LONG).show()
                cityCountryTv.text = "Location is off. Tap to enable."
                longLatTv.text = "GPS coordinates unavailable"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        if (!isLocationServiceEnabled()) {
            promptEnableLocation()
            return
        }

        // Try getting last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                updateLocationDetails(location)
            }
        }

        // Request fresh current location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { freshLocation: Location? ->
                if (freshLocation != null) {
                    updateLocationDetails(freshLocation)
                }
            }

        // Setup real-time updates so movement or newly acquired fix automatically updates the UI
        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (loc in locationResult.locations) {
                        if (loc != null) {
                            updateLocationDetails(loc)
                            break
                        }
                    }
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        fusedLocationClient.removeLocationUpdates(locationCallback!!)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun requestLocationUpdates() {
        checkLocationSettingsAndStart()
    }

    @SuppressLint("SetTextI18n")
    private fun displayCurrentLocation() {
        checkLocationSettingsAndStart()
    }

    @SuppressLint("SetTextI18n")
    private fun updateLocationDetails(location: Location) {
        val address = getAddress(location.latitude, location.longitude)
        val exactAddress = if (address != null) {
            val fullLine = if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) else null
            if (!fullLine.isNullOrBlank()) {
                fullLine
            } else {
                listOfNotNull(
                    address.subThoroughfare,
                    address.thoroughfare,
                    address.subLocality,
                    address.locality,
                    address.adminArea,
                    address.postalCode,
                    address.countryName
                ).filter { it.isNotBlank() }.joinToString(", ")
            }
        } else {
            "Lat: ${String.format(Locale.US, "%.5f", location.latitude)}, Long: ${String.format(Locale.US, "%.5f", location.longitude)}"
        }
        cityCountryTv.text = exactAddress

        val latFormatted = String.format(Locale.US, "%.6f", Math.abs(location.latitude))
        val lonFormatted = String.format(Locale.US, "%.6f", Math.abs(location.longitude))
        val latDir = if (location.latitude >= 0) "N" else "S"
        val lonDir = if (location.longitude >= 0) "E" else "W"
        findViewById<TextView>(R.id.long_lat_tv).text = "Lat $latFormatted° $latDir  Long $lonFormatted° $lonDir"

        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.date_day_time_tv).text = "$currentDate, $currentDay, $currentTime"

        val timeZoneTextView = findViewById<TextView>(R.id.time_zone_tv_main_template)
        timeZoneTextView.text = "GMT " + SimpleDateFormat("XXX", Locale.getDefault()).format(Date())
        timeZoneTextView.visibility = View.VISIBLE

        val altitudeTextView = findViewById<TextView>(R.id.altitude_tv_main_template)
        if (location.hasAltitude() && location.altitude != 0.0) {
            altitudeTextView.text = "${String.format(Locale.US, "%.1f", location.altitude)} m"
            altitudeTextView.visibility = View.VISIBLE
            altitudeIcon.visibility = View.VISIBLE
        } else {
            altitudeTextView.visibility = View.GONE
            altitudeIcon.visibility = View.GONE
        }

        val accuracyTextView = findViewById<TextView>(R.id.accuracy_tv_template_main)
        if (location.hasAccuracy()) {
            accuracyTextView.text = "±${String.format(Locale.US, "%.1f", location.accuracy)} m"
            accuracyTextView.visibility = View.VISIBLE
            accuracyIcon.visibility = View.VISIBLE
            if (::gpsStatusTv.isInitialized) {
                gpsStatusTv.text = "GPS Active ±${location.accuracy.toInt()}m"
            }
        } else {
            accuracyTextView.visibility = View.GONE
            accuracyIcon.visibility = View.GONE
            if (::gpsStatusTv.isInitialized) {
                gpsStatusTv.text = "GPS Active"
            }
        }

        // Hide unused placeholders so they never display "Lat/Long" dummy text
        findViewById<TextView>(R.id.plus_code_tv_main_location_details_card).visibility = View.GONE
        findViewById<TextView>(R.id.weather_tv_main_template_card).visibility = View.GONE
        cloudyIcon.visibility = View.GONE
        findViewById<TextView>(R.id.wind_tv_main_template).visibility = View.GONE
        windIcon.visibility = View.GONE
        findViewById<TextView>(R.id.pressure_tv_main_template).visibility = View.GONE
        pressureIcon.visibility = View.GONE

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): Address? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) addresses[0] else null
        } catch (e: Exception) {
            null
        }
    }

    fun updatePlusCodeText(selectedText: String) {
        val plusCodeTextView = findViewById<TextView>(R.id.plus_code_tv_main_location_details_card)
        plusCodeTextView.text = selectedText
    }

    fun updateTimeZoneText(selectedText: String){
        val timeZoneTextView = findViewById<TextView>(R.id.time_zone_tv_main_template)
        timeZoneTextView.text = selectedText
    }

    fun updateWeatherText(selectedText: String){
        val weatherTextView = findViewById<TextView>(R.id.weather_tv_main_template_card)
        weatherTextView.text = selectedText
        cloudyIcon.visibility = View.VISIBLE
    }

    fun updateWindText(selectedText: String){
        val windTextView = findViewById<TextView>(R.id.wind_tv_main_template)
        windTextView.text = selectedText
        windIcon.visibility = View.VISIBLE
    }

    fun updatePressureText(selectedText: String){
        val pressureTextView = findViewById<TextView>(R.id.pressure_tv_main_template)
        pressureTextView.text = selectedText
        pressureIcon.visibility = View.VISIBLE
    }

    fun updateAltitudeText(selectedText: String){
        val altitudeTextView = findViewById<TextView>(R.id.altitude_tv_main_template)
        altitudeTextView.text = selectedText
        altitudeIcon.visibility = View.VISIBLE
    }

    fun updateAccuracyText(selectedText: String){
        val accuracyTextView = findViewById<TextView>(R.id.accuracy_tv_template_main)
        accuracyTextView.text = selectedText
        accuracyIcon.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }

    }


    private fun toggleFlashlight() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector)
        if (camera.cameraInfo.hasFlashUnit()) {
            val cameraControl = camera.cameraControl
            val torchState = camera.cameraInfo.torchState.value
            val isTorchOn = torchState == TorchState.ON

            // Check if the back camera is selected before changing the icon
            if (!isBackCameraSelected) {
                cameraControl.enableTorch(!isTorchOn)
                flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
            }
        }
    }



    private fun updateMapTypeFromTemplate(mapType: Int) {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            googleMap.mapType = mapType
        }
    }

    fun updateLatLongText(selectedText: String) {
        val longLatTextView = findViewById<TextView>(R.id.long_lat_tv)
        longLatTextView.text = selectedText
    }

    override fun onResume() {
        super.onResume()
        // Retrieve the selected map type from SharedPreferences
        val sharedPreferences = getSharedPreferences("MapPreferences", Context.MODE_PRIVATE)
        val selectedMapType = sharedPreferences.getInt("SelectedMapType", GoogleMap.MAP_TYPE_NORMAL)
        updateMapTypeFromTemplate(selectedMapType)
        cameraIcon.setImageResource(R.drawable.camera_change_icon_main_activity)
        flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
        restoreTimerState()
        updateCollectionThumbnail()
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (availability != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 0)?.show()
        }

        if (hasLocationPermission()) {
            if (isLocationServiceEnabled()) {
                startLocationUpdates()
            } else if (!isResolvingLocation && !hasPromptedLocationOnResume) {
                hasPromptedLocationOnResume = true
                promptEnableLocation()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun updateCollectionThumbnail() {
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        executor.execute {
            var latestFile: File? = null

            val dirs = listOf(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Map Camera App/Site 1"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Map Camera App/Site 2")
            )

            for (dir in dirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { f ->
                        f.isFile && (f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) || f.name.endsWith(".png", true))
                    }
                    val recent = files?.maxByOrNull { it.lastModified() }
                    if (recent != null && (latestFile == null || recent.lastModified() > latestFile.lastModified())) {
                        latestFile = recent
                    }
                }
            }

            if (latestFile != null) {
                try {
                    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                    val bitmap = BitmapFactory.decodeFile(latestFile.absolutePath, options)
                    if (bitmap != null) {
                        runOnUiThread {
                            collectionIcon.setImageBitmap(bitmap)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        // Create a file to save the captured image
        val photoFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        if (timerTv.text != "Timer 3sec" && timerTv.text != "Timer 5sec") {
            progressBarAnimation.visibility = View.VISIBLE
            progressBarAnimation.playAnimation()
        }

        // Capture the image and save it to the specified file
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri ?: Uri.fromFile(photoFile)

                    // Load the captured image
                    val capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    // Create a mutable bitmap to combine the captured image and overlay views
                    val combinedBitmap = Bitmap.createBitmap(capturedBitmap.width, capturedBitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(combinedBitmap)
                    canvas.drawBitmap(capturedBitmap, 0f, 0f, null)

                    // Define the scale factor and new positions
                    val scaleFactor = 3.5f

                    // Calculate card positions based on the image height and width
                    val imageHeight = capturedBitmap.height.toFloat()
                    val imageWidth = capturedBitmap.width.toFloat()

                    val cardView1 = findViewById<View>(R.id.location_details_card_view) // replace with your first card view id
                    val cardView2 = findViewById<View>(R.id.map_card_view) // replace with your second card view id

                    // Measure card views to get their dimensions
                    cardView1.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    cardView2.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

                    val cardWidth1 = cardView1.measuredWidth * scaleFactor
                    val cardWidth2 = cardView2.measuredWidth * scaleFactor
                    val cardHeight = cardView1.measuredHeight * scaleFactor // Assuming both cards have the same height

                    // Calculate horizontal spacing
                    val spacing = 140f * scaleFactor

                    val totalWidth = cardWidth1 + spacing + cardWidth2
                    val leftMargin = (imageWidth - totalWidth) / 2

                    val card2Position = PointF(leftMargin, imageHeight - cardHeight - 10f)
                    val card1Position = PointF(leftMargin + cardWidth2 + spacing, imageHeight - cardHeight - 10f)
                    // Draw the first card view onto the canvas
                    drawViewOnCanvas(cardView1, canvas, scaleFactor, card1Position)

                    // Draw the second card view onto the canvas
                    drawViewOnCanvas(cardView2, canvas, scaleFactor, card2Position)

                    // Draw the map fragment onto the canvas
                    val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
                    mapFragment.getMapAsync { googleMap ->
                        googleMap.snapshot { mapBitmap ->
                            if (mapBitmap != null) {
                                drawMapOnCanvas(mapBitmap, canvas, scaleFactor, card2Position)

                                // Save the combined bitmap to a new file
                                saveCombinedBitmap(combinedBitmap, photoFile)

                                // Hide the progress bars and animations
                                progressBarAnimation.visibility = View.INVISIBLE
                                threeToOneAnimationView.visibility = View.INVISIBLE
                                fiveToOneAnimationView.visibility = View.INVISIBLE

                                // Display a message indicating that the image has been saved to the gallery
                                Toast.makeText(
                                    this@MainActivity,
                                    "Image saved to gallery",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Broadcast the saved image to the media scanner so it is immediately available in the gallery
                                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                mediaScanIntent.data = Uri.fromFile(photoFile)
                                sendBroadcast(mediaScanIntent)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    progressBarAnimation.visibility = View.INVISIBLE
                    threeToOneAnimationView.visibility = View.INVISIBLE
                    fiveToOneAnimationView.visibility = View.INVISIBLE
                    // Image capture failed, handle error
                    Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to save image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun drawViewOnCanvas(view: View, canvas: Canvas, scaleFactor: Float, position: PointF) {
        // Create a temporary bitmap and canvas to draw the view
        val tempBitmap = Bitmap.createBitmap((view.width * scaleFactor).toInt(), (view.height * scaleFactor).toInt(), Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)

        // Scale the view
        tempCanvas.scale(scaleFactor, scaleFactor)
        view.draw(tempCanvas)

        // Draw the temporary bitmap onto the main canvas at the specified position
        canvas.drawBitmap(tempBitmap, position.x, position.y, null)
    }

    private fun drawMapOnCanvas(
        mapBitmap: Bitmap,
        canvas: Canvas,
        scaleFactor: Float,
        position: PointF
    ) {
        // Create a scaled bitmap for the map
        val scaledMapBitmap = Bitmap.createScaledBitmap(mapBitmap, (mapBitmap.width * scaleFactor).toInt(), (mapBitmap.height * scaleFactor).toInt(), true)

        // Draw the scaled map bitmap onto the main canvas at the specified position
        canvas.drawBitmap(scaledMapBitmap, position.x, position.y, null)
    }
    private fun saveCombinedBitmap(bitmap: Bitmap, file: File) {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun saveTimerState(timerState: String, iconResId: Int) {
        val sharedPref = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("timer_state_key", timerState)
            putInt("timer_icon_key", iconResId)
            apply()
        }
    }

    private fun restoreTimerState() {
        val sharedPref = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
        val timerState = sharedPref.getString("timer_state_key", "Timer Off")
        val iconResId = sharedPref.getInt("timer_icon_key", R.drawable.timer_off_icon)
        timerIcon.setImageResource(iconResId)
        timerTv.text = timerState
    }

    private fun playCameraSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.camera_shutter_6305)
        mediaPlayer?.start()
    }

    private fun saveSoundState(isSoundOn: Boolean) {
        val sharedPref = getSharedPreferences("sound_state", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_sound_on", isSoundOn)
            apply()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun restoreSoundState() {
        val sharedPref = getSharedPreferences("sound_state", Context.MODE_PRIVATE)
        isSoundOn = sharedPref.getBoolean("is_sound_on", true)
        if (isSoundOn) {
            soundOffIcon.setImageResource(R.drawable.sound_on_icon)
            soundOffTv.text = "Sound On"
        } else {
            soundOffIcon.setImageResource(R.drawable.sound_off_icon)
            soundOffTv.text = "Sound Off"
        }
    }

    private fun showFocusIconFor3Seconds() {
        focusView.visibility = View.VISIBLE
        handler.postDelayed({
            focusView.visibility = View.INVISIBLE
        }, 3000) // 3000 milliseconds = 3 seconds
    }

    fun updateMapType(mapType: Int) {
        map.mapType = mapType
        // Save the selected map type
        val sharedPreferences = getSharedPreferences("MapSelection", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("SelectedMapType", mapType)
        editor.apply()
    }

    private fun saveEffectState(iconId: String, color: Int?, overlayColor: Int?) {
        val sharedPreferences = getSharedPreferences("EffectState", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("$iconId-color", color ?: -1)
        editor.putInt("$iconId-overlayColor", overlayColor ?: -1)
        editor.apply()
    }

    private fun restoreEffectState(iconId: String) {
        val sharedPreferences = getSharedPreferences("EffectState", Context.MODE_PRIVATE)
        val color = sharedPreferences.getInt("$iconId-color", -1)
        val overlayColor = sharedPreferences.getInt("$iconId-overlayColor", -1)

        when (iconId) {
            "autoEffectIcon" -> {
                autoEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "manualEffectIcon" -> {
                manualEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }

            }

            "incandescentEffectIcon" -> {
                incandescentEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "florescentEffectIcon" -> {
                florescentEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }

            }

            "warmEffectIcon" -> {
                warmEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "dayLightEffectIcon" -> {
                dayLightEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "cloudyEffectIcon" -> {
                cloudyEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "twiiLightEffectIcon" -> {
                twiiLightEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

            "shadeEffectIcon" -> {
                shadeEffectIcon.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                if (overlayColor != -1) {
                    val overlay = ColorDrawable(overlayColor)
                    previewView.foreground = overlay
                }
            }

        }
    }

    override fun onFolderClicked() {
    }

    private companion object {
        private const val PREF_NAME_MANUAL = "MapDataManualEntryPrefs"
        private const val KEY_LINE_1 = "key_line_1"
        private const val KEY_LINE_2 = "key_line_2"
        private const val KEY_LINE_3 = "key_line_3"
        private const val REQUEST_CHECK_SETTINGS = 1001
    }


}