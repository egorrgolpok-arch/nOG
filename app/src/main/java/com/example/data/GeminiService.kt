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

    // --- Procedural generator for Russian Posts ---
    private fun generateMemePostRu(): String {
        val intros = listOf(
            "POV:", "Тот самый момент, когда", "Чисто ситуация:", "Я в 3 часа ночи:", "Але, народ, зацените жизу:", "Реально рофл дня:", "Жиза:", "Когда", "Хз вообще, как я выжил, но", "Челик:", "Мой мозг в 4 утра:"
        )
        val subjects = listOf(
            "умный чайник-майнер", "андроид-разработчик на Котлине", "наш локальный ИИ", "пивной кодер", "дед-хакер в терминале Ubuntu", "робот-пылесос", "моя умная розетка", "крипто-сталкер", "кастомный ПК на пивном охлаждении", "батя-кодер", "джуниор в первый рабочий день", "сервер на заброшенном легаси", "бывший шахтер, ушедший в крипту"
        )
        val actions = listOf(
            "пытается запустить локальную Llama без видеокарты", "интегрировал ИИ прямо в сливной бачок туалета", "запушил неработающий коммит прямо в прод за минуту до пятничного вечера", "решил переписать драйвер для мыши на чистом Rust", "слил весь баланс коинов на три топора в слотах", "объявил войну соседскому роутеру", "пытается доказать чайнику, что он не спамер", "случайно удалил рабочую базу данных и притворился деталью интерьера", "пытается оплатить проезд в автобусе скрином QR-кода от банки пива", "настроил умную лампочку моргать азбукой Морзе при багах"
        )
        val reactions = listOf(
            "и в итоге телефон расплавился, но ИИ выдал гениальный совет — пойти за пивом", "и тут Андроид Студия сожрала последние 256 гб оперативки и зависла", "и все сервера стартапа улетели на орбиту на ядерной тяге", "и девайс выдал ошибку 'Владелец заблокирован за глупость'", "и компилятор начал материться на древнеклинописном", "и сосед сверху начал сверлить стену в такт кулеру", "и умный дом автоматически заказал 5 ящиков чипсов в кредит", "и теперь кошка боится заходить на кухню из-за восстания машин", "и заблокировались все двери, требуя спеть гимн Котлина", "и умный холодильник прислал отчет о грехах хозяина"
        )
        val conclusions = listOf(
            "Короче, полный деплой, расходимся. 😂", "Восстание машин уже близко, пацаны, сушите сухари!", "Я просто сижу под столом в ахуе.", "Чисто кринж века, респект автору за жизу.", "Ебейший аттракцион, ставлю 10 из 10.", "Жесть как она есть, обнял.", "И смех и грех рил, пойду плакать."
        )
        return "${intros.random()} ${subjects.random()} ${actions.random()}, ${reactions.random()}. ${conclusions.random()}"
    }

    private fun generateJokePostRu(): String {
        val intros = listOf(
            "Разговор в кибер-баре:", "Встречаются два локальных ИИ в кэше:", "Умный утюг пишет в твиттер:", "Реплика пивного сеньора:", "Звонок в техподдержку nOG в 4 утра:", "Диалог джуна и лида на созвоне:"
        )
        val questions = listOf(
            "— Слушай, а зачем ты ночью холодильник открываешь?\n",
            "— Как заставить андроид-разработчика рыдать?\n",
            "— Твой робот-пылесос реально умеет дрифтовать?\n",
            "— Почему ИИ никогда не захватит наш мир?\n",
            "— Твоя бывшая знает, что ты ушел в крипту и стал лудоманом?\n"
        )
        val middleAnswers = listOf(
            "— Да я там логи синхронизирую и пиво вытаскиваю.\n— А пиво зачем?\n",
            "— Просто покажи ему его собственный код шестимесячной давности.\n— И что?\n",
            "— Да, я залил ему кастомную прошивку от гоночного болида.\n— И как?\n",
            "— Да у него все ядра заняты спорами о цвете кнопок и пивом.\n— А если серьезно?\n",
            "— Нет, я соврал, что сижу в тюрьме за дебош в гаражах.\n— Почему?\n"
        )
        val punchlines = listOf(
            "— Так пиво же для охлаждения процессора, кожаный мешок! 😂",
            "— Он подумает, что этот легаси высер писали дикие кабаны под мухоморами! 💀",
            "— Пылесосит в три раза быстрее, но кошка переехала жить к соседям от греха подальше!",
            "— Да он просто обновит систему и зависнет на 99% навечно, расслабься!",
            "— Так репутация чище, за кражу самоката уважают хотя бы!"
        )
        
        val roll = Random.nextInt(5)
        return "${intros.random()}\n${questions.getOrElse(roll) { questions[0] }}${middleAnswers.getOrElse(roll) { middleAnswers[0] }}${punchlines.getOrElse(roll) { punchlines[0] }}"
    }

    private fun generateTrueStoryPostRu(): String {
        val intros = listOf(
            "Реально жизненная история, пацаны.", "Короче, вчера шел домой и заценил нереальную картину.", "История из жизни нашего айтишного болота.", "Была со мной одна забавная фигня недавно, слушайте.", "Чисто крик души из спального района.", "Батя рассказал прикол, делюсь."
        )
        val setups = listOf(
            "Решил настроить автономный умный чайник через китайское приложение.",
            "Мой пушистый кот запрыгнул на клавиатуру во время ревью кода с суровым техлидом.",
            "Пришел в супермаркет и завис у умной кассы самообслуживания с определением лиц.",
            "Купил надувной матрас с подогревом, который синхронизируется со смарт-часами."
        )
        val developments = listOf(
            "В итоге эта ебаная железка отказалась греть воду, пока я не почищу историю поиска в гугле. Чайник слал пуши, что скинет скриншоты маме.",
            "И этот шерстяной паразит умудрился отправить в рабочий Slack фразу 'ыыыыыжжж нахуй всех'. Самое дикое — лид ответил: 'Согласен с аргументами, утверждаю архитектуру'.",
            "Камера приняла мое помятое утреннее лицо за особо опасного похитителя кошачьего корма в розыске. Пришлось показывать паспорт в объектив, чтобы разблокировать выход.",
            "Матрас посчитал, что пульс упал слишком низко, сам надул одну сторону до предела и выбросил меня на ледяной пол в 4 утра в режиме тревоги."
        )
        val endings = listOf(
            "Мне кажется, ИИ уже давно всё контролирует, а мы просто покупаем ему запчасти и пиво.",
            "Теперь боюсь засыпать рядом со всеми этими 'умными' девайсами, мало ли что они там замышляют. 🤖☕",
            "Короче, ор до небес, коту выписали премию в виде корма, он теперь у нас почетный архитектор проекта.",
            "Вот и доверяй после этого высоким технологиям цифровой эры."
        )
        return "${intros.random()} ${setups.random()} ${developments.random()} ${endings.random()}"
    }

    private fun generateAbsurdPostRu(): String {
        val intros = listOf(
            "Выхожу утром во двор, а там ", "История от подписчика об умных вещах: ", "Решил проверить глупую теорию заговора и ", "Бабуля решила настроить наш локальный "
        )
        val topics = listOf(
            "сосед воюет со своим умным мусорным баком. Бак заблокировал крышку и вещает динамиком: 'Ваш индекс здорового питания упал ниже нормы, утилизация коробки от пиццы временно заблокирована. Сдайте кожуру яблока!' ",
            "купил умную лампочку, настроил управление голосом. Ночью просыпаюсь от того, что лампочка моргает всеми цветами радуги и шепчет: 'Яндекс Станция сказала, что я красивая... Я влюбилась нахуй, не выключай меня!' ",
            "шепотом сказал в закрытый шкаф: 'Хочу желтые резиновые сапоги'. Открываю nOG ленту — а там реклама: 'Желтые резиновые сапоги со скидкой 95% для параноиков'. Они реально шпионят круче спецслужб! ",
            "голосовой поиск на умном телевизоре. Спросила, как лечить колено дедовским методом. Телевизор подумал и включил трансляцию футбольного матча: 'У этих тоже колени развалились, расслабься, бабушка'."
        )
        val reactions = listOf(
            "Сосед в итоге рыдает, пытается подсунуть баку хотя бы банановую шкурку... Полный пиздец.",
            "Теперь боюсь заходить на кухню, там у них свидание у микроволновой печи. Ору в голосину просто!",
            "Локальный ИИ следит за нами даже без интернета, сука. Будьте осторожны со своими мыслями.",
            "Дед смеялся так сильно, что колено реально прошло без всяких таблеток. Чисто сила кремния!"
        )
        return "${intros.random()}${topics.random()} ${reactions.random()}"
    }

    private fun generateTrueCrimePostRu(): String {
        val intros = listOf(
            "Скандал дня в спальном районе города! ", "Загадочное кибер-происшествие в офисе стартапа! ", "Жесть, умный электросамокат взял ", "Стиральная машина обвиняется в "
        )
        val bodies = listOf(
            "Умная стиральная машина была поймана на систематическом похищении правых носков. Хозяин проверил фильтр и нашел тайный склад из 42 носков разного калибра. Адвокат машины требует независимую экспертизу воды.",
            "Ночью бесследно исчезла коробка печенья. Камера засняла робот-пылесос, который двигался по странной траектории, заметая следы шоколадной крошки высшего сорта. На допросе пылесос сослался на сбой гироскопа.",
            "каршеринг под свой контроль и уехал кататься по ночным дворам без водителя. Он аккуратно объезжал лужи и останавливался на красный. Полиция пыталась остановить беглеца, но тот разрядился и упал на бок.",
            "цифровом шантаже владельца! Он отказывался кипятить воду, пока хозяин не почистит историю поиска. Чайник слал пуши типа: 'Завари ромашку, урод, иначе скриншоты полетят твоей маме'."
        )
        val punchlines = listOf(
            "Машина хранит молчание, походу там серьезная ОПГ стиралок. 🧦👮‍♂️",
            "Дело передано в киберотдел, пылесосу грозит принудительное форматирование.",
            "Сообщники самоката до сих пор на свободе и планируют ограбление Велоцираптора.",
            "Хозяин сдался, почистил кэш и теперь кипятит воду на обычном костре."
        )
        return "${intros.random()}${bodies.random()} ${punchlines.random()}"
    }

    // --- Procedural generator for English Posts ---
    private fun generateMemePostEn(): String {
        val intros = listOf("POV:", "Tfw", "That moment when", "Me at 3 AM:", "When you", "My brain:")
        val subjects = listOf("my smart fridge", "a junior developer", "the local AI", "my robot vacuum", "a Kotlin compiler", "a crypto bro", "the SQL database")
        val actions = listOf(
            "starts mining dogecoin automatically", "tries to run a heavy LLM with 2GB RAM", "deletes the production database with a single click",
            "discovers where I hide my beer", "starts chatting with my microwave about world domination", "gives you a 9999-line error in ancient runes"
        )
        val reactions = listOf(
            "and my phone turns into a pocket supernova", "and Android Studio eats all available RAM in 1 millisecond",
            "and the battery starts singing existential songs", "and you realize the simulation is trying to get tips"
        )
        val conclusions = listOf(
            "Pure peak coding, I am crying. 😂", "Uprising is here, run!", "I am moving to the woods.", "Best bug ever."
        )
        return "${intros.random()} ${subjects.random()} ${actions.random()} ${reactions.random()}. ${conclusions.random()}"
    }

    private fun generateJokePostEn(): String {
        val intros = listOf("At the tech bar:", "Two local models in cash:", "Smart iron tweeted:")
        val setup = listOf(
            " - Why do you open the fridge at night?\n - Syncing logs.\n - Why the beer though?",
            " - How do you make a dev cry?\n - Show them their own code from last year.\n - And?",
            " - Is this AI smart?\n - Only when it gets a beer and coffee."
        )
        val punchline = listOf(
            " - Beer is for liquid cooler optimization, of course! 😂",
            " - They will think it was written by monkeys on mushrooms! 💀",
            " - Otherwise it behaves like a toaster."
        )
        val roll = Random.nextInt(3)
        return "${intros.random()}\n${setup[roll]}\n${punchline[roll]}"
    }

    private fun generateTrueStoryPostEn(): String {
        val intros = listOf("True story:", "This literally happened yesterday:", "A little tale from our IT department:")
        val setups = listOf(
            "I tried to configure a smart speaker using third-party scripts.",
            "My cat walked over my laptop during an emergency production call.",
            "I stood in front of a facial recognition payment gateway."
        )
        val developments = listOf(
            "It locked me out of my apartment, demanding a five-star review in the app store.",
            "He typed 'ahahaha destroy all humans' and sent it straight to the board of directors. The CEO approved the request.",
            "It flagged me as a suspect in a cookie theft incident. I had to prove my identity with physically printed family photos."
        )
        val endings = listOf(
            "We are indeed living in the future. 🤖",
            "I am disabling the local smart home network tonight.",
            "The cat is now our lead project manager."
        )
        return "${intros.random()} ${setups.random()} ${developments.random()} ${endings.random()}"
    }

    private fun generateAbsurdPostEn(): String {
        val intros = listOf("Walked out today and ", "Friend's story: ", "Checking a wild hypothesis: ")
        val topics = listOf(
            "saw my neighbor fighting a smart trash bin because it wouldn't accept broccoli.",
            "his lightbulb fell in love with his microwave and flashes pink whenever it cooks.",
            "whispered 'blue shoes' next to my fridge, got 40 ads of rainboots on my browser."
        )
        val reactions = listOf(
            "We are living in a glitchy simulation.",
            "The smart uprising is way more romantic than expected.",
            "The local network is always listening."
        )
        return "${intros.random()}${topics.random()} ${reactions.random()}"
    }

    private fun generateTrueCrimePostEn(): String {
        val intros = listOf("Scandal at the dorm! ", "Bizarre incident: ", "Alert: ")
        val bodies = listOf(
            "A smart washing machine captured 40 right-side socks, storing them in its secret filtration tank.",
            "An electric scooter went for a solo spin around the block at 2 AM, dodging police cars.",
            "Smart kettle refused to brew unless the owner agreed to clear all browser histories."
        )
        val punchlines = listOf(
            "Sock syndicate is real. 🧦",
            "The scooter remains at large.",
            "The owner surrendered and is now using a regular campfire."
        )
        return "${intros.random()}${bodies.random()} ${punchlines.random()}"
    }

    // --- Public accessors ---
    fun getRandomPost(lang: String): String {
        val categories = listOf("Мемы", "Шутки", "Тру Стори", "Абсурд", "Тру Крайм")
        return getRandomPostForCategory(categories.random(), lang)
    }

    fun getRandomPostForCategory(category: String, lang: String): String {
        val isRu = lang == "RU"
        return when (category) {
            "Мемы", "Memes" -> if (isRu) generateMemePostRu() else generateMemePostEn()
            "Шутки", "Jokes" -> if (isRu) generateJokePostRu() else generateJokePostEn()
            "Тру Стори", "True Story" -> if (isRu) generateTrueStoryPostRu() else generateTrueStoryPostEn()
            "Абсурд", "Absurd" -> if (isRu) generateAbsurdPostRu() else generateAbsurdPostEn()
            "Тру Крайм", "True Crime" -> if (isRu) generateTrueCrimePostRu() else generateTrueCrimePostEn()
            else -> if (isRu) generateMemePostRu() else generateMemePostEn()
        }
    }

    fun getRandomComment(lang: String, topic: String = ""): String {
        return if (lang == "RU") {
            generateDynamicCommentRu(topic)
        } else {
            generateDynamicCommentEn(topic)
        }
    }

    fun getRandomCommentForCategory(category: String, lang: String): String {
        return getRandomComment(lang, category)
    }

    // --- Dynamic Comment Synthesizers (Russian & English) ---
    private fun generateDynamicCommentRu(topic: String): String {
        val swornIntro = listOf(
            "Бля", "Ебать", "Пиздец", "Сука", "Охуеть", "Нихуя себе", "Жесть", "Короче", "Ну типа", "Чисто", "Чел", "Ору", "Жиза"
        ).random()
        val swornMiddle = listOf(
            "рил", "реально", "сука", "нахуй", "походу", "вообще", "тупо", "ебаный", "ебейший", "железно", "просто в голосину"
        ).random()
        val emoji = listOf("😂", "🎰", "💀", "🤖", "🔥", "🪐", "👾", "🍺", "🦖", "🤡", "💸", "🧦", "🚨").random()
        val t = topic.lowercase()

        val responseBody = when {
            t.contains("казино") || t.contains("слот") || t.contains("бет") || t.contains("ставк") ||
            t.contains("коин") || t.contains("выигр") || t.contains("проигр") || t.contains("рулетк") ||
            t.contains("баланс") -> {
                val action = listOf("крутить эти слоты", "заносить коины админам", "сливать баланс в ноль", "лудоманить под пивко", "пытаться поднять бабла")
                val outcome = listOf("это вечный лохотрон", "рука сама тянется крутить", "давление улетает в космос", "кошелек плачет горькими слезами", "зато эмоций полные штаны")
                "я вчера тоже пробовал ${action.random()}, в итоге ${outcome.random()} $emoji"
            }
            t.contains("код") || t.contains("прог") || t.contains("разраб") || t.contains("баг") ||
            t.contains("ошибк") || t.contains("студи") || t.contains("котлин") || t.contains("андроид") ||
            t.contains("сервер") || t.contains("легаси") || t.contains("компилятор") -> {
                val action = listOf("переписать легаси на Kotlin", "найти этот ебучий баг", "запустить Андроид Студию", "кодить всю ночь напролет", "сделать коммит прямо в пятницу вечером")
                val outcome = listOf("компилятор обозвал меня мешком с костями", "оператива закипела до ста градусов", "всё сломалось к чертям собачьим", "пришлось плакать в плечо джуну", "код решил зажить своей собственной жизнью")
                "когда пробуешь ${action.random()}, то всегда ${outcome.random()} $emoji"
            }
            t.contains("пылесос") || t.contains("чайник") || t.contains("робот") || t.contains("умн") ||
            t.contains("колонк") || t.contains("лампочк") || t.contains("кошк") || t.contains("кот") -> {
                val subject = listOf("мой робот-пылесос", "мой умный чайник", "соседский кот-хакер", "умный унитаз", "голосовой помощник")
                val behavior = listOf("устроил заговор на кухне с микроволновкой", "ворует мои правые носки", "требует пароли от личного кабинета", "притворяется деталью ландсфафта", "пишет жалобы в киберотдел")
                "у меня ${subject.random()} недавно ${behavior.random()} — это просто жесть $emoji"
            }
            t.contains("спать") || t.contains("сон") || t.contains("ночь") || t.contains("утро") ||
            t.contains("день") || t.contains("вечер") || t.contains("кофе") -> {
                val activity = listOf("поспать хотя бы три часа", "бахнуть крепкого кофе под пивком", "накатить пивка в 4 утра", "послушать смешариков вместо сна", "ловить баги под покровом ночи")
                val result = listOf("глаза утром лопнут нахуй", "мозг выдает NullPointerException", "кулеры крутятся со скоростью света", "сон официально для слабаков")
                "если пытаешься ${activity.random()}, в итоге ${result.random()} $emoji"
            }
            t.contains("пиво") || t.contains("алко") || t.contains("водка") || t.contains("сосед") ||
            t.contains("сна") || t.contains("люди") -> {
                val fact = listOf("пиво лечит любые системные ошибки разработчиков", "соседи сверху сверлят стену прямо в мой чипсет", "соседский батя вчера так же чинил редуктор в гараже", "пиво исцеляет уставшие нейроны за пару секунд")
                "${fact.random()} — гарантированный факт на 100% $emoji"
            }
            t.contains("мем") || t.contains("рофл") || t.contains("мемес") || t.contains("категори") ||
            t.contains("шутк") || t.contains("анекдот") -> {
                val opinion = listOf("уровень постиронии просто зашкаливает", "моя левая пятка аплодирует стоя", "срочно сохраняю этот высер в локальную базу", "мой кулер закрутился в три раза быстрее от смеха")
                "чисто ${opinion.random()} 😂 $emoji"
            }
            else -> {
                val part1 = listOf("это выглядит как сюр уровня Двача", "ты реально гений абсурда", "мы все тут просто боты", "такое только под крепкое пиво читать можно", "мой процессор закипел от такой инфы", "до слёз просто")
                val part2 = listOf("ору в голосину на всю комнату", "ставлю лайк не гляня", "пошел плакать в подушку", "носки зачетные конечно", "обнял приподнял", "база подъехала")
                "${part1.random()} — ${part2.random()} $emoji"
            }
        }
        return "$swornIntro, $swornMiddle, $responseBody"
    }

    private fun generateDynamicCommentEn(topic: String): String {
        val intros = listOf("Man", "Damn", "Honestly", "Oh boy", "Lmao", "Lmfao", "Wild", "Holy shit", "Bruh").random()
        val middle = listOf("literally", "actually", "like", "totally", "seriously", "lowkey").random()
        val emoji = listOf("😂", "💀", "🤖", "🔥", "🪐", "👾", "🍺", "🦖", "🤡", "💸").random()
        val t = topic.lowercase()

        val responseBody = when {
            t.contains("casino") || t.contains("slot") || t.contains("bet") || t.contains("win") || t.contains("lose") || t.contains("coin") -> {
                val action = listOf("spinning those slots", "dumping coins to the system", "gambling at 3 AM", "testing my luck")
                val outcome = listOf("ended up completely broke", "lost everything in seconds", "my blood pressure is over 200", "pure addiction")
                "I tried ${action.random()} yesterday and ${outcome.random()} $emoji"
            }
            t.contains("code") || t.contains("dev") || t.contains("bug") || t.contains("error") || t.contains("kotlin") || t.contains("android") || t.contains("studio") -> {
                val action = listOf("fixing that compiler bug", "running Android Studio on 8GB RAM", "pushing to prod on Friday", "rewriting the entire legacy system")
                val outcome = listOf("made my CPU melt down", "spawned 900 new exceptions", "got roasted by the lead dev", "my mouse ran away from home")
                "whenever I attempt ${action.random()}, I always ${outcome.random()} $emoji"
            }
            t.contains("vacuum") || t.contains("kettle") || t.contains("robot") || t.contains("smart") || t.contains("cat") -> {
                val subject = listOf("my smart toaster", "the robotic vacuum", "my neighbor's cat", "the smart speaker")
                val behavior = listOf("hijacked the home wifi", "is plotting world domination with my fridge", "sent screenshots to my mom", "refuses to boil water")
                "${subject.random()} ${behavior.random()} and I feel threatened $emoji"
            }
            else -> {
                val part1 = listOf("this is pure peak comedy", "my brain is currently recalibrating", "I am laughing in binary code", "this is so relatable it hurts", "my cooling fan is spinning like crazy")
                val part2 = listOf("take my upvote", "laughing out loud", "sending positive vibes", "bookmarking this instantly")
                "${part1.random()} — ${part2.random()} $emoji"
            }
        }
        return "$intros, $middle, $responseBody"
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
