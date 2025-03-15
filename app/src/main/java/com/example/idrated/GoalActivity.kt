package com.example.idrated

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.idrated.databinding.ActivityGoalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream
import java.util.*

class GoalActivity : AppCompatActivity() {

    private lateinit var onboardingActivity: OnboardingActivity
    private var age: Int? = null
    private var gender: String? = null
    private var activityLevel: String? = null

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
    private lateinit var binding: ActivityGoalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun getUserDataFromDatabase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        userRef.get().addOnSuccessListener { snapshot ->
            // Get the data from Firebase Realtime Database
            age = snapshot.child("age").getValue(Int::class.java)
            gender = snapshot.child("gender").getValue(String::class.java)
            activityLevel = snapshot.child("activityLevel").getValue(String::class.java)

            // Use a Toast for debugging purposes
            Toast.makeText(this, "Data fetched: Age - $age, Gender - $gender, Activity Level - $activityLevel", Toast.LENGTH_SHORT).show()

            // Once data is fetched, calculate hydration
            calculateHydration()   // Calculate hydration after all data is ready
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateHydration() {
        // Ensure all values are non-null before proceeding
        val age = this.age
        val gender = this.gender
        val activityLevel = this.activityLevel

        if (age != null && gender != null && activityLevel != null) {
            // Constants for base water recommendations (in milliliters)
            val WATER_GOAL_MEN_ADULT = 3700   // 3.7 liters = 3700 mL
            val WATER_GOAL_WOMEN_ADULT = 2700 // 2.7 liters = 2700 mL
            val WATER_GOAL_CHILD_4_8 = 1200   // 1.2 liters = 1200 mL
            val WATER_GOAL_BOYS_9_13 = 2400   // 2.4 liters = 2400 mL
            val WATER_GOAL_BOYS_14_18 = 3300  // 3.3 liters = 3300 mL
            val WATER_GOAL_GIRLS_9_13 = 2100  // 2.1 liters = 2100 mL
            val WATER_GOAL_GIRLS_14_18 = 2300 // 2.3 liters = 2300 mL
            val WATER_GOAL_OLDER_MEN = 3200   // 3.2 liters = 3200 mL
            val WATER_GOAL_OLDER_WOMEN = 2800 // 2.8 liters = 2800 mL

            val LIGHTLY_ACTIVE = 250    // Add 250 mL for lightly active people
            val MODERATELY_ACTIVE = 500 // Add 500 mL for moderately active people
            val VERY_ACTIVE = 750      // Add 750 mL for very active people

            // Step 1: Base water intake calculation based on age and gender
            var recommendedWaterIntake = when {
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

            // Step 2: Adjust for activity level
            recommendedWaterIntake += when (activityLevel) {
                "Sedentary" -> 0   // No addition for sedentary
                "Lightly Active" -> LIGHTLY_ACTIVE
                "Moderately Active" -> MODERATELY_ACTIVE
                "Highly Active" -> VERY_ACTIVE
                else -> 0
            }

            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val realtimeDatabase = FirebaseDatabase.getInstance().reference
                val userRef = realtimeDatabase.child("users").child(userId)

                userRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentWaterGoal = currentData.child("waterGoal").getValue(Double::class.java) ?: 0.0
                        val newWaterGoal = recommendedWaterIntake
                        currentData.child("waterGoal").value = newWaterGoal
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (committed) {
                            Toast.makeText(this@GoalActivity, "Water goal successfully saved to Firebase.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@GoalActivity, "Failed to save water goal: ${error?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            } else {
                Toast.makeText(this, "User is not authenticated", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "Recommended daily water intake: ${recommendedWaterIntake} mL", Toast.LENGTH_LONG).show()

        } else {
            Toast.makeText(this, "Required data not available to calculate hydration.", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if Bluetooth is enabled; prompt if not
    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT).show()
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, bluetoothPermissionRequestCode)
        } else {
            checkAndRequestPermissions()
        }
    }

    // Handle permissions
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), bluetoothPermissionRequestCode)
        } else {
            loadPairedDevices()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == bluetoothPermissionRequestCode) {
            if (bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to proceed.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupUIListeners() {
        binding.btnSetDailyGoal.setOnClickListener {
            val intent = Intent(this, WaterInputActivity::class.java)
            startActivity(intent)
        }

        binding.addWaterButton.setOnClickListener {
            val waterIntake = binding.waterInput.text.toString().toDoubleOrNull()
            if (waterIntake != null && waterIntake > 0) {
                updateWaterConsumed(waterIntake)
                binding.waterInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        binding.LogoutBtn.setOnClickListener {
            auth.signOut()
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
            finish()
        }

        binding.connectButton.setOnClickListener {
            val selectedIndex = binding.deviceDropdown.selectedItemPosition
            if (selectedIndex in pairedDevicesList.indices) {
                connectToDevice(pairedDevicesList[selectedIndex])
            } else {
                Toast.makeText(this, "Please select a valid device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        pairedDevicesList.clear()
        bluetoothAdapter?.bondedDevices?.let { devices ->
            pairedDevicesList.addAll(devices)
            val deviceNames = devices.map { it.name ?: "Unknown Device" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deviceNames)
            binding.deviceDropdown.adapter = adapter
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (isConnected) {
            Toast.makeText(this, "Already connected to a device", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            startReadingData()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Connection failed: ${e.message}")
            closeBluetoothConnection()
            Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            binding.receivedDataTextView.text = "Received: $receivedData"

                            // Convert to Double and add to waterConsumed
                            val waterIntake = receivedData.toDoubleOrNull()
                            if (waterIntake != null && waterIntake > 0) {
                                addToWaterConsumed(waterIntake)
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
                    Toast.makeText(this@GoalActivity, "Failed to retrieve updates", Toast.LENGTH_SHORT).show()
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
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("Firebase", "Failed to update waterConsumed: ${error.message}")
                        Toast.makeText(this@GoalActivity, "Failed to update water intake", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("Firebase", "Successfully updated waterConsumed")
                        Toast.makeText(this@GoalActivity, "Water intake updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun loadWaterGoalAndConsumed() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = realtimeDatabase.child("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val savedGoal = snapshot.child("waterGoal").getValue(Double::class.java) ?: 0.0
                    val consumed = snapshot.child("waterConsumed").getValue(Double::class.java) ?: 0.0
                    updateGoalDisplay(savedGoal, consumed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GoalActivity, "Failed to load goal", Toast.LENGTH_SHORT).show()
                }
            })
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
            (consumed / goal) * 100
        } else {
            0.0
        }
        binding.percent.text = String.format("%.2f%%", percentage)
    }

    private fun updateProgressBar(goal: Double, consumed: Double) {
        binding.progressBar.max = goal.toInt() // Cast to Int for progress bar max
        binding.progressBar.progress = consumed.toInt() // Cast to Int for progress bar progress
    }

    private fun updateWaterConsumed(waterIntake: Double) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = realtimeDatabase.child("users").child(userId)
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentConsumed = currentData.child("waterConsumed").getValue(Double::class.java) ?: 0.0
                val goal = currentData.child("waterGoal").getValue(Double::class.java) ?: 0.0

                // Calculate the new consumed value while ensuring it doesn't exceed the goal
                val newConsumed = (currentConsumed + waterIntake).coerceAtMost(goal)
                currentData.child("waterConsumed").value = newConsumed

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (error != null) {
                    Toast.makeText(this@GoalActivity, "Failed to update water intake: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firebase", "Transaction failed: ${error.message}")
                } else if (committed) {
                    val goal = snapshot?.child("waterGoal")?.getValue(Double::class.java) ?: 0.0
                    val newConsumed = snapshot?.child("waterConsumed")?.getValue(Double::class.java) ?: 0.0
                    updateGoalDisplay(goal, newConsumed)
                    Toast.makeText(this@GoalActivity, "Water intake updated!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    private fun closeBluetoothConnection() {
        inputStream?.close()
        bluetoothSocket?.close()
        isConnected = false
    }

    override fun onDestroy() {
        super.onDestroy()
        closeBluetoothConnection()
    }
}
