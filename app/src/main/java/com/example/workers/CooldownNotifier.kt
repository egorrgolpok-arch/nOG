package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.example.ui.screens.TamagotchiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CooldownNotifier {
    suspend fun checkAndNotifyAllCooldowns(context: Context) = withContext(Dispatchers.IO) {
        val generalPrefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val isRu = generalPrefs.getString("selected_lang", "RU") == "RU"
        
        try {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "nog_social_database"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
            
            val socialDao = db.socialDao()
            val user = socialDao.getUserById("user")
            val isVerified = user?.isVerified == true
            
            // 1. Incubator cooldown check
            val tamaState = TamagotchiManager.loadState(context)
            val tamaPrefs = context.getSharedPreferences("nog_tamagotchi_prefs3", Context.MODE_PRIVATE)
            val notifiedIncubator = tamaPrefs.getBoolean("notified_incubator", false)
            if (!tamaState.hasPet && tamaState.cooldownUntil > 0L) {
                if (System.currentTimeMillis() >= tamaState.cooldownUntil) {
                    if (!notifiedIncubator) {
                        sendPushAndDatabase(
                            context = context,
                            db = db,
                            title = if (isRu) "Инкубатор готов! ⚡" else "Incubator ready! ⚡",
                            message = if (isRu) "Ограничение на запуск нового питомца истекло. Начните инкубацию!" else "The incubator cooldown has completed. You can start a new pet!"
                        )
                        tamaPrefs.edit().putBoolean("notified_incubator", true).apply()
                    }
                } else {
                    if (notifiedIncubator) {
                        tamaPrefs.edit().putBoolean("notified_incubator", false).apply()
                    }
                }
            }
            
            // 2. Feeding cooldown check
            if (tamaState.hasPet && !tamaState.isDead && tamaState.lastFedTime > 0L) {
                val notifiedFeed = tamaPrefs.getBoolean("notified_feed", false)
                val elapsedFeed = System.currentTimeMillis() - tamaState.lastFedTime
                val cooldownFeedMs = if (isVerified) 1800L * 1000L else 3600L * 1000L
                if (elapsedFeed >= cooldownFeedMs) {
                    if (!notifiedFeed) {
                        sendPushAndDatabase(
                            context = context,
                            db = db,
                            title = if (isRu) "Пора покормить! 🍽️" else "Feeding time! 🍽️",
                            message = if (isRu) "Ваш питомец ${tamaState.petName} снова проголодался и готов к приему пищи." else "Your pet ${tamaState.petName} is ready to be fed again."
                        )
                        tamaPrefs.edit().putBoolean("notified_feed", true).apply()
                    }
                } else {
                    if (notifiedFeed) {
                        tamaPrefs.edit().putBoolean("notified_feed", false).apply()
                    }
                }
            }
            
            // 3. Flappy Bot cooldown check
            val flappyPrefs = context.getSharedPreferences("nog_flappy_prefs", Context.MODE_PRIVATE)
            val lastGameOverTime = flappyPrefs.getLong("flappy_last_game_over", 0L)
            if (!isVerified && lastGameOverTime > 0L) {
                val notifiedFlappy = flappyPrefs.getBoolean("notified_flappy", false)
                val elapsedFlappy = System.currentTimeMillis() - lastGameOverTime
                val cooldownMs = 5 * 60 * 1000L
                if (elapsedFlappy >= cooldownMs) {
                    if (!notifiedFlappy) {
                        sendPushAndDatabase(
                            context = context,
                            db = db,
                            title = if (isRu) "Flappy Bot готов! 🎮" else "Flappy Bot is ready! 🎮",
                            message = if (isRu) "5 минут истекло. Запускайте Flappy Bot и бейте рекорды!" else "5-minute cooldown is over. Launch Flappy Bot and set your new high scores!"
                        )
                        flappyPrefs.edit().putBoolean("notified_flappy", true).apply()
                    }
                } else {
                    if (notifiedFlappy) {
                        flappyPrefs.edit().putBoolean("notified_flappy", false).apply()
                    }
                }
            }
        } catch (e: Exception) {
            // Safe block to handle SQLite/Preference interactions gracefully
        }
    }
    
    private suspend fun sendPushAndDatabase(context: Context, db: AppDatabase, title: String, message: String) {
        db.socialDao().insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                type = "SYSTEM"
            )
        )
        
        val channelId = "cooldown_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Cooldown Status Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(title.hashCode(), notification)
    }
}
