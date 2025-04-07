package com.example.idrated

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.File

class GoalActivity : AppCompatActivity() {

    private lateinit var waterInput: EditText
    private lateinit var addWaterButton: Button
    private lateinit var goalInput: EditText
    private lateinit var goalButton: Button
    private lateinit var goalConsumed: TextView
    private lateinit var goalDisplay: TextView
    private lateinit var percent: TextView
    private lateinit var progressBar: ProgressBar

    // Local JSON file name
    private val fileName = "IDrated.json"

    // Data structure for storage
    data class WaterData(val amount: Float)  // Change to Float for numeric operations
    data class GoalData(val goal: Float)

    // Combined data class to store both water and goal data
    data class IDratedData(val waterData: WaterData, val goalData: GoalData)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)

        // Initialize views
        waterInput = findViewById(R.id.waterInput)
        addWaterButton = findViewById(R.id.addWaterButton)
        goalInput = findViewById(R.id.GoalInput)
        goalButton = findViewById(R.id.GoalButton)
        goalConsumed = findViewById(R.id.goalConsumed)
        goalDisplay = findViewById(R.id.goalDisplay)
        percent = findViewById(R.id.percent)
        progressBar = findViewById(R.id.progressBar)

        // Load previously saved values
        loadData()

        // Water input logic (add water and update goalConsumed)
        addWaterButton.setOnClickListener {
            val waterInputText = waterInput.text.toString()
            if (waterInputText.isNotEmpty()) {
                val waterAmount = waterInputText.toFloatOrNull()
                if (waterAmount != null) {
                    // Add the water input to the existing goalConsumed
                    val updatedWaterAmount = waterAmount + (goalConsumed.text.toString().toFloatOrNull() ?: 0f)
                    goalConsumed.text = updatedWaterAmount.toString()  // Update UI with new water consumed value

                    // Save the updated water amount to JSON
                    saveData(updatedWaterAmount, null)  // Only update the water data

                    // Calculate and update percent and progress bar
                    updateProgress(updatedWaterAmount)
                }
                waterInput.text.clear() // Clear input after saving
            }
        }

        // Goal input logic (save goal and update goalDisplay)
        goalButton.setOnClickListener {
            val goalInputText = goalInput.text.toString()
            if (goalInputText.isNotEmpty()) {
                val goalAmount = goalInputText.toFloatOrNull()
                if (goalAmount != null) {
                    // Save the goal input
                    saveData(null, goalAmount)
                    goalDisplay.text = goalInputText  // Update UI with goal value

                    // Update progress when goal is set
                    updateProgress(goalConsumed.text.toString().toFloatOrNull() ?: 0f)
                }
                goalInput.text.clear() // Clear input after saving
            }
        }
    }

    // Function to save both water and goal input to a single JSON file in Downloads
    private fun saveData(waterInputText: Float?, goalInputText: Float?) {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filePath, fileName)

        // Get existing data (if any) and update it with new values
        val existingData = if (file.exists()) {
            val json = file.readText()
            Gson().fromJson(json, IDratedData::class.java)
        } else {
            IDratedData(WaterData(0f), GoalData(0f))
        }

        val newWaterData = waterInputText?.let { WaterData(it) } ?: existingData.waterData
        val newGoalData = goalInputText?.let { GoalData(it) } ?: existingData.goalData

        // Create new data with updated values
        val idratedData = IDratedData(newWaterData, newGoalData)

        // Save the updated data to the JSON file
        val json = Gson().toJson(idratedData)
        file.writeText(json)
    }

    // Function to load both water and goal input from the single JSON file in Downloads
    private fun loadData() {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(filePath, fileName)

        if (file.exists()) {
            val json = file.readText()
            val idratedData = Gson().fromJson(json, IDratedData::class.java)
            // Display the fetched data in both goalConsumed and goalDisplay
            goalConsumed.text = idratedData.waterData.amount.toString()
            goalDisplay.text = idratedData.goalData.goal.toString()

            // Update progress when data is loaded
            updateProgress(idratedData.waterData.amount)
        }
    }

    // Function to update percent and progress bar
    private fun updateProgress(waterAmount: Float) {
        val goalAmount = goalDisplay.text.toString().toFloatOrNull() ?: 0f
        if (goalAmount > 0) {
            // Calculate percent
            val percentValue = (waterAmount / goalAmount) * 100
            percent.text = "${percentValue.toInt()}%"  // Update percent TextView

            // Update ProgressBar
            progressBar.progress = percentValue.toInt()
        }
    }
}
