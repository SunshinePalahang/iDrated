package com.example.idrated

import android.os.Bundle
import android.widget.*
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
    private lateinit var urineCheckSpinner: Spinner
    private lateinit var healthConditionTextView: TextView // Read-only Health Condition
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

        initializeViews()
        fetchUserData()

        backButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { saveUserData() }
        changeAvatarButton.setOnClickListener { showAvatarSelectionDialog() }
    }

    private fun initializeViews() {
        usernameInput = findViewById(R.id.username_input)
        profileImageView = findViewById(R.id.profile_image_input)
        ageInput = findViewById(R.id.age_input)
        genderSpinner = findViewById(R.id.gender_input)
        activityLevelSpinner = findViewById(R.id.activity_level_input)
        urineCheckSpinner = findViewById(R.id.urine_check_input)
        healthConditionTextView = findViewById(R.id.health_condition_text) // Read-only TextView
        saveButton = findViewById(R.id.save_button)
        changeAvatarButton = findViewById(R.id.change_avatar_button)
        backButton = findViewById(R.id.backButton)

        val genderOptions = arrayOf("Male", "Female")
        val activityLevelOptions = arrayOf("Sedentary", "Lightly Active", "Moderately Active", "Highly Active")
        val urineCheckOptions = arrayOf("Well-hydrated", "Slightly Dehydrated", "Dehydrated")

        genderSpinner.adapter = createSpinnerAdapter(genderOptions)
        activityLevelSpinner.adapter = createSpinnerAdapter(activityLevelOptions)
        urineCheckSpinner.adapter = createSpinnerAdapter(urineCheckOptions)
    }

    private fun createSpinnerAdapter(options: Array<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun fetchUserData() {
        currentUser?.let { user ->
            val userRef = database.child("users").child(user.uid)
            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    usernameInput.setText(snapshot.child("username").value?.toString() ?: "")
                    ageInput.setText(snapshot.child("age").value?.toString() ?: "")

                    val gender = snapshot.child("gender").value?.toString()
                    gender?.let { setSpinnerSelection(genderSpinner, it) }

                    val activityLevel = snapshot.child("activityLevel").value?.toString()
                    activityLevel?.let { setSpinnerSelection(activityLevelSpinner, it) }

                    val urineCheck = snapshot.child("urineCheck").value?.toString()
                    urineCheck?.let { setSpinnerSelection(urineCheckSpinner, it) }

                    val healthCondition = snapshot.child("healthCondition").value?.toString()?.toBoolean()
                    healthConditionTextView.text = if (healthCondition == true) "Yes" else "No" // Set read-only health status

                    selectedAvatarResId = snapshot.child("profile_avatar_res_id").value?.toString()?.toIntOrNull()
                    profileImageView.setImageResource(selectedAvatarResId ?: R.drawable.dflt_user)
                }
            }
        }
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as? ArrayAdapter<String>
        adapter?.let {
            val position = it.getPosition(value)
            if (position >= 0) {
                spinner.setSelection(position)
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
            "urineCheck" to urineCheckSpinner.selectedItem.toString(),
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
