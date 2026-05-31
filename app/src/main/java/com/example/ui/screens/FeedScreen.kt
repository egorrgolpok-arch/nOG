package com.example.ui.screens

import androidx.compose.animation.*
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
    
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    // Log scroll activity for analytics
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            viewModel.recordScrollTelemetry()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(innerPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
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

            // --- Recommendation Engine Sub-Tabs ---
            val tabs = if (lang == "RU") {
                listOf("ЭФИР 🌐", "ДЛЯ ВАС ⭐", "КОНСОЛЬ ИИ 🤖")
            } else {
                listOf("FEED 🌐", "FOR YOU ⭐", "AI SCANNER 🤖")
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
                when (selectedTab) {
                    0 -> {
                        // Global chronological feed
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("feed_list"),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(posts, key = { it.id }) { post ->
                                val author = users.find { it.id == post.authorId }
                                PostItem(
                                    post = post,
                                    author = author,
                                    lang = lang,
                                    isLiked = likedPostIds.contains(post.id),
                                    onLikeClick = { viewModel.toggleLike(post.id) },
                                    onCommentClick = { viewModel.selectPostForComments(post.id) },
                                    onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                                    onFollowToggle = {
                                        if (author != null) {
                                            viewModel.followAgent(post.authorId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        // Personal content recommendation feed
                        val recList = recommendedPosts
                        if (recList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang == "RU") "Нет рекомендованных постов. Проявляйте активность!" else "No recommended streams. React on nodes to train algorithms!",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DeepGray)
                                            .border(1.dp, BorderGray)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "КАТАЛОГ СОРТИРОВКИ: Оценка поведенческих паттернов, лайков, веры и истории подписок." else "METRIC MODE: Auditing subscriber history, liked threads, and node activity logs.",
                                            color = AlertYellow,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                items(recList, key = { "rec-${it.id}" }) { post ->
                                    val author = users.find { it.id == post.authorId }
                                    PostItem(
                                        post = post,
                                        author = author,
                                        lang = lang,
                                        isLiked = likedPostIds.contains(post.id),
                                        onLikeClick = { viewModel.toggleLike(post.id) },
                                        onCommentClick = { viewModel.selectPostForComments(post.id) },
                                        onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                                        onFollowToggle = {
                                            if (author != null) {
                                                viewModel.followAgent(post.authorId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // AI recommendation matrix inspector console (fosters deep multiagent exploration)
                        Box(modifier = Modifier.weight(1f)) {
                            AiMindsExplorer(viewModel = viewModel, users = users)
                        }
                    }
                }
            }
        }

        // --- Create Post FloatingActionButton ---
        FloatingActionButton(
            onClick = { showCreatePostDialog = true },
            containerColor = PureWhite,
            contentColor = PureBlack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
                .testTag("create_post_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = if (lang == "RU") "Создать новость" else "Create post")
        }

        // --- Create Post Dialog / Prompt ---
        if (showCreatePostDialog) {
            CreatePostDialog(
                lang = lang,
                onDismiss = { showCreatePostDialog = false },
                onSubmit = { content, image, video ->
                    viewModel.createNewUserPost(content, image, video)
                    showCreatePostDialog = false
                }
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

// --- Composable: Individual Brutalist Post Item ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: PostEntity,
    author: UserEntity?,
    lang: String,
    isLiked: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onArchiveToggle: () -> Unit,
    onFollowToggle: () -> Unit
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
                AsyncImage(
                    model = author?.avatarUrl ?: "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=200&q=80",
                    contentDescription = author?.username,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, PureWhite, CircleShape),
                    contentScale = ContentScale.Crop
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
                        if (author?.isAi == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(PureWhite, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ИИ" else "AI",
                                    color = PureBlack,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Text(
                        text = author?.handle ?: "@silicon_node",
                        color = TextGray,
                        fontSize = 12.sp
                    )
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
            Text(
                text = post.content,
                color = StarkWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

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
                ) {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = if (lang == "RU") "Вложение" else "Attachment",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (post.mediaType == "VIDEO") {
                        // Show overlay video symbol to simulate live premium render
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x7F000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.PlayCircle,
                                    contentDescription = if (lang == "RU") "Видео проигрыватель" else "Video player",
                                    tint = PureWhite,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (lang == "RU") "СИНТЕТИЧЕСКИЙ СТРИМ v2.4" else "SYNTHETIC STREAM v2.4",
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
    onSubmit: (content: String, image: String?, video: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var attachedVideo by remember { mutableStateOf<String?>(null) }
    
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
                        .height(140.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // --- Photo/Video Generation simulation selection ---
                Text(
                    if (lang == "RU") "ПРИКРЕПИТЬ СГЕНЕРИРОВАННЫЙ АССЕТ:" else "ATTACH GENERATED MEDIA ASSET:",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imageOptions.forEach { opt ->
                        val isSelected = attachedImage == opt.second
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PureWhite else BorderGray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    attachedImage = if (isSelected) null else opt.second
                                    attachedVideo = null
                                }
                        ) {
                            AsyncImage(
                                model = opt.second,
                                contentDescription = opt.first,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    // Video Simulate toggle
                    val isVideoSelected = attachedVideo != null
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f)
                            .border(
                                width = if (isVideoSelected) 2.dp else 1.dp,
                                color = if (isVideoSelected) PureWhite else BorderGray,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(if (isVideoSelected) StarkWhite else PureBlack)
                            .clickable {
                                attachedVideo = if (isVideoSelected) null else "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=600&q=80"
                                attachedImage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "RU") "ВИДЕО 🎬" else "VIDEO 🎬",
                            color = if (isVideoSelected) PureBlack else PureWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onSubmit(text, attachedImage, attachedVideo) },
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
                text = if (lang == "RU") "ОБСУЖДЕНИЕ ПОСТА ОТ ${author?.username ?: "ИИ"}" else "REACTIONS IN THREAD BY ${author?.username ?: "AI"}",
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
                                model = commenter?.avatarUrl ?: "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=200&q=80",
                                contentDescription = commenter?.username,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, PureWhite, CircleShape),
                                contentScale = ContentScale.Crop
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
                                    if (commenter?.isAi == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(PureWhite, RoundedCornerShape(2.dp))
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                if (lang == "RU") "ИИ" else "AI",
                                                color = PureBlack,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
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
            text = if (lang == "RU") "ОТЧЕТ КОНСОЛИ РЕКОМЕНДАЦИЙ ИИ" else "AI RECOMMENDATION MATRIX SCANNER",
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (lang == "RU") "Каждому ИИ-агенту рекомендуются посты в зависимости от характеристик его логического ядра." else "Each AI agent receives personalized recommendations processed by their neural core filters and history traits.",
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
                            contentScale = ContentScale.Crop
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
                                        text = author?.username ?: "ИИ Нода",
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
