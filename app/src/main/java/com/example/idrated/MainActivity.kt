package com.example.idrated

import SettingsFragment
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.idrated.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var timeText: TextView
    private val handler = Handler()
    private val timeRunnable: Runnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = sdf.format(Date())
            timeText.text = currentTime
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timeText = findViewById(R.id.time_text)

        // Start updating the time every second
        handler.post(timeRunnable)

        // Fetch user data
        fetchUserData()

        // Initialize the default fragment
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_goal
            replaceFragment(GoalFragment())
            updateNavigationIcons(R.id.nav_goal)
        }

        // Handle bottom navigation item selection
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            val selectedItemId = menuItem.itemId
            updateNavigationIcons(selectedItemId)

            when (selectedItemId) {
                R.id.nav_weather -> replaceFragment(WeatherFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_goal -> replaceFragment(GoalFragment())
                R.id.nav_tips -> replaceFragment(TipsFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                else -> false
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserData()
    }
    private fun fetchUserData() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loadUserDataFromPreferences()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = database.child("users").child(userId)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val storedUsername = snapshot.child("username").value?.toString() ?: "User"
                    val storedProfileAvatarResId = snapshot.child("profile_avatar_res_id").value?.toString()?.toIntOrNull()
                    val editor = sharedPreferences.edit()
                    editor.putString("username", storedUsername)
                    storedProfileAvatarResId?.let { editor.putInt("profile_avatar_res_id", it) }
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val lastSavedDate = sharedPreferences.getString("lastSavedDate", "")

                    if (currentDate != lastSavedDate) {
                        resetWaterConsumed()
                        with(editor) {
                            putString("lastSavedDate", currentDate)
                            apply()
                        }
                    }
                    editor.apply()
                    updateUserData(storedUsername, storedProfileAvatarResId)
                }
            }.addOnFailureListener {
                Log.e("MainActivity", "Error fetching user data: ${it.message}")
            }
        }
    }
    private fun resetWaterConsumed() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        showUrineColorAnalysisPopup()
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

            userRef.child("waterConsumed").setValue(0)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Water consumption has been reset.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this@MainActivity, "Error resetting water consumption: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showUrineColorAnalysisPopup() {
        val context = this

        val dialog = AlertDialog.Builder(context)
            .setCancelable(false)
            .create()

        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.urine_color_analysis_popup, null)

        val hydrationStatusTextView = layout.findViewById<TextView>(R.id.hydrationStatusTextView)
        val lightButton = layout.findViewById<Button>(R.id.lightButton)
        val moderateButton = layout.findViewById<Button>(R.id.moderateButton)
        val darkButton = layout.findViewById<Button>(R.id.darkButton)
        val saveButton = layout.findViewById<Button>(R.id.saveButton)

        var selectedStatus = ""

        fun updateSelection(statusText: String) {
            selectedStatus = statusText
            hydrationStatusTextView.text = statusText
            saveButton.isEnabled = true
        }

        lightButton.setOnClickListener { updateSelection("Well-hydrated") }
        moderateButton.setOnClickListener { updateSelection("Slightly Dehydrated") }
        darkButton.setOnClickListener { updateSelection("Dehydrated") }

        saveButton.setOnClickListener {
            saveUrineCheckToDatabase(selectedStatus)
            dialog.dismiss()
        }

        dialog.setView(layout)
        dialog.show()
    }

    private fun saveUrineCheckToDatabase(status: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/$uid/urineCheck")
            userRef.setValue(status)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving urine check: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadUserDataFromPreferences() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "User") ?: "User"
        val profileAvatarResId = sharedPreferences.getInt("profile_avatar_res_id", R.drawable.dflt_user)

        updateUserData(username, profileAvatarResId)
    }
    private fun updateUserData(username: String, avatarResId: Int?) {
        findViewById<TextView>(R.id.username).text = username
        val profileIcon = findViewById<ImageView>(R.id.profile_icon)
        profileIcon.setImageResource(avatarResId ?: R.drawable.dflt_user)
    }


    private fun updateNavigationIcons(selectedItemId: Int) {
        binding.bottomNavigation.menu.findItem(R.id.nav_weather).setIcon(R.drawable.weather1)
        binding.bottomNavigation.menu.findItem(R.id.nav_history).setIcon(R.drawable.history1)
        binding.bottomNavigation.menu.findItem(R.id.nav_goal).setIcon(R.drawable.goal1)
        binding.bottomNavigation.menu.findItem(R.id.nav_tips).setIcon(R.drawable.tips1)
        binding.bottomNavigation.menu.findItem(R.id.nav_settings).setIcon(R.drawable.settings1)

        when (selectedItemId) {
            R.id.nav_weather -> binding.bottomNavigation.menu.findItem(R.id.nav_weather).setIcon(R.drawable.weather2)
            R.id.nav_history -> binding.bottomNavigation.menu.findItem(R.id.nav_history).setIcon(R.drawable.history2)
            R.id.nav_goal -> binding.bottomNavigation.menu.findItem(R.id.nav_goal).setIcon(R.drawable.goal2)
            R.id.nav_tips -> binding.bottomNavigation.menu.findItem(R.id.nav_tips).setIcon(R.drawable.tips2)
            R.id.nav_settings -> binding.bottomNavigation.menu.findItem(R.id.nav_settings).setIcon(R.drawable.settings2)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeRunnable)
    }
}
