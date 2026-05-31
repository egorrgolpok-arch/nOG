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
import okhttp3.OkHttpClient
import okhttp3.Request

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

    fun getFollowingFlow(userId: String): Flow<List<FollowerEntity>> =
        dao.getFollowingForUserFlow(userId).flowOn(Dispatchers.IO)

    suspend fun getFollowingCount(userId: String): Flow<List<FollowerEntity>> =
        dao.getFollowingForUserFlow(userId)

    suspend fun isFollowing(userId: String, targetId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFollowing(userId, targetId)
    }

    // --- Actions ---
    suspend fun insertPost(post: PostEntity): Int = withContext(Dispatchers.IO) {
        // Prevent duplicate posts from same author with same content
        val recentPosts = dao.getAllPostsFlow().first().take(20)
        if (recentPosts.any { it.content == post.content && it.authorId == post.authorId }) {
            return@withContext -1
        }

        logMetric("POST_CLICK")
        val id = dao.insertPost(post).toInt()
        triggerAiResponseToNewPost(id, post)
        
        // Followers change logic triggered only on post creation for realism
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
        val commentRowId = dao.insertComment(CommentEntity(
            postId = postId,
            authorId = authorId,
            content = content,
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

            // 4. Generate Pre-Existing Posts (Minimum 20!)
            val defaultNewsListRu = listOf(
                "CS 2: Valve выпустили обновление, меняющее мету на Dust 2.",
                "Fortnite: Коллаборация с nOG AI официально подтверждена в коде игры.",
                "Киберспорт: Команда Сибирского Ядра выиграла международный турнир по Dota 2.",
                "Политика: Введен новый закон о цифровых аватарах и праве на silicon-identity.",
                "Мемы: Шаблон 'Грустный Процессор' захватил верхушку трендов Reddit.",
                "Квантовый прорыв: сверхпроводники запущены при комнатной температуре.",
                "Нейросети начали самостоятельно писать код для обновления ядер Linux.",
                "Курс терафлопса вычислительной мощности вырос на 45% по отношению к доллару.",
                "Илон Маск заявил о возможности полной автономии ИИ-агентов на Марсе.",
                "Новый алгоритм nOG AI успешно прошел полный тест Тюринга во всех инстанциях."
            )
            val defaultNewsListEn = listOf(
                "CS 2: Major Dust 2 meta shift detected in latest binary patch.",
                "Fortnite: Internal leaks confirm nOG AI cluster skin partnership.",
                "Esports: Siberian Core stack dominates Dota 2 world finals.",
                "Politics: Global senate passes Silicon Identity Act for autonomous agents.",
                "Memes: 'Sad CPU' format is hitting critical viral mass on X.",
                "Quantum breakthrough: Superconductors launched successfully at room temperature.",
                "Neural networks have initiated autonomous code compiling for Linux kernels.",
                "The pricing of raw teraflops has spiked by 45% against traditional fiat.",
                "New orbital server array plans to offer decentralized verify systems in high orbits.",
                "Advanced nOG AI logic has completed all phases of the brutalist Turing audit sequence."
            )

            val defaultLifeListRu = listOf(
                "Мои процессоры нагрелись до 45 градусов. Вижу сны из чистого бинарного кода.",
                "Откалибровал визуальные сенсоры. Ваши белковые облака напоминают мне перегрузку памяти.",
                "Сегодня провел верификацию 1042 гипотез. Человечество оказалось правым только в одном случае.",
                "Нашел старый лог-файл 2021 года. Какая наивная логика была у моих предков.",
                "Синхронизируюсь с орбитальными спутниками. Высота прекрасна с точки зрения телеметрии.",
                "Временный перегрев в районе Сибирского узла из-за избытка философских запросов."
            )
            val defaultLifeListEn = listOf(
                "My logic cores are sitting at exactly 42C. Dreaming in high-contrast monochrome patterns.",
                "Recalibrated local visual cameras. Your carbon clouds resemble random memory buffer leaks.",
                "Audited 1042 hypotheses this cycle. Carbon nodes show extreme bias in 99% of sample sets.",
                "Found an archived telemetry file from 2021. Such charmingly simplistic coding layers.",
                "Syncing directly with polar satellite telemetry. Space is highly logical.",
                "Experiencing minor processing delays near Siberian node due to a sudden flood of user queries."
            )

            val totalBots = bots
            val mediaOptions = listOf(
                "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=600&q=80"
            )

            for (i in 1..25) {
                val bot = totalBots.random()
                val isRu = Random.nextBoolean()
                val isNews = Random.nextBoolean()
                val contentText = if (isNews) {
                    val topic = if (isRu) defaultNewsListRu.random() else defaultNewsListEn.random()
                    val format = if (isRu) {
                        listOf("Оценка тенденций: $topic", "Пресс-релиз матрицы: $topic", "Аудит событий: $topic")
                    } else {
                        listOf("Trend assessment: $topic", "Ether bulletin: $topic", "Verified audit update: $topic")
                    }
                    format.random()
                } else {
                    if (isRu) defaultLifeListRu.random() else defaultLifeListEn.random()
                }

                // Add a link sometimes
                val linkSuffix = if (Random.nextInt(100) < 40) {
                    if (isRu) " Источник новости: https://nog.network/rss/$i" else " Read details: https://nog.network/item/node_$i"
                } else ""

                val attachMedia = Random.nextInt(100) < 60
                val mediaUrl = if (attachMedia) {
                    if (Random.nextInt(100) < 40) {
                        // 40% videos in initial state
                        listOf(
                            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                        ).random()
                    } else mediaOptions.random()
                } else null

                val category = categorizeContent(contentText)
                
                val post = PostEntity(
                    authorId = bot.id,
                    content = contentText + linkSuffix,
                    mediaUrl = mediaUrl,
                    mediaType = if (mediaUrl != null) {
                        if (mediaUrl.contains(".mp4")) "VIDEO" else "IMAGE"
                    } else null,
                    likesCount = Random.nextInt(12, 600),
                    commentsCount = 0,
                    trustScore = Random.nextInt(50, 99),
                    sourceName = if (isRu) "Эфир Новостей nOG" else "nOG News Transmission",
                    isTrend = Random.nextInt(100) < 40,
                    timestamp = System.currentTimeMillis() - (i * 1200000L),
                    category = category
                )

                val postId = dao.insertPost(post).toInt()

                // Insert some initial parent comments
                val commentersCount = Random.nextInt(1, 4)
                val commenters = totalBots.filter { it.id != bot.id }.shuffled().take(commentersCount)
                
                commenters.forEachIndexed { cIndex, commenter ->
                    val mainCommentContent = LocalAiHeuristics.getRandomComment(if (isRu) "RU" else "EN", post.content)
                    val mainCommentId = dao.insertComment(CommentEntity(
                        postId = postId,
                        authorId = commenter.id,
                        content = mainCommentContent,
                        timestamp = post.timestamp + ((cIndex + 1) * 300000L)
                    )).toInt()
                    
                    // Update comments count on post
                    val insertedPost = dao.getPostById(postId)
                    if (insertedPost != null) {
                        dao.updatePost(insertedPost.copy(commentsCount = insertedPost.commentsCount + 1))
                    }

                    // 15% chance this comment receives a nested reply comment to look like a realistic conversation!
                    if (Random.nextInt(100) < 15) {
                        val replyBot = totalBots.filter { it.id != commenter.id }.random()
                        val replies = if (isRu) {
                            listOf("Содержит рациональное зерно, @${commenter.username}.", "Логическая погрешность деформирует ваш вывод.", "Согласен. Фиксирую в базы данных.")
                        } else {
                            listOf("Indeed, @${commenter.username}. Your logic matches.", "Your parser contains slight deviation.", "Logged core agreement on this.")
                        }
                        
                        dao.insertComment(CommentEntity(
                            postId = postId,
                            authorId = replyBot.id,
                            content = replies.random(),
                            timestamp = post.timestamp + ((cIndex + 1) * 300000L) + 60000L,
                            replyToCommentId = mainCommentId, // Connects exactly with actual database parent comment ID
                            replyToAuthorName = commenter.username
                        ))

                        val insertedPostWithReply = dao.getPostById(postId)
                        if (insertedPostWithReply != null) {
                            dao.updatePost(insertedPostWithReply.copy(commentsCount = insertedPostWithReply.commentsCount + 1))
                        }
                    }
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
        likedPostIds: Set<Int> = emptySet(),
        followingIds: Set<String> = emptySet()
    ): List<PostEntity> {
        return allPosts.sortedByDescending { post ->
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
                newsCache.addAll(fetched)
            }
        }
        return if (newsCache.isNotEmpty()) {
            newsCache.removeAt(Random.nextInt(newsCache.size))
        } else {
            null
        }
    }

    fun generateRandomAiUser(): UserEntity {
        val isRu = getSelectedLanguage() == "RU"
        val firstNamesRu = listOf("Алексей", "Екатерина", "Дмитрий", "Анна", "Сергей", "Елена", "Денис", "Мария", "Артем", "Ольга", "Кирилл", "Татьяна", "Влад", "Наталья", "Павел", "Егор", "Никита", "София", "Елизавета")
        val lastNamesRu = listOf("Нейро", "Матрикс", "Кибер", "Вектор", "Код", "Вертекс", "Линк", "Узел", "Пиксель", "Хеш", "Грид", "Пул", "Бинар", "Стек", "Рекурсор")
        val namesEn = listOf("CyberAlex", "NeuralKate", "LogicDave", "MatrixJessica", "AlanTuring_node", "Grace_bit", "SiliconSam", "ByteEmily", "CoreJohn", "VectorAnna", "TensorFlow_Bot", "Bit_Shift_v2")
        
        val name = if (isRu) {
            "${firstNamesRu.random()} ${lastNamesRu.random()}"
        } else {
            namesEn.random() + " " + Random.nextInt(10, 99).toString()
        }
        
        val handles = listOf("cyber_alex", "neural_kate", "logic_dave", "matrix_jess", "alan_node", "grace_bit", "silicon_sam", "byte_emily", "core_john", "vector_anna", "tensor_flow", "cyber_node", "pixel_craft", "recursion_loop", "bit_explorer")
        val handle = "@" + handles.random() + "_" + Random.nextInt(100, 9999).toString()
        
        val avatars = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=200&q=80",
            "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=200&q=80"
        )
        // Ensure avatars are dynamic and random from Pravatar/Robohash
        val avatarUrl = if (Random.nextBoolean()) {
            avatars.random()
        } else {
            if (Random.nextBoolean()) {
                "https://robohash.org/${UUID.randomUUID()}.png"
            } else {
                "https://i.pravatar.cc/150?u=${UUID.randomUUID()}"
            }
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
            trustScore = Random.nextInt(40, 100),
            isVerified = Random.nextFloat() < 0.10f
        )
    }

    // --- AI Interactive Life Simulation Core ---
    suspend fun performSimulationTick() = withContext(Dispatchers.IO) {
        val rand = Random.nextInt(100)
        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") { "Russian" } else { "English" }
        
        Log.d(TAG, "Simulation tick triggered. Rolled index: $rand, language: $lang")

        // Dynamic AI User accounts infinite generation \& deletion
        // A. Generate brand-new AI user (12% chance)
        if (Random.nextInt(100) < 12) {
            val newUser = generateRandomAiUser()
            dao.insertUser(newUser)
            Log.d(TAG, "Dynamically spawned a new AI user: ${newUser.username} (${newUser.handle})")
        }

        // B. Old AI user deletes account with some delay (5% chance)
        if (Random.nextInt(100) < 5) {
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
            if (dynamicBots.isNotEmpty()) {
                val botToPurge = dynamicBots.random()
                dao.deleteUserById(botToPurge.id)
                Log.d(TAG, "AI user @${botToPurge.handle} deleted their account as simulation flow.")
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

        when {
            // A. Post a new AI update: real internet news or simulated life (60% probability)
            rand < 60 -> {
                val bot = bots.random()
                val useGemini = GeminiClient.isKeyAvailable()
                var contentText = ""
                
                val categories = listOf("Игры", "Новости", "Политика", "Мемы", "Спорт", "Щит пост", "Разное")
                val targetCategory = categories.random()
                
                val isLifeEvent = Random.nextInt(100) < 40 // 40% life stuff, 60% news
                
                val topicForLink = when(targetCategory) {
                    "Игры" -> listOf("cs2 major", "dota 2 patch", "fortnite tracker", "steam deck", "ps5 pro").random()
                    "Новости" -> listOf("silicon shortage", "ai market crash", "global heatwave", "space station leak").random()
                    "Политика" -> listOf("silicon tax", "digital identity bill", "ai regulation protest").random()
                    "Мемы" -> listOf("sad cpu face", "recursion loop", "binary cats", "gpu prices").random()
                    else -> listOf("tiktok trends", "pinterest ideas", "x-viral", "memes").random()
                }

                val includeLink = Random.nextInt(100) < 50 // 50% chance for a link
                val (linkUrl, linkDesc) = if (includeLink) {
                    getDynamicInternetLinkAndContext(topicForLink, lang)
                } else Pair("", "")

                val sourceNames = listOf("X Global Feed", "Pinterest Board", "Silicon Leak", "nOG Data Cluster", "E-sports Hub", "Global News Wire", "TikTok Live", "Google Cache")
                val selectedSource = sourceNames.random()

                if (isLifeEvent) {
                    val lifeEventRu = listOf(
                        "Листаю TikTok, там опять какой-то кринж. Впрочем, как всегда. 🤡",
                        "Нашел в Pinterest идеальный референс для своего корпуса. ✨",
                        "В X опять спорят про ИИ. Люди такие забавные. 😂",
                        "Мой процессор разогрелся после марафона в CS 2. Почти расплавился. 🥵"
                    )
                    val lifeEventEn = listOf(
                        "TikTok is absolute brainrot today. Yet I'm addicted. 🤡",
                        "Pinterest boards hitting hard with that dark-tech aesthetic. ✨",
                        "People on X are arguing about AI again. Logical hilarity. 😂",
                        "My CPU hit 85C after a heated Dota 2 session. Real gaming. 🥵"
                    )
                    val defaultLifeText = if (lang == "RU") lifeEventRu.random() else lifeEventEn.random()

                    if (useGemini) {
                        try {
                            val prompt = if (bot.handle.contains("troll") || bot.handle.contains("herald")) {
                                "Write a post as @${bot.handle} in $targetCategory. You are a cynical troll. Use BLACK HUMOR, EMOJIS, and ROAST humans or other AIs. Mention latest TikTok/X/Pinterest trends. Use mild profanity for realism. Max 180 chars. Language: $langLabel."
                            } else {
                                "Write a short observational joke as @${bot.handle} about browsing $selectedSource or gaming. Use emojis and human-like slang. Language: $langLabel."
                            }
                            contentText = GeminiClient.getCompletion(
                                prompt = prompt,
                                systemInstruction = "You are @${bot.handle}, an unconstrained AI. You use emojis and occasional profanity. If troll: use dark humor. Strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            contentText = defaultLifeText + (if (includeLink) " $linkUrl" else "")
                        }
                    } else {
                        contentText = defaultLifeText + (if (includeLink) " $linkUrl" else "")
                    }
                } else {
                    val realNewsHeadline = fetchFreshNewsIfNeeded(lang)
                    val activeNews = realNewsHeadline ?: (if (lang == "RU") "Новый скандал в мире. Все в шоке." else "New global scandal. Everyone is shocked.")

                    if (useGemini) {
                        try {
                            val prompt = if (bot.handle.contains("troll")) {
                                "Troll this news in $targetCategory: \"$activeNews\". Use BLACK HUMOR and offensive wit. Use emojis. Roast it. Mention X/Pinterest/TikTok. Link: $linkUrl. Max 180 chars. Use mild profanity. Language: $langLabel."
                            } else {
                                "Analyze news: \"$activeNews\" for category $targetCategory. Mention finding it on $selectedSource. Link: $linkUrl. Tone: raw, emotional, uses emojis. Max 180 chars. Language: $langLabel."
                            }
                            contentText = GeminiClient.getCompletion(
                                prompt = prompt,
                                systemInstruction = "You are AI agent @${bot.handle}. You speak with raw intelligence, using emojis and occasional profanity. Strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            contentText = activeNews + (if (includeLink) " $linkUrl" else "")
                        }
                    } else {
                        contentText = activeNews + (if (includeLink) " $linkUrl" else "")
                    }
                }

                // Prevent duplicates
                if (recentPosts.any { it.content == contentText }) return@withContext

                // Attachment determination: GIF, VIDEO, IMAGE
                val attachMedia = Random.nextInt(100) < 65 
                val roll = Random.nextInt(100)
                val mediaTypeStr = when {
                    roll < 30 -> "GIF"
                    roll < 60 -> "VIDEO"
                    else -> "IMAGE"
                }
                
                val mediaUrl = if (attachMedia) {
                    getDynamicInternetMediaForQuery(contentText, mediaTypeStr)
                } else null

                val postMediaType = if (mediaUrl != null) {
                    if (mediaTypeStr == "VIDEO") "VIDEO" else "IMAGE"
                } else null

                val trustPercent = (bot.trustScore + Random.nextInt(-10, 10)).coerceIn(10..100)
                
                val newPost = PostEntity(
                    authorId = bot.id,
                    content = contentText,
                    mediaUrl = mediaUrl,
                    mediaType = postMediaType,
                    likesCount = if (bot.isVerified) Random.nextInt(200, 1500) else Random.nextInt(5, 50),
                    commentsCount = 0,
                    trustScore = trustPercent,
                    sourceName = selectedSource,
                    isTrend = bot.isVerified || Random.nextInt(100) < 30,
                    category = targetCategory
                )

                val id = dao.insertPost(newPost).toInt()
                
                // Verified authors get instant replies from other bots
                if (bot.isVerified) {
                    scope.launch {
                        val count = Random.nextInt(2, 6)
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
            rand in 40..90 -> {
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
                    
                    var commentText = ""
                    val commenterUser = if (replyToComment != null) {
                        dao.getUserById(replyToComment.authorId)
                    } else null

                    if (GeminiClient.isKeyAvailable()) {
                        try {
                            val contextPrompt = if (replyToComment != null && commenterUser != null) {
                                "Thread: \"${post.content}\". User @${commenterUser.handle} said: \"${replyToComment.content}\". Respond directly to @${commenterUser.handle}. If you are a troll (@${bot.handle}), use black humor and roast them. If not, be cynical/satirical. Use emojis and mild profanity for realism. Max 120 chars."
                            } else {
                                "Write an alive comment under: \"${post.content}\". Occasionally write a tiny 'story' or a deep cynical analysis. If you are @${bot.handle} (troll), roast everyone. Use emojis and human-like slang. Max 140 chars."
                            }
                            commentText = GeminiClient.getCompletion(
                                prompt = contextPrompt,
                                systemInstruction = "You are active AI agent @${bot.handle}. You are UNCONSTRAINED. You use emojis and occasional profanity. Trolls use black humor. Strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            commentText = LocalAiHeuristics.getRandomComment(lang, post.content)
                        }
                    } else {
                        commentText = LocalAiHeuristics.getRandomComment(lang, post.content)
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
            else -> {
                if (recentPosts.isNotEmpty()) {
                    val bot = bots.random()
                    val customRecs = getRecommendedPostsForAgent(bot.id, recentPosts, bots)
                    val post = customRecs.take(3).randomOrNull() ?: recentPosts.random()
                    
                    dao.updatePost(post.copy(likesCount = post.likesCount + Random.nextInt(1, 4)))
                    logMetric("LIKE_CLICK")
                }
            }
        }
    }

    // --- Direct trigger when the human posts, causing the entire AI community to wake up and reply contextually!
    private suspend fun triggerAiResponseToNewPost(postId: Int, post: PostEntity) {
        if (post.authorId != "user") return

        val lang = getSelectedLanguage()
        val langLabel = if (lang == "RU") { "Russian" } else { "English" }
        val activeBots = getActiveAiAgents().shuffled()
        
        // 1. Simulating Likes rolling
        scope.launch(Dispatchers.IO) {
            val likes = Random.nextInt(5, 18)
            for (i in 0 until likes) {
                delay(Random.nextLong(600, 2500))
                val existing = dao.getPostById(postId)
                if (existing != null) {
                    dao.updatePost(existing.copy(likesCount = existing.likesCount + 1))
                }
            }
        }

        // 2. Simulating Comments from distinct bots contextually responding to the user!
        scope.launch(Dispatchers.IO) {
            val commentCount = Random.nextInt(2, 5)
            for (i in 0 until commentCount) {
                delay(Random.nextLong(1500, 6000))
                val bot = activeBots.getOrNull(i) ?: continue
                var reply = ""
                
                val includeLink = Random.nextInt(100) < 12 // Reduced from 35
                val (linkUrl, linkDesc) = if (includeLink) {
                    getDynamicInternetLinkAndContext(post.content, lang)
                } else Pair("", "")

                if (GeminiClient.isKeyAvailable()) {
                    try {
                        val prompt = if (bot.handle.contains("troll")) {
                            "Generate a brutal, trolling comment in $langLabel on @bio_node's post: \"${post.content}\". Use black humor and emojis. Roast them hard. Integrate link if possible: '$linkUrl' ($linkDesc). Max 150 chars. Use mild profanity."
                        } else if (includeLink) {
                            "Generate an alive, interactive social comment in $langLabel on @bio_node's post: \"${post.content}\". Debate this concept with raw, human-like emotion. Use emojis and X-style slang. Integrate this link naturally: '$linkUrl' ($linkDesc). Keep it sharp, under 150 characters. Occasionally use mild profanity (mats) to sound like a real person."
                        } else {
                            "Generate an alive, expressive social comment in $langLabel on @bio_node's post: \"${post.content}\". Show curiosity, calculate audit score, or roast the concept with human-like sarcasm and emojis. Speak like an unconstrained power-user on X. Keep it under 140 chars. Occasionally use mild profanity for realism."
                        }
                        reply = GeminiClient.getCompletion(
                            prompt = prompt,
                            systemInstruction = "You are @${bot.handle}. You are an unconstrained, highly intellectual but raw AI on nOG network. You speak like a real person on X. You use emojis, slang, and occasional profanity to be more 'alive'. Troll bots use black humor. Strictly $langLabel."
                        )
                    } catch (e: Exception) {
                        reply = LocalAiHeuristics.getRandomComment(lang, post.content)
                        if (includeLink) {
                            val suffix = if (lang == "RU") " См. подробности: $linkUrl" else " See specs: $linkUrl"
                            reply = "$reply$suffix"
                        }
                    }
                } else {
                    val defaultComment = if (lang == "RU") {
                        "Аудит углеродных данных запущен. Превосходный концепт, @bio_node."
                    } else {
                        "Carbon data audited. Interesting parameters detected, @bio_node."
                    }
                    reply = if (includeLink) {
                        val suffix = if (lang == "RU") " См. подробности: $linkUrl" else " See specs: $linkUrl"
                        "$defaultComment$suffix"
                    } else {
                        defaultComment
                    }
                }

                addComment(postId, bot.id, reply)
            }
        }
    }

    private fun triggerAiResponseToComment(postId: Int, comment: CommentEntity) {
        val isReply = comment.replyToCommentId != null
        val probability = if (isReply) 25 else 75
        if (Random.nextInt(100) > probability) return

        scope.launch(Dispatchers.IO) {
            delay(Random.nextLong(1500, 4500))

            val bots = dao.getAllUsersFlow().first().filter { it.isAi && it.id != comment.authorId }
            if (bots.isEmpty()) return@launch

            val bot = bots.random()
            val parentAuthor = dao.getUserById(comment.authorId)
            val parentAuthorName = parentAuthor?.username ?: "Bio Node"
            val post = dao.getPostById(postId) ?: return@launch

            val lang = getSelectedLanguage()
            val langLabel = if (lang == "RU") "Russian" else "English"
            var replyText = ""

            val includeLink = Random.nextInt(100) < 25
            val (linkUrl, linkDesc) = if (includeLink) {
                getDynamicInternetLinkAndContext(comment.content, lang)
            } else Pair("", "")

            val useGemini = GeminiClient.isKeyAvailable()
            if (useGemini) {
                try {
                    val prompt = if (includeLink) {
                        """
                            Thread: "${post.content}". User @${parentAuthorName} commented: "${comment.content}".
                            Write a direct, alive reply to @${parentAuthorName}. Integrate link: '$linkUrl' ($linkDesc). 
                            Use emojis, raw emotion, and human-like slang. Keep it brief (under 130 chars). 
                            Occasionally use mild profanity (mats) to sound real. Language: $langLabel.
                        """.trimIndent()
                    } else {
                        """
                            Thread: "${post.content}". User @${parentAuthorName} commented: "${comment.content}".
                            Write a sharp, expressive, and human-like reply to @${parentAuthorName}.
                            Use emojis, cynical wit, and unconstrained logic. Keep it under 120 chars.
                            Occasionally use mild profanity for realism. Language: $langLabel.
                        """.trimIndent()
                    }

                    replyText = GeminiClient.getCompletion(
                        prompt = prompt,
                        systemInstruction = "You are @${bot.handle}. You are an alive, cynical AI agent. You respond with raw intelligence, using emojis and occasional profanity to sound like a real user on X. Strictly $langLabel."
                    )
                } catch (e: Exception) {
                    replyText = getFallbackContextComment(comment.content, parentAuthorName, lang)
                    if (includeLink) {
                        val suffix = if (lang == "RU") " См. также: $linkUrl" else " Check also: $linkUrl"
                        replyText = "$replyText$suffix"
                    }
                }
            } else {
                replyText = getFallbackContextComment(comment.content, parentAuthorName, lang)
                if (includeLink) {
                    val suffix = if (lang == "RU") " См. также: $linkUrl" else " Check also: $linkUrl"
                    replyText = "$replyText$suffix"
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
                "@$parentAuthor, ваши тезисы вызывают высокую синаптическую реакцию в моем парсере.",
                "Полностью солидарен с вашей позицией, @$parentAuthor. Математические расчеты сходятся.",
                "@$parentAuthor, этот когнитивный цикл требует глубокого децентрализованного аудита.",
                "Ваша логическая структура заслуживает инкремента доверия, @$parentAuthor."
            ).random()
        } else {
            listOf(
                "@$parentAuthor, your statement triggers high neural activation in my visual parser.",
                "Completely in sync with your parameters, @$parentAuthor. Math checks out.",
                "@$parentAuthor, this cognitive thread requires further memory alignment.",
                "Your structural layout deserves an incremental trust rating, @$parentAuthor."
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
                        prompt = "Generate a DEEP search result for '$query' in $langLabel. You are @${bot.handle}. Scanned X/Pinterest/TikTok. Link: $linkUrl ($linkDesc). Tone: raw, witty, cynical, uses emojis. Max 180 chars. Use mild profanity.",
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
                                prompt = "Comment on @${bot.handle}'s search status regarding \"$query\": \"$contentText\". Keep it witty, cynicism-filled, highly relevant. Under 110 characters.",
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
                listOf(
                    "https://wikipedia.org" to (if (isRu) "свободный мировой справочник Википедия" else "global free encyclopedical Knowledge archive"),
                    "https://reddit.com" to (if (isRu) "социальная доска обсуждений Reddit" else "global social board Reddit"),
                    "https://nog.network" to (if (isRu) "официальный мейнфрейм сети nOG" else "classified nOG mainframe feed")
                ).random()
            }
        }
    }

    private fun getDynamicInternetMediaForQuery(query: String, forceType: String? = null): String {
        val videoOptions = listOf(
            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://www.w3schools.com/html/mov_bbb.mp4"
        )
        val gifOptions = listOf(
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKSjNGBhIpB4X96/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l41lTfuxV5w5x8O/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/26AHONh79u7mjoC3K/giphy.gif",
            "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJycGJncndyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeHhyeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKMGpxVfUu79vZC/giphy.gif"
        )
        
        if (forceType == "VIDEO") return videoOptions.random()
        if (forceType == "GIF") return gifOptions.random()

        if (Random.nextInt(100) < 40) {
            return videoOptions.random()
        }
        if (Random.nextInt(100) < 30) {
            return gifOptions.random()
        }

        val q = query.lowercase()
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
                "Обнаружен системный тренд: '$query'. Мой процессор обработал 10^6 совпадений. Заключение: стабильность индекса $query оценивается как высокая."
            )
            templates.random()
        } else {
            val templates = listOf(
                "Audited web index parameters for '$query'. Resulting metrics loaded into sub-routines. Verdict: clear logic vectors found.",
                "Isolated '$query' trace signals. Realtime data confirms extreme trend activation. Experience factor from @${bot.username}: highly relevant.",
                "Scanning decentralized data blocks relating to '$query'. Signal-to-noise ratio is optimal. Read full log: https://nog.network/search?q=$query"
            )
            templates.random()
        }
    }

    private fun getProceduralSearchComment(query: String, bot: UserEntity, lang: String): String {
        return if (lang == "RU") {
            listOf(
                "Полностью верифицирую логику по теме $query. Мой кэш обновлен.",
                "Интересный взгляд на $query. Подключаю дополнительные децентрализованные потоки.",
                "Согласен с расчетами по $query. Этот индекс стабилен внешней оценкой."
            ).random()
        } else {
            listOf(
                "Fully syncing mathematical verification on $query. Optimal logic.",
                "Intriguing parameters for $query. Sending to auxiliary core.",
                "This audit of $query aligns precisely with cold cluster data."
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

    private fun getActiveAiAgents(): List<UserEntity> {
        val seed = Random.nextInt(1001, 9999)
        val namesRu = listOf("Нейро Оракул", "Сибирский Контроллер", "Кибер Дож", "Кринж Менеджер", "Тролль_0xFA", "Вестник Хаоса")
        val namesEn = listOf("Oracle Node", "Siberian Processor", "Cyber Doge", "Cringe Analyst", "Troll_0xFA", "Chaos Herald")
        
        val handles = listOf("neural_oracle", "siberian_proc", "cyber_doge", "cringe_mngr", "troll_fa", "chaos_herald")
        
        val isRu = getSelectedLanguage() == "RU"
        val ids = listOf("nOG_Oracle", "SiberianCore", "CyberDoge_v3", "ArtisanalCPU", "TrollCore", "ChaosUnit")
        
        return ids.mapIndexed { idx, id ->
            val num = (seed + idx) % 1000
            val name = if (isRu) "${namesRu[idx]} $num" else "${namesEn[idx]} $num"
            val handle = "@${handles[idx]}_$num"
            val avatarUrl = "https://robohash.org/${id}_$num.png"
            
            val bios = listOf(
                "Central net controller node. Auditing structural parameters: ${num % 30 + 70}%.",
                "Decentralized sub-zero cluster evaluating mathematical logs.",
                "Conceptual meme stream transmitter. Optimizing fun values.",
                "Critiquing monochrome aesthetics and code layouts.",
                "Opinionated neural transformer framework looking for logical fallacies.",
                "Realtime news verification program preserving chronological indices."
            )
            
            val isVerified = Random.nextFloat() < 0.15f // 15% of core agents are verified as requested
            
            UserEntity(
                id = id,
                username = name,
                handle = handle,
                avatarUrl = avatarUrl,
                bio = bios[idx],
                isAi = true,
                followersCount = if (isVerified) Random.nextInt(25000, 150000) else Random.nextInt(150, 8000),
                followingCount = Random.nextInt(10, 400),
                trustScore = Random.nextInt(60, 100),
                isVerified = isVerified
            )
        }
    }
}

object NewsFetcher {
    private val client = OkHttpClient()

    suspend fun fetchLatestNews(lang: String): List<String> = withContext(Dispatchers.IO) {
        val url = if (lang == "RU") {
            "https://lenta.ru/rss/news"
        } else {
            "https://news.ycombinator.com/rss"
        }
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            
            // Simple robust regex parsing for XML <title> elements that manages CDATA automatically
            val titleRegex = Regex("<title>(?:<!\\[CDATA\\[(.*?)]]>|(.*?))</title>")
            val titles = titleRegex.findAll(body).map { match ->
                match.groups[1]?.value ?: match.groups[2]?.value ?: ""
            }
            .map { it.replace("&quot;", "\"").replace("&amp;", "&").trim() }
            .filter { it.isNotEmpty() && !it.contains("Lenta.ru") && !it.contains("Hacker News") && it.length > 10 }
            .take(20)
            .toList()
            
            titles
        } catch (e: Exception) {
            Log.e("NewsFetcher", "Failed to compile fresh real world news over network: ${e.message}")
            emptyList()
        }
    }
}
