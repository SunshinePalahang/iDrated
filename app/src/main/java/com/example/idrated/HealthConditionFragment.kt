package com.example.idrated

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HealthConditionFragment : Fragment(R.layout.fragment_health_condition) {

    private lateinit var radioYes: RadioButton
    private lateinit var radioNo: RadioButton
    private lateinit var radioGroup: RadioGroup
    private lateinit var textDisclaimer: TextView
    private lateinit var onboardingActivity: OnboardingActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radioGroup = view.findViewById(R.id.radioGroupHealth)
        radioYes = view.findViewById(R.id.radioYes)
        radioNo = view.findViewById(R.id.radioNo)
        textDisclaimer = view.findViewById(R.id.textDisclaimer)

        // Get reference to the parent activity
        onboardingActivity = activity as OnboardingActivity

        // Hide disclaimer initially
        textDisclaimer.visibility = View.GONE

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioYes) {
                textDisclaimer.visibility = View.VISIBLE
                saveHealthConditionToDatabase(true)
            } else if (checkedId == R.id.radioNo) {
                textDisclaimer.visibility = View.GONE
                saveHealthConditionToDatabase(false)
            }
        }
    }

    // Save health condition status to Firebase Database
    private fun saveHealthConditionToDatabase(hasCondition: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/$uid")

            val waterIntake = if (hasCondition) "2L" else "Personalized"
            userRef.child("healthCondition").setValue(hasCondition)
            userRef.child("recommendedWaterIntake").setValue(waterIntake)
                .addOnSuccessListener {
                    // Successfully saved
                }
                .addOnFailureListener { e ->
                    // Handle error
                }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
}
