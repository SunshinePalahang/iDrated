package com.example.idrated

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UrineColorAnalysisFragment : Fragment(R.layout.fragment_urine_color_analysis) {

    private lateinit var veryLightButton: Button
    private lateinit var lightButton: Button
    private lateinit var mediumButton: Button
    private lateinit var urineImageView: ImageView
    private lateinit var hydrationStatusTextView: TextView
    private lateinit var hydrationDescriptionTextView: TextView
    private lateinit var onboardingActivity: OnboardingActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        veryLightButton = view.findViewById(R.id.veryLightButton)
        lightButton = view.findViewById(R.id.lightButton)
        mediumButton = view.findViewById(R.id.mediumButton)
        urineImageView = view.findViewById(R.id.urineImageView)
        hydrationStatusTextView = view.findViewById(R.id.hydrationStatusTextView)
        hydrationDescriptionTextView = view.findViewById(R.id.hydrationDescriptionTextView)

        // Ensuring onboardingActivity is properly initialized
        onboardingActivity = activity as OnboardingActivity

        veryLightButton.setOnClickListener {
            updateSelection(
                veryLightButton,
                "Well-hydrated",
                "Your hydration level is excellent! Keep it up!",
                R.drawable.very_light_urine
            )
            saveUrineCheckToDatabase("Well-hydrated")
            onboardingActivity.markPageAsInteracted(5)
        }

        lightButton.setOnClickListener {
            updateSelection(
                lightButton,
                "Slightly Dehydrated",
                "You're doing okay, but consider drinking more water.",
                R.drawable.light_urine
            )
            saveUrineCheckToDatabase("Slightly Dehydrated")
            onboardingActivity.markPageAsInteracted(5)
        }

        mediumButton.setOnClickListener {
            updateSelection(
                mediumButton,
                "Moderately Dehydrated",
                "You need to drink more water soon.",
                R.drawable.medium_urine
            )
            saveUrineCheckToDatabase("Moderately Dehydrated")
            onboardingActivity.markPageAsInteracted(5)
        }
    }

    private fun updateSelection(
        selectedButton: Button,
        statusText: String,
        descriptionText: String,
        imageResId: Int
    ) {
        hydrationStatusTextView.text = statusText
        hydrationDescriptionTextView.text = descriptionText
        urineImageView.setImageResource(imageResId)
    }

    private fun saveUrineCheckToDatabase(status: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$uid/urineCheck")
            userRef.setValue(status)  // Overwrites the previous urine check value
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
}
