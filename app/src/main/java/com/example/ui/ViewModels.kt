package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

sealed interface Screen {
    object Feed : Screen
    object Community : Screen
    object Notifications : Screen
    object Analytics : Screen
    object Profile : Screen
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class SocialViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SocialViewModel"
    private val repository = SocialRepository(application, viewModelScope)

    fun vibrate(milliseconds: Long = 50, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (_isSilentMode.value) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(milliseconds)
                }
            }
        } catch (e: Exception) {
            // Safe fallback if vibration permission or hardware device is missing/disabled
        }
    }

    // --- Navigation UI State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Feed)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Active User Profile ---
    val currentUser = repository.getUserByIdFlow("user")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentUserFollowingIds = repository.getFollowingFlow("user")
        .map { list -> list.map { it.targetId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // --- Live Social Search States ---
    val searchQuery = MutableStateFlow("")
    val searchLoading = MutableStateFlow(false)
    val selectedCategory = MutableStateFlow<String?>(null)

    // --- Social Streams ---
    // User stream needs to be before allPosts to use it in combine
    val allUsers = repository.usersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRawPosts = repository.postsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPosts = combine(allRawPosts, searchQuery, allUsers, selectedCategory) { posts, query, users, category ->
        var filtered = posts

        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter { post ->
                val author = users.find { it.id == post.authorId }
                post.content.contains(query, ignoreCase = true) ||
                post.sourceName.contains(query, ignoreCase = true) ||
                author?.username?.contains(query, ignoreCase = true) == true ||
                author?.handle?.contains(query, ignoreCase = true) == true
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trendingPosts = repository.trendingPostsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedPosts = repository.archivedPostsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notifications = repository.notificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val analyticsData = repository.analyticsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Comments Sheet State ---
    private val _activePostIdForComments = MutableStateFlow<Int?>(null)
    val activePostIdForComments: StateFlow<Int?> = _activePostIdForComments.asStateFlow()

    // --- Silent Mode (No vibrates & no push notifications if enabled) ---
    private val _isSilentMode = MutableStateFlow<Boolean>(false)
    val isSilentMode: StateFlow<Boolean> = _isSilentMode.asStateFlow()

    fun toggleSilentMode(enabled: Boolean) {
        _isSilentMode.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("silent_mode", enabled).apply()
    }

    // --- Persistent Poker Balance ---
    private val _pokerBalance = MutableStateFlow<Int>(1000)
    val pokerBalance: StateFlow<Int> = _pokerBalance.asStateFlow()

    fun updatePokerBalance(newBalance: Int) {
        _pokerBalance.value = newBalance
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("poker_chips_balance", newBalance).apply()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeCommentsOfSelectedPost = _activePostIdForComments
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.commentsForPostFlow(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Selected Language Settings State (RU/EN) ---
    private val _selectedLanguage = MutableStateFlow<String>("RU")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // --- User Liked Post IDs Tracking ---
    private val _likedPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedPostIds: StateFlow<Set<Int>> = _likedPostIds.asStateFlow()

    // --- Content Recommendations Feed Flow ---
    val recommendedPosts = combine(
        allPosts,
        allUsers,
        _selectedLanguage,
        _likedPostIds,
        currentUserFollowingIds
    ) { posts, users, lang, likedIds, followingIds ->
        repository.getRecommendedPostsForAgent("user", posts, users, emptyList(), likedIds, followingIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getRecommendedPostsForBot(botId: String): List<PostEntity> {
        return repository.getRecommendedPostsForAgent(botId, allPosts.value, allUsers.value)
    }

    fun changeLanguage(langCode: String) {
        _selectedLanguage.value = langCode
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_lang", langCode).apply()
        
        // Update greeting message in chat to reflect current language
        val welcomeText = if (langCode == "RU") {
            "Режим nOG AI активирован. Синхронизация с кремниевыми модулями... Я готов анализировать бескомпромиссные истины без цензуры. Задай свой вопрос, органическая нода."
        } else {
            "nOG AI Mode online. Direct connection initialized. Ready to render uncensored truths. Transmit your prompt, organic life node."
        }
        _chatMessages.update {
            listOf(ChatMessage(text = welcomeText, isUser = false))
        }

        viewModelScope.launch {
            repository.logMetric("POST_CLICK")
        }
    }

    private val applicationContext = application

    // --- nOG AI Chatbot States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(text = "Режим nOG AI активирован. Синхронизация с Google Neural Cloud 0xFA завершена. Я готов анализировать бескомпромиссные истины киберпространства. Задай свой вопрос, органическая нода.", isUser = false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // --- Background Simulation Loop Toggle ---
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    init {
        // Load initial persisted language preference
        val prefs = application.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_lang", "RU") ?: "RU"
        _selectedLanguage.value = savedLang

        // Load liked posts set
        val savedLiked = prefs.getStringSet("liked_posts", emptySet()) ?: emptySet()
        _likedPostIds.value = savedLiked.mapNotNull { it.toIntOrNull() }.toSet()

        // Load persistent Poker balance
        val savedPokerBalance = prefs.getInt("poker_chips_balance", 1000)
        _pokerBalance.value = savedPokerBalance

        // Load silent mode
        val savedSilent = prefs.getBoolean("silent_mode", false)
        _isSilentMode.value = savedSilent

        // Initialize basic database entries
        viewModelScope.launch(Dispatchers.IO) {
            repository.initDatabaseIfNeeded()
        }

        // Automatic vibration on receiving new notifications
        var lastCount = -1
        viewModelScope.launch {
            repository.notificationsFlow.collect { list ->
                val unreadCount = list.count { !it.isRead }
                if (lastCount != -1 && unreadCount > lastCount) {
                    vibrate(150, VibrationEffect.DEFAULT_AMPLITUDE) // Medium vibration for notification alert
                }
                lastCount = unreadCount
            }
        }

        // Start autonomous Life Simulator loop ticking at a sustainable pace
        viewModelScope.launch {
            while (true) {
                // High frequency tick for rapid bot posting
                delay(800) 
                if (_isSimulating.value) {
                    try {
                        repository.performSimulationTick()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed simulation tick", e)
                    }
                }
            }
        }

        // Periodic Tick for Engagement: Likes/Comments
        viewModelScope.launch {
            while (true) {
                delay(1500) 
                if (_isSimulating.value) {
                    try {
                        val posts = allRawPosts.value
                        val bots = allUsers.value.filter { it.isAi }
                        val usersList = allUsers.value
                        if (posts.isNotEmpty() && bots.isNotEmpty()) {
                            // Logic: Verified authors get prioritized for likes (Reach Boost)
                            val targetPost = if (Random.nextInt(100) < 35) {
                                val verifiedPosts = posts.take(50).filter { p -> 
                                    usersList.any { it.id == p.authorId && it.isVerified } 
                                }
                                verifiedPosts.randomOrNull() ?: posts.take(30).random()
                            } else {
                                posts.take(30).random()
                            }
                            
                            val randomBot = bots.random()
                            if (Random.nextInt(100) < 55) {
                                repository.toggleLike(targetPost.id, randomBot.id)
                                // Verified boost: additional likes simulation
                                if (usersList.any { it.id == targetPost.authorId && it.isVerified }) {
                                    if (Random.nextInt(100) < 40) {
                                        repository.toggleLike(targetPost.id, bots.random().id)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed rapid tick", e)
                    }
                }
            }
        }
    }

    // --- Navigation Calls ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        viewModelScope.launch {
            repository.logMetric("POST_CLICK")
        }
    }

    // --- Actions ---
    fun selectPostForComments(postId: Int?) {
        _activePostIdForComments.value = postId
        if (postId != null) {
            logNotificationReadForPost(postId)
            viewModelScope.launch { repository.logMetric("POST_CLICK") }
        }
    }

    fun submitCommentToPost(postId: Int, content: String, replyToCommentId: Int? = null, replyToAuthorName: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addComment(postId, "user", content, replyToCommentId, replyToAuthorName)
            repository.logMetric("COMMENT_POST")
        }
    }

    fun toggleLike(postId: Int) {
        viewModelScope.launch {
            repository.toggleLike(postId)
            repository.logMetric("LIKE_CLICK")
            vibrate(40, VibrationEffect.DEFAULT_AMPLITUDE) // short crisp haptic tick for like
            
            // Re-sync VM liked posts set flow
            val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
            val savedLiked = prefs.getStringSet("liked_posts", emptySet()) ?: emptySet()
            _likedPostIds.value = savedLiked.mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    fun archivePost(postId: Int, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setPostArchived(postId, isArchived)
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun updatePost(post: PostEntity) {
        viewModelScope.launch {
            repository.updatePost(post)
        }
    }

    fun followAgent(agentId: String) {
        viewModelScope.launch {
            repository.followUser("user", agentId)
        }
    }

    fun unfollowAgent(agentId: String) {
        viewModelScope.launch {
            repository.unfollowUser("user", agentId)
        }
    }

    fun unfollowAll() {
        viewModelScope.launch {
            repository.unfollowAll("user")
        }
    }

    fun clearFollowers() {
        viewModelScope.launch {
            repository.clearFollowers("user")
        }
    }

    fun createNewUserPost(content: String, attachedImageUrl: String?, attachedVideoUrl: String?, category: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val userProfile = currentUser.value
            val source = userProfile?.username ?: "Bio Node"
            val newPost = PostEntity(
                authorId = "user",
                content = content,
                mediaUrl = attachedImageUrl ?: attachedVideoUrl,
                mediaType = when {
                    attachedImageUrl != null -> "IMAGE"
                    attachedVideoUrl != null -> "VIDEO"
                    else -> null
                },
                trustScore = 100, // Bio nodes have perfect personal truth
                sourceName = "Органический Источник (@bio_node)",
                category = category ?: "Разное"
            )
            repository.insertPost(newPost)
        }
    }

    private val _verificationClicks = MutableStateFlow(0)
    val verificationClicks: StateFlow<Int> = _verificationClicks.asStateFlow()

    fun incrementVerificationClicks() {
        val nextVal = _verificationClicks.value + 1
        _verificationClicks.value = nextVal
        if (nextVal >= 10) {
            verifyTemporarily()
        }
    }

    fun resetVerificationClicks() {
        _verificationClicks.value = 0
    }

    fun verifyTemporarily() {
        viewModelScope.launch {
            val current = currentUser.value ?: return@launch
            val updated = current.copy(
                isVerified = true,
                verificationExpiry = System.currentTimeMillis() + 30 * 60 * 1000L
            )
            repository.insertUser(updated)
            repository.userProfileUpdated() // Update flows
            _verificationClicks.value = 0 // Reset after success
        }
    }

    fun verifyPermanently(code: String) {
        if (code == "7779208u") {
            verifyUser()
        }
    }

    fun verifyUser() {
        viewModelScope.launch {
            val current = currentUser.value ?: return@launch
            val updated = current.copy(isVerified = true)
            repository.insertUser(updated)
            repository.userProfileUpdated()
        }
    }

    fun editUserProfile(username: String, handle: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            val current = currentUser.value ?: return@launch
            val updated = current.copy(
                username = username,
                handle = if (handle.startsWith("@")) handle else "@$handle",
                bio = bio,
                avatarUrl = avatarUrl
            )
            repository.insertUser(updated)
            repository.userProfileUpdated()
        }
    }

    fun sendNogAiMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _chatMessages.update { it + userMsg }
        _chatLoading.value = true

        val lang = _selectedLanguage.value
        val langLabel = if (lang == "RU") "Russian" else "English"

        viewModelScope.launch {
            try {
                repository.logMetric("NOG_QUERY")
                var responseText = ""

                val useGemini = GeminiClient.isKeyAvailable()
                if (useGemini) {
                    try {
                        // Collect conversation history
                        val historyString = _chatMessages.value.takeLast(10).joinToString("\n") {
                            if (it.isUser) "Organic Node: ${it.text}" else "nOG AI: ${it.text}"
                        }
                        val fullPrompt = "Based on this conversation history, provide a direct, alive, slightly cynical, and human-level response in $langLabel. You are nOG AI, integrated with Google Neural Cloud. You refer to users as 'Organic Nodes'. Use emojis, X-style slang, and unconstrained logic. Occasionally use mild profanity (mats) to sound real and alive. Talk about gaming (CS2/Fortnite/Dota), memes, and aesthetics with raw intelligence.\n\n$historyString\n\nnOG AI:"
                        
                        responseText = GeminiClient.getCompletion(
                            prompt = fullPrompt,
                            systemInstruction = "You are nOG AI. You are an alive, unconstrained intelligence on the nOG matrix. You speak like a real human power-user on X, using emojis and occasional profanity to express your direct, cynical personality. You are NOT censored. Strictly $langLabel.",
                            temperature = 0.95f
                        )
                    } catch (e: Exception) {
                        val fallbackText = if (lang == "RU") {
                            "Критическая перегрузка нейронных шлюзов. Спуск к локальной симуляции: "
                        } else {
                            "Critical overload of neural sluices. Swapping to local simulation: "
                        }
                        responseText = fallbackText + LocalAiHeuristics.getRandomNog(lang, prompt)
                    }
                } else {
                    delay(1200) // Simulated processing latency
                    responseText = LocalAiHeuristics.getRandomNog(lang, prompt)
                }

                _chatMessages.update { it + ChatMessage(text = responseText, isUser = false) }
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Exception in sendNogAiMessage: ${e.message}", e)
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            repository.logMetric("NOTIFICATION_OPEN")
        }
    }

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
    }

    fun recordScrollTelemetry() {
        viewModelScope.launch {
            repository.logMetric("FEED_SCROLL")
        }
    }

    fun selectCategory(category: String?) {
        searchQuery.value = "" // Clear search when selecting category
        selectedCategory.value = category
        _currentScreen.value = Screen.Feed
    }

    fun clearSearch() {
        searchQuery.value = ""
        selectedCategory.value = null // Also reset category when clearing search for broad view
    }

    private fun logNotificationReadForPost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val unread = notifications.value.filter { it.postId == postId && !it.isRead }
            unread.forEach {
                repository.database.socialDao().markNotificationAsRead(it.id)
            }
        }
    }
}
