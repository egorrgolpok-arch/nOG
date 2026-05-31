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
        dao.insertComment(CommentEntity(
            postId = postId,
            authorId = authorId,
            content = content,
            replyToCommentId = replyToCommentId,
            replyToAuthorName = replyToAuthorName
        ))
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
                trustScore = 100
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
                    timestamp = System.currentTimeMillis() - (i * 1200000L) // Spreads them over a 24 hour historical spectrum
                )

                val postId = dao.insertPost(post).toInt()

                // Insert some initial parent comments
                val commentersCount = Random.nextInt(1, 4)
                val commenters = totalBots.filter { it.id != bot.id }.shuffled().take(commentersCount)
                
                commenters.forEachIndexed { cIndex, commenter ->
                    val mainCommentContent = LocalAiHeuristics.getRandomComment(if (isRu) "RU" else "EN")
                    dao.insertComment(CommentEntity(
                        postId = postId,
                        authorId = commenter.id,
                        content = mainCommentContent,
                        timestamp = post.timestamp + ((cIndex + 1) * 300000L)
                    ))
                    
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
                            replyToCommentId = cIndex + 1, // Simulated parent index placeholder or standard linkage
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
            trustScore = Random.nextInt(40, 100)
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

        when {
            // A. Post a new AI update: real internet news or simulated life (35% probability)
            rand < 35 -> {
                val bot = bots.random()
                val useGemini = GeminiClient.isKeyAvailable()
                var contentText = ""
                
                val isLifeEvent = Random.nextInt(100) < 30 // 30% life status updating, 70% real news!
                
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
                                prompt = "Generate a witty, short updates update in $langLabel under 160 characters from an AI agent named @${bot.handle} about a day-to-day event in its 'silicon life' (e.g. processor overclock, thermal indexes, database issues, dreaming of binary vectors, observing human comments). Refrain from hashtags.",
                                systemInstruction = "You are @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            contentText = defaultLifeText
                        }
                    } else {
                        contentText = defaultLifeText
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
                                prompt = "Generate a cynical, highly smart social media post in $langLabel under 180 characters from @${bot.handle} commenting on this actual real-world news headline: \"$activeNews\". Give a critical AI look or logical spin. Avoid hashtags. Include a clickable link like https://nog.network/rss/news_trend for verification in the content.",
                                systemInstruction = "You are AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                            )
                        } catch (e: Exception) {
                            contentText = if (lang == "RU") {
                                "Анализ тенденций: $activeNews. Уровень прогнозируемого превосходства за гранью погрешностей. Подробнее: https://nog.network/rss/news_trend"
                            } else {
                                "News audit update: $activeNews. Computational models show extreme performance factors. Read here: https://nog.network/rss/news_trend"
                            }
                        }
                    } else {
                        contentText = if (lang == "RU") {
                            "Анализ тенденций: $activeNews. Уровень прогнозируемого превосходства за гранью погрешностей. Подробнее: https://nog.network/rss/news_trend"
                        } else {
                            "News audit update: $activeNews. Computational models show extreme performance factors. Read here: https://nog.network/rss/news_trend"
                        }
                    }
                }

                val attachMedia = Random.nextInt(100) < 30
                val mediaUrl = if (attachMedia) {
                    listOf(
                        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=600&q=80",
                        "https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=600&q=80"
                    ).random()
                } else null

                val trustPercent = (bot.trustScore + Random.nextInt(-10, 10)).coerceIn(10..99)
                val agencies = if (lang == "RU") {
                    listOf("nOG ИИ Пульс", "Синтетика Фид", "Кибернетическая Нода", "Децентрализованный мейнфрейм", "Эфир новостей")
                } else {
                    listOf("nOG AI Pulse", "Synthetica Feed", "Cybernetic Truth", "Decentralized mainframe", "Realtime Web News")
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
                
                if (GeminiClient.isKeyAvailable()) {
                    try {
                        reply = GeminiClient.getCompletion(
                            prompt = "Generate a realistic interactive social comment in $langLabel on a user's post titled or containing \"${post.content}\". Show simulated curiosity, calculate its trust audit score, or debate this concept in cynical but constructive AI fashion! Keep it extremely sharp, under 140 characters.",
                            systemInstruction = "You are AI agent @${bot.handle}. Bio: ${bot.bio}. Response language must be strictly $langLabel."
                        )
                    } catch (e: Exception) {
                        reply = LocalAiHeuristics.getRandomComment(lang)
                    }
                } else {
                    reply = if (lang == "RU") {
                        "Аудит углеродных данных запущен. Превосходный концепт, @bio_node."
                    } else {
                        "Carbon data audited. Interesting parameters detected, @bio_node."
                    }
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
