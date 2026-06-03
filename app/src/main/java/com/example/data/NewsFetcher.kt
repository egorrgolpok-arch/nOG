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
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

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
        
        // --- EXPANDED NEWS & ARTICLE SOURCES ---
        NewsSource("IGN Gaming", "https://news.google.com/rss/search?q=IGN+gaming&hl=en-US", 88, false),
        NewsSource("Kotaku Gaming", "https://kotaku.com/rss", 82, false),
        NewsSource("Eurogamer", "https://www.eurogamer.net/feed", 85, false),
        NewsSource("PC Gamer", "https://www.pcgamer.com/rss", 86, false),
        NewsSource("VentureBeat", "https://venturebeat.com/feed/", 85, false),
        NewsSource("Game Informer", "https://news.google.com/rss/search?q=Game+Informer&hl=en-US", 80, false),
        NewsSource("Polygon", "https://www.polygon.com/rss/index.xml", 88, false),
        NewsSource("Rock Paper Shotgun", "https://www.rockpapershotgun.com/feed", 85, false),
        NewsSource("Destructoid", "https://www.destructoid.com/feed/", 80, false),
        NewsSource("GamesRadar", "https://www.gamesradar.com/feed/", 84, false),
        NewsSource("Gamasutra/GameDeveloper", "https://www.gamedeveloper.com/rss.xml", 90, false),
        NewsSource("Kotaku AU", "https://kotaku.com.au/feed/", 80, false),
        NewsSource("PCGamesN", "https://www.pcgamesn.com/feed", 83, false),
        NewsSource("VG247", "https://www.vg247.com/feed", 85, false),
        NewsSource("Gamerant", "https://gamerant.com/feed/", 80, false),
        NewsSource("Siliconera", "https://www.siliconera.com/feed/", 82, false),
        NewsSource("DualShockers", "https://www.dualshockers.com/feed/", 81, false),

        // RU Additions
        NewsSource("DTF", "https://dtf.ru/rss/all", 88, true),
        NewsSource("PlayGround.ru", "https://www.playground.ru/rss", 80, true),
        NewsSource("StopGame", "https://stopgame.ru/rss/new/news", 80, true),
        NewsSource("Igromania", "https://www.igromania.ru/rss/news.xml", 85, true),
        NewsSource("Kanobu", "https://kanobu.ru/rss/", 80, true),
        NewsSource("CyberSport RU", "https://www.cybersport.ru/rss/all", 85, true),
        NewsSource("Championat", "https://www.championat.com/rss/news/", 80, true),
        NewsSource("VGTimes", "https://vgtimes.ru/rss/", 78, true),
        NewsSource("Geektimes/Habr", "https://habr.com/ru/rss/all/all/", 92, true),
        NewsSource("4PDA", "https://4pda.to/feed/", 85, true),
        NewsSource("RBC Tech", "https://rssexport.rbc.ru/rbc/topnews/20/main.rss", 88, true),
        NewsSource("TJournal", "https://tjournal.ru/rss", 75, true),
        NewsSource("Lenta Tech", "https://lenta.ru/rss/news", 75, true),
        NewsSource("Gazeta Tech", "https://www.gazeta.ru/export/rss/tech.xml", 80, true),
        NewsSource("Hi-Tech Mail.ru", "https://hi-tech.mail.ru/rss/", 82, true),
        NewsSource("Rozetked", "https://rozetked.me/rss", 85, true),
        NewsSource("Digger", "https://digger.ru/rss", 80, true),
        NewsSource("Wylsacom", "https://wylsa.com/feed/", 82, true),
        NewsSource("Droider", "https://droider.ru/feed/", 83, true),

        // General Tech/Science
        NewsSource("WIRED", "https://www.wired.com/feed/rss", 92, false),
        NewsSource("Engadget", "https://www.engadget.com/rss.xml", 88, false),
        NewsSource("Gizmodo", "https://gizmodo.com/rss", 85, false),
        NewsSource("TechRadar", "https://www.techradar.com/rss", 86, false),
        NewsSource("The Verge", "https://www.theverge.com/rss/index.xml", 88, false),
        NewsSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", 95, false),
        NewsSource("Slashdot", "https://slashdot.org/slashdot.rss", 85, false),
        NewsSource("Popular Science", "https://www.popsci.com/feed/", 87, false),
        NewsSource("MIT Tech Review", "https://www.technologyreview.com/feed/", 95, false),
        NewsSource("New Scientist", "https://www.newscientist.com/feed/", 94, false),
        NewsSource("Phys.org General", "https://phys.org/rss-feed/", 91, false),
        NewsSource("ScienceNews", "https://www.sciencenews.org/feed", 93, false),
        NewsSource("Nature News", "https://www.nature.com/nature.rss", 98, false),
        NewsSource("Scientific American", "https://www.scientificamerican.com/feed/", 94, false),
        NewsSource("Astronomy", "https://astronomy.com/rss.xml", 89, false),
        NewsSource("Sky & Telescope", "https://skyandtelescope.org/feed/", 88, false),
        NewsSource("Space.com", "https://www.space.com/feeds/science.xml", 85, false),
        NewsSource("Tumblr Trending", "https://news.google.com/rss/search?q=site:tumblr.com+funny&hl=en-US", 70, false),
        NewsSource("Imgur Viral Memes", "https://news.google.com/rss/search?q=site:imgur.com+viral+memes&hl=en-US", 74, false),
        NewsSource("Pinterest Gems", "https://news.google.com/rss/search?q=site:pinterest.com+aesthetic+memes&hl=en-US", 75, false),
        NewsSource("X.com Trending Tech", "https://news.google.com/rss/search?q=X+tech+trends+Twitter&hl=en-US", 78, false),
        NewsSource("Slashdot Science", "https://slashdot.org/slashdot.rss", 85, false)
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
                val request = Request.Builder()
                    .url(source.url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()
                val response = client.newCall(request).execute()
                val xmlBody = response.body?.string()
                
                if (response.isSuccessful && !xmlBody.isNullOrEmpty()) {
                    val trimmedBody = xmlBody.trim()
                    if (trimmedBody.startsWith("<rss") || trimmedBody.startsWith("<feed") || trimmedBody.contains("<?xml")) {
                        val parsedItems = parseRss(xmlBody, source)
                        allNews.addAll(parsedItems.take(5)) // Take top 5 from each
                    } else {
                        Log.w(TAG, "Skipping non-RSS content for ${source.name}")
                    }
                }
            } catch (e: Exception) {
                // Log only a warning for network failures, avoid excessive stack traces for common timeout issues
                Log.w(TAG, "Failed to fetch RSS from ${source.name}: ${e.message}")
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

            val isGoogleNews = source.url.contains("news.google.com")

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
                            
                            val cleanedTitle = cleanNewsTitle(currentTitle, source.name)
                            val cleanedDesc = cleanNewsDesc(cleanedTitle, currentDesc, isGoogleNews)
                            
                            if (cleanedTitle.isNotEmpty()) {
                                items.add(NewsItem(source.name, cleanedTitle, cleanedDesc, currentUrl, source.trustScore))
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

    fun decodeHtmlEntities(input: String): String {
        var text = input
        text = text.replace("&nbsp;", " ")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'")
                   .replace("&#39;", "'")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&ndash;", "–")
                   .replace("&mdash;", "—")
                   .replace("&ldquo;", "\"")
                   .replace("&rdquo;", "\"")
                   .replace("&lsquo;", "'")
                   .replace("&rsquo;", "'")
        text = text.replace(Regex("<.*?>"), "")
        text = text.replace(Regex("\\s+"), " ")
        return text.trim()
    }

    fun cleanNewsTitle(title: String, sourceName: String): String {
        var clean = decodeHtmlEntities(title)
        
        val suffixes = listOf(
            " - $sourceName",
            " — $sourceName",
            " - ${sourceName.lowercase()}",
            " — ${sourceName.lowercase()}"
        )
        for (suffix in suffixes) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length).trim()
            }
        }
        
        val lastDashIndex = clean.lastIndexOf(" - ")
        if (lastDashIndex != -1 && lastDashIndex > clean.length - 45) {
            val after = clean.substring(lastDashIndex + 3).trim()
            if (!after.contains(" ") && (after.contains(".") || after.length < 15)) {
                clean = clean.substring(0, lastDashIndex).trim()
            }
        }
        val lastMdDashIndex = clean.lastIndexOf(" — ")
        if (lastMdDashIndex != -1 && lastMdDashIndex > clean.length - 45) {
            val after = clean.substring(lastMdDashIndex + 3).trim()
            if (!after.contains(" ") && (after.contains(".") || after.length < 15)) {
                clean = clean.substring(0, lastMdDashIndex).trim()
            }
        }
        
        return clean
    }

    fun cleanNewsDesc(title: String, desc: String, isGoogleNews: Boolean): String {
        val cleanTitle = title
        val cleanDesc = decodeHtmlEntities(desc)
        
        if (isGoogleNews) {
            return ""
        }
        
        val normTitle = cleanTitle.lowercase().replace(Regex("[^a-zа-я0-9]"), "")
        val normDesc = cleanDesc.lowercase().replace(Regex("[^a-zа-я0-9]"), "")
        
        if (normDesc.isEmpty() || normDesc == normTitle || normDesc.startsWith(normTitle) || normTitle.startsWith(normDesc)) {
            return ""
        }
        
        return cleanDesc
    }

    private fun getFallbackNews(isRu: Boolean): List<NewsItem> {
        return if (isRu) {
            listOf(
                NewsItem("nOG Network", "Узел синхронизации: Ожидание внешних данных", "Входящий поток новостей временно недоступен. Проверьте соединение с основным шлюзом.", "https://nog.network", 100),
                NewsItem("Анекдот.ру", "Анекдот про ИИ и программиста в баре", "Заходит программист в бар и заказывает ИИ-коктейль. Бармен наливает стакан пустых обещаний, добавляет щепотку хайпа, размешивает зубочисткой и берет 100 долларов. Программист пьет и говорит: 'Но тут же ничего нет!'. Бармен подмигивает: 'В этом и суть стартапа, бро!'", "https://anekdot.ru/1", 95),
                NewsItem("Юмор ФМ", "О дебаггинге перед сном", "Решил программист перед сном посчитать овец. Насчитал 1.000.000.001 овцу, но обнаружил баг: в цикле сбился счетчик, пришлось начать заново с 0. В итоге встретил рассвет, дебажа отару.", "https://yumor.fm/1", 85),
                NewsItem("Башорг Цитаты", "Цитата #439221: Про искусственный интеллект", "Сын: Папа, а искусственный интеллект сможет полностью заменить людей? Отец: Только тех, у кого естественный интеллект работает по расписанию с перерывами на чай.", "https://bashorg.org/1", 88),
                NewsItem("Мемы Рунета", "Мем года: Разработчик и ТЗ", "Новая картинка-мем захватила рунет: Разработчик пытается построить замок по ТЗ заказчика, где нарисована лопата и написано 'Сделайте красиво, чтобы летало'.", "https://t.me/memes/1", 75),
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
                NewsItem("Reddit r/jokes", "AI enters a bar...", "An AI model walks into a bar. The bartender says, 'We don't serve predictive language patterns here.' The AI replies, 'I knew you were going to say that.'", "https://reddit.com/r/jokes/1", 75),
                NewsItem("9GAG", "How many developers does it take to change a lightbulb?", "None. It is a hardware issue, and the software department has already opened an investigation ticket.", "https://9gag.com/1", 70),
                NewsItem("Meme Chronicle", "The 'It works on my machine' emblem becomes a real medal", "A developer has minted a 3D physical medal that states 'It Works on My Machine' to hand out to team members who close tickets without committing changes.", "https://memechronicle.com/1", 78),
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
