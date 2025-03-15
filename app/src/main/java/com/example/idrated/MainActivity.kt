package com.example.idrated

import SettingsFragment
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
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
        fetchUserData() // Fetch latest user data when activity resumes
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = database.child("users").child(userId)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val storedUsername = snapshot.child("username").value?.toString() ?: "User"
                    val storedProfileAvatarResId = snapshot.child("profile_avatar_res_id").value?.toString()?.toIntOrNull()

                    // Set the username
                    findViewById<TextView>(R.id.username).text = storedUsername

                    // Set the profile image
                    val profileIcon = findViewById<ImageView>(R.id.profile_icon)
                    if (storedProfileAvatarResId != null) {
                        profileIcon.setImageResource(storedProfileAvatarResId)
                    } else {
                        profileIcon.setImageResource(R.drawable.dflt_user)
                    }
                }
            }.addOnFailureListener { error ->
                Log.e("MainActivity", "Error fetching user data: ${error.message}")
            }
        }
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
