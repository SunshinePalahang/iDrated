package com.example.idrated

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WaterInputActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_input)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Find views by ID
        val etWaterAmount = findViewById<EditText>(R.id.et_water_amount)
        val btnSubmit = findViewById<Button>(R.id.btn_submit)

        // Set up button click listener
        btnSubmit.setOnClickListener {
            val inputText = etWaterAmount.text.toString().trim()

            if (inputText.isNotEmpty()) {
                val waterAmount = inputText.toDoubleOrNull()

                if (waterAmount != null) {
                    // Save water amount to Realtime Database
                    saveWaterGoalToDatabase(waterAmount)

                    // Pass water amount to GoalActivity
                    val intent = Intent(this, GoalActivity::class.java)
                    intent.putExtra("waterAmount", waterAmount)  // Pass the value to GoalActivity
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a water amount.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to save the water goal to Realtime Database
    private fun saveWaterGoalToDatabase(waterAmount: Double) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = database.getReference("users/$userId/waterGoal")
            userRef.setValue(waterAmount)
                .addOnSuccessListener {
                    // Optional: Show a success message
                    Toast.makeText(this, "Water goal saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Optional: Handle failure
                    Toast.makeText(this, "Failed to save water goal. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in. Please log in and try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
