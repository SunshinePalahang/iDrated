package com.example.idrated

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HydrationReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour !in 6..23) return Result.success()

        val user = auth.currentUser ?: return Result.success()
        val uid = user.uid
        val userRef = database.child("users").child(uid)

        val latch = CountDownLatch(1)

        var lastSavedTime = 0L
        var lastNotificationTimeFirebase = 0L
        var waterConsumed = 0
        var waterGoal = 0

        userRef.get().addOnSuccessListener { snapshot ->
            lastSavedTime = snapshot.child("lastSavedTime").getValue(Long::class.java) ?: 0L
            lastNotificationTimeFirebase = snapshot.child("lastNotificationTime").getValue(Long::class.java) ?: 0L
            waterConsumed = snapshot.child("waterConsumed").getValue(Int::class.java) ?: 0
            waterGoal = snapshot.child("waterGoal").getValue(Int::class.java) ?: 0
            latch.countDown()
        }.addOnFailureListener { latch.countDown() }

        latch.await(5, TimeUnit.SECONDS)

        val now = System.currentTimeMillis()
        val lastNotificationTimeLocal = sharedPreferences.getLong("lastNotificationTime", 0L)
        val lastNotificationTime = maxOf(lastNotificationTimeFirebase, lastNotificationTimeLocal)

        val timeSinceLastDrink = now - lastSavedTime
        val timeSinceLastNotification = now - lastNotificationTime

        val hasStartedDrinkingToday = isToday(lastSavedTime) && lastSavedTime != 0L
        val shouldNotify = (timeSinceLastDrink >= 15 * 60 * 1000) && (timeSinceLastNotification >= 15 * 60 * 1000)

        if (shouldNotify) {
            val message = selectNotificationMessage(isFollowUp = hasStartedDrinkingToday)
            showHydrationNotification(message)
            saveNotificationToHistory(message)

            userRef.child("lastNotificationTime").setValue(now)
        }

        // ✅ Goal Achieved Logic
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastGoalAchievedDate = sharedPreferences.getString("goalAchievedDate", "")

        if (waterConsumed >= waterGoal && lastGoalAchievedDate != today && waterGoal > 0) {
            showGoalAchievedNotification()
            saveNotificationToHistory("🎉 You achieved your hydration goal today!")
            sharedPreferences.edit().putString("goalAchievedDate", today).apply()
        }

        return Result.success()
    }

    private fun selectNotificationMessage(isFollowUp: Boolean): String {
        val initialMessages = listOf(
            "💧 Time to hydrate! Drink water now!",
            "💧 Drink some water now for better focus!",
            "🌞 Stay fresh—hydrate regularly!",
            "🚰 A new day, a new chance to stay hydrated. Drink water now!",
            "🫗 Your first drink of the day matters—keep your body fueled!",
            "💦 Begin strong. Drink water and fuel your body!",
            "🌅 Morning boost: water is the best way to start your day!",
            "👋 Hey there! Don’t forget your water—it’s your best habit.",
            "📅 New day, same goal: stay hydrated! Drink up.",
            "🧠 Clear mind starts with a hydrated body. Drink water now.",
            "☀️ Rise and hydrate—your body’s been waiting all night!",
            "🔥 Fuel your day with hydration. Drink water now!"
        )

        val followUpMessages = listOf(
            "🥤 Keep going! Your body still needs water—go ahead and drink up!",
            "💦 Don’t forget to drink more for your health. Take a sip now!",
            "💧 Hydrate to activate! Your body will thank you. Drink some water!",
            "💧 Time to hydrate! Grab your bottle and take a drink.",
            "💧 Drink some water now for better focus! Just a few sips will do wonders.",
            "🌞 Stay fresh—hydrate regularly. Drink water now!",
            "😅 Hey, looks like you haven’t had water in a while! That’s not good for your body. Drink now!",
            "🚨 Seems like you’ve been skipping your water breaks. Take one now—your body needs it!",
            "🧠 Losing focus? You might just be thirsty. Grab a drink!",
            "👀 Your body’s giving signs—it wants water. Listen to it and drink up!",
            "💡 Feeling off? A sip of water might be all you need. Drink now!",
            "📢 A quick hydration check: when was your last drink? Take one now!",
            "😰 Don’t make your body work harder. Help it out—drink some water!",
            "🍃 Time for a water break! You’ll feel better after a drink.",
            "👋 It’s been a bit! Keep the momentum going and drink your water.",
            "🫗 Even your houseplants get watered regularly. So should you. Drink up!"
        )

        return if (isFollowUp) followUpMessages.random() else initialMessages.random()
    }

    private fun showHydrationNotification(message: String) {
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
    }

    private fun showGoalAchievedNotification() {
        val channelId = "hydration_goal_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hydration Goal Achieved",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("🎉 Goal Achieved!")
            .setContentText("You’ve reached your hydration goal for today. Great job!")
            .setSmallIcon(R.drawable.water_drop)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1002, notification)
    }

    private fun saveNotificationToHistory(message: String) {
        val json = sharedPreferences.getString("notificationHistory", "[]")
        val type = object : TypeToken<MutableList<NotificationEntry>>() {}.type
        val history: MutableList<NotificationEntry> = gson.fromJson(json, type) ?: mutableListOf()

        val now = System.currentTimeMillis()
        val isDuplicate = history.any { isToday(it.timestamp) && it.message == message }

        if (isDuplicate) return // Prevent duplicate message for today

        history.add(NotificationEntry(now, message))

        val todayOnly = history.filter { isToday(it.timestamp) }

        sharedPreferences.edit().apply {
            putString("notificationHistory", gson.toJson(todayOnly))
            putLong("lastNotificationTime", now)
            apply()
        }
    }


    private fun isToday(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance()
        then.timeInMillis = timestamp
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    }

    data class NotificationEntry(
        val timestamp: Long,
        val message: String
    )
}
