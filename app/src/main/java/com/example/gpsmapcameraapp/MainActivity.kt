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
import android.graphics.Matrix
import android.graphics.PointF
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gpsmapcameraapp.focusview.FocusView
import com.example.gpsmapcameraapp.viewmodels.ImageSavingViewModel
import com.example.gpsmapcameraapp.models.StampData
import com.example.gpsmapcameraapp.models.StampTemplate
import com.example.gpsmapcameraapp.utils.StampRenderer
import com.example.gpsmapcameraapp.models.AspectRatioMode
import com.example.gpsmapcameraapp.models.CameraTimerMode
import android.os.CountDownTimer
import androidx.transition.TransitionManager
import com.example.gpsmapcameraapp.fragments.TemplateSelectorBottomSheetDialog
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
    private var camera: Camera? = null
    private lateinit var optionsIcon: ImageView
    private lateinit var cardView: CardView
    private var isOptionsIconSelected = false
    private var selectedAspectRatio = AspectRatioMode.RATIO_16_9
    private var selectedTimer = CameraTimerMode.OFF
    private lateinit var topRatioTv: TextView
    private lateinit var btnQuickOptions: View
    private lateinit var countdownTextView: TextView
    private var activeCountDownTimer: CountDownTimer? = null
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
    private var gpsStatusTv: TextView? = null
    private var gpsStatusDot: ImageView? = null
    private lateinit var settingsButton: View
    private var selectedTemplate: StampTemplate = StampTemplate.MODERN_CARD
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var currentCompassAzimuth: Float = 0f
    private var latestMapBitmap: Bitmap? = null

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthInDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentCompassAzimuth = (azimuthInDeg + 360f) % 360f
                updateLiveCompassDial(currentCompassAzimuth)
            } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                currentCompassAzimuth = (event.values[0] + 360f) % 360f
                updateLiveCompassDial(currentCompassAzimuth)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateLiveCompassDial(azimuth: Float) {
        if (selectedTemplate == StampTemplate.CLEAN_OVERLAY) {
            findViewById<ImageView>(R.id.compass_dial_image)?.rotation = -azimuth
        }
    }

    private var orientationEventListener: OrientationEventListener? = null
    private var currentUIRotation: Float = 0f

    private fun setupOrientationEventListener() {
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val targetRotation = when {
                    orientation in 45..134 -> 270f   // Landscape right (phone tilted clockwise)
                    orientation in 135..224 -> 180f  // Reverse portrait
                    orientation in 225..314 -> 90f   // Landscape left (phone tilted counter-clockwise)
                    else -> 0f                       // Normal portrait
                }

                if (targetRotation != currentUIRotation) {
                    currentUIRotation = targetRotation
                    animateControlIconsRotation(targetRotation)
                }
            }
        }
    }

    private fun animateControlIconsRotation(targetAngle: Float) {
        val viewsToRotate = listOfNotNull(
            // Top action bar icons
            findViewById<View>(R.id.map_data_icon),
            findViewById<View>(R.id.default_icon),
            findViewById<View>(R.id.template_icon),
            findViewById<View>(R.id.flash_light_icon_main_activity),
            findViewById<View>(R.id.flash_light_on_icon_main_activity),
            findViewById<View>(R.id.options_icon_main_activity),
            findViewById<View>(R.id.settings_icon_main_activity),
            // Options dropdown icons (rotate icons only to keep text clean and prevent overlaps)
            findViewById<View>(R.id.timer_off_icon),
            findViewById<View>(R.id.focus_manual_icon),
            findViewById<View>(R.id.sound_off_icon),
            findViewById<View>(R.id.white_balance_icon),
            // Bottom deck controls
            findViewById<View>(R.id.camera_change_icon_main_activity),
            findViewById<View>(R.id.collection_icon),
            findViewById<View>(R.id.template_tv),
            // Filter scroll view cross & effect icons
            findViewById<View>(R.id.horizontal_scroll_view_main_cross_icon),
            findViewById<View>(R.id.auto_effect_icon),
            findViewById<View>(R.id.manual_effect_icon),
            findViewById<View>(R.id.incandescent_effect_icon),
            findViewById<View>(R.id.flourescent_effect_icon),
            findViewById<View>(R.id.warm_effect_icon),
            findViewById<View>(R.id.day_light_effect_icon),
            findViewById<View>(R.id.cloudy_effect_icon),
            findViewById<View>(R.id.day_light_effect_icon_2),
            findViewById<View>(R.id.shade_effect_icon)
        )

        val dropdownTexts = listOfNotNull(
            findViewById<View>(R.id.timer_off_tv),
            findViewById<View>(R.id.focus_manual_tv),
            findViewById<View>(R.id.focus_auto_tv),
            findViewById<View>(R.id.sound_off_tv),
            findViewById<View>(R.id.white_balance_tv)
        )
        for (tv in dropdownTexts) {
            tv.rotation = 0f
        }

        for (view in viewsToRotate) {
            animateIconToRotation(view, targetAngle)
        }
    }

    private fun animateIconToRotation(view: View, targetAngle: Float) {
        var currentRot = view.rotation % 360f
        if (currentRot < 0f) currentRot += 360f

        var diff = (targetAngle - currentRot) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        val endRotation = view.rotation + diff
        view.animate()
            .rotation(endRotation)
            .setDuration(280)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configure edge-to-edge transparent system bars (status bar & navigation bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        // Ensure top control bar and bottom shutter deck accommodate status bar & navigation bar insets
        val rootView = findViewById<View>(R.id.root_layout_activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val navBarHeight = navBarInsets.bottom
            val navBarLeft = navBarInsets.left
            val navBarRight = navBarInsets.right

            // Top control bar padding
            val topControlsBar = findViewById<View>(R.id.linear_layout_main_activity)
            val padSide = (16 * resources.displayMetrics.density).toInt()
            val padTop = statusBarHeight + (8 * resources.displayMetrics.density).toInt()
            val padBottom = (8 * resources.displayMetrics.density).toInt()
            topControlsBar?.setPadding(padSide, padTop, padSide, padBottom)

            // Bottom shutter deck sizing & padding (handles 3-button nav and gesture navigation seamlessly)
            val cardViewMain = findViewById<View>(R.id.card_view_main_activity)
            val baseDeckHeight = (115 * resources.displayMetrics.density).toInt()
            val deckParams = cardViewMain?.layoutParams
            if (deckParams != null && deckParams.height != (baseDeckHeight + navBarHeight)) {
                deckParams.height = baseDeckHeight + navBarHeight
                cardViewMain.layoutParams = deckParams
            }
            val shutterControls = findViewById<View>(R.id.bottom_shutter_controls)
            shutterControls?.setPadding(navBarLeft, 0, navBarRight, navBarHeight)

            // Also accommodate navigation bar in effects scroll view
            val effectsScrollView = findViewById<View>(R.id.horizontal_scroll_view_main)
            val baseEffectsHeight = (80 * resources.displayMetrics.density).toInt()
            val effectsParams = effectsScrollView?.layoutParams
            if (effectsParams != null && effectsParams.height != (baseEffectsHeight + navBarHeight)) {
                effectsParams.height = baseEffectsHeight + navBarHeight
                effectsScrollView.layoutParams = effectsParams
            }
            effectsScrollView?.setPadding(navBarLeft, 0, navBarRight, navBarHeight)

            insets
        }
        ViewCompat.requestApplyInsets(rootView)

        val templatePrefs = getSharedPreferences("TemplatePreferences", Context.MODE_PRIVATE)
        selectedTemplate = StampTemplate.fromId(templatePrefs.getInt("selected_template_id", 1))

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        templateIcon = findViewById(R.id.template_icon)
        val templateTv = findViewById<TextView>(R.id.template_tv)
        templateTv?.text = selectedTemplate.title

        val openTemplate = View.OnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }
        templateIcon.setOnClickListener(openTemplate)
        findViewById<View>(R.id.template_button)?.setOnClickListener(openTemplate)
        templateTv?.setOnClickListener(openTemplate)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))


        previewView = findViewById(R.id.previewView)
        flashLightIcon = findViewById(R.id.flash_light_icon_main_activity)
        cameraIcon = findViewById(R.id.camera_change_icon_main_activity)
        optionsIcon = findViewById(R.id.options_icon_main_activity)
        btnQuickOptions = findViewById(R.id.btn_quick_options)
        topRatioTv = findViewById(R.id.top_ratio_tv)
        countdownTextView = findViewById(R.id.countdown_text_view)
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
        cardViewGoogleMap.setCardBackgroundColor(Color.parseColor("#59000000"))
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
        locationDetailsCard.setCardBackgroundColor(Color.TRANSPARENT)
        settingsButton = findViewById<View>(R.id.settings_button_main)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        findViewById<View>(R.id.map_data_button)?.setOnClickListener(openMapData)
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
        findViewById<View>(R.id.folders_button)?.setOnClickListener(openFolders)
        folderTv.setOnClickListener(openFolders)

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

        findViewById<View>(R.id.vertical_info_container)?.let {
            setupDraggableHUD(it)
            it.setOnClickListener {
                if (!isLocationServiceEnabled()) {
                    promptEnableLocation()
                } else {
                    startLocationUpdates()
                    Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.minimal_info_container)?.let {
            setupDraggableHUD(it)
            it.setOnClickListener {
                if (!isLocationServiceEnabled()) {
                    promptEnableLocation()
                } else {
                    startLocationUpdates()
                    Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.satellite_details_card_view)?.setOnClickListener {
            if (!isLocationServiceEnabled()) {
                promptEnableLocation()
            } else {
                startLocationUpdates()
                Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.compass_overlay_container)?.setOnClickListener {
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
        findViewById<View>(R.id.gallery_preview_card)?.setOnClickListener(openCollection)
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
                setupQuickOptionsAndRatioPanel()
        restoreAspectRatioState()
        restoreTimerState()


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
        applyTemplateSelection(selectedTemplate, showToast = false)
        setupOrientationEventListener()
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

                        val targetRatio = when (selectedAspectRatio) {
                AspectRatioMode.RATIO_4_3 -> AspectRatio.RATIO_4_3
                else -> AspectRatio.RATIO_16_9
            }

            val preview = Preview.Builder()
                .setTargetAspectRatio(targetRatio)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(targetRatio)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select camera based on active selection
            val cameraSelector = if (isBackCameraSelected) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                cameraIcon.setImageResource(if (isBackCameraSelected) R.drawable.camera_change_icon_main_activity else R.drawable.front_cam_icon)

                flashLightIcon.setOnClickListener {
                    toggleFlashlight()
                }

                cameraIcon.setOnClickListener {
                    val newCameraSelector = if (isBackCameraSelected) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider.unbindAll()

                        camera = cameraProvider.bindToLifecycle(
                            this,
                            newCameraSelector,
                            preview,
                            imageCapture
                        )

                        isBackCameraSelected = !isBackCameraSelected // Toggle the camera selection flag

                        if (!isBackCameraSelected) {
                            flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
                        }

                        cameraIcon.animate().rotationBy(180f).setDuration(300).start()
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


    private fun triggerShutterFlash() {
        previewView.foreground = ColorDrawable(Color.argb(160, 255, 255, 255))
        previewView.postDelayed({
            previewView.foreground = null
        }, 60)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun executeImageCapture() {
        imageCaptureIcon.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction {
            imageCaptureIcon.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        }.start()

                when (selectedTimer) {
            CameraTimerMode.TIMER_3S -> {
                threeToOneAnimationView.visibility = View.VISIBLE
                threeToOneAnimationView.playAnimation()
                threeToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun onAnimationEnd(animation: Animator) {
                        threeToOneAnimationView.clearAnimation()
                        threeToOneAnimationView.visibility = View.INVISIBLE
                        if (isSoundOn) {
                            playCameraSound()
                        }
                        triggerShutterFlash()
                        captureImage()
                    }
                })
            }

            CameraTimerMode.TIMER_5S -> {
                fiveToOneAnimationView.visibility = View.VISIBLE
                fiveToOneAnimationView.playAnimation()
                fiveToOneAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun onAnimationEnd(animation: Animator) {
                        fiveToOneAnimationView.clearAnimation()
                        fiveToOneAnimationView.visibility = View.INVISIBLE
                        if (isSoundOn) {
                            playCameraSound()
                        }
                        triggerShutterFlash()
                        captureImage()
                    }
                })
            }

            CameraTimerMode.TIMER_10S -> {
                runTenSecondCountdown {
                    if (isSoundOn) {
                        playCameraSound()
                    }
                    triggerShutterFlash()
                    captureImage()
                }
            }

            CameraTimerMode.OFF -> {
                if (isSoundOn) {
                    playCameraSound()
                }
                triggerShutterFlash()
                captureImage()
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
            gpsStatusTv?.text = "Acquiring GPS..."
            startLocationUpdates()
        } else {
            gpsStatusTv?.text = "Location Off"
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
                gpsStatusTv?.text = "Acquiring GPS..."
                Toast.makeText(this, "Location enabled. Acquiring GPS...", Toast.LENGTH_SHORT).show()
                startLocationUpdates()
            } else {
                gpsStatusTv?.text = "Location Off"
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
            gpsStatusTv?.text = "GPS Active ±${location.accuracy.toInt()}m"
        } else {
            accuracyTextView.visibility = View.GONE
            accuracyIcon.visibility = View.GONE
            gpsStatusTv?.text = "GPS Active"
        }

        // Hide unused placeholders so they never display "Lat/Long" dummy text
        findViewById<TextView>(R.id.plus_code_tv_main_location_details_card).visibility = View.GONE
        findViewById<TextView>(R.id.weather_tv_main_template_card).visibility = View.GONE
        cloudyIcon.visibility = View.GONE
        findViewById<TextView>(R.id.wind_tv_main_template).visibility = View.GONE
        windIcon.visibility = View.GONE
        findViewById<TextView>(R.id.pressure_tv_main_template).visibility = View.GONE
        pressureIcon.visibility = View.GONE
        updateActiveTemplateViews()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            googleMap.setOnMapLoadedCallback {
                try {
                    googleMap.snapshot { bmp ->
                        if (bmp != null) {
                            latestMapBitmap = bmp
                        }
                    }
                } catch (_: Exception) {}
            }
            googleMap.setOnCameraIdleListener {
                try {
                    googleMap.snapshot { bmp ->
                        if (bmp != null) {
                            latestMapBitmap = bmp
                        }
                    }
                } catch (_: Exception) {}
            }
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
        if (!isBackCameraSelected) {
            Toast.makeText(this, "Flashlight unavailable for front camera", Toast.LENGTH_SHORT).show()
            return
        }
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            val cameraControl = cam.cameraControl
            val torchState = cam.cameraInfo.torchState.value
            val isTorchOn = torchState == TorchState.ON
            cameraControl.enableTorch(!isTorchOn)
            flashLightIcon.setImageResource(
                if (isTorchOn) R.drawable.flash_light_icon_main_activity
                else R.drawable.flash_on_icon
            )
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
        // Reload selected template from TemplatePreferences in real time
        val templatePrefs = getSharedPreferences("TemplatePreferences", Context.MODE_PRIVATE)
        val templateId = templatePrefs.getInt("selected_template_id", 1)
        applyTemplateSelection(StampTemplate.fromId(templateId), showToast = false)

        // Apply Keep Screen On setting
        val cameraSettings = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        val keepAwake = cameraSettings.getBoolean("keep_screen_on", true)
        if (keepAwake) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Sync aspect ratio & timer from settings
        val savedRatio = AspectRatioMode.fromLabel(cameraSettings.getString("camera_aspect_ratio", "16:9"))
        if (savedRatio != selectedAspectRatio) {
            applyAspectRatio(savedRatio, restartCamera = true)
        }
        val savedTimer = CameraTimerMode.fromLabel(cameraSettings.getString("camera_timer_mode", "Off"))
        if (savedTimer != selectedTimer) {
            applyTimerMode(savedTimer)
        }

        // Retrieve the selected map type from SharedPreferences
        val sharedPreferences = getSharedPreferences("MapPreferences", Context.MODE_PRIVATE)
        val selectedMapType = sharedPreferences.getInt("SelectedMapType", GoogleMap.MAP_TYPE_NORMAL)
        updateMapTypeFromTemplate(selectedMapType)
        cameraIcon.setImageResource(if (isBackCameraSelected) R.drawable.camera_change_icon_main_activity else R.drawable.front_cam_icon)
        flashLightIcon.setImageResource(R.drawable.flash_light_icon_main_activity)
        restoreTimerState()
        restoreSoundState()
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

        rotationSensor?.let {
            sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        orientationEventListener?.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener?.disable()
        stopLocationUpdates()
        sensorManager?.unregisterListener(sensorEventListener)
    }

        override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val cardView = findViewById<View>(R.id.card_view_options_icon)
            if (isOptionsIconSelected && cardView != null && cardView.visibility == View.VISIBLE) {
                val cardRect = android.graphics.Rect()
                cardView.getGlobalVisibleRect(cardRect)
                val btnRect = android.graphics.Rect()
                btnQuickOptions.getGlobalVisibleRect(btnRect)
                val x = ev.rawX.toInt()
                val y = ev.rawY.toInt()

                if (!cardRect.contains(x, y) && !btnRect.contains(x, y)) {
                    closeQuickOptionsCard()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
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
        activeCountDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        cameraExecutor.shutdown()
        sensorManager?.unregisterListener(sensorEventListener)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        // 1. Determine destination folder based on user's selected folder in FoldersActivity
        val folderPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val targetDir = if (folderPrefs.getBoolean("isDefaultFolderSelected", true)) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        } else if (folderPrefs.getBoolean("isSiteOneFolderSelected", false)) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Map Camera App/Site 1")
        } else if (folderPrefs.getBoolean("isSiteTwoFolderSelected", false)) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPS Map Camera App/Site 2")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val photoFile = File(
            targetDir,
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )

        // Save raw capture into private cache directory so Android MediaStore never sees or indexes unstamped photos
        val tempRawFile = File(cacheDir, "raw_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempRawFile).build()

        // 2. Snapshot telemetry immediately on main thread at the exact moment of shutter press
        val rawAddress = cityCountryTv.text.toString().trim()
        val (shortAddress, _) = extractShortAndFullAddress(rawAddress)
        val fullAddress = rawAddress
        val latLong = longLatTv.text.toString().trim()
        val altText = findViewById<TextView>(R.id.altitude_tv_main_template)?.text?.toString()?.ifEmpty { "14.7 m" } ?: "14.7 m"
        val accText = findViewById<TextView>(R.id.accuracy_tv_template_main)?.text?.toString()?.ifEmpty { "±32.0 m" } ?: "±32.0 m"
        val dateText = findViewById<TextView>(R.id.date_day_time_tv)?.text?.toString()?.trim()?.ifEmpty {
            val curD = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
            val curDay = SimpleDateFormat("EEEE", Locale.US).format(Date())
            val curT = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
            "$curD, $curDay, $curT"
        } ?: SimpleDateFormat("dd MMM yyyy, EEEE", Locale.US).format(Date())
        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        val timezoneText = findViewById<TextView>(R.id.time_zone_tv_main_template)?.text?.toString()?.ifEmpty { "GMT +05:30" } ?: "GMT +05:30"
        val isBack = isBackCameraSelected
        val activeTemplate = selectedTemplate

        // Compute relative position of active HUD on screen for 1:1 WYSIWYG printing
        val activeHudView = when (activeTemplate) {
            StampTemplate.VERTICAL_INFO -> findViewById<View>(R.id.vertical_info_container)
            StampTemplate.MINIMAL -> findViewById<View>(R.id.minimal_info_container)
            StampTemplate.MAP_INFO -> findViewById<View>(R.id.satellite_details_card_view)
            StampTemplate.CLEAN_OVERLAY -> findViewById<View>(R.id.compass_overlay_container)
            StampTemplate.MODERN_CARD, StampTemplate.CUSTOM -> findViewById<View>(R.id.location_details_card_view)
        }

                val preview = previewView
        val relStampX: Float?
        val relStampY: Float?

        if (activeHudView != null && preview.width > 0 && preview.height > 0 && activeHudView.visibility == View.VISIBLE) {
            val hudLoc = IntArray(2)
            val prevLoc = IntArray(2)
            activeHudView.getLocationOnScreen(hudLoc)
            preview.getLocationOnScreen(prevLoc)
            val rx = (hudLoc[0] - prevLoc[0]).toFloat() / preview.width.toFloat()
            val ry = (hudLoc[1] - prevLoc[1]).toFloat() / preview.height.toFloat()
            relStampX = rx.coerceIn(0f, 1f)
            relStampY = ry.coerceIn(0f, 1f)
        } else {
            relStampX = null
            relStampY = null
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        val requiresMap = (activeTemplate == StampTemplate.MODERN_CARD || activeTemplate == StampTemplate.MAP_INFO)

        // Snapshot map immediately on the main thread right when the shutter is pressed
        if (requiresMap && mapFragment != null) {
            try {
                mapFragment.getMapAsync { googleMap ->
                    googleMap.snapshot { bmp ->
                        if (bmp != null) {
                            latestMapBitmap = bmp
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Trigger capture with background executor: UI never blocks, preview never freezes, zero loading lag
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val rawBitmap = BitmapFactory.decodeFile(tempRawFile.absolutePath)
                    try {
                        tempRawFile.delete()
                    } catch (_: Exception) {}

                    if (rawBitmap == null) {
                        Log.e(TAG, "Capture failed to decode bitmap from cache: ${tempRawFile.absolutePath}")
                        return
                    }

                                        // For front camera (selfie), mirror horizontally so photo matches preview
                    val orientedBitmap = if (!isBack) {
                        val matrix = Matrix().apply { preScale(-1f, 1f) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }

                    // Crop / frame according to selected aspect ratio
                    val capturedBitmap = when (selectedAspectRatio) {
                        AspectRatioMode.RATIO_1_1 -> {
                            val side = minOf(orientedBitmap.width, orientedBitmap.height)
                            val cropX = (orientedBitmap.width - side) / 2
                            val cropY = (orientedBitmap.height - side) / 2
                            Bitmap.createBitmap(orientedBitmap, cropX, cropY, side, side)
                        }
                        AspectRatioMode.RATIO_4_3 -> {
                            val targetWidth = (orientedBitmap.height * 3) / 4
                            if (targetWidth <= orientedBitmap.width) {
                                val cropX = (orientedBitmap.width - targetWidth) / 2
                                Bitmap.createBitmap(orientedBitmap, cropX, 0, targetWidth, orientedBitmap.height)
                            } else {
                                val targetHeight = (orientedBitmap.width * 4) / 3
                                val cropY = (orientedBitmap.height - targetHeight) / 2
                                Bitmap.createBitmap(orientedBitmap, 0, cropY, orientedBitmap.width, targetHeight)
                            }
                        }
                        AspectRatioMode.RATIO_16_9 -> {
                            val targetWidth = (orientedBitmap.height * 9) / 16
                            if (targetWidth <= orientedBitmap.width) {
                                val cropX = (orientedBitmap.width - targetWidth) / 2
                                Bitmap.createBitmap(orientedBitmap, cropX, 0, targetWidth, orientedBitmap.height)
                            } else {
                                val targetHeight = (orientedBitmap.width * 16) / 9
                                val cropY = (orientedBitmap.height - targetHeight) / 2
                                Bitmap.createBitmap(orientedBitmap, 0, cropY, orientedBitmap.width, targetHeight)
                            }
                        }
                        AspectRatioMode.RATIO_FULL -> {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val screenHeight = resources.displayMetrics.heightPixels
                            val screenRatio = screenHeight.toFloat() / screenWidth.toFloat()
                            val imageRatio = orientedBitmap.height.toFloat() / orientedBitmap.width.toFloat()
                            if (screenRatio > imageRatio) {
                                val targetWidth = (orientedBitmap.height / screenRatio).toInt()
                                val cropX = ((orientedBitmap.width - targetWidth) / 2).coerceAtLeast(0)
                                if (targetWidth > 0 && cropX + targetWidth <= orientedBitmap.width) {
                                    Bitmap.createBitmap(orientedBitmap, cropX, 0, targetWidth, orientedBitmap.height)
                                } else {
                                    orientedBitmap
                                }
                            } else {
                                orientedBitmap
                            }
                        }
                    }

                    fun finalizeAndSave(mapBitmap: Bitmap?) {
                        try {
                            val stampData = StampData(
                                shortAddress = shortAddress,
                                fullAddress = fullAddress,
                                latitude = 0.0,
                                longitude = 0.0,
                                latLongText = latLong,
                                altitudeText = altText,
                                accuracyText = accText,
                                dateText = dateText,
                                timeText = timeText,
                                timezoneText = timezoneText,
                                mapBitmap = mapBitmap,
                                compassAzimuth = currentCompassAzimuth,
                                relativeStampX = relStampX,
                                relativeStampY = relStampY
                            )

                            val finalBitmap = StampRenderer.applyStamp(
                                context = this@MainActivity,
                                sourceBitmap = capturedBitmap,
                                template = activeTemplate,
                                data = stampData
                            )

                            saveCombinedBitmap(finalBitmap, photoFile)

                            runOnUiThread {
                                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                mediaScanIntent.data = Uri.fromFile(photoFile)
                                sendBroadcast(mediaScanIntent)
                                updateCollectionThumbnail()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error rendering stamp: ${e.message}", e)
                            saveCombinedBitmap(capturedBitmap, photoFile)
                            runOnUiThread {
                                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                mediaScanIntent.data = Uri.fromFile(photoFile)
                                sendBroadcast(mediaScanIntent)
                                updateCollectionThumbnail()
                            }
                        }
                    }

                    if (requiresMap && mapFragment != null) {
                        runOnUiThread {
                            try {
                                mapFragment.getMapAsync { googleMap ->
                                    googleMap.snapshot { mapBitmap ->
                                        val mapToUse = mapBitmap ?: latestMapBitmap
                                        cameraExecutor.execute {
                                            finalizeAndSave(mapToUse)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                cameraExecutor.execute {
                                    finalizeAndSave(latestMapBitmap)
                                }
                            }
                        }
                    } else {
                        finalizeAndSave(null)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.flush()
        outputStream.close()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableHUD(view: View?) {
        if (view == null) return
        var dX = 0f
        var dY = 0f
        var downRawX = 0f
        var downRawY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

        view.setOnTouchListener { v, event ->
            val root = findViewById<View>(R.id.root_layout_activity_main) ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    isDragging = false
                    v.bringToFront()
                    (v.parent as? View)?.invalidate()
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dist = Math.hypot((event.rawX - downRawX).toDouble(), (event.rawY - downRawY).toDouble())
                    if (dist > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val maxX = (root.width - v.width).toFloat().coerceAtLeast(0f)
                        val maxY = (root.height - v.height).toFloat().coerceAtLeast(0f)
                        v.x = (event.rawX + dX).coerceIn(0f, maxX)
                        v.y = (event.rawY + dY).coerceIn(0f, maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    if (!isDragging) {
                        v.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateVerticalCardViews() {
        val verticalCard = findViewById<View>(R.id.vertical_info_container) ?: return

        val rawAddress = cityCountryTv.text.toString().trim()
        val (shortAddr, fullAddr) = extractShortAndFullAddress(rawAddress)

        verticalCard.findViewById<TextView>(R.id.vertical_short_address_tv)?.text = shortAddr
        verticalCard.findViewById<TextView>(R.id.vertical_full_address_tv)?.text = fullAddr

        val latLong = findViewById<TextView>(R.id.long_lat_tv)?.text?.toString() ?: ""
        verticalCard.findViewById<TextView>(R.id.vertical_lat_long_tv)?.text = latLong.ifEmpty { "Lat -- Long --" }

        val altText = findViewById<TextView>(R.id.altitude_tv_main_template)?.text?.toString()?.ifEmpty { "14.7 m" } ?: "14.7 m"
        val accText = findViewById<TextView>(R.id.accuracy_tv_template_main)?.text?.toString()?.ifEmpty { "±32.0 m" } ?: "±32.0 m"
        verticalCard.findViewById<TextView>(R.id.vertical_altitude_tv)?.text = "Alt $altText  |  $accText"

        val dateText = SimpleDateFormat("dd MMM yyyy, EEEE", Locale.US).format(Date())
        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        verticalCard.findViewById<TextView>(R.id.vertical_date_tv)?.text = dateText
        verticalCard.findViewById<TextView>(R.id.vertical_time_tv)?.text = timeText
    }

    private fun updateMinimalCardViews() {
        val container = findViewById<View>(R.id.minimal_info_container) ?: return
        val rawAddress = cityCountryTv.text.toString().trim()
        val (shortAddr, fullAddr) = extractShortAndFullAddress(rawAddress)

        container.findViewById<TextView>(R.id.minimal_short_address_tv)?.text = shortAddr
        container.findViewById<TextView>(R.id.minimal_full_address_tv)?.text = fullAddr

        val latLong = findViewById<TextView>(R.id.long_lat_tv)?.text?.toString() ?: ""
        container.findViewById<TextView>(R.id.minimal_lat_long_tv)?.text = latLong.ifEmpty { "Lat -- Long --" }

        val altText = findViewById<TextView>(R.id.altitude_tv_main_template)?.text?.toString()?.ifEmpty { "14.7 m" } ?: "14.7 m"
        container.findViewById<TextView>(R.id.minimal_altitude_tv)?.text = "Alt $altText"

        val dateText = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        val dayText = SimpleDateFormat("EEEE", Locale.US).format(Date())
        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        container.findViewById<TextView>(R.id.minimal_date_tv)?.text = dateText
        container.findViewById<TextView>(R.id.minimal_day_tv)?.text = dayText
        container.findViewById<TextView>(R.id.minimal_time_tv)?.text = timeText
    }

    private fun updateSatelliteCardViews() {
        val container = findViewById<View>(R.id.satellite_details_card_view) ?: return
        val rawAddress = cityCountryTv.text.toString().trim()
        val (shortAddr, fullAddr) = extractShortAndFullAddress(rawAddress)

        container.findViewById<TextView>(R.id.hud_satellite_short_address_tv)?.text = shortAddr
        container.findViewById<TextView>(R.id.hud_satellite_full_address_tv)?.text = fullAddr

        val latLong = findViewById<TextView>(R.id.long_lat_tv)?.text?.toString() ?: ""
        container.findViewById<TextView>(R.id.hud_satellite_lat_long_tv)?.text = latLong.ifEmpty { "Lat -- Long --" }

        val altText = findViewById<TextView>(R.id.altitude_tv_main_template)?.text?.toString()?.ifEmpty { "14.7 m" } ?: "14.7 m"
        val accText = findViewById<TextView>(R.id.accuracy_tv_template_main)?.text?.toString()?.ifEmpty { "±32.0 m" } ?: "±32.0 m"
        container.findViewById<TextView>(R.id.hud_satellite_altitude_tv)?.text = altText
        container.findViewById<TextView>(R.id.hud_satellite_accuracy_tv)?.text = accText

        val dateText = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        container.findViewById<TextView>(R.id.hud_satellite_date_tv)?.text = dateText
        container.findViewById<TextView>(R.id.hud_satellite_time_tv)?.text = timeText
    }

    private fun updateCompassOverlayViews() {
        val container = findViewById<View>(R.id.compass_overlay_container) ?: return
        val rawAddress = cityCountryTv.text.toString().trim()
        val (shortAddr, fullAddr) = extractShortAndFullAddress(rawAddress)

        container.findViewById<TextView>(R.id.compass_short_address_tv)?.text = shortAddr
        container.findViewById<TextView>(R.id.compass_full_address_tv)?.text = fullAddr

        val latLong = findViewById<TextView>(R.id.long_lat_tv)?.text?.toString() ?: ""
        container.findViewById<TextView>(R.id.compass_lat_long_tv)?.text = latLong.ifEmpty { "Lat -- Long --" }

        val altText = findViewById<TextView>(R.id.altitude_tv_main_template)?.text?.toString()?.ifEmpty { "14.7 m" } ?: "14.7 m"
        val accText = findViewById<TextView>(R.id.accuracy_tv_template_main)?.text?.toString()?.ifEmpty { "±32.0 m" } ?: "±32.0 m"
        container.findViewById<TextView>(R.id.compass_altitude_tv)?.text = altText
        container.findViewById<TextView>(R.id.compass_accuracy_tv)?.text = accText

        val dateText = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        val dayText = SimpleDateFormat("EEEE", Locale.US).format(Date())
        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        container.findViewById<TextView>(R.id.compass_date_tv)?.text = dateText
        container.findViewById<TextView>(R.id.compass_day_tv)?.text = dayText
        container.findViewById<TextView>(R.id.compass_time_tv)?.text = timeText

        container.findViewById<ImageView>(R.id.compass_dial_image)?.rotation = -currentCompassAzimuth
    }

    private fun updateActiveTemplateViews() {
        when (selectedTemplate) {
            StampTemplate.MODERN_CARD, StampTemplate.CUSTOM -> {
                // cityCountryTv and longLatTv are updated directly in location_details_card_view
            }
            StampTemplate.MINIMAL -> updateMinimalCardViews()
            StampTemplate.VERTICAL_INFO -> updateVerticalCardViews()
            StampTemplate.MAP_INFO -> updateSatelliteCardViews()
            StampTemplate.CLEAN_OVERLAY -> updateCompassOverlayViews()
        }
    }

    fun applyTemplateSelection(template: StampTemplate, showToast: Boolean = false) {
        selectedTemplate = template
        findViewById<TextView>(R.id.template_tv)?.text = template.title
        val templatePrefs = getSharedPreferences("TemplatePreferences", Context.MODE_PRIVATE)
        templatePrefs.edit().putInt("selected_template_id", template.id).apply()

        val mapCard = findViewById<View>(R.id.map_card_view)
        val locationDetailsCard = findViewById<View>(R.id.location_details_card_view)
        val satelliteDetailsCard = findViewById<View>(R.id.satellite_details_card_view)
        val minimalContainer = findViewById<View>(R.id.minimal_info_container)
        val verticalContainer = findViewById<View>(R.id.vertical_info_container)
        val compassContainer = findViewById<View>(R.id.compass_overlay_container)

        val topBadgeLeft = findViewById<View>(R.id.top_geotag_badge_left)
        val topBadgeRight = findViewById<View>(R.id.top_geotag_badge_right)
        val topTagline = findViewById<View>(R.id.top_script_tagline)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment

        // Reset HUD translations to default layout positions if user explicitly chose template
        if (showToast) {
            verticalContainer?.translationX = 0f
            verticalContainer?.translationY = 0f
            minimalContainer?.translationX = 0f
            minimalContainer?.translationY = 0f
        }

        // Reset all HUD containers first
        topBadgeLeft?.visibility = View.GONE
        topBadgeRight?.visibility = View.GONE
        topTagline?.visibility = View.GONE

        mapCard?.visibility = View.GONE
        locationDetailsCard?.visibility = View.GONE
        satelliteDetailsCard?.visibility = View.GONE
        minimalContainer?.visibility = View.GONE
        verticalContainer?.visibility = View.GONE
        compassContainer?.visibility = View.GONE

        val satelliteBadge = findViewById<View>(R.id.hud_satellite_badge)
        satelliteBadge?.visibility = View.GONE

        when (template) {
            StampTemplate.MODERN_CARD, StampTemplate.CUSTOM -> {
                topBadgeLeft?.visibility = View.VISIBLE
                mapCard?.visibility = View.VISIBLE
                locationDetailsCard?.visibility = View.VISIBLE
                mapFragment?.getMapAsync { googleMap ->
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                }
            }
            StampTemplate.MINIMAL -> {
                topTagline?.visibility = View.VISIBLE
                minimalContainer?.visibility = View.VISIBLE
                setupDraggableHUD(minimalContainer)
                updateMinimalCardViews()
            }
            StampTemplate.VERTICAL_INFO -> {
                verticalContainer?.visibility = View.VISIBLE
                setupDraggableHUD(verticalContainer)
                updateVerticalCardViews()
            }
            StampTemplate.MAP_INFO -> {
                topBadgeRight?.visibility = View.VISIBLE
                mapCard?.visibility = View.VISIBLE
                satelliteBadge?.visibility = View.VISIBLE
                satelliteDetailsCard?.visibility = View.VISIBLE
                mapFragment?.getMapAsync { googleMap ->
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
                updateSatelliteCardViews()
            }
            StampTemplate.CLEAN_OVERLAY -> {
                compassContainer?.visibility = View.VISIBLE
                updateCompassOverlayViews()
            }
        }

        // Respect StampSettings toggles for live viewfinder HUD
        val stampPref = getSharedPreferences("StampSettings", Context.MODE_PRIVATE)
        val showMap = stampPref.getBoolean("stamp_map", true)
        val showAddress = stampPref.getBoolean("stamp_address", true)
        val showTelemetry = stampPref.getBoolean("stamp_telemetry", true)
        val customTagline = stampPref.getString("stamp_tagline", "Simatrix GPS Camera") ?: "Simatrix GPS Camera"

        if (!showMap) {
            mapCard?.visibility = View.GONE
            satelliteBadge?.visibility = View.GONE
        }
        if (!showAddress) {
            cityCountryTv?.visibility = View.GONE
            findViewById<View>(R.id.hud_satellite_short_address_tv)?.visibility = View.GONE
            findViewById<View>(R.id.hud_satellite_full_address_tv)?.visibility = View.GONE
            findViewById<View>(R.id.compass_location_block)?.visibility = View.GONE
        } else {
            cityCountryTv?.visibility = View.VISIBLE
            findViewById<View>(R.id.hud_satellite_short_address_tv)?.visibility = View.VISIBLE
            findViewById<View>(R.id.hud_satellite_full_address_tv)?.visibility = View.VISIBLE
            findViewById<View>(R.id.compass_location_block)?.visibility = View.VISIBLE
        }

        if (!showTelemetry) {
            findViewById<TextView>(R.id.long_lat_tv)?.visibility = View.GONE
            findViewById<View>(R.id.hud_satellite_lat_long_tv)?.visibility = View.GONE
            findViewById<View>(R.id.compass_telemetry_block)?.visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.long_lat_tv)?.visibility = View.VISIBLE
            findViewById<View>(R.id.hud_satellite_lat_long_tv)?.visibility = View.VISIBLE
            findViewById<View>(R.id.compass_telemetry_block)?.visibility = View.VISIBLE
        }

        if (customTagline.isNotBlank()) {
            findViewById<TextView>(R.id.top_script_tagline)?.text = if (customTagline == "Simatrix GPS Camera") "Location Tells the Story" else customTagline
            (findViewById<ViewGroup>(R.id.top_geotag_badge_left)?.getChildAt(1) as? TextView)?.text = customTagline
            (findViewById<ViewGroup>(R.id.top_geotag_badge_right)?.getChildAt(1) as? TextView)?.text = customTagline
        }

        if (showToast) {
            Toast.makeText(this, "Selected: ${template.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTimerState(timerState: String, iconResId: Int) {
        val sharedPref = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("timer_state_key", timerState)
            putInt("timer_icon_key", iconResId)
            apply()
        }
    }

        private fun setupQuickOptionsAndRatioPanel() {
        val toggleOptions = View.OnClickListener {
            toggleQuickOptionsCard()
        }
        btnQuickOptions.setOnClickListener(toggleOptions)
        optionsIcon.setOnClickListener(toggleOptions)

        // Row 1: Aspect Ratio Tabs
        findViewById<View>(R.id.tab_ratio_1_1)?.setOnClickListener {
            applyAspectRatio(AspectRatioMode.RATIO_1_1)
        }
        findViewById<View>(R.id.tab_ratio_4_3)?.setOnClickListener {
            applyAspectRatio(AspectRatioMode.RATIO_4_3)
        }
        findViewById<View>(R.id.tab_ratio_16_9)?.setOnClickListener {
            applyAspectRatio(AspectRatioMode.RATIO_16_9)
        }
        findViewById<View>(R.id.tab_ratio_full)?.setOnClickListener {
            applyAspectRatio(AspectRatioMode.RATIO_FULL)
        }

        // Row 2: Timer Tabs
        findViewById<View>(R.id.tab_timer_off)?.setOnClickListener {
            applyTimerMode(CameraTimerMode.OFF)
        }
        findViewById<View>(R.id.tab_timer_3s)?.setOnClickListener {
            applyTimerMode(CameraTimerMode.TIMER_3S)
        }
        findViewById<View>(R.id.tab_timer_5s)?.setOnClickListener {
            applyTimerMode(CameraTimerMode.TIMER_5S)
        }
        findViewById<View>(R.id.tab_timer_10s)?.setOnClickListener {
            applyTimerMode(CameraTimerMode.TIMER_10S)
        }
    }

    private fun toggleQuickOptionsCard() {
        if (!isOptionsIconSelected) {
            btnQuickOptions.setBackgroundResource(R.drawable.bg_square_glass_active)
            cardView.visibility = View.VISIBLE
            cardView.alpha = 0f
            cardView.translationY = -20f
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
            optionsIcon.animate().rotation(180f).setDuration(200).start()
            isOptionsIconSelected = true
        } else {
            closeQuickOptionsCard()
        }
    }

    private fun closeQuickOptionsCard() {
        if (isOptionsIconSelected) {
            btnQuickOptions.setBackgroundResource(R.drawable.bg_square_glass)
            cardView.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(150)
                .withEndAction {
                    cardView.visibility = View.INVISIBLE
                }
                .start()
            optionsIcon.animate().rotation(0f).setDuration(200).start()
            isOptionsIconSelected = false
        }
    }

    private fun applyAspectRatio(mode: AspectRatioMode, restartCamera: Boolean = true) {
        selectedAspectRatio = mode
        val cameraSettings = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        cameraSettings.edit().putString("camera_aspect_ratio", mode.label).apply()

        topRatioTv.text = mode.label
        updateRatioTabsUI(mode)

        val root = findViewById<ConstraintLayout>(R.id.root_layout_activity_main) ?: return
        TransitionManager.beginDelayedTransition(root)
        val cs = ConstraintSet()
        cs.clone(root)

        when (mode) {
            AspectRatioMode.RATIO_1_1 -> {
                cs.clear(R.id.previewView, ConstraintSet.TOP)
                cs.clear(R.id.previewView, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                cs.connect(R.id.previewView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                cs.connect(R.id.previewView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                cs.setDimensionRatio(R.id.previewView, "1:1")
                cs.constrainWidth(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                cs.constrainHeight(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                anchorHudToPreview(cs, anchorToPreview = true)
            }
            AspectRatioMode.RATIO_4_3 -> {
                cs.clear(R.id.previewView, ConstraintSet.TOP)
                cs.clear(R.id.previewView, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                cs.connect(R.id.previewView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                cs.connect(R.id.previewView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                cs.setDimensionRatio(R.id.previewView, "3:4")
                cs.constrainWidth(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                cs.constrainHeight(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                anchorHudToPreview(cs, anchorToPreview = true)
            }
            AspectRatioMode.RATIO_16_9 -> {
                cs.clear(R.id.previewView, ConstraintSet.TOP)
                cs.clear(R.id.previewView, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                cs.connect(R.id.previewView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                cs.connect(R.id.previewView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                cs.setDimensionRatio(R.id.previewView, "9:16")
                cs.constrainWidth(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                cs.constrainHeight(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                anchorHudToPreview(cs, anchorToPreview = false)
            }
            AspectRatioMode.RATIO_FULL -> {
                cs.clear(R.id.previewView, ConstraintSet.TOP)
                cs.clear(R.id.previewView, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                cs.connect(R.id.previewView, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                cs.connect(R.id.previewView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                cs.connect(R.id.previewView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                cs.setDimensionRatio(R.id.previewView, null)
                cs.constrainWidth(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                cs.constrainHeight(R.id.previewView, ConstraintSet.MATCH_CONSTRAINT)
                anchorHudToPreview(cs, anchorToPreview = false)
            }
        }
        cs.applyTo(root)

        if (restartCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    private fun anchorHudToPreview(cs: ConstraintSet, anchorToPreview: Boolean) {
        val hudIds = intArrayOf(
            R.id.map_card_view,
            R.id.location_details_card_view,
            R.id.minimal_info_container,
            R.id.vertical_info_container,
            R.id.satellite_details_card_view,
            R.id.compass_overlay_container
        )
        val density = resources.displayMetrics.density
        val marginPx = if (anchorToPreview) (8 * density).toInt() else (12 * density).toInt()
        val targetAnchorId = if (anchorToPreview) R.id.previewView else R.id.card_view_main_activity
        val targetAnchorSide = if (anchorToPreview) ConstraintSet.BOTTOM else ConstraintSet.TOP

        for (id in hudIds) {
            if (findViewById<View>(id) != null) {
                cs.clear(id, ConstraintSet.BOTTOM)
                cs.connect(id, ConstraintSet.BOTTOM, targetAnchorId, targetAnchorSide, marginPx)
            }
        }
    }

    private fun updateRatioTabsUI(mode: AspectRatioMode) {
        val tab1 = findViewById<View>(R.id.tab_ratio_1_1) ?: return
        val tab4 = findViewById<View>(R.id.tab_ratio_4_3) ?: return
        val tab16 = findViewById<View>(R.id.tab_ratio_16_9) ?: return
        val tabFull = findViewById<View>(R.id.tab_ratio_full) ?: return

        val tv1 = findViewById<TextView>(R.id.tv_ratio_1_1)
        val ic1 = findViewById<ImageView>(R.id.ic_ratio_1_1)
        val tv4 = findViewById<TextView>(R.id.tv_ratio_4_3)
        val tv16 = findViewById<TextView>(R.id.tv_ratio_16_9)
        val tvFull = findViewById<TextView>(R.id.tv_ratio_full)
        val selectedBg = R.drawable.bg_segmented_selected_square
        val unselectedBg = android.R.color.transparent
        val colorSelected = Color.BLACK
        val colorUnselected = Color.WHITE

        val is1 = mode == AspectRatioMode.RATIO_1_1
        tab1.setBackgroundResource(if (is1) selectedBg else unselectedBg)
        tv1?.setTextColor(if (is1) colorSelected else colorUnselected)
        ic1?.setColorFilter(if (is1) colorSelected else colorUnselected)

        val is4 = mode == AspectRatioMode.RATIO_4_3
        tab4.setBackgroundResource(if (is4) selectedBg else unselectedBg)
        tv4?.setTextColor(if (is4) colorSelected else colorUnselected)

        val is16 = mode == AspectRatioMode.RATIO_16_9
        tab16.setBackgroundResource(if (is16) selectedBg else unselectedBg)
        tv16?.setTextColor(if (is16) colorSelected else colorUnselected)

        val isFull = mode == AspectRatioMode.RATIO_FULL
        tabFull.setBackgroundResource(if (isFull) selectedBg else unselectedBg)
        tvFull?.setTextColor(if (isFull) colorSelected else colorUnselected)
    }

    private fun applyTimerMode(mode: CameraTimerMode) {
        selectedTimer = mode
        val cameraSettings = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        cameraSettings.edit().putString("camera_timer_mode", mode.label).apply()

        // Sync legacy timer_state
        val legacyText = when (mode) {
            CameraTimerMode.TIMER_3S -> "Timer 3sec"
            CameraTimerMode.TIMER_5S -> "Timer 5sec"
            CameraTimerMode.TIMER_10S -> "Timer 10sec"
            CameraTimerMode.OFF -> "Timer Off"
        }
        val sharedPref = getSharedPreferences("timer_state", Context.MODE_PRIVATE)
        sharedPref.edit().putString("timer_state_key", legacyText).apply()
        timerTv.text = legacyText

        updateTimerTabsUI(mode)
    }

    private fun updateTimerTabsUI(mode: CameraTimerMode) {
        val tabOff = findViewById<View>(R.id.tab_timer_off) ?: return
        val tab3s = findViewById<View>(R.id.tab_timer_3s) ?: return
        val tab5s = findViewById<View>(R.id.tab_timer_5s) ?: return
        val tab10s = findViewById<View>(R.id.tab_timer_10s) ?: return

        val tvOff = findViewById<TextView>(R.id.tv_timer_off)
        val icOff = findViewById<ImageView>(R.id.ic_timer_off)
        val tv3s = findViewById<TextView>(R.id.tv_timer_3s)
        val tv5s = findViewById<TextView>(R.id.tv_timer_5s)
        val tv10s = findViewById<TextView>(R.id.tv_timer_10s)

        val selectedBg = R.drawable.bg_segmented_selected
        val unselectedBg = android.R.color.transparent
        val colorSelected = Color.BLACK
        val colorUnselected = Color.WHITE

        val isOff = mode == CameraTimerMode.OFF
        tabOff.setBackgroundResource(if (isOff) selectedBg else unselectedBg)
        tvOff?.setTextColor(if (isOff) colorSelected else colorUnselected)
        icOff?.setColorFilter(if (isOff) colorSelected else colorUnselected)

        val is3s = mode == CameraTimerMode.TIMER_3S
        tab3s.setBackgroundResource(if (is3s) selectedBg else unselectedBg)
        tv3s?.setTextColor(if (is3s) colorSelected else colorUnselected)

        val is5s = mode == CameraTimerMode.TIMER_5S
        tab5s.setBackgroundResource(if (is5s) selectedBg else unselectedBg)
        tv5s?.setTextColor(if (is5s) colorSelected else colorUnselected)

        val is10s = mode == CameraTimerMode.TIMER_10S
        tab10s.setBackgroundResource(if (is10s) selectedBg else unselectedBg)
        tv10s?.setTextColor(if (is10s) colorSelected else colorUnselected)
    }

    private fun restoreAspectRatioState() {
        val cameraSettings = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        val ratioLabel = cameraSettings.getString("camera_aspect_ratio", "16:9")
        val ratioMode = AspectRatioMode.fromLabel(ratioLabel)
        applyAspectRatio(ratioMode, restartCamera = false)
    }

    private fun restoreTimerState() {
        val cameraSettings = getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
        val timerLabel = cameraSettings.getString("camera_timer_mode", "Off")
        val timerMode = CameraTimerMode.fromLabel(timerLabel)
        applyTimerMode(timerMode)
    }

    private fun runTenSecondCountdown(onComplete: () -> Unit) {
        countdownTextView.visibility = View.VISIBLE
        activeCountDownTimer?.cancel()
        activeCountDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = ((millisUntilFinished / 1000) + 1).toInt()
                countdownTextView.text = sec.toString()
                countdownTextView.scaleX = 1.3f
                countdownTextView.scaleY = 1.3f
                countdownTextView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start()
            }

            override fun onFinish() {
                countdownTextView.visibility = View.GONE
                onComplete()
            }
        }.start()
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