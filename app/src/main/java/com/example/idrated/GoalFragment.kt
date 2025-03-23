package com.example.idrated

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.idrated.databinding.FragmentGoalBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.FirebaseDatabase

class GoalFragment : Fragment() {
    // User Data
    private var username: String? = null
    private var age: Int? = null
    private var gender: String? = null
    private var activityLevel: String? = null


    // Weather API
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = "5756d076b5a3f5039968a7e610d3c11c"

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDatabase: DatabaseReference

    // Bluetooth-related variables
    private val bluetoothPermissionRequestCode = 1
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private val pairedDevicesList = mutableListOf<BluetoothDevice>()

    // Binding
    private var _binding: FragmentGoalBinding? = null
    private val binding get() = _binding!!

    private var isHydrationCalculated = false
    private var lastHydrationCalculationDate: String? = null
    //Hydration Goal
    private var recommendedWaterIntake = 2000 // Default to 2L if user has a health condition


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val sharedPrefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Get stored values for hydration calculation
        lastHydrationCalculationDate = sharedPrefs.getString("lastHydrationCalculationDate", null)
        isHydrationCalculated = sharedPrefs.getBoolean("isHydrationCalculated", false)

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (lastHydrationCalculationDate != currentDate) {
            lastHydrationCalculationDate = currentDate
            isHydrationCalculated = false  // Reset flag for the new day

            // Save the new date and reset flag
            with(sharedPrefs.edit()) {
                putString("lastHydrationCalculationDate", currentDate)
                putBoolean("isHydrationCalculated", false)
                apply()
            }

            getLocationAndWeather()
        }
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
        }
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance().reference

        // Permissions & Bluetooth Setup
        checkBluetoothState()

        // Observe changes to waterConsumed
        observeWaterConsumedUpdates()
        setupUIListeners()

        // Fetch the stored data (age, gender, activity level, weather)
        getUserDataFromDatabase()
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

                        // Check if hydration was already calculated today
                        if (!isHydrationCalculated) {
                            calculateHydration(temperature)
                            isHydrationCalculated = true

                            // Save the state in SharedPreferences
                            val sharedPrefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            sharedPrefs.edit().putBoolean("isHydrationCalculated", true).apply()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Error fetching weather", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Offline Mode", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    private fun getUserDataFromDatabase() {
        val sharedPrefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // Load locally stored data first
        username = sharedPrefs.getString("username", null)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        userRef.get().addOnSuccessListener { snapshot ->
            username = snapshot.child("username").getValue(String::class.java) ?: username
            age = snapshot.child("age").getValue(Int::class.java) ?: age
            gender = snapshot.child("gender").getValue(String::class.java) ?: gender
            activityLevel = snapshot.child("activityLevel").getValue(String::class.java) ?: activityLevel

            // Save to SharedPreferences for offline access
            with(sharedPrefs.edit()) {
                putString("username", username)
                apply()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Offline Mode: Using stored data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateHydration(temperature: Double) {
        val age = this.age
        val gender = this.gender
        val activityLevel = this.activityLevel


        if (age != null && gender != null && activityLevel != null) {
            val WATER_GOAL_MEN_ADULT = 3700
            val WATER_GOAL_WOMEN_ADULT = 2700
            val WATER_GOAL_CHILD_4_8 = 1200
            val WATER_GOAL_BOYS_9_13 = 2400
            val WATER_GOAL_BOYS_14_18 = 3300
            val WATER_GOAL_GIRLS_9_13 = 2100
            val WATER_GOAL_GIRLS_14_18 = 2300
            val WATER_GOAL_OLDER_MEN = 3200
            val WATER_GOAL_OLDER_WOMEN = 2800

            val LIGHTLY_ACTIVE = 250
            val MODERATELY_ACTIVE = 500
            val VERY_ACTIVE = 750

            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val realtimeDatabase = FirebaseDatabase.getInstance().reference
                val userRef = realtimeDatabase.child("users").child(userId)

                userRef.child("healthCondition").get().addOnSuccessListener { snapshot ->
                    val hasHealthCondition = snapshot.getValue(Boolean::class.java) ?: false

                    if (!hasHealthCondition) {
                        recommendedWaterIntake = when {
                            age in 1..8 -> WATER_GOAL_CHILD_4_8
                            age in 9..18 -> {
                                when (gender) {
                                    "Male" -> if (age <= 13) WATER_GOAL_BOYS_9_13 else WATER_GOAL_BOYS_14_18
                                    "Female" -> if (age <= 13) WATER_GOAL_GIRLS_9_13 else WATER_GOAL_GIRLS_14_18
                                    else -> 0
                                }
                            }
                            age in 19..64 -> {
                                when (gender) {
                                    "Male" -> WATER_GOAL_MEN_ADULT
                                    "Female" -> WATER_GOAL_WOMEN_ADULT
                                    else -> 0
                                }
                            }
                            age >= 65 -> {
                                when (gender) {
                                    "Male" -> WATER_GOAL_OLDER_MEN
                                    "Female" -> WATER_GOAL_OLDER_WOMEN
                                    else -> 0
                                }
                            }
                            else -> 0
                        }

                        recommendedWaterIntake += when (activityLevel) {
                            "Sedentary" -> 0
                            "Lightly Active" -> LIGHTLY_ACTIVE
                            "Moderately Active" -> MODERATELY_ACTIVE
                            "Highly Active" -> VERY_ACTIVE
                            else -> 0
                        }

                        if (temperature >= 30.0) {
                            recommendedWaterIntake += 500
                        }
                    }
                    userRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            currentData.child("waterGoal").value = recommendedWaterIntake
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            if (!committed) {
                                Toast.makeText(requireContext(), "Failed to save water goal: ${error?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            } else {
                Toast.makeText(requireContext(), "User is not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            activity?.finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(requireContext(), "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT).show()
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                missingPermissions.toTypedArray(),
                1
            )
        } else {
            loadPairedDevices()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == bluetoothPermissionRequestCode) {
            if (bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(requireContext(), "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            } else {
                Toast.makeText(requireContext(), "Bluetooth must be enabled to proceed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUIListeners() {
        binding.connectButton.setOnClickListener {
            val selectedIndex = binding.deviceDropdown.selectedItemPosition
            if (selectedIndex in pairedDevicesList.indices) {
                connectToDevice(pairedDevicesList[selectedIndex])
            } else {
                Toast.makeText(requireContext(), "Please select a valid device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        pairedDevicesList.clear()

        val defaultMessage = "Select a device"
        val deviceNames = mutableListOf(defaultMessage)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, deviceNames)
        binding.deviceDropdown.adapter = adapter
        binding.deviceDropdown.setOnTouchListener { view, _ ->

            view.performClick()

            if (deviceNames.first() == defaultMessage) {
                bluetoothAdapter?.bondedDevices?.takeIf { it.isNotEmpty() }?.let { devices ->
                    pairedDevicesList.addAll(devices)
                    val updatedDeviceNames = devices.map { it.name ?: "Unknown Device" }
                    val updatedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, updatedDeviceNames)
                    binding.deviceDropdown.adapter = updatedAdapter
                }
            }
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (isConnected) {
            Toast.makeText(requireContext(), "Already connected to a device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uuid = device.uuids?.firstOrNull()?.uuid
                ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()

            inputStream = bluetoothSocket?.inputStream
            isConnected = true

            binding.deviceNameTextView.text = "Connected to: ${device.name}"
            Toast.makeText(requireContext(), "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            startReadingData()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Connection failed: ${e.message}")
            closeBluetoothConnection()
            Toast.makeText(requireContext(), "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startReadingData() {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val buffer = ByteArray(1024)
                while (isConnected) {
                    val bytesRead = inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        val receivedData = String(buffer, 0, bytesRead).trim()
                        handler.post {
                            if (binding != null && isAdded && context != null) {
                                binding.receivedDataTextView.text = "Received: $receivedData"
                            } else {
                                Log.e("Bluetooth", "Fragment not attached, skipping UI update.")
                                return@post
                            }

                            val waterIntake = receivedData.toDoubleOrNull()
                            if (waterIntake == null) {
                                Log.e("Bluetooth", "Invalid data received: $receivedData")
                                return@post
                            }

                            if (waterIntake > 0) {
                                addToWaterConsumed(waterIntake)
                                updateWaterConsumed(waterIntake) // Make sure to remove this if it's unnecessary
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error reading data: ${e.message}")
                isConnected = false
            }
        }.start()
    }

    private fun observeWaterConsumedUpdates() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = realtimeDatabase.child("users").child(userId)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentConsumed = snapshot.child("waterConsumed").getValue(Double::class.java) ?: 0.0
                    val currentGoal = snapshot.child("waterGoal").getValue(Double::class.java) ?: 0.0

                    updateGoalDisplay(currentGoal, currentConsumed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to retrieve updates", Toast.LENGTH_SHORT).show()
                    Log.e("Firebase", "Failed to listen for updates: ${error.message}")
                }
            })
        }
    }

    private fun addToWaterConsumed(waterIntake: Double) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = realtimeDatabase.child("users").child(userId)
            userRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentConsumed = currentData.child("waterConsumed").getValue(Double::class.java) ?: 0.0
                    val newConsumed = currentConsumed + waterIntake
                    currentData.child("waterConsumed").value = newConsumed
                    currentData.child("lastSavedTime").value = System.currentTimeMillis()

                    // ✅ Add the intake to the history correctly here
                    saveIndividualIntake(waterIntake)

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("Firebase", "Failed to update waterConsumed: ${error.message}")
                        Toast.makeText(requireContext(), "Failed to update water intake", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("Firebase", "Successfully updated waterConsumed")
                        Toast.makeText(requireContext(), "Water intake updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGoalDisplay(goal: Double, consumed: Double) {
        binding.goalDisplay.text = String.format("%.2f", goal)
        binding.goalConsumed.text = String.format("%.2f", consumed)
        updatePercentage(goal, consumed)
        updateProgressBar(goal, consumed)
    }

    private fun updatePercentage(goal: Double, consumed: Double) {
        val percentage = if (goal > 0) {
            ((consumed / goal) * 100).coerceAtMost(100.0)
        } else {
            0.0
        }
        binding.percent.text = String.format("%.2f%%", percentage)
    }

    private fun updateProgressBar(goal: Double, consumed: Double) {
        binding.progressBar.max = goal.toInt()
        binding.progressBar.progress = consumed.toInt()
    }

    private fun updateWaterConsumed(newConsumed: Double) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = realtimeDatabase.child("users").child(userId)
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                currentData.child("waterGoal").value = recommendedWaterIntake
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to update water goal.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Water goal updated successfully.", Toast.LENGTH_SHORT).show()

                    // Call saveWaterIntake() after setting the goal
                    saveWaterIntake(newConsumed)
                }
            }
        })
    }

    private fun saveIndividualIntake(waterAmount: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")
            val historyRef = userRef.child("history")

            // Timestamp and date formatting
            val timestamp = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

            // Create a history item for the individual intake
            val historyItem = HistoryItem(
                dateTime = formattedDate,
                waterIntake = waterAmount // ✅ Save only the individual amount here
            )

            // Save this intake as a new history entry
            historyRef.child(timestamp.toString()).setValue(historyItem)
                .addOnSuccessListener {
                    Log.d("Firebase", "Water intake recorded successfully in history.")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to save history entry: ${it.message}")
                    Toast.makeText(context, "Failed to save history!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveWaterIntake(waterAmount: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$userId")

            // Save updated waterConsumed and waterGoal if needed
            userRef.child("waterConsumed").setValue(waterAmount)

            // Save to history correctly
            val historyRef = userRef.child("history")

            val timestamp = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

            val historyItem = HistoryItem(
                dateTime = formattedDate,
                waterIntake = waterAmount
            )

            // Push new history entry
            historyRef.child(timestamp.toString()).setValue(historyItem)
                .addOnSuccessListener {
                    Toast.makeText(context, "Water intake recorded successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to save history!", Toast.LENGTH_SHORT).show()
                }

            // Remove lastSavedTime (optional)
            userRef.child("lastSavedTime").removeValue()
        }
    }

    private fun closeBluetoothConnection() {
        try {
            inputStream?.close()
            bluetoothSocket?.close()
            isConnected = false
            binding.deviceNameTextView.text = "Disconnected"
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error closing connection: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        closeBluetoothConnection()
        _binding = null
    }
}
