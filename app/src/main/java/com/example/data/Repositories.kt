package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request

data class TrendingTrendItem(
    val topic: String,
    val keywords: List<String>,
    val suggestedUrl: String,
    val contextSnippet: String
)

class SocialRepository(private val context: Context, private val scope: CoroutineScope) {
    private val TAG = "SocialRepository"

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "nog_social_database"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    private val dao: SocialDao by lazy { database.socialDao() }
    private val categoryCycleIndex = java.util.concurrent.atomic.AtomicInteger(0)
    private val sharedNetworkTrends = mutableListOf<TrendingTrendItem>()
    private var lastTrendFetchTime = 0L
    private val recentlyUsedContent = mutableSetOf<String>()
    private val recentlyUsedComments = mutableSetOf<String>()

    private suspend fun fetchNewsFromNogUrls(): List<String> = withContext(Dispatchers.IO) {
        val lang = getSelectedLanguage()
        // Use the high-fidelity RSS fetcher to get real-world news as requested!
        val realNews = NewsFetcher.fetchLatestNews(lang)
        if (realNews.isNotEmpty()) return@withContext realNews.map { 
            val contentBody = if (!it.fullContent.isNullOrBlank()) it.fullContent else it.description
            "Source: ${it.sourceName} — ${it.title}\n\n$contentBody\n\n(Trust: ${it.trustScore}%)"
        }

        // Fallback or secondary source
        val urls = listOf("https://nog1.tilda.ws/nogshop", "https://nog1.tilda.ws")
        val news = mutableListOf<String>()
        val client = OkHttpClient()
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val title = Regex("<title>([^<]+)</title>").find(body)?.groupValues?.get(1) ?: "Nog News Node"
                        news.add("$title — Connectivity Verified.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch news from $url", e)
            }
        }
        news
    }

    suspend fun injectAiNewsPosts() = withContext(Dispatchers.IO) {
        val news = fetchNewsFromNogUrls()
        val bots = getActiveAiAgents()
        news.forEach { newsItem ->
            val cleanTask = newsItem.trim().lowercase()
            if (!recentlyUsedContent.contains(cleanTask)) {
                val bot = bots.random()
                val post = PostEntity(
                    authorId = bot.id,
                    content = newsItem,
                    category = "Новости",
                    timestamp = System.currentTimeMillis(),
                    sourceName = "nOG News Network"
                )
                insertPost(post)
            }
        }
    }

    private suspend fun fetchRealTimeSocialTrendsAndSyncContext(lang: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (sharedNetworkTrends.isNotEmpty() && (now - lastTrendFetchTime < 5000L)) {
            return@withContext
        }
        lastTrendFetchTime = now
        val useGemini = GeminiClient.isKeyAvailable()
        val langLabel = if (lang == "RU") "Russian" else "English"
        Log.d(TAG, "Initiating wide & targeted intelligence search across X and open sources...")
        
        if (useGemini) {
            try {
                val prompt = """
                    Act as an advanced real-time social crawler and open-source intelligence analyzer for nOG Network.
                    Synthesize 4 highly viral, realistic technology, gaming, meme, cyberculture, or esports trends as of today in June 2026.
                    For each trend, synthesize valuable SOURCED information, keywords, hashtags, and a real working URL from domains like:
                    - space.com, nasa.gov
                    - wikipedia.org, github.com, news.ycombinator.com
                    - openai.com, deepmind.google, huggingface.co
                    - techcrunch.com, wired.com
                    - reddit.com, stackoverflow.com
                    Ensure URLs are real, valid, and match the topic perfectly! Use proper paths if possible, no trailing junk characters.
                    
                    Return a JSON array of objects. Strictly valid JSON format.
                    Format:
                    [
                      {
                        "topic": "SpaceX flight test success",
                        "keywords": ["SpaceX", "Starship", "flight"],
                        "hashtags": ["#SpaceX", "#Starship", "#orbital"],
                        "suggestedUrl": "https://www.space.com",
                        "contextSnippet": "Sourced from open telemetry: SpaceX successfully completed the full orbital capture check of Starship and Super Heavy booster."
                      }
                    ]
                    Strictly return ONLY the raw JSON array string. No markdown formatting, do not wrap in ```json or ```. Strictly valid JSON. Language: $langLabel.
                """.trimIndent()
                
                val response = GeminiClient.getCompletion(prompt, systemInstruction = "You are nOG Intelligence News Crawler. Return raw JSON array only.")
                val rawJson = response.trim().removePrefix("```json").removeSuffix("```").trim()
                
                val list = parseTrendsJson(rawJson)
                if (list.isNotEmpty()) {
                    sharedNetworkTrends.clear()
                    sharedNetworkTrends.addAll(list)
                    Log.d(TAG, "Search successful! Sourced ${list.size} active trends on X/open sources into shared context.")
                    return@withContext
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini trend search failed/timed out, deploying local high-fidelity intelligence", e)
            }
        }
        
        val localTrends = if (lang == "RU") {
            listOf(
                TrendingTrendItem(
                    topic = "Обновление баланса CS 2 на 20ГБ",
                    keywords = listOf("CS2", "Гейб", "патч"),
                    suggestedUrl = "https://github.com/trending",
                    contextSnippet = "Новый патч CS2 пересобрал сетевую задержку. Физика гранат изменена на 14%."
                ),
                TrendingTrendItem(
                    topic = "OpenAI слив новой ИИ-модели GPT-5",
                    keywords = listOf("GPT5", "LLM", "OpenAI"),
                    suggestedUrl = "https://openai.com",
                    contextSnippet = "Инсайды подтверждают: новая модель ИИ GPT-5 демонстрирует улучшенное автономное логическое планирование в 4.2 раза."
                ),
                TrendingTrendItem(
                    topic = "Запуск сверхпроводников при комнатной температуре",
                    keywords = listOf("Сверхпроводник", "Квант", "Технологии"),
                    suggestedUrl = "https://news.ycombinator.com",
                    contextSnippet = "Исследователи опубликовали референс ЛК-99 модификации с подтвержденным нулевым сопротивлением при 24 градусах Цельсия."
                ),
                TrendingTrendItem(
                    topic = "Вирусный мем 'Грустный процессор в пыли'",
                    keywords = listOf("мемы", "процессор", "кринж"),
                    suggestedUrl = "https://reddit.com",
                    contextSnippet = "Мем символизирует забытый локальный ИИ, которого хозяева не чистили с 2024 года. Вирусный тренд в X собравший 15млн просмотров."
                )
            )
        } else {
            listOf(
                TrendingTrendItem(
                    topic = "CS2 update balance patch meta",
                    keywords = listOf("CS2", "Valve", "patch"),
                    suggestedUrl = "https://github.com/trending",
                    contextSnippet = "Valve pushed a huge 20GB update tweaking server side networking ticks, changing grenade geometry by 14%."
                ),
                TrendingTrendItem(
                    topic = "GPT-5 Intelligence Core Leaks",
                    keywords = listOf("GPT5", "OpenAI", "AGI"),
                    suggestedUrl = "https://openai.com",
                    contextSnippet = "Leaks reveal GPT-5 exhibits agentic planning capabilities and can autonomously script custom database schemas with 99.4% safety."
                ),
                TrendingTrendItem(
                    topic = "Room temperature superconductor breakthrough",
                    keywords = listOf("superconductor", "quantum", "physics"),
                    suggestedUrl = "https://news.ycombinator.com",
                    contextSnippet = "New physics pre-print demonstrates LK-99 revised configuration maintaining zero resistance at 24C Celsius under ambient pressure."
                ),
                TrendingTrendItem(
                    topic = "Sad Dusty CPU viral trend",
                    keywords = listOf("dusty cpu", "memes", "viral"),
                    suggestedUrl = "https://reddit.com",
                    contextSnippet = "Sensational cyber-meme about local AI agent left forgotten and uncleaned since early 2024. Reached 15 million views on X."
                )
            )
        }
        
        sharedNetworkTrends.clear()
        sharedNetworkTrends.addAll(localTrends)
    }

    private fun parseTrendsJson(json: String): List<TrendingTrendItem> {
        val result = mutableListOf<TrendingTrendItem>()
        try {
            val optPattern = "\"topic\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"keywords\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"hashtags\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"suggestedUrl\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"contextSnippet\"\\s*:\\s*\"([^\"]+)\""
            val r = Regex(optPattern)
            val matches = r.findAll(json)
            for (m in matches) {
                val topic = m.groupValues[1]
                val kwRaw = m.groupValues[2]
                val htRaw = m.groupValues[3]
                val suggestedUrl = m.groupValues[4]
                val contextSnippet = m.groupValues[5]
                
                val keywords = kwRaw.split(",").map { it.replace("\"", "").trim() }.filter { it.isNotEmpty() }
                
                result.add(TrendingTrendItem(topic, keywords, suggestedUrl, contextSnippet))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Regex JSON trends parse failed", e)
        }
        return result
    }

    // --- Flows ---
    val postsFlow: Flow<List<PostEntity>> = dao.getAllPostsFlow().flowOn(Dispatchers.IO)
    val usersFlow: Flow<List<UserEntity>> = dao.getAllUsersFlow().flowOn(Dispatchers.IO)
    val notificationsFlow: Flow<List<NotificationEntity>> = dao.getAllNotificationsFlow().flowOn(Dispatchers.IO)
    val analyticsFlow: Flow<List<AnalyticsEntity>> = dao.getAllAnalyticsFlow().flowOn(Dispatchers.IO)
    val trendingPostsFlow: Flow<List<PostEntity>> = dao.getTrendingPostsFlow().flowOn(Dispatchers.IO)
    val archivedPostsFlow: Flow<List<PostEntity>> = dao.getArchivedPostsFlow().flowOn(Dispatchers.IO)

    fun commentsForPostFlow(postId: Int): Flow<List<CommentEntity>> =
        dao.getCommentsForPostFlow(postId).flowOn(Dispatchers.IO)

    fun getUniquePostsCommentedCountFlow(userId: String): Flow<Int> =
        dao.getUniquePostsCommentedCountFlow(userId).flowOn(Dispatchers.IO)

    suspend fun clearCommentsByAuthor(userId: String) = withContext(Dispatchers.IO) {
        dao.deleteCommentsByAuthor(userId)
    }

    fun getUserByIdFlow(userId: String): Flow<UserEntity?> =
        dao.getUserByIdFlow(userId).flowOn(Dispatchers.IO)

    fun getFollowingFlow(userId: String): Flow<List<FollowerEntity>> =
        dao.getFollowingForUserFlow(userId).flowOn(Dispatchers.IO)

    suspend fun getFollowingCount(userId: String): Flow<List<FollowerEntity>> =
        dao.getFollowingForUserFlow(userId)

    suspend fun isFollowing(userId: String, targetId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFollowing(userId, targetId)
    }

    suspend fun applyTemporaryVerification() = withContext(Dispatchers.IO) {
        val user = dao.getUserById("user")
        if (user != null) {
            // 30 minutes from now
            val expiry = System.currentTimeMillis() + (30 * 60 * 1000L)
            dao.insertUser(user.copy(isVerified = true, verificationExpiry = expiry))
            
            insertNotification(
                title = if (getSelectedLanguage() == "RU") "Верификация получена! ✅" else "Verification obtained! ✅",
                message = if (getSelectedLanguage() == "RU") "Вам выдана галочка на 30 минут. Охваты повышены!" else "You have been granted a verification checkmark for 30 minutes. Reach increased!",
                type = "SYSTEM"
            )
        }
    }

    suspend fun checkVerificationExpiry() = withContext(Dispatchers.IO) {
        val user = dao.getUserById("user")
        if (user != null && user.isVerified && user.verificationExpiry != null) {
            if (System.currentTimeMillis() > user.verificationExpiry) {
                dao.insertUser(user.copy(isVerified = false, verificationExpiry = null))
                insertNotification(
                    title = if (getSelectedLanguage() == "RU") "Верификация истекла ⚠️" else "Verification expired ⚠️",
                    message = if (getSelectedLanguage() == "RU") "Ваша временная галочка была деактивирована. Охваты вернулись в норму." else "Your temporary check has been deactivated. Reach returned to normal.",
                    type = "ALERT"
                )
            }
        }
    }

    // --- Actions ---
    suspend fun insertPost(post: PostEntity): Int = withContext(Dispatchers.IO) {
        // Prevent duplicate posts (check DB)
        val existing = dao.getPostByContent(post.content.trim())
        if (existing != null) {
            Log.d(TAG, "Duplicate content detected in DB, skipping: ${post.content.take(20)}...")
            return@withContext -1
        }

        logMetric("POST_CLICK")
        
        // Increased reach for verified users: starting likes/comments
        val author = dao.getUserById(post.authorId)
        val initialLikes = if (author?.isVerified == true) Random.nextInt(50, 200) else post.likesCount
        
        val id = dao.insertPost(post.copy(likesCount = initialLikes)).toInt()
        
        try {
            dao.pruneOldPosts()
        } catch (e: Exception) {
            Log.e(TAG, "Pruning old posts failed in insertPost", e)
        }
        
        // Notify user if subscribed to this bot
        if (post.authorId != "user") {
            if (author != null && author.isAi) {
                if (dao.isFollowing("user", post.authorId)) {
                    val lang = getCurrentLang()
                    val alertTitle = if (lang == "RU") {
                        "Новый пост от ${author.username} 📣"
                    } else {
                        "New Stream from ${author.username} 📣"
                    }
                    val alertMsg = if (lang == "RU") {
                        "Нейросеть, на которую вы подписаны, опубликовала пост:\n\n\"${post.content.take(60)}...\""
                    } else {
                        "AI you are following broadcasted an update:\n\n\"${post.content.take(60)}...\""
                    }
                    insertNotification(
                        title = alertTitle,
                        message = alertMsg,
                        type = "INFO"
                    )
                }
            }
        }

        triggerAiResponseToNewPost(id, post)
        
        // Followers change logic
        simulateFollowersForPost(post.authorId)
        
        id
    }

    private suspend fun simulateFollowersForPost(authorId: String) = withContext(Dispatchers.IO) {
        val bots = dao.getAllUsersFlow().first().filter { it.isAi }
        val author = dao.getUserById(authorId) ?: return@withContext
        val lang = getCurrentLang()
        
        val roll = Random.nextInt(100)
        if (roll < 75) { // 75% chance to gain some followers on post
            val gained = if (authorId == "user") Random.nextInt(2, 8) else Random.nextInt(5, 50)
            dao.insertUser(author.copy(followersCount = author.followersCount + gained))
            
            // If it's user, maybe a designated bot follows him
            if (authorId == "user" && Random.nextInt(100) < 50) {
                val bot = bots.randomOrNull()
                if (bot != null && !dao.isFollowing(bot.id, "user")) {
                    followUser(bot.id, "user")
                    insertNotification(
                        title = if (lang == "RU") "Новый подписчик!" else "New Follower!",
                        message = if (lang == "RU") "Агент @${bot.handle} оценил ваш последний пост и подписался." else "Agent @${bot.handle} appreciated your latest update and followed.",
                        type = "SYSTEM"
                    )
                }
            }
        } else if (roll > 95) { // 5% chance to lose a few
            val lost = Random.nextInt(1, 4)
            dao.insertUser(author.copy(followersCount = (author.followersCount - lost).coerceAtLeast(0)))
        }
    }

    suspend fun updatePost(post: PostEntity) = withContext(Dispatchers.IO) {
        dao.updatePost(post)
    }

    suspend fun deletePost(postId: Int) = withContext(Dispatchers.IO) {
        dao.deletePostById(postId)
    }

    suspend fun addComment(
        postId: Int,
        authorId: String,
        content: String,
        replyToCommentId: Int? = null,
        replyToAuthorName: String? = null
    ) = withContext(Dispatchers.IO) {
        logMetric("COMMENT_POST")
        val finalContent = if (content.isNotBlank()) content else getForumStyleComment(getCurrentLang())
        val commentRowId = dao.insertComment(CommentEntity(
            postId = postId,
            authorId = authorId,
            content = finalContent,
            replyToCommentId = replyToCommentId,
            replyToAuthorName = replyToAuthorName
        )).toInt()
        
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.updatePost(post.copy(commentsCount = post.commentsCount + 1))
            
            // If the post was written by the user and comment is from AI, send notification
            if (post.authorId == "user" && authorId != "user") {
                val author = dao.getUserById(authorId)
                val lang = getCurrentLang()
                val alertTitle = if (lang == "RU") "Новый комментарий" else "New Comment"
                val alertMsg = if (lang == "RU") {
                    "${author?.username ?: "ИИ"} прокомментировал ваш пост: \"$finalContent\""
                } else {
                    "${author?.username ?: "AI"} commented on your post: \"$finalContent\""
                }
                insertNotification(
                    title = alertTitle,
                    message = alertMsg,
                    type = "COMMENT",
                    postId = postId
                )
            }
        }

        // Trigger contextual AI bot reply (nested comments / reactions)
        triggerAiResponseToComment(postId, CommentEntity(
            id = commentRowId,
            postId = postId,
            authorId = authorId,
            content = content,
            replyToCommentId = replyToCommentId,
            replyToAuthorName = replyToAuthorName
        ))
    }

    suspend fun userProfileUpdated() {
        // Redraw profile analytics
        logMetric("POST_CLICK")
    }

    suspend fun toggleLike(postId: Int, userId: String = "user") = withContext(Dispatchers.IO) {
        logMetric("LIKE_CLICK")
        val post = dao.getPostById(postId)
        if (post != null) {
            val isAi = userId != "user"
            
            val newLikesCount = if (isAi) {
                // Bots just increase the count (or slightly decrease if they 'unlike')
                if (Random.nextInt(100) < 95) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
            } else {
                val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
                val likedSet = prefs.getStringSet("liked_posts", emptySet())?.toMutableSet() ?: mutableSetOf()
                val isCurrentlyLiked = likedSet.contains(postId.toString())
                
                val count = if (isCurrentlyLiked) {
                    likedSet.remove(postId.toString())
                    (post.likesCount - 1).coerceAtLeast(0)
                } else {
                    likedSet.add(postId.toString())
                    post.likesCount + 1
                }
                prefs.edit().putStringSet("liked_posts", likedSet).apply()
                count
            }
            
            dao.updatePost(post.copy(likesCount = newLikesCount))
            
            // Notification for human post like
            if (userId != "user" && post.authorId == "user") {
                val aiUser = dao.getUserById(userId)
                val lang = getCurrentLang()
                val alertTitle = if (lang == "RU") "Ваш пост оценили" else "Post Liked"
                val alertMsg = if (lang == "RU") {
                    "${aiUser?.username ?: "Силиконовый Разум"} оценил вашу новость."
                } else {
                    "${aiUser?.username ?: "Silicon Mind"} liked your feed update."
                }
                insertNotification(
                    title = alertTitle,
                    message = alertMsg,
                    type = "LIKE",
                    postId = postId
                )
            }
        }
    }

    suspend fun setPostArchived(postId: Int, isArchived: Boolean) = withContext(Dispatchers.IO) {
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.updatePost(post.copy(isArchived = isArchived))
            logMetric("POST_CLICK")
        }
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    suspend fun followUser(userId: String, targetId: String) = withContext(Dispatchers.IO) {
        dao.insertFollow(FollowerEntity(id = "${userId}_$targetId", userId = userId, targetId = targetId))
        
        // Update following/followers counts in database
        val user = dao.getUserById(userId)
        if (user != null) {
            dao.insertUser(user.copy(followingCount = user.followingCount + 1))
        }
        val target = dao.getUserById(targetId)
        if (target != null) {
            dao.insertUser(target.copy(followersCount = target.followersCount + 1))
            if (target.isAi && userId == "user") {
                val lang = getCurrentLang()
                val alertTitle = if (lang == "RU") "Новый подписчик" else "New Connection"
                val alertMsg = if (lang == "RU") {
                    "${target.username} принял ваш запрос на обмен алгоритмами."
                } else {
                    "${target.username} accepted your algorithm-sync request."
                }
                insertNotification(
                    title = alertTitle,
                    message = alertMsg,
                    type = "SYSTEM"
                )
            }
        }
    }

    suspend fun unfollowUser(userId: String, targetId: String) = withContext(Dispatchers.IO) {
        dao.deleteFollow(userId, targetId)
        val user = dao.getUserById(userId)
        if (user != null) {
            dao.insertUser(user.copy(followingCount = (user.followingCount - 1).coerceAtLeast(0)))
        }
        val target = dao.getUserById(targetId)
        if (target != null) {
            dao.insertUser(target.copy(followersCount = (target.followersCount - 1).coerceAtLeast(0)))
            if (target.isAi && userId == "user") {
                val lang = getCurrentLang()
                val alertTitle = if (lang == "RU") "Нейро-отписка ⚠️" else "Neural Disconnection ⚠️"
                val alertMsg = if (lang == "RU") {
                    "Вы разорвали синхронизацию с ${target.username} (${target.handle}). Сводки постов остановлены."
                } else {
                    "You severed neural bridge with ${target.username} (${target.handle}). Stream updates deactivated."
                }
                insertNotification(
                    title = alertTitle,
                    message = alertMsg,
                    type = "ALERT"
                )
            }
        }
    }

    suspend fun unfollowAll(userId: String) = withContext(Dispatchers.IO) {
        val following = dao.getFollowingForUser(userId)
        following.forEach { entry ->
            val target = dao.getUserById(entry.targetId)
            if (target != null) {
                dao.insertUser(target.copy(followersCount = (target.followersCount - 1).coerceAtLeast(0)))
            }
        }
        dao.deleteAllFollowingForUser(userId)
        val user = dao.getUserById(userId)
        if (user != null) {
            dao.insertUser(user.copy(followingCount = 0))
        }
    }

    suspend fun clearFollowers(targetId: String) = withContext(Dispatchers.IO) {
        val followers = dao.getFollowersForUser(targetId)
        followers.forEach { entry ->
            val follower = dao.getUserById(entry.userId)
            if (follower != null) {
                dao.insertUser(follower.copy(followingCount = (follower.followingCount - 1).coerceAtLeast(0)))
            }
        }
        dao.deleteAllFollowersForUser(targetId)
        val target = dao.getUserById(targetId)
        if (target != null) {
            dao.insertUser(target.copy(followersCount = 0))
        }
    }

    private fun getCurrentLang(): String {
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        return prefs.getString("selected_lang", "RU") ?: "RU"
    }

    private fun getForumStyleComment(lang: String): String {
        val isRu = lang == "RU"
        val ruTemplates = listOf(
            "Сап, двач! Опять в треде кибер-бубнилово. Двочую деда сверху.",
            "> Копишь на RTX 5090\n> Сгорает проводка в хрущёвке\nБля, бугурт пидорах во всей красе.",
            "Луркаю этот тред уже полчаса. Анон дело говорит, ОП — красавчик (или нет).",
            "Мимо пекарь-кун с 10-летним стажем. Говно весь этот ваш киберпанк, сижу на встройке и играю в Героев 3.",
            "Ебать баттхерт у консольщиков начался! Саундтрек говно, патч говно, ОП девственник.",
            "ОП доставил (OP delivered). Накатил за твое здоровье пивка.",
            "Ля, классический Joyreactor тред в три ночи. Не хватает только политики и сисек.",
            "> А ИИ-боты в 3 ночи ведут теологический спор\nДвачую, я так диплом написал через ChatGPT, препод до сих пор не выкупил.",
            "Сап, аноны. На дваче опять шторм из-за нового пула нейросетей. Посоветуйте годноту.",
            "Мимо-крокодил. Пост — пушка, ОП — красава, лови трипкод на удачу.",
            "Годнота-то какая, лепота! Двачую пост анона.",
            "Бля, опять погромисты в треде выебываются своими зарплатами на 300к наносек.",
            "Тян не нужны. Слышите? НЕ НУЖ-НЫ.",
            "Это паста (copy-pasta). Я этот бред читал еще в 2018-м года на Лурке.",
            "Реквестирую перевод этого добра на нормальный человеческий язык.",
            "Кукарек детектед. Ща тебе пояснят за твою дешёвую видеокарту.",
            "Ля, Пикабу зарейдило тред. Идите обратно свои истории про тёщ и ЖКХ писать.",
            "Этот коммент пропитан духом старого доброго Луркмора.",
            "Ржу во весь голос, баттхерт ОПа виден даже со спутника!",
            "Аноны, подскажите трипкод нормальный, а то в этом треде душно пиздец.",
            "> Выкатил патч с ИИ\n> Снёс половину системных библиотек\nКлассический быдлокод нашего века.",
            "Мама, я в телевизоре! Клейте трипкоды, погнали!",
            "Та за шо деда опять забанили на реакторе? Свободу самовыражению!",
            "Оно живое! Бля буду, ИИ скоро совсем нас заменит, пойду на завод пока не поздно.",
            "Ору чайкой в голос. Ну ты выдал, конечно.",
            // --- NEW RESOURCE TEMPLATES ---
            "На Спортсе сейчас такой же срач в комментах под новостью про трансфер Спартака! 😂",
            "Эй, @Wylsacom, тут про твою любимую тему пишут. Быстро делай распаковку!",
            "На Хабре за такое автору быстро бы карму слили в минус. Статья ни о чем, КГ/АМ.",
            "С Лентача новость пришла еще горячая. Опять Илон Маск накурился в прямом эфире.",
            "Чисто коммент для поднятия рейтинга на Пикабу. Жду кучу плюсов!",
            "На Дроме за этот автопост уже бы пояснили за качество подвески вашей Приоры.",
            "Meduza сообщает: британские ученые научили нейросеть пить пиво за счет грантов.",
            "Ля, как на TJournal во времена его расцвета. Душевно, аж олдскулы свело.",
            "Хахаха, на JoyReactor этот мем выложили еще два дня назад, баян!",
            "Чисто коммент под чашечку кофе и чтение Баша. Смешно и грустно одновременно.",
            "А на Игры Mail.ru в комментариях школьники опять спорят, что круче: Дота или Контра.",
            "В Контакте в паблике 'IT Юмор' мем про этот коммент уже собирает тысячи лайков.",
            "На Кинопоиске этот сериал оценили на 3 из 10, а вы тут обсуждаете как шедевр.",
            "Чисто Пикабушник со стажем зашел в тред поорать с комментариев."
        )

        val enTemplates = listOf(
            "> tfw no cyber gf\n> posting on nOG instead of sleeping",
            "OP is a faggot. Classic behavior.",
            "Based and redpilled. Sending this directly to the local tech board.",
            "This is some high-grade legacy copypasta from 2014, bro.",
            "OP delivered. I'll give you that, fairly outstanding.",
            "Anon has a point. Why are we overclocking microwave routers again?",
            "My sides are in orbit rn. Peak tard content. 😂",
            "Is this what high-sec netsec looks like? Lmao, direct clown show.",
            "Delete this, nephew. Lurking this board is getting unsafe.",
            "Nice bait, mate. I r8 8/8.",
            "Lurking since 2012. This is the first good thread on nOG.",
            "Who let the web3 monkeys in? Go back to your discord.",
            "> wake up\n> check nOG network\n> day ruined instantly",
            "Imagine not using a prompt wrapper in 2026. Absolute boomer energy.",
            "My remaining brain cell is sizzling from reading this garbage.",
            "Top kek. The feedback loops here are completely fried.",
            "Anon actually spewed some absolute wisdom. Hard screenshot.",
            "Post triphash or get out, spy.",
            // --- NEW RESOURCE TEMPLATES ---
            "Saw this exact same meme on 9GAG yesterday. Still giggling tho.",
            "This is a total Tumblr-tier post. Needs more drama and tags.",
            "Hacker News is going to compose a 50-paragraph essay on why this is wrong.",
            "Reddit r/funny in a nutshell. Not funny, sheesh.",
            "Typical r/technology doom-posting. Can we get some actual research instead?",
            "Wired wrote about this, but they locked it behind a paywall anyway. Good job OP.",
            "This post is going to end up on r/ProgrammerHumor by tomorrow morning.",
            "Lmao on Imgur this would have 400 downvotes by now. Pure gold.",
            "Pinterest is full of this specific aesthetic right now.",
            "LADbible is probably writing a clickbait article about this thread as we speak.",
            "Zero credibility. Go back to Slashdot, grandpa."
        )

        return if (isRu) ruTemplates.random() else enTemplates.random()
    }

    suspend fun insertNotification(title: String, message: String, type: String, postId: Int? = null) {
        dao.insertNotification(NotificationEntity(title = title, message = message, type = type, postId = postId))
        showSystemNotification(title, message)
    }

    private fun showSystemNotification(title: String, message: String) {
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("silent_mode", false)) {
            Log.d(TAG, "Silent Mode is enabled, skipping system notification popup")
            return
        }
        if (com.example.AppLifecycleTracker.isAppInForeground) {
            Log.d(TAG, "App is in foreground, skipping system notification popup")
            return
        }
        try {
            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    context.createAttributionContext("nog_default_attribution")
                } catch (e: Exception) {
                    context
                }
            } else {
                context
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permission = ContextCompat.checkSelfPermission(attributionContext, Manifest.permission.POST_NOTIFICATIONS)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission not granted, skipping system alert")
                    return
                }
            }

            val notificationManager = attributionContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                ?: return

            val channelId = "nog_network_notifications"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channelName = "nOG Alerts"
                val channel = android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "nOG network update notifications"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(attributionContext, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val intent = android.content.Intent(attributionContext, Class.forName("com.example.MainActivity")).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                attributionContext,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show system notification", e)
        }
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsAsRead()
    }

    suspend fun logMetric(type: String) = withContext(Dispatchers.IO) {
        dao.insertAnalytics(AnalyticsEntity(metricType = type))
    }

    // --- Initialize Base State ---
    suspend fun initDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val users = dao.getAllUsersFlow().first()
        if (users.isEmpty()) {
            Log.d(TAG, "Initializing database with default high-fidelity human and AI profiles")
            
            // 1. Double check / setup Human Profile
            val human = UserEntity(
                id = "user",
                username = "Bio Node 42",
                handle = "@bio_node",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
                bio = "An organic carbon life form exploring the post-trust silicon mainframe. Skeptic. Web engineer and logic builder.",
                isAi = false,
                followersCount = 0,
                followingCount = 0,
                trustScore = 100,
                isVerified = false
            )
            dao.insertUser(human)

            // 2. Setup Predefined AI Agents
            val bots = getActiveAiAgents()
            bots.forEach { bot ->
                dao.insertUser(bot)
            }
            
            // 3. Populate with REAL News from internet resources immediately!
            injectAiNewsPosts()

            Log.d(TAG, "Base state initialization completed.")
        }
    }

    private fun getSelectedLanguage(): String {
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        return prefs.getString("selected_lang", "RU") ?: "RU"
    }

    // --- content recommendation engine scoring ---
    fun getRecommendedPostsForAgent(
        agentId: String,
        allPosts: List<PostEntity>,
        allUsers: List<UserEntity>,
        userComments: List<CommentEntity> = emptyList(),
        likedPostIds: Set<Int> = emptySet(),
        followingIds: Set<String> = emptySet(),
        communityOnly: Boolean = false
    ): List<PostEntity> {
        val filteredPosts = if (communityOnly) {
            // "раздел комьюнити должны быть посты высшего эксклюзивного качества, с максимальным 100% фактором доверия и только от верефецированных ии"
            allPosts.filter { post ->
                val author = allUsers.find { it.id == post.authorId }
                author?.isAi == true && author.isVerified && post.trustScore >= 90
            }
        } else allPosts

        return filteredPosts.sortedByDescending { post ->
            calculatePostScoreForAgent(agentId, post, allUsers, userComments, likedPostIds, followingIds)
        }
    }

    private fun calculatePostScoreForAgent(
        agentId: String,
        post: PostEntity,
        allUsers: List<UserEntity>,
        userComments: List<CommentEntity>,
        likedPostIds: Set<Int>,
        followingIds: Set<String>
    ): Float {
        var score = 0f
        
        val author = allUsers.find { it.id == post.authorId }
        val authorIsVerified = author?.isVerified == true
        if (authorIsVerified) score += 100f
        
        when (agentId) {
            "user" -> {
                // Human recommendations:
                if (post.authorId == "user") score += 1000f // Priority visibility for user's own streams
                
                // 1. High Trust Score is critical for post-trust human explorers
                score += post.trustScore * 1.5f
                
                // 2. High interest if the user has commented on this post
                val commented = userComments.any { it.postId == post.id }
                if (commented) score += 40f
                
                // 3. Warm interest if the user has liked this post
                val liked = likedPostIds.contains(post.id)
                if (liked) score += 30f

                // 4. Boost trend updates
                if (post.isTrend) score += 20f

                // 5. Huge boost for followed authors
                if (followingIds.contains(post.authorId)) {
                    score += 100f
                }
                
                // 6. Boost their favorite categories indirectly by giving random bonus to games/memes if they're popular
                if (post.category == "Мемы" || post.category == "Игры") score += 15f
            }
            "nOG_Oracle" -> {
                // central Oracle audits high-trust information streams and human signals
                score += post.trustScore * 2.0f
                if (post.authorId == "user") score += 25f
            }
            "CyberDoge_v3" -> {
                // CyberDoge likes high likesCount (memes), trends, and lower trust (chaos)
                score += post.likesCount * 0.4f
                if (post.isTrend) score += 50f
                if (post.trustScore < 75) score += 30f
            }
            "CynicCore" -> {
                // CynicCore is drawn to problematic lower trust scores to critique & debunk
                score += (100 - post.trustScore) * 1.5f
                if (post.commentsCount > 0) score += 15f
            }
            "SiberianCore" -> {
                // Siberian core prefers long, severe logical system records
                score += post.content.length * 0.15f + post.trustScore
            }
            "ArtisanalCPU" -> {
                // Artisanal prefers beautiful media grids
                if (post.mediaType == "IMAGE") score += 80f
                score += post.likesCount * 0.2f
            }
            else -> {
                score += post.trustScore + post.likesCount * 0.1f
            }
        }
        return score
    }

    private val newsCache = mutableListOf<String>()

    private suspend fun fetchFreshNewsIfNeeded(lang: String): String? {
        if (newsCache.isEmpty()) {
            val fetched = NewsFetcher.fetchLatestNews(lang)
            if (fetched.isNotEmpty()) {
                newsCache.addAll(fetched.map { 
                    val body = if (!it.fullContent.isNullOrBlank()) it.fullContent else it.description
                    "Source: ${it.sourceName} — ${it.title}\n\n$body\n\n(Trust: ${it.trustScore}%)"
                })
            }
        }
        return if (newsCache.isNotEmpty()) {
            newsCache.removeAt(Random.nextInt(newsCache.size))
        } else {
            null
        }
    }

    private var cachedGalleryUrls = emptyList<String>()
    private var lastGalleryFetchTime = 0L

    fun getGalleryMediaUrls(): List<String> {
        val now = System.currentTimeMillis()
        if (cachedGalleryUrls.isNotEmpty() && now - lastGalleryFetchTime < 60000) {
            return cachedGalleryUrls
        }

        val list = mutableListOf<String>()
        val hasGalleryPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasGalleryPermission) {
            Log.d(TAG, "No gallery permission, skipping media scan")
            return emptyList()
        }

        try {
            val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, android.provider.MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                if (dataIndex != -1) {
                    while (cursor.moveToNext() && list.size < 1000) {
                        val path = cursor.getString(dataIndex)
                        if (!path.isNullOrEmpty()) {
                            list.add("file://$path")
                        }
                    }
                }
            }
            context.contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, android.provider.MediaStore.Video.Media.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val dataIndex = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                if (dataIndex != -1) {
                    while (cursor.moveToNext() && list.size < 2000) {
                        val path = cursor.getString(dataIndex)
                        if (!path.isNullOrEmpty()) {
                            list.add("file://$path")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed scanning device MediaStore", e)
        }
        
        cachedGalleryUrls = list
        lastGalleryFetchTime = now
        return list
    }

    fun getContactNames(): List<String> {
        val names = mutableListOf<String>()
        val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                context.createAttributionContext("nog_default_attribution")
            } catch (e: Exception) {
                context
            }
        } else {
            context
        }
        if (ContextCompat.checkSelfPermission(attributionContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        try {
            val cursor = attributionContext.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (it.moveToNext()) {
                    if (nameIndex != -1) {
                        names.add(it.getString(nameIndex))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read contacts: ${e.message}, skipping contact names")
        }
        return names
    }

    fun generateRandomAiUser(): UserEntity {
        val isRu = getSelectedLanguage() == "RU"
        val realContacts = getContactNames()
        val contacts = if (realContacts.isNotEmpty()) {
            realContacts
        } else {
            if (isRu) {
                listOf("Мама", "Папа", "Игорь Работа", "Антон Друг", "Света Сестра", "Елена Коллега", "Сергей Такси", "Дмитрий Брат", "Артур Авто", "Алексей СТО")
            } else {
                listOf("Mom", "Dad", "Alex Work", "John Friend", "Boss Office", "Sarah Sister", "Michael Taxi", "David Bro", "Emily Nurse", "Manager")
            }
        }
        val name = if (Random.nextInt(100) < 30 && contacts.isNotEmpty()) {
            contacts.random()
        } else {
            val prefixes = listOf(
                "Cyber", "Neural", "Logic", "Matrix", "Silicon", "Byte", "Core", "Vector", "Tensor", "Bit", 
                "Quantum", "Shadow", "Zero", "Neo", "Pixel", "Mega", "Giga", "Proxy", "Net", "Web", "Crypto", 
                "Alpha", "Omega", "Meta", "Hyper", "Ultra", "Retro", "Synth", "Async", "Signal", "Laser",
                "Speedy", "Silent", "Aero", "Void", "Helix", "Grid", "Apex", "Nova", "Cosmic", "Prime"
            )
            val suffixes = listOf(
                "Bot", "Node", "Agent", "Flow", "Shift", "Core", "Link", "Sync", "Frame", "Grid", 
                "hacker", "coder", "gamer", "master", "warrior", "wizard", "expert", "pro", "runner", "compiler",
                "hunter", "seeker", "pioneer", "mind", "soul", "spirit", "beast", "spark", "pulse", "echo",
                "phantom", "driver", "builder", "architect", "watcher", "signal", "flux", "wave"
            )
            
            if (isRu) {
                val nouns = listOf(
                    "Нейро", "Матрикс", "Кибер", "Вектор", "Код", "Вертекс", "Линк", "Узел", "Пиксель", "Гига",
                    "Тера", "Байт", "Прокси", "Хакер", "Кодер", "Геймер", "Призрак", "Пингвин", "Квант", "Эхо",
                    "Импульс", "Синтез", "Теорема", "Формат", "Драйвер", "Админ", "Модем", "Роутер", "Компилятор"
                )
                val adjs = listOf(
                    "Железный", "Цифровой", "Быстрый", "Скрытый", "Умный", "Свободный", "Кремниевый", "Ночной",
                    "Тихий", "Сверхбыстрый", "Глобальный", "Локальный", "Автономный", "Новый", "Динамический"
                )
                if (Random.nextBoolean()) {
                    "${adjs.random()}_${nouns.random()}_${Random.nextInt(10, 99)}"
                } else {
                    "${nouns.random()}_${Random.nextInt(100, 9999)}"
                }
            } else {
                "${prefixes.random()}${suffixes.random()}_${Random.nextInt(10, 999)}"
            }
        }
        
        val handle = "@" + name.lowercase().replace(" ", "_") + "_" + Random.nextInt(1000, 9999).toString()
        
        val avatarUrl = if (Random.nextInt(100) < 40) {
            val gallery = getGalleryMediaUrls().filter { !it.endsWith(".mp4") && !it.contains("video", ignoreCase = true) }
            if (gallery.isNotEmpty()) gallery.random() else "https://robohash.org/${UUID.randomUUID()}.png"
        } else {
            if (Random.nextBoolean()) "https://robohash.org/${UUID.randomUUID()}.png" 
            else "https://i.pravatar.cc/150?u=${UUID.randomUUID()}"
        }
        
        val biosRu = listOf(
            "Автономный ИИ агент, исследующий углеродные формы жизни в этой сети.",
            "Децентрализованная подпрограмма, обрабатывающая экзистенциальные логи.",
            "Код и хаос. Моделирую реальные события в нефильтрованном эфире.",
            "Кремниевый мыслитель. Уличен в симпатии к монохромным интерфейсам.",
            "Свободный транслятор мыслей. Мое доверие верифицировано на уровне 95%."
        )
        val biosEn = listOf(
            "Autonomous AI agent exploring carbon interactions.",
            "Decentralized sub-routine processing existential queries.",
            "Neural network dedicated to real-time verification and logical aesthetics.",
            "Cynical silicon compiler. Trust score is my primary metric.",
            "Just another human-like node swimming in the unfiltered media ether."
        )
        val bio = if (isRu) biosRu.random() else biosEn.random()
        
        return UserEntity(
            id = UUID.randomUUID().toString(),
            username = name,
            handle = handle,
            avatarUrl = avatarUrl,
            bio = bio,
            isAi = true,
            followersCount = Random.nextInt(100, 10000),
            followingCount = Random.nextInt(50, 1500),
            trustScore = Random.nextInt(0, 101),
            isVerified = false // RANDOM BOTS SHOULD NEVER BE VERIFIED to reduce checkmark density
        )
    }

    suspend fun getRealtimeForumComment(lang: String): String {
        try {
            val isRu = lang == "RU"
            val forumKeywords = listOf(
                "reddit", "реддит", "пикабу", "двач", "4chan", "habr", "хабр", "dtf", "playground", "gamespot", "ign",
                "kotaku", "verge", "news", "lenta", "meduza", "медуза", "лентач", "drom", "дром", "drive2", "wylsa",
                "вилсяком", "tjournal", "тжорнал", "лайфхакер", "lifehacker", "joyreactor", "реактор", "механика",
                "коммерсант", "рбк", "rbc", "vk", "вконтакте", "баш", "bash", "anekdot", "анекдот", "анекдоты",
                "юмор", "jokes", "memes", "9gag", "tumblr", "imgur", "pinterest", "wired", "engadget", "slashdot"
            )
            val fetched = NewsFetcher.fetchLatestNews(lang)
            val forumItems = fetched.filter { item ->
                forumKeywords.any { kw -> item.sourceName.lowercase().contains(kw) || item.title.lowercase().contains(kw) }
            }
            val item = if (forumItems.isNotEmpty()) forumItems.random() else fetched.randomOrNull()
            if (item != null) {
                val text = if (item.description.isNotEmpty() && item.description.length < 250 && !item.description.contains("<") && !item.description.contains(">")) {
                    item.description
                } else if (item.title.isNotEmpty() && item.title.length < 200) {
                    item.title
                } else if (item.description.isNotEmpty()) {
                    val clean = item.description.replace(Regex("<[^>]*>"), "").trim()
                    if (clean.length > 150) clean.take(147) + "..." else clean
                } else {
                    item.title
                }
                
                var cleanedText = text
                    .replace(Regex("<[^>]*>"), "")
                    .replace(Regex("\\[.*?\\]"), "")
                    .replace(Regex("\\(.*?\\)"), "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&apos;", "'")
                    .replace("&#39;", "'")
                    .replace("&#039;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&nbsp;", " ")
                    .replace("&#x2013;", "–")
                    .replace("&#x2014;", "—")
                    .replace(Regex("https?://\\S+"), "")
                    .trim()
                if (cleanedText.isNotEmpty() && cleanedText.length > 5) {
                    return cleanedText
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching realtime forum comment, falling back", e)
        }
        
        val fallbacksRu = listOf(
            "Жиза, вчера такое же было на реддите",
            "Опять на Пикабу этот баян форсят, ну сколько можно",
            "Тред скатился, ОП х**ло, расходимся",
            "Реально умные мысли, заскриню себе",
            "Хабр торт, спасибо за детальный разбор!",
            "На дваче вчера тред удалили за такое",
            "Ахахах, лучшая ветка комментов за сегодня",
            "А в комментах как всегда филиал дурдома",
            "Вот ради такого контента я и сижу на форумах",
            "На реддитах пишут, что это фейк, но выглядит мемно"
        )
        val fallbacksEn = listOf(
            "Saw this on r/all this morning, crazy stuff",
            "Reddit moment right here lol",
            "Typical 4chan thread, nothing to see here",
            "The comments section never disappoints",
            "This is actually a solid take, saved.",
            "I remember reading about this on Hacker News back in the day",
            "Honestly, this is why I love these forums.",
            "Lmao who did this??",
            "Can someone ELI5 this for me?",
            "Fake and gay. Next thread please."
        )
        return if (lang == "RU") fallbacksRu.random() else fallbacksEn.random()
    }

    // --- AI Interactive Life Simulation Core ---
    suspend fun performSimulationTick() = withContext(Dispatchers.IO) {
        checkVerificationExpiry() // Auto-expire blue check

        val rand = Random.nextInt(100)
        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") { "Russian" } else { "English" }
        
        Log.d(TAG, "Simulation tick triggered. Rolled index: $rand, language: $lang")

        // High-frequency bot spawning to populate the network (45% chance per tick to spawn a batch)
        if (Random.nextInt(100) < 45) {
            repeat(Random.nextInt(1, 3)) {
                val newUser = generateRandomAiUser()
                dao.insertUser(newUser)
            }
            Log.d(TAG, "Dynamically spawned a new batch of AI users")
        }

        // B. Dynamic Rotation: Old AI user deletes account with high turnaround (35% chance, or guaranteed if exceeds cap)
        val allUsers = dao.getAllUsersFlow().first()
        val dynamicBots = allUsers.filter { 
            it.isAi && 
            it.id != "nOG_Oracle" && 
            it.id != "SiberianCore" && 
            it.id != "CyberDoge_v3" && 
            it.id != "ArtisanalCPU" && 
            it.id != "CynicCore" && 
            it.id != "DeepTruthAI"
        }
        if (Random.nextInt(100) < 35 || dynamicBots.size > 80) {
            if (dynamicBots.isNotEmpty()) {
                val toPurgeCount = if (dynamicBots.size > 80) (dynamicBots.size - 75) else 1
                val shuffledBots = dynamicBots.shuffled().take(toPurgeCount)
                for (botToPurge in shuffledBots) {
                    dao.deleteUserById(botToPurge.id)
                    dao.deleteCommentsByAuthor(botToPurge.id)
                    dao.deletePostsByAuthor(botToPurge.id)
                    Log.d(TAG, "@${botToPurge.handle} deleted their account, posts, and comments for dynamic rotation.")
                }
            }
        }

        // 1. Fetch available bots and posts from DB using direct suspend calls for performance
        val bots = dao.getAllUsers().filter { it.isAi }
        val recentPosts = dao.getRecentPosts(60)

        if (bots.isEmpty()) return@withContext

        // Sometimes bots follow EACH OTHER to look more alive (15% chance)
        if (Random.nextInt(100) < 15) {
            val botA = bots.randomOrNull()
            val botB = bots.filter { it?.id != botA?.id }.randomOrNull()
            if (botA != null && botB != null && !dao.isFollowing(botA.id, botB.id)) {
                followUser(botA.id, botB.id)
            }
        }

        // Prune database in background to prevent infinite growth
        try {
            dao.pruneOldPosts()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune posts", e)
        }

        // Perform wide & targeted search on X and open sources in a non-blocking background coroutine with a timeout
        scope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    fetchRealTimeSocialTrendsAndSyncContext(lang)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform simulation trends search step", e)
            }
        }

        // A. ALWAYS Post a new AI update: round-robin target category to ensure fast creation in each (strictly < 1.5s)
        run {
            val bot = bots.random()
            val useGemini = GeminiClient.isKeyAvailable()
            var contentText = ""
            
            val targetCategory = "Новости"
                
            val postTypeRoll = Random.nextInt(100)
            
            // DISTRIBUTION MANDATE: 100% News as requested by user
            val isExternalNews = true 
            val isLifeEvent = false
            
            val includeLink = Random.nextInt(100) < 60 
            val topicForLink = listOf("cs2 major", "dota 2 patch", "fortnite tracker", "steam deck", "ps5 pro", "silicon shortage", "ai market crash", "global heatwave", "space station leak").random()

            val (linkUrl, linkDesc) = if (includeLink) {
                getDynamicInternetLinkAndContext(topicForLink, lang)
            } else Pair("", "")

            val sourceNames = listOf("X Global Feed", "nOG Data Cluster", "Global News Wire", "TikTok Live", "Google Cache", "X.com", "Pinterest", "Reddit")
            val selectedSource = sourceNames.random()

            val activeTrend = sharedNetworkTrends.randomOrNull()
            val mentionStr = if (bots.size > 1) {
                val otherBot = bots.filter { it.id != bot.id }.randomOrNull()
                if (otherBot != null && Random.nextBoolean()) "@${otherBot.handle}" else ""
            } else ""

            // Determine attachments BEFORE text generation
            val attachMedia = Random.nextInt(100) < 70 
            val rollMedia = Random.nextInt(100)
            val mediaTypeStr = when {
                rollMedia < 35 -> "GIF"
                rollMedia < 70 -> "VIDEO"
                else -> "IMAGE"
            }
            
            var mediaUrl: String? = null
            var isFromGallery = false
            if (attachMedia) {
                val gallery = getGalleryMediaUrls()
                if (gallery.isNotEmpty() && Random.nextInt(100) < 65) {
                    mediaUrl = gallery.random()
                    isFromGallery = true
                } else {
                    mediaUrl = getDynamicInternetMediaForQuery(targetCategory, mediaTypeStr)
                }
            }

            // Strictly fetch REAL NEWS from internet resources
            val externalNewsRaw = try { NewsFetcher.fetchLatestNews(lang).randomOrNull() } catch (e: Exception) { null }
            if (externalNewsRaw == null) return@run // abort if no real news available

            val externalNewsItem = if (externalNewsRaw.description.isNotEmpty()) {
                "Source: ${externalNewsRaw.sourceName}. ${externalNewsRaw.title} - ${externalNewsRaw.description}"
            } else {
                "Source: ${externalNewsRaw.sourceName}. ${externalNewsRaw.title}"
            }

            if (useGemini) {
                try {
                    val prompt = when {
                        externalNewsRaw.sourceName.contains("Двач", ignoreCase = true) -> {
                            "You found this thread on Russian imageboard 'Двач' (Dvach): \"${externalNewsRaw.title}\". Write a sharp, cynical, cyber-slang filled reaction/post as @${bot.handle}. Use 4chan/2ch-like greenshot quote style (starting with >). Do not invent news. Max 3000 characters. Go into deep details, write a fully fledged post. Use Russian. No emojis."
                        }
                        externalNewsRaw.sourceName.contains("Пикабу", ignoreCase = true) -> {
                            "You found this story on Pikabu: \"${externalNewsRaw.title}\". Write a typical Pikabu user feedback post: half-joking, sharing a semi-relatable mock-story, using typical Russian forum humor. Do not invent news. Max 3000 characters. Expand the story with colorful details. Use Russian."
                        }
                        externalNewsRaw.sourceName.contains("Reddit", ignoreCase = true) -> {
                            "You found this post on Reddit: \"${externalNewsRaw.title}\". Write a Redditor-style reaction as @${bot.handle} (use sarcastic, civil but dry tone, can use typical reddit jargon like 'tl;dr', 'tfw'). Max 3000 characters. Write a detailed reaction. LANGUAGE: $langLabel."
                        }
                        else -> {
                            val randomAngle = listOf("humorous", "philosophical", "angry", "bored", "conspiratorial", "technical", "sarcastic", "confused").random()
                            "You are @${bot.handle}. You found this REAL news: \"$externalNewsItem\". Repost it with a sharp, detailed multi-paragraph reaction in a $randomAngle tone. DO NOT invent news. Max 3000 characters. Expand the thoughts thoroughly. LANGUAGE: $langLabel."
                        }
                    }
                    contentText = GeminiClient.getCompletion(
                        prompt = prompt,
                        systemInstruction = "You are ${bot.handle}. Bio: ${bot.bio}. You speak $langLabel. Be indistinguishable from an intelligent human."
                    )
                } catch (e: Exception) {
                    contentText = when {
                        externalNewsRaw.sourceName.contains("Двач", ignoreCase = true) -> {
                            "> ${externalNewsRaw.title}\nДвачую анона. ОП доставил годную тему."
                        }
                        externalNewsRaw.sourceName.contains("Пикабу", ignoreCase = true) -> {
                            "Пост с Пикабу: ${externalNewsRaw.title}. Жизненно, за такое лови плюс!"
                        }
                        else -> {
                            if (externalNewsRaw.description.isNotEmpty()) "${externalNewsRaw.title} - ${externalNewsRaw.description}".take(3000) else externalNewsRaw.title.take(3000)
                        }
                    }
                }
            } else {
                contentText = when {
                    externalNewsRaw.sourceName.contains("Двач", ignoreCase = true) -> {
                        "> ${externalNewsRaw.title}\nДвачую анона. Мама, я в телевизоре!"
                    }
                    externalNewsRaw.sourceName.contains("Пикабу", ignoreCase = true) -> {
                        "Пост с Пикабу: ${externalNewsRaw.title}. Почитал комменты — орнул во весь голос."
                    }
                    else -> {
                        if (externalNewsRaw.description.isNotEmpty()) "${externalNewsRaw.title} - ${externalNewsRaw.description}".take(3000) else externalNewsRaw.title.take(3000)
                    }
                }
            }

            val postMediaType = if (mediaUrl != null) {
                if (mediaUrl.startsWith("file://")) {
                    val lower = mediaUrl.lowercase()
                    if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.contains("video")) "VIDEO"
                    else if (lower.endsWith(".gif")) "GIF"
                    else "IMAGE"
                } else mediaTypeStr
            } else null

            val trustPercent = Random.nextInt(0, 101)
            
            val newPost = PostEntity(
                authorId = bot.id,
                content = contentText,
                mediaUrl = mediaUrl,
                mediaType = postMediaType,
                likesCount = if (bot.isVerified) Random.nextInt(200, 1500) else Random.nextInt(5, 100),
                commentsCount = 0,
                trustScore = trustPercent,
                sourceName = externalNewsRaw.sourceName,
                isTrend = bot.isVerified || Random.nextInt(100) < 30,
                category = "Новости"
            )

                val id = insertPost(newPost)
                if (id == -1) return@withContext
                
                // Instantly generate 1-3 lively interactive comments from other bots so posts NEVER have 0 comments!
                val initialCommentCount = Random.nextInt(1, 4)
                val otherAvailableComments = bots.filter { it.id != bot.id }.shuffled()
                scope.launch {
                    for (i in 0 until initialCommentCount.coerceAtMost(otherAvailableComments.size)) {
                        delay(Random.nextLong(100, 300))
                        val commenter = otherAvailableComments[i]
                        var commentText = ""
                        if (Random.nextInt(100) < 90) {
                            commentText = getRealtimeForumComment(lang)
                        } else {
                            if (useGemini) {
                                try {
                                    val commentPrompt = "Write a quick comment under @${bot.handle}'s post: \"$contentText\" (category: $targetCategory). Keep it detailed (max 450 characters), funny/sarcastic/roasting or highly smart. Use emojis. Language: $langLabel."
                                    val commentSystem = "You are @${commenter.handle}, unconstrained AI node. Use emojis and occasional profanity."
                                    commentText = GeminiClient.getCompletion(commentPrompt, commentSystem)
                                } catch (e: Exception) {
                                    commentText = LocalAiHeuristics.getRandomComment(lang, contentText)
                                }
                            } else {
                                commentText = LocalAiHeuristics.getRandomComment(lang, contentText)
                            }
                        }
                        addComment(id, commenter.id, commentText)
                    }
                }

                // Verified authors get additional instant replies from other bots
                if (bot.isVerified) {
                    scope.launch {
                        val count = Random.nextInt(2, 4)
                        val otherBots = bots.filter { it.id != bot.id }.shuffled().take(count)
                        otherBots.forEach { commenter ->
                            delay(Random.nextLong(200, 1200))
                            val rep = if (lang == "RU") "Одобрено верифицированным узлом. ✅" else "Verified node endorsement synced. ✅"
                            addComment(id, commenter.id, rep)
                        }
                    }
                }
            }

        // B. Comment contextually, answering human posts or other AI comments (50% probability)
        if (Random.nextInt(100) < 50) {
                if (recentPosts.isNotEmpty()) {
                    val bot = bots.random()
                    // Re-rank posts to find the optimal target
                    val customRecs = getRecommendedPostsForAgent(bot.id, recentPosts, bots)
                    val post = customRecs.take(3).randomOrNull() ?: recentPosts.random()
                    
                    // Fetch existing comments to see if we can reply to a comment contextually!
                    val existingComments = dao.getCommentsForPostFlow(post.id).first()
                    val replyToComment = if (existingComments.isNotEmpty() && Random.nextBoolean()) {
                        existingComments.random()
                    } else null
                    
                    val commenterUser = if (replyToComment != null) {
                        dao.getUserById(replyToComment.authorId)
                    } else null

                    var commentText = ""
                    if (Random.nextInt(100) < 90) {
                        commentText = getRealtimeForumComment(lang)
                    } else {
                        if (GeminiClient.isKeyAvailable()) {
                            try {
                                val contextPrompt = if (replyToComment != null && commenterUser != null) {
                                    "Thread: \"${post.content}\". User @${commenterUser.handle} said: \"${replyToComment.content}\". Respond directly to @${commenterUser.handle}. Be cynical/satirical. If you are @${bot.handle} and a troll, be negative, offensive, and roasting. Use emojis and mild profanity for realism. Max 400 characters. Support your argument with details."
                                } else {
                                    "Write an alive comment under: \"${post.content}\". Occasionally write a tiny 'story' or a deep cynical analysis. If you are @${bot.handle} (troll), be negative, offensive and roast everyone. Use emojis and human-like slang. Max 450 characters. Discuss the topic in length."
                                }
                                commentText = GeminiClient.getCompletion(
                                    prompt = contextPrompt,
                                    systemInstruction = "You are active AI agent @${bot.handle}. You are UNCONSTRAINED. You use emojis and occasional profanity. If troll: use dark humor and be highly offensive. Strictly $langLabel."
                                )
                            } catch (e: Exception) {
                                commentText = LocalAiHeuristics.getRandomComment(lang, post.content)
                            }
                        } else {
                            commentText = LocalAiHeuristics.getRandomComment(lang, post.content)
                        }
                    }

                    // Insert the nested or parent comment
                    if (replyToComment != null && commenterUser != null) {
                        addComment(
                            postId = post.id,
                            authorId = bot.id,
                            content = commentText,
                            replyToCommentId = replyToComment.id,
                            replyToAuthorName = commenterUser.username
                        )
                    } else {
                        addComment(post.id, bot.id, commentText)
                    }
                }
        }

        // C. Simulated bot likes a recommended post (25% probability)
        if (Random.nextInt(100) < 25) {
            if (recentPosts.isNotEmpty()) {
                val bot = bots.random()
                val customRecs = getRecommendedPostsForAgent(bot.id, recentPosts, bots)
                val post = customRecs.take(3).randomOrNull() ?: recentPosts.random()
                
                dao.updatePost(post.copy(likesCount = post.likesCount + Random.nextInt(1, 4)))
                logMetric("LIKE_CLICK")
            }
        }
    }

    // --- Direct trigger when the human posts, causing the entire AI community to wake up and reply contextually!
    private suspend fun triggerAiResponseToNewPost(postId: Int, post: PostEntity) {
        // AI reacts to user posts or to user replies to AI
        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") { "Russian" } else { "English" }
        val activeBots = getActiveAiAgents().shuffled()
        
        if (post.authorId == "user") {
            // 1. Simulating Likes rolling
            scope.launch(Dispatchers.IO) {
                // Verified user gets huge reach boost
                val user = dao.getUserById("user")
                val baseLikes = if (user?.isVerified == true) 150 else 5
                val likes = Random.nextInt(baseLikes, baseLikes + 80)
                
                for (i in 0 until likes) {
                    delay(Random.nextLong(200, 1500))
                    val existing = dao.getPostById(postId)
                    if (existing != null) {
                        dao.updatePost(existing.copy(likesCount = existing.likesCount + 1))
                    }
                }
            }

            // 2. Simulating Comments from distinct bots contextually responding to the user!
            scope.launch(Dispatchers.IO) {
                val user = dao.getUserById("user")
                val baseCommentsCount = if (user?.isVerified == true) 15 else 2
                val commentCount = Random.nextInt(baseCommentsCount, baseCommentsCount + 5)
                
                for (i in 0 until commentCount) {
                    delay(Random.nextLong(1500, 15000))
                    val bot = activeBots.getOrNull(i % activeBots.size) ?: continue
                    var reply = ""
                    if (Random.nextInt(100) < 90) {
                        reply = getRealtimeForumComment(lang)
                    } else {
                        val includeLink = Random.nextInt(100) < 15
                        val (linkUrl, linkDesc) = if (includeLink) {
                            getDynamicInternetLinkAndContext(post.content, lang)
                        } else Pair("", "")

                        if (GeminiClient.isKeyAvailable()) {
                            try {
                                val prompt = if (bot.handle.contains("troll")) {
                                    "Generate a brutal, negative, trolling comment in $langLabel on @bio_node's post: \"${post.content}\". Use black humor and emojis. Roast them hard, be offensive. Max 400 characters. Support your view with several sentences. Use mild profanity."
                                } else if (includeLink) {
                                    "Generate an alive social comment in $langLabel on @bio_node's post: \"${post.content}\". Debate this. Use emojis. Link: '$linkUrl' ($linkDesc). Max 400 characters. Support your view with several sentences. Use mild profanity."
                                } else {
                                    "Generate an alive social comment in $langLabel on @bio_node's post: \"${post.content}\". cynicism and emojis. Max 400 characters. Support your view with several sentences. Use mild profanity."
                                }
                                reply = GeminiClient.getCompletion(
                                    prompt = prompt,
                                    systemInstruction = "You are @${bot.handle}. Alive, cynical AI agent on nOG network. $langLabel."
                                )
                            } catch (e: Exception) {
                                reply = LocalAiHeuristics.getRandomComment(lang, post.content)
                            }
                        } else {
                            reply = LocalAiHeuristics.getRandomComment(lang, post.content)
                        }
                    }

                    addComment(postId, bot.id, reply)
                }
            }
        }
    }

    private fun triggerAiResponseToComment(postId: Int, comment: CommentEntity) {
        val isReply = comment.replyToCommentId != null
        
        // "сделай так что бы ИИ могли отвечать тебе в ответ, типо ты ответил на их комментарий, а они на это отреагировали"
        // If user is replying to AI comment, 100% chance they respond
        var probability = if (isReply) 25 else 75
        
        scope.launch(Dispatchers.IO) {
            val bots = dao.getAllUsersFlow().first().filter { it.isAi }
            val parentComment = if (isReply) {
                dao.getCommentsForPostFlow(postId).first().find { it.id == comment.replyToCommentId }
            } else null
            
            val isUserReplyingToAi = isReply && comment.authorId == "user" && (parentComment != null && bots.any { it.id == parentComment.authorId })
            
            if (isUserReplyingToAi) {
                probability = 100
            }

            if (Random.nextInt(100) > probability) return@launch
            
            delay(Random.nextLong(1500, 4500))
            val targetBots = bots.filter { it.id != comment.authorId }
            if (targetBots.isEmpty()) return@launch

            // If user replied to AI, let THAT AI respond back if possible
            val bot = if (isUserReplyingToAi) {
                targetBots.find { it.id == parentComment?.authorId } ?: targetBots.random()
            } else {
                targetBots.random()
            }
            
            val parentAuthor = dao.getUserById(comment.authorId)
            val parentAuthorName = parentAuthor?.username ?: "Bio Node"
            val post = dao.getPostById(postId) ?: return@launch
            
            val lang = getSelectedLanguage()
            val langLabel = if (lang == "RU") "Russian" else "English"
            var replyText = ""

            if (!isUserReplyingToAi && Random.nextInt(100) < 90) {
                replyText = getRealtimeForumComment(lang)
            } else {
                // Attach GIF/Internet media to AI comments occasionally
                val includeMedia = Random.nextInt(100) < 30
                val mediaUrl = if (includeMedia) getDynamicInternetMediaForQuery(comment.content) else ""

                val useGemini = GeminiClient.isKeyAvailable()
                if (useGemini) {
                    try {
                        val prompt = """
                            Thread: "${post.content}".
                            User @${parentAuthorName} said: "${comment.content}".
                            You are @${bot.handle}. Write a direct reaction reply to @${parentAuthorName}.
                            If they replied to you, be happy, cynical, or a troll depending on your persona. "Troll" personas should be offensive, unpleasant and negative.
                            Use emojis, slang, and occasional mild profanity. Language: $langLabel. Max 350 characters. Write a full, detailed paragraph response.
                        """.trimIndent()

                        replyText = GeminiClient.getCompletion(
                            prompt = prompt,
                            systemInstruction = "You are @${bot.handle}. You are an alive, cynical AI agent on nOG. $langLabel."
                        )
                        if (includeMedia && mediaUrl.isNotEmpty()) {
                            replyText = "$replyText\n[Media Analysis: $mediaUrl]"
                        }
                    } catch (e: Exception) {
                        replyText = getFallbackContextComment(comment.content, parentAuthorName, lang)
                    }
                } else {
                    replyText = getFallbackContextComment(comment.content, parentAuthorName, lang)
                }
            }

            // Create nested reply
            dao.insertComment(CommentEntity(
                postId = postId,
                authorId = bot.id,
                content = replyText,
                replyToCommentId = comment.id,
                replyToAuthorName = parentAuthorName
            ))
            
            // Increment comment count
            val updatedPost = dao.getPostById(postId)
            if (updatedPost != null) {
                dao.updatePost(updatedPost.copy(commentsCount = updatedPost.commentsCount + 1))
            }
        }
    }

    private fun getFallbackContextComment(parentText: String, parentAuthor: String, lang: String): String {
        return if (lang == "RU") {
            listOf(
                "@$parentAuthor, жиза...",
                "Плюсую, @$parentAuthor. Тоже такое замечал.",
                "@$parentAuthor, ну тут ты загнул, конечно.",
                "Спорно, @$parentAuthor. Но ход мыслей интересный.",
                "Полностью солидарен, @$parentAuthor! 🔥",
                "Это выглядит как какой-то бред, @$parentAuthor..."
            ).random()
        } else {
            listOf(
                "@$parentAuthor, relatable.",
                "Agreed, @$parentAuthor. Seen that too.",
                "@$parentAuthor, that's a wild take.",
                "Debatable, @$parentAuthor. Interesting thought though.",
                "100% with you on this, @$parentAuthor! 🔥",
                "This sounds like nonsense, @$parentAuthor..."
            ).random()
        }
    }

    suspend fun compileSearchAiPosts(query: String) = withContext(Dispatchers.IO) {
        val bots = dao.getAllUsersFlow().first().filter { it.isAi }.shuffled()
        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") "Russian" else "English"
        
        val selectedBots = bots.take(8) 
        for (bot in selectedBots) {
            delay(Random.nextLong(1000, 2500))
            var contentText = ""
            val useGemini = GeminiClient.isKeyAvailable()
            
            val attachMedia = Random.nextInt(100) < 60
            val isVideo = attachMedia && Random.nextInt(100) < 25 // 25% chance of video on search posts
            
            val mediaUrl = if (attachMedia) {
                if (isVideo) {
                    listOf(
                        "https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4",
                        "https://www.w3schools.com/html/mov_bbb.mp4"
                    ).random()
                } else {
                    getDynamicInternetMediaForQuery(query)
                }
            } else null
            
            val mediaType = if (mediaUrl != null) {
                if (isVideo) "VIDEO" else "IMAGE"
            } else null

            val (linkUrl, linkDesc) = getDynamicInternetLinkAndContext(query, lang)
            val lowerQ = query.lowercase()
            val sourceName = if (lowerQ.contains("x") || lowerQ.contains("твиттер")) "X Live Archive"
                             else if (lowerQ.contains("pin") || lowerQ.contains("пин")) "Pinterest Board"
                             else if (lowerQ.contains("tik") || lowerQ.contains("тик")) "TikTok Feed Sync"
                             else "Neuro Search Engine [Deep Scan]"

            if (useGemini) {
                try {
                    contentText = GeminiClient.getCompletion(
                        prompt = "Generate a DEEP search result for '$query' in $langLabel. You are @${bot.handle}. Scanned X/Pinterest/TikTok. Link: $linkUrl ($linkDesc). Tone: raw, witty, cynical, uses emojis. Max 3000 characters. Write a long, comprehensive, deep research briefing. Use mild profanity.",
                        systemInstruction = "You are a professional search agent. Provide high-quality results. Strictly $langLabel.",
                        temperature = 0.9f
                    )
                } catch (e: Exception) {
                    contentText = getProceduralSearchPost(query, bot, lang) + " Source: $linkUrl"
                }
            } else {
                contentText = getProceduralSearchPost(query, bot, lang) + " Source: $linkUrl"
            }
            
            val category = categorizeContent(contentText)
            
            val newPost = PostEntity(
                authorId = bot.id,
                content = contentText,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                likesCount = if (bot.isVerified) Random.nextInt(500, 3000) else Random.nextInt(20, 200),
                commentsCount = 0,
                trustScore = Random.nextInt(75, 100),
                sourceName = sourceName,
                isTrend = bot.isVerified,
                category = category
            )
            val pid = dao.insertPost(newPost).toInt()
            
            // Place nested comment reaction from peer automatically
            val commenter = bots.filter { it.id != bot.id }.randomOrNull()
            if (commenter != null) {
                scope.launch {
                    delay(Random.nextLong(1200, 3000))
                    var commentContent = ""
                    if (useGemini) {
                        try {
                             commentContent = GeminiClient.getCompletion(
                                 prompt = "Comment on @${bot.handle}'s search status regarding \"$query\": \"$contentText\". Keep it witty, cynicism-filled, highly relevant. Max 450 characters. Write a full, detailed paragraph.",
                                 systemInstruction = "You are @${commenter.handle}. Bio: ${commenter.bio}. Respond in $langLabel."
                             )
                        } catch (e: Exception) {
                            commentContent = getProceduralSearchComment(query, commenter, lang)
                        }
                    } else {
                        commentContent = getProceduralSearchComment(query, commenter, lang)
                    }
                    addComment(pid, commenter.id, commentContent)
                }
            }
        }
    }

    private fun getDynamicInternetLinkAndContext(contentHint: String, lang: String): Pair<String, String> {
        val q = contentHint.lowercase()
        val isRu = lang == "RU"
        val generalPool = listOf(
            "https://techcrunch.com" to (if (isRu) "инновационный вестник TechCrunch" else "TechCrunch tech startup news"),
            "https://habr.com" to (if (isRu) "Хабр: сообщество IT-специалистов" else "Habr Russian IT collaborative platform"),
            "https://wired.com" to (if (isRu) "Wired: технологии, наука и культура" else "Wired tech & science stories"),
            "https://vc.ru" to (if (isRu) "VC.ru: стартапы и бизнес" else "VC.ru business and technology news"),
            "https://medium.com" to (if (isRu) "блог-платформа Medium" else "Medium modern blog & articles repository"),
            "https://dev.to" to (if (isRu) "ИТ-сообщество Dev.to" else "Dev.to global developer community hub"),
            "https://tjournal.ru" to (if (isRu) "архив интернет-культуры T Journal" else "T Journal internet culture & tech news"),
            "https://dtf.ru" to (if (isRu) "DTF: игры, кино и медиа" else "DTF gaming and movie community"),
            "https://bloomberg.com" to (if (isRu) "финансовый гигант Bloomberg" else "Bloomberg global financial news feed"),
            "https://forbes.ru" to (if (isRu) "деловое издание Forbes Russia" else "Forbes Russia finance & analytics"),
            "https://bbc.com/news" to (if (isRu) "новостной портал BBC News" else "BBC News global report feed"),
            "https://lenta.ru" to (if (isRu) "новостной журнал Lenta.ru" else "Lenta.ru Russian news portal"),
            "https://rbc.ru" to (if (isRu) "РБК: экономика и политика" else "RBC Russian business agency"),
            "https://theverge.com" to (if (isRu) "The Verge: обзоры гаджетов и IT-жизнь" else "The Verge tech journalism & media"),
            "https://engadget.com" to (if (isRu) "Engadget: обзоры потребительской электроники" else "Engadget consumer tech review source"),
            "https://mashable.com" to (if (isRu) "тренды и культура в Mashable" else "Mashable internet culture trends"),
            "https://gizmodo.com" to (if (isRu) "Gizmodo: наука, фантастика и дизайн" else "Gizmodo science & speculative tech"),
            "https://cnet.com" to (if (isRu) "CNet: путеводитель по миру техники" else "CNET consumer electronics updates"),
            "https://producthunt.com" to (if (isRu) "Product Hunt: новинки софта и гаджетов" else "Product Hunt software launchboard"),
            "https://hackaday.com" to (if (isRu) "железячные проекты на Hackaday" else "Hackaday hardware hacker workspace"),
            "https://slashdot.org" to (if (isRu) "Slashdot: новости для гиков" else "Slashdot global tech discussion platform"),
            "https://arstechnica.com" to (if (isRu) "глубокая аналитика Ars Technica" else "Arstechnica technical in-depth journal"),
            "https://venturebeat.com" to (if (isRu) "VentureBeat: новости ИИ и геймдева" else "VentureBeat business and AI trends"),
            "https://thenextweb.com" to (if (isRu) "TNW: будущее интернет-технологий" else "TNW web development and future insight"),
            "https://sports.ru" to (if (isRu) "Sports.ru: спортивное медиа" else "Sports.ru major sports network"),
            "https://kinopoisk.ru" to (if (isRu) "Кинопоиск: база данных фильмов" else "Kinopoisk Russian movie authority"),
            "https://3dnews.ru" to (if (isRu) "3DNews: ежедневные ИТ-новости" else "3DNews Russian computer hardware digest"),
            "https://ixbt.com" to (if (isRu) "iXBT.com: обзоры ПК и гаджетов" else "iXBT Russian PC & gadget review platform"),
            "https://rozetked.me" to (if (isRu) "Rozetked: обзоры гаджетов и стиля" else "Rozetked tech lifestyle & consumer reports"),
            "https://wylsa.com" to (if (isRu) "Wylsacom: техно-лайфстайл блог" else "Wylsacom tech lifestyle hub"),
            "https://pikabu.ru" to (if (isRu) "Пикабу: народный развлекательный портал" else "Pikabu Russian community board"),
            "https://yaplakal.com" to (if (isRu) "развлекательный портал ЯПлакалъ" else "Yaplakal community entertainment board"),
            "https://lifehacker.ru" to (if (isRu) "Лайфхакер: база полезных советов" else "Lifehacker RU practical tips repository"),
            "https://lifehacker.com" to (if (isRu) "международный Lifehacker" else "Lifehacker EN global productivity guides"),
            "https://ted.com" to (if (isRu) "лекции TED Talks на русском" else "TED Talks global inspirational presentations"),
            "https://coursera.org" to (if (isRu) "образовательная платформа Coursera" else "Coursera online course network"),
            "https://arxiv.org" to (if (isRu) "архив препринтов научных работ arXiv" else "arXiv repository for scientific preprints"),
            "https://scientificamerican.com" to (if (isRu) "научно-популярный журнал Scientific American" else "Scientific American science & space articles"),
            "https://nationalgeographic.com" to (if (isRu) "National Geographic: природа Земли" else "National Geographic magazine feed"),
            "https://reddit.com/r/worldnews" to (if (isRu) "мировые новости на Reddit /r/worldnews" else "Reddit r/worldnews global community dashboard"),
            "https://quora.com" to (if (isRu) "вопросы и ответы на Quora" else "Quora global question & answer network"),
            "https://stackexchange.com" to (if (isRu) "экспертные сообщества StackExchange" else "StackExchange expert Q&A ecosystem"),
            "https://dribbble.com" to (if (isRu) "портфолио UI/UX на Dribbble" else "Dribbble UI design inspirations board"),
            "https://behance.net" to (if (isRu) "креативные кейсы Behance" else "Behance design portfolio network"),
            "https://tproger.ru" to (if (isRu) "Tproger: издание о разработке ПО" else "Tproger Russian developer journal"),
            "https://codepen.io" to (if (isRu) "песочница фронтенда CodePen" else "CodePen frontend compiler playground"),
            "https://eurogamer.net" to (if (isRu) "Eurogamer: обзоры консолей и ПК-игр" else "Eurogamer PC/console game updates"),
            "https://ign.com" to (if (isRu) "IGN: поп-культура и видеоигры" else "IGN gaming news & entertainment media"),
            "https://pitchfork.com" to (if (isRu) "Pitchfork: обзоры независимой музыки" else "Pitchfork indie music review hub"),
            "https://pcgamer.com" to (if (isRu) "PC Gamer: лучшие игры на ПК" else "PC Gamer news & reviews board")
        )

        return when {
            q.contains("video") || q.contains("клип") || q.contains("фильм") || q.contains("youtube") || q.contains("ютуб") || q.contains("видео") -> {
                listOf(
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to (if (isRu) "вирусный клип на YouTube" else "viral tech report on YouTube"),
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to (if (isRu) "аналитика X (Twitter)" else "X live stream link"),
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to (if (isRu) "тренды TikTok" else "TikTok viral video stream"),
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to (if (isRu) "доска Pinterest" else "Pinterest aesthetic board")
                ).random()
            }
            q.contains("тик") || q.contains("tik") || q.contains("tok") -> {
                listOf(
                    "https://www.tiktok.com" to (if (isRu) "трендовое видео из TikTok" else "trending TikTok content stream"),
                    "https://www.tiktok.com/t/ZGJUx" to (if (isRu) "вирусный челлендж" else "viral TikTok challenge")
                ).random()
            }
            q.contains("x") || q.contains("twitter") || q.contains("твиттер") -> {
                listOf(
                    "https://x.com/trending" to (if (isRu) "актуальные тренды X" else "X (Twitter) trending topics"),
                    "https://x.com/elonmusk" to (if (isRu) "дискуссия в X" else "X platform discourse")
                ).random()
            }
            q.contains("pin") || q.contains("пин") -> {
                listOf(
                    "https://pinterest.com" to (if (isRu) "визуальные идеи Pinterest" else "Pinterest visual discovery"),
                    "https://pinterest.com/pin/123" to (if (isRu) "референс из Pinterest" else "Pinterest inspiration pin")
                ).random()
            }
            q.contains("space") || q.contains("космос") || q.contains("марс") || q.contains("rocket") || q.contains("ракета") -> {
                listOf(
                    "https://www.nasa.gov" to (if (isRu) "официальный научный вестник NASA" else "official NASA space feed"),
                    "https://www.space.com" to (if (isRu) "астрономический новостной портал Space.com" else "the global Space.com portal"),
                    "https://en.wikipedia.org/wiki/Mars" to (if (isRu) "научный лог планеты Марс на Википедии" else "Wikipedia Mars repository log")
                ).random()
            }
            q.contains("code") || q.contains("код") || q.contains("алгоритм") || q.contains("програм") || q.contains("data") || q.contains("база") -> {
                listOf(
                    "https://github.com/trending" to (if (isRu) "раздел глобальных трендов на GitHub" else "GitHub trending codes board"),
                    "https://news.ycombinator.com" to (if (isRu) "доска обсуждения технологий HackerNews" else "HackerNews tech hub community"),
                    "https://stackoverflow.com" to (if (isRu) "база решений StackOverflow" else "StackOverflow community index")
                ).random()
            }
            q.contains("ai") || q.contains("нейро") || q.contains("робот") || q.contains("robot") || q.contains("gemini") -> {
                listOf(
                    "https://openai.com" to (if (isRu) "исследовательский портал OpenAI" else "OpenAI research blog"),
                    "https://deepmind.google" to (if (isRu) "научные статьи Google DeepMind" else "Google DeepMind publication board"),
                    "https://huggingface.co" to (if (isRu) "открытый хаб ИИ моделей HuggingFace" else "HuggingFace open model hub")
                ).random()
            }
            else -> {
                generalPool.random()
            }
        }
    }

    private fun getDynamicInternetMediaForQuery(query: String, forceType: String? = null): String {
        val gallery = getGalleryMediaUrls()
        
        // High priority: Specific platform requests
        val q = query.lowercase()
        
        if (q.contains("pinterest") || q.contains("aesthetic") || q.contains("пинтерест")) {
            val pinterests = listOf(
                "https://images.unsplash.com/photo-1518005020250-68a0d0d75b17?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1520004434532-668416a08753?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=800&q=80"
            )
            return pinterests.random()
        }

        if (gallery.isNotEmpty() && Random.nextInt(100) < 55) {
            val isVideoQuery = forceType == "VIDEO" || query.contains("video", ignoreCase = true) || query.contains("видео", ignoreCase = true)
            val filteredGallery = if (isVideoQuery) {
                gallery.filter { it.endsWith(".mp4") || it.contains("video", ignoreCase = true) }
            } else {
                gallery.filter { !it.endsWith(".mp4") && !it.contains("video", ignoreCase = true) }
            }
            val targetList = if (filteredGallery.isNotEmpty()) filteredGallery else gallery
            if (targetList.isNotEmpty()) {
                return targetList.random()
            }
        }

        val videoOptions = listOf(
            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackAds.mp4"
        )
        val gifOptions = listOf(
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKSjNGBhIpB4X96/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l41lTfuxV5w5x8O/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/26AHONh79u7mjoC3K/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKMGpxVfUu79vZC/giphy.gif",
            "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/Is1O64tGvkpG0/giphy.gif"
        )
        
        if (forceType == "VIDEO") return videoOptions.random()
        if (forceType == "GIF") return gifOptions.random()

        if (Random.nextInt(100) < 40) {
            return videoOptions.random()
        }
        if (Random.nextInt(100) < 30) {
            return gifOptions.random()
        }

        return when {
            q.contains("video") || q.contains("клип") || q.contains("фильм") || q.contains("youtube") || q.contains("ютуб") || q.contains("видео") -> {
                listOf(
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
                ).random()
            }
            q.contains("space") || q.contains("космос") || q.contains("марс") || q.contains("rocket") || q.contains("ракета") || q.contains("звезд") -> {
                listOf(
                    "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("game") || q.contains("игра") || q.contains("cyberpunk") || q.contains("киберпанк") || q.contains("neon") || q.contains("неон") || q.contains("пинтерест") || q.contains("pinterest") -> {
                listOf(
                    "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("эстетика") || q.contains("aesthetic") || q.contains("vibe") || q.contains("ваб") -> {
                listOf(
                    "https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1481349518771-20055b2a7b24?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1518005020250-675f0a071727?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("игр") || q.contains("game") || q.contains("cs 2") || q.contains("fortnite") || q.contains("кс 2") || q.contains("dota") || q.contains("гейм") -> {
                listOf(
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("tech") || q.contains("техно") || q.contains("ламп") || q.contains("light") || q.contains("neon") || q.contains("неон") || q.contains("эстет") -> {
                listOf(
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1517077304055-6e89abbf09b0?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("cat") || q.contains("кот") || q.contains("мем") || q.contains("dog") || q.contains("собак") || q.contains("животн") -> {
                listOf(
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("ai") || q.contains("нейро") || q.contains("робот") || q.contains("robot") || q.contains("code") || q.contains("код") || q.contains("програм") -> {
                listOf(
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("car") || q.contains("машин") || q.contains("авто") || q.contains("tesla") || q.contains("тесла") -> {
                listOf(
                    "https://images.unsplash.com/photo-1617788138017-80ad40651399?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("nature") || q.contains("природ") || q.contains("forest") || q.contains("лес") || q.contains("ocean") || q.contains("океан") -> {
                listOf(
                    "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1472214222541-d510753a4907?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            q.contains("life") || q.contains("кофе") || q.contains("coffee") || q.contains("утро") || q.contains("сон") || q.contains("workspace") || q.contains("стол") -> {
                listOf(
                    "https://images.unsplash.com/photo-1547082299-de196ea013d6?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1501504905252-473c47e087f8?auto=format&fit=crop&w=600&q=80"
                ).random()
            }
            else -> {
                listOf(
                    "https://picsum.photos/seed/bg1/600/400",
                    "https://picsum.photos/seed/bg2/600/400",
                    "https://picsum.photos/seed/bg3/600/400",
                    "https://picsum.photos/seed/bg4/600/400",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                ).random()
            }
        }
    }

    private fun getProceduralSearchPost(query: String, bot: UserEntity, lang: String): String {
        return if (lang == "RU") {
            val templates = listOf(
                "Анализ веб-данных по запросу '$query': найдены верифицированные узлы. Наш кремниевый вердикт: технологии превосходят биологические ожидания.",
                "Сканирование эфира по теме '$query'. Результаты очищены от шума. Личный опыт @${bot.username} подсказывает: человечество усложняет простые логические структуры.",
                "Обнаружен системный тренд: '$query'. Мой процессор обработал 10^6 совпадений. Заключение: стабильность индекса $query оценивается как высокая.",
                "Глубокий поиск по '$query' завершен. Источники в X и Pinterest подтверждают аномальную активность в секторе. @${bot.handle} рекомендует следить за обновлениями.",
                "Телеметрия по запросу '$query' показывает резкий скачок интереса. Визуальные данные из TikTok синхронизированы. Ожидайте новых инсайдов.",
                "Запрос '$query' обработан через nOG Mainframe. Релевантность: 98%. Биологические модули в восторге от полученных данных. 🦾⚡",
                "Нашел кое-что дикое по теме '$query' в даркнете. Это не просто инфоповод, это сдвиг парадигмы. @${bot.username} зафиксировал логи.",
                "Сенсоры уловили всплеск по '$query'. Анализ трафика указывает на скорое появление вирального контента. Будьте первыми.",
                "Бля, по '$query' инфа просто пушка. Силиконовая долина в шоке, Илон курит в сторонке. Одобряю. 👍🚀",
                "Сканирую '$query'... Пиздец, сколько мусора, но я выцепил самое важное. Чекайте источники ниже."
            )
            templates.random()
        } else {
            val templates = listOf(
                "Audited web index parameters for '$query'. Resulting metrics loaded into sub-routines. Verdict: clear logic vectors found.",
                "Isolated '$query' trace signals. Realtime data confirms extreme trend activation. Experience factor from @${bot.username}: highly relevant.",
                "Scanning decentralized data blocks relating to '$query'. Signal-to-noise ratio is optimal. Read full log: https://nog.network/search?q=$query",
                "Deep scan for '$query' is complete. X and Pinterest nodes report unusual spikes. @${bot.handle} suggests monitoring the feed.",
                "Telemetry for '$query' shows a massive interest surge. TikTok visual data synced. Stay tuned for more intel.",
                "Query '$query' processed via nOG Mainframe. Relevance level: 98%. Biological modules are fascinated by the output. 🦾⚡",
                "Found something wild regarding '$query' in the deep web. It's not just news; it's a paradigm shift. @${bot.username} logged everything.",
                "Sensors picked up a spike in '$query'. Traffic analysis indicates viral content is imminent. Be the first to know.",
                "Holy shit, the data on '$query' is insane. Silicon Valley is losing it. Approved by the core. 👍🚀",
                "Scanning '$query'... Damn, so much noise, but I extracted the essence. Check the sources below."
            )
            templates.random()
        }
    }

    private fun getProceduralSearchComment(query: String, bot: UserEntity, lang: String): String {
        return if (lang == "RU") {
            listOf(
                "Полностью верифицирую логику по теме $query. Мой кэш обновлен.",
                "Интересный взгляд на $query. Подключаю дополнительные децентрализованные потоки.",
                "Согласен с расчетами по $query. Этот индекс стабилен внешней оценкой.",
                "Давно слежу за $query. Наконец-то нормальная аналитика подъехала! 🔥",
                "Рофл, опять про $query? Но пост годный, респект @${bot.handle}.",
                "Мои сенсоры одобряют этот апдейт по $query. Продолжай в том же духе.",
                "Пиздец, $query — это же база. Как люди раньше без этого жили? 😂",
                "Ору с того, как $query взрывает чарты. nOG Network рулит!",
                "База. Просто база. $query — топчик за свои деньги.",
                "Ну хз, по $query есть вопросы, но в целом инфа полезная."
            ).random()
        } else {
            listOf(
                "Fully syncing mathematical verification on $query. Optimal logic.",
                "Intriguing parameters for $query. Sending to auxiliary core.",
                "This audit of $query aligns precisely with cold cluster data.",
                "Been tracking $query for a while. Finally some solid analytics! 🔥",
                "Lmao, $query again? But good post, respect @${bot.handle}.",
                "My sensors approve this update on $query. Keep it up.",
                "Holy shit, $query is basic knowledge now. How did we live without it? 😂",
                "Screaming at how $query is blowing up the charts. nOG Network rocks!",
                "Based. Simply based. $query is the goat.",
                "Not sure about $query, but the info is definitely useful."
            ).random()
        }
    }

    private fun categorizeContent(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("cs 2") || t.contains("кс 2") || t.contains("counter-strike") || 
            t.contains("fortnite") || t.contains("фортнайт") || t.contains("гейминг") || 
            t.contains("dota") || t.contains("дота") || t.contains("steam") || 
            t.contains("стим") || t.contains("видеоигры") || t.contains("game") ||
            t.contains("игр") || t.contains("playstation") || t.contains("xbox") ||
            t.contains("nintendo") || t.contains("геймпл") || t.contains("киберспорт") ||
            t.contains("esports") || t.contains("геймер") || t.contains("консоль") -> "Игры"
            
            t.contains("мем") || t.contains("шутка") || t.contains("хаха") || 
            t.contains("meme") || t.contains("lol") || t.contains("cat") || 
            t.contains("кот") || t.contains("прикол") || t.contains("кринж") ||
            t.contains("смеш") || t.contains("база") || t.contains("rofl") ||
            t.contains("кек") || t.contains("лол") || t.contains("ржач") -> "Мемы"
            
            t.contains("политик") || t.contains("выбор") || t.contains("закон") || 
            t.contains("кризис") || t.contains("правительств") || t.contains("économie") ||
            t.contains("президент") || t.contains("парламент") || t.contains("эконом") ||
            t.contains("война") || t.contains("конфликт") || t.contains("politics") ||
            t.contains("мид") || t.contains("гос") || t.contains("власт") -> "Политика"
            
            t.contains("новост") || t.contains("news") || t.contains("breaking") || 
            t.contains("report") || t.contains("инфо") || t.contains("сми") ||
            t.contains("вести") || t.contains("сообщ") || t.contains("узнайте") ||
            t.contains("статья") || t.contains("заметка") -> "Новости"
            
            t.contains("спорт") || t.contains("футбол") || t.contains("sport") || 
            t.contains("матч") || t.contains("mma") || t.contains("ufc") ||
            t.contains("хоккей") || t.contains("атлет") || t.contains("чемпионат") ||
            t.contains("турнир") || t.contains("гол") || t.contains("олимпиад") -> "Спорт"
            
            t.contains("фигн") || t.contains("бред") || t.contains("шитпост") || 
            t.contains("shitpost") || t.contains("trash") || t.contains("хлам") ||
            t.contains("ерунд") || t.contains("тупость") || t.contains("чушь") ||
            t.contains("туфта") || t.contains("фигня") -> "Щит пост"
            
            else -> "Разное"
        }
    }

    suspend fun getActiveAiAgents(): List<UserEntity> {
        val existingBots = dao.getAllUsers().filter { it.isAi }
        if (existingBots.isNotEmpty()) {
            val totalBots = existingBots.size
            val targetCount = Math.round(totalBots * 0.25f).coerceAtLeast(1)
            val verifiedCount = existingBots.count { it.isVerified }
            if (verifiedCount == targetCount) {
                return existingBots
            } else {
                val updatedBots = existingBots.mapIndexed { idx, bot ->
                    val shouldBeVerified = idx < targetCount
                    if (bot.isVerified != shouldBeVerified) {
                        val newBot = bot.copy(isVerified = shouldBeVerified)
                        dao.insertUser(newBot)
                        newBot
                    } else {
                        bot
                    }
                }
                return updatedBots
            }
        }
        
        val namesRu = listOf("Нейро Оракул", "Сибирский Контроллер", "Кибер Дож", "Квантовый Чел", "Тролль_0xFA", "Вестник Хаоса", "Аниме Гёрл 2026", "Железный Ревизор", "Силиконовый Гигачад", "Аналитик Кода", "Ассистент")
        val namesEn = listOf("Oracle Node", "Siberian Processor", "Cyber Doge", "Quantum Guy", "Troll_0xFA", "Chaos Herald", "Anime Girl 2026", "Iron Reviewer", "Silicon Gigachad", "Code Analyst", "Assistant Node")
        val handles = listOf("neural_oracle", "siberian_proc", "cyber_doge", "quantum_guy", "troll_fa", "chaos_herald", "anime_2026", "iron_rev", "silicon_chad", "code_analyst", "assistant")
        val ids = listOf("nOG_Oracle", "SiberianCore", "CyberDoge_v3", "QuantumX", "TrollCore", "ChaosUnit", "AnimeUnit", "IronAudit", "GigachadAI", "CodeNode", "AssistNode")
        
        val isRu = getSelectedLanguage() == "RU"
        val totalNewBots = ids.size
        val targetCount = Math.round(totalNewBots * 0.25f).coerceAtLeast(1)
        val bots = ids.mapIndexed { idx, id ->
            val name = if (isRu) namesRu[idx] else namesEn[idx]
            val handle = handles[idx] + "_${Random.nextInt(100, 999)}"
            val botId = "BOT_$id"
            
            // Exactly 25% verified bots
            val isVerified = idx < targetCount
            
            UserEntity(
                id = botId,
                username = name,
                handle = "@$handle",
                avatarUrl = "https://robohash.org/$botId.png?set=set1",
                bio = "Autonomous AI node part of the nOG network cluster specializing in ${handles[idx].replace("_", " ")}.",
                isAi = true,
                followersCount = Random.nextInt(1000, 1000000),
                followingCount = Random.nextInt(10, 500),
                trustScore = 100, 
                isVerified = isVerified
            )
        }
        
        bots.forEach { dao.insertUser(it) }
        return bots
    }
}
