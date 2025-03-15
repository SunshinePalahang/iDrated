package com.example.idrated

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.idrated.databinding.FragmentTandcBinding

class TandCActivity : AppCompatActivity() {
    private lateinit var binding: FragmentTandcBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentTandcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the back button to navigate to RegisterActivity
        findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.backButton).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
