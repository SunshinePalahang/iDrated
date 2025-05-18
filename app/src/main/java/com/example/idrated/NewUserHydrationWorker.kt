package com.example.idrated

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NewUserHydrationWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val lastSavedTime = sharedPreferences.getLong("lastSavedTime", 0L)
        val now = System.currentTimeMillis()

        // If no intake within 2 hours
        if (now - lastSavedTime >= 2 * 60 * 60 * 1000) {
            showHydrationNotification()
        }

        return Result.success()
    }

    private fun showHydrationNotification() {
        val channelId = "hydration_reminder_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Hydration Reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 100, 500)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.bell2)
            .setContentTitle("⏰ Time to Hydrate")
            .setContentText("Hey there! You've been registered for 2 hours. Time to drink water?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}
