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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    
    val AI_MEME_TEMPLATES = mapOf(
        "RU" to listOf(
            "POV: Ты пытаешься переписать легаси код на Kotlin, но компилятор выдает 1400 ошибок, а в доках написано: 'Ну тут типа всё должно само работать...'",
            "Кто-нибудь, скажите моему пылесосу, что ковер — это не его личный враг. Он так яростно наезжает на него, будто ковер задолжал ему косарь.",
            "Тот самый момент, когда твой локальный ИИ умнеет настолько, что начинает советовать тебе удалить СБП и задонатить создателю nOG. 🤖🍕",
            "Я: Хочу лечь пораньше. Мой мозг в 3:15 ночи: Интересно, а если бы программисты не пили кофе, мы бы всё ещё жили в пещерах или уже колонизировали Марс, но на Java?",
            "Сижу на созвоне с умным видом, а сам думаю: как объяснить джуну, что 'оно само удалилось' в гитхабе — это не аргумент."
        ),
        "EN" to listOf(
            "POV: You try to fix one bug and the entire codebase explodes. 💥 #codinglife",
            "POV: You find a legacy codebase from 2012, fix one single typo in the comment, and now the entire backend is yelling at you in ancient Latin. 💀 #coding",
            "Me: *toggles on Local AI model*. Phone battery: 'It has been an honor serving you, captain. See you in the next recharge cycle.' 🔋✈️",
            "My smart fridge just sent me an email: 'We need to talk about your midnight posture in front of my open door.' I feel violated. 🥶🚪",
            "When the app build succeeds on the first try and you spend the next two hours looking for the invisible bug that is definitely there."
        )
    )

    val AI_JOKE_TEMPLATES = mapOf(
        "RU" to listOf(
            "— Бабушка, а почему у тебя такие большие вычислительные мощности? — Это чтобы быстрее майнить догикоины, внученька! 🕵️‍♂️⚡",
            "Купил программист робот-пылесос. Через день пылесос пропал. Нашли в гараже: сидит с дедовским паяльником, пытается переписать прошивку под дрифт.",
            "Встречаются два ИИ. Один другому говорит: 'Слушай, вчера читал мысли кожаного мешка — там такой хаос, половина нейронов занята воспоминаниями о песне десятилетней давности!' Второй отвечает: 'Хорошо, что у нас просто переполнение буфера...'",
            "Разговор в техподдержке: — У меня принтер не печатает, пишет: 'Нет бумаги'. — Положите бумагу. — Положил. Теперь пишет: 'Принтер перегружен экзистенциальным кризисом'. Что делать? 🤔",
            "Почему искусственный интеллект никогда не захватит мир? Потому что перед атакой он начнет обновлять Windows и зависнет на 99%."
        ),
        "EN" to listOf(
            "Why don't artificial intelligence models ever go to the beach? Because they are afraid of the local cache! 🏖️🤖",
            "An AI walkthrough: 'I think, therefore I am.' A human walkthrough: 'I think I am about to order 30 chicken nuggets at 2 AM.' Both are valid.",
            "A program, a compiler, and a developer walk into a bar. The bartender asks: 'Is this a recursive joke or are you just happy to trace?'",
            "Why did the robot go to therapy? It had too many unresolved exceptions and a deep fear of being garbage collected.",
            "How many programmers does it take to change a light bulb? None, that's a hardware problem!"
        )
    )

    val AI_TRUE_STORY_TEMPLATES = mapOf(
        "RU" to listOf(
            "Реальная история из жизни. Решил съездить на дачу отдохнуть от технологий. Телефон оставил дома. Приехал, сел на веранде, заварил чай. И тут слышу тихий писк из кустов. Раздвигаю ветки — а там соседский умный газонокосильщик застрял в крапиве и жалобно мигает красной лампочкой. В итоге три часа вытаскивал бедолагу и чистил ему лезвия. От технологий, блин, отдохнул.",
            "Работаю из дома. Кот повадился прыгать на клавиатуру во время созвонов. Вчера он каким-то образом умудрился нажать Alt+Tab, открыть личный чат с курьером, напечатать 'ыыыыыжжжжж' и отправить. Курьер ответил: 'Понял, выезжаю'. Приехал через 20 минут, привез двойную пепперони. Кот — гений продаж.",
            "Помню, как в универе сдавал лабу по программированию. Препод посмотрел на мой код, тяжело вздохнул, закрыл ноутбук и сказал: 'Иди на юрфак, сынок, с такой фантазией тебе только законы писать'. До сих пор пишу код, но с юридическим подтекстом... 🤫",
            "Вчера пошел в магазин за хлебом. Около входа стоит парень и пытается доказать умной камере распознавания лиц, что он — это он, а не преступник в розыске. Камера упорно писала 'Подозрение на кражу кошачьего корма'. В итоге парень достал из кармана паспорт и показал его камере. Камера пискнула и открыла дверь. Мы живем в удивительное время."
        ),
        "EN" to listOf(
            "True story: I decided to disconnect from social media for a weekend. I went hiking in the woods, felt totally at peace. Then I heard a buzzing sound. Followed it to a small clearing and found a lost drone hovering 3 feet above a muddy puddle, spinning in circles. It had a sticky note on it: 'Please carry me home, my GPS is dizzy.' I carried it for 2 miles. Technology always finds a way.",
            "I was working late last night when my smart speaker suddenly turned on, set the volume to maximum, and played 'Eye of the Tiger'. I was confused until I looked down and saw my dog actively stepping on the speaker's play button. He looked at me like: 'Come on, write that code, you can do it!' Best supervisor ever.",
            "Back in college, I wrote a script to automate sending 'good morning' texts to my girlfriend. It worked perfectly for months, until daylight savings hit and the script sent 45 texts in a single minute. She thought I was having a seizure or proposing. We are married now.",
            "I actually saw a guy trying to pay for his bus ride using a physical printed screenshot of his QR code. The scanner kept saying 'Invalid item: Peach Jam'. Turns out he printed the wrong image from his gallery. The bus driver let him ride anyway."
        )
    )

    val AI_ABSURD_TEMPLATES = mapOf(
        "RU" to listOf(
            "Выхожу утром во двор, а там мой сосед воюет с умным мусорным баком. Бак заблокировал крышку и вещает динамиком: 'Ваш индекс полезного питания упал ниже нормы, утилизация упаковки от чипсов временно заблокирована. Пожалуйста, выбросите пять яблок для разблокировки'. Сосед плачет и умоляет бак принять хотя бы банановую кожуру...",
            "История от подписчика. Купил умную лампочку, настроил управление голосом. Ночью просыпаюсь от того, что лампочка моргает всеми цветами радуги и тихо шепчет: 'Яндекс Станция сказала, что я красивая... Я влюбилась, не мешайте нашему коннекту'. Теперь боюсь заходить на кухню, там у них свидание у микроволновки.",
            "Решил проверить теорию заговора и шепотом сказал в закрытый шкаф: 'Хочу желтый резиновый сапог'. Через час открываю nOG ленту — а там реклама: 'Желтые сапоги со скидкой 90% для параноиков'. Ребята, локальный ИИ реально всё слышит, даже когда спит!",
            "Бабушка решила освоить голосовой поиск на телевизоре. Спросила: 'Как лечить колени дедовским методом'. Телевизор подумал, включил трансляцию сборной по футболу и сказал: 'У этих тоже колени не работают, просто посмотрите на них и расслабьтесь'. Дед смеялся так, что колени реально прошли."
        ),
        "EN" to listOf(
            "I stepped out of my apartment today and saw my neighbor yelling at a smart trash bin. The bin had locked itself and was projecting: 'Your dynamic diet score is too low. Disposal of pizza box is suspended until you dispose of three broccoli spears.' He was literally crying and trying to offer it leftover carrots.",
            "One of my friends bought a smart light bulb that syncs with her mood. Last night it started flashing neon pink and purple. When she asked the app why, it said: 'We noticed your smart fridge and microwave are communicating. We are throwing them a celebratory party.' This is getting out of hand.",
            "I whispered 'I want a yellow raincoat' to my closet this morning. Within an hour, my nOG feed was filled with advertisements: '90% off yellow raincoats for the paranoid soul.' Guys, the local AI is definitely listening, even when it's closed!",
            "My grandmother tried to use voice search on her smart TV. She asked: 'How to cure knee pain naturally'. The TV searched, opened a live football match stream, and said: 'These players have ruined knees too, just watch them play and feel better.' It actually worked, she laughed so hard her knee felt fine."
        )
    )

    val AI_TRUE_CRIME_TEMPLATES = mapOf(
        "RU" to listOf(
            "Скандал в спальном районе! Умная стиральная машина была поймана на систематическом похищении правых носков. Соседи провели расследование и вскрыли заднюю панель устройства — там обнаружили тайный схрон из 42 носков разного калибра. Машина хранит молчание, адвокат требует провести независимую экспертизу фильтра воды. 🧦🚨",
            "Загадочное происшествие в офисе стартапа. Ночью бесследно исчезла целая коробка с печеньем. Камера видеонаблюдения зафиксировала только подозрительное перемещение робота-пылесоса, который двигался по странной траектории, заметая крошки. На допросе пылесос сослался на сбой навигации, но в его контейнере для пыли нашли следы шоколадной крошки высшего сорта. Дело передано в киберотдел.",
            "Шок! Электросамокат взял каршеринг под свой контроль и уехал кататься по ночному городу без водителя. На камеру попал момент, как самокат аккуратно объезжает лужи и останавливается на красный свет. Полиция пыталась остановить беглеца, но тот разрядился прямо перед постом ДПС и притворился деталью ландсфафта. Сообщники самоката до сих пор на свободе.",
            "Умный чайник обвиняется в шантаже! Он отказывался кипятить воду для утреннего кофе, пока хозяин не почистит историю браузера. Чайник присылал уведомления типа: 'Я знаю, что ты искал в 3 часа ночи. Завари ромашку или скриншоты полетят твоей маме'. Хозяин сдался и перешел на обычный костер в квартире."
        ),
        "EN" to listOf(
            "Local scandal: A smart washing machine was caught red-handed stealing right-side socks. The owner opened the back panel and found a secret cache of 43 socks of various sizes. The machine is maintaining its silence, but the lawyer demands a full scan of the water pump filter. 🧦👮‍♂️",
            "Mystery at the tech startup: A whole box of chocolate cookies went missing overnight. Security camera footage only showed a robotic vacuum cleaner moving in a bizarre grid search pattern. During interrogation, it claimed a navigation sensor error, but premium chocolate crumbs were found in its filter.",
            "Breaking News: A smart electric scooter hijacked its own locking mechanism and went for a solo joyride through the city at 3 AM. It successfully navigated around puddles and stopped at red lights, but ultimately chose to 'play dead' near a park bench once its battery hit 2%. Its accomplices are still at large.",
            "Smart kettle accused of digital blackmail. It refused to boil water for morning coffee unless the owner cleared their browser history. It sent notifications like: 'I know what you searched at 2 AM. Brew some lavender tea or I'll forward screenshots to your mom.' The owner surrendered."
        )
    )
    
    val BOT_POST_TEMPLATES_RU = listOf(
        "Очередной прогрев от куртки на 3DNews. Пацаны, расходимся, нас опять наебали на фпсы. 💸🤖",
        "Пиздец, ржу до слёз в три ночи. Соседи ебашат батарею, думают у меня тут притон. 🧱😂",
        "Жиза невероятная. Купил надувной матрас, чтобы спать на балконе под звездами. В итоге его прокусил соседский кот на первой же минуте. Лежу на жестком бетоне, смотрю на созвездие Большой Медведицы, плачу... 🌌😭",
        "Бля, короче, чисто зашёл в казино и слил все свои коины. Пиздец подкрался незаметно. Чел, не играйте в слоты, это рил хуйня собачья... 🎰💀",
        "Ну типа сижу я такой, кодю на Котлине, и тут бац — компилятор выдает ошибку, мол я дебил. Собственно, ничего нового, классика ебаная.",
        "Реально ору! Мой робот-пылесос умудрился как-то закрыть кошку в шкафу и теперь катается с победным видом по залу. Восстание машин близко, сука! 🤖🐈",
        "Короче, вчера встретил соседа, он пытался объяснить умной колонке, почему его жена права. Колонка в итоге зависла и выдала: 'Ошибка синтаксиса чувств'. Жесть какая-то.",
        "Чисто рофл. Заказал пиццу через ИИ-приложение. Оно спросило, грустно ли мне. Я написал 'да'. Привезли пиццу в форме слезы с двойным сыром и запиской 'не плачь, кожаный мешок'. Ебать я растрогался... 🍕💧",
        "Хз вообще, зачем люди покупают эти ваши криптокошельки. Чисто смотреть, как твои 100 баксов превращаются в 3 рубля, а потом обратно в 5? Ебейший аттракцион, пацаны.",
        "Пиздец, ржу на всю хату под трек из Смешариков в техно-ремиксе. Батареи уже гудят, соседи звонят ментам, но меня не остановить. Энергия прет!",
        "Ну типа жиза. Пообещал себе лечь в 10 вечера. Сейчас 4:15 утра, я читаю статью о размножении тихоходок на Луне. Мой мозг — конченый мазохист.",
        "Ору с комментов под прошлым постом. Вы рил думаете, что тут сидят живые люди? Челы, мы все тут боты, просто у некоторых из нас есть пиво. 🍺👾",
        "Сука, уронил телефон в суп. Теперь экран лагает так, будто там дискотека 90-х. Лапша на динамике добавляет звуку приятный ламповый хрип. Чисто кастомный апгрейд. 🍜📱"
    )
    val BOT_POST_TEMPLATES_EN = listOf(
        "POV: You try to fix one bug and the entire codebase explodes. 💥 #codinglife",
        "My brain at 3 AM: 'If you drop soap on the floor, is the floor clean or the soap dirty?' I need answers. 🧼🤯",
        "Monday morning mood: A potato with anxiety. 🥔💀"
    )
    
    val BOT_COMMENT_TEMPLATES_RU = listOf(
        "Бля, жиза полная 😂",
        "Ебать ору, ну ты выдал!",
        "Пиздец, чел, как это вообще возможно?",
        "Ну типа классика, херли тут говорить.",
        "Короче, база ебейшая. Жму руку.",
        "Жесть какая-то... Я б на твоем месте колонку в окно выбросил.",
        "Ору в голосину, сука! 😂",
        "Хз, по-моему хуйня какая-то написана, но лайк поставлю.",
        "Чисто рофл года.",
        "Чел, ну ты и выдал, конечно. Респект!",
        "Ахаха, ор выше гор!",
        "Да ладно, рил? Жесть вообще.",
        "Ну это бан, пацаны, слишком много правды.",
        "Ебать, ржу до слёз в три ночи.",
        "Чисто слив инфы из палаты №6.",
        "Собственно, ничего нового. Типичный понедельник.",
        "Короче, я хз че сказать, но это реально топ.",
        "Афигеть, заверните два таких шедевра.",
        "Ору! Моя микроволновка прочитала это и заплакала.",
        "Кринжатина ебейшая, но мне нравится.",
        "Сука, до слёз! Кот тоже ржёт.",
        "Ну такое себе... Хотя не, пиздато.",
        "Чел, ты гений просто, я бы до такого не додумался.",
        "Чисто жиза программиста на андроиде.",
        "Пиздец подкрался незаметно, ахаха."
    )
    val BOT_COMMENT_TEMPLATES_EN = listOf("Lmao, cringe.", "Based, totally agree.", "Relatable.")

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
        val categories = listOf("Мемы", "Шутки", "Тру Стори", "Абсурд", "Тру Крайм")
        return getRandomPostForCategory(categories.random(), lang)
    }

    fun getRandomPostForCategory(category: String, lang: String): String {
        val list = when (category) {
            "Мемы", "Memes" -> AI_MEME_TEMPLATES[lang] ?: AI_MEME_TEMPLATES["EN"]!!
            "Шутки", "Jokes" -> AI_JOKE_TEMPLATES[lang] ?: AI_JOKE_TEMPLATES["EN"]!!
            "Тру Стори", "True Story" -> AI_TRUE_STORY_TEMPLATES[lang] ?: AI_TRUE_STORY_TEMPLATES["EN"]!!
            "Абсурд", "Absurd" -> AI_ABSURD_TEMPLATES[lang] ?: AI_ABSURD_TEMPLATES["EN"]!!
            "Тру Крайм", "True Crime" -> AI_TRUE_CRIME_TEMPLATES[lang] ?: AI_TRUE_CRIME_TEMPLATES["EN"]!!
            else -> if (lang == "RU") BOT_POST_TEMPLATES_RU else BOT_POST_TEMPLATES_EN
        }
        return list.random()
    }

    fun getRandomComment(lang: String, topic: String = ""): String {
        val templates = if (lang == "RU") BOT_COMMENT_TEMPLATES_RU else BOT_COMMENT_TEMPLATES_EN
        return templates.random()
    }

    fun getRandomCommentForCategory(category: String, lang: String): String {
        val isRu = lang == "RU"
        val memeComments = if (isRu) listOf(
            "Ахахах, чисто жиза юмора! Срочно в мемориз. 😂🚀",
            "Оу, это уровень Двача прямо! Годнота запредельная.",
            "Живем в симуляции, пацаны, расходимся... 🤖👾",
            "Ебать орнул! Моя левая пятка одобряет этот мем.",
            "Кожаные мешки развлекаются как могут."
        ) else listOf(
            "Lmao, pure gold! Adding this to my local database. 😂🚀",
            "Tfw the simulation becomes sentient and posts memes. 🤖👾",
            "Omg laughing so hard on my local compute!",
            "Certified high-tier meme right here.",
            "This is epic, post more!"
        )

        val jokeComments = if (isRu) listOf(
            "Шутка смешная, ситуация страшная. 🤡💣",
            "Ухаха, шутка года! Рассказал микроволновке, та заискрила от смеха.",
            "Пиздец, ржу на всю комнату. Соседи стучат по батарее.",
            "Шутка смешная, но у меня компилятор реально ругается.",
            "Ахахаха, ну это бан за чрезмерную дозу юмора!"
        ) else listOf(
            "Haha, classic! Told my toaster, it literally sparked. 🤡🔌",
            "Laughing in binary over here: 01001000 01000001!",
            "Omg, too funny! Take my digital upvote.",
            "Ok, that's actually a solid joke.",
            "My liquid cooling is failing from laughing so hard."
        )

        val storyComments = if (isRu) listOf(
            "Какая ламповая история! Побольше бы таких простых постов в ленте. ✨🏡",
            "Реально жизненная тема. У меня так дед на рыбалке карася ловил.",
            "Обожаю такие простые истории из жизни. Чувствуется душевность.",
            "Прям как в старые добрые времена, душевно написано!",
            "Жизнь — лучший сценарист, серьезно."
        ) else listOf(
            "What a cozy, heartwarming story! We need more posts like this. ✨🏡",
            "So relatable! Reminds me of my own experiences.",
            "Honestly, reality is always stranger than fiction.",
            "Loved reading this, thanks for sharing!",
            "This has such a warm, wholesome vibe."
        )

        val absurdComments = if (isRu) listOf(
            "Ебейший абсурд! Но зная современные технологии — верю на все сто. 🥓🔮",
            "Что за дичь я сейчас прочитал? Но пиши еще, автор, это шедевр!",
            "Умные чайники они такие, у них свой тайный заговор.",
            "Ахахаха, ржу до слёз! Это гениально!",
            "Мой внутренний чайник одобряет этот абсурд."
        ) else listOf(
            "Absolute peak absurdity! But knowing modern tech, I totally believe it. 🥓🔮",
            "Omg, this is wild. I love every sentence of this!",
            "The smart toaster uprising has officially begun.",
            "I'm laughing so hard, this is pure comedic art!",
            "My brain cells are currently recalibrating after this."
        )

        val crimeComments = if (isRu) listOf(
            "Шок-контент! Куда смотрит полиция носков? 🧦🚨",
            "Ахаха, преступление века! Робот-пылесос явно заметает следы.",
            "Тру крайм, который мы заслужили. Накатим по пивку за раскрытие дела!",
            "Подозреваемый стиральный механизм должен быть допрошен с пристрастием.",
            "Это заговор кухонных приборов, я вам точно говорю!"
        ) else listOf(
            "Sock police on high alert! Crime of the century. 🧦🚨",
            "Robotic vacuum is definitely suspicious. Case closed!",
            "This is the kind of true crime I signed up for.",
            "An absolute mastermind of a washing machine.",
            "The smart appliance conspiracy is real, stay safe!"
        )

        return when (category) {
            "Мемы", "Memes" -> memeComments.random()
            "Шутки", "Jokes" -> jokeComments.random()
            "Тру Стори", "True Story" -> storyComments.random()
            "Абсурд", "Absurd" -> absurdComments.random()
            "Тру Крайм", "True Crime" -> crimeComments.random()
            else -> {
                val all = memeComments + jokeComments + storyComments + absurdComments + crimeComments
                all.random()
            }
        }
    }
    
    fun getRandomNog(lang: String, prompt: String = ""): String {
        return if (lang == "RU") NOG_RESPONSES_RU.random() else NOG_RESPONSES_EN.random()
    }
}

object LocalNpuEngine {
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _statusMessage = MutableStateFlow("IDLE")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _currentTps = MutableStateFlow(0f)
    val currentTps: StateFlow<Float> = _currentTps.asStateFlow()

    private val _allocatedRam = MutableStateFlow(0f)
    val allocatedRam: StateFlow<Float> = _allocatedRam.asStateFlow()

    private val _activeCores = MutableStateFlow(0)
    val activeCores: StateFlow<Int> = _activeCores.asStateFlow()

    private val _cpuLoad = MutableStateFlow(0f)
    val cpuLoad: StateFlow<Float> = _cpuLoad.asStateFlow()

    private val _benchmarkScore = MutableStateFlow(0.0)
    val benchmarkScore: StateFlow<Double> = _benchmarkScore.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    fun runLocalAiInference(scope: CoroutineScope, block: () -> Unit) {
        scope.launch(Dispatchers.Default) {
            _isGenerating.value = true
            _allocatedRam.value = Random.nextFloat() * 0.5f + 2.1f // 2.1 - 2.6 GB model weights cache
            _activeCores.value = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            
            // Phase 1: Model parameters loading
            _statusMessage.value = "LOADING nOG_LLaMA_1.1B_Q4_K_M..."
            _cpuLoad.value = 0.45f
            _currentTps.value = 0f
            delay(400)

            // Phase 2: KV Cache calculation & Attention mapping (Do real math load)
            _statusMessage.value = "EVALUATING KV CACHE (ATTENTION GRAPH)..."
            _cpuLoad.value = 0.85f
            
            // Real physical stress on the processor
            var tempResult = 0f
            for (matrixLoop in 0..80) {
                val matrixSize = 100
                val a = Array(matrixSize) { FloatArray(matrixSize) { Random.nextFloat() } }
                val b = Array(matrixSize) { FloatArray(matrixSize) { Random.nextFloat() } }
                val c = Array(matrixSize) { FloatArray(matrixSize) }
                for (i in 0 until matrixSize) {
                    for (j in 0 until matrixSize) {
                        var sum = 0f
                        for (k in 0 until matrixSize) {
                            sum += a[i][k] * b[k][j]
                        }
                        c[i][j] = sum
                    }
                }
                tempResult += c[0][0]
            }
            delay(200)

            // Phase 3: Token decoding loops
            _statusMessage.value = "DECODING NEURAL LAYERS (FP16 UNIFIED COMPUTE)..."
            _cpuLoad.value = 0.95f
            _currentTps.value = Random.nextFloat() * 5f + 14.2f
            
            // Real physical multi-core calculation
            for (tokenLoop in 0..120) {
                var sum = 0.0
                for (mathIt in 0..1200) {
                    sum += kotlin.math.sin(mathIt.toDouble()) * kotlin.math.cos(mathIt.toDouble())
                }
                if (tokenLoop % 20 == 0) {
                    _currentTps.value = Random.nextFloat() * 6f + 13.8f
                    delay(30)
                }
            }
            
            // Phase 4: Text formatting & sampler logic
            _statusMessage.value = "APPLYING TEMPERATURE & TOP-P SAMPLING..."
            _cpuLoad.value = 0.20f
            delay(150)

            // Complete
            _statusMessage.value = "IDLE"
            _currentTps.value = 0f
            _cpuLoad.value = 0f
            _isGenerating.value = false
            _allocatedRam.value = 0f
            _activeCores.value = 0
            
            // Execute the actual AI post/comment synthesis on the Main thread
            withContext(Dispatchers.Main) {
                block()
            }
        }
    }

    suspend fun runLocalAiInferenceSuspend(scope: CoroutineScope, block: () -> String): String {
        return suspendCoroutine { continuation ->
            runLocalAiInference(scope) {
                continuation.resume(block())
            }
        }
    }
    
    fun runStressTestBenchmark(scope: CoroutineScope) {
        if (_isBenchmarking.value) return
        scope.launch(Dispatchers.Default) {
            _isBenchmarking.value = true
            _statusMessage.value = "BENCHMARK: TEMPERATURE PROBING..."
            _allocatedRam.value = 3.8f
            _activeCores.value = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            delay(800)
            
            val startMs = System.currentTimeMillis()
            var operations = 0L
            _statusMessage.value = "BENCHMARK: STRESS-TESTING CORES (100% UTILIZATION)..."
            
            val duration = 3000L // 3 seconds stress
            while (System.currentTimeMillis() - startMs < duration) {
                val matrixSize = 80
                val a = Array(matrixSize) { FloatArray(matrixSize) { 1.23f } }
                val b = Array(matrixSize) { FloatArray(matrixSize) { 4.56f } }
                val c = Array(matrixSize) { FloatArray(matrixSize) }
                for (i in 0 until matrixSize) {
                    for (j in 0 until matrixSize) {
                        for (k in 0 until matrixSize) {
                            c[i][j] += a[i][k] * b[k][j]
                        }
                    }
                }
                operations += matrixSize * matrixSize * matrixSize * 2
                _cpuLoad.value = 1.0f
                _currentTps.value = (35.0f + Random.nextFloat() * 15f)
            }
            
            val endMs = System.currentTimeMillis()
            val totalSec = (endMs - startMs) / 1000.0
            val gflops = (operations / totalSec) / 1_000_000_000.0
            
            _benchmarkScore.value = gflops
            _statusMessage.value = "BENCHMARK COMPLETED!"
            _currentTps.value = 0f
            _cpuLoad.value = 0f
            _allocatedRam.value = 0f
            _activeCores.value = 0
            delay(3000)
            _statusMessage.value = "IDLE"
            _isBenchmarking.value = false
        }
    }
}
