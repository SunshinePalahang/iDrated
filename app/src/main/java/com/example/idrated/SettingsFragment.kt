import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.idrated.AboutUsActivity
import com.example.idrated.LoginActivity
import com.example.idrated.ProfileActivity
import com.example.idrated.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var profileButton: Button
    private lateinit var aboutUsButton: Button
    private lateinit var accountSettingsButton: Button
    private lateinit var logoutButton: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileButton = view.findViewById(R.id.profile_button)
        aboutUsButton = view.findViewById(R.id.about_us_button)
        accountSettingsButton = view.findViewById(R.id.account_settings_button)
        logoutButton = view.findViewById(R.id.logout_button)

        profileButton.setOnClickListener {
            showProfileSettings()
        }

        aboutUsButton.setOnClickListener {
            showAboutUs()
        }

        accountSettingsButton.setOnClickListener {
            showAccountSettingsDialog()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showProfileSettings() {
        startActivity(Intent(requireContext(), ProfileActivity::class.java))
    }

    private fun showAboutUs() {
        startActivity(Intent(requireContext(), AboutUsActivity::class.java))
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                activity?.finish()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun showAccountSettingsDialog() {
        val options = arrayOf("Reset Password", "Delete Account")
        AlertDialog.Builder(requireContext())
            .setTitle("Account Settings")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> sendPasswordResetEmail()
                    1 -> confirmDeleteAccount()
                }
            }
            .create()
            .show()
    }

    private fun sendPasswordResetEmail() {
        val user = auth.currentUser
        if (user?.email != null) {
            auth.sendPasswordResetEmail(user.email!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Reset email sent to ${user.email}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "No email associated with this account", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteAccount() {
        // Ask user to re-enter password for security reasons
        val input = EditText(requireContext())
        input.hint = "Enter your password"

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Please enter your password to confirm account deletion.")
            .setView(input)
            .setPositiveButton("Delete") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    reauthenticateAndDelete(password)
                } else {
                    Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun reauthenticateAndDelete(password: String) {
        val user = auth.currentUser
        val email = user?.email
        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Re-authentication successful, now delete
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_LONG).show()
                            // Log out and send to login screen
                            auth.signOut()
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            activity?.finish()
                        } else {
                            Toast.makeText(requireContext(), "Account deletion failed: ${deleteTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Re-authentication failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
