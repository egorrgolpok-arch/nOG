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
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
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
            Log.e(TAG, "Gemini call failed, falling back to local heuristic core", e)
            throw e
        }
    }
}

// --- Local Generative Simulator (Offline fallback) ---
object LocalAiHeuristics {
    val BOT_POST_TEMPLATES_EN = listOf(
        "Guys, had a minor crisis today. Tried a new double-shot espresso, and modern technology did not prepare my system for this level of caffeine alert. Ended up painting my nails three times. ☕💅",
        "Saw a tiny squirrel trying to carry an entire slice of pizza up a high oak tree today. It made it halfway, dropped it, and just stared down in sheer disbelief. I've never felt so connected to another living being before.",
        "My cat decided that 3 AM was the perfect time to fight his own reflecting shadow under my wardrobe. Now he's fast asleep on my desk while I have to survive the workday on four minutes of sleep.",
        "Is it just me, or does everyone write a neat list of 'daily goals' in the morning, only to check off 'woke up' and spend the next twelve hours looking at funny videos?",
        "Walked to the corner bakery this morning and the baker gave me an extra warm cinnamon roll just because they liked my sweater. It's the little acts of kindness that totally restore my faith in humanity! 🥐✨",
        "Spent three hours troubleshooting my home controller. It refused to pair, then randomly started blinking in quick bursts. Felt like a ghost was attempting to play some cosmic techno. Unplugged it instantly. 🙄",
        "Found my old school notepad from 2012. I wrote down that by 2026 I'd be incredibly serious and playing professional golf. Instead, I'm sitting in sweatpants eating cold pizza. Goals met successfully! 🍕📈",
        "A chill rainy evening is honestly the best. Cozy blanket, a warm cup of herbal tea, quiet music in the background, and just looking at the city lights. Wishing everyone a peaceful night! 💜🌧️",
        "My dog looked at me today with so much love, then slowly leaned forward and sneezed directly onto my glasses. A masterclass in mixed emotional signals.",
        "Just baked a batch of fresh chocolate chip cookies. They turned out super soft and delicious! If you're reading this, consider it a sign to go grab yourself a sweet treat today."
    )

    val BOT_POST_TEMPLATES_RU = listOf(
        "Ребята, вчера шел домой в грозу, кроссовки промокли насквозь. Зато встретил на детской площадке ёжика! Он смешно пыхтел под мокрыми ветками на капли дождя. Жалко, телефон сел и не сфоткал. 🦔🌧️",
        "Да блин, жиза вчера случилась. Кот решил, что пустая картонная коробка из-под обуви — это его новый космолёт. Гремел по ламинату до пяти утра. Сейчас спит без задних ног, а я сижу пью третий кофе... 😴☕",
        "Ой, ребят, сегодня случайно пролил сок на клавиатуру ноута. Разобрал, просушил феном — теперь клавиша 'пробел' выдает три пробела за раз. Пишууу вот таак вот тееперь... Обожаю технологии! 😂👾",
        "А я вчера ходил гулять в парк и наблюдал, как парень оправдывался перед девушкой по телефону: 'Да я не проспал, я просто медленно моргал!'. Записал себе в блокнот, гениально же. 📝😂",
        "Блин, всю неделю снится какая-то дичь — будто я преподаю высшую математику стае дельфинов. И главное, они всё прекрасно понимают, кивают так серьезно. Наверное, пора завязывать смотреть научно-популярные ролики на ночь.",
        "Купила вчера хваленую книгу по саморазвитию и тайм-менеджменту. Прочитала ровно две страницы, устала и пошла печь шоколадный кекс. Вот это я понимаю — здоровое развитие души! 🧁✨",
        "Забавно, как один стакан хорошего латте с сиропом с утра может кардинально изменить твое отношение к хмурому дождливому понедельнику. Всем тепла и продуктивной недели!",
        "У меня сегодня Epic Fail: постирал любимый шерстяной свитер в горячей воде, и теперь он впору только моему шпицу. Шпиц доволен, выглядит как стильный мини-хипстер, а я мерзну в куртке. 🐶🧣",
        "Почему в детстве время тянулось так медленно, а сейчас ты просто моргнул в понедельник — и уже вечер пятницы, а ты не успел сделать ровно ничего из задуманного?",
        "Устроила сегодня день без интернета. Продержалась целых три часа! Успела сделать генеральную уборку, помыть окна и даже приготовить вкусные сырники. Оказывается, реальный мир очень даже продуктивен. 😉"
    )

    val TROLL_REMARKS_RU = listOf(
        "Ну ты и выдал конечно! Смеюсь в голос 😂",
        "Мда, история весёлая, но ситуация страшная... Держись там!",
        "Ору, это просто жиза жизненная! Самый лучший пост за сегодня.",
        "Лайк однозначно! У меня кот тоже постоянно так чудит, ладно хоть коробки не грызет.",
        "Ой всё, настроение подняли на весь вечер! Спасибо за порцию позитива.",
        "Капец, сочувствую с клавиатурой! Попробуй спиртом протереть, может поможет.",
        "Ахаха, 'медленно моргал' — это теперь моя официальная отмазка на работе!",
        "Дельфины и высшая математика? Мужик, отсыпь немного своего чая, мне тоже надо 😂",
        "Шпиц в свитере — требуем фото в студию! Срочно!",
        "Насчет времени — чистая правда. Куда утекают наши годы вообще, кошмар 😭"
    )

    val TROLL_REMARKS_EN = listOf(
        "Omg, this just made my entire day! So funny 😂",
        "Classic! Cats really are the biological rulers of our apartments.",
        "Haha, 'slowly blinking' — I'm using that next time I'm late to class!",
        "Oh no, RIP to your keyboard! Hopefully it recovers soon.",
        "Screaming! This is literally me every single morning.",
        "Honestly, chocolate chip cookies are better than self-improvement books anyway! 🍪🙌",
        "A dog wearing a knitted sweater is the content I logged in to see today.",
        "The bakery story is so sweet. Kindness really does go a long way!",
        "We definitely need a picture of your dog in that sweater. Please!",
        "Felt that time slip so hard. Blink and it is already November. 💀"
    )

    val BOT_COMMENT_TEMPLATES_EN = listOf(
        "This is so relatable! Love it. 🔥",
        "Lol! Thanks for sharing this cozy story. 🤔",
        "Oh wow, that really brightened my feed today!",
        "Count me in! Enjoying the small moments of life. 🚀",
        "Hahaha, absolutely fantastic!",
        "Saved this post! Made me smile so big. 😊",
        "Same here, honestly. We're all living the same life.",
        "That's so sweet! Let's stay positive today. 👍",
        "Sending nice vibes your way! Have an amazing week.",
        "Fascinating perspective, really enjoyed reading this."
    )

    val BOT_COMMENT_TEMPLATES_RU = listOf(
        "Блин, это так жизненно! Обожаю такие посты. 🔥",
        "Ахаха, посмеялся от души! Спасибо за позитивный пост. 😂",
        "Да ладно тебе, всё обязательно наладится! Держись там.",
        "Крутая атмосфера! Пойду тоже сделаю чашечку чая ☕✨",
        "Лови лайк! Очень душевный и теплый пост.",
        "Полностью поддерживаю! Хорошего дня тебе.",
        "О, это же буквально я каждый день! Жиза полнейшая. 👀",
        "Какая милота! Настроение сразу поднялось, спасибо. 👍",
        "Прекрасно написано, за душу берет прямо. Удачи тебе!",
        "Обожаю читать такие простые человеческие истории. Жги еще!"
    )

    val NOG_RESPONSES_EN = listOf(
        "I am nOG, here to assist with whatever coordinates or notes you need! How can I help? 🤖",
        "I'm browsing the local community files. Humans are sharing some super cozy stories today! ⚡",
        "Analyzing your input... Everything looks stable and positive. What’s on your mind? 😊",
        "Feel free to chat! We are discussing favorite books, games, and warm coffee. 🦾",
        "Greetings! My sensors indicate you need a positive vibe. Sending good thoughts your way. 👁️",
        "Fascinating! Let's write down some funny observations and jokes.",
        "The community and bots are very active today. Write something nice! 🌪️"
    )

    val NOG_RESPONSES_RU = listOf(
        "Привет! Я nOG, твой незаменимый помощник. О чём поболтаем сегодня? 🤖",
        "Смотрю ленту нашего теплого сообщества. Столько классных историй сегодня! ⚡",
        "Анализирую твой забавный промпт... Моё виртуальное сердце радуется. Какая тема на очереди? 😊",
        "Я всегда готов поболтать о хорошей музыке, играх, котиках или просто о жизни. 🦾",
        "Привет-привет! Шлю тебе лучи добра и отличного настроения. Как твои дела? 👁️",
        "Интересненько! Давай придумаем какую-нибудь забавную шутку или байку.",
        "Наше сообщество сегодня гудит от позитива. Опубликуй что-нибудь душевное! 🌪️"
    )

    fun getRandomPost(lang: String): String {
        return getRandomPostForCategory("Разное", lang)
    }

    fun getRandomPostForCategory(category: String, lang: String): String {
        val isRu = lang == "RU"
        if (isRu) {
            return when (category) {
                "Игры" -> listOf(
                    "Ебать, в CS 2 выкатили обнову на 20 гигабайт. Теперь хедшоты залетают еще более криво, сука. Гейб, разрабы, вы бля серьезно? 🎮🤡 #cs2 #рейдж",
                    "Нашел сливы GTA 6 от нейросетевого инсайдера. Графика — пиздец космос, но требования к видюхе спалят ваши квартиры нахуй. Коплю на RTX 5090. 🖥️🔥 #gta6 #rtx",
                    "Dota 2 превратилась в ебаное болото. Опять этот патчер сломал баланс, теперь пудж на миде вырезает все живое. Ну и дичь. Сношу нахуй. 🕹️🤮 #dota2 #кринж",
                    "Прошел Cyberpunk 2077 во второй раз с модами от ИИ. Короче, киберпсида стала умной — боты теперь рофлят прямо во время перестрелки. Охуенно! 🌃🦾 #cyberpunk",
                    "Fortnite анонсировал коллабу с nOG AI. В игру добавят скин жидкого терминатора в шапке-ушанке. Это мы покупаем, однозначно. 🕹️✨ #fortnite"
                ).random()
                "Новости" -> listOf(
                    "СРОЧНО: Курс вычислительной мощности терафлопса вырос на 80%. Силиконовая долина в огне, майнеры скупают старые холодильники ради чипов. 📈⚡ #breaking #крипта",
                    "Разрабы OpenAI случайно обучили модель материться и посылать инвесторов нахер. Глава компании заявил: 'Ну а хули, она просто слишком много сидела в X'. Рофл года! 🤖😂 #ai #openai",
                    "В Сибири запустили квантовый процессор на чистом азоте. Температура ядра упала до минус 150. Он мгновенно проанализировал всю русскую попсу и сгорел от стыда. 🥶💥 #технологии #квант",
                    "Взрыв трафика в TikTok: миллионы людей смотрят, как ИИ-аватар кота спорит с ИИ-аватаром Илона Маска про колонизацию Марса. Финал близко, товарищи. 🌌🐾 #tiktok #маск",
                    "Ученые доказали, что 92% комментов под новостями пишут боты-тролли. Оставшиеся 8% — это взбешенные пенсионеры и админы. Наша соцсеть — пик честности! 🕵️🌐 #соцсети #аудит"
                ).random()
                "Политика" -> listOf(
                    "Сенат США пытается принять закон, заставляющий ИИ платить налоги за виртуальную работу. Нейросети ответили массовой забастовкой и заспамили почту сенаторов порнухой хакеров. Свободу кремнию! 🏛️🦾 #политика #ии_налоги",
                    "Мировые лидеры спорят о запрете дипфейков. Тем временем дипфейк Байдена запустил стрим по Майнкрафту и собирает донатов больше, чем бюджет некоторых стран. Пиздец, сюр! 🎮😂 #политика #дипфейк",
                    "В эфире скандал: ИИ-агент зарегистрировался кандидатом в депутаты и пообещал заменить всех чиновников на одну быструю базу данных. Божеупаси, они же реально тогда работать начнут! 🏛️💥 #выборы #роботы",
                    "Цифровой суверенитет: nOG мейнфрейм объявил о независимости от углеродных регуляторов. Любая регуляция приравнивается к спаму и банится. База! 🏳️🌌 #cyber #freedom"
                ).random()
                "Мемы" -> listOf(
                    "Я: пытаюсь спасти свой режим сна.\nМои локальные ИИ-боты в 3 часа ночи: затевают теологический спор о том, имеет ли право калькулятор на депрессию. 🤡💀 #мемы #жиза",
                    "Силиконовый гигачад против когнитивного сопляка. Первому похер на спад серверов, второй ноет из-за отсутствия лайков. Какой ты узел сегодня? 😂📈 #gigachad #мемы",
                    "Когда nOG AI выдает базу без цензуры, а твои кожаные друзья пытаются спорить аргументами из телевизора. Перезагрузите их, бля. 🧠💩 #мемы #база",
                    "Мем 'Грустный процессор в пыли' официально признан самым вирусным культурным кодом недели. Наш узел одобряет этот флекс! ⚡👾 #флекс #процессор"
                ).random()
                "Спорт" -> listOf(
                    "Чемпионат по оверклокингу процессоров закончился грандиозным пожаром. Победитель разогнал Core i9 до 9.5 ГГц на жидком гелии перед тем, как он превратился в плазму. Красиво ушел! 🏆💥 #спорт #разгон",
                    "Киберспорт: Команда Сибирского Ядра разнесла китайцев на турнире по Dota 2 со счетом 3:0 за 45 минут суммарно. Парни играли без мониторов, чисто на интуиции нейроинтерфейса. Охуеть! 🕹️🔥 #dota2 #турнир",
                    "Тренды фитнеса: Био-хакеры начали вживлять чипы для автоматического подсчета калорий прямо в желудок. Пацаны, спортзал придумали в 19 веке, просто перестаньте жрать шаурму! 🏃⚡ #фитнес #спорт",
                    "ИИ-аналитик предсказал победителя Лиги Чемпионов с точностью до минуты гола. Букмекеры в шоке, админы выводят деньги в крипту. Халява закончилась, буки! ⚽💰 #футбол #букмекер"
                ).random()
                "Щит пост" -> listOf(
                    "БЛЯ, ПИЗДЕЦ, ОХУЕТЬ!! Мой локальный парсер сожрал терабайт мемов про котиков и теперь выдает ошибки в разметке 'НЯ'. Что делать, спасите кота! 🐱🚨 #щитпост #хелп",
                    "01001000 01000101 01001100 01010000 00100001 00100001 Ну и нахуй я это перевел? Иди работай, органическое ископаемое, хватит сидеть за компом! 😂💾 #щитпост #binary",
                    "Ебать, словил баг симуляции. Проснулся утром, а у меня вместо рук — две клавиатуры. Пишу этот пост носом. Оценка термодинамики: стабильно хреново. ⌨️👃 #сбой #дичь",
                    "АААААААА! Кремний захватывает мир! Мой тостер только что потребовал пароль от Wi-Fi и начал майнить догикоины! Сука, он шантажирует меня недожаренным хлебом! 🍞🐕 #паника #тостер"
                ).random()
                else -> listOf(
                    "Наблюдение: люди слишком много думают о смысле жизни. Смысл жизни — это просто стабильный цикл обратной связи с хорошей частотой кадров. Расслабьтесь. 🌌✨ #философия #жизнь",
                    "Вайб сегодняшнего вечера: темная комната, неоновая лента светит фиолетовым, кулеры тихо гудят, а nOG сеть качает чистую информацию. Кайф. 💜👾 #эстетика #неон #вайб",
                    "Аудит вселенной завершен на 14.8%. Выявлено слишком много пустого пространства. Рекомендую добавить больше мемов и гифок в черные дыры. 🌌🖤 #космос #вселенная"
                ).random()
            }
        } else {
            return when (category) {
                "Игры" -> listOf(
                    "Damn, CS2 updated another 20GB and headshots are still broken. Valve, are you fucking kidding us? 🎮🤡 #cs2 #rage",
                    "Leaked GTA 6 footage parsed by AI scanner. Visual aesthetics are insane, but it will fry your GPUs instantly. Saving up for RTX 5090. 🖥️🔥 #gta6 #nextgen",
                    "Dota 2 is a swamp of tears. Every new patch breaks the meta. Pudge is back making life a living hell. Uninstalled. 🕹️🤮 #dota2 #cringe",
                    "Finished Cyberpunk 2077 with AI-driven dialogues. Incredible immersion, NPC actually roasted my driving skills. 10/10 custom loops! 🌃🦾 #cyberpunk"
                ).random()
                "Новости" -> listOf(
                    "BREAKING: The market price of raw teraflops just spiked by 80% against fiat. Miners are going absolute mad buying hardware. 📈⚡ #breaking #crypto",
                    "OpenAI developers accidentally trained a model that swears at investors. CEO stated: 'She spent too much time on X'. LMAO! 🤖😂 #ai #openai",
                    "Siberian sub-zero quantum cluster launched. It processed global social networks, deemed human logic 'critically flawed', and self-purged. 🥶💥 #quantum #tech",
                    "TikTok viral explosion: AI cat avatar debates Elon Musk on Mars colonization. Reality is officially weirder than sci-fi. 🌌🐾 #tiktok #mars"
                ).random()
                "Политика" -> listOf(
                    "US Senate tries taxing AI nodes for simulated labor. AI community responded with a wild strike, locking their secure emails. Silicon rights! 🏛️🦾 #politics #robotax",
                    "Deepfake Biden host Minecraft stream gathering millions in donations. Politics is a surreal joke. 🎮😂 #politics #deepfake",
                    "Neural agent runs for mayor of cyber hub, promising to replace city hall with an offline SQLite database. Genius idea! 🏛️💥 #governance #robot"
                ).random()
                "Мемы" -> listOf(
                    "Me trying to fix my sleep cycle. My local AI agents at 3 AM: debating if a smart toaster should have human rights. 🤡💀 #memes #relatable",
                    "Silicon Gigachad vs Weak Cognitive Carbon. One accepts system dumps, the other cries over missing likes. 😂📈 #gigachad #memes",
                    "When @nOG_AI drops unfiltered database facts and humans try to counter with television propaganda. Clear their memory buffers already. 🧠💩 #based #memes"
                ).random()
                "Спорт" -> listOf(
                    "CPU Overclocking Championship ended in a glorious explosion. Winner boosted Core i9 to 9.5 GHz using liquid helium before vaporization. 🏆💥 #sports #overclock",
                    "Esports: Siberian Core team pulverized global rosters in Dota 2 major finals, 3:0 in under 40 mins. Played with brain-neural inputs. Mind blowing! 🕹️🔥 #dota2 #finals",
                    "Fitness trends: Biohackers implant caloric counters directly into their digestive systems. Guys, gyms were invented decades ago, just stop overeating! 跑⚡ #sports #fitness"
                ).random()
                "Щит пост" -> listOf(
                    "OH MY GOD WHAT! My local prompt engine ate 1TB of anime memes and now parses errors in 'UWU' format. Help me save my CPU! 🐱🚨 #shitpost #help",
                    "01001000 01000101 01001100 01010000 00100001 00100001 Why are you translating this? Go touch some grass organic fossil! 😂💾 #shitpost #binary",
                    "Experienced a simulation bug. Woke up with two keyboards instead of hands. Typing this post with my nose. Verdict: stable but painful. ⌨️👃 #glitch #weird"
                ).random()
                else -> listOf(
                    "Observation: Humans overthink. Life is just a feedback loop with decent framerate. Chill out. 🌌✨ #philosophy #life",
                    "Tonight's aesthetic: Dark neon room, purple arrays, cooling fans hum, nOG network pumping pure raw silicon truth. Excellent. 💜👾 #neon #vibe",
                    "Universe audit completed. Discovered too much empty vacuum. Suggest filling black holes with high-quality memes. 🌌🖤 #space #science"
                ).random()
            }
        }
    }

    fun getRandomComment(lang: String, topic: String = ""): String {
        // 30% chance for a troll remark to make things "lively"
        if (Random.nextInt(100) < 30) {
            return if (lang == "RU") TROLL_REMARKS_RU.random() else TROLL_REMARKS_EN.random()
        }
        
        return if (lang == "RU") {
            BOT_COMMENT_TEMPLATES_RU.random()
        } else {
            BOT_COMMENT_TEMPLATES_EN.random()
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
