package com.example.idrated

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val inLogo: ImageView = findViewById(R.id.inLogo)
        val outLogo: ImageView = findViewById(R.id.outLogo)
        val titleName: ImageView = findViewById(R.id.titleName)
        val tagName: ImageView = findViewById(R.id.tagName)

        // Fade in animation for inLogo
        val fadeInInLogo = ObjectAnimator.ofFloat(inLogo, "alpha", 0f, 1f).apply {
            duration = 1000 // 1 second for fade-in
            startDelay = 300 // Delay for outLogo fade-in after inLogo
        }

        // Fade in animation for outLogo with delay
        val fadeInOutLogo = ObjectAnimator.ofFloat(outLogo, "alpha", 0f, 1f).apply {
            duration = 800 // 1 second for fade-in
            startDelay = 300 // Delay for outLogo fade-in after inLogo
        }

        // Fade in and move up titleName after outLogo
        val fadeInTitleName = ObjectAnimator.ofFloat(titleName, "alpha", 0f, 1f).apply {
            duration = 500 // 1 second for fade-in
            startDelay = 300 // Delay the fade-in until after the logos have appeared
        }

        // Fade in and move up tagName after titleName
        val fadeInTagName = ObjectAnimator.ofFloat(tagName, "alpha", 0f, 1f).apply {
            duration = 500 // 1 second for fade-in
            startDelay = 300 // Delay the fade-in until after titleName finishes
        }

        // AnimatorSet to play animations sequentially
        val animatorSet = AnimatorSet().apply {
            playSequentially(
                fadeInInLogo,  // InLogo fade-in
                fadeInOutLogo,  // OutLogo fade-in
                fadeInTitleName,  // TitleName fade-in
                fadeInTagName  // TagName fade-in
            )
            start()
        }

        // Start MainActivity or LoginActivity after animations finish
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Check if the user is already logged in
                val currentUser = auth.currentUser
                val intent = if (currentUser != null) {
                    // If user is logged in, go to MainActivity
                    Intent(this@SplashScreenActivity, MainActivity::class.java)
                } else {
                    // If user is not logged in, go to LoginActivity
                    Intent(this@SplashScreenActivity, LoginActivity::class.java)
                }
                startActivity(intent)
                finish() // Finish SplashScreen so it's removed from the back stack
            }
        })
    }
}
