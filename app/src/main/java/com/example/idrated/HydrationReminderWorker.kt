package com.example.idrated

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HydrationReminderWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun doWork(): Result {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Only run between 6 AM and 11 PM
        if (currentHour !in 6..23) return Result.success()

        val user = auth.currentUser ?: return Result.success()
        val uid = user.uid
        val userRef = database.child("users").child(uid)

        // Use CountDownLatch to wait for async Firebase calls
        val latch = CountDownLatch(1)

        var lastSavedTime = 0L
        var lastNotificationTimeFirebase = 0L
        var notificationHistoryFirebaseJson = "[]"

        // Fetch data from Firebase: lastSavedTime, lastNotificationTime, notificationHistory
        userRef.get().addOnSuccessListener { snapshot ->
            lastSavedTime = snapshot.child("lastSavedTime").getValue(Long::class.java) ?: 0L
            lastNotificationTimeFirebase = snapshot.child("lastNotificationTime").getValue(Long::class.java) ?: 0L
            notificationHistoryFirebaseJson = snapshot.child("notificationHistory").getValue(String::class.java) ?: "[]"
            latch.countDown()
        }.addOnFailureListener {
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)

        val now = System.currentTimeMillis()
        // Fallback to local lastNotificationTime if Firebase data missing or older
        val lastNotificationTimeLocal = sharedPreferences.getLong("lastNotificationTime", 0L)
        val lastNotificationTime = maxOf(lastNotificationTimeFirebase, lastNotificationTimeLocal)

        val timeSinceLastDrink = now - lastSavedTime
        val timeSinceLastNotification = now - lastNotificationTime

        val shouldNotify = (timeSinceLastDrink >= 15 * 60 * 1000) && (timeSinceLastNotification >= 15 * 60 * 1000)
        if (shouldNotify) {
            showHydrationNotification(lastNotificationTime != 0L)
            // Save notification time and history synced
            saveLastNotificationTimeAndHistoryToFirebase()
        }

        return Result.success()
    }

    // Data class for notification entry
    data class NotificationEntry(
        val timestamp: Long,
        val message: String
    )

    private fun saveLastNotificationTimeAndHistoryToFirebase() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val userRef = database.child("users").child(uid)
        val now = System.currentTimeMillis()

        // Load already-saved local notification history
        val localJson = sharedPreferences.getString("notificationHistory", "[]")
        val type = object : TypeToken<MutableList<NotificationEntry>>() {}.type
        val localNotifications: MutableList<NotificationEntry> = gson.fromJson(localJson, type) ?: mutableListOf()

        val notificationsJson = gson.toJson(localNotifications)

        // Update Firebase in one shot: lastNotificationTime and notificationHistory
        val updates = mapOf<String, Any>(
            "lastNotificationTime" to now,
            "notificationHistory" to notificationsJson
        )
        userRef.updateChildren(updates)

        // Also update local SharedPreferences time
        sharedPreferences.edit()
            .putLong("lastNotificationTime", now)
            .apply()
    }

    private fun showHydrationNotification(isFollowUp: Boolean) {
        val initialMessages = listOf(
            "💧 Time to hydrate! Drink now.",
            "💧 Drink some water now for better focus!",
            "🌞 Stay fresh—hydrate regularly!"
        )

        val followUpMessages = listOf(
            "🥤 Keep going! Your body still needs water.",
            "💦 Don’t forget to drink more for your health!",
            "💧 Hydrate to activate! Your body will thank you."
        )

        val message = if (isFollowUp) followUpMessages.random() else initialMessages.random()

        val channelId = "hydration_reminder_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 100, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("⏰ Hydration Reminder")
            .setContentText(message)
            .setSmallIcon(R.drawable.bell2)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)

        // Save notification in local SharedPreferences immediately so saveLastNotificationTimeAndHistoryToFirebase can read it
        saveNotificationEntry(message)
    }

    private fun saveNotificationEntry(message: String) {
        val json = sharedPreferences.getString("notificationHistory", "[]")
        val type = object : TypeToken<MutableList<NotificationEntry>>() {}.type
        val notifications: MutableList<NotificationEntry> = gson.fromJson(json, type) ?: mutableListOf()

        notifications.add(NotificationEntry(System.currentTimeMillis(), message))

        sharedPreferences.edit().putString("notificationHistory", gson.toJson(notifications)).apply()
    }
}
