package com.example.idrated

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.idrated.databinding.ActivityAboutUsBinding

class AboutUsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the correct layout for About Us
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the back button to navigate to SettingsFragment
        binding.backButton.setOnClickListener {
            finish() // Simply finish the current activity instead of recreating SettingsFragment
        }
    }
}
