package com.example.idrated

import NotificationFragment
import SettingsFragment
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.idrated.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isNewUser = sharedPreferences.getBoolean("isNewUser", true)

        if (isNewUser) {
            showMotivationalQuotePopup()

            // Save registration time
            sharedPreferences.edit().apply {
                putBoolean("isNewUser", false)
                putLong("registrationTime", System.currentTimeMillis())
                apply()
            }

            // Schedule check for hydration in 2 hours
            scheduleNewUserHydrationCheck()
        }

        val periodicRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(periodicRequest)

        // Fetch user data
        fetchUserData()

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, GoalActivity::class.java)
            startActivity(intent)
        }

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

        val bellIcon = findViewById<ImageView>(R.id.notification)

        bellIcon.setOnClickListener {
            // Set active icon
            bellIcon.setImageResource(R.drawable.bell2)

            // Deselect bottom nav items visually
            binding.bottomNavigation.menu.setGroupCheckable(0, false, true)

            // Load the NotificationFragment
            replaceFragment(NotificationFragment())
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

                        val hasSeenPopup = sharedPreferences.getBoolean("hasSeenPopup", false)
                        if (hasSeenPopup) {
                            showUrineColorAnalysisPopup()
                        } else {
                            // Mark that the user has now seen it to avoid future popups
                            editor.putBoolean("hasSeenPopup", true)
                        }

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

    private fun showNotification(title: String, message: String) {
        val channelId = "hydration_reminder_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Hydration Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.bell2)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(1002, notification)
    }

    private fun showMotivationalQuotePopup() {
        val context = this
        val dialog = AlertDialog.Builder(context).create()
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.motivational_quote_popup, null)

        val quoteTextView = layout.findViewById<TextView>(R.id.quoteTextView)
        val closeButton = layout.findViewById<Button>(R.id.closeButton)

        val quotes = listOf(
            "“Drink water like you love your body.”",
            "“Every sip counts. Start now.”",
            "“Hydrate to feel great!”",
            "“Strong starts with water.”",
            "“Your body’s whisper: Drink more water.”"
        )

        quoteTextView.text = quotes.random()

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setView(layout)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun scheduleNewUserHydrationCheck() {
        val workRequest = OneTimeWorkRequestBuilder<NewUserHydrationWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
