package com.example.idrated

import android.content.Context
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UsernameInputFragment : Fragment(R.layout.fragment_username_input) {

    private lateinit var onboardingActivity: OnboardingActivity
    private lateinit var chooseAvatarButton: Button
    private lateinit var photoDisplay: ImageView
    private val avatarList = listOf(
        R.drawable.avatar_f1, R.drawable.avatar_m1, R.drawable.avatar_f2, R.drawable.avatar_m2
    )
    private var selectedAvatarResId: Int? = null
    private val defaultAvatarResId = R.drawable.dflt_user // Default avatar resource

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference to the parent activity
        onboardingActivity = activity as OnboardingActivity

        // Reference to views in the layout
        val usernameInput: EditText = view.findViewById(R.id.usernameInput)
        chooseAvatarButton = view.findViewById(R.id.chooseAvatarButton)
        photoDisplay = view.findViewById(R.id.photoDisplay)

        // Set the initial profile photo to the default avatar
        photoDisplay.setImageResource(defaultAvatarResId)

        // Handle username input change and mark page as interacted
        usernameInput.addTextChangedListener { editable ->
            if (!editable.isNullOrEmpty()) {
                onboardingActivity.markPageAsInteracted(0)
            }
        }

        // Handle avatar selection
        chooseAvatarButton.setOnClickListener {
            showAvatarSelectionDialog()
        }

        // Set the save listener for the "Next" button
        onboardingActivity.setOnSaveListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
            } else if (username.length !in 6..8) {
                Toast.makeText(context, "Username must be 6 to 8 characters long", Toast.LENGTH_SHORT).show()
            } else {
                saveUserData(username)
            }
        }
    }

    // Show a dialog with avatars and their borders
    private fun showAvatarSelectionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Slide to choose an Avatar")

        // Inflate the custom dialog layout
        val view = layoutInflater.inflate(R.layout.dialog_avatar_selection, null)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        // Create the dialog object here to manage its state
        val dialog = builder.setView(view)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        // Set up RecyclerView to display avatars
        val avatarAdapter = AvatarAdapter(avatarList) { selectedAvatar ->
            selectedAvatarResId = selectedAvatar
            dialog.dismiss() // Close the dialog when an avatar is selected
            updateProfilePhoto()
        }

        // Use the custom SnappyLinearLayoutManager
        recyclerView.layoutManager = SnappyLinearLayoutManager(requireContext())
        recyclerView.adapter = avatarAdapter

        // Attach LinearSnapHelper to center the avatars
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Show the dialog
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateProfilePhoto() {
        if (selectedAvatarResId != null) {
            // Update the profile photo with the selected avatar
            photoDisplay.setImageResource(selectedAvatarResId!!)

            // Add a border to the ImageView using a drawable background
            photoDisplay.background = requireContext().getDrawable(R.drawable.avatar_border)

            // Change the button text to "Change Avatar"
            chooseAvatarButton.text = "Change Avatar"

            // Ensure the photo display is visible
            photoDisplay.visibility = View.VISIBLE
        } else {
            // If no avatar is selected, set the default image
            photoDisplay.setImageResource(defaultAvatarResId)
            photoDisplay.background = null // Remove the border if any
            chooseAvatarButton.text = "Choose Avatar"
        }
    }

    private fun saveUserData(username: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if an avatar is selected
        if (selectedAvatarResId != null) {
            // Save the selected avatar to the database
            saveProfileToDatabase(userId, username, selectedAvatarResId!!)
        } else {
            // Save the default avatar if none is selected
            saveProfileToDatabase(userId, username, defaultAvatarResId)
        }
    }

    private fun saveProfileToDatabase(userId: String, username: String, avatarResId: Int) {
        val userMap = mutableMapOf<String, Any>()
        userMap["username"] = username
        userMap["profile_avatar_res_id"] = avatarResId

        // Update user data in Firebase
        database.child("users").child(userId).updateChildren(userMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "User data saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Custom LinearLayoutManager for controlled avatar sliding
    class SnappyLinearLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {
        // Custom layout manager can control snapping behavior
        init {
            isSmoothScrollbarEnabled = true
        }
    }
}
