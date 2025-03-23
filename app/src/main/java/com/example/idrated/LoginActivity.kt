package com.example.idrated

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.idrated.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible: Boolean = false
    private val bluetoothPermissionRequestCode = 1
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()  // Initialize Realtime Database

        // Check Bluetooth state and request permission if needed
        checkBluetoothState()

        // Login button logic
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            loginUser(email, password)
        }

        // Register link logic
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Toggle password visibility
        binding.showHidePasswordIcon.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is off, prompt to enable it
            Toast.makeText(this, "Bluetooth is disabled. Please enable it.", Toast.LENGTH_SHORT).show()
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(enableBluetoothIntent, bluetoothPermissionRequestCode)
        } else {
            // Check Bluetooth permissions if Bluetooth is enabled
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            // Request the missing permissions
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), bluetoothPermissionRequestCode)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == bluetoothPermissionRequestCode) {
            if (bluetoothAdapter?.isEnabled == true) {
                Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show()
                // Re-check permissions after enabling Bluetooth
                checkAndRequestPermissions()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to proceed.", Toast.LENGTH_LONG).show()
                finish()  // Close the app if Bluetooth is not enabled
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userRef = db.getReference("users").child(userId!!)

                    userRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val userData = snapshot.value as? Map<*, *>  // Get user data
                            Toast.makeText(this, "Welcome, ${userData?.get("username")}", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java)) // Redirect to GoalActivity
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.showHidePasswordIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            // Show password
            binding.passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.showHidePasswordIcon.setImageResource(R.drawable.ic_visibility)
        }
        isPasswordVisible = !isPasswordVisible

        // Move the cursor to the end of the text
        binding.passwordInput.setSelection(binding.passwordInput.text.length)
    }
}
