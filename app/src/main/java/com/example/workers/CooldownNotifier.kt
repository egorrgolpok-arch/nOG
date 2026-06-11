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
                            title = if (isRu) "🧬 ИНКУБАТОР ЖДЕТ ГЕНОВ ГИГАЧАДА!" else "🧬 INCUBATOR BREATHES LIFE!",
                            message = if (isRu) "Кулдаун на синтез новой жизни спал. Заходи выращивать альфа-доминанта!" else "Cooldown on synthetic bio-fusion completed. Synthesize your next unit right now!"
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
                            title = if (isRu) "🍽️ КОРМЛЕНИЕ ПИТОНА ПО ПЛАНУ!" else "🍽️ FUEL RESERVES CRITICAL!",
                            message = if (isRu) "Калорийность ${tamaState.petName} упала ниже плинтуса. Занеси пайку своему мелкому сожителю!" else "The energy grid of ${tamaState.petName} is failing! Inject nutritional payloads immediately!"
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
                            title = if (isRu) "🎮 БОТ РЖЕТ НАД ТВОИМ РЕКОРДОМ!" else "🎮 BOT IS MOCKING YOUR HIGHSCORE!",
                            message = if (isRu) "Кулдаун Flappy Bot прошел. Заходи доказать этой груде металла, кто здесь батя!" else "Flappy engine cooled down. Connect to prove this scrap metal frame who holds the root access!"
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
