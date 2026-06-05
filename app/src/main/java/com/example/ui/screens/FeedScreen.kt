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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showFlappyBotGame by remember { mutableStateOf(false) }
    var showTamagotchiDialog by remember { mutableStateOf(false) }
    var showDecorationShopDialog by remember { mutableStateOf(false) }
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    // Log scroll activity for analytics
    val context = LocalContext.current
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            viewModel.recordScrollTelemetry()
            while (true) {
                kotlinx.coroutines.delay(1000)
                val state = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.ui.screens.TamagotchiManager.loadState(context)
                }
                if (state.hasPet && !state.isDead) {
                    val tickNow = System.currentTimeMillis()
                    val deltaMs = tickNow - state.lastTickTime
                    var newState = com.example.ui.screens.updateTamaStats(state, deltaMs, isAppActive = true)
                    // Additional specific scroll boosting
                    newState = newState.copy(
                        mood = (newState.mood + 0.5f).coerceAtMost(100f) // extra joy during scroll
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
        // --- Fullscreen Video / Image Zoom Dialog ---
        if (zoomImageUrl != null) {
            val isVideoInZoom = zoomImageUrl?.endsWith(".mp4", ignoreCase = true) == true || 
                                zoomImageUrl?.contains("video", ignoreCase = true) == true ||
                                zoomImageUrl?.contains("gtv-videos-bucket", ignoreCase = true) == true

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
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    setVideoURI(android.net.Uri.parse(zoomImageUrl))
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
                            if (dragAmountSum > 140f && selectedTab == 1) { // Swipe Right
                                selectedTab = 0
                                viewModel.vibrate(25)
                            } else if (dragAmountSum < -140f && selectedTab == 0) { // Swipe Left
                                selectedTab = 1
                                viewModel.vibrate(25)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount -> dragAmountSum += dragAmount }
                    )
                }
        ) {
            
            // --- Live Activity Stream Ticker ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGray)
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
                listOf("ЭФИР 🌐", "СКАНЕР 🤖")
            } else {
                listOf("FEED 🌐", "SCANNER 🤖")
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .border(1.dp, BorderGray)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .background(if (isSelected) DeepGray else PureBlack)
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
                                        }
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
        } else {
            // --- Bottom Right Action Column (Decorations + Tamagotchi) ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
            val selectedPost = posts.find { it.id == selectedPostForComments }
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
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .background(PureBlack)
            .combinedClickable(
                onClick = onCommentClick,
                onLongClick = onArchiveToggle
            ),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = PureBlack)
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
                    borderWidthDp = 1
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
                            Icons.Filled.FactCheck,
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

            // --- Post Content Text ---
            LinkifyText(
                text = post.content,
                modifier = Modifier.fillMaxWidth()
            )

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
                    if (post.mediaType == "VIDEO" || post.mediaUrl.endsWith(".mp4") || post.mediaUrl.contains("gtv-videos-bucket")) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    setVideoURI(android.net.Uri.parse(post.mediaUrl))
                                    val mc = android.widget.MediaController(ctx)
                                    mc.setAnchorView(this)
                                    setMediaController(mc)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                        start()
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

                // AI Sources Verification Flag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (post.isTrend) Icons.Filled.TrendingUp else Icons.Outlined.Info,
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
            
            Divider(color = BorderGray, thickness = 1.dp)

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

            Divider(color = BorderGray, thickness = 1.dp)

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
                        Icons.Filled.Send,
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
