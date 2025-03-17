package com.example.idrated

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    private lateinit var viewPager: ViewPager2
    private val userInputStates = mutableMapOf<Int, Boolean>() // Tracks interaction state

    private var onSaveListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = OnboardingAdapter(this)

        viewPager.isUserInputEnabled = false

        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)
        updateButtonState(viewPager.currentItem)

        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < 5) { // Updated to include the last fragment
                onSaveListener?.invoke() // Notify the fragment to save data
                viewPager.currentItem = currentItem + 1
                updateButtonState(currentItem + 1)
            } else {
                navigateToMainActivity() // Changed method name here
            }
        }

        backButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem > 0) {
                viewPager.currentItem = currentItem - 1
                updateButtonState(currentItem - 1)
            }
        }

        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15

            if (viewPager.currentItem == 0 && isKeyboardVisible) {
                nextButton.visibility = View.GONE
            } else {
                nextButton.visibility = View.VISIBLE
            }
        }

        // Initialize interaction states for all pages
        for (i in 0..4) userInputStates[i] = false // Require interaction on pages 0-3
        userInputStates[5] = true // Mark the last page as already interacted
    }

    private fun updateButtonState(currentPage: Int) {
        backButton.isEnabled = currentPage > 0
        nextButton.text = if (currentPage == 5) "Get Started" else ">"
        nextButton.isEnabled = userInputStates[currentPage] == true
    }

    fun markPageAsInteracted(pageIndex: Int) {
        userInputStates[pageIndex] = true
        updateButtonState(pageIndex)
    }

    fun setOnSaveListener(listener: () -> Unit) {
        onSaveListener = listener
    }

    // Change the method to navigate to MainActivity after the onboarding process
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
