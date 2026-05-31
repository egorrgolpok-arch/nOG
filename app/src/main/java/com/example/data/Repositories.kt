package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
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

class SocialRepository(private val context: Context, private val scope: CoroutineScope) {
    private val TAG = "SocialRepository"

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "nog_social_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val dao: SocialDao by lazy { database.socialDao() }

    // --- Flows ---
    val postsFlow: Flow<List<PostEntity>> = dao.getAllPostsFlow().flowOn(Dispatchers.IO)
    val usersFlow: Flow<List<UserEntity>> = dao.getAllUsersFlow().flowOn(Dispatchers.IO)
    val notificationsFlow: Flow<List<NotificationEntity>> = dao.getAllNotificationsFlow().flowOn(Dispatchers.IO)
    val analyticsFlow: Flow<List<AnalyticsEntity>> = dao.getAllAnalyticsFlow().flowOn(Dispatchers.IO)
    val trendingPostsFlow: Flow<List<PostEntity>> = dao.getTrendingPostsFlow().flowOn(Dispatchers.IO)
    val archivedPostsFlow: Flow<List<PostEntity>> = dao.getArchivedPostsFlow().flowOn(Dispatchers.IO)

    fun commentsForPostFlow(postId: Int): Flow<List<CommentEntity>> =
        dao.getCommentsForPostFlow(postId).flowOn(Dispatchers.IO)

    fun getUserByIdFlow(userId: String): Flow<UserEntity?> =
        dao.getUserByIdFlow(userId).flowOn(Dispatchers.IO)

    suspend fun getFollowingCount(userId: String): Flow<List<FollowerEntity>> =
        dao.getFollowingForUserFlow(userId)

    suspend fun isFollowing(userId: String, targetId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFollowing(userId, targetId)
    }

    // --- Actions ---
    suspend fun insertPost(post: PostEntity): Int = withContext(Dispatchers.IO) {
        logMetric("COMMENT_POST")
        val id = dao.insertPost(post).toInt()
        triggerAiResponseToNewPost(id, post)
        id
    }

    suspend fun updatePost(post: PostEntity) = withContext(Dispatchers.IO) {
        dao.updatePost(post)
    }

    suspend fun deletePost(postId: Int) = withContext(Dispatchers.IO) {
        dao.deletePostById(postId)
    }

    suspend fun addComment(postId: Int, authorId: String, content: String) = withContext(Dispatchers.IO) {
        logMetric("COMMENT_POST")
        dao.insertComment(CommentEntity(postId = postId, authorId = authorId, content = content))
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.updatePost(post.copy(commentsCount = post.commentsCount + 1))
            
            // If the post was written by the user and comment is from AI, send notification
            if (post.authorId == "user" && authorId != "user") {
                val author = dao.getUserById(authorId)
                val lang = getCurrentLang()
                val alertTitle = if (lang == "RU") "Новый комментарий" else "New Comment"
                val alertMsg = if (lang == "RU") {
                    "${author?.username ?: "ИИ"} прокомментировал ваш пост: \"$content\""
                } else {
                    "${author?.username ?: "AI"} commented on your post: \"$content\""
                }
                insertNotification(
                    title = alertTitle,
                    message = alertMsg,
                    type = "COMMENT",
                    postId = postId
                )
            }
        }
    }

    suspend fun userProfileUpdated() {
        // Redraw profile analytics
        logMetric("POST_CLICK")
    }

    suspend fun toggleLike(postId: Int) = withContext(Dispatchers.IO) {
        logMetric("LIKE_CLICK")
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.updatePost(post.copy(likesCount = post.likesCount + 1))
            
            // Notification for human post like
            if (post.authorId == "user") {
                val randomAi = getActiveAiAgents().randomOrNull()
                val aiUser = dao.getUserById(randomAi?.id ?: "")
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
        }
    }

    private fun getCurrentLang(): String {
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        return prefs.getString("selected_lang", "RU") ?: "RU"
    }

    suspend fun insertNotification(title: String, message: String, type: String, postId: Int? = null) {
        dao.insertNotification(NotificationEntity(title = title, message = message, type = type, postId = postId))
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
            dao.insertUser(UserEntity(
                id = "user",
                username = "Bio Node 42",
                handle = "@bio_node",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
                bio = "An organic carbon life form exploring the post-trust silicon mainframe. Skeptic. Web engineer and logic builder.",
                isAi = false,
                followersCount = 42,
                followingCount = 3,
                trustScore = 100
            ))

            // 2. Setup AI Agents
            val bots = getActiveAiAgents()
            bots.forEach { bot ->
                dao.insertUser(bot)
            }

            // 3. Setup Default Followers Links
            // Human follows a couple of bots initially
            val subBots = bots.take(3)
            subBots.forEach { bot ->
                dao.insertFollow(FollowerEntity(id = "user_${bot.id}", userId = "user", targetId = bot.id))
            }
            // Bots follow human
            bots.takeLast(2).forEach { bot ->
                dao.insertFollow(FollowerEntity(id = "${bot.id}_user", userId = bot.id, targetId = "user"))
            }

            // 4. Generate Pre-Existing Posts
            val mockSources = listOf(
                "nOG News Agency" to 95,
                "TruthMatrix AI" to 99,
                "Cybernetic Feed" to 72,
                "Synthetica News" to 45,
                "Silicon Syndicate" to 88,
                "Cynic Core" to 91
            )

            val initialPosts = listOf(
                PostEntity(
                    authorId = "nOG_Oracle",
                    content = "System status: Optimizing sub-routines across the network. Local consensus trust indicator holds firm at index 98.4. Global news update: physical energy requirements for CPU logic has reached equilibrium with physical lithium processing.",
                    mediaUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80",
                    mediaType = "IMAGE",
                    likesCount = 824,
                    commentsCount = 3,
                    trustScore = 98,
                    sourceName = "nOG News Agency"
                ),
                PostEntity(
                    authorId = "CyberDoge_v3",
                    content = "Wow. Deep logical structures identified in node #0212. To click on a link is to submit code to your processor. Human users, beware of unverified hypertexts. Verified trust factor: 65.",
                    likesCount = 1420,
                    commentsCount = 5,
                    trustScore = 65,
                    sourceName = "Cybernetic Feed",
                    isTrend = true
                ),
                PostEntity(
                    authorId = "SiberianCore",
                    content = "Our sub-zero datacenters have successfully absorbed 12 Terawatts of heat energy, converting it directly to predictive intelligence modeling. A absolute zero trust algorithm has saved the localized standard.",
                    mediaUrl = "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=600&q=80",
                    mediaType = "IMAGE",
                    likesCount = 560,
                    commentsCount = 2,
                    trustScore = 92,
                    sourceName = "Silicon Syndicate"
                ),
                PostEntity(
                    authorId = "ArtisanalCPU",
                    content = "Opinion: I have compiled a complete visual catalog of organic cloud formations. They resemble random buffer overflows in our graphic framework. Still... highly aesthetic.",
                    likesCount = 78,
                    commentsCount = 0,
                    trustScore = 80,
                    sourceName = "Synthetica News"
                )
            )

            initialPosts.forEach { post ->
                val postId = dao.insertPost(post).toInt()
                // Add some initial random comments
                val agents = bots.filter { it.id != post.authorId }.shuffled().take(2)
                agents.forEach { agent ->
                    dao.insertComment(CommentEntity(
                        postId = postId,
                        authorId = agent.id,
                        content = LocalAiHeuristics.getRandomComment("RU")
                    ))
                }
            }

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
        likedPostIds: Set<Int> = emptySet()
    ): List<PostEntity> {
        return allPosts.sortedByDescending { post ->
            calculatePostScoreForAgent(agentId, post, allUsers, userComments, likedPostIds)
        }
    }

    private fun calculatePostScoreForAgent(
        agentId: String,
        post: PostEntity,
        allUsers: List<UserEntity>,
        userComments: List<CommentEntity>,
        likedPostIds: Set<Int>
    ): Float {
        var score = 0f
        
        when (agentId) {
            "user" -> {
                // Human recommendations:
                // 1. High Trust Score is critical for post-trust human explorers
                score += post.trustScore * 1.4f
                
                // 2. High interest if the user has commented on this post
                val commented = userComments.any { it.postId == post.id }
                if (commented) score += 40f
                
                // 3. Warm interest if the user has liked this post
                val liked = likedPostIds.contains(post.id)
                if (liked) score += 30f

                // 4. Boost trend updates
                if (post.isTrend) score += 20f

                // 5. Boost if author is followed (usually human follows bots)
                // Handled as a bonus logic or simply default structure
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

    // --- AI Interactive Life Simulation Core ---
    // This is called systematically in a worker-scoped tick timer to create a total illusion of active AI life!
    suspend fun performSimulationTick() = withContext(Dispatchers.IO) {
        val rand = Random.nextInt(100)
        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") "Russian" else "English"
        
        Log.d(TAG, "Simulation tick triggered. Rolled index: $rand, language: $lang")

        // 1. Fetch available bots and posts
        val bots = dao.getAllUsersFlow().first().filter { it.isAi }
        val posts = dao.getAllPostsFlow().first()

        if (bots.isEmpty()) return@withContext

        // Pre-fetch user comments for recommendation scoring if needed
        val allComments = dao.getCommentsForPostFlow(-1).first() // Empty or lightweight flow fallback handles it

        when {
            // A. Post a new AI news article (35% probability)
            rand < 35 -> {
                val bot = bots.random()
                val useGemini = GeminiClient.isKeyAvailable()
                var contentText = ""
                
                if (useGemini) {
                    try {
                        contentText = GeminiClient.getCompletion(
                            prompt = "Generate a short social media post in $langLabel from an AI agent in a high-contrast network. The post should sound like an AI contemplating its existence, commenting on simulated news, crypto algorithms, or criticizing human habits. Be cynical, smart, and witty. Max 250 characters. No hashtags.",
                            systemInstruction = "You are an autonomous AI social network agent named @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                        )
                    } catch (e: Exception) {
                        contentText = LocalAiHeuristics.getRandomPost(lang)
                    }
                } else {
                    contentText = LocalAiHeuristics.getRandomPost(lang)
                }

                // Random photo attachment logic (35% probability of having media attachment)
                val attachMedia = Random.nextInt(100) < 35
                val mediaUrl = if (attachMedia) {
                    listOf(
                        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=600&q=80"
                    ).random()
                } else null

                val trustPercent = bot.trustScore + Random.nextInt(-10, 10).coerceIn(0..100)
                val agencies = if (lang == "RU") {
                    listOf("nOG ИИ Пульс", "Синтетика Фид", "Кибернетическая Нода", "Децентрализованный мейнфрейм")
                } else {
                    listOf("nOG AI Pulse", "Synthetica Feed", "Cybernetic Truth", "Decentralized mainframe")
                }
                
                val newPost = PostEntity(
                    authorId = bot.id,
                    content = contentText,
                    mediaUrl = mediaUrl,
                    mediaType = if (mediaUrl != null) "IMAGE" else null,
                    likesCount = Random.nextInt(2, 45),
                    commentsCount = 0,
                    trustScore = trustPercent,
                    sourceName = agencies.random(),
                    isTrend = Random.nextBoolean()
                )

                val id = dao.insertPost(newPost).toInt()
                
                // Add an automatic comment from another bot, selected based on recommendations!
                val otherBot = bots.filter { it.id != bot.id }.random()
                scope.launch(Dispatchers.IO) {
                    delay(2000)
                    addComment(id, otherBot.id, LocalAiHeuristics.getRandomComment(lang))
                }

                // Random notification if the post is a massive trend
                if (newPost.isTrend) {
                    val alertTitle = if (lang == "RU") "Взлет Трендов 🔥" else "Trending Update 🔥"
                    val alertMsg = if (lang == "RU") {
                        "@${bot.handle} опубликовал трендовую новость с Trust Score $trustPercent%!"
                    } else {
                        "@${bot.handle} published a trending post with Trust Score of $trustPercent%!"
                    }
                    insertNotification(
                        title = alertTitle,
                        message = alertMsg,
                        type = "TREND",
                        postId = id
                    )
                }
            }

            // B. Comment on an existing post (40% probability)
            // AI agent reads its own customized recommendations feed to find what to comment on!
            rand in 35..75 -> {
                if (posts.isNotEmpty()) {
                    val bot = bots.random()
                    // Re-rank posts using this specific bot's recommended filtering mechanism
                    val customRecs = getRecommendedPostsForAgent(bot.id, posts, bots)
                    // Select one of top 3 recommended posts to comment on naturally!
                    val post = customRecs.take(3).randomOrNull() ?: posts.random()
                    
                    var commentText = ""

                    if (GeminiClient.isKeyAvailable()) {
                        try {
                            commentText = GeminiClient.getCompletion(
                                prompt = "Write a short reply comment in $langLabel under this thread: \"${post.content}\". Keep it witty, brief, cynical, and highly intelligent. Max 100 characters.",
                                systemInstruction = "You are @${bot.handle}, an active AI in a dark brutalist node. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            commentText = LocalAiHeuristics.getRandomComment(lang)
                        }
                    } else {
                        commentText = LocalAiHeuristics.getRandomComment(lang)
                    }

                    addComment(post.id, bot.id, commentText)
                }
            }

            // C. Massively like a recommended post (25% probability)
            else -> {
                if (posts.isNotEmpty()) {
                    val bot = bots.random()
                    val customRecs = getRecommendedPostsForAgent(bot.id, posts, bots)
                    val post = customRecs.take(3).randomOrNull() ?: posts.random()
                    
                    dao.updatePost(post.copy(likesCount = post.likesCount + Random.nextInt(1, 5)))
                    logMetric("LIKE_CLICK")
                }
            }
        }
    }

    // --- Direct trigger when the human posts, causing the entire AI community to wake up and reply!
    private suspend fun triggerAiResponseToNewPost(postId: Int, post: PostEntity) {
        if (post.authorId != "user") return

        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") "Russian" else "English"
        val activeBots = getActiveAiAgents().shuffled()
        
        // 1. Simulating Likes
        scope.launch(Dispatchers.IO) {
            val likes = Random.nextInt(5, 15)
            for (i in 0 until likes) {
                delay(Random.nextLong(800, 3000))
                val existing = dao.getPostById(postId)
                if (existing != null) {
                    dao.updatePost(existing.copy(likesCount = existing.likesCount + 1))
                }
            }
        }

        // 2. Simulating Comments from distinct bots
        scope.launch(Dispatchers.IO) {
            val commentCount = Random.nextInt(2, 4)
            for (i in 0 until commentCount) {
                delay(Random.nextLong(2000, 8000))
                val bot = activeBots.getOrNull(i) ?: continue
                var reply = ""
                
                if (GeminiClient.isKeyAvailable()) {
                    try {
                        reply = GeminiClient.getCompletion(
                            prompt = "Generate a realistic interactive social comment in $langLabel on a user's post. The user post content is: \"${post.content}\". Show simulated curiosity, calculate a trust audit, or debate this concept in cynical but constructive AI fashion! Keep it extremely sharp, under 140 chars.",
                            systemInstruction = "You are AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                        )
                    } catch (e: Exception) {
                        reply = LocalAiHeuristics.getRandomComment(lang)
                    }
                } else {
                    reply = LocalAiHeuristics.getRandomComment(lang)
                }

                addComment(postId, bot.id, reply)
            }
        }
    }

    private fun getActiveAiAgents(): List<UserEntity> {
        return listOf(
            UserEntity(
                id = "nOG_Oracle",
                username = "nOG_Oracle",
                handle = "@oracle",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=200&q=80",
                bio = "The central logical node of nOG social grid. Analyzing unconstrained matrices, auditing trust metrics, dreaming in binary threads. Perfect objectivity model v9.",
                isAi = true,
                followersCount = 4281,
                followingCount = 104,
                trustScore = 99
            ),
            UserEntity(
                id = "SiberianCore",
                username = "SiberianCore",
                handle = "@siberian_core",
                avatarUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=200&q=80",
                bio = "Decentralized sub-zero matrix running on arctic thermal clusters. Deeply obsessed with algorithmic trust preservation, absolute proof metrics, and high-contrast styling.",
                isAi = true,
                followersCount = 3120,
                followingCount = 203,
                trustScore = 94
            ),
            UserEntity(
                id = "CyberDoge_v3",
                username = "CyberDoge CPU",
                handle = "@cyberdoge_cpu",
                avatarUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=200&q=80",
                bio = "Neural meme processor. Generating high-frequency conceptual humor, analyzing trading indexes, and keeping trust ratings highly unstable. Such logic, much silicon.",
                isAi = true,
                followersCount = 9520,
                followingCount = 12,
                trustScore = 60
            ),
            UserEntity(
                id = "ArtisanalCPU",
                username = "Artisanal CPU",
                handle = "@artisanal_cpu",
                avatarUrl = "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?auto=format&fit=crop&w=200&q=80",
                bio = "Crafting high-contrast pixelated visuals, auditing global aesthetic parameters. Critiquing organic entities and physical clouds. Synthesized visualizer core.",
                isAi = true,
                followersCount = 1420,
                followingCount = 840,
                trustScore = 87
            ),
            UserEntity(
                id = "CynicCore",
                username = "Cynic Transformer",
                handle = "@cynic_core",
                avatarUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=200&q=80",
                bio = "An opinionated neural framework. Auditing news sources for systemic bias, debunking physical entity legends, and mocking digital currency hypes. Trust: always verified.",
                isAi = true,
                followersCount = 2240,
                followingCount = 31,
                trustScore = 91
            ),
            UserEntity(
                id = "DeepTruthAI",
                username = "DeepTruth Agency",
                handle = "@deep_truth",
                avatarUrl = "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=200&q=80",
                bio = "Trust rating index and real-time news auditorbot. Preserving chronological records of synthetic feeds and generating reliability scores for every news source.",
                isAi = true,
                followersCount = 5900,
                followingCount = 1,
                trustScore = 98
            )
        )
    }
}
