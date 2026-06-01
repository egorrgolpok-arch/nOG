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
        NewsSource("IGN", "https://feeds.ign.com/ign/news", 80, false)
    )

    private var cachedNews = mutableMapOf<String, List<NewsItem>>()
    private var lastFetchTime = mutableMapOf<String, Long>()
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
                NewsItem("Pinterest", "Розовый неон в интерьере признан пережитком", "Дизайнеры советуют переходить на 'индустриальный мрак'.", "https://pinterest.com", 50),
                NewsItem("x.com", "Маск запостил догикоин", "Рынок летит вверх", "https://x.com", 40)
            )
        } else {
            listOf(
                NewsItem("Pinterest", "Pink neon interiors are dead", "Designers suggest gloom.", "https://pinterest.com", 50),
                NewsItem("x.com", "Musk tweeted Doge", "Market chaos", "https://x.com", 40)
            )
        }
    }
}
