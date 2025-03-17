package com.example.idrated

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
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

        radioYes.setOnClickListener {
            textDisclaimer.visibility = View.VISIBLE
            onboardingActivity.markPageAsInteracted(4) // Assuming page 2 is health condition input
            saveHealthConditionToDatabase(true)
        }

        radioNo.setOnClickListener {
            textDisclaimer.visibility = View.GONE
            onboardingActivity.markPageAsInteracted(4)
            saveHealthConditionToDatabase(false)
        }
    }

    // Function to handle button appearance


    // Save health condition status to Firebase Database
    private fun saveHealthConditionToDatabase(hasCondition: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("healthCondition").setValue(hasCondition)
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
