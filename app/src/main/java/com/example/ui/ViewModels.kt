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
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

sealed interface Screen {
    object Feed : Screen
    object Community : Screen
    object Notifications : Screen
    object Analytics : Screen
    object Casino : Screen
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

    val userContacts = MutableStateFlow<List<String>>(emptyList())
    val userGallery = MutableStateFlow<List<String>>(emptyList())

    fun loadDeviceData() {
        viewModelScope.launch(Dispatchers.IO) {
            userContacts.value = repository.getContactNames()
            userGallery.value = repository.getGalleryMediaUrls()
        }
    }

    private val cachedVibrator: Vibrator? = try {
        val context = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    fun vibrate(milliseconds: Long = 50, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (_isSilentMode.value) return
        try {
            val vibrator = cachedVibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            // Safe fallback if vibration permission or hardware device is missing/disabled
        }
    }

    // --- Navigation UI State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Feed)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _lastCommunityViewTime = MutableStateFlow(0L)
    val lastCommunityViewTime: StateFlow<Long> = _lastCommunityViewTime.asStateFlow()
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

    // Unread Community Posts Count
    val unreadCommunityPostsCount = combine(allRawPosts, lastCommunityViewTime) { posts, lastViewTime ->
        // To be accurate with CommunityScreen logic, we just count posts created after the last view time.
        // We could filter strictly for community, but any new post in the simulation counts as network activity.
        // Let's filter for posts not by the current user, created after lastViewTime.
        posts.count { it.authorId != "user" && it.timestamp > lastViewTime }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
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

    // --- Low-end Device Mode (Режим для слабых устройств) ---
    private val _isLowEndDeviceMode = MutableStateFlow<Boolean>(false)
    val isLowEndDeviceMode: StateFlow<Boolean> = _isLowEndDeviceMode.asStateFlow()

    fun toggleLowEndDeviceMode(enabled: Boolean) {
        _isLowEndDeviceMode.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("low_end_device_mode", enabled).apply()
    }

    // --- Markov Chain Mode (Марков Чейн для комментариев) ---
    private val _isMarkovEnabled = MutableStateFlow<Boolean>(true) // Enabled by default
    val isMarkovEnabled: StateFlow<Boolean> = _isMarkovEnabled.asStateFlow()

    fun toggleMarkovEnabled(enabled: Boolean) {
        _isMarkovEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("markov_chain_enabled", enabled).apply()
        // Also sync state down to repository
        repository.setMarkovMarkovEnabled(enabled)
    }

    // --- Persistent Poker Balance ---
    private val _pokerBalance = MutableStateFlow<Int>(1000)
    val pokerBalance: StateFlow<Int> = _pokerBalance.asStateFlow()

    fun updatePokerBalance(newBalance: Int) {
        _pokerBalance.value = newBalance
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("poker_chips_balance", newBalance).apply()
    }

    // --- Persistent User Coins (Coins System) ---
    private val _userCoins = MutableStateFlow<Int>(100)
    val userCoins: StateFlow<Int> = _userCoins.asStateFlow()

    // --- Login Streak State ---
    private val _loginStreak = MutableStateFlow<Int>(1)
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()

    private val _feedViews = MutableStateFlow<Int>(0)
    val feedViews: StateFlow<Int> = _feedViews.asStateFlow()

    // Unique posts viewed (cheat-proof views count)
    private val _uniqueViewsCount = MutableStateFlow(0)
    val uniqueViewsCount: StateFlow<Int> = _uniqueViewsCount.asStateFlow()
    
    private val viewMutex = kotlinx.coroutines.sync.Mutex()

    fun markPostAsViewed(postId: Int) {
        viewModelScope.launch {
            viewMutex.withLock {
                val context = getApplication<Application>()
                val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
                val viewedStr = prefs.getString("viewed_post_ids_set", "") ?: ""
                val viewedSet = if (viewedStr.isEmpty()) mutableSetOf() else viewedStr.split(",").toMutableSet()
                if (!viewedSet.contains(postId.toString())) {
                    viewedSet.add(postId.toString())
                    prefs.edit().putString("viewed_post_ids_set", viewedSet.joinToString(",")).apply()
                    
                    val oldUniqueSize = _uniqueViewsCount.value
                    val newUniqueSize = viewedSet.size
                    _uniqueViewsCount.value = newUniqueSize
                    
                    // Keep feedViews in sync as well for UI representations
                    _feedViews.value = newUniqueSize
                    prefs.edit().putInt("feed_views", newUniqueSize).apply()

                    val oldCoinsFromViews = oldUniqueSize / 10
                    val newCoinsFromViews = newUniqueSize / 10
                    if (newCoinsFromViews > oldCoinsFromViews) {
                        val earned = newCoinsFromViews - oldCoinsFromViews
                        val updatedCoins = _userCoins.value + earned
                        updateCoins(updatedCoins)

                        repository.insertNotification(
                            title = if (_selectedLanguage.value == "RU") "Монета заработана! 🪙" else "Coin Earned! 🪙",
                            message = if (_selectedLanguage.value == "RU") "Вы заработали +$earned монету за просмотр 10 уникальных постов в ленте." else "You earned +$earned coin for viewing 10 unique posts in the feed.",
                            type = "SYSTEM"
                        )
                    }
                }
            }
        }
    }

    // Unique commented posts count
    val uniqueCommentsCount = repository.getUniquePostsCommentedCountFlow("user")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Real-time spent active time tracker
    private val _appTimeSpentToday = MutableStateFlow(0f)
    val appTimeSpentToday: StateFlow<Float> = _appTimeSpentToday.asStateFlow()

    private val _weeklyEngagementHours = MutableStateFlow<List<Float>>(listOf(1.2f, 1.8f, 0.9f, 2.3f, 1.5f, 1.7f, 0.4f))
    val weeklyEngagementHours: StateFlow<List<Float>> = _weeklyEngagementHours.asStateFlow()

    private val _activeDecorationId = MutableStateFlow<Int?>(null)
    val activeDecorationId: StateFlow<Int?> = _activeDecorationId.asStateFlow()

    private val _decorationExpiry = MutableStateFlow<Long>(0L)
    val decorationExpiry: StateFlow<Long> = _decorationExpiry.asStateFlow()

    private val _purchasedDecorationIds = MutableStateFlow<Set<Int>>(emptySet())
    val purchasedDecorationIds: StateFlow<Set<Int>> = _purchasedDecorationIds.asStateFlow()

    private val _isDailyRewardClaimable = MutableStateFlow<Boolean>(false)
    val isDailyRewardClaimable: StateFlow<Boolean> = _isDailyRewardClaimable.asStateFlow()

    fun updateCoins(newAmount: Int) {
        _userCoins.value = newAmount
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("user_coins2", newAmount).apply()
    }

    fun resetLeaderboardCategory(category: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
            when (category) {
                0 -> { // Views
                    prefs.edit().putString("viewed_post_ids_set", "").putInt("feed_views", 0).apply()
                    _uniqueViewsCount.value = 0
                    _feedViews.value = 0
                }
                1 -> { // Likes
                    prefs.edit().putStringSet("liked_posts", emptySet()).apply()
                    _likedPostIds.value = emptySet()
                }
                2 -> { // Comments
                    repository.clearCommentsByAuthor("user")
                }
            }
        }
    }

    fun getDecorationExpiry(id: Int): Long {
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("user_decoration_expires_id_$id", 0L)
    }

    fun isDecorationOwnedValid(id: Int): Boolean {
        return System.currentTimeMillis() < getDecorationExpiry(id)
    }

    fun checkAndRefreshDecorationExpiry() {
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val activeId = prefs.getInt("user_active_decoration", -1)
        val expiry = prefs.getLong("user_decoration_expiry", 0L)
        _decorationExpiry.value = expiry
        
        if (activeId != -1 && System.currentTimeMillis() < expiry) {
            _activeDecorationId.value = activeId
        } else {
            _activeDecorationId.value = null
            if (activeId != -1) {
                prefs.edit().putInt("user_active_decoration", -1).apply()
            }
        }
        
        // Refresh purchased IDs
        val purchasedStr = prefs.getStringSet("purchased_decorations", emptySet()) ?: emptySet()
        _purchasedDecorationIds.value = purchasedStr.mapNotNull { it.toIntOrNull() }.toSet()
        
        // Refresh daily check claimable
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastClaimed = prefs.getString("last_claimed_daily_reward_date", "") ?: ""
        _isDailyRewardClaimable.value = todayStr != lastClaimed
    }

    fun buyDecoration(id: Int, durationDays: Int, price: Int): Boolean {
        if (_userCoins.value < price) return false
        
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        
        // Exclusives check
        if (id in 201..210) {
            val user = currentUser.value
            if (user?.isVerified != true) return false
        }
        
        // Calculate new expiry for this specific decoration
        val currentExpiry = prefs.getLong("user_decoration_expires_id_$id", 0L)
        val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val durationMs = durationDays * 24L * 3600L * 1000L
        val newExpiry = baseTime + durationMs
        
        prefs.edit().putLong("user_decoration_expires_id_$id", newExpiry).apply()
        
        // Deduct money
        val updatedCoins = _userCoins.value - price
        updateCoins(updatedCoins)
        
        // Save to purchased list
        val currentPurchased = prefs.getStringSet("purchased_decorations", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentPurchased.add(id.toString())
        prefs.edit().putStringSet("purchased_decorations", currentPurchased).apply()
        
        // Wear it automatically!
        prefs.edit()
            .putInt("user_active_decoration", id)
            .putLong("user_decoration_expiry", newExpiry)
            .apply()
            
        checkAndRefreshDecorationExpiry()
        
        viewModelScope.launch {
            repository.insertNotification(
                title = if (_selectedLanguage.value == "RU") "Покупка успешно оформлена! ⚡" else "Purchase complete! ⚡",
                message = if (_selectedLanguage.value == "RU") "Вы надели новое украшение на $durationDays дн.!" else "You are now wearing your new decoration for $durationDays days!",
                type = "SYSTEM"
            )
        }
        return true
    }

    fun wearDecoration(id: Int) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val expiry = prefs.getLong("user_decoration_expires_id_$id", 0L)
        
        if (expiry > System.currentTimeMillis()) {
            prefs.edit()
                .putInt("user_active_decoration", id)
                .putLong("user_decoration_expiry", expiry)
                .apply()
            checkAndRefreshDecorationExpiry()
        }
    }

    fun unwearDecoration() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("user_active_decoration", -1)
            .putLong("user_decoration_expiry", 0L)
            .apply()
        checkAndRefreshDecorationExpiry()
    }

    fun claimDailyReward() {
        val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit().putString("last_claimed_daily_reward_date", todayStr).apply()
        
        val current = _userCoins.value
        val earned = kotlin.random.Random.nextInt(100, 1001)
        val updatedCoins = current + earned
        updateCoins(updatedCoins)
        
        _isDailyRewardClaimable.value = false
        
        viewModelScope.launch {
            repository.insertNotification(
                title = if (_selectedLanguage.value == "RU") "Календарь наград! 🪙" else "Daily Reward Claimed! 🪙",
                message = if (_selectedLanguage.value == "RU") "Получено +$earned монет за вход сегодня. Розыгрыш новых подарков в полночь!" else "Claimed +$earned coins today. Next drop available at midnight!",
                type = "SYSTEM"
            )
        }
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

    private val _isFlappyActive = MutableStateFlow(false)
    val isFlappyActive: StateFlow<Boolean> = _isFlappyActive.asStateFlow()

    fun setFlappyActive(active: Boolean) {
        _isFlappyActive.value = active
    }

    init {
        loadDeviceData()
        // Load initial persisted language preference
        val prefs = application.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("selected_lang", "RU") ?: "RU"
        _selectedLanguage.value = savedLang

        // --- WEEKLY TOURNAMENT / MONDAY RESET LOGIC ---
        val calendar = java.util.Calendar.getInstance()
        val currentWeekOfYear = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val lastResetWeek = prefs.getInt("last_monday_reset_week", -1)
        val lastResetYear = prefs.getInt("last_monday_reset_year", -1)
        val isMonday = calendar.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY

        if ((isMonday || currentWeekOfYear != lastResetWeek || currentYear != lastResetYear) && (lastResetWeek != -1)) {
            if (isMonday && (lastResetWeek != currentWeekOfYear || lastResetYear != currentYear)) {
                // Reset Views
                prefs.edit().putString("viewed_post_ids_set", "").putInt("feed_views", 0).apply()
                _uniqueViewsCount.value = 0
                _feedViews.value = 0

                // Reset Likes
                prefs.edit().putStringSet("liked_posts", emptySet()).apply()
                _likedPostIds.value = emptySet()

                // Reset Comments
                viewModelScope.launch {
                    repository.clearCommentsByAuthor("user")
                }

                // Record reset
                prefs.edit()
                    .putInt("last_monday_reset_week", currentWeekOfYear)
                    .putInt("last_monday_reset_year", currentYear)
                    .apply()

                viewModelScope.launch {
                    repository.insertNotification(
                        title = if (savedLang == "RU") "Новый турнир начался! 🏆" else "New Tournament Started! 🏆",
                        message = if (savedLang == "RU") "Понедельник наступил! Все ваши турнирные очки (просмотры, лайки, ответы) сброшены. Время покорять топ заново!" else "Monday is here! All your tournament points (views, likes, comments) have been reset. Time to conquer the top again!",
                        type = "SYSTEM"
                    )
                }
            }
        }
        if (lastResetWeek == -1) {
            prefs.edit()
                .putInt("last_monday_reset_week", currentWeekOfYear)
                .putInt("last_monday_reset_year", currentYear)
                .apply()
        }

        // Calculate consecutive day login streak
        val lastStreakDate = prefs.getString("last_streak_date", "") ?: ""
        val currentStreak = prefs.getInt("login_streak", 0)
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayStr = sdf.format(java.util.Date())
        
        var finalStreak = currentStreak
        if (lastStreakDate.isEmpty()) {
            finalStreak = 1
            prefs.edit().putString("last_streak_date", todayStr).putInt("login_streak", 1).apply()
        } else if (lastStreakDate != todayStr) {
            try {
                val dToday = sdf.parse(todayStr)
                val dLast = sdf.parse(lastStreakDate)
                if (dToday != null && dLast != null) {
                    val diffMs = dToday.time - dLast.time
                    val diffDays = diffMs / (1000 * 60 * 60 * 24)
                    if (diffDays == 1L) {
                        finalStreak = currentStreak + 1
                        val coinsReward = 10 * finalStreak
                        val currentCoins = prefs.getInt("user_coins2", 100)
                        prefs.edit()
                            .putInt("user_coins2", currentCoins + coinsReward)
                            .apply()
                        _userCoins.value = currentCoins + coinsReward
                    } else if (diffDays > 1L) {
                        // Reset streak if missed a day
                        finalStreak = 1
                    }
                }
            } catch (e: Exception) {
                finalStreak = 1
            }
            prefs.edit().putString("last_streak_date", todayStr).putInt("login_streak", finalStreak).apply()
        }
        _loginStreak.value = finalStreak

        // Load last community view time
        _lastCommunityViewTime.value = prefs.getLong("last_community_view", 0L)

        // Load liked posts set
        val savedLiked = prefs.getStringSet("liked_posts", emptySet()) ?: emptySet()
        _likedPostIds.value = savedLiked.mapNotNull { it.toIntOrNull() }.toSet()

        // Load persistent Poker balance
        val savedPokerBalance = prefs.getInt("poker_chips_balance", 1000)
        _pokerBalance.value = savedPokerBalance

        // Load persistent user coins and views
        val savedCoins = prefs.getInt("user_coins2", 100)
        _userCoins.value = savedCoins
        
        val savedViews = prefs.getInt("feed_views", 0)
        _feedViews.value = savedViews
        
        checkAndRefreshDecorationExpiry()

        // Load silent mode
        val savedSilent = prefs.getBoolean("silent_mode", false)
        _isSilentMode.value = savedSilent

        // Load low-end device mode
        val savedLowEnd = prefs.getBoolean("low_end_device_mode", false)
        _isLowEndDeviceMode.value = savedLowEnd

        // Load Markov Chain mode
        val savedMarkov = prefs.getBoolean("markov_chain_enabled", true)
        _isMarkovEnabled.value = savedMarkov
        repository.setMarkovMarkovEnabled(savedMarkov)

        // Load unique viewed posts count
        val viewedStr = prefs.getString("viewed_post_ids_set", "") ?: ""
        _uniqueViewsCount.value = if (viewedStr.isEmpty()) 0 else viewedStr.split(",").size

        // Load time spent today with correct weekday calendar calculation
        val currentCalendar = java.util.Calendar.getInstance()
        val dayOfWeek = currentCalendar.get(java.util.Calendar.DAY_OF_WEEK)
        // Monday (2) is 1, Tuesday (3) is 2, ..., Sunday (1) is 7
        val todayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1

        val currentCalendarDay = currentCalendar.get(java.util.Calendar.DAY_OF_YEAR)
        val lastSavedDay = prefs.getInt("last_saved_calendar_day", -1)
        var todaySecs = prefs.getInt("time_spent_today_secs", 1440)

        if (lastSavedDay != -1 && lastSavedDay != currentCalendarDay) {
            // Day has rolled over! Zero-out today's seconds and reset
            prefs.edit()
                .putInt("time_spent_today_secs", 0)
                .putInt("last_saved_calendar_day", currentCalendarDay)
                .apply()
            todaySecs = 0
        } else if (lastSavedDay == -1) {
            prefs.edit().putInt("last_saved_calendar_day", currentCalendarDay).apply()
        }

        _appTimeSpentToday.value = todaySecs / 3600f

        // Helper to construct dynamic list of hours for Monday to Sunday
        fun getWeeklyList(currentTodaySecs: Int): List<Float> {
            val list = mutableListOf(
                prefs.getFloat("weekly_h_1", 1.2f),
                prefs.getFloat("weekly_h_2", 1.8f),
                prefs.getFloat("weekly_h_3", 0.9f),
                prefs.getFloat("weekly_h_4", 2.3f),
                prefs.getFloat("weekly_h_5", 1.5f),
                prefs.getFloat("weekly_h_6", 1.7f),
                prefs.getFloat("weekly_h_7", 0.8f)
            )
            list[todayIndex - 1] = currentTodaySecs / 3600f
            return list
        }

        _weeklyEngagementHours.value = getWeeklyList(todaySecs)

        // Increment seconds every second the app is open
        viewModelScope.launch {
            var totalSecs = todaySecs
            while (true) {
                kotlinx.coroutines.delay(1000L)
                totalSecs += 1
                _appTimeSpentToday.value = totalSecs / 3600f
                prefs.edit()
                    .putInt("time_spent_today_secs", totalSecs)
                    .putFloat("weekly_h_$todayIndex", totalSecs / 3600f)
                    .apply()
                _weeklyEngagementHours.value = getWeeklyList(totalSecs)
            }
        }

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
            val context = getApplication<Application>()
            var duoTickCount = 0
            while (true) {
                // Speed up bot post generation by ticking much faster
                val tickDelay = if (_isLowEndDeviceMode.value) 4000L else 350L
                delay(tickDelay)
                
                // Continuous check for all active cooldowns
                com.example.workers.CooldownNotifier.checkAndNotifyAllCooldowns(context)

                if (_isSimulating.value && !_isFlappyActive.value) {
                    try {
                        repository.performSimulationTick()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed simulation tick", e)
                    }
                }
                
                // Duolingo push notifications trigger (~ every 30 seconds of active loop)
                duoTickCount++
                if (duoTickCount >= 25) {
                    duoTickCount = 0
                    triggerDuolingoAlert()
                }
            }
        }

        // Periodic Tick for Engagement: Likes/Comments
        viewModelScope.launch {
            while (true) {
                // Speed up bot engagement (likes and comments)
                val tickDelay = if (_isLowEndDeviceMode.value) 6000L else 450L
                delay(tickDelay) 
                if (_isSimulating.value && !_isFlappyActive.value) {
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
        if (screen is Screen.Community) {
            val now = System.currentTimeMillis()
            _lastCommunityViewTime.value = now
            val prefs = getApplication<Application>().getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_community_view", now).apply()
        }
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

    fun createSystemNotification(title: String, message: String) {
        viewModelScope.launch {
            repository.insertNotification(title, message, "SYSTEM")
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

    fun incrementViewsBy(count: Int) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
            val currViews = prefs.getInt("feed_views", 0)
            val updatedViews = currViews + count
            _feedViews.value = updatedViews
            prefs.edit().putInt("feed_views", updatedViews).apply()

            val oldCoinsFromViews = currViews / 10
            val newCoinsFromViews = updatedViews / 10
            if (newCoinsFromViews > oldCoinsFromViews) {
                val earned = newCoinsFromViews - oldCoinsFromViews
                val updatedCoins = _userCoins.value + earned
                updateCoins(updatedCoins)

                repository.insertNotification(
                    title = if (_selectedLanguage.value == "RU") "Монета заработана! 🪙" else "Coin Earned! 🪙",
                    message = if (_selectedLanguage.value == "RU") "Вы заработали +$earned монету за просмотр 10 новостей в ленте." else "You earned +$earned coin for scrolling 10 posts in the feed.",
                    type = "SYSTEM"
                )
            }
        }
    }

    fun recordScrollTelemetry() {
        viewModelScope.launch {
            repository.logMetric("FEED_SCROLL")
            val context = getApplication<Application>()

            val state = com.example.ui.screens.TamagotchiManager.loadState(context)
            if (state.hasPet && !state.isDead) {
                val newPoints = (state.feedScrollPoints + 1f).coerceAtMost(100f)
                com.example.ui.screens.TamagotchiManager.saveState(context, state.copy(feedScrollPoints = newPoints))
            }
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

    // --- CS2 CASE DROPS & DUOLINGO ALERTS INTEGRATION ---
    fun unlockCaseDecoration(id: Int, name: String, rarity: String, styleType: Int, durationHours: Int) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
        
        // Calculate custom temporary expiry
        val durationMs = durationHours * 3600L * 1000L
        val expiry = System.currentTimeMillis() + durationMs
        prefs.edit().putLong("user_decoration_expires_id_$id", expiry).apply()
        
        // Save to SharedPreferences set
        val currentPurchased = prefs.getStringSet("purchased_decorations", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentPurchased.add(id.toString())
        prefs.edit().putStringSet("purchased_decorations", currentPurchased).apply()
        
        // Auto wear
        prefs.edit()
            .putInt("user_active_decoration", id)
            .putLong("user_decoration_expiry", expiry)
            .apply()
            
        checkAndRefreshDecorationExpiry()
        
        viewModelScope.launch {
            repository.insertNotification(
                title = if (_selectedLanguage.value == "RU") "ВЫПАЛ ДРОП ИЗ КЕЙСА! 🎉" else "CASE DROP UNLOCKED! 🎉",
                message = if (_selectedLanguage.value == "RU") "Ого! Временное украшение: $name на $durationHours ч.!" else "Whoa! Got temporary frame: $name for $durationHours hrs!",
                type = "SYSTEM"
            )
        }
    }

    fun triggerDuolingoAlert() {
        val alert = duoAlerts.random()
        val title = if (_selectedLanguage.value == "RU") alert.titleRu else alert.titleEn
        val msg = if (_selectedLanguage.value == "RU") alert.textRu else alert.textEn
        viewModelScope.launch {
            repository.insertNotification(title, msg, "SYSTEM")
        }
    }
}

data class DuoAlert(val titleRu: String, val textRu: String, val titleEn: String, val textEn: String)

val duoAlerts = listOf(
    DuoAlert(
        "🦉 Зеленая сова следит", 
        "Привет, это Дуо! Твои уведомления выключены? Ничего, я уже стою у твоей двери.",
        "🦉 Green Owl is watching", 
        "Hi, it's Duo! Your notifications seem disabled. That's fine, I'm already standing outside your door."
    ),
    DuoAlert(
        "👀 Слишком расслабился?", 
        "Ты не заходил во Флаппи Бот уже целых 5 секунд. Кошка плачет, бот грустит.",
        "👀 Too relaxed?", 
        "You haven't played FlappyBot for a whole 5 seconds. The cat is crying, the bot is sad."
    ),
    DuoAlert(
        "🪵 Твой тамагочи голодает", 
        "Если ты не покормишь его сейчас, я припомню тебе это во сне. Заходи быстро!",
        "🪵 Your Tamagotchi is starving", 
        "If you don't feed it right now, I'll remind you of this in your dreams. Get back in!"
    ),
    DuoAlert(
        "🪙 16,000 монет на кейсы?", 
        "Хватит копить монеты, как цифровой дракон! Открой кейс и выбей что-то ахуенное!",
        "🪙 16,000 coins for Cases?", 
        "Stop hoarding coins like a digital dragon! Spin a case and drop something insane!"
    ),
    DuoAlert(
        "😡 Не игнорируй меня!", 
        "Я знаю, что ты видишь это пуш-уведомление. Твой стрик горит так же ярко, как моя ярость.",
        "😡 Don't ignore me!", 
        "I know you see this push. Your streak is burning as bright as my righteous anger."
    ),
    DuoAlert(
        "🧠 5 секунд кодинга в день...", 
        "...могли бы спасти тебя от пожизненного кринжа. Но ты выбрал листать мемы.",
        "🧠 5 seconds of coding a day...", 
        "...could save you from a lifetime of cringe. But you chose to scroll memes instead."
    ),
    DuoAlert(
        "📍 Координаты получены", 
        "Обнаружен в 12 метрах от дивана. Хватит бегать от тренировок, возвращайся в приложение!",
        "📍 Coordinates GPS-locked", 
        "Detected 12 meters from your couch. Stop running away from lessons, return immediately!"
    ),
    DuoAlert(
        "📉 Стрик потерян? Почти.", 
        "Еще минута — и твоя серия посещений сгорит. И нет, взятка Габену тут не поможет.",
        "📉 Streak lost? Almost.", 
        "One more minute and your daily streak turns to ash. No, bribery of Gabe won't save you."
    ),
    DuoAlert(
        "🎯 Открыть кейсы?", 
        "Дешёвые понты стоят дорого. Давай проверим твою удачу в симуляторе кейсов CS2!",
        "🎯 Open cases?", 
        "Cheap bragging rights are expensive. Let's test your luck in the CS2 case simulator!"
    ),
    DuoAlert(
        "💀 Трупные пятна тамагочи", 
        "Если не откроешь питомца прямо сейчас, он превратится в пиксельный фарш. Бегом сюда!",
        "💀 Tamagotchi rigor mortis", 
        "If you don't check your pet right now, it will turn into pixel mush. Get in here now!"
    )
)
