package com.example.idrated

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.idrated.databinding.ActivityWaterInputBinding

class WaterInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaterInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaterInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmit.setOnClickListener {
            val inputAmount = binding.etWaterAmount.text.toString()

            if (inputAmount.isNotEmpty()) {
                val intent = Intent(this, GoalActivity::class.java)
                intent.putExtra("input_water_amount", inputAmount)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a water amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
