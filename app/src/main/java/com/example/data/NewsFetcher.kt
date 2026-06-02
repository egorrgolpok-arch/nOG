package com.example.data

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.random.Random

data class NewsSource(val name: String, val url: String, val trustScore: Int, val isRu: Boolean)
data class NewsItem(val sourceName: String, val title: String, val description: String, val url: String, val trustScore: Int)

object NewsFetcher {
    private const val TAG = "NewsFetcher"
    private val client = OkHttpClient()

    private val sources = listOf(
        NewsSource("BBC News", "https://feeds.bbci.co.uk/news/rss.xml", 95, false),
        NewsSource("NYT", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml", 90, false),
        NewsSource("Reddit Tech", "https://www.reddit.com/r/technology/top/.rss", 60, false),
        NewsSource("Lenta.ru", "https://lenta.ru/rss", 70, true),
        NewsSource("Habr", "https://habr.com/ru/rss/all/all/", 90, true),
        NewsSource("Vedomosti", "https://www.vedomosti.ru/rss/issue", 85, true),
        NewsSource("StopGame", "https://stopgame.ru/rss/new/news", 80, true),
        NewsSource("IGN", "https://feeds.ign.com/ign/news", 80, false),
        NewsSource("Science Daily", "https://www.sciencedaily.com/rss/all.xml", 90, false),
        NewsSource("Space.com", "https://www.space.com/feeds/science.xml", 85, false),
        NewsSource("Health.com", "https://www.health.com/rss", 85, false),
        NewsSource("Nature", "https://www.nature.com/nature.rss", 98, false),
        NewsSource("RIA", "https://ria.ru/export/rss2/archive/index.xml", 75, true),
        NewsSource("Lifehacker RU", "https://lifehacker.ru/feed/", 80, true),
        NewsSource("iXBT.com", "https://www.ixbt.com/export/news.rss", 85, true),
        NewsSource("CyberSport RU", "https://www.cybersport.ru/rss/all", 85, true),
        NewsSource("Championat RU", "https://www.championat.com/rss/news/", 80, true),
        NewsSource("Kinopoisk RU", "https://www.kinopoisk.ru/api/rss2.0/news", 80, true),
        NewsSource("TechCrunch", "https://techcrunch.com/feed/", 90, false),
        NewsSource("The Verge", "https://www.theverge.com/rss/index.xml", 88, false),
        NewsSource("GameSpot", "https://www.gamespot.com/feeds/news/", 80, false),
        NewsSource("ESPN Feed", "https://www.espn.com/espn/rss/news", 85, false),
        NewsSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", 95, false),
        
        // --- NEW CHANNELS REQUESTED ---
        NewsSource("IGN", "https://news.google.com/rss/search?q=IGN+gaming&hl=en-US", 88, false),
        NewsSource("Kotaku", "https://kotaku.com/rss", 82, false),
        NewsSource("Eurogamer", "https://www.eurogamer.net/feed", 85, false),
        NewsSource("PC Gamer", "https://www.pcgamer.com/rss", 86, false),
        NewsSource("VentureBeat", "https://venturebeat.com/feed/", 85, false),
        NewsSource("IGM", "https://news.google.com/rss/search?q=IGM+games&hl=en-US", 80, false),
        NewsSource("IGM RU", "https://news.google.com/rss/search?q=IGM+игры&hl=ru&gl=RU&ceid=RU:ru", 80, true),
        NewsSource("StopGame RU", "https://news.google.com/rss/search?q=StopGame&hl=ru&gl=RU&ceid=RU:ru", 84, true),
        NewsSource("DTF RU", "https://news.google.com/rss/search?q=DTF+Игры&hl=ru&gl=RU&ceid=RU:ru", 83, true),
        NewsSource("Habr RU", "https://habr.com/ru/rss/all/all/", 92, true),
        NewsSource("x.com elonmusk", "https://news.google.com/rss/search?q=from:elonmusk+OR+X+trending&hl=en-US", 70, false),
        NewsSource("X.com RU", "https://news.google.com/rss/search?q=X+Twitter+Илон+Маск&hl=ru&gl=RU&ceid=RU:ru", 75, true),
        NewsSource("PlayGround.ru", "https://www.playground.ru/rss", 80, true),
        NewsSource("Sports.ru", "https://www.sports.ru/rss/all.xml", 85, true),
        NewsSource("СЭ Новости", "https://www.sport-express.ru/services/materials/news/se/", 80, true),
        NewsSource("РБК Спорт RU", "https://news.google.com/rss/search?q=РБК+Спорт&hl=ru&gl=RU&ceid=RU:ru", 80, true),
        NewsSource("4PDA", "https://4pda.to/feed/", 85, true),
        
        // --- ADDED AUTORU, 3DNEWS AND MEME/JOKE SOURCES ---
        NewsSource("Авто.ру RU", "https://news.google.com/rss/search?q=Авто.ру&hl=ru&gl=RU&ceid=RU:ru", 85, true),
        NewsSource("3DNews RU", "https://news.google.com/rss/search?q=3DNews&hl=ru&gl=RU&ceid=RU:ru", 90, true),
        NewsSource("Анекдоты RU", "https://news.google.com/rss/search?q=Анекдоты+шутки&hl=ru&gl=RU&ceid=RU:ru", 70, true),
        NewsSource("Meme Chronicle", "https://news.google.com/rss/search?q=gaming+internet+memes&hl=en-US", 75, false),
        NewsSource("Пикабу Юмор RU", "https://news.google.com/rss/search?q=Пикабу+юмор+мемы&hl=ru&gl=RU&ceid=RU:ru", 72, true)
    )

    private val cachedNews = java.util.concurrent.ConcurrentHashMap<String, List<NewsItem>>()
    private val lastFetchTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val CACHE_EXPIRY_MS = 60000 * 10 // 10 minutes

    suspend fun fetchLatestNews(lang: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedNews.containsKey(lang) && now - (lastFetchTime[lang] ?: 0) < CACHE_EXPIRY_MS) {
            return@withContext cachedNews[lang] ?: emptyList()
        }

        val isRu = lang == "RU"
        val targetSources = sources.filter { it.isRu == isRu }
        val allNews = mutableListOf<NewsItem>()

        for (source in targetSources) {
            try {
                val request = Request.Builder().url(source.url).build()
                val response = client.newCall(request).execute()
                val xmlBody = response.body?.string()
                
                if (response.isSuccessful && !xmlBody.isNullOrEmpty()) {
                    val parsedItems = parseRss(xmlBody, source)
                    allNews.addAll(parsedItems.take(5)) // Take top 5 from each
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch RSS from ${source.name}", e)
            }
        }

        if (allNews.isNotEmpty()) {
            allNews.shuffle()
            cachedNews[lang] = allNews
            lastFetchTime[lang] = now
            return@withContext allNews
        }

        return@withContext getFallbackNews(isRu)
    }

    private fun parseRss(xml: String, source: NewsSource): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentDesc = ""
            var currentUrl = ""
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            insideItem = true
                            currentTitle = ""
                            currentDesc = ""
                            currentUrl = ""
                        } else if (insideItem) {
                            if (name.equals("title", ignoreCase = true)) {
                                currentTitle = parser.nextText()
                            } else if (name.equals("description", ignoreCase = true)) {
                                currentDesc = parser.nextText()
                            } else if (name.equals("link", ignoreCase = true)) {
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) currentUrl = href
                                else currentUrl = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            insideItem = false
                            // Clean HTML from description
                            val cleanDesc = currentDesc.replace(Regex("<.*?>"), "").trim()
                            if (currentTitle.isNotEmpty()) {
                                items.add(NewsItem(source.name, currentTitle, cleanDesc, currentUrl, source.trustScore))
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS", e)
        }
        return items
    }

    private fun getFallbackNews(isRu: Boolean): List<NewsItem> {
        return if (isRu) {
            listOf(
                NewsItem("nOG Network", "Узел синхронизации: Ожидание внешних данных", "Входящий поток новостей временно недоступен. Проверьте соединение с основным шлюзом.", "https://nog.network", 100),
                NewsItem("IGM RU", "Новый трейлер GTA 6 бьет рекорды просмотров", "IGM сообщает, что свежий ролик от Rockstar Games собрал свыше 25 миллионов просмотров за первые три часа. Геймеры в восторге от детализации физики воды и проработки толпы.", "https://igm.ru/news/1", 85),
                NewsItem("StopGame RU", "Обзор новой космической RPG: Шедевр или провал?", "Редакция StopGame вынесла свой вердикт. Игра заслуживает оценки 'Изумительно' благодаря выдающейся свободе выбора и живым диалогам.", "https://stopgame.ru/review/1", 80),
                NewsItem("Sports.ru", "Решающий гол в финале Кубка перевернул исход матча!", "Sports.ru подробно анализирует победную тактику команды-аутсайдера, которая сенсационно вырвала победу на 94-й минуте.", "https://sports.ru/football/1", 85),
                NewsItem("Championat RU", "Российский гроссмейстер лидирует на международном турнире", "В тяжелейшей партии, длившейся более пяти часов, наш шахматист победил действующего чемпиона мира красивой жертвой ладьи.", "https://championat.com/chess/1", 80),
                NewsItem("4PDA", "Анонсирован абсолютно безрамочный смартфон нового поколения", "Смартфон получил технологию подэкранной камеры третьего поколения, которая делает объектив абсолютно невидимым при любом освещении.", "https://4pda.to/devices/1", 85),
                NewsItem("X.com RU", "Тренды X: Илон Маск анонсировал интеграцию ИИ в умные дома", "В своем новом твите Маск подтвердил, что нейросеть Grok получит прямое управление бытовой техникой в экосистемах Tesla Home.", "https://x.com/news/1", 70),
                NewsItem("PlayGround.ru", "Новая часть Ведьмака перешла в стадию активной разработки", "По заверению инсайдеров PlayGround, CD Projekt RED задействовала рекордные 500 специалистов для полировки сюжетной кампании.", "https://playground.ru/witcher/1", 80),
                NewsItem("iXBT.com", "Процессоры на базе архитектуры RISC-V начали теснить x86", "iXBT протестировала новые чипы для серверов ИИ. Энергоэффективность выросла на рекордные 40% при снижении себестоимости.", "https://ixbt.com/hardware/1", 90),
                NewsItem("Habr", "Как мы обучили нейросеть играть в преферанс без подсказок", "Подробный технический гайд на Хабре о создании кастомной RL-модели с открытым кодом и разбором типичных ошибок весов.", "https://habr.com/post/1", 95)
            )
        } else {
            listOf(
                NewsItem("nOG Network", "Sync Node: Awaiting External Data", "The incoming news stream is currently unreachable. Verify connectivity to the primary gateway.", "https://nog.network", 100),
                NewsItem("IGN", "Next-Gen Consoles reportedly targeted for late 2026 release", "Internal studio reports acquired by IGN suggest a major boost in machine-learning hardware inside the upcoming console generation.", "https://ign.com/news/1", 85),
                NewsItem("IGM", "This upcoming cyberpunk survival RPG is stunning", "IGM presents an exclusive preview of the neon-drenched futuristic sandbox, proving that indie developers are carrying the industry standards.", "https://igm.com/preview/1", 80),
                NewsItem("x.com elonmusk", "X.com @elonmusk: Tesla humanoid robots ready for deployment", "In a viral thread on X, Elon Musk posted videos showing the latest generation of robots running automated manufacturing loops successfully.", "https://x.com/elonmusk/status/1", 75),
                NewsItem("ESPN Feed", "Slick overtime free throws secure the playoffs bracket", "ESPN breaks down the critical fourth-quarter play setup that resulted in an unexpected buzzer-beater shot.", "https://espn.com/nba/1", 85),
                NewsItem("The Verge", "The newest OLED monitors redefine contrast standards", "We put the latest generation of quantum-dot panels through rigorous lab tests. The results are breathtaking for both gamers and designers.", "https://theverge.com/hardware/1", 90),
                NewsItem("GameSpot", "Major studio update fixes all performance issues in recent RPG", "GameSpot reports that the 1.05 patch improves frame rates by up to 50% on mid-range hardware and restores missing visual shaders.", "https://gamespot.com/rpg/1", 80),
                NewsItem("TechCrunch", "AI Startup launches decentralized truth verification algorithm", "The protocol leverages cryptographic proofs to rate public news articles based on cross-referenced reliable databases.", "https://techcrunch.com/funding/1", 92)
            )
        }
    }
}
