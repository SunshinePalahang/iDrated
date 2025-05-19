package com.example.idrated

import SettingsFragment
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.idrated.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private var isNotificationActive = false
    private var lastSelectedNavItemId: Int = R.id.nav_goal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isNewUser = sharedPreferences.getBoolean("isNewUser", true)

        if (isNewUser) {
            showMotivationalQuotePopup()
            savePopupMessageToHistory("Welcome to iDrated! Stay hydrated and healthy.")
            sharedPreferences.edit().apply {
                putBoolean("isNewUser", false)
                putLong("registrationTime", System.currentTimeMillis())
                apply()
            }
            scheduleNewUserHydrationCheck()
        }

        val periodicRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(periodicRequest)

        fetchUserData()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, GoalActivity::class.java))
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_goal
            replaceFragment(GoalFragment())
            updateNavigationIcons(R.id.nav_goal)
        }

        val bellIcon = findViewById<ImageView>(R.id.notification)
        bellIcon.setOnClickListener {
            toggleNotificationFragment(bellIcon)
        }

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            val selectedItemId = menuItem.itemId
            updateNavigationIcons(selectedItemId)

            isNotificationActive = false
            bellIcon.setImageResource(R.drawable.bell1)
            binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

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

    private fun toggleNotificationFragment(bellIcon: ImageView) {
        if (!isNotificationActive) {
            lastSelectedNavItemId = binding.bottomNavigation.selectedItemId
            isNotificationActive = true
            bellIcon.setImageResource(R.drawable.bell2)
            binding.bottomNavigation.menu.setGroupCheckable(0, false, true)
            replaceFragment(NotificationFragment())
        } else {
            isNotificationActive = false
            bellIcon.setImageResource(R.drawable.bell1)
            binding.bottomNavigation.menu.setGroupCheckable(0, true, true)
            binding.bottomNavigation.selectedItemId = lastSelectedNavItemId
            updateNavigationIcons(lastSelectedNavItemId)
        }
    }

    private fun fetchUserData() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loadUserDataFromPreferences()

        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").value?.toString() ?: "User"
                val avatarId = snapshot.child("profile_avatar_res_id").value?.toString()?.toIntOrNull()
                val waterConsumed = snapshot.child("waterConsumed").value?.toString()?.toIntOrNull() ?: 0
                val waterGoal = snapshot.child("waterGoal").value?.toString()?.toIntOrNull() ?: 0

                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                avatarId?.let { editor.putInt("profile_avatar_res_id", it) }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lastSavedDate = sharedPreferences.getString("lastSavedDate", "")
                if (today != lastSavedDate) {
                    resetWaterConsumed()
                    if (sharedPreferences.getBoolean("hasSeenPopup", false)) {
                        showUrineColorAnalysisPopup()
                    } else {
                        editor.putBoolean("hasSeenPopup", true)
                    }
                    editor.putString("lastSavedDate", today)
                }

                editor.apply()
                updateUserData(username, avatarId)

                checkHydrationGoalAchievement(waterConsumed, waterGoal)
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "Error fetching user data: ${it.message}")
        }
    }

    private fun checkHydrationGoalAchievement(consumed: Int, goal: Int) {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val lastNotified = prefs.getString("goalAchievedDate", "")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (consumed >= goal && lastNotified != today) {
            Toast.makeText(this, "🎉 You achieved your hydration goal today!", Toast.LENGTH_LONG).show()
            savePopupMessageToHistory("🎉 You achieved your hydration goal today!")

            prefs.edit().putString("goalAchievedDate", today).apply()
        }
    }

    private fun resetWaterConsumed() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        userRef.child("waterConsumed").setValue(0)
            .addOnSuccessListener {
                Toast.makeText(this, "Water consumption reset.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reset water: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUrineColorAnalysisPopup() {
        val dialog = AlertDialog.Builder(this).setCancelable(false).create()
        val layout = LayoutInflater.from(this).inflate(R.layout.urine_color_analysis_popup, null)

        val hydrationStatus = layout.findViewById<TextView>(R.id.hydrationStatusTextView)
        val saveButton = layout.findViewById<Button>(R.id.saveButton)
        var selectedStatus = ""

        layout.findViewById<Button>(R.id.lightButton).setOnClickListener {
            selectedStatus = "Well-hydrated"
            hydrationStatus.text = selectedStatus
            saveButton.isEnabled = true
        }

        layout.findViewById<Button>(R.id.moderateButton).setOnClickListener {
            selectedStatus = "Slightly Dehydrated"
            hydrationStatus.text = selectedStatus
            saveButton.isEnabled = true
        }

        layout.findViewById<Button>(R.id.darkButton).setOnClickListener {
            selectedStatus = "Dehydrated"
            hydrationStatus.text = selectedStatus
            saveButton.isEnabled = true
        }

        saveButton.setOnClickListener {
            saveUrineCheckToDatabase(selectedStatus)
            dialog.dismiss()
        }

        dialog.setView(layout)
        dialog.show()
    }

    private fun saveUrineCheckToDatabase(status: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users/$uid/urineCheck")
            .setValue(status)
            .addOnFailureListener {
                Toast.makeText(this, "Error saving: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMotivationalQuotePopup() {
        val dialog = AlertDialog.Builder(this).create()
        val layout = LayoutInflater.from(this).inflate(R.layout.motivational_quote_popup, null)

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

        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.setView(layout)
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun savePopupMessageToHistory(message: String) {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val gson = Gson()
        val uid = auth.currentUser?.uid ?: return

        val localJson = sharedPreferences.getString("notificationHistory", "[]")
        val type = object : TypeToken<MutableList<HydrationReminderWorker.NotificationEntry>>() {}.type
        val history: MutableList<HydrationReminderWorker.NotificationEntry> =
            gson.fromJson(localJson, type) ?: mutableListOf()

        val now = System.currentTimeMillis()
        history.add(HydrationReminderWorker.NotificationEntry(now, message))

        sharedPreferences.edit().apply {
            putString("notificationHistory", gson.toJson(history))
            putLong("lastNotificationTime", now)
            apply()
        }
    }

    private fun scheduleNewUserHydrationCheck() {
        val workRequest = OneTimeWorkRequestBuilder<NewUserHydrationWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun loadUserDataFromPreferences() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = prefs.getString("username", "User") ?: "User"
        val avatarId = prefs.getInt("profile_avatar_res_id", R.drawable.dflt_user)
        updateUserData(username, avatarId)
    }

    private fun updateUserData(username: String, avatarResId: Int?) {
        findViewById<TextView>(R.id.username).text = username
        findViewById<ImageView>(R.id.profile_icon).setImageResource(avatarResId ?: R.drawable.dflt_user)
    }

    private fun updateNavigationIcons(selectedItemId: Int) {
        val menu = binding.bottomNavigation.menu
        menu.findItem(R.id.nav_weather).setIcon(R.drawable.weather1)
        menu.findItem(R.id.nav_history).setIcon(R.drawable.history1)
        menu.findItem(R.id.nav_goal).setIcon(R.drawable.goal1)
        menu.findItem(R.id.nav_tips).setIcon(R.drawable.tips1)
        menu.findItem(R.id.nav_settings).setIcon(R.drawable.settings1)

        when (selectedItemId) {
            R.id.nav_weather -> menu.findItem(R.id.nav_weather).setIcon(R.drawable.weather2)
            R.id.nav_history -> menu.findItem(R.id.nav_history).setIcon(R.drawable.history2)
            R.id.nav_goal -> menu.findItem(R.id.nav_goal).setIcon(R.drawable.goal2)
            R.id.nav_tips -> menu.findItem(R.id.nav_tips).setIcon(R.drawable.tips2)
            R.id.nav_settings -> menu.findItem(R.id.nav_settings).setIcon(R.drawable.settings2)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
