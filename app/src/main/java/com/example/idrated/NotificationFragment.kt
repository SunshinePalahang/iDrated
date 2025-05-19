package com.example.idrated

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class NotificationFragment : Fragment() {

    private lateinit var rvToday: RecyclerView
    private lateinit var rvPrevious: RecyclerView
    private lateinit var tvNoNotifications: TextView
    private lateinit var btnTogglePrevious: Button
    private lateinit var previousSection: LinearLayout
    private lateinit var todayAdapter: NotificationAdapter
    private lateinit var previousAdapter: NotificationAdapter
    private lateinit var btnClearNotifications: Button

    private val sharedPrefsName = "UserPrefs"
    private val notificationHistoryKey = "notificationHistory"

    private var isPreviousVisible = false

    private var todayNotifications: List<HydrationReminderWorker.NotificationEntry> = emptyList()
    private var previousNotifications: List<HydrationReminderWorker.NotificationEntry> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClearNotifications = view.findViewById(R.id.btnClearNotifications)

        btnClearNotifications.setOnClickListener {
            showClearConfirmationDialog()
        }

        rvToday = view.findViewById(R.id.rvToday)
        rvPrevious = view.findViewById(R.id.rvPrevious)
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications)
        btnTogglePrevious = view.findViewById(R.id.btnTogglePrevious)
        previousSection = view.findViewById(R.id.previousSection)

        rvToday.layoutManager = LinearLayoutManager(requireContext())
        rvPrevious.layoutManager = LinearLayoutManager(requireContext())

        todayAdapter = NotificationAdapter()
        previousAdapter = NotificationAdapter()

        rvToday.adapter = todayAdapter
        rvPrevious.adapter = previousAdapter

        loadNotifications()

        todayAdapter.submitList(todayNotifications)
        previousAdapter.submitList(previousNotifications)

        if (todayNotifications.isEmpty()) {
            tvNoNotifications.visibility = View.VISIBLE
            rvToday.visibility = View.GONE
        } else {
            tvNoNotifications.visibility = View.GONE
            rvToday.visibility = View.VISIBLE
        }

        if (previousNotifications.isNotEmpty()) {
            btnTogglePrevious.visibility = View.VISIBLE
            previousSection.visibility = View.GONE
        } else {
            btnTogglePrevious.visibility = View.GONE
            previousSection.visibility = View.GONE
        }

        if (todayNotifications.isEmpty() && previousNotifications.isEmpty()) {
            btnClearNotifications.visibility = View.GONE
        } else {
            btnClearNotifications.visibility = View.VISIBLE
        }

        btnTogglePrevious.setOnClickListener {
            isPreviousVisible = !isPreviousVisible
            previousSection.visibility = if (isPreviousVisible) View.VISIBLE else View.GONE
            btnTogglePrevious.text =
                if (isPreviousVisible) "Hide Previous Notifications" else "Show Previous Notifications"
        }
    }

    private fun loadNotifications() {
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(notificationHistoryKey, "[]")
        val gson = Gson()
        val type = object : TypeToken<List<HydrationReminderWorker.NotificationEntry>>() {}.type
        val allNotifications: List<HydrationReminderWorker.NotificationEntry> = gson.fromJson(json, type) ?: emptyList()

        todayNotifications = allNotifications.filter { isToday(it.timestamp) }.reversed()
        previousNotifications = allNotifications.filter { !isToday(it.timestamp) }.reversed()
    }

    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }

        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Notifications")
            .setMessage("Are you sure you want to clear all notification history? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                clearNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearNotifications() {
        // Disable button to prevent multiple taps
        btnClearNotifications.isEnabled = false

        // Clear local SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(notificationHistoryKey).apply()

        // Clear Firebase Realtime Database (adjust path as per your app)
        val userId = getCurrentUserId()
        if (userId != null) {
            val databaseRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)
                .child("notificationHistory")

            databaseRef.removeValue().addOnCompleteListener { task ->
                // Re-enable button
                btnClearNotifications.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Notification history cleared", Toast.LENGTH_SHORT).show()
                    // Clear UI lists and refresh
                    todayNotifications = emptyList()
                    previousNotifications = emptyList()
                    todayAdapter.submitList(todayNotifications)
                    previousAdapter.submitList(previousNotifications)
                    tvNoNotifications.visibility = View.VISIBLE
                    rvToday.visibility = View.GONE
                    btnTogglePrevious.visibility = View.GONE
                    previousSection.visibility = View.GONE
                } else {
                    Toast.makeText(requireContext(), "Failed to clear notifications from server", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // If no user ID, just clear local and UI
            btnClearNotifications.isEnabled = true
            Toast.makeText(requireContext(), "Notification history cleared locally", Toast.LENGTH_SHORT).show()
            todayNotifications = emptyList()
            previousNotifications = emptyList()
            todayAdapter.submitList(todayNotifications)
            previousAdapter.submitList(previousNotifications)
            tvNoNotifications.visibility = View.VISIBLE
            rvToday.visibility = View.GONE
            btnTogglePrevious.visibility = View.GONE
            previousSection.visibility = View.GONE
        }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}
