package com.example.idrated

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AgeInputFragment : Fragment(R.layout.fragment_age_input) {

    private lateinit var onboardingActivity: OnboardingActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reference to the parent activity
        onboardingActivity = activity as OnboardingActivity

        val ageSeekBar: SeekBar = view.findViewById(R.id.ageSeekBar)
        val ageEditText: EditText = view.findViewById(R.id.ageEditText)

        // Customize SeekBar thumb
        val thumbDrawable = ageSeekBar.thumb
        val wrappedDrawable = DrawableCompat.wrap(thumbDrawable)
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        ageSeekBar.thumb = wrappedDrawable

        // Limit input to numbers from 0 to 100
        ageEditText.filters = arrayOf(InputFilter.LengthFilter(3))

        // Update SeekBar as EditText changes
        ageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val age = s.toString().toInt()
                    if (age in 0..100) {
                        ageSeekBar.progress = age
                    } else if (age > 100) {
                        ageEditText.setText("100")
                        ageEditText.setSelection(ageEditText.text.length)
                    }
                } catch (e: NumberFormatException) {
                    ageEditText.setText("0")
                    ageEditText.setSelection(ageEditText.text.length)
                }

                // Mark page as interacted if valid
                if (!s.isNullOrEmpty() && s.toString().toIntOrNull() in 0..100) {
                    onboardingActivity.markPageAsInteracted(0)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Update EditText as SeekBar changes
        ageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (ageEditText.text.toString() != progress.toString()) {
                    ageEditText.setText(progress.toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save age on button interaction
        onboardingActivity.setOnSaveListener {
            val age = ageEditText.text.toString().toIntOrNull()
            if (age != null) {
                saveAgeToDatabase(age)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid age.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAgeToDatabase(age: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users/$uid")

            userRef.child("age").setValue(age)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
        } else {
        }
    }
}
