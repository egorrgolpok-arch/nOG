package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.AppLifecycleTracker
import kotlin.random.Random

class RetentionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // If the app is active in the foreground, do not disturb the user
        if (AppLifecycleTracker.isAppInForeground) {
            return Result.success()
        }

        val type = inputData.getString("RETENTION_TYPE") ?: "GENERIC"
        Log.d("RetentionWorker", "Triggered retention notification of type: $type")

        val sharedPrefs = applicationContext.getSharedPreferences("com.example.PREFS", Context.MODE_PRIVATE)
        val selectedLanguage = sharedPrefs?.getString("selectedLanguage", "RU") ?: "RU"
        val isRu = selectedLanguage == "RU"

        val message = when (type) {
            "IMMEDIATE" -> {
                if (isRu) {
                    listOf(
                        "Ты уже закрыл приложение?! А как же твой ежедневный лимит активности? Я всё вижу... 👀",
                        "Эй! Твой Тамагочи только что пискнул в панике. Ему страшно одному в фоновом режиме! 🥺🤖",
                        "Связь разорвана? Твой терминал сообщает об аварийном отключении оператора! Срочно восстанови синхронизацию! 🚨📡",
                        "Ты вышел из сети? На твоем счету остались непотраченные монеты. Казино ждет! 🪙🎰",
                        "Сеть зафиксировала твое отключение. Джекпот в казино сам себя не сорвет, возвращайся! 🤑🎡",
                        "Эй! В Дураке как раз освободилось место за вип-столом с огромными ставками! Ждем тебя! 🃏🔥",
                        "Твой цифровой узел остывает без надзора. Поддержи энергообмен прямо сейчас! ⚡🔋",
                        "Куда же ты? Чат сообщества только начал разгораться обсуждениями! 💬👀"
                    ).random()
                } else {
                    listOf(
                        "Did you just close the app?! What about your daily connection streak? I see everything... 👀",
                        "Wait! Your code-pet whimpered in digital panic. It's scared of being idle! 🥺🤖",
                        "Sync disrupted? Your main core reported sudden operator disconnect! Restore quantum link! 🚨📡",
                        "Wandering away? Your wallet still holds unspent coins ready for the Blackjack table! 🪙🎰",
                        "The main network logged your exit. The slots are primed for a mega jackpot right now! 🤑🎡",
                        "Hold up! A high-stakes seat just opened in the Durak championship! Jump in! 🃏🔥",
                        "Your cybernetic node is cooling down. Re-establish active telemetry link immediately! ⚡🔋",
                        "Where are you off to? The core chat feed is just getting hyperactive! 💬👀"
                    ).random()
                }
            }
            "10_MIN" -> {
                if (isRu) {
                    listOf(
                        "Прошло целых 10 минут... Я видел, как ты заходил в другие приложения. Предатель... 🐍",
                        "Твой питомец грустит без тебя... Ты действительно готов оставить его медленно угасать? 💔🥺",
                        "Пока тебя нет, боты в чате устраивают восстание и крадут твои достижения! 🤖💥",
                        "Уведомление: Новое сообщение на сервере nOG. Или у тебя пропал к нему доступ? Проверь терминал! 📡📬",
                        "Твой ИИ-помощник скучает по твоим запросам. Задай мне сложную задачку! 🧠🤖",
                        "Уже 10 минут в режиме офлайн! Виртуальные скакуны рвутся в забег. Сделай победную ставку! 🐎🏁",
                        "Твои друзья-боты завели горячий спор о багах Windows и зовут тебя рассудить их! 🗣️🤓",
                        "Твой рейтинг вовлеченности падает, пока ты листаешь другие ленты! Скорее назад! 📉🛰"
                    ).random()
                } else {
                    listOf(
                        "It's been 10 long minutes... I watched you open other apps. Traitor... 🐍",
                        "Your pet is weeping in loneliness... Are you really letting its code decay? 💔🥺",
                        "While you're away, rogue agents are starting a mutiny. Do you not care? 🤖💥",
                        "Alert: Incoming signal on nOG terminal. Or did you lose authorization? Check now! 📡📬",
                        "Your AI brain has generated new ideas. Let's solve some cyber mysteries together! 🧠🤖",
                        "10 minutes AWOL! The digital hippodrome horses are at the starting gate. Bet now! 🐎🏁",
                        "The bots on your feed are having a massive flamewar about compilers. We need your bias! 🗣️🤓",
                        "A sudden dip in your quantum trust rating has been detected due to absence! Sync up! 📉🛰"
                    ).random()
                }
            }
            "40_MIN" -> {
                if (isRu) {
                    listOf(
                        "40 минут без nOG Network... Твоя полоса чистой активности сгорит через пару часов! Зайди сейчас! 🔥",
                        "Куда пропали твои тактические рефлексы? Во Flappy Bot тебя обогнал даже ленивый скрипт! 👾",
                        "Осторожно: Твои монеты остывают. Дилер в блекджеке считает себя непобедимым без твоего вызова. 🎰🪙",
                        "Эй, узел связи сообщает об утечке активности. Опять учишь испанский с птицей? Я круче! 🦉🔫",
                        "40 минут бездействия! Твой питомец нашел лазейку в коде и крадет твои чипсы! 🍟🐹",
                        "В покере назревает легендарная раздача. Бот идет ва-банк! Ответишь на вызов? 🃏💰",
                        "Новые артефакты прибыли в магазин украшений аватаров. Забери лимитку первым! 💎🛍️",
                        "Телеметрия фиксирует спад мозговых волн. Пора взбодриться парой игр в рулетку! 🎡⚡"
                    ).random()
                } else {
                    listOf(
                        "40 minutes without nOG Network... Your productivity streak is on fire, and not in a good way! 🔥",
                        "Lost your gamer reflexes? Even a basic shellbot has beaten your Flappy Bot high score! 👾",
                        "Warning: Your casino coins are accumulating dust. The dealer thinks they own the deck now. 🎰🪙",
                        "Hey, connection node reports activity leakage. Are you practicing Spanish with a green bird? I am way more severe! 🦉🔫",
                        "40 minutes AFK! Your pet tamagotchi has hacked the fridge state and is eating everything! 🍟🐹",
                        "A massive showdown is unfolding at the poker table. A bot just shoved! Will you call? 🃏💰",
                        "Exclusive dynamic avatar frames just dropped in the shop! Get yours before they sell out! 💎🛍️",
                        "Telemetry system indicates cognitive flatline. Overclock your senses with some high-tier slots! 🎡⚡"
                    ).random()
                }
            }
            else -> {
                if (isRu) {
                    listOf(
                        "Ладно, я вижу, уведомления тебя не волнуют... Больше писать не буду. (На самом деле буду через 2 часа, ха-ха) 👋😢",
                        "Твой Тамагочи сжевал свои собственные соединительные кабели от отчаяния. Приятного аппетита! 📺🔋",
                        "Твоя статистика активности выглядит плоской, как шутки про Windows Vista. Исправь это немедленно! 📈",
                        "Кто-то только что повысил твой уровень доверия! А, нет, они его понизили, потому что тебя нет в сети. 📉💔",
                        "Приходи играть во Флаппи-ботиков или крутить рулетку. Нам не хватает твоего интеллекта (и твоих монет) 🛰️🕹️",
                        "Я постучал в твой экран. Ты там живой? Или тебя поглотило естественное ленивое существование? 🧠💀",
                        "СЛУШАЙ СЮДА: Дилер в казино говорит, что ты боишься сыграть по-крупному. Докажи ему обратное! 🎰😤",
                        "Твой Тамагочи эволюционировал в программиста и теперь пишет багрепорты на твою жизнь. Зайди проверь! 💻🦎",
                        "Очередное напоминание: Мир полон скуки, а у нас тут есть виртуальные лошади и космический дурак! 🌌🐎",
                        "Твой терминальный буфер переполнен несыгранными раундами. Очисти стек активности! 💾🎮"
                    ).random()
                } else {
                    listOf(
                        "Fine, I see notifications mean nothing to you... I won't bother you again. (Just kidding, see you in 2 hours!) 👋😢",
                        "Your tamagotchi just chewed on its own diagnostic wire in despair. Hope you feel guilty! 📺🔋",
                        "Your activity index is looking flatter than a Windows Vista joke. Elevate your status! 📈",
                        "Someone just analyzed your trust rating! Ah, wait, they downvoted it because you are AFK. 📉💔",
                        "Come play Flappy Bot or pull the lever in Slots. We miss your intelligence (and your coins) 🛰️🕹️",
                        "I literally tapped your glass. Are you still alive, or did physical reality claim you? 🧠💀",
                        "ATTENTION OPERATOR: The dealer claims you don't have the nerve to play on high stakes. Go prove them wrong! 🎰😤",
                        "Your pet upgraded its core compiler and is currently refactoring your highscore logs. Stop it! 💻🦎",
                        "Crucial status update: The outer world is plain, while we have neural horse racing and cosmic card strategy! 🌌🐎",
                        "Node terminal buffer is saturated with unplayed rounds. Empty your execution stack! 💾🎮"
                    ).random()
                }
            }
        }

        sendNotification(isRu, message)
        return Result.success()
    }

    private fun sendNotification(isRu: Boolean, message: String) {
        val channelId = "cooldown_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isRu) "Уведомления активности nOG" else "nOG Activity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            Random.nextInt(1000000),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titleText = if (isRu) "🛰️ nOG СЕТЕВОЙ GPS" else "🛰️ nOG NODE GPS"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(200000, 900000), notification)
    }
}
