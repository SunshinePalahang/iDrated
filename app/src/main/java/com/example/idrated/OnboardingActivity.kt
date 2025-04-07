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
            if (currentItem < 6) { // Updated for 7 steps
                onSaveListener?.invoke() // Notify fragment to save data
                viewPager.currentItem = currentItem + 1
                updateButtonState(currentItem + 1)
            } else {
                navigateToMainActivity() // Redirects to the main app
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

        // Initialize interaction states for all pages (Require interaction for input pages)
        for (i in 0..5) userInputStates[i] = false
        userInputStates[6] = true // The last page is marked as completed by default
    }

    private fun updateButtonState(currentPage: Int) {
        backButton.isEnabled = currentPage > 0
        nextButton.text = if (currentPage == 6) "Get Started" else ">"
        nextButton.isEnabled = userInputStates[currentPage] == true
    }

    fun markPageAsInteracted(pageIndex: Int) {
        userInputStates[pageIndex] = true
        updateButtonState(pageIndex)
    }

    fun setOnSaveListener(listener: () -> Unit) {
        onSaveListener = listener
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
