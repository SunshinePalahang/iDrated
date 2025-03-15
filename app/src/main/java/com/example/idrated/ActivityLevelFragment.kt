package com.example.idrated

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ActivityLevelFragment : Fragment(R.layout.fragment_activity_level) {

    private lateinit var sedentaryButton: Button
    private lateinit var moderateActivityButton: Button
    private lateinit var highlyActiveButton: Button
    private lateinit var lightlyActiveButton: Button
    private lateinit var activityImageView: ImageView
    private lateinit var onboardingActivity: OnboardingActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the buttons and caption TextViews
        sedentaryButton = view.findViewById(R.id.sedentaryButton)
        moderateActivityButton = view.findViewById(R.id.moderateActivityButton)
        highlyActiveButton = view.findViewById(R.id.highlyActiveButton)
        lightlyActiveButton = view.findViewById(R.id.lightlyActiveButton)
        activityImageView = view.findViewById(R.id.activityImageView)
        val activityCaptionTextView: TextView = view.findViewById(R.id.activityCaptionTextView)
        val activityDescriptionTextView: TextView = view.findViewById(R.id.activityDescriptionTextView)

        // Get reference to the parent activity
        onboardingActivity = activity as OnboardingActivity

        // Set up button click listeners
        sedentaryButton.setOnClickListener {
            updateSelection(
                sedentaryButton,
                activityCaptionTextView,
                activityDescriptionTextView,
                "Sedentary",
                "Little to no physical activity. Often involves sitting or lying down for most of the day.",
                R.drawable.sedentary // Image for Sedentary
            )
            saveActivityLevelToDatabase("Sedentary")
            onboardingActivity.markPageAsInteracted(2) // Mark the page as interacted (page 2 is the activity level page)
        }

        lightlyActiveButton.setOnClickListener {
            updateSelection(
                lightlyActiveButton,
                activityCaptionTextView,
                activityDescriptionTextView,
                "Lightly Active",
                "Light physical activity, such as casual walking or easy cycling. Exercise 1-2 days a week.",
                R.drawable.lightly // Image for Lightly Active
            )
            saveActivityLevelToDatabase("Lightly Active")
            onboardingActivity.markPageAsInteracted(2) // Mark the page as interacted (page 2 is the activity level page)
        }

        moderateActivityButton.setOnClickListener {
            updateSelection(
                moderateActivityButton,
                activityCaptionTextView,
                activityDescriptionTextView,
                "Moderately Active",
                "Regular physical activity like walking, light jogging, or light cycling. Some exercise 3-5 days a week.",
                R.drawable.moderately // Image for Moderately Active
            )
            saveActivityLevelToDatabase("Moderately Active")
            onboardingActivity.markPageAsInteracted(2) // Mark the page as interacted (page 2 is the activity level page)
        }

        highlyActiveButton.setOnClickListener {
            updateSelection(
                highlyActiveButton,
                activityCaptionTextView,
                activityDescriptionTextView,
                "Highly Active",
                "Intense physical activity, such as running, weight lifting, or intense sports. Frequent exercise 6-7 days a week.",
                R.drawable.highly // Image for Highly Active
            )
            saveActivityLevelToDatabase("Highly Active")
            onboardingActivity.markPageAsInteracted(2) // Mark the page as interacted (page 2 is the activity level page)
        }
    }

    // Update the selection with visual feedback
    private fun updateSelection(
        selectedButton: Button,
        captionTextView: TextView,
        descriptionTextView: TextView,
        captionText: String,
        descriptionText: String,
        imageResId: Int
    ) {
        // Update text views
        captionTextView.text = captionText
        descriptionTextView.text = descriptionText

        // Update the ImageView with the selected image
        activityImageView.setImageResource(imageResId)

        // Update button styles for selection
        setButtonSelectedStyle(selectedButton)

        // Reset other buttons
        if (selectedButton != sedentaryButton) resetButtonStyle(sedentaryButton)
        if (selectedButton != moderateActivityButton) resetButtonStyle(moderateActivityButton)
        if (selectedButton != highlyActiveButton) resetButtonStyle(highlyActiveButton)
        if (selectedButton != lightlyActiveButton) resetButtonStyle(lightlyActiveButton)
    }

    // Apply selected style to the chosen button
    private fun setButtonSelectedStyle(button: Button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        button.setTextColor(Color.WHITE)
        button.elevation = 8f
    }

    // Reset button style to default
    private fun resetButtonStyle(button: Button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        button.setTextColor(Color.WHITE)
        button.elevation = 2f
    }

    // Save the selected activity level to the Realtime Database
    private fun saveActivityLevelToDatabase(activityLevel: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/$uid")

            userRef.child("activityLevel").setValue(activityLevel)
                .addOnSuccessListener {
                    // Activity level saved successfully
                }
                .addOnFailureListener { e ->
                    // Handle error
                }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }
}
