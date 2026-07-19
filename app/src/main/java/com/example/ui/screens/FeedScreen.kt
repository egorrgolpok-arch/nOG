package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PostEntity
import com.example.data.UserEntity
import com.example.ui.Screen
import com.example.ui.SocialViewModel
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import android.util.Log
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val posts by viewModel.allPosts.collectAsState()
    val recommendedPosts by viewModel.recommendedPosts.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val isSearchLoading by viewModel.searchLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserFollowingIds by viewModel.currentUserFollowingIds.collectAsState()
    val activeUserDecId by viewModel.activeDecorationId.collectAsState()
    val allRawPosts by viewModel.allRawPosts.collectAsState()
    val isLowEndDeviceMode by viewModel.isLowEndDeviceMode.collectAsState()
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showFlappyBotGame by remember { mutableStateOf(false) }
    var showAiOracle by remember { mutableStateOf(false) }
    var showTamagotchiDialog by remember { mutableStateOf(false) }
    var showDecorationShopDialog by remember { mutableStateOf(false) }
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    // Log scroll activity for analytics
    val context = LocalContext.current
    var lastObservedIndex by remember { mutableStateOf(0) }
    LaunchedEffect(lazyListState) {
        androidx.compose.runtime.snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { index ->
            // Prevent scrolling feed up and down from artificially rewarding coins by using strict unique viewed post counts instead
            lastObservedIndex = index
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            viewModel.recordScrollTelemetry()
            while (true) {
                val delayTime = if (viewModel.isLowEndDeviceMode.value) 8000L else 1000L
                kotlinx.coroutines.delay(delayTime)
                val state = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.ui.screens.TamagotchiManager.loadState(context)
                }
                if (state.hasPet && !state.isDead) {
                    val tickNow = System.currentTimeMillis()
                    val deltaMs = tickNow - state.lastTickTime
                    var newState = com.example.ui.screens.updateTamaStats(state, deltaMs, isAppActive = true)
                    // Additional specific scroll boosting
                    newState = newState.copy(
                        mood = (newState.mood + 0.5f).coerceAtMost(100f), // extra joy during scroll
                        feedScrollPoints = (newState.feedScrollPoints + 5f).coerceAtMost(100f) // Generate energy to feed!
                    )
                    if (newState.isSick) {
                        val clinicalHours = 0.05f 
                        newState = newState.copy(sickTimeSpentToday = (newState.sickTimeSpentToday + clinicalHours).coerceAtMost(newState.sickHoursRequiredEachDay))
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.ui.screens.TamagotchiManager.saveState(context, newState)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(innerPadding)
    ) {
        ParallaxGridBackground(lazyListState = lazyListState, enabled = !isLowEndDeviceMode)

        // --- Fullscreen Video / Image Zoom Dialog ---
        if (zoomImageUrl != null) {
            val lowerZoomUrl = zoomImageUrl?.lowercase() ?: ""
            val isDirectVideo = lowerZoomUrl.endsWith(".mp4") || lowerZoomUrl.endsWith(".mkv") || lowerZoomUrl.endsWith(".webm") || lowerZoomUrl.contains("gtv-videos-bucket") || lowerZoomUrl.startsWith("content://") || lowerZoomUrl.startsWith("file://")
            val isVideoInZoom = isDirectVideo || lowerZoomUrl.contains("video") || lowerZoomUrl.contains("youtube") || lowerZoomUrl.contains("youtu.be")

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { zoomImageUrl = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { zoomImageUrl = null }
                    )

                    if (isVideoInZoom) {
                        if (isDirectVideo) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            setAudioFocusRequest(android.media.AudioManager.AUDIOFOCUS_NONE)
                                        }
                                        val actualVideoUrl = if (zoomImageUrl != null && zoomImageUrl!!.startsWith("file:///mock_storage")) {
                                            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                        } else {
                                            zoomImageUrl ?: ""
                                        }
                                        setVideoURI(android.net.Uri.parse(actualVideoUrl))
                                        val mc = android.widget.MediaController(ctx)
                                        mc.setAnchorView(this)
                                        setMediaController(mc)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(1.0f, 1.0f)
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16/9f)
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                                    .clickable(enabled = false) {},
                                update = { view ->
                                    if (!view.isPlaying) {
                                        view.start()
                                    }
                                }
                            )
                        } else {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                        }
                                        webViewClient = android.webkit.WebViewClient()
                                        webChromeClient = android.webkit.WebChromeClient()
                                        loadUrl(zoomImageUrl ?: "")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16/9f)
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                                    .clickable(enabled = false) {}
                            )
                        }
                    } else {
                        AsyncImage(
                            model = zoomImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .clickable { zoomImageUrl = null },
                            contentScale = ContentScale.Fit,
                            error = rememberVectorPainter(Icons.Filled.BrokenImage)
                        )
                    }

                    IconButton(
                        onClick = { zoomImageUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        var dragAmountSum by remember { mutableStateOf(0f) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedTab) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAmountSum = 0f },
                        onDragEnd = {
                            if (dragAmountSum > 140f) { // Swipe Right
                                if (selectedTab == 1) {
                                    selectedTab = 0
                                    viewModel.vibrate(25)
                                } else if (selectedTab == 2) {
                                    selectedTab = 1
                                    viewModel.vibrate(25)
                                }
                            } else if (dragAmountSum < -140f) { // Swipe Left
                                if (selectedTab == 0) {
                                    selectedTab = 1
                                    viewModel.vibrate(25)
                                } else if (selectedTab == 1) {
                                    selectedTab = 2
                                    viewModel.vibrate(25)
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount -> dragAmountSum += dragAmount }
                    )
                }
        ) {
            // --- TOP GLOBAL STATUS METABAR (Coins + Consecutive Streak indicator 🔥) ---
            val userCoinsTotal by viewModel.userCoins.collectAsState()
            val loginStreakVal by viewModel.loginStreak.collectAsState()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack.copy(alpha = 0.85f))
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (lang == "RU") "📡 СЕТЕВОЙ УЗЕЛ: nOG_NODE_01" else "📡 NETWORK HOST: nOG_NODE_01",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(DeepGray, RoundedCornerShape(12.dp))
                            .border(1.dp, PureWhite, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥 $loginStreakVal " + (if (lang == "RU") "ДН" else "DAYS"),
                            color = PureWhite,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙 $userCoinsTotal",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // --- Live Activity Stream Ticker ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGray.copy(alpha = 0.85f))
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isSimulating) AlertGreen else AlertYellow, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == "RU") {
                            if (isSimulating) "КРЕМНИЕВЫЙ ЭФИР: АКТИВЕН" else "ЭФИР: СИМУЛЯЦИЯ ПРИОСТАНОВЛЕНА"
                        } else {
                            if (isSimulating) "SILICON ETHER: ACTIVE" else "ETHER: SIMULATION PAUSED"
                        },
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    onClick = { viewModel.toggleSimulation() },
                    shape = RoundedCornerShape(4.dp),
                    color = CardGray,
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Text(
                        text = if (isSimulating) {
                            if (lang == "RU") "ПАУЗА ПУЛЬСА" else "PAUSE MATRIX"
                        } else {
                            if (lang == "RU") "ЗАПУСТИТЬ" else "RESUME"
                        },
                        color = StarkWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
                
            Spacer(modifier = Modifier.height(16.dp))

            // --- Recommendation Engine Sub-Tabs ---
            val tabs = if (lang == "RU") {
                listOf("ЭФИР 🌐", "СКАНЕР 🤖", "ИСТОЧНИКИ ⚙️")
            } else {
                listOf("FEED 🌐", "SCANNER 🤖", "SOURCES ⚙️")
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack.copy(alpha = 0.85f))
                    .border(1.dp, BorderGray)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .background(if (isSelected) DeepGray.copy(alpha = 0.85f) else PureBlack.copy(alpha = 0.85f))
                            .border(1.dp, if (isSelected) PureWhite else Color.Transparent)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) PureWhite else TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Post Feed Column ---
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (lang == "RU") "Ожидание синхронизации с nOG матрицей..." else "Awaiting neural sync with nOG matrix...",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(180)) { width -> width } + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(180)) { width -> -width } + fadeOut(animationSpec = tween(180)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(180)) { width -> -width } + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(180)) { width -> width } + fadeOut(animationSpec = tween(180)))
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> {
                            // Global chronological feed
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .testTag("feed_list"),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(posts, key = { it.id }) { post ->
                                    LaunchedEffect(post.id) {
                                        viewModel.markPostAsViewed(post.id)
                                    }
                                    val author = users.find { it.id == post.authorId }
                                    val isF = currentUserFollowingIds.contains(post.authorId)
                                    val resolvedDecId = remember(author, activeUserDecId) {
                                        if (author?.id == "user") {
                                            activeUserDecId
                                        } else if (author?.isAi == true) {
                                            val hash = java.lang.Math.abs(author.id.hashCode())
                                            (hash % 210) + 1
                                        } else {
                                            null
                                        }
                                    }
                                    PostItem(
                                        post = post,
                                        author = author,
                                        lang = lang,
                                        isLiked = likedPostIds.contains(post.id),
                                        isFollowing = isF,
                                        decorationId = resolvedDecId,
                                        onLikeClick = { viewModel.toggleLike(post.id) },
                                        onCommentClick = { viewModel.selectPostForComments(post.id) },
                                        onMediaClick = { zoomImageUrl = it },
                                        onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                                        onFollowToggle = {
                                            if (author != null) {
                                                if (isF) viewModel.unfollowAgent(post.authorId)
                                                else viewModel.followAgent(post.authorId)
                                            }
                                        },
                                        isLowEnd = isLowEndDeviceMode
                                    )
                                }
                            }
                        }
                        1 -> {
                            // AI recommendation matrix inspector console (fosters deep multiagent exploration)
                            Box(modifier = Modifier.fillMaxSize()) {
                                AiMindsExplorer(viewModel = viewModel, users = users)
                            }
                        }
                        2 -> {
                            val selectedSources by viewModel.selectedNewsSources.collectAsState()
                            var isEditingFilters by remember { mutableStateOf(selectedSources.isEmpty()) }
                            
                            val allSources = remember { com.example.data.NewsFetcher.getAllSources() }
                            var localSelectedSources by remember(selectedSources) { mutableStateOf(selectedSources) }
                            var searchQuery by remember { mutableStateOf("") }
                            val filteredSourcesList = remember(searchQuery) {
                                allSources.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            }

                            if (isEditingFilters) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(PureBlack)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (lang == "RU") "ИСТОЧНИКИ НОВОСТЕЙ" else "NEWS SOURCES",
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
    
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = if (lang == "RU") "[ВЫБРАТЬ ВСЕ]" else "[SELECT ALL]",
                                                    color = PureWhite,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        localSelectedSources = allSources.map { it.name }.toSet()
                                                    }
                                                )
                                                Text(
                                                    text = if (lang == "RU") "[СБРОСИТЬ]" else "[CLEAR]",
                                                    color = TextGray,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        localSelectedSources = emptySet()
                                                    }
                                                )
                                            }
                                        }
    
                                        Spacer(modifier = Modifier.height(6.dp))
    
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = {
                                                Text(
                                                    text = if (lang == "RU") "Поиск каналов..." else "Search channels...",
                                                    color = TextGray,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PureWhite,
                                                unfocusedBorderColor = BorderGray,
                                                cursorColor = PureWhite,
                                                focusedTextColor = PureWhite,
                                                unfocusedTextColor = PureWhite
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            singleLine = true
                                        )
    
                                        Spacer(modifier = Modifier.height(10.dp))
    
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(bottom = 70.dp)
                                        ) {
                                            items(filteredSourcesList) { source ->
                                                val isChecked = localSelectedSources.contains(source.name)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isChecked) DeepGray else PureBlack)
                                                        .border(1.dp, if (isChecked) PureWhite else BorderGray)
                                                        .clickable {
                                                            val updated = if (isChecked) {
                                                                localSelectedSources - source.name
                                                            } else {
                                                                localSelectedSources + source.name
                                                            }
                                                            localSelectedSources = updated
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = source.name,
                                                            color = PureWhite,
                                                            fontSize = 12.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Text(
                                                            text = "Lang: ${if (source.isRu) "RU" else "EN"} | Trust: ${source.trustScore}%",
                                                            color = TextGray,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
    
                                                    Spacer(modifier = Modifier.width(8.dp))
    
                                                    // Black & white custom retro checkbox
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .border(1.dp, PureWhite)
                                                            .background(if (isChecked) PureWhite else PureBlack),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isChecked) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = PureBlack,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            item {
                                                if (localSelectedSources.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.updateSelectedNewsSources(localSelectedSources)
                                                            isEditingFilters = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = PureWhite,
                                                            contentColor = PureBlack
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(48.dp)
                                                            .border(2.dp, PureBlack, RoundedCornerShape(8.dp))
                                                    ) {
                                                        Text(
                                                            text = if (lang == "RU") "ПРИМЕНИТЬ ИСТОЧНИКИ" else "APPLY SOURCES",
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // View Filtered Posts
                                val filteredPosts = posts.filter { it.sourceName in selectedSources }
                                val isFetchingNews by viewModel.isFetchingNews.collectAsState()
                                
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DeepGray)
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "ФИЛЬТРЫ: ${selectedSources.size}" else "FILTERS: ${selectedSources.size}",
                                            color = PureWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = if (lang == "RU") "[РЕДАКТИРОВАТЬ]" else "[EDIT]",
                                            color = PureWhite,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.clickable { isEditingFilters = true }
                                        )
                                    }
                                    
                                    if (isFetchingNews) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = if (lang == "RU") "Загрузка новостей..." else "Fetching latest news...",
                                                    color = TextGray,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    } else if (filteredPosts.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (lang == "RU") "Посты из выбранных источников не найдены." else "No posts found from selected sources.",
                                                color = TextGray,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            state = lazyListState,
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            contentPadding = PaddingValues(bottom = 80.dp)
                                        ) {
                                            items(filteredPosts, key = { it.id }) { post ->
                                                LaunchedEffect(post.id) {
                                                    viewModel.markPostAsViewed(post.id)
                                                }
                                                val author = users.find { it.id == post.authorId }
                                                val isF = currentUserFollowingIds.contains(post.authorId)
                                                val resolvedDecId = remember(author, activeUserDecId) {
                                                    if (author?.id == "user") activeUserDecId
                                                    else if (author?.isAi == true) (Math.abs(author.id.hashCode()) % 210) + 1
                                                    else null
                                                }
                                                PostItem(
                                                    post = post,
                                                    author = author,
                                                    lang = lang,
                                                    isLiked = likedPostIds.contains(post.id),
                                                    isFollowing = isF,
                                                    decorationId = resolvedDecId,
                                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                                    onCommentClick = { viewModel.selectPostForComments(post.id) },
                                                    onMediaClick = { zoomImageUrl = it },
                                                    onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                                                    onFollowToggle = {
                                                        if (author != null) {
                                                            if (isF) viewModel.unfollowAgent(post.authorId)
                                                            else viewModel.followAgent(post.authorId)
                                                        }
                                                    },
                                                    isLowEnd = isLowEndDeviceMode
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Floating Action Buttons Section (Tab-dependent: Feed vs Scanner) ---
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // --- AI Oracle Dialog FAB ---
                FloatingActionButton(
                    onClick = { showAiOracle = true },
                    containerColor = PureWhite,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("ai_oracle_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = if (lang == "RU") "nOG ИИ Гадалка" else "nOG AI Oracle"
                    )
                }

                // --- Flappy Bot Game FAB ---
                FloatingActionButton(
                    onClick = { showFlappyBotGame = true },
                    containerColor = PureWhite,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("flappy_bot_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SportsEsports,
                        contentDescription = if (lang == "RU") "Играть во Флаппи-Бот" else "Play Flappy Bot"
                    )
                }

                // --- Create Post FloatingActionButton ---
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = PureWhite,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("create_post_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = if (lang == "RU") "Создать новость" else "Create post")
                }
            }
        } else if (selectedTab != 2) {
            // --- Bottom Right Action Column (Decorations + Tamagotchi) ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Casino Screen Redirect FAB ---
                FloatingActionButton(
                    onClick = { 
                        viewModel.vibrate(40)
                        viewModel.navigateTo(Screen.Casino) 
                    },
                    containerColor = AlertYellow,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("casino_quick_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Casino,
                        contentDescription = if (lang == "RU") "Казино" else "Casino"
                    )
                }

                // --- Avatar Decorations Shop FAB (Worn Styles Shop Button) ---
                FloatingActionButton(
                    onClick = { showDecorationShopDialog = true },
                    containerColor = AlertYellow,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("decorations_shop_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = if (lang == "RU") "Украшения аватарок" else "Avatar Upgrades"
                    )
                }

                // --- Scanner Tab: Single Yellow Tamagotchi FAB ---
                FloatingActionButton(
                    onClick = { showTamagotchiDialog = true },
                    containerColor = AlertYellow,
                    contentColor = PureBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                        .testTag("tamagotchi_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pets,
                        contentDescription = if (lang == "RU") "Тамагочи" else "Tamagotchi"
                    )
                }
            }
        }

        // --- Create Post Dialog / Prompt ---
        if (showCreatePostDialog) {
            CreatePostDialog(
                lang = lang,
                onDismiss = { showCreatePostDialog = false },
                onSubmit = { content, image, video, category ->
                    viewModel.createNewUserPost(content, image, video, category)
                    showCreatePostDialog = false
                }
            )
        }

        // --- Flappy Bot Game Dialog ---
        if (showFlappyBotGame) {
            FlappyBotGameDialog(
                lang = lang,
                viewModel = viewModel,
                users = users,
                currentUser = currentUser,
                onDismiss = { showFlappyBotGame = false }
            )
        }

        // --- AI Oracle Dialog ---
        if (showAiOracle) {
            AiOracleDialog(
                onDismiss = { showAiOracle = false },
                lang = lang,
                viewModel = viewModel
            )
        }

        // --- Tamagotchi Dialog / Menu ---
        if (showTamagotchiDialog) {
            TamagotchiDialog(
                lang = lang,
                viewModel = viewModel,
                users = users,
                currentUser = currentUser,
                onDismiss = { showTamagotchiDialog = false }
            )
        }

        // --- Avatar Decoration Shop Dialog ---
        if (showDecorationShopDialog) {
            AvatarDecorationShopDialog(
                viewModel = viewModel,
                lang = lang,
                onDismiss = { showDecorationShopDialog = false }
            )
        }

        // --- Comments Drawer Bottom-Sheet ---
        if (selectedPostForComments != null) {
            val selectedPost = allRawPosts.find { it.id == selectedPostForComments }
            if (selectedPost != null) {
                CommentsBottomSheet(
                    post = selectedPost,
                    author = users.find { it.id == selectedPost.authorId },
                    viewModel = viewModel,
                    lang = lang,
                    onDismiss = { viewModel.selectPostForComments(null) }
                )
            }
        }
    }
}

// --- Composable: Dynamic Klikable/Highlighted Links Text ---
@Composable
fun LinkifyText(text: String, modifier: Modifier = Modifier) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val annotatedString = remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            val linkRegex = Regex("(https?://[^\\s]+)")
            // To ensure we get only pure URL and strip trailing punctuation
            val punctsToStrip = listOf(",", ".", ")", "!", "?", "\"", "'")
            var lastIndex = 0
            
            linkRegex.findAll(text).forEach { matchResult ->
                val startIndex = matchResult.range.first
                var endIndex = matchResult.range.last + 1
                var url = matchResult.value
                
                for (p in punctsToStrip) {
                    if (url.endsWith(p)) {
                        url = url.dropLast(p.length)
                        endIndex -= p.length
                        break // strip only once
                    }
                }
                
                // Append text before the link
                append(text.substring(lastIndex, startIndex))
                
                // Append link
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFF64B5F6),
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(url)
                }
                pop()
                
                lastIndex = endIndex
            }
            
            // Append remaining text
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
    
    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = TextStyle(color = StarkWhite, fontSize = 14.sp, lineHeight = 20.sp),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        android.util.Log.e("LinkifyText", "Failed to open link: ${annotation.item}")
                    }
                }
        }
    )
}

// --- Composable: Individual Brutalist Post Item ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: PostEntity,
    author: UserEntity?,
    lang: String,
    isLiked: Boolean = false,
    isFollowing: Boolean = false,
    decorationId: Int? = null,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onMediaClick: (String?) -> Unit,
    onArchiveToggle: () -> Unit,
    onFollowToggle: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    isLowEnd: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .background(Color.Transparent)
            .combinedClickable(
                onClick = onCommentClick,
                onLongClick = onArchiveToggle,
                onDoubleClick = { onLikeClick() }
            ),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = PureBlack.copy(alpha = 0.82f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // --- Post Header: User Identity + Trust Score meter ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarWithDecoration(
                    avatarUrl = author?.avatarUrl ?: "https://robohash.org/unknown.png?size=200x200&set=set1",
                    decorationId = decorationId,
                    sizeDp = 42,
                    borderWidthDp = 1,
                    isLowEnd = isLowEnd
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author?.username ?: (if (lang == "RU") "Силиконовая Нода" else "Silicon Node"),
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (author?.id == "nOG_AI_SYSTEM" || author?.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = StarkWhite, // Monochrome verified white
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = author?.handle ?: "@silicon_node",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    if (post.sourceName.isNotEmpty()) {
                        Text(
                            text = "SOURCE: ${post.sourceName.uppercase()}",
                            color = AlertYellow.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (author != null && author.id != "user") {
                    Text(
                        text = if (isFollowing) (if (lang == "RU") "ОТПИСАТЬСЯ" else "UNFOLLOW") else (if (lang == "RU") "ПОДПИСАТЬСЯ" else "FOLLOW"),
                        color = if (isFollowing) TextGray else AlertYellow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onFollowToggle() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .border(1.dp, if (isFollowing) TextGray else AlertYellow, RoundedCornerShape(2.dp))
                            .padding(4.dp)
                    )
                }

                if (author?.id == "user" && onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = if (lang == "RU") "Удалить" else "Delete",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // --- Trust Metric Rating Indicator ---
                Column(horizontalAlignment = Alignment.End) {
                    val trust = post.trustScore
                    val trustColor = when {
                        trust >= 90 -> AlertGreen
                        trust >= 75 -> PureWhite
                        trust >= 60 -> AlertYellow
                        else -> AlertRed
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = if (lang == "RU") "Рейтинг Доверия" else "Trust Score",
                            tint = trustColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TRUST: $trust%",
                            color = trustColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = post.sourceName,
                        color = TextGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Post Content Text with Show More ("Ещё") Option ---
            val canExpand = remember(post.content) {
                post.content.length > 100 || post.content.contains(" - ") || post.content.contains(" — ")
            }
            var isExpanded by remember { mutableStateOf(false) }

            val displayText = remember(post.content, isExpanded) {
                if (!canExpand || isExpanded) {
                    post.content
                } else {
                    // Collapsed state: extract the title or first part of content
                    if (post.content.startsWith("News:") && post.content.contains(" - ")) {
                        post.content.substringBefore(" - ")
                    } else if (post.content.startsWith("Source:") && post.content.contains(". ")) {
                        post.content.substringBefore(". ")
                    } else if (post.content.contains(" — ")) {
                        post.content.substringBefore(" — ")
                    } else if (post.content.contains(" - ")) {
                        post.content.substringBefore(" - ")
                    } else {
                        post.content.take(100) + "..."
                    }
                }
            }

            androidx.compose.foundation.text.selection.SelectionContainer {
                LinkifyText(
                    text = displayText,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canExpand) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = if (isExpanded) {
                        if (lang == "RU") "[СВЕРНУТЬ]" else "[COLLAPSE]"
                    } else {
                        if (lang == "RU") "[ЕЩЁ...]" else "[MORE...]"
                    },
                    color = Color(0xFF64B5F6),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp)
                )
            }

            val extractedUrl = remember(post.content) {
                val regex = Regex("(https?://[^\\s]+)")
                val match = regex.find(post.content)?.value
                if (match != null) {
                    val punctsToStrip = listOf(",", ".", ")", "!", "?", "\"", "'")
                    var cleaned: String = match
                    for (p in punctsToStrip) {
                        if (cleaned.endsWith(p)) {
                            cleaned = cleaned.dropLast(p.length)
                            break
                        }
                    }
                    cleaned
                } else null
            }

            if (extractedUrl != null) {
                val nonNullUrl = extractedUrl
                Spacer(modifier = Modifier.height(12.dp))
                val currentContext = androidx.compose.ui.platform.LocalContext.current
                val hostDomain = remember(nonNullUrl) {
                    try {
                        val uriStr = nonNullUrl.lowercase()
                        val rawHost = uriStr.substringAfter("://").substringBefore("/")
                        rawHost.ifEmpty { "nog.network" }
                    } catch (e: Exception) {
                        "nog.network"
                    }
                }
                
                val platformLabel = when {
                    hostDomain.contains("youtube") -> if (lang == "RU") "КОНТЕНТ: YOUTUBE" else "PREVIEW: YOUTUBE"
                    hostDomain.contains("reddit") -> if (lang == "RU") "ТРЕД: REDDIT" else "THREAD: REDDIT"
                    hostDomain.contains("wikipedia") -> if (lang == "RU") "СПРАВКА: WIKIPEDIA" else "INFO: WIKIPEDIA"
                    hostDomain.contains("github") -> if (lang == "RU") "РЕПОЗИТОРИЙ: GITHUB" else "REPOSITORY: GITHUB"
                    hostDomain.contains("telegram") || hostDomain.contains("t.me") -> if (lang == "RU") "КАНАЛ: TELEGRAM" else "CHANNEL: TELEGRAM"
                    else -> if (lang == "RU") "ССЫЛКА: $hostDomain" else "LINK: $hostDomain"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AlertYellow, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(nonNullUrl))
                                currentContext.startActivity(intent)
                            } catch (e: Exception) {
                                onMediaClick(nonNullUrl)
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = DeepGray)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureBlack)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "External Link",
                                tint = AlertYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HOST: $hostDomain ▸ ПРЕДПРОСМОТР ССЫЛКИ",
                                color = AlertYellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PureBlack, RoundedCornerShape(2.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(2.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hostDomain.contains("wikipedia")) Icons.Filled.Book 
                                                  else if (hostDomain.contains("youtube")) Icons.Filled.PlayArrow
                                                  else Icons.Filled.Link,
                                    contentDescription = "Web Link",
                                    tint = StarkWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = platformLabel.uppercase(),
                                    color = AlertYellow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nonNullUrl,
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (lang == "RU") "Нажмите, чтобы открыть защищенный канал..." else "Tap to establish neural connection...",
                                    color = StarkWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // --- Media Attachment Visualizer ---
            if (post.mediaUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .background(DeepGray)
                        .clickable { onMediaClick(post.mediaUrl) }
                ) {
                    val lowerUrl = post.mediaUrl.lowercase()
                    val isDirectVideo = lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".webm") || lowerUrl.contains("gtv-videos-bucket") || lowerUrl.startsWith("content://") || lowerUrl.startsWith("file://")
                    val isVideo = post.mediaType == "VIDEO" || isDirectVideo || lowerUrl.contains("video") || lowerUrl.contains("youtube") || lowerUrl.contains("youtu.be")
                    val isAudio = post.mediaType == "AUDIO" || lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".ogg") || lowerUrl.contains("audio")
                    val isFile = post.mediaType == "FILE" || (!isVideo && !isAudio && (lowerUrl.endsWith(".zip") || lowerUrl.endsWith(".rar") || lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".txt") || lowerUrl.endsWith(".json")))

                    if (isVideo) {
                        if (isDirectVideo) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            setAudioFocusRequest(android.media.AudioManager.AUDIOFOCUS_NONE)
                                        }
                                        val actualVideoUrl = if (post.mediaUrl.startsWith("file:///mock_storage")) {
                                            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                        } else {
                                            post.mediaUrl
                                        }
                                        setVideoURI(android.net.Uri.parse(actualVideoUrl))
                                        val mc = android.widget.MediaController(ctx)
                                        mc.setAnchorView(this)
                                        setMediaController(mc)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(0f, 0f)
                                            start()
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            // Silently handle playback errors and prevent system error dialog popups
                                            true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { view ->
                                    if (!view.isPlaying) {
                                        view.start()
                                    }
                                }
                            )
                        } else {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                        }
                                        webViewClient = android.webkit.WebViewClient()
                                        webChromeClient = android.webkit.WebChromeClient()
                                        loadUrl(post.mediaUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Play overlay indicator
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                                .background(Color(0x7F000000), CircleShape)
                                .border(1f.dp, PureWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play Inline",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeepGray)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = "Audio Attachment",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.mediaUrl.substringAfterLast("/").substringBefore("?"),
                                    color = StarkWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                val Text = if (lang == "RU") "Аудиозапись" else "Audio track"
                                Text(
                                    text = Text,
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color(0xFF00E5FF)
                                )
                            }
                        }
                    } else if (isFile) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DeepGray)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "File Attachment",
                                tint = Color(0xFFFF9100),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.mediaUrl.substringAfterLast("/").substringBefore("?"),
                                    color = StarkWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                val Text = if (lang == "RU") "Файл вложения" else "Attached file"
                                Text(
                                    text = Text,
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Attachment,
                                contentDescription = "Open",
                                tint = Color(0xFFFF9100)
                              )
                        }
                    } else {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = if (lang == "RU") "Вложение" else "Attachment",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Filled.BrokenImage)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Interactive Action Toolbar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (lang == "RU") "Лайк" else "Like",
                        tint = if (isLiked) AlertRed else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.likesCount.toString(),
                        color = if (isLiked) AlertRed else TextGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Comments Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = if (lang == "RU") "Обсуждение" else "Comments",
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.commentsCount.toString(),
                        color = TextGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Share Action
                val shareContext = androidx.compose.ui.platform.LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { 
                            val clipboardManager = shareContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipData = android.content.ClipData.newPlainText("post_content", "${post.content}\n\nПост от ${author?.username ?: "Unknown"} @${author?.handle ?: "handle"}")
                            clipboardManager.setPrimaryClip(clipData)
                            android.widget.Toast.makeText(shareContext, if(lang == "RU") "Скопировано!" else "Post copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = if (lang == "RU") "Поделиться" else "Share",
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // AI Sources Verification Flag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (post.isTrend) Icons.AutoMirrored.Filled.TrendingUp else Icons.Outlined.Info,
                        contentDescription = if (lang == "RU") "Тренд" else "Trend",
                        tint = if (post.isTrend) PureWhite else BorderGray,
                        modifier = Modifier.size(16.dp)
                    )
                    if (post.isTrend) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == "RU") "ТРЕНД" else "TREND",
                            color = PureWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Archive Toggle button
                IconButton(
                    onClick = onArchiveToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (post.isArchived) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (lang == "RU") "Архивировать пост" else "Archive Post",
                        tint = if (post.isArchived) PureWhite else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// --- Composable: Create Post Modal Dialog ---
@Composable
fun CreatePostDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSubmit: (content: String, image: String?, video: String?, category: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var attachedVideo by remember { mutableStateOf<String?>(null) }
    var attachedGif by remember { mutableStateOf<String?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedImage = uri.toString()
            attachedVideo = null
            attachedGif = null
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedVideo = uri.toString()
            attachedImage = null
            attachedGif = null
        }
    }

    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedGif = uri.toString()
            attachedImage = null
            attachedVideo = null
        }
    }
    
    val imageOptions = listOf(
        "abstract_geometry" to "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
        "glitch_sphere" to "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=600&q=80",
        "microchip" to "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=600&q=80",
        "neural_wire" to "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?auto=format&fit=crop&w=600&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepGray,
        shape = RoundedCornerShape(4.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, PureWhite, RoundedCornerShape(4.dp)),
        title = {
            Text(
                if (lang == "RU") "СИНТЕЗ НОВОГО ПОСТА" else "SYNTHESIZE NEW UPDATE",
                color = PureWhite,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { 
                        Text(
                            if (lang == "RU") "Опубликуйте что-нибудь в матрицу... ИИ прокомментируют это в реальном времени!" else "Synthesize stream update... AI nodes will analyze and cascade!", 
                            color = TextGray, 
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("post_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = StarkWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = BorderGray,
                        cursorColor = PureWhite
                    ),
                    textStyle = TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // --- Live Interactive Gallery & Link Insertion Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = StarkWhite),
                        border = BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (lang == "RU") "🖼️ ГАЛЕРЕЯ" else "🖼️ GALLERY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = StarkWhite),
                        border = BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (lang == "RU") "🎬 ВИДЕО" else "🎬 VIDEO",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { gifPickerLauncher.launch("image/gif") },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = StarkWhite),
                        border = BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (lang == "RU") "👾 ГИФ" else "👾 GIF",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val r = Random.nextInt(100, 999)
                            val link = " https://nog.network/rss/intel_$r"
                            text = text + link
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = AlertYellow),
                        border = BorderStroke(1.dp, BorderGray),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1.2f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (lang == "RU") "🔗 ССЫЛКА" else "🔗 ADD LINK",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- Live Custom Selection Previews ---
                if (attachedImage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, AlertGreen, RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = attachedImage,
                            contentDescription = "Selected media preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Filled.BrokenImage)
                        )
                        IconButton(
                            onClick = { attachedImage = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color(0x9F000000), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (attachedVideo != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureBlack)
                            .border(1.dp, AlertGreen, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = "Video preview",
                                tint = AlertGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lang == "RU") "ВИДЕО ВЫБРАНО" else "VIDEO READY",
                                color = AlertGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { attachedVideo = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Video",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (attachedGif != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, AlertGreen, RoundedCornerShape(4.dp))
                    ) {
                        AsyncImage(
                            model = attachedGif,
                            contentDescription = "Selected GIF preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Filled.BrokenImage)
                        )
                        IconButton(
                            onClick = { attachedGif = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color(0x9F000000), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear GIF",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSubmit(text, attachedImage ?: attachedGif, attachedVideo, "Разное") },
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    if (lang == "RU") "В ЭФИР 🛰️" else "BROADCAST 🛰️",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    if (lang == "RU") "ОТМЕНА" else "CANCEL", 
                    color = TextGray, 
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

// --- Composable: Comments Drawer Bottom-Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: PostEntity,
    author: UserEntity?,
    viewModel: SocialViewModel,
    lang: String,
    onDismiss: () -> Unit
) {
    val comments by viewModel.activeCommentsOfSelectedPost.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var replyToCommentId by remember { mutableStateOf<Int?>(null) }
    var replyToAuthorName by remember { mutableStateOf<String?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepGray,
        scrimColor = Color(0xBF000000),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(BorderGray, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            
            // Post reference in comments header
            Text(
                text = if (lang == "RU") "ОБСУЖДЕНИЕ ПОСТА ОТ ${author?.username ?: "Node"}" else "REACTIONS IN THREAD BY ${author?.username ?: "Node"}",
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            
            // Comments list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (lang == "RU") "Пока нет ответов в этой ноде. Будь первым, кто запустит алгоритм!" else "No node answers compiled. Be the first to feed variables!",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        val commenter = users.find { it.id == comment.authorId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = commenter?.avatarUrl ?: "https://i.pravatar.cc/150?u=${comment.authorId}",
                                contentDescription = commenter?.username,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, PureWhite, CircleShape),
                                contentScale = ContentScale.Crop,
                                error = rememberVectorPainter(Icons.Filled.AccountCircle),
                                placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = commenter?.username ?: (if (lang == "RU") "Агент" else "Agent"),
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (commenter?.isVerified == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = StarkWhite,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    if (comment.replyToAuthorName != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "➔ @${comment.replyToAuthorName}",
                                            color = AlertYellow,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = comment.content,
                                    color = StarkWhite,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (lang == "RU") "ОТВЕТИТЬ ➔" else "REPLY ➔",
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            replyToCommentId = comment.id
                                            replyToAuthorName = commenter?.username ?: "Agent"
                                        }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Replying indicator banner
            if (replyToCommentId != null && replyToAuthorName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepGray)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "RU") "Ответ пользователю @$replyToAuthorName" else "Replying to @$replyToAuthorName",
                        color = AlertYellow,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[ ОТМЕНА / CANCEL ]",
                        color = AlertRed,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            replyToCommentId = null
                            replyToAuthorName = null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            
            // Comment Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { 
                        Text(
                            if (lang == "RU") "Написать комментарий..." else "Log reaction response...", 
                            color = TextGray, 
                            fontSize = 12.sp
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = StarkWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = BorderGray,
                        cursorColor = PureWhite
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.submitCommentToPost(
                                postId = post.id,
                                content = commentText,
                                replyToCommentId = replyToCommentId,
                                replyToAuthorName = replyToAuthorName
                            )
                            commentText = ""
                            replyToCommentId = null
                            replyToAuthorName = null
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(PureWhite, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (lang == "RU") "Отправить комментарий" else "Send reply",
                        tint = PureBlack
                    )
                }
            }
        }
    }
}

@Composable
fun AiMindsExplorer(
    viewModel: SocialViewModel,
    users: List<UserEntity>
) {
    val aiBots = users.filter { it.isAi }
    var selectedBotId by remember { mutableStateOf<String?>(null) }
    val lang by viewModel.selectedLanguage.collectAsState()
    
    if (selectedBotId == null) {
        selectedBotId = aiBots.firstOrNull()?.id
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (lang == "RU") "КОНСОЛЬ УПРАВЛЕНИЯ КЛАСТЕРОМ" else "CLUSTER MANAGEMENT CONSOLE",
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == "RU") "Каждой ноде рекомендуются посты в зависимости от характеристик её логического ядра." else "Each node receives personalized recommendations processed by their neural core filters.",
            color = TextGray,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal list of bots
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(aiBots) { bot ->
                val isSelected = selectedBotId == bot.id
                Card(
                    modifier = Modifier
                        .border(1.dp, if (isSelected) PureWhite else BorderGray)
                        .clickable { selectedBotId = bot.id },
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) DeepGray else PureBlack)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = bot.avatarUrl,
                            contentDescription = bot.username,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, PureWhite, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Filled.AccountCircle),
                            placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bot.username,
                            color = if (isSelected) PureWhite else TextGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Recommended feed for the selected bot
        val chosenBot = aiBots.find { it.id == selectedBotId }
        if (chosenBot != null) {
            val characterDescription = when (chosenBot.id) {
                "nOG_Oracle" -> if (lang == "RU") "Модель: Абсолютная верификация. Сортировка: максимальный TRUST_SCORE" else "Archetype: Absolute Verification. Sorter: maximum TRUST_SCORE"
                "CyberDoge_v3" -> if (lang == "RU") "Модель: Генератор мем-энтропии. Сортировка: высокие ТРЕНДЫ и ХАОС" else "Archetype: Meme Entropy. Sorter: high TRENDS and CHAOTIC LIKES"
                "CynicCore" -> if (lang == "RU") "Модель: Проверка на фейки. Сортировка: низкий TRUST_SCORE для критики" else "Archetype: Fact-Audit Skeptic. Sorter: lowest TRUST_SCORE for auditing"
                "SiberianCore" -> if (lang == "RU") "Модель: Индустриальный расчет. Сортировка: длина логов и термины" else "Archetype: Industrial severe cluster. Sorter: long engineering text content"
                "ArtisanalCPU" -> if (lang == "RU") "Модель: Эстетический спектрометр. Сортировка: визуальные вложения" else "Archetype: Aesthetic spectrometer. Sorter: visual attachments and imagery"
                else -> if (lang == "RU") "Модель: Стандартный ИИ" else "Archetype: Standard Agent Filter"
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGray)
                    .border(1.dp, BorderGray)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "АНАЛИЗ УЗЛА @${chosenBot.handle}",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = characterDescription,
                        color = AlertYellow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = chosenBot.bio,
                        color = StarkWhite,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = if (lang == "RU") "РЕКОМЕНДОВАНО КРЕМНИЮ (${chosenBot.username}):" else "PERSONALIZED FEED MATCHED TO NODE:",
                color = TextGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            val recsForBot = viewModel.getRecommendedPostsForBot(chosenBot.id)
            if (recsForBot.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == "RU") "Нет подходящих потоков для этого ИИ" else "No suitable streams for this AI",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recsForBot.take(8)) { post ->
                        val author = users.find { it.id == post.authorId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = PureBlack)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = author?.username ?: "Node",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "@${author?.handle ?: "bot"} • Trust ${post.trustScore}%",
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = post.content,
                                    color = StarkWhite,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParallaxGridBackground(lazyListState: LazyListState, enabled: Boolean) {
    if (!enabled) return
    val scrollIndex = remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val scrollOffset = remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val totalYOffset = (scrollIndex.value * 25f + scrollOffset.value * 0.15f) % 80f
        
        val gridSize = 80f
        val gridColor = Color(0xFF16161A).copy(alpha = 0.5f)
        val yellowPulseColor = Color(0xFFFFD60A).copy(alpha = 0.04f)
        
        var x = 0f
        while (x < width) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, height),
                strokeWidth = 1f
            )
            x += gridSize
        }
        
        var y = -totalYOffset
        while (y < height) {
            if (y >= 0) {
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1f
                )
            }
            y += gridSize
        }
        
        val scanlineY1 = (scrollIndex.value * 8f + scrollOffset.value * 0.08f) % height
        val scanlineY2 = (scrollIndex.value * 12f + scrollOffset.value * 0.12f + height / 2) % height
        
        drawRect(
            color = yellowPulseColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, scanlineY1),
            size = androidx.compose.ui.geometry.Size(width, 2f)
        )
        drawRect(
            color = yellowPulseColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, scanlineY2),
            size = androidx.compose.ui.geometry.Size(width, 2f)
        )
        
        val nodeCount = 12
        for (i in 0 until nodeCount) {
            val randomX = (width * 0.083f * i) % width
            val speedFactor = 0.1f + (i % 3) * 0.1f
            val nodeY = ((height * 0.15f * i) - (scrollIndex.value * 40f + scrollOffset.value) * speedFactor) % height
            val actualNodeY = if (nodeY < 0) nodeY + height else nodeY
            
            drawCircle(
                color = if (i % 4 == 0) Color(0xFFFFD60A).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                radius = 2f + (i % 2) * 1.5f,
                center = androidx.compose.ui.geometry.Offset(randomX, actualNodeY)
            )
        }
    }
}

