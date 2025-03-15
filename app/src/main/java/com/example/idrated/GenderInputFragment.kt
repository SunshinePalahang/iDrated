package com.example.idrated

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class GenderInputFragment : Fragment(R.layout.fragment_gender_input) {

    private lateinit var maleButton: Button
    private lateinit var femaleButton: Button
    private lateinit var onboardingActivity: OnboardingActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference to buttons
        maleButton = view.findViewById(R.id.maleButton)
        femaleButton = view.findViewById(R.id.femaleButton)

        // Get reference to the parent activity
        onboardingActivity = activity as OnboardingActivity

        // Set listeners to handle button clicks
        maleButton.setOnClickListener {
            setSelectedButton(maleButton, femaleButton)
            // Mark the page as interacted when a gender is selected
            onboardingActivity.markPageAsInteracted(1) // Page 1 is the gender input page

            // Save the selected gender to the database
            saveGenderToDatabase("Male")
        }

        femaleButton.setOnClickListener {
            setSelectedButton(femaleButton, maleButton)
            // Mark the page as interacted when a gender is selected
            onboardingActivity.markPageAsInteracted(1) // Page 1 is the gender input page

            // Save the selected gender to the database
            saveGenderToDatabase("Female")
        }
    }

    // Function to handle button appearance
    private fun setSelectedButton(selectedButton: Button, otherButton: Button) {
        // Highlight the selected button
        selectedButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        selectedButton.setTextColor(Color.WHITE)
        selectedButton.elevation = 8f

        // Reset the other button
        otherButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        otherButton.setTextColor(Color.WHITE)
        otherButton.elevation = 2f
    }

    // Save the selected gender to the Realtime Database
    private fun saveGenderToDatabase(gender: String?) {
        if (gender != null) {
            // Get the current user's UID from Firebase Auth
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                val genderData = mapOf("gender" to gender)

                // Update the gender in the Realtime Database under the current user's UID
                FirebaseDatabase.getInstance().getReference("users").child(userId).updateChildren(genderData)
                    .addOnSuccessListener {
                        // Gender data saved successfully
                    }
                    .addOnFailureListener { e ->
                        // Handle error
                    }
            } else {
                // User is not authenticated
            }
        }
    }
}
