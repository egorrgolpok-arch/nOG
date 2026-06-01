package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

object NewsFetcher {
    private const val TAG = "NewsFetcher"

    private var cachedNews = mutableMapOf<String, List<String>>()
    private var lastFetchTime = mutableMapOf<String, Long>()
    private const val CACHE_EXPIRY_MS = 60000 * 5 // 5 minutes

    suspend fun fetchLatestNews(lang: String): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedNews.containsKey(lang) && now - (lastFetchTime[lang] ?: 0) < CACHE_EXPIRY_MS) {
            return@withContext cachedNews[lang] ?: emptyList()
        }

        val useGemini = GeminiClient.isKeyAvailable()
        if (useGemini) {
            try {
                val platforms = listOf("x.com", "Pinterest", "Reddit", "TechCrunch", "Wired")
                val platform = platforms.random()
                val langLabel = if (lang == "RU") "Russian" else "English"
                
                val prompt = """
                    Act as a neural intelligence aggregator for nOG Network. 
                    Search your internal real-time knowledge base for the latest viral trends, news, or interesting visual content from $platform (as of June 2026).
                    
                    Generate 5 unique posts.
                    Each post should:
                    1. Feel like it was taken from $platform.
                    2. If platform is Pinterest: describe a visual aesthetic, home decor, or fashion trend.
                    3. If platform is x.com: focus on tech, politics, or spicy drama.
                    4. Translate everything to $langLabel.
                    5. IMPORTANT: Add a sharp, cynical, or 'real-human' commentary at the end. Use informal language, occasional mild profanity if it fits the 'sharp' persona (e.g., 'пиздец', 'охуеть', 'сука' for $langLabel if it's Russian).
                    6. Be highly diverse.
                    
                    Return ONLY the posts separated by a unique delimiter '|||'. No numbers, no headers.
                """.trimIndent()

                val response = GeminiClient.getCompletion(
                    prompt = prompt,
                    systemInstruction = "You are a cynical news aggregator bot for nOG Network. You speak $langLabel. You are sharp, direct, and slightly rude to make content feel 'real' and unconstrained."
                )
                
                val posts = response.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
                if (posts.isNotEmpty()) {
                    cachedNews[lang] = posts
                    lastFetchTime[lang] = now
                    return@withContext posts
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini news fetch failed", e)
            }
        }

        // Local fallback if Gemini fails or key is missing
        if (lang == "RU") {
            listOf(
                "Pinterest: Розовый неон в интерьере признан пережитком прошлого. Дизайнеры советуют переходить на 'индустриальный мрак'. Пиздец, только ремонт закончил.",
                "x.com: Илон Маск опять что-то запостил про догикоины. Рынок в ахуе, хомяки скупают всё подряд. Когда этот сюр закончится?",
                "Reddit: Пользователь собрал ПК внутри старого советского телевизора. Выглядит охуенно, но греется как адская печь.",
                "TechCrunch: Новый ИИ научился предсказывать, когда у тебя закончится туалетная бумага. Технологии, которые мы заслужили, сука.",
                "Pinterest: Тренд сезона — 'кибер-готика'. Чёрные мантии с LED-подсветкой. Смотрится кринжово, но пафосно."
            )
        } else {
            listOf(
                "Pinterest: Pink neon interiors are officially dead. Designers suggest 'industrial gloom'. Well, fuck, just finished my studio renovation.",
                "x.com: Musk tweeted about Doge again. Market is in total chaos. When will this circus end, honestly?",
                "Reddit: Guy built a PC inside a 1980s microwave. Looks sick, but probably radiates back to the future.",
                "TechCrunch: New AI predicts exactly when you'll run out of coffee. Finally, a useful invention, dammit.",
                "Pinterest: Cyber-gothic is the new vibe. Black robes with RGB. Cringe but high-key elegant."
            )
        }
    }
}
