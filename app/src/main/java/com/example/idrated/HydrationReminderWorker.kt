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

    override fun doWork(): Result {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Only run between 6 AM and 11 PM
        if (currentHour !in 6..23) return Result.success()

        val user = auth.currentUser ?: return Result.success()
        val uid = user.uid
        val userRef = database.child("users").child(uid)
        val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val latch = CountDownLatch(1)
        var shouldNotify = false
        var lastSavedTime = 0L

        // Fetch lastSavedTime from Firebase Realtime Database
        userRef.child("lastSavedTime").get().addOnSuccessListener { snapshot ->
            lastSavedTime = snapshot.getValue(Long::class.java) ?: 0L
            latch.countDown()
        }.addOnFailureListener {
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)

        val now = System.currentTimeMillis()
        val lastNotificationTime = sharedPreferences.getLong("lastNotificationTime", 0L)

        val timeSinceLastDrink = now - lastSavedTime
        val timeSinceLastNotification = now - lastNotificationTime

        // Notify if 15 minutes passed since last drink
        // and at least 15 minutes since last notification
        if (timeSinceLastDrink >= 15 * 60 * 1000 && timeSinceLastNotification >= 15 * 60 * 1000) {
            shouldNotify = true
        }

        if (shouldNotify) {
            showHydrationNotification(timeSinceLastNotification > 15 * 60 * 1000)
            // Removed duplicate saveLastNotificationTime() here, already called inside showHydrationNotification()
        }

        return Result.success()
    }

    // Data class for each notification entry
    data class NotificationEntry(val timestamp: Long, val message: String)

    private fun saveNotificationEntry(message: String) {
        val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("notificationHistory", "[]")
        val type = object : TypeToken<MutableList<NotificationEntry>>() {}.type
        val notifications: MutableList<NotificationEntry> = gson.fromJson(json, type) ?: mutableListOf()

        notifications.add(NotificationEntry(System.currentTimeMillis(), message))

        sharedPreferences.edit().putString("notificationHistory", gson.toJson(notifications)).apply()
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

        saveLastNotificationTime()
        saveNotificationEntry(message)
    }

    private fun saveLastNotificationTime() {
        val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastNotificationTime", System.currentTimeMillis()).apply()
    }
}
