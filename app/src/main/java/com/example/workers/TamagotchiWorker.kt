package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.example.ui.screens.TamagotchiManager
import com.example.ui.screens.updateTamaStats
import androidx.room.Room

class TamagotchiWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("nog_tamagotchi_prefs3", Context.MODE_PRIVATE)
        val isRu = prefs.getString("lang", "EN") == "RU" // Though we don't save lang here directly, let's just make it EN/RU by default or infer

        val state = TamagotchiManager.loadState(applicationContext)
        if (!state.hasPet || state.isDead) return Result.success()

        val tickNow = System.currentTimeMillis()
        val deltaMs = tickNow - state.lastTickTime
        
        // Background tick, so isAppActive = false
        val newState = updateTamaStats(state, deltaMs, false)
        TamagotchiManager.saveState(applicationContext, newState)

        // Threshold checks for notifications
        if (newState.isDead && !state.isDead) { // Just died
            val title = "Pet Deceased"
            val text = "Your pet ${newState.petName} has passed away due to ${newState.deathReason}."
            sendPushAndInApp(title, text)
        } else if (newState.isSick && !state.isSick) { // Just got sick
            val title = "Pet is Sick!"
            val text = "Your pet ${newState.petName} needs medical attention."
            sendPushAndInApp(title, text)
        } else if (newState.hunger < 20f && state.hunger >= 20f) {
            val title = "Pet is Hungry!"
            val text = "Feed ${newState.petName} before it starves."
            sendPushAndInApp(title, text)
        } else if (newState.hygiene < 20f && state.hygiene >= 20f) {
            val title = "Pet is Dirty!"
            val text = "${newState.petName} needs to be washed."
            sendPushAndInApp(title, text)
        } else if (newState.mood < 20f && state.mood >= 20f) {
            val title = "Pet is Depressed!"
            val text = "Spend some time with ${newState.petName}."
            sendPushAndInApp(title, text)
        }

        return Result.success()
    }

    private suspend fun sendPushAndInApp(title: String, text: String) {
        // Push notification
        val channelId = "tamagotchi_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pet Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // In-app Notification
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "nog_social_database"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        
        db.socialDao().insertNotification(
            NotificationEntity(
                title = "Tamagotchi System",
                message = text,
                type = "SYSTEM"
            )
        )
    }
}
