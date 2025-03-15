package com.example.idrated

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var ageInput: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var activityLevelSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var changeAvatarButton: Button
    private lateinit var backButton: AppCompatTextView

    private val avatarList = listOf(
        R.drawable.avatar_f1, R.drawable.avatar_m1, R.drawable.avatar_f2, R.drawable.avatar_m2
    )
    private var selectedAvatarResId: Int? = null

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize views
        initializeViews()

        // Fetch user data from Firebase
        fetchUserData()

        // Back button logic
        backButton.setOnClickListener { finish() }

        // Save button logic
        saveButton.setOnClickListener { saveUserData() }

        // Change avatar button logic
        changeAvatarButton.setOnClickListener { showAvatarSelectionDialog() }
    }

    private fun initializeViews() {
        usernameInput = findViewById(R.id.username_input)
        profileImageView = findViewById(R.id.profile_image_input)
        ageInput = findViewById(R.id.age_input)
        genderSpinner = findViewById(R.id.gender_input)
        activityLevelSpinner = findViewById(R.id.activity_level_input)
        saveButton = findViewById(R.id.save_button)
        changeAvatarButton = findViewById(R.id.change_avatar_button)
        backButton = findViewById(R.id.backButton)

        // Initialize spinners
        val genderOptions = arrayOf("Male", "Female")
        val activityLevelOptions = arrayOf("Sedentary", "Lightly Active", "Moderately Active", "Highly Active")

        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = genderAdapter

        val activityLevelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, activityLevelOptions)
        activityLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activityLevelSpinner.adapter = activityLevelAdapter
    }

    private fun fetchUserData() {
        currentUser?.let { user ->
            val userRef = database.child("users").child(user.uid)
            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    usernameInput.setText(snapshot.child("username").value.toString())
                    ageInput.setText(snapshot.child("age").value.toString())

                    val gender = snapshot.child("gender").value.toString()
                    genderSpinner.setSelection((genderSpinner.adapter as ArrayAdapter<String>).getPosition(gender))

                    val activityLevel = snapshot.child("activityLevel").value.toString()
                    activityLevelSpinner.setSelection((activityLevelSpinner.adapter as ArrayAdapter<String>).getPosition(activityLevel))

                    val avatarResId = snapshot.child("profile_avatar_res_id").value?.toString()?.toIntOrNull()
                    if (avatarResId != null) {
                        selectedAvatarResId = avatarResId
                        profileImageView.setImageResource(avatarResId)
                    } else {
                        profileImageView.setImageResource(R.drawable.dflt_user)
                    }
                }
            }
        }
    }

    private fun saveUserData() {
        val username = usernameInput.text.toString().trim()
        val age = ageInput.text.toString().toIntOrNull()

        if (username.isEmpty() || age == null || age <= 0) {
            Toast.makeText(this, "Please fill out all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val userData = mapOf(
            "username" to username,
            "age" to age,
            "gender" to genderSpinner.selectedItem.toString(),
            "activityLevel" to activityLevelSpinner.selectedItem.toString(),
            "profile_avatar_res_id" to (selectedAvatarResId ?: R.drawable.dflt_user)
        )

        currentUser?.let { user ->
            database.child("users").child(user.uid).updateChildren(userData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAvatarSelectionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Slide to select Avatar")
        val view = layoutInflater.inflate(R.layout.dialog_avatar_selection, null)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        val dialog = builder.setView(view)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        val avatarAdapter = AvatarAdapter(avatarList) { selectedAvatar ->
            selectedAvatarResId = selectedAvatar
            dialog.dismiss()
            profileImageView.setImageResource(selectedAvatar)
        }

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = avatarAdapter
        dialog.show()
    }
}
