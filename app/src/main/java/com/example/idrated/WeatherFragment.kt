package com.example.idrated

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvTemperature: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvLocation: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView

    private val apiKey = "5756d076b5a3f5039968a7e610d3c11c"

    private val handler = Handler(Looper.getMainLooper()) // Handler to update UI on the main thread
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            displayCurrentDateTime() // Update time and date
            handler.postDelayed(this, 1000) // Repeat every second
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvLocation = view.findViewById(R.id.tvLocation)
        weatherIcon = view.findViewById(R.id.ivWeatherIcon)
        tvDate = view.findViewById(R.id.tvDate)
        tvTime = view.findViewById(R.id.tvTime)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Display the current time and date immediately
        displayCurrentDateTime()

        // Start updating the time every second
        handler.post(updateTimeRunnable)

        // Check location permissions
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLocationAndWeather()
        }
    }

    private fun displayCurrentDateTime() {
        val sdfDate = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = sdfDate.format(Date())
        val currentTime = sdfTime.format(Date())

        tvDate.text = "$currentDate"
        tvTime.text = "$currentTime"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndWeather()
        } else {
            Toast.makeText(requireContext(), "Permission denied, cannot fetch weather.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndWeather() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                fetchWeatherData(latitude, longitude)
            } else {
                Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherApi = retrofit.create(WeatherApiService::class.java)

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = weatherApi.getWeather(latitude, longitude, apiKey)
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {
                        val temperature = it.main.temp
                        val description = it.weather.firstOrNull()?.description ?: "No description"
                        val humidity = it.main.humidity
                        val locationName = it.name
                        updateWeatherUI(temperature, description, humidity, locationName)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error fetching weather", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWeatherUI(
        temperature: Double,
        description: String,
        humidity: Int,
        locationName: String
    ) {
        tvTemperature.text = "$temperature°C"
        tvDescription.text = description
        tvHumidity.text = "Humidity: $humidity%"
        tvLocation.text = locationName

        when {
            description.contains("clear", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.sunny)
            description.contains("cloud", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.cloudy)
            description.contains("rain", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.rainy)
            description.contains("storm", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.rainy)
            else -> weatherIcon.setImageResource(R.drawable.cloudy)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable) // Stop updating when the view is destroyed
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}