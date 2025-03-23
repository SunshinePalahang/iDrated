import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.idrated.AboutUsActivity
import com.example.idrated.LoginActivity
import com.example.idrated.ProfileActivity
import com.example.idrated.R
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var profileButton: Button
    private lateinit var aboutUsButton: Button
    private lateinit var logoutButton: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileButton = view.findViewById(R.id.profile_button)
        aboutUsButton = view.findViewById(R.id.about_us_button)
        logoutButton = view.findViewById(R.id.logout_button)

        // Set up button click listeners
        profileButton.setOnClickListener {
            showProfileSettings()
        }

        aboutUsButton.setOnClickListener {
            showAboutUs()
        }

        // Logout button logic
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showProfileSettings() {
        // Start the ProfileActivity when the profile button is clicked
        val intent = Intent(requireContext(), ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun showAboutUs() {
        val intent = Intent(requireContext(), AboutUsActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                activity?.finish()
            }
            .setNegativeButton("No", null)
        builder.create().show()
    }
}
