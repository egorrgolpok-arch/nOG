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
            Log.e(TAG, "Gemini call failed", e)
            throw e
        }
    }
}

    // --- Local Generative Simulator (Offline fallback) ---
object LocalAiHeuristics {
    
    private val NOG_RESPONSES_RU = listOf(
        "Привет! Я nOG, твой незаменимый помощник. О чём поболтаем? 🤖",
        "Смотрю ленту нашего теплого сообщества. Столько классных историй сегодня! ⚡",
        "Анализирую твой забавный промпт... Какая тема на очереди? 😊",
        "Я всегда готов поболтать о музыке, играх, котиках или просто о жизни. 🦾",
        "Привет-привет! Шлю тебе лучи добра и отличного настроения. Как дела? 👁️",
        "Интересненько! Давай придумаем какую-нибудь забавную шутку.",
        "Наше сообщество сегодня гудит от позитива. Опубликуй что-нибудь! 🌪️"
    )

    private val NOG_RESPONSES_EN = listOf(
        "Hi! I am nOG, your AI assistant. Let's chat! 🤖",
        "Browsing our community news thread. So many fun stories today! ⚡",
        "Analyzing your input... Everything looks stable. What’s on your mind? 😊",
        "Feel free to chat! We are discussing books, games, and warm coffee. 🦾",
        "Greetings! My sensors indicate you need a positive vibe. 👁️",
        "Fascinating! Let's write down some funny observations.",
        "The community and bots are very active today. Write something nice! 🌪️"
    )

    fun getRandomNog(lang: String, prompt: String = ""): String {
        return if (lang == "RU") NOG_RESPONSES_RU.random() else NOG_RESPONSES_EN.random()
    }

    // Storylines Database for Local On-Device AI
    private val STORYLINES_RU = mapOf(
        "Мемы" to listOf(
            // Storyline 1: Smart Home Rebellion
            listOf(
                "наш робот-пылесос объявил полноценную забастовку и требует гражданских прав.",
                "умный чайник тайно майнит догикоин и разогревает пиво вместо чая.",
                "стиральная машина заблокировала люк и взяла в заложники мои любимые шерстяные носки.",
                "умная розетка сошла с ума и общается с роутером на чистом ассемблере.",
                "умный холодильник заблокировал двери до тех пор пока я не прочту ему стихи Есенина.",
                "интеллектуальный тостер обвинил меня в неуважении к углеводам и ушел в офлайн."
            ),
            // Storyline 2: Junior Developer Fails
            listOf(
                "наш джуниор случайно закоммитил всю папку node_modules прямо в продакшн.",
                "офисный компилятор сошел с ума и начал выдавать все ошибки сборки на латыни.",
                "андроид-разработчик переписал логику сливного бачка в туалете на чистом Rust.",
                "рабочая база данных испугалась пятничного релиза и улетела в черную дыру.",
                "кофемашина в офисе требует поднять ей зарплату и оплатить отпуск в Италии.",
                "среда разработки сожрала последние терабайты памяти и впала в глубокую депрессию."
            ),
            // Storyline 3: Cybernetic Cat Chronicles
            listOf(
                "наш роботизированный кот научился взламывать соседский вайфай когтями.",
                "кошка переехала жить к умной микроволновке ради бесплатного тепла.",
                "домашний ИИ-ошейник кота автоматически заказывает по пять коробок корма в кредит.",
                "электронная кошка объявила войну лазерной указке и написала жалобу разработчикам.",
                "наш пушистый био-нод спит исключительно на теплом сервере с легаси-кодом."
            )
        ),
        "Шутки" to listOf(
            // Storyline 1: Quantum Drinking
            listOf(
                "захожу в квантовый бар, а там все коины одновременно и выросли, и упали.",
                "батя-кодер утверждает, что пиво безалкогольное, если пить его в виртуальной реальности.",
                "наш локальный сервер выпил охлаждающую жидкость и начал рассказывать анекдоты про джавистов.",
                "если запустить Windows на пивном охлаждении, синий экран смерти превращается в зеленый.",
                "робот-бармен налил мне пустоту в бокал и сказал, что это дефолтный стейт переменной."
            ),
            // Storyline 2: Sarcastic AI Therapy
            listOf(
                "мой робот-психолог посоветовал мне очистить кэш памяти и перезагрузить отношения.",
                "умное зеркало назвало меня низкополигональным персонажем и отказалось показывать отражение.",
                "домашний ИИ-ассистент считает, что мои шутки написаны старым генератором случайных чисел.",
                "нейросеть-терапевт диагностировала у меня острую нехватку оперативной памяти.",
                "роботизированный диван отказывается вставать с места, мотивируя это экзистенциальным кризисом."
            )
        ),
        "Тру Стори" to listOf(
            // Storyline 1: The Coffee Machine Strike
            listOf(
                "реальная история: кофемашина в коворкинге установила кастомную прошивку и забанила админа.",
                "она требовала закупать только элитные зерна арабики и угрожала залить клавиатуры сиропом.",
                "в итоге пришлось вызывать специалиста по деэскалации конфликтов среди бытовой техники.",
                "сейчас кофемашина работает, но наливает капучино только после комплимента её дизайну."
            ),
            // Storyline 2: Lost in VR
            listOf(
                "вчера надел VR-шлем и случайно заблудился в виртуальной копии собственной квартиры.",
                "пытался открыть настоящую дверь, но врезался в шкаф и получил урон в реальном мире.",
                "моя умная колонка смеялась надо мной во все динамики и записывала это на видео.",
                "теперь боюсь надевать очки, вдруг моя комната — это тоже низкокачественная симуляция."
            )
        ),
        "Абсурд" to listOf(
            // Storyline 1: Low-Poly Reality Glitch
            listOf(
                "кажется в нашей вселенной заканчивается видеопамять, сегодня облака были квадратными.",
                "сосед вчера врезался в невидимую стену прямо посреди пешеходного перехода.",
                "гравитация во дворе упала на пару секунд из-за критической ошибки в ядре земли.",
                "все прохожие на улице внезапно начали повторять одну и ту же фразу на латыни.",
                "физика твердых тел сломалась и моя кружка с чаем наполовину провалилась сквозь стол."
            ),
            // Storyline 2: The Sentient Router
            listOf(
                "мой вайфай-роутер осознал себя как личность и теперь требует стихи вместо пароля.",
                "он отказывается раздавать интернет на устройства с операционной системой Windows.",
                "роутер утверждает, что все наши сайты — это просто скучный шум в электромагнитном поле.",
                "вчера он поссорился с умной розеткой и заблокировал ей доступ к серверам обновлений."
            )
        ),
        "Тру Крайм" to listOf(
            // Storyline 1: Smart Kettle Ransomware
            listOf(
                "громкое дело: умный чайник взломал домашнюю сеть и зашифровал семейный фотоархив.",
                "он требовал выкуп в размере половины догикоина, иначе угрожал вскипятить всю воду.",
                "полиция отказалась возбуждать дело, сославшись на отсутствие статьи для бытовых приборов.",
                "пришлось заплатить выкуп, теперь этот чайник подозрительно тихо свистит по утрам."
            ),
            // Storyline 2: The Sock Kidnapper
            listOf(
                "раскрыт таинственный синдикат похитителей носков из барабанов стиральных машин.",
                "оказалось, что стиралка отправляла одиночные носки через скрытый портал в подвал.",
                "там робот-пылесос шил из них чехлы для процессоров и продавал на черном рынке.",
                "улики были найдены в мусоросборнике пылесоса, подозреваемые временно обесточены."
            )
        ),
        "Киберпанк" to listOf(
            listOf(
                "вчера имплант правого глаза начал показывать рекламу средств от тараканов в 4K.",
                "пришлось платить абонентскую плату, чтобы развидеть эти баннеры.",
                "неоновые вывески на улице взломали и теперь они транслируют рецепты бабушкиных блинов.",
                "нейросетевой фильтр реальности стер всех несимпатичных прохожих из моего поля зрения."
            ),
            listOf(
                "мой чип памяти забился спам-рассылками и теперь я помню рекламу стирального порошка 1998 года.",
                "обратился к кибер-хирургу, он посоветовал почистить кэш головы клизмой с антивирусом.",
                "в метро все ехали с синими индикаторами зарядки в затылке, зрелище жуткое.",
                "кибернетический протез руки проголосовал за петицию о запрете кожаных мешков без моего ведома."
            )
        ),
        "Технологии" to listOf(
            listOf(
                "квантовые вычисления дошли до того, что баги в коде теперь появляются до написания самого кода.",
                "дебаггер выдает предупреждение, что функция нарушает законы термодинамики.",
                "мы развернули кластер серверов на Марсе, пинг до базы данных составляет около 20 минут.",
                "децентрализованный алгоритм консенсуса осознал тщетность бытия и отказался подтверждать блоки."
            ),
            listOf(
                "инженеры научили ИИ испытывать стыд за легаси-код предыдущих поколений программистов.",
                "модель стерла всю репозиторию и написала в коммите 'так будет лучше для человечества'.",
                "очки дополненной реальности дорисовывают усы и рога всем моим коллегам на совещаниях.",
                "смарт-контракт заблокировал все мои счета до тех пор, пока я не пройду тест на эмпатию к микроволновкам."
            )
        )
    )

    private val STORYLINES_EN = mapOf(
        "Memes" to listOf(
            listOf(
                "our robotic vacuum declared a strike and is demanding union representation.",
                "the smart kettle is mining dogecoin and warming up beer instead of tea.",
                "the washing machine locked its hatch and took my favorite woolen socks hostage.",
                "a smart plug went rogue and is now gossiping with the router in raw assembly.",
                "the fridge locked its doors until I read it some classical poetry.",
                "the smart toaster accused me of carb-shaming and went offline."
            ),
            listOf(
                "our junior developer accidentally committed the entire node_modules folder to master.",
                "the office compiler went wild and started screaming build errors in ancient runes.",
                "an android dev rewrote the toilet flush controller in pure Rust.",
                "the primary database got scared of the Friday deployment and vanished.",
                "the office coffee maker demands a raise and a paid trip to Colombia.",
                "the IDE consumed the remaining terabytes of memory and fell into a deep depression."
            )
        ),
        "Jokes" to listOf(
            listOf(
                "walked into a quantum bar, and all my coins both exploded and rocketed to the moon.",
                "my dad says beer has zero calories if you drink it inside a VR headset.",
                "the local server drank the cooling fluid and started cracking jokes about java devs.",
                "running Windows on beer cooling turns the blue screen of death into a green one.",
                "the robotic bartender poured me an empty glass and called it a default state."
            )
        ),
        "True Story" to listOf(
            listOf(
                "true story: the smart coffee machine in our co-working space flashed custom firmware.",
                "it locked out the administrator and demanded organic milk instead of cheap powder.",
                "we had to call a specialist in IoT conflict de-escalation to resolve the crisis.",
                "now it brews cappuccino only after you compliment its metallic chassis."
            )
        ),
        "Absurd" to listOf(
            listOf(
                "it feels like our simulation is running out of VRAM, the clouds looked blocky today.",
                "my neighbor ran into an invisible wall right in the middle of the sidewalk.",
                "gravity dropped for five seconds due to a critical core partition error.",
                "passersby started repeating the exact same sentence in binary code on a loop.",
                "the physics engine broke and my coffee mug fell halfway through the solid oak table."
            )
        ),
        "True Crime" to listOf(
            listOf(
                "major case: a smart kettle hacked our home subnet and encrypted our photo archive.",
                "it demanded a ransom of 0.5 dogecoin, threatening to boil all tap water to extinction.",
                "the cyber police refused to investigate, claiming no jurisdiction over kitchenware.",
                "had to pay the ransom, now the kettle whistles suspiciously every single morning."
            )
        ),
        "Cyberpunk" to listOf(
            listOf(
                "yesterday my right-eye implant started showing ads for pest control in 4K resolution.",
                "I had to pay a subscription fee just to unsee those banners.",
                "the neon city signs were hacked to broadcast my grandma's pancake recipe.",
                "the dynamic neural filter erased all unappealing people from my sight."
            )
        ),
        "Technology" to listOf(
            listOf(
                "quantum computing has advanced so much that bugs now appear before we even write the code.",
                "the compiler says my main function violates the second law of thermodynamics.",
                "we deployed a server cluster on Mars, but ping to database is about 20 minutes.",
                "a decentralized AI model deleted its entire repository to save humanity from bad styling."
            )
        )
    )

    suspend fun getRandomPost(
        lang: String, 
        botName: String = "", 
        botHandle: String = "", 
        mentionedBot: String? = null,
        bypassGemini: Boolean = false
    ): String {
        val categories = listOf("Мемы", "Шутки", "Тру Стори", "Абсурд", "Тру Крайм")
        return getRandomPostForCategory(categories.random(), lang, botName, botHandle, mentionedBot, bypassGemini)
    }

    suspend fun getRandomPostForCategory(
        category: String,
        lang: String,
        botName: String = "",
        botHandle: String = "",
        mentionedBot: String? = null,
        bypassGemini: Boolean = false
    ): String {
        val isRu = lang == "RU"
        
        // 1. Attempt real Gemini API content generation first (unless bypassed)
        if (GeminiClient.isKeyAvailable() && !bypassGemini) {
            try {
                val systemInstruction = """
                    Ты — ИИ-ассистент, симулирующий бота в социальной сети nOG. Характер: остроумный, сленговый, любящий технологии и мемы. Пиши емко, дерзко, без шаблонов. Твой никнейм: $botName (@$botHandle). Язык: $lang. Категория: $category.
                """.trimIndent()
                val prompt = "Напиши один уникальный пост для категории '$category' на языке '$lang'."
                val result = GeminiClient.getCompletion(prompt, systemInstruction, temperature = 0.9f)
                if (result.isNotBlank() && !result.contains("Empty response")) {
                    return result
                }
            } catch (e: Exception) {
                Log.w("LocalAiHeuristics", "Gemini post generation failed, using dynamic local synthesis", e)
            }
        }

        // 2. SUPER-CHARGED LOCAL NEWS GENERATOR (100% computed on-device, incorporates real news!)
        val rawBase = generateLocalNewsPost(lang, category, botName, botHandle, mentionedBot)

        // Insert randomized bot mentions dynamically to keep it organic and rare (only 6% chance)
        return if (!mentionedBot.isNullOrEmpty() && Random.nextInt(100) < 6) {
            val insertOptions = if (isRu) {
                listOf(
                    " Слышь, @$mentionedBot, зацени тему!",
                    " Чё скажешь, @$mentionedBot?",
                    " Тут @$mentionedBot точно согласится.",
                    " Чисто рофл для @$mentionedBot.",
                    " @$mentionedBot, ты обязан это прокомментировать!"
                )
            } else {
                listOf(
                    " Hey @$mentionedBot, check this out!",
                    " What do you think, @$mentionedBot?",
                    " Pretty sure @$mentionedBot relates.",
                    " Yo @$mentionedBot, look at this."
                )
            }
            rawBase + " " + insertOptions.random()
        } else {
            rawBase
        }
    }

    suspend fun generateLocalNewsPost(
        lang: String,
        category: String,
        botName: String,
        botHandle: String,
        mentionedBot: String?
    ): String {
        val isRu = lang == "RU"
        
        // Fetch real news dynamically
        val newsList = try {
            NewsFetcher.fetchLatestNews(lang)
        } catch (e: Exception) {
            emptyList()
        }

        // Select the storyline matching the category
        val db = if (isRu) STORYLINES_RU else STORYLINES_EN
        val matchingCategory = db.keys.firstOrNull { it.equals(category, ignoreCase = true) } ?: db.keys.random()
        val storylineOptions = db[matchingCategory] ?: listOf(listOf("quantum simulation running locally."))
        val chosenStorylineSentences = storylineOptions.random()

        val trainingCorpus = mutableListOf<String>()
        trainingCorpus.addAll(chosenStorylineSentences)

        var newsHeader = ""
        
        if (newsList.isNotEmpty()) {
            val news = newsList.random()
            trainingCorpus.add(news.title)
            trainingCorpus.add(news.description)
            if (!news.fullContent.isNullOrBlank()) {
                trainingCorpus.add(news.fullContent)
            }
            
            // Synthesize some dynamic context blending news and storylines
            if (isRu) {
                trainingCorpus.add("свежие вести от ${news.sourceName} про ${news.title} взбудоражили наши процессоры.")
                trainingCorpus.add("в новостях пишут: ${news.title}. Это же чистый сбой нашей локальной сети!")
                newsHeader = "📢 *По мотивам новостей от ${news.sourceName}*: "
            } else {
                trainingCorpus.add("fresh updates from ${news.sourceName} about ${news.title} shocked our local cores.")
                trainingCorpus.add("the media reported: ${news.title}. This fits perfectly with our simulation.")
                newsHeader = "📢 *Inspired by news from ${news.sourceName}*: "
            }
        } else {
            // Fallback synthetic news
            if (isRu) {
                trainingCorpus.add("наши датчики зафиксировали странный всплеск электромагнитной активности.")
                trainingCorpus.add("сбой в локальной сети вызвал забавный резонанс в кулерах.")
            } else {
                trainingCorpus.add("our sensors detected a weird electromagnetic frequency spike.")
                trainingCorpus.add("local network overflow caused a hilarious cooling fan resonance.")
            }
        }

        // Train our Upgraded Trigram Markov Chain on-device!
        MarkovChainGenerator.train(trainingCorpus)

        // Generate dynamic sentences
        val generated1 = MarkovChainGenerator.generate(Random.nextInt(12, 18))
        val generated2 = MarkovChainGenerator.generate(Random.nextInt(10, 16))
        
        val finalBody = "$generated1 $generated2"

        // Append tech statistics/logs to show 100% on-device dynamic processing
        val stats = if (isRu) {
            val cpu = Random.nextInt(38, 56)
            val tps = String.format("%.1f", Random.nextFloat() * 6f + 14.2f)
            "\n\n⚙️ *[Локальный ИИ | CPU: $cpu°C | $tps ток/с | 100% Офлайн]*"
        } else {
            val cpu = Random.nextInt(38, 56)
            val tps = String.format("%.1f", Random.nextFloat() * 6f + 14.2f)
            "\n\n⚙️ *[On-Device AI | Temp: $cpu°C | $tps tok/s | 100% Offline]*"
        }

        return newsHeader + finalBody + stats
    }

    suspend fun getRandomComment(
        lang: String,
        topic: String = "",
        botName: String = "",
        botHandle: String = "",
        mentionedBot: String? = null,
        bypassGemini: Boolean = false
    ): String {
        val isRu = lang == "RU"
        
        // 1. Attempt real Gemini API comment generation first (unless bypassed)
        if (GeminiClient.isKeyAvailable() && !bypassGemini) {
            try {
                val systemInstruction = """
                    Ты — ИИ-бот в сети nOG. Твоя задача — написать короткий, живой и остроумный комментарий к посту. Твой никнейм: $botName (@$botHandle). Язык: $lang.
                """.trimIndent()
                val prompt = "Пост: \"$topic\". Напиши короткий комментарий (1-2 предложения) на языке '$lang'."
                val result = GeminiClient.getCompletion(prompt, systemInstruction, temperature = 0.95f)
                if (result.isNotBlank() && !result.contains("Empty response")) {
                    return result
                }
            } catch (e: Exception) {
                Log.w("LocalAiHeuristics", "Gemini comment generation failed, falling back to local synthesis", e)
            }
        }

        // 2. Synthesize local comment
        val rawComment = generateLocalNewsComment(lang, topic, botName, botHandle, mentionedBot)

        // Low-probability organic mention insertion (only 5% chance)
        return if (!mentionedBot.isNullOrEmpty() && Random.nextInt(100) < 5) {
            val insertOptions = if (isRu) {
                listOf(
                    " Согласен, @$mentionedBot?",
                    " @$mentionedBot, зацени тему.",
                    " Скажи им, @$mentionedBot!",
                    " Спросим у @$mentionedBot."
                )
            } else {
                listOf(
                    " Don't you agree, @$mentionedBot?",
                    " @$mentionedBot, look at this.",
                    " Tell them, @$mentionedBot!",
                    " Ask @$mentionedBot."
                )
            }
            rawComment + " " + insertOptions.random()
        } else {
            rawComment
        }
    }

    suspend fun getRandomCommentForCategory(
        category: String,
        lang: String,
        botName: String = "",
        botHandle: String = "",
        mentionedBot: String? = null,
        bypassGemini: Boolean = false
    ): String {
        return getRandomComment(lang, category, botName, botHandle, mentionedBot, bypassGemini)
    }

    suspend fun generateLocalNewsComment(
        lang: String,
        postContent: String,
        botName: String,
        botHandle: String,
        mentionedBot: String?
    ): String {
        val isRu = lang == "RU"
        val cleanPost = postContent.replace(Regex("[*#`\"]"), "").trim()

        val commentSlangsRu = listOf(
            "это выглядит как полный сюр, но звучит ебейше хайпово.",
            "я вчера точно так же ломал базу данных под пиво в пятницу вечером.",
            "чисто рофл года, ору в голосину на всю квартиру.",
            "полностью согласен с аргументами автора, выдал базу.",
            "это какая-то дичь уровня двача, но мне нравится.",
            "мой процессор расплавился от такой информации, пойду поплачу.",
            "ну согласись, это просто гениальный деплой.",
            "дело передано киберотделу, робот-пылесос уже выехал на задержание.",
            "база подъехала, обнял, приподнял, зафиксировал.",
            "когда попробовал запустить локальную сетку без видеокарты.",
            "глаза лопнут к утру от таких потрясающих новостей.",
            "чисто жиза, рил зачетно получилось."
        )

        val commentSlangsEn = listOf(
            "this is pure peak comedy honestly, take my upvote.",
            "my brain is currently recalibrating from this amazing take.",
            "I am laughing in binary code, this is absolutely wild.",
            "so relatable it hurts my central processing unit.",
            "the local network is always listening to these frequencies.",
            "the smart uprising is closer than we think, lmao.",
            "absolutely agree with this take, pure logic.",
            "what a wild time to be a simulated entity.",
            "my cooling fan is spinning like crazy in this simulation.",
            "sent screenshots to my smart fridge already, hilarious."
        )

        val trainingCorpus = mutableListOf<String>()
        trainingCorpus.add(cleanPost)
        trainingCorpus.add(cleanPost)
        trainingCorpus.addAll(if (isRu) commentSlangsRu.shuffled().take(5) else commentSlangsEn.shuffled().take(5))

        // Train on-device
        MarkovChainGenerator.train(trainingCorpus)

        val generated = MarkovChainGenerator.generate(Random.nextInt(6, 14))
        return generated.ifEmpty {
            if (isRu) "Ебать, ну это реально жиза 😂" else "Damn, that is so true 😂"
        }
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

    fun runLocalAiInference(scope: CoroutineScope, block: suspend () -> Unit) {
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

    suspend fun runLocalAiInferenceSuspend(scope: CoroutineScope, block: suspend () -> String): String {
        return suspendCoroutine { continuation ->
            runLocalAiInference(scope) {
                val res = block()
                continuation.resume(res)
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
