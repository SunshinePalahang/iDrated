package com.example.idrated

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes for parsing the JSON response from OpenWeatherMap API
data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String // Location name
)

data class Main(val temp: Double, val humidity: Int)
data class Weather(val description: String)

interface WeatherApiService {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Temperature in Celsius
    ): Response<WeatherResponse>
}

class WeatherActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = "5756d076b5a3f5039968a7e610d3c11c" // Replace with your OpenWeatherMap API Key

    // UI Elements
    private lateinit var tvTemperature: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvLocation: TextView
    private lateinit var weatherIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        // Initialize views
        tvTemperature = findViewById(R.id.tvTemperature)
        tvDescription = findViewById(R.id.tvDescription)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvLocation = findViewById(R.id.tvLocation)
        weatherIcon = findViewById(R.id.ivWeatherIcon)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocationAndWeather()
        }
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                getLocationAndWeather()
            } else {
                Toast.makeText(this, "Permission denied, cannot fetch weather.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun getLocationAndWeather() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                // Fetch weather data using latitude and longitude
                fetchWeatherData(latitude, longitude)
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherApi = retrofit.create(WeatherApiService::class.java)

        // Make API call using coroutines
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

                        // Update UI with weather data
                        updateWeatherUI(temperature, description, humidity, locationName)
                    }
                } else {
                    Toast.makeText(this@WeatherActivity, "Error fetching weather", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WeatherActivity, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Set weather icon based on description
        when {
            description.contains("clear", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.sunny)
            description.contains("cloud", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.cloudy)
            description.contains("rain", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.rainy)
            description.contains("storm", ignoreCase = true) -> weatherIcon.setImageResource(R.drawable.rainy)
            else -> weatherIcon.setImageResource(R.drawable.cloudy) // Default icon
        }
    }
}
