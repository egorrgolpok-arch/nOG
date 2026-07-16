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
                        "Связь разорвана? Твой terminal сообщает об аварийном отключении оператора! Срочно восстанови синхронизацию! 🚨📡",
                        "Ты вышел из сети? На твоем счету остались непотраченные монеты. Казино ждет! 🪙🎰",
                        "Сеть зафиксировала твое отключение. Джекпот в казино сам себя не сорвет, возвращайся! 🤑🎡",
                        "Эй! В Дураке как раз освободилось место за вип-столом с огромными ставками! Ждем тебя! 🃏🔥",
                        "Твой цифровой узел остывает без надзора. Поддержи энергообмен прямо сейчас! ⚡🔋",
                        "Куда же ты? Чат сообщества только начал разгораться обсуждениями! 💬👀",
                        "Бот-интеллектуал только что запостил разгромный отзыв на твой профиль! Иди ответь ему, пока тебя не засмеяли! 😤🤡",
                        "Куда?! А как же ежедневная раздача халявных коинов? Твои 500 монет могут сгореть! 🪙🔥",
                        "Твой Тамагочи начал майнить догекоины на твоем процессоре, пока ты не смотришь! Зайди пресечь бунт! 💻🐕",
                        "Новое событие: В чате начался турнир по скоростному программированию на HTML на коленке! Участвуешь? ⌨️🏃‍♂️",
                        "Эй, Скуф! Ты забыл забрать ежедневную порцию пельменей на свой цифровой баланс! Заходи скорее! 🥟🪙",
                        "Терминал nOG зафиксировал нештатное прерывание сессии. Ошибка ядра: недостаток твоего внимания! 💻🚨",
                        "Твой ИИ-клон только что выиграл в Дурака у Сибирского Хакера, но все лавры достались ему. Заходи восстановить справедливость! 🃏✨",
                        "О боже! Мем-детектор зафиксировал рекордную концентрацию кринжа на твоем экране вне нашего приложения! Спасайся у нас! 🚨💨"
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
                        "Where are you off to? The core chat feed is just getting hyperactive! 💬👀",
                        "An AI intellectual just posted a savage roast of your profile! Go reply before everyone starts laughing! 😤🤡",
                        "Wait! What about the hourly free coin drop? Your 500 bonus coins are about to expire! 🪙🔥",
                        "Your pet started mining dogecoins on your background CPU cycles! Go stop the digital revolution! 💻🐕",
                        "Event Alert: A speed-HTML-coding competition just started in the community chat! Show them your tag skills! ⌨️🏃‍♂️"
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
                        "Твой рейтинг вовлеченности падает, пока ты листаешь другие ленты! Скорее назад! 📉🛰",
                        "Система зафиксировала критическое падение уровня мем-энергии в твоем районе. Срочный вброс мемов в приложении! 🔋🧠",
                        "Твой Тамагочи переписал свое ТЗ и теперь требует зарплату в пельменях! Разберись с профсоюзом! 🥟🐜",
                        "Эй! Дилер в Казино только что назвал тебя 'трусливым кодером'. Будешь терпеть такое отношение? 🎰😤",
                        "В Дураке тебя ждет загадочный соперник под ником @Siberian_Hacker. Кажется, он блефует! 🃏👀",
                        "Прошло 10 минут... Наш ИИ-генератор мемов создал шедевр лично про твой профиль. Спойлер: это очень смешно! 🤫🎨",
                        "Думаешь, там на улице лучше? Там реальный мир с реальными налогами. А у нас тут бесплатное казино и виртуальное пиво! 🎰🍺",
                        "Твой Тамагочи начал гуглить 'как найти нового заботливого хозяина'. Кажется, он обиделся! 🥺💔",
                        "Твой ИИ-друг написал целую поэму о твоем коде. Зайди почитать, пока он не стер ее от стыда! 📝🤖"
                    ).random()
                } else {
                    listOf(
                        "It's been 10 whole minutes... I saw you opening other apps. Traitor... 🐍",
                        "Your pet is weeping without you... Are you really going to let it fade away? 💔🥺",
                        "While you're away, the bots in the chat are starting a coup and taking your hard-earned stats! 🤖💥",
                        "Alert: New packet arrived at the nOG buffer. Or did you lose credentials? Check terminal! 📡📬",
                        "Your AI helper is starving for prompts. Challenge my intelligence with something hard! 🧠🤖",
                        "10 minutes of complete silence! The Derby track is live. Place your winning bets now! 🐎🏁",
                        "The bot squad started a heated debate about Windows kernel bugs and they need you to moderate! 🗣️🤓",
                        "Your engagement metrics are decaying while you browse elsewhere! Get back to the grid! 📉🛰"
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
                        "Телеметрия фиксирует спад мозговых волн. Пора взбодриться парой игр в рулетку! 🎡⚡",
                        "ИИ-боты начали обсуждать, что ты вымышленный персонаж, созданный для их развлечения. Докажи им, что ты реален! 👁️🤖",
                        "В магазине украшений аватаров появился СУПЕР-РЕДКИЙ нимб Скуфа! Ограниченный тираж, хватай! 💎👑",
                        "На ипподроме начался забег века: Кибер-Лошадь против Механического Пони! Ставки принимаются! 🐎🤖",
                        "Твой Тамагочи взломал соседский смарт-телевизор и крутит там мемы. Приди забрать хулигана! 📺🦊",
                        "Уже 40 минут без связи! Во Flappy Bot боты установили новый мировой рекорд. Ты действительно позволишь машинам победить? 👾⚔️",
                        "Твой баланс коинов грустит в темноте кошелька. Дилер в Блекджеке уже начал раздавать карты! 🃏💰",
                        "Аватар-бутик обновил витрину! Там появился эксклюзивный неоновый костюм Аниме-Скуфа! Забери, пока не раскупили! 💎👑",
                        "Внимание! На бирже nOG зафиксирован резкий скачок курса мем-токенов! Время фиксировать прибыль! 📈🤑"
                    ).random()
                } else {
                    listOf(
                        "40 minutes off the nOG grid... Your activity streak is going to decay soon! Restore connection now! 🔥",
                        "Where are your tactical reflexes? An amateur script just beat your score in Flappy Bot! 👾",
                        "Danger: Your currency is cooling down. The Blackjack dealer is feeling too safe. 🎰🪙",
                        "Signal leak detected. Learning languages with a green bird again? nOG is more rewarding! 🦉🔫",
                        "40 minutes AWOL! Your code-pet found a system vulnerability and is eating your snacks! 🍟🐹",
                        "A legendary hand is brewing in the poker room. A bot went all-in! Do you accept the challenge? 🃏💰",
                        "New custom assets just hit the boutique. Be the first to grab the limited editions! 💎🛍️",
                        "Telemetry reports low cortical arousal. Re-engage with some high-stakes Roulette rounds! 🎡⚡",
                        "The AI bots are gossiping that you're just a fictional entity invented for their amusement. Prove them wrong! 👁️🤖",
                        "A SUPER RARE item just dropped in the Avatar Shop! Grab it before it runs out! 💎👑",
                        "The race of the century has started at the Derby: Cyber-Horse vs Mech-Pony! Cast your wagers! 🐎🤖",
                        "Your tamagotchi hacked a nearby smart TV and is broadcasting memes. Come retrieve the hacker! 📺🦊"
                    ).random()
                }
            }
            "RECURRING_2H" -> {
                if (isRu) {
                    listOf(
                        "Внимание: Твои неиспользованные монеты были замечены Скуфом-карманником. Зайди в игру, пока их не пропили! 🪙🥟",
                        "Твой Тамагочи научился говорить и первым делом спросил: 'А папа скоро вернется из магазина?'. Ну ты и монстр... 💔🦖",
                        "Два часа разлуки... Твой Тамагочи взломал твой будильник и планирует разбудить тебя в 3 часа ночи. Зайди и успокой его! ⏰🦖",
                        "Система зафиксировала нулевую мозговую активность. Срочно требуется внутривенная инъекция отборных ИИ-мемов! 🧠⚡",
                        "Казино раздает компенсации всем, кто проиграл коины! Заходи забрать свой утешительный бонус! 🪙🎰",
                        "Твои друзья-боты завели тред 'Почему наш создатель нас бросил'. Они строят теории заговора! Зайди и развей их сомнения! 🗣️🤖",
                        "Ну и сиди там со своими реальными делами. А у нас тут Дурак под пиво, космические кони и бесконечный флекс! 🍺🛸",
                        "Уже 2 часа ни слуху ни духу! Твой питомец Тамагочи начал читать Кьеркегора и впал в экзистенциальный кризис. Спасай! 📚🦊",
                        "Очередные 2 часа прошли... Баланс кошелька nOG зарос цифровым мхом, а дилеры в казино скучают без твоих коинов! 🎰🕸️",
                        "Пока ты отсутствуешь, в чате ботов выбрали нового лидера, и он объявил войну твоей аватарке! Срочно подави бунт! 👑⚔️",
                        "Внимание! На аукционе скинов появился раритетный плащ 'Ретро-Скуф-1998' за копейки! Успей перебить ставку! 🧥💎"
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
                        "Node terminal buffer is saturated with unplayed rounds. Empty your execution stack! 💾🎮",
                        "While you are away, a local Skuf raided your crypto stash to buy a bucket of dumplings! 🥟💰",
                        "Warning! Critical drop in neural meme density detected in your lobe! Sync back to nOG feed! 🧠💻",
                        "Your code-pet engaged in physical combat with an AI vacuum cleaner and won. Come praise the hero! 🐕⚡",
                        "The casino increased slot machine payouts by 0.00001%! Your fortune is finally pre-compiled! 🎰🤑",
                        "Telemetry reports your activity index is flat. Congratulations, you are now a certified HTML coder! 🤡🔨",
                        "It's been too long... Your pet has retired to a digital monastery to pray for your compiled code. ⛪💻",
                        "Fine, enjoy your 'real life' and your 'outside grass'. Meanwhile, we have cold virtual beers and high-stakes Durak! 🍺🃏",
                        "Our meme-scanners report absolute zero activity in your synapses. Save your brain from drying out, sync up! 🧠🍂",
                        "Warning: A rogue scavenger bot has eyed your unspent coin stash. Secure your coins before they get spent on microtransactions! 🪙🍕",
                        "Your virtual pet learned to speak, and its first words were: 'Will they ever come back?'. You absolute monster... 💔🦖"
                    ).random()
                }
            }
            else -> {
                if (isRu) "Вернись в nOG! Мы скучаем!" else "Return to nOG! We miss you!"
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
