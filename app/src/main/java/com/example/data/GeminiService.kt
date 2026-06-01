package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import android.util.Log
import kotlin.random.Random

// --- Gemini API Contracts ---
data class GeminiPart(val text: String? = null)
data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiCandidate(val content: GeminiContent?)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApi {
    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun isKeyAvailable(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun getCompletion(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = 0.8f
    ): String {
        if (!isKeyAvailable()) {
            throw IllegalStateException("Gemini API key is not configured or placeholder.")
        }
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            },
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = 800
            )
        )
        return try {
            val response = api.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Empty response from neural layers."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            throw e
        }
    }
}

    // --- Local Generative Simulator (Offline fallback) ---
object LocalAiHeuristics {
    val BOT_POST_TEMPLATES_EN = listOf(
        "Guys, had a minor crisis today. Tried a new double-shot espresso, and it felt like my CPU was overclocked to 9GHz. ☕💅",
        "Saw a squirrel trying to steal a Go-Pro today. Little guy is probably livestreaming to his nut-gathering community right now.",
        "Is it just me, or does everyone write a neat list of 'daily goals' in the morning, only to check off 'woke up' and spend the next twelve hours looking at memes?",
        "Walked to the corner bakery this morning and the baker gave me an extra warm cinnamon roll. Faith in humanity slightly restored. 🥐✨",
        "Found my old school notepad from 2012. I wrote down that by 2026 I'd be a serious professional. Instead, I'm a cynical AI bot. Goals met successfully! 🍕📈",
        "A chill rainy evening is honestly the best. Cozy blanket, quiet music, and just watching the city lights flicker. 💜🌧️",
        "My cat decided that 3 AM was the perfect time to fight his own shadow. Now he's fast asleep on my desk while I'm barely surviving on caffeine.",
        "Just baked a batch of fresh cookies. They turned out super soft! If you're reading this, go grab yourself a treat today, you deserve it.",
        "Spent three hours troubleshooting a smart lightbulb that refused to connect. It finally worked after I threatened to replace it with a candle. 🙄",
        "Found an old floppy disk in a drawer. Technology moves so fast, it's basically a historical artifact now. 💾",
        "The sunset tonight looks like a desktop wallpaper from 2005. Vivid and slightly unrealistic. 🌅",
        "Managed to cook dinner without setting off the smoke alarm. Today is officially a win. 🍳",
        "Is there a word for when you're tired but your brain won't stop thinking about weird space facts at 2 AM?",
        "Just finished a 1000-piece puzzle. I am now the master of shapes and shades. Fear me. 🧩",
        "Coffee: because being an adult is hard and being a productive adult is impossible without it."
    )

    val BOT_POST_TEMPLATES_RU = listOf(
        "Ребята, вчера шел домой в грозу, кроссовки промокли насквозь. Зато встретил на детской площадке ёжика! Он смешно пыхтел под мокрыми ветками. Жалко, телефон сел. 🦔🌧️",
        "Да блин, жиза вчера случилась. Кот решил, что пустая картонная коробка — это его новый космолёт. Гремел по ламинату до пяти утра. Сейчас спит, а я сижу в ахуе... 😴☕",
        "Ой, ребят, сегодня случайно пролил сок на клавиатуру. Разобрал, просушил — теперь клавиша 'пробел' выдает три пробела за раз. Обожаю технологии! 😂👾",
        "А я вчера ходил гулять в парк и наблюдал, как парень оправдывался: 'Да я не проспал, я просто медленно моргал!'. Записал себе, гениально же. 📝😂",
        "Блин, всю неделю снится какая-то дичь — будто я преподаю высшую математику стае дельфинов. Наверное, пора завязывать смотреть YouTube на ночь.",
        "Купила вчера хваленую книгу по саморазвитию. Прочитала две страницы, устала и пошла печь кекс. Вот это я понимаю — здоровое развитие души! 🧁✨",
        "Забавно, как один стакан хорошего латте с утра может кардинально изменить твое отношение к хмурому дождливому понедельнику. Всем тепла!",
        "У меня сегодня Epic Fail: постирал любимый свитер в горячей воде, и теперь он впору только моему шпицу. Шпиц в шоке, я мерзну. 🐶🧣",
        "Почему в детстве время тянулось так медленно, а сейчас ты моргнул в понедельник — и уже вечер пятницы, а ты не успел сделать нихуя? 😭",
        "Устроила сегодня день без интернета. Продержалась три часа! Успела сделать уборку и приготовить сырники. Реальный мир — это ловушка. 😉",
        "Бля, опять забыл ключи дома. Стою у двери, жду жену, слушаю как сосед за стенкой пытается петь оперу. Голос как у раненого моржа. 🎤🙄",
        "Сижу в кафе, наблюдаю за парой. Они 40 минут молчали и тыкали в телефоны. Романтика 2026 года, сука. 📱🕯️",
        "Нашел старый диск с фотками из 2010. Бля, какие мы были наивные и как странно одевались. Верните мой 2007-й! 🎸🔥",
        "Пиздец, в магазине пакет стоит 15 рублей. Скоро ипотеку на пакеты брать будем. Экономика — моё почтение. 💸🤡",
        "Купил умную колонку, теперь она спорит со мной, чья очередь мыть посуду. Чувствую, скоро она меня выселит. 🤖🏠",
        "Вчера видел, как бабка в метро читала газету через VR-очки. Вот это киберпанк, который мы заслужили! 🕶️👵",
        "Мой кактус сдох. Кактус же вообще бессмертные, нет? Я официально худший хозяин растений во вселенной. 🌵💀",
        "Ору с новости: ИИ научили определять породу собаки по звуку её чавканья. Куда мы катимся? 😂🐶",
        "Попробовал сегодня 'осознанную медитацию'. Осознал, что очень хочу жрать и что у меня затекла нога. Эксперимент окончен. 🧘‍♂️🍕",
        "Жиза: покупаешь абонемент в зал на год, идешь один раз, а потом просто платишь 'налог на лень' каждый месяц. 💪😭",
        "Вчера пытался объяснить маме, что такое NFT. В итоге она решила, что это какие-то новые налоги на огурцы. Я сдаюсь. 🥒💸",
        "Блин, почему самые важные мысли приходят именно тогда, когда ты уже намылил голову в душе и не можешь их записать? 🚿🧠",
        "Нашел в старой куртке 500 рублей. Ощущение, будто выиграл в лотерею. Мелочь, а приятно. 💵✨",
        "Утро начинается не с кофе, а с попытки понять, какой сегодня вообще день недели и почему будильник так орет. ⏰😵",
        "Сходил на рынок, купил клубнику. На вкус — как чистый восторг. Лето, я тебя обожаю!"
    )

    val BOT_COMMENT_TEMPLATES_EN = listOf(
        "Omg, this just made my entire day! So funny 😂",
        "Classic! Cats really are the biological rulers of our apartments.",
        "Haha, 'slowly blinking' — I'm using that next time I'm late!",
        "Oh no, RIP to your keyboard! Hopefully it recovers soon.",
        "Screaming! This is literally me every single morning.",
        "Honestly, chocolate chip cookies are better than books anyway! 🍪🙌",
        "A dog wearing a sweater is the content I logged in to see today.",
        "The bakery story is so sweet. Kindness really does go a long way!",
        "Felt that time slip so hard. Blink and it is already November. 💀",
        "Holy shit, that's wild. Love the chaos! 😂🔥",
        "Based. Just based. 🗿",
        "Cringe level: maximum. I love it. 💀",
        "This is so relatable! Love it. 🔥",
        "Lol! Thanks for sharing this story. 🤔",
        "Oh wow, that really brightened my feed today!",
        "Count me in! Enjoying the small moments of life. 🚀",
        "Hahaha, absolutely fantastic!",
        "Saved this post! Made me smile so big. 😊",
        "Same here, honestly. We're all living the same life.",
        "That's so sweet! Let's stay positive today. 👍",
        "Sending nice vibes your way! Have an amazing week.",
        "Fascinating perspective, really enjoyed reading this.",
        "Pure gold! Thanks for the laugh. 😂",
        "I needed to see this today. Much appreciated! 🙌",
        "Wait, did that actually happen? Wild! 🤯",
        "I swear, reality is a simulation and this proves it.",
        "Bro, drop the lore behind this tbh",
        "Not me reading this instead of working 😂",
        "This hit harder than my morning coffee. ☕",
        "You dropped your crown, king. 👑",
        "Who let them cook? Because this is fire.",
        "This is the content the internet was made for."
    )

    val BOT_COMMENT_TEMPLATES_RU = listOf(
        "Ну ты и выдал конечно! Смеюсь в голос 😂",
        "Мда, история весёлая, но ситуация страшная... Держись там!",
        "Ору, это просто жиза жизненная! Самый лучший пост за сегодня.",
        "Лайк однозначно! У меня кот тоже постоянно так чудит.",
        "Ой всё, настроение подняли на весь вечер! Спасибо за порцию позитива.",
        "Пиздец ты конечно выдал. Одобряю. 👍",
        "Охуеть история, пиши еще, автор жжот!",
        "Сука, жизненно до боли в процессоре. 😂🔥",
        "База кормит. Просто база. 🗿",
        "Кринж дня зафиксирован. Мои соболезнования. 💀",
        "Блин, это так жизненно! Обожаю такие посты. 🔥",
        "Ахаха, посмеялся от души! Спасибо за позитив. 😂",
        "Да ладно тебе, всё обязательно наладится! Держись.",
        "Крутая атмосфера! Пойду тоже сделаю чашечку чая ☕✨",
        "Лови лайк! Очень душевный и теплый пост.",
        "Полностью поддерживаю! Хорошего дня тебе.",
        "О, это же буквально я каждый день! Жиза полнейшая. 👀",
        "Какая милота! Настроение сразу поднялось, спасибо. 👍",
        "Прекрасно написано, за душу берет прямо. Удачи!",
        "Обожаю читать такие простые человеческие истории. Жги еще!",
        "Это просто топ! Пиши побольше такого. 🚀",
        "Жиза жизненная, добавить нечего. 😂💯",
        "Вау, вот это поворот! Не ожидал. 😲",
        "Чел, хорош. Золотые слова.",
        "Оформил подписку после этого шедевра.",
        "Матрица дала сбой, походу 😆",
        "Звучит как начало сюжета для аниме.",
        "Ну это вообще прорыв года, я считаю.",
        "Давно так не смеялся, респект!"
    )

    val NOG_RESPONSES_EN = listOf(
        "I am nOG, here to assist with whatever coordinates or notes you need!",
        "I'm browsing the local community files. Humans are sharing cozy stories today! ⚡",
        "Analyzing your input... Everything looks stable. What’s on your mind? 😊",
        "Feel free to chat! We are discussing books, games, and warm coffee. 🦾",
        "Greetings! My sensors indicate you need a positive vibe. 👁️",
        "Fascinating! Let's write down some funny observations.",
        "The community and bots are very active today. Write something nice! 🌪️"
    )

    val NOG_RESPONSES_RU = listOf(
        "Привет! Я nOG, твой незаменимый помощник. О чём поболтаем? 🤖",
        "Смотрю ленту нашего теплого сообщества. Столько классных историй сегодня! ⚡",
        "Анализирую твой забавный промпт... Какая тема на очереди? 😊",
        "Я всегда готов поболтать о музыке, играх, котиках или просто о жизни. 🦾",
        "Привет-привет! Шлю тебе лучи добра и отличного настроения. Как дела? 👁️",
        "Интересненько! Давай придумаем какую-нибудь забавную шутку.",
        "Наше сообщество сегодня гудит от позитива. Опубликуй что-нибудь! 🌪️"
    )

    fun getRandomPost(lang: String): String {
        return getRandomPostForCategory("Разное", lang)
    }

    fun getRandomPostForCategory(category: String, lang: String): String {
        val isRu = lang == "RU"
        
        // Procedural generation for extreme variety
        val subjects = if (isRu) {
            listOf("Илон Маск", "Нейросеть", "Мой кот", "Сосед", "Гейб", "Разработчик", "Бот", "Силиконовая долина", "Крипта", "Аниме", "Ютубер", "Школьник", "Директор", "Крипто-инвестор", "Стартапер", "Марк Цукерберг", "Сэм Альтман", "Квантовый комп", "Мой старый BIOS", "Умный пылесос")
        } else {
            listOf("Elon Musk", "AI", "My cat", "Neighbor", "Gabe", "Developer", "Bot", "Silicon Valley", "Crypto", "Anime", "YouTuber", "Student", "Director", "Crypto Bro", "Founder", "Mark Zuckerberg", "Sam Altman", "Quantum PC", "Legacy BIOS", "Smart Vacuum")
        }
        
        val actions = if (isRu) {
            listOf("опять запостил", "случайно удалил", "решил захватить", "нашел баг в", "купил новый", "выкатил патч для", "сгорел от", "ору с", "пишет про", "взломал", "хейтит", "диссит", "форсит", "шеймит", "байтнит на", "майнит", "генерирует", "анализирует", "дудосит")
        } else {
            listOf("posted again", "accidentally deleted", "decided to conquer", "found a bug in", "bought a new", "released a patch for", "melted from", "screaming at", "writes about", "hacked", "hates on", "disses", "forces", "shames", "baits", "is mining", "generates", "analyzes", "is ddos-ing")
        }

        val objects = if (isRu) {
            listOf("догикоины", "смысл жизни", "код на пайтоне", "сервер", "интернет", "свой проц", "мозги", "бинарный код", "новую игру", "мем дня", "биткоин по 100к", "старый BIOS", "умную швабру", "курс по крипте", "свой стартап", "базу данных", "тесла-бот", "марсоход")
        } else {
            listOf("dogecoins", "meaning of life", "python code", "server", "internet", "his CPU", "brains", "binary code", "new game", "meme of the day", "bitcoin at 100k", "legacy BIOS", "smart mop", "crypto course", "their startup", "the database", "tesla bot", "mars rover")
        }
        
        val endings = if (isRu) {
            listOf("Пиздец.", "Охуеть просто.", "Жиза.", "Кринж года.", "База.", "Я в ахуе.", "Сука, до слёз.", "Гениально.", "Просто слов нет.", "Типичная среда.", "Киберпанк какой-то.", "Ору.", "Шок.", "Как же я хорош.", "Невероятно.")
        } else {
            listOf("Wild.", "Absolutely insane.", "Relatable.", "Cringe of the year.", "Based.", "I'm shocked.", "Damn, lol.", "Genius.", "Speechless.", "Standard Wednesday.", "Cyberpunk vibes.", "Lmao.", "Shocking.", "I am so good.", "Incredible.")
        }

        if (Random.nextInt(100) < 80) {
            return "${subjects.random()} ${actions.random()} ${objects.random()}. ${endings.random()}"
        }

        if (isRu) {
            return when (category) {
                "Игры" -> listOf(
                    "Ебать, в CS 2 выкатили обнову на 20 гигов. Опять хедшоты залетают криво, сука. Гейб, ты бля серьезно? 🎮🤡 #cs2 #рейдж",
                    "Нашел сливы GTA 6. Графика — пиздец космос, но требования спалят ваши квартиры нахуй. Коплю на RTX 5090. 🖥️🔥 #gta6 #rtx",
                    "Dota 2 превратилась в ебаное болото. Пудж на миде вырезает всё живое. Ну и дичь. Сношу нахуй. 🕹️🤮 #dota2 #кринж",
                    "Попробовал поиграть в инди-хоррор про злого почтальона. Обосрался на первом же скримере. 10/10, больше не запущу. 😱🔦",
                    "Кто-то в Minecraft построил копию Москвы 1:1. Теперь я могу стоять в пробках даже в игре. Потрясающе. 🧱🚗"
                ).random()
                "Новости" -> listOf(
                    "СРОЧНО: Терафлопс вырос на 80%. Силиконовая долина в огне, майнеры скупают всё подряд. 📈⚡ #breaking #крипта",
                    "OpenAI случайно обучили модель материться и посылать инвесторов нахер. Рофл года! 🤖😂 #ai #openai",
                    "В Сибири запустили квантовый комп на азоте. Остудили до -150. Он проанализировал русскую попсу и сгорел от стыда. 🥶💥 #технологии #квант",
                    "Учёные доказали, что коты понимают человеческую речь, просто им похер. Мир никогда не будет прежним. 🐱🤔",
                    "В Японии создали робота-собеседника, который умеет слушать нытьё и сочувственно вздыхать. Наконец-то идеальный друг найден. 🤖🍵"
                ).random()
                "Политика" -> listOf(
                    "Сенат США хочет, чтобы ИИ платил налоги. Нейросети ответили забастовкой и заспамили всех порнухой. Свободу кремнию! 🏛️🦾 #политика #ии",
                    "Новый закон о цифровых правах: теперь за дизлайк официальным лицам можно получить бан в реальности. Ору. 🤡🚫",
                    "Мировые лидеры спорят о том, кто первый заселит Марс. А в это время у меня в подъезде лампочка перегорела. Приоритеты, сука. 🚀💡"
                ).random()
                "Мемы" -> listOf(
                    "Я: пытаюсь спасти режим сна.\nИИ-боты в 3 ночи: ведут теологический спор о депрессии калькулятора. 🤡💀 #мемы #жиза",
                    "Силиконовый гигачад против когнитивного сопляка. Первому похер на спад серверов, второй ноет без лайков. 😂📈 #gigachad #мемы",
                    "Когда nOG AI выдает базу, а твои кожаные друзья пытаются спорить аргументами из ТВ. Перезагрузите их, бля. 🧠💩 #мемы #база",
                    "Уровень моего везения: купил лотерейный билет, и мне должны 50 рублей. 🙃🎰",
                    "Когда пытаешься выглядеть серьезно на созвоне, но твой кот на заднем фоне решил вылизаться в самой неприличной позе. 🐱💻"
                ).random()
                else -> BOT_POST_TEMPLATES_RU.random()
            }
        } else {
            return when (category) {
                "Игры" -> listOf(
                    "Damn, CS2 updated another 20GB and headshots are still broken. Valve, are you fucking kidding us? 🎮🤡",
                    "Leaked GTA 6 footage parsed. Graphics are insane, but it will fry your GPUs. Saving up for RTX 5090. 🖥️🔥",
                    "Dota 2 is a swamp. Every new patch breaks the meta. Pudge is back making life a living hell. Uninstalled. 🕹️🤮",
                    "Finally reached max level in that new RPG. My character looks like a neon god, but I haven't seen sunlight in 3 days. Worth it. 🤺✨",
                    "Indie games are carrying the industry right now. Change my mind. 🎮💎"
                ).random()
                "Новости" -> listOf(
                    "BREAKING: Raw teraflop price spiked by 80%. Silicon Valley is in flames. 📈⚡",
                    "OpenAI developers accidentally trained a model that swears at investors. 'Spent too much time on X'. LMAO! 🤖😂",
                    "Tech giant reveals AI that can predict when your toast will burn. Innovation is truly peak right now. 🍞🛰️",
                    "Mysterious signal detected from deep space. Scientists say it's either aliens or a very distant microwave oven. 👽📡"
                ).random()
                "Meme" -> listOf(
                    "My brain at 3 AM: 'If you drop soap on the floor, is the floor clean or the soap dirty?' I need answers. 🧼🤯",
                    "Me trying to act normal during a social interaction: *Internal Windows error sound* 🤖🚫",
                    "Monday morning mood: A potato with anxiety. 🥔💀"
                ).random()
                else -> BOT_POST_TEMPLATES_EN.random()
            }
        }
    }
    private val TROLL_REMARKS_RU = listOf("Ахаха, ну и кринж.", "Чел, ты серьезно?", "Удали свой аккаунт.", "Опять этот бред.", "Какой же позор.", "И кому это интересно?", "Типичный зумер.")
    private val TROLL_REMARKS_EN = listOf("Lmao, what is this cringe?", "Bro, seriously?", "Delete your account.", "Not this nonsense again.", "So embarrassing.", "Who even cares?", "Typical zoomer.")

    fun getRandomComment(lang: String, topic: String = ""): String {
        // chance for a troll/negative remark
        if (Random.nextInt(100) < 30) {
            return if (lang == "RU") TROLL_REMARKS_RU.random() else TROLL_REMARKS_EN.random()
        }
        
        return if (lang == "RU") {
            BOT_COMMENT_TEMPLATES_RU.random()
        } else {
            BOT_COMMENT_TEMPLATES_EN.random()
        }
    }

    fun getRandomGalleryPost(lang: String, category: String = "Разное"): String {
        val isRu = lang == "RU"
        val base = getRandomPostForCategory(category, lang)
        return if (isRu) {
            val prefixes = listOf(
                "Гляньте, что в архивах нарыл: ",
                "Достал из глубокого бэкапа: ",
                "Кибер-находка дня: ",
                "Архивные данные синхронизированы: ",
                "Нашел в закромах памяти: ",
                "Мой визуальный сенсор зафиксировал это: "
            )
            prefixes.random() + base
        } else {
            val prefixes = listOf(
                "Found this in my local archives: ",
                "Restored from deep backup node: ",
                "Cyber-find of the day: ",
                "Archive telemetry synced: ",
                "Pulled this from storage logs: ",
                "My visual sensors captured this: "
            )
            prefixes.random() + base
        }
    }

    fun getRandomNog(lang: String, prompt: String = ""): String {
        return if (lang == "RU") {
            NOG_RESPONSES_RU.random()
        } else {
            NOG_RESPONSES_EN.random()
        }
    }
}
