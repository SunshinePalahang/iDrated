package com.example.idrated

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.idrated.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        val passwordToggle: ImageView = binding.registerPasswordVisibilityToggle
        val confirmPasswordToggle: ImageView = binding.confirmPasswordVisibilityToggle
        val passwordInput: EditText = binding.registerPasswordInput
        val confirmPasswordInput: EditText = binding.confirmPasswordInput

        passwordToggle.setOnClickListener {
            togglePasswordVisibility(passwordInput, passwordToggle)
        }

        confirmPasswordToggle.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput, confirmPasswordToggle)
        }

        val termsCheckBox: CheckBox = binding.termsCheckBox
        val termsLinkText: TextView = binding.termsLinkText

        termsLinkText.setOnClickListener {
            val intent = Intent(this, TandCActivity::class.java)
            startActivity(intent)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.registerEmailInput.text.toString()
            val password = binding.registerPasswordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (password == confirmPassword) {
                    if (termsCheckBox.isChecked) {
                        registerUser(email, password)
                    } else {
                        Toast.makeText(this, "You must agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            }
        }

        val loginLink: TextView = binding.loginLink
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun togglePasswordVisibility(passwordInput: EditText, passwordToggle: ImageView) {
        if (passwordInput.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_visibility)
        } else {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        }
        passwordInput.setSelection(passwordInput.text.length)
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    val userMap = mapOf(
                        "email" to email,
                        "emailVerified" to false
                    )

                    if (userId != null) {
                        db.reference.child("users").child(userId)
                            .setValue(userMap)
                            .addOnSuccessListener {
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            showVerificationDialog()
                                        } else {
                                            Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save user details", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showVerificationDialog() {
        val user = auth.currentUser
        val email = user?.email ?: "Unknown Email"

        if (user != null && user.isEmailVerified) {
            checkEmailVerification()
            return
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Verify Your Email")
        builder.setMessage("A verification email has been sent to $email.\n\nPlease check your inbox. If you didn't receive it, tap 'Resend'.")

        builder.setPositiveButton("Resend", null)

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            val resendButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)

            fun startCooldown() {
                var secondsLeft = 30
                resendButton.isEnabled = false

                val timer = object : CountDownTimer(30000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        resendButton.text = "Resend (${--secondsLeft}s)"
                    }

                    override fun onFinish() {
                        resendButton.text = "Resend"
                        resendButton.isEnabled = true
                    }
                }
                timer.start()
            }

            resendButton.setOnClickListener {
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { resendTask ->
                        if (resendTask.isSuccessful) {
                            Toast.makeText(this, "Verification email resent", Toast.LENGTH_SHORT).show()
                            startCooldown()
                        } else {
                            Toast.makeText(this, "Failed to resend email", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            startCooldown()

            // Auto-check verification every 5 seconds
            val checkTimer = object : CountDownTimer(5 * 60 * 1000, 5000) { // up to 5 minutes
                override fun onTick(millisUntilFinished: Long) {
                    user?.reload()?.addOnCompleteListener { reloadTask ->
                        if (reloadTask.isSuccessful && user.isEmailVerified) {
                            dialog.dismiss()
                            updateEmailVerifiedStatus(user.uid)
                            val onboardingIntent = Intent(this@RegisterActivity, OnboardingActivity::class.java)
                            startActivity(onboardingIntent)
                            finish()
                            cancel() // stop the timer
                        }
                    }
                }

                override fun onFinish() {
                }
            }
            checkTimer.start()
        }

        dialog.show()
    }


    private fun checkEmailVerification() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                if (user.isEmailVerified) {
                    updateEmailVerifiedStatus(user.uid)
                    val onboardingIntent = Intent(this, OnboardingActivity::class.java)
                    startActivity(onboardingIntent)
                    finish()
                } else {
                    Toast.makeText(this, "Please verify your email before continuing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to reload user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmailVerifiedStatus(userId: String) {
        val userRef = db.reference.child("users").child(userId)
        userRef.child("emailVerified").setValue(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Email verification status updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update email verification status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (user.isEmailVerified) {
                    updateEmailVerifiedStatus(user.uid)
                    val onboardingIntent = Intent(this, OnboardingActivity::class.java)
                    startActivity(onboardingIntent)
                    finish()
                }
            }
        }
    }
}
