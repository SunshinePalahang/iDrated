package com.example.idrated

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.util.*

class GoalActivity : AppCompatActivity() {

    // Views
    private lateinit var waterInput: EditText
    private lateinit var addWaterButton: Button
    private lateinit var goalInput: EditText
    private lateinit var goalButton: Button
    private lateinit var connectButton: Button
    private lateinit var goalConsumed: TextView
    private lateinit var goalDisplay: TextView
    private lateinit var percent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var deviceDropdown: Spinner
    private lateinit var deviceNameTextView: TextView

    private val fileName = "IDrated.json"

    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var selectedDevice: BluetoothDevice? = null

    // Data models
    data class WaterData(val amount: Float)
    data class GoalData(val goal: Float)
    data class IDratedData(val waterData: WaterData, val goalData: GoalData)

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_CODE_BT_CONNECT = 2
        private val DEFAULT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        initViews()
        loadData()
        setupBluetooth()

        goalButton.setOnClickListener {
            val goal = goalInput.text.toString().toFloatOrNull()
            if (goal != null) {
                saveData(null, goal)
                goalDisplay.text = goal.toString()
                updateProgress(goalConsumed.text.toString().toFloatOrNull() ?: 0f)
                goalInput.text.clear()
            } else {
                toast("Please enter a valid goal")
            }
        }

        connectButton.setOnClickListener {
            selectedDevice?.let { connectToDevice(it) } ?: toast("No device selected")
        }

        deviceDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedName = parent.getItemAtPosition(position).toString()
                selectedDevice = bluetoothAdapter?.bondedDevices?.firstOrNull { it.name == selectedName }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        findViewById<TextView>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initViews() {
        goalInput = findViewById(R.id.GoalInput)
        goalButton = findViewById(R.id.GoalButton)
        goalConsumed = findViewById(R.id.goalConsumed)
        goalDisplay = findViewById(R.id.goalDisplay)
        percent = findViewById(R.id.percent)
        progressBar = findViewById(R.id.progressBar)
        deviceDropdown = findViewById(R.id.deviceDropdown)
        connectButton = findViewById(R.id.connectButton)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            toast("Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE_BT_CONNECT)
        } else {
            populatePairedDevicesDropdown()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun populatePairedDevicesDropdown() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            toast("Bluetooth connect permission required")
            return
        }

        val devices = bluetoothAdapter?.bondedDevices?.sortedBy { it.name } ?: emptySet()
        val deviceNames = devices.map { it.name }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceDropdown.adapter = adapter
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (isConnected) {
            toast("Already connected")
            return
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            toast("Bluetooth connect permission required")
            return
        }

        Thread {
            try {
                val uuid = device.uuids?.firstOrNull()?.uuid ?: DEFAULT_UUID
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                isConnected = true

                runOnUiThread {
                    toast("Connected to ${device.name}")
                    deviceNameTextView.text = "Connected to: ${device.name}"
                }

                startReadingData()
            } catch (e: Exception) {
                Log.e("Bluetooth", "Connection failed: ${e.message}")
                isConnected = false
                runOnUiThread {
                    toast("Bluetooth connection failed")
                }
            }
        }.start()
    }

    private fun startReadingData() {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val buffer = ByteArray(1024)
                var partialData = ""

                while (isConnected) {
                    val bytesRead = inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        partialData += String(buffer, 0, bytesRead)
                        val lines = partialData.split("\n")
                        partialData = if (partialData.endsWith("\n")) "" else lines.last()

                        for (i in 0 until lines.size - 1) {
                            val value = lines[i].trim().toFloatOrNull()
                            if (value != null && value > 50) {
                                handler.post {
                                    updateWaterIntake(value)
                                    toast("Received $value ml via Bluetooth")
                                }
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

    private fun updateWaterIntake(value: Float) {
        val current = goalConsumed.text.toString().toFloatOrNull() ?: 0f
        val updated = current + value
        goalConsumed.text = updated.toString()
        saveData(updated, null)
        updateProgress(updated)
    }

    private fun saveData(waterAmount: Float?, goalAmount: Float?) {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filePath, fileName)

        val existingData = if (file.exists()) {
            Gson().fromJson(file.readText(), IDratedData::class.java)
        } else {
            IDratedData(WaterData(0f), GoalData(0f))
        }

        val updatedWater = waterAmount?.let { WaterData(it) } ?: existingData.waterData
        val updatedGoal = goalAmount?.let { GoalData(it) } ?: existingData.goalData

        val newData = IDratedData(updatedWater, updatedGoal)
        file.writeText(Gson().toJson(newData))
    }

    private fun loadData() {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filePath, fileName)

        if (file.exists()) {
            val data = Gson().fromJson(file.readText(), IDratedData::class.java)
            goalConsumed.text = data.waterData.amount.toString()
            goalDisplay.text = data.goalData.goal.toString()
            updateProgress(data.waterData.amount)
        }
    }

    private fun updateProgress(waterAmount: Float) {
        val goalAmount = goalDisplay.text.toString().toFloatOrNull() ?: 0f
        if (goalAmount > 0) {
            val percentValue = (waterAmount / goalAmount) * 100
            percent.text = "${percentValue.toInt()}%"
            progressBar.progress = percentValue.toInt()

            if (percentValue >= 100) {
                showSuccessPopup()
            }
        }
    }

    private fun showSuccessPopup() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_achieve_goal, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BT_CONNECT && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            populatePairedDevicesDropdown()
        } else {
            toast("Bluetooth connect permission denied")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error closing socket: ${e.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
