package com.example.gpsmapcameraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.Priority
import java.util.Locale

class MapDataAutomaticActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var backIcon: ImageView
    private lateinit var latitudeValueTv: TextView
    private lateinit var longitudeValueTv: TextView
    private lateinit var dropDownIcon: ImageView
    private var isDropDownIconDown = true
    private var fromManualActivity = false

    @SuppressLint("SetTextI18n", "RestrictedApi", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_data_automatic)

         fromManualActivity = intent.getBooleanExtra("from_manual_activity", false)

        disableViews()

        backIcon = findViewById(R.id.back_icon_2)
        dropDownIcon = findViewById(R.id.down_icon)
        longitudeValueTv = findViewById(R.id.longitude_value_tv)
        latitudeValueTv = findViewById(R.id.latitude_value_tv)

        dropDownIcon.setOnClickListener {
            if (isDropDownIconDown) {
                dropDownIcon.setImageResource(R.drawable.up_icon)
                val popupMenu = PopupMenu(this, dropDownIcon)
                popupMenu.inflate(R.menu.drop_down_menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_item_1 -> {
                            dropDownIcon.setImageResource(R.drawable.down_icon)
                            Toast.makeText(this, "Automatic Selected", Toast.LENGTH_SHORT).show()
                            true
                        }

                        R.id.menu_item_2 -> {
                            val intent = Intent(this, MapDataManualActivity::class.java)
                            startActivity(intent)
                            finish()
                            Toast.makeText(this, "Manual Selected", Toast.LENGTH_SHORT).show()
                            true
                        }

                        else -> false
                    }
                }
                popupMenu.show()
            } else {
                dropDownIcon.setImageResource(R.drawable.down_icon)
            }
            isDropDownIconDown = !isDropDownIconDown
        }

        backIcon.setOnClickListener {
            navigateBack()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateUiWithLocation(location)
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { freshLocation: Location? ->
                                freshLocation?.let { updateUiWithLocation(it) }
                            }
                    }
                }
            }
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment_data_automatic) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            googleMap.uiSettings.setAllGesturesEnabled(false)
        }
    }

    private fun updateUiWithLocation(location: Location) {
        val address = getAddress(location.latitude, location.longitude)
        val city = address?.locality ?: ""
        val province = address?.adminArea ?: ""
        val country = address?.countryName ?: ""
        val fullAddress = if (address != null && address.maxAddressLineIndex >= 0 && !address.getAddressLine(0).isNullOrBlank()) {
            address.getAddressLine(0)
        } else {
            listOf(city, province, country).filter { it.isNotBlank() }.joinToString(", ")
        }
        findViewById<TextView>(R.id.city_province_country_tv).text = fullAddress
        findViewById<TextView>(R.id.city_tv).text = city
        findViewById<TextView>(R.id.province_country_tv).text = listOf(province, country).filter { it.isNotBlank() }.joinToString(", ")
        latitudeValueTv.text = " ${location.latitude}"
        longitudeValueTv.text = "${location.longitude}"

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment_data_automatic) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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

    private fun disableViews() {
        // Disable all the views
        findViewById<CardView>(R.id.map_card_view_data_automatic).isEnabled = false
        findViewById<TextView>(R.id.gps_coordinates_tv).isEnabled = false
        findViewById<TextView>(R.id.latitude_tv).isEnabled = false
        findViewById<TextView>(R.id.longitude_tv).isEnabled = false
        findViewById<TextView>(R.id.latitude_value_tv).isEnabled = false
        findViewById<TextView>(R.id.longitude_value_tv).isEnabled = false
        findViewById<View>(R.id.latitude_value_view).isEnabled = false
        findViewById<View>(R.id.longitude_value_view).isEnabled = false
        findViewById<TextView>(R.id.location_tv).isEnabled = false
        findViewById<TextView>(R.id.line_1_tv).isEnabled = false
        findViewById<TextView>(R.id.city_province_country_tv).isEnabled = false
        findViewById<View>(R.id.city_province_country_tv_view).isEnabled = false
        findViewById<TextView>(R.id.line_2).isEnabled = false
        findViewById<TextView>(R.id.city_tv).isEnabled = false
        findViewById<View>(R.id.city_tv_view).isEnabled = false
        findViewById<TextView>(R.id.line_3_tv).isEnabled = false
        findViewById<TextView>(R.id.province_country_tv).isEnabled = false
        findViewById<View>(R.id.province_country_tv_view).isEnabled = false

        // Apply fade and blur effect
        applyFadeAndBlur()
    }

    private fun applyFadeAndBlur() {
        // Set alpha to indicate disabled state
        findViewById<CardView>(R.id.map_card_view_data_automatic).alpha = 0.5f
        findViewById<TextView>(R.id.gps_coordinates_tv).alpha = 0.5f
        findViewById<TextView>(R.id.latitude_tv).alpha = 0.5f
        findViewById<TextView>(R.id.longitude_tv).alpha = 0.5f
        findViewById<TextView>(R.id.latitude_value_tv).alpha = 0.5f
        findViewById<TextView>(R.id.longitude_value_tv).alpha = 0.5f
        findViewById<View>(R.id.latitude_value_view).alpha = 0.5f
        findViewById<View>(R.id.longitude_value_view).alpha = 0.5f
        findViewById<TextView>(R.id.location_tv).alpha = 0.5f
        findViewById<TextView>(R.id.line_1_tv).alpha = 0.5f
        findViewById<TextView>(R.id.city_province_country_tv).alpha = 0.5f
        findViewById<View>(R.id.city_province_country_tv_view).alpha = 0.5f
        findViewById<TextView>(R.id.line_2).alpha = 0.5f
        findViewById<TextView>(R.id.city_tv).alpha = 0.5f
        findViewById<View>(R.id.city_tv_view).alpha = 0.5f
        findViewById<TextView>(R.id.line_3_tv).alpha = 0.5f
        findViewById<TextView>(R.id.province_country_tv).alpha = 0.5f
        findViewById<View>(R.id.province_country_tv_view).alpha = 0.5f
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        navigateBack()
    }

    private fun navigateBack() {
        val intent = Intent(this, MainActivity::class.java)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("The data will be changed according to the current location. Are you sure you want to continue?")
            .setPositiveButton("OK") { dialog, which ->
                if (fromManualActivity) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // Request permissions if needed
                        return@setPositiveButton
                    }
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            val address = getAddress(location.latitude, location.longitude)
                            val city = address?.locality ?: ""
                            val province = address?.adminArea ?: ""
                            val country = address?.countryName ?: ""
                            val locationDetails = if (address != null && address.maxAddressLineIndex >= 0 && !address.getAddressLine(0).isNullOrBlank()) {
                                address.getAddressLine(0)
                            } else {
                                listOf(city, province, country).filter { it.isNotBlank() }.joinToString(", ")
                            }

                            intent.putExtra("location_details", locationDetails)
                            intent.putExtra("latitude", location.latitude)
                            intent.putExtra("longitude", location.longitude)
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()

        alertDialog.show()
    }

}




