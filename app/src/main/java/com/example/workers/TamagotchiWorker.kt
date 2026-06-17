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
        val generalPrefs = applicationContext.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val isRu = generalPrefs.getString("selected_lang", "RU") == "RU"

        val state = TamagotchiManager.loadState(applicationContext)
        if (!state.hasPet || state.isDead) return Result.success()

        val tickNow = System.currentTimeMillis()
        val deltaMs = tickNow - state.lastTickTime
        
        // Background tick, so isAppActive = false
        val newState = updateTamaStats(state, deltaMs, false)
        TamagotchiManager.saveState(applicationContext, newState)

        // Threshold checks for notifications
        if (newState.isDead && !state.isDead) { // Just died
            val title = if (isRu) "Питомец ПОГИБ... Или нет?! 💀" else "Your pet PERISHED... Or did it?! 💀"
            val causeRu = when (newState.deathReason) {
                "disease" -> "цифровой чумы (${newState.deathDiseaseName ?: "квантовый вирус"})"
                "neglect" -> "полного игнора и недостатка твоей любви"
                else -> "древней старости динозавра"
            }
            val causeEn = when (newState.deathReason) {
                "disease" -> "digital plague (${newState.deathDiseaseName ?: "cyber infection"})"
                "neglect" -> "absolute neglect and severe lack of your love"
                else -> "becoming a low-poly fossil from old age"
            }
            val text = if (isRu) {
                "Твой питомец ${newState.petName} откинул лапки от $causeRu! Забеги в игру посмотреть на призрака!"
            } else {
                "Your pet ${newState.petName} went offline forever due to $causeEn! Click to witness the spirit!"
            }
            sendPushAndInApp(title, text)
        } else if (newState.isSick && !state.isSick) { // Just got sick
            val title = if (isRu) "🚨 КРИТИЧЕСКАЯ МУТАЦИЯ У ПИТОМЦА!" else "🚨 MONSTER MUTATION DETECTED!"
            val text = if (isRu) {
                "${newState.petName} подцепил квантовую заразу и превращается в крипера. Требуется срочная сыворотка!"
            } else {
                "${newState.petName} caught cyber-measles and is turning into a toxic mutation! Cure asap!"
            }
            sendPushAndInApp(title, text)
        } else if (newState.hunger < 20f && state.hunger >= 20f) {
            val title = if (isRu) "🍽️ ОН ЖРЁТ ТВОЙ БАЛАНС!" else "🍽️ SECURING YOUR COIN BALANCE!"
            val text = if (isRu) {
                "${newState.petName} настолько голоден, что начинает жевать файлы твоего кошелька! Покорми его скорее!"
            } else {
                "${newState.petName} is so starved that it is literally eating your coin database files! Feed it!"
            }
            sendPushAndInApp(title, text)
        } else if (newState.hygiene < 20f && state.hygiene >= 20f) {
            val title = if (isRu) "🧼 Вонь пробивает твой экран!" else "🧼 Smells like burnt silicon!"
            val text = if (isRu) {
                "От ${newState.petName} несет хуже, чем из серверной после майнинга. Время устроить банный день!"
            } else {
                "${newState.petName} smells worse than a dusty bitcoin mining rig. Shower the low-poly beast!"
            }
            sendPushAndInApp(title, text)
        } else if (newState.mood < 20f && state.mood >= 20f) {
            val title = if (isRu) "💔 План побега питомца готов!" else "💔 Escape protocol initiated!"
            val text = if (isRu) {
                "${newState.petName} в глубокой депрессии и пакует чемоданы из-за твоего игнора. Обними беднягу!"
            } else {
                "${newState.petName} has drafted an escape plan due to severe loneliness. Intercept it with a hug!"
            }
            sendPushAndInApp(title, text)
        }

        // Also check and alarm for all active cooldowns during background runs
        CooldownNotifier.checkAndNotifyAllCooldowns(applicationContext)

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
        val db = com.example.data.DatabaseProvider.getDatabase(applicationContext)
        
        db.socialDao().insertNotification(
            NotificationEntity(
                title = "Tamagotchi System",
                message = text,
                type = "SYSTEM"
            )
        )
    }
}
