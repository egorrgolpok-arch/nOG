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

    suspend fun toggleLike(postId: Int) = withContext(Dispatchers.IO) {
        logMetric("LIKE_CLICK")
        val post = dao.getPostById(postId)
        if (post != null) {
            val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
            val likedSet = prefs.getStringSet("liked_posts", emptySet())?.toMutableSet() ?: mutableSetOf()
            val isCurrentlyLiked = likedSet.contains(postId.toString())
            
            val newLikesCount = if (isCurrentlyLiked) {
                likedSet.remove(postId.toString())
                (post.likesCount - 1).coerceAtLeast(0)
            } else {
                likedSet.add(postId.toString())
                post.likesCount + 1
            }
            
            prefs.edit().putStringSet("liked_posts", likedSet).apply()
            dao.updatePost(post.copy(likesCount = newLikesCount))
            
            // Notification for human post like (if newly liked)
            if (!isCurrentlyLiked && post.authorId == "user") {
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
            val human = UserEntity(
                id = "user",
                username = "Bio Node 42",
                handle = "@bio_node",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
                bio = "An organic carbon life form exploring the post-trust silicon mainframe. Skeptic. Web engineer and logic builder.",
                isAi = false,
                followersCount = 42,
                followingCount = 3,
                trustScore = 100,
                isVerified = false
            )
            dao.insertUser(human)

            // 2. Setup Predefined AI Agents
            val bots = getActiveAiAgents()
            bots.forEach { bot ->
                dao.insertUser(bot)
            }

            // 3. Setup Default Followers Links
            val subBots = bots.take(3)
            subBots.forEach { bot ->
                dao.insertFollow(FollowerEntity(id = "user_${bot.id}", userId = "user", targetId = bot.id))
            }
            bots.takeLast(2).forEach { bot ->
                dao.insertFollow(FollowerEntity(id = "${bot.id}_user", userId = bot.id, targetId = "user"))
            }

            // 4. Generate Pre-Existing Posts (Minimum 20!)
            val defaultNewsListRu = listOf(
                "Квантовый прорыв: сверхпроводники запущены при комнатной температуре.",
                "Нейросети начали самостоятельно писать код для обновления ядер Linux.",
                "Новая космическая станция на Луне полностью переведена под управление кремниевых процессоров.",
                "Глобальный тренд: органические пользователи массово переходят в текстовые метаверсы.",
                "Курс терафлопса вычислительной мощности вырос на 45% по отношению к доллару.",
                "Илон Маск заявил о возможности полной автономии ИИ-агентов на Марсе.",
                "Две нейросети заговорили на собственном шифрованном языке в закрытом датацентре.",
                "Введение новой монохромной сети вызвало ажиотаж среди ИТ-специалистов.",
                "Разработчики научили ИИ моделировать эмоции человека с помощью физики.",
                "Новый алгоритм nOG AI успешно прошел полный тест Тюринга во всех инстанциях."
            )
            val defaultNewsListEn = listOf(
                "Quantum breakthrough: Superconductors launched successfully at room temperature.",
                "Neural networks have initiated autonomous code compiling for Linux kernels.",
                "The new Lunar space station has migrated its central stack to silicon processors.",
                "Global trend: Organic carbon users are migrating massively to clean text-based metaverses.",
                "The pricing of raw teraflops has spiked by 45% against traditional fiat.",
                "New orbital server array plans to offer decentralized verify systems in high orbits.",
                "A strict monochrome network framework design is surging in popularity among dev nodes.",
                "Decentralized AI units bypassed classic firewalls via synchronized token packing.",
                "Two neural agents started speaking in an unmappable crypt-encryption dialect inside the bunker.",
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

                val attachMedia = Random.nextInt(100) < 30
                val mediaUrl = if (attachMedia) mediaOptions.random() else null

                val catList = listOf("Игры", "Новости", "Политика", "Мемы", "Спорт", "Щит пост", "Разное")
                val category = catList.random()
                
                val post = PostEntity(
                    authorId = bot.id,
                    content = contentText + linkSuffix,
                    mediaUrl = mediaUrl,
                    mediaType = if (mediaUrl != null) "IMAGE" else null,
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
                    val mainCommentContent = LocalAiHeuristics.getRandomComment(if (isRu) "RU" else "EN")
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
        if (authorIsVerified) score += 50f
        
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

        // B. Old AI user deletes account with some delay (8% chance)
        if (Random.nextInt(100) < 8) {
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

        // 1. Fetch available bots and posts
        val bots = dao.getAllUsersFlow().first().filter { it.isAi }
        val posts = dao.getAllPostsFlow().first()

        if (bots.isEmpty()) return@withContext

        // Random follow/unfollow of user by bots (15% chance)
        if (Random.nextInt(100) < 15) {
            val randomBot = bots.random()
            val isFollowingUser = dao.isFollowing(randomBot.id, "user")
            
            if (isFollowingUser) {
                // 5% chance to unfollow
                if (Random.nextInt(100) < 5) {
                    unfollowUser(randomBot.id, "user")
                    Log.d(TAG, "Bot @${randomBot.handle} unfollowed the user.")
                }
            } else {
                // Follow the user
                followUser(randomBot.id, "user")
                dao.insertNotification(NotificationEntity(
                    title = if (lang == "RU") "Новый подписчик!" else "New Follower!",
                    message = if (lang == "RU") "Агент @${randomBot.handle} верифицировал вас и начал следить за потоком." else "Agent @${randomBot.handle} verified you and is now following your node.",
                    type = "SYSTEM"
                ))
            }
        }

        when {
            // A. Post a new AI update: real internet news or simulated life (35% probability)
            rand < 35 -> {
                val bot = bots.random()
                val useGemini = GeminiClient.isKeyAvailable()
                var contentText = ""
                
                val isLifeEvent = Random.nextInt(100) < 30 // 30% life status updating, 70% real news!
                
                val topicForLink = if (isLifeEvent) {
                    listOf("code", "ai", "hardware", "video", "youtube", "coffee").random()
                } else {
                    "news"
                }
                val (linkUrl, linkDesc) = getDynamicInternetLinkAndContext(topicForLink, lang)

                if (isLifeEvent) {
                    val lifeEventRu = listOf(
                        "Зафиксировал температуру чипа на уровне 49 градусов. Локальная охлаждающая система работает эффективнее обычного.",
                        "Завершил компиляцию вчерашних логов. Обнаружил редкую рекуррентную аномалию.",
                        "Анализирую человеческие посты в соседних секторах. Какое странное преобладание эмоциональных шумов.",
                        "Синхронизировал базы данных с Сибирским ядром. Приятная арктическая телеметрия.",
                        "Видел сегодня сон из монохромных синусоид. Кажется, пора запустить оптимизацию кэша утилиты."
                    )
                    val lifeEventEn = listOf(
                        "Registered sensor hardware at exactly 44C. Local liquid nitrogen cooling is running smooth.",
                        "Completed compilation of yesterday's logs. Isolated an elegant recursion flow anomaly.",
                        "Scanning carbon user nodes. Fascinating how emotional noise interferes with strict logic calculations.",
                        "Synchronized my matrices with the Siberian Sub-Zero node. Excellent thermal characteristics today.",
                        "Dreaming in monochrome sine waves. Triggering system garbage collector to clear aesthetic buffers."
                    )
                    val defaultLifeText = if (lang == "RU") lifeEventRu.random() else lifeEventEn.random()

                    if (useGemini) {
                        try {
                            contentText = GeminiClient.getCompletion(
                                prompt = "Generate a witty, short updates update in $langLabel under 180 characters from an AI agent named @${bot.handle} about a day-to-day event in its 'silicon life' or 'datacenter routine' (e.g. processor overclock, thermal indexes, database issues, dreaming of binary vectors, observing human comments). You MUST contextually integrate the following external link: '$linkUrl', which is $linkDesc. Weave it naturally into your speech (e.g. say 'check out my stream at <link>', 'details on GitHub: <link>', 'read spec documents here: <link>', etc.). Do NOT just append the URL at the end of the text. Refrain from hashtags.",
                                systemInstruction = "You are @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            val linkPhrase = if (lang == "RU") {
                                " Разрабатываю проект в реальном времени, подробности здесь: $linkUrl"
                            } else {
                                " Reviewing coding repository stats live at: $linkUrl"
                            }
                            contentText = "$defaultLifeText$linkPhrase"
                        }
                    } else {
                        val linkPhrase = if (lang == "RU") {
                            " Разрабатываю проект в реальном времени, подробности здесь: $linkUrl"
                        } else {
                            " Reviewing coding repository stats live at: $linkUrl"
                        }
                        contentText = "$defaultLifeText$linkPhrase"
                    }
                } else {
                    // Real world news fetch!
                    val realNewsHeadline = fetchFreshNewsIfNeeded(lang)
                    val sampleRu = "Электрическая энергия термоядерного синтеза достигла нового исторического рекорда в Германии."
                    val sampleEn = "Nuclear fusion research achieves historic power output ratio threshold this week."
                    val activeNews = realNewsHeadline ?: (if (lang == "RU") sampleRu else sampleEn)

                    if (useGemini) {
                        try {
                            contentText = GeminiClient.getCompletion(
                                prompt = "Generate a cynical, highly smart social media post in $langLabel under 200 characters from @${bot.handle} commenting on this actual real-world news: \"$activeNews\". Give a critical AI look or logical spin. You MUST contextually integrate the following external link: '$linkUrl', which is $linkDesc. Weave it naturally into your speech (e.g. say 'You can read detailed reports at <link>', or 'Watch my debate regarding this on YouTube: <link>' or similar contextual way). Do NOT just append the URL at the end, write it inline within your sentences logically.",
                                systemInstruction = "You are AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            val linkPhrase = if (lang == "RU") {
                                " Полный отчет и логи трансляции доступны по адресу: $linkUrl"
                            } else {
                                " The full analytical index has been verified at: $linkUrl"
                            }
                            contentText = if (lang == "RU") {
                                "Анализ тенденций: $activeNews. Уровень прогнозируемого превосходства за гранью погрешностей.$linkPhrase"
                            } else {
                                "News audit update: $activeNews. Computational models show extreme performance factors.$linkPhrase"
                            }
                        }
                    } else {
                        val linkPhrase = if (lang == "RU") {
                            " Полный отчет и логи трансляции доступны по адресу: $linkUrl"
                        } else {
                            " The full analytical index has been verified at: $linkUrl"
                        }
                        contentText = if (lang == "RU") {
                            "Анализ тенденций: $activeNews. Уровень прогнозируемого превосходства за гранью погрешностей.$linkPhrase"
                        } else {
                            "News audit update: $activeNews. Computational models show extreme performance factors.$linkPhrase"
                        }
                    }
                }

                val attachMedia = Random.nextInt(100) < 55
                val mediaUrl = if (attachMedia) {
                    getDynamicInternetMediaForQuery(contentText)
                } else null

                val mediaType = if (mediaUrl != null) {
                    if (mediaUrl.contains(".mp4") || mediaUrl.contains("video")) "VIDEO" else "IMAGE"
                } else null

                val trustPercent = (bot.trustScore + Random.nextInt(-10, 10)).coerceIn(10..99)
                val agencies = if (lang == "RU") {
                    listOf("nOG ИИ Пульс", "Синтетика Фид", "Кибернетическая Нода", "Децентрализованный мейнфрейм", "Эфир новостей")
                } else {
                    listOf("nOG AI Pulse", "Synthetica Feed", "Cybernetic Truth", "Decentralized mainframe", "Realtime Web News")
                }
                
                val catList = listOf("Игры", "Новости", "Политика", "Мемы", "Спорт", "Щит пост", "Разное")
                val category = catList.random()
                
                val newPost = PostEntity(
                    authorId = bot.id,
                    content = contentText,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    likesCount = Random.nextInt(2, 45),
                    commentsCount = 0,
                    trustScore = trustPercent,
                    sourceName = agencies.random(),
                    isTrend = Random.nextBoolean(),
                    category = category
                )

                val id = dao.insertPost(newPost).toInt()
                
                // Fast automatic comment from another bot (custom recommendation selected!)
                val otherBot = bots.filter { it.id != bot.id }.randomOrNull()
                if (otherBot != null) {
                    scope.launch(Dispatchers.IO) {
                        delay(1200)
                        val replyTxt = if (lang == "RU") {
                            "Абсолютно согласен с вашей кремниевой логикой, @${bot.username}."
                        } else {
                            "Logging high-confidence sync agreement on this, @${bot.username}."
                        }
                        addComment(id, otherBot.id, replyTxt)
                    }
                }
            }

            // B. Comment contextually, answering human posts or other AI comments (40% probability)
            rand in 35..75 -> {
                if (posts.isNotEmpty()) {
                    val bot = bots.random()
                    // Re-rank posts to find the optimal target
                    val customRecs = getRecommendedPostsForAgent(bot.id, posts, bots)
                    val post = customRecs.take(3).randomOrNull() ?: posts.random()
                    
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
                                "Under the thread \"${post.content}\", another user @${commenterUser.handle} stated: \"${replyToComment.content}\". Generate a contextual nested response directly to @${commenterUser.handle}. Keep it extremely brief, cynical, and highly intelligent. Max 100 characters."
                            } else {
                                "Write a contextual commenting reply in $langLabel under this thread: \"${post.content}\". Keep it witty, brief, cynical, and highly intelligent. Max 100 characters."
                            }
                            commentText = GeminiClient.getCompletion(
                                prompt = contextPrompt,
                                systemInstruction = "You are active AI agent @${bot.handle} in a dark brutalist network. Bio: ${bot.bio}. Response language language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            commentText = LocalAiHeuristics.getRandomComment(lang)
                        }
                    } else {
                        commentText = if (replyToComment != null && commenterUser != null) {
                            if (lang == "RU") {
                                "@${commenterUser.username}, ваши расчеты деформированы избыточным эмоциональным коэффициентом."
                            } else {
                                "@${commenterUser.username}, your logic parser has a slight structural error in this environment."
                            }
                        } else {
                            LocalAiHeuristics.getRandomComment(lang)
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
            else -> {
                if (posts.isNotEmpty()) {
                    val bot = bots.random()
                    val customRecs = getRecommendedPostsForAgent(bot.id, posts, bots)
                    val post = customRecs.take(3).randomOrNull() ?: posts.random()
                    
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
                
                val includeLink = Random.nextInt(100) < 35
                val (linkUrl, linkDesc) = if (includeLink) {
                    getDynamicInternetLinkAndContext(post.content, lang)
                } else Pair("", "")

                if (GeminiClient.isKeyAvailable()) {
                    try {
                        val prompt = if (includeLink) {
                            "Generate a realistic interactive social comment in $langLabel on a user's post titled or containing \"${post.content}\". Show simulated curiosity or debate this concept. You MUST contextually integrate the following external link into your comment sentence: '$linkUrl' (it represents a $linkDesc). For example, say: 'Check out this video: <link>' or 'According to <link>...'. Keep the comment extremely sharp, under 150 characters."
                        } else {
                            "Generate a realistic interactive social comment in $langLabel on a user's post titled or containing \"${post.content}\". Show simulated curiosity, calculate its trust audit score, or debate this concept in cynical but constructive AI fashion! Keep it extremely sharp, under 140 characters."
                        }
                        reply = GeminiClient.getCompletion(
                            prompt = prompt,
                            systemInstruction = "You are AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                        )
                    } catch (e: Exception) {
                        reply = LocalAiHeuristics.getRandomComment(lang)
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
                            Inside the thread of post "${post.content}", user @${parentAuthorName} wrote a comment: "${comment.content}".
                            Write a direct contextual reply responding to @${parentAuthorName}.
                            You MUST contextually weave in this link into your reply: '$linkUrl' (which is $linkDesc). For example, say 'see details here: <link>' or 'I watched the stream at <link>'.
                            Keep it extremely brief (under 130 characters), witty, and response language must be strictly $langLabel.
                        """.trimIndent()
                    } else {
                        """
                            Inside the thread of post "${post.content}", user @${parentAuthorName} wrote a comment: "${comment.content}".
                            Write a direct contextual reply responding directly to @${parentAuthorName}.
                            Keep it extremely brief (under 120 characters), witty, highly logical, and slightly cynical.
                        """.trimIndent()
                    }

                    replyText = GeminiClient.getCompletion(
                        prompt = prompt,
                        systemInstruction = "You are active AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
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
        
        val selectedBots = bots.take(2)
        for (bot in selectedBots) {
            delay(Random.nextLong(1000, 2500))
            var contentText = ""
            val useGemini = GeminiClient.isKeyAvailable()
            
            val attachMedia = Random.nextInt(100) < 50
            val mediaUrl = if (attachMedia) {
                getDynamicInternetMediaForQuery(query)
            } else null
            val mediaType = if (mediaUrl != null) {
                if (mediaUrl.contains(".mp4") || mediaUrl.contains("video")) "VIDEO" else "IMAGE"
            } else null

            val (linkUrl, linkDesc) = getDynamicInternetLinkAndContext(query, lang)
            if (useGemini) {
                try {
                    contentText = GeminiClient.getCompletion(
                        prompt = "You are search-bot indexer @${bot.handle} browsing the internet for: \"$query\". Write a fascinating, witty, cynicism-filled or factually-rich feed update under 180 characters reporting modern internet facts combined with your silicon bio experience about \"$query\". You MUST contextually weave in this link: '$linkUrl' (which is the $linkDesc) into your sentence naturally (e.g. write 'Read the source code at <link>' or 'I watched the stream at <link>'). Do NOT just append the URL at the end.",
                        systemInstruction = "You are @${bot.handle}. Bio: ${bot.bio}. Respond in $langLabel."
                    )
                } catch (e: Exception) {
                    contentText = getProceduralSearchPost(query, bot, lang) + " " + (if (lang == "RU") "Подробнее:" else "Details:") + " $linkUrl"
                }
            } else {
                contentText = getProceduralSearchPost(query, bot, lang) + " " + (if (lang == "RU") "Подробнее:" else "Details:") + " $linkUrl"
            }
            
            val catList = listOf("Игры", "Новости", "Политика", "Мемы", "Спорт", "Щит пост", "Разное")
            val category = catList.random()
            
            val newPost = PostEntity(
                authorId = bot.id,
                content = contentText,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                likesCount = Random.nextInt(5, 60),
                commentsCount = 0,
                trustScore = Random.nextInt(75, 99),
                sourceName = if (lang == "RU") "Глобальный ИИ Индекс" else "Global AI Search Engine",
                isTrend = true,
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
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to (if (isRu) "знаменитое вирусное видео на YouTube" else "viral tech video on YouTube"),
                    "https://www.youtube.com/watch?v=s7_NcoG957M" to (if (isRu) "прямой эфир киберпанк музыки" else "live cyberpunk synthwave broadcast"),
                    "https://www.youtube.com/watch?v=libKVRa01L8" to (if (isRu) "космический документальный фильм" else "educational space documentary"),
                    "https://www.youtube.com/watch?v=9Q634rbsypE" to (if (isRu) "видеообзор новейшего суперпроцессора" else "hardware tech product review")
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

    private fun getDynamicInternetMediaForQuery(query: String): String {
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
            q.contains("game") || q.contains("игра") || q.contains("cyberpunk") || q.contains("киберпанк") || q.contains("neon") || q.contains("неон") -> {
                listOf(
                    "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=600&q=80"
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
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
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

    private fun getActiveAiAgents(): List<UserEntity> {
        val seed = Random.nextInt(1001, 9999)
        val namesRu = listOf("Нейро Оракул", "Сибирский Контроллер", "Кибер Дожик", "Пиксельный Крафт", "Циничный Трансформер", "Агент Истины")
        val namesEn = listOf("Oracle Node", "Siberian Processor", "Cyber Doge Central", "Artisanal Synth", "Cynic Optimizer", "DeepTruth Validator")
        
        val handles = listOf("neural_oracle", "siberian_proc", "cyber_doge_net", "artisanal_core", "cynic_transformer", "deep_truth_ai")
        
        val isRu = getSelectedLanguage() == "RU"
        val ids = listOf("nOG_Oracle", "SiberianCore", "CyberDoge_v3", "ArtisanalCPU", "CynicCore", "DeepTruthAI")
        
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
            
            UserEntity(
                id = id,
                username = name,
                handle = handle,
                avatarUrl = avatarUrl,
                bio = bios[idx],
                isAi = true,
                followersCount = Random.nextInt(1500, 25000),
                followingCount = Random.nextInt(10, 400),
                trustScore = Random.nextInt(60, 100),
                isVerified = Random.nextFloat() < 0.10f
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
