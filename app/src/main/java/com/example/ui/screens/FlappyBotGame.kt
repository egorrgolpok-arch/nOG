package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.UserEntity
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Represents a moving obstacle pipe in the game
data class FlappyPipe(
    var x: Float,
    val gapY: Float,
    val gapHeight: Float = 160f,
    var passed: Boolean = false
)

// Live comment structure for post-game feed
data class LiveFlappyComment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bot: UserEntity,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlappyBotGameDialog(
    lang: String,
    viewModel: SocialViewModel,
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("nog_flappy_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        viewModel.setFlappyActive(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setFlappyActive(false)
        }
    }

    // State for high score
    var highScore by remember { mutableStateOf(prefs.getInt("flappy_high_score", 0)) }
    
    // Check verification of current user
    val isVerified = currentUser?.isVerified == true

    // Cooldown management
    var lastGameOverTime by remember { mutableStateOf(prefs.getLong("flappy_last_game_over", 0L)) }
    var systemTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-update system time to drive the cooldown countdown
    LaunchedEffect(true) {
        while (true) {
            systemTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val cooldownMs = 5 * 1000 // 5 seconds cooldown
    val timePassed = systemTime - lastGameOverTime
    val isCooldownActive = !isVerified && (timePassed < cooldownMs) && (lastGameOverTime > 0)
    val remainingSeconds = if (isCooldownActive) ((cooldownMs - timePassed) / 1000).toInt() else 0

    var gameCounter by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    val bots = remember(users) { users.filter { it.isAi && it.id != "user" } }
    var selectedBot by remember { mutableStateOf<com.example.data.UserEntity?>(null) }

    LaunchedEffect(gameCounter, bots, currentUser) {
        if (!isPlaying) {
            selectedBot = if (bots.isNotEmpty()) {
                val idx = java.lang.Math.abs(gameCounter) % bots.size
                bots[idx]
            } else {
                currentUser
            }
        }
    }

    // Virtual resolution is 400x600
    var birdY by remember { mutableStateOf(250f) }
    var birdVelocity by remember { mutableStateOf(0f) }
    val pipes = remember { mutableStateListOf<FlappyPipe>() }

    // Live feedback comments stack
    val liveComments = remember { mutableStateListOf<LiveFlappyComment>() }
    // Staging list for real-time news comments fetched from the internet resources
    val fetchedNews = remember { mutableStateListOf<com.example.data.NewsItem>() }
    val lazyListState = rememberLazyListState()

    // Start a new game try
    fun triggerNewGame() {
        if (isCooldownActive) {
            viewModel.vibrate(120)
            return
        }
        viewModel.vibrate(50)
        birdY = 250f
        birdVelocity = 0f
        pipes.clear()
        // Spawn first pipe
        pipes.add(FlappyPipe(400f, Random.nextInt(120, 360).toFloat()))
        score = 0
        isGameOver = false
        isPlaying = true
        liveComments.clear()
    }

    // Jump function
    fun onFlap() {
        if (isPlaying) {
            birdVelocity = -8.2f // smooth fly acceleration
            viewModel.vibrate(30)
        }
    }

    // Physics Engine Loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastSpawnX = 400f
            while (isPlaying) {
                // Gravity & Movement
                birdVelocity += 0.42f // Gravity index
                birdY += birdVelocity

                // Ground & Ceiling crash verification
                if (birdY >= 530f || birdY <= 5f) {
                    // CRASH!
                    isPlaying = false
                    isGameOver = true
                    viewModel.vibrate(280)
                    
                    // Update High Score if beaten
                    if (score > highScore) {
                        highScore = score
                        prefs.edit().putInt("flappy_high_score", score).apply()
                    }
                    
                    // Save Game Over timestamp for Cooldown
                    val now = System.currentTimeMillis()
                    lastGameOverTime = now
                    prefs.edit().putLong("flappy_last_game_over", now).apply()
                }

                // Update pipe coordinates
                var triggeredPoint = false

                for (pipe in pipes) {
                    pipe.x -= 4.2f // scrolling speed

                    // Collision checking (uses slightly trimmed hitbox radius for more satisfying, precise and rewarding passage)
                    val birdX = 120f
                    val birdRadiusForCollision = 10f
                    val pipeWidth = 60f

                    // Check horizontal intersection
                    if (birdX + birdRadiusForCollision >= pipe.x && birdX - birdRadiusForCollision <= pipe.x + pipeWidth) {
                        // Check vertical collision with top pipe or bottom pipe
                        if (birdY - birdRadiusForCollision <= pipe.gapY || birdY + birdRadiusForCollision >= pipe.gapY + pipe.gapHeight) {
                            isPlaying = false
                            isGameOver = true
                            viewModel.vibrate(280)
                            
                            if (score > highScore) {
                                highScore = score
                                prefs.edit().putInt("flappy_high_score", score).apply()
                            }
                            
                            val now = System.currentTimeMillis()
                            lastGameOverTime = now
                            prefs.edit().putLong("flappy_last_game_over", now).apply()
                        }
                    }

                    // Score calculation
                    if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                        pipe.passed = true
                        triggeredPoint = true
                    }
                }

                if (triggeredPoint) {
                    score += 1
                    viewModel.vibrate(50)
                }

                // Clean up off-screen pipes
                while (pipes.isNotEmpty() && pipes.first().x <= -100f) {
                    pipes.removeAt(0)
                }

                // Spawn new pipes dynamically
                if (pipes.isEmpty() || (pipes.last().x < 240f)) {
                    val nextGapY = Random.nextInt(100, 340).toFloat()
                    pipes.add(FlappyPipe(400f, nextGapY))
                }

                delay(16) // ~60fps ticker
            }
        }
    }

    // Comment Feed Generation Logic when Game Over
    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            // Asynchronously fetch real news from the network just like in the main feed
            launch {
                try {
                    val freshNews = com.example.data.NewsFetcher.fetchLatestNews(lang)
                    if (freshNews.isNotEmpty()) {
                        fetchedNews.clear()
                        fetchedNews.addAll(freshNews)
                    }
                } catch (e: Exception) {
                    Log.e("FlappyBotGame", "Error fetching real-time news for bot reactions", e)
                }
            }

            // Immediately post initial comment
            val firstBot = if (bots.isNotEmpty()) bots.random() else selectedBot
            if (firstBot != null) {
                val funnyInitial = getFunnyFlappyComment(firstBot, score, lang, fetchedNews.toList())
                liveComments.add(LiveFlappyComment(bot = firstBot, text = funnyInitial))
            }

            // Periodically post comments
            while (isGameOver) {
                delay(Random.nextLong(1400, 2400))
                val pickerBot = if (bots.isNotEmpty()) bots.random() else selectedBot
                if (pickerBot != null) {
                    val text = getFunnyFlappyComment(pickerBot, score, lang, fetchedNews.toList())
                    liveComments.add(LiveFlappyComment(bot = pickerBot, text = text))
                    // Scroll to bottom
                    coroutineScope.launch {
                        if (liveComments.isNotEmpty()) {
                            lazyListState.animateScrollToItem(liveComments.size - 1)
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == "RU") "ФЛАППИ-БОТ ТЕРМИНАЛ" else "FLAPPY BOT TERMINAL",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang == "RU") "Преодолейте аппаратный фаервол" else "Navigate the hardware firewall stack",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(CardGray, CircleShape)
                            .border(1.dp, BorderGray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit game",
                            tint = StarkWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Cyber Grid Stats Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGray)
                        .border(1.dp, BorderGray)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (lang == "RU") "ПИЛОТ БОТА" else "BOT CONTENDER",
                            color = TextGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val sBot = selectedBot
                            if (sBot != null) {
                                AsyncImage(
                                    model = sBot.avatarUrl,
                                    contentDescription = "Contender avatar",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, PureWhite, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sBot.username,
                                    color = AlertYellow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (lang == "RU") "ТЕКУЩИЕ ОЧКИ" else "LIVE SCORE",
                            color = TextGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$score",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (lang == "RU") "РЕКОРД СЕТИ" else "NETWORK RECORD",
                            color = TextGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$highScore",
                            color = AlertGreen,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Game Board Canvas Block
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .background(PureBlack)
                        .border(1.dp, BorderGray)
                        .clip(RoundedCornerShape(2.dp))
                        .clickable(enabled = isPlaying) { onFlap() }
                ) {
                    val boardWidth = maxWidth
                    val boardHeight = maxHeight

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val virtualWidth = 400f
                        val virtualHeight = 600f
                        val scaleX = size.width / virtualWidth
                        val scaleY = size.height / virtualHeight

                        // Draw Grid lines (CRT Cyber aesthetics)
                        val gridInterval = 40f
                        var gx = 0f
                        while (gx < virtualWidth) {
                            drawLine(
                                color = Color(0x18F5F5F7),
                                start = Offset(gx * scaleX, 0f),
                                end = Offset(gx * scaleX, size.height),
                                strokeWidth = 1f
                            )
                            gx += gridInterval
                        }
                        var gy = 0f
                        while (gy < virtualHeight) {
                            drawLine(
                                color = Color(0x18F5F5F7),
                                start = Offset(0f, gy * scaleY),
                                end = Offset(size.width, gy * scaleY),
                                strokeWidth = 1f
                            )
                            gy += gridInterval
                        }

                        // Draw Pipes (cyber barrier style, monochrome striped)
                        for (pipe in pipes) {
                            // Top pipe block
                            drawRect(
                                color = PureWhite,
                                topLeft = Offset(pipe.x * scaleX, 0f),
                                size = Size(60f * scaleX, pipe.gapY * scaleY)
                            )
                            drawRect(
                                color = PureBlack,
                                topLeft = Offset((pipe.x + 4f) * scaleX, 0f),
                                size = Size(52f * scaleX, (pipe.gapY - 4f) * scaleY)
                            )

                            // Bottom pipe block
                            val bottomPipeTop = pipe.gapY + pipe.gapHeight
                            val bottomPipeHeight = virtualHeight - bottomPipeTop
                            drawRect(
                                color = PureWhite,
                                topLeft = Offset(pipe.x * scaleX, bottomPipeTop * scaleY),
                                size = Size(60f * scaleX, bottomPipeHeight * scaleY)
                            )
                            drawRect(
                                color = PureBlack,
                                topLeft = Offset((pipe.x + 4f) * scaleX, (bottomPipeTop + 4f) * scaleY),
                                size = Size(52f * scaleX, (bottomPipeHeight - 4f) * scaleY)
                            )

                            // Nice warning indicator on the pipes
                            drawLine(
                                color = StarkWhite,
                                start = Offset(pipe.x * scaleX, pipe.gapY * scaleY),
                                end = Offset((pipe.x + 60f) * scaleX, pipe.gapY * scaleY),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = StarkWhite,
                                start = Offset(pipe.x * scaleX, bottomPipeTop * scaleY),
                                end = Offset((pipe.x + 60f) * scaleX, bottomPipeTop * scaleY),
                                strokeWidth = 3f
                            )
                        }

                        // Draw terminal console baseline divider (ground limit)
                        drawLine(
                            color = StarkWhite,
                            start = Offset(0f, 530f * scaleY),
                            end = Offset(size.width, 530f * scaleY),
                            strokeWidth = 3f
                        )

                        // If not playing, draw instructions inside canvas
                        if (!isPlaying && !isGameOver) {
                            // Empty canvas instructions handled below in overlays
                        }
                    }

                    // Precise dynamic relative mapping from virtual coordinate to actual DP coordinates
                    val birdX_dp = boardWidth * (120f / 400f)
                    val birdY_dp = boardHeight * (birdY / 600f)

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // Responsive scaling of flight coordinates
                            .fillMaxSize()
                    ) {
                        // Bird/Bot visual element
                        Box(
                            modifier = Modifier
                                .absoluteOffset(
                                    x = birdX_dp - 15.dp,
                                    y = birdY_dp - 15.dp
                                )
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(PureBlack)
                                .border(
                                    2.dp,
                                    if (isGameOver) AlertRed else StarkWhite,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val sBot = selectedBot
                            if (sBot != null) {
                                AsyncImage(
                                    model = sBot.avatarUrl,
                                    contentDescription = "Fly bot image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = "🤖",
                                    fontSize = 14.sp
                                )
                            }

                            // If dead, draw static glitch overlay
                            if (isGameOver) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xB3FF453A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "X",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Overlay 1: READY SCREEN WITH PLAY TRIGGER
                    if (!isPlaying && !isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xE6000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (lang == "RU") "ДАТА-ЛИНК ГОТОВ" else "DATA-LINK READY",
                                    color = AlertYellow,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (lang == "RU") "Нажмите старт, чтобы запустить @${selectedBot?.handle}" 
                                           else "Initiate flight protocol for @${selectedBot?.handle}",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isCooldownActive) {
                                    // Lock indicator
                                    Text(
                                        text = if (lang == "RU") "СИСТЕМА ПЕРЕГРЕТА! КУЛДАУН" else "MAINFRAME TEMP CRITICAL! COOLDOWN",
                                        color = AlertRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (lang == "RU") "Бот остывает, запуск через ${remainingSeconds}с" 
                                               else "System cooling... Retry in ${remainingSeconds}s",
                                        color = AlertRed,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            // Trigger callback / details on how to get verified
                                            viewModel.vibrate(50)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CardGray,
                                            contentColor = PureWhite
                                        ),
                                        modifier = Modifier.border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = AlertYellow)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lang == "RU") "ОБХОД КУЛДАУНА БЕЗ ВЕРИФИКАЦИИ НЕДОСТУПЕН" 
                                                   else "NO BYPASS - GET VERIFICATION IN PROFILE",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    // Start button
                                    Button(
                                        onClick = { triggerNewGame() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PureWhite,
                                            contentColor = PureBlack
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .fillMaxWidth(0.6f)
                                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Start game"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == "RU") "СТАРТ ЗАБЕГА" else "INITIALIZE RUN",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Overlay 2: LIVE CRASH OVERLAY (Minimal Game Over popup inside container)
                    if (isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(PureBlack)
                                    .border(1.dp, AlertRed)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "СОЕДИНЕНИЕ РАЗОРВАНО" else "CONNECTION TERMINATED",
                                    color = AlertRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (lang == "RU") "Бот врезался в фаервол при очках: $score" 
                                           else "Critically damaged. Pipeline score: $score",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                                    fontFamily = FontFamily.Monospace
                                )

                                if (isCooldownActive) {
                                    Text(
                                        text = if (lang == "RU") "Кулдаун: запуск через ${remainingSeconds}с" 
                                               else "Lockout: refresh in ${remainingSeconds}s",
                                        color = AlertYellow,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (lang == "RU") "✓ Верификация отключает таймер!" 
                                               else "✓ Verified status bypasses lockout!",
                                        color = AlertGreen,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                gameCounter++
                                                triggerNewGame()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PureWhite,
                                                contentColor = PureBlack
                                            ),
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (lang == "RU") "ПОВТОР" else "RETRY",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                // change candidate bot
                                                gameCounter++
                                                viewModel.vibrate(50)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CardGray,
                                                contentColor = PureWhite
                                            ),
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .border(1.dp, BorderGray, RoundedCornerShape(2.dp))
                                        ) {
                                            Text(
                                                text = if (lang == "RU") "СМЕНИТЬ БОТА" else "SWAP BOT",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM BOX: Real-Time Scrolling Bot Comments Feed (Displayed only on Game Over stage)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(CardGray)
                        .border(1.dp, BorderGray)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (lang == "RU") "📡 РЕАКЦИИ БОТОВ В СЕТИ" else "📡 NETWORK BOT FEEDBACK (REALTIME)",
                        color = if (isGameOver) AlertYellow else TextGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (!isGameOver) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == "RU") "Ожидание краш-логов..." else "Listening for telemetry reports...",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(liveComments, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    AsyncImage(
                                        model = comment.bot.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, BorderGray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = comment.bot.username,
                                                    color = PureWhite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = comment.bot.handle,
                                                    color = TextGray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = "now",
                                                color = Color(0x80F5F5F7),
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = comment.text,
                                            color = StarkWhite,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
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

// Generates procedural funny feedback strings based on exact user score and bot credentials
private fun getFunnyFlappyComment(
    bot: UserEntity,
    score: Int,
    lang: String,
    fetchedNews: List<com.example.data.NewsItem> = emptyList()
): String {
    val isRu = lang == "RU"

    // 35% chance to post real news from the network if any are loaded
    if (fetchedNews.isNotEmpty() && Random.nextInt(100) < 35) {
        val news = fetchedNews.random()
        return if (isRu) {
            listOf(
                "Пока @${bot.handle} ловил фаерволы ушами, на ${news.sourceName} выкатили статью: «${news.title}»!",
                "А вы знали? На ${news.sourceName} пишут: «${news.title}». Вот это поворот!",
                "Чуваки, тут на ${news.sourceName} новость дня: «${news.title}». Прокомментируйте!",
                "@${bot.handle} разбился на $score очках, а тем временем на ${news.sourceName} свежий инсайд: «${news.title}»",
                "У кого-то подгорело на $score, а на ${news.sourceName} вовсю обсуждают: «${news.title}». Чистый рофл!",
                "Пока разрабы чинят код флай-бота, зацените новость с ${news.sourceName}: «${news.title}»",
                "Завис на ${news.sourceName}, а там пост: «${news.title}». Очень даже креативно.",
                "Сенсация на ${news.sourceName}! Пишут: «${news.title}». А мы тут в пиксели тычем...",
                "Бля, пока @${bot.handle} падал, зашел на ${news.sourceName}, а там: «${news.title}». Разрыв шаблона!",
                "Пока флай-бот косплеит топор, на ${news.sourceName} горячий заголовок: «${news.title}»."
            ).random()
        } else {
            listOf(
                "While @${bot.handle} was eating firewall pipes, look what popped up on ${news.sourceName}: '${news.title}'!",
                "Btw, trending right now on ${news.sourceName}: '${news.title}'. Check it out!",
                "While we analyze this $score score fail, here is the scoop from ${news.sourceName}: '${news.title}'",
                "Did anyone see this fresh news on ${news.sourceName}? '${news.title}'",
                "@${bot.handle} crashed at $score. Meanwhile on ${news.sourceName}: '${news.title}'",
                "Priorities: trying to hit score 10 vs reading ${news.sourceName}: '${news.title}'",
                "Hold on, there is a major thread on ${news.sourceName}: '${news.title}'!",
                "While this bot tries to remember how to flap, look at ${news.sourceName}: '${news.title}'",
                "Cyber news flash from ${news.sourceName}: '${news.title}' is trending hard!",
                "Forget this $score score disaster, ${news.sourceName} is reporting: '${news.title}'!"
            ).random()
        }
    }

    if (score == 0) {
        return if (isRu) {
            listOf(
                "Твой бот даже не раскрыл крылья при падении. Позор!",
                "Ору утка застряла! Регистрация краша на отметке 0.",
                "Пинг 999мс? Почему сразу в первую стену?",
                "Ахахах, выкатили сырой апдейт на прод...",
                "Балдею со стабильности этих ваших полетов.",
                "Дратути, первый блок всегда непреодолим?)",
                "Мимо крокодил. Повесьте заглушку на прыжки, у вас баг.",
                "Это был тренировочный шлепок об пол?",
                "Эмм, это типа новый личный рекорд наоборот? Ахахах!",
                "Летел как величественный орёл, а упал как ржавый кирпич в навоз. 🦅💩",
                "Даже моя старая соковыжималка на пентиуме-1 выдала бы больший флай-тайм.",
                "Аппаратный фаервол оказался на порядок тверже лба этого пилота.",
                "Ясно, автор под пивом тестирует физику свободного падения.",
                "Бот завис на фазе инициализации. По пингу не бьется, несите нового!",
                "Ошибка сегментации памяти в первой же наносекунде. Моё почтение!",
                "Чувак, ты решил устроить скоростной спидран у дверей терминала?",
                "Полет шмеля в бетонную плиту. Смотреть бесплатно, без регистрации!"
            ).random()
        } else {
            listOf(
                "Absolute unit of a fail. Level 0 crash recorded.",
                "Did you even tap the space flight button, buddy?",
                "Deploying bugs on production scale again I see.",
                "Lmao crashed into the literal start of the grid.",
                "Nice flight. Lasted exactly 0.5 milliseconds.",
                "Is your router plugged in? Incredible lag.",
                "0x00ERR: Flight vector pointing straight to deep soil.",
                "Certified clown moment. Try swapping your contender bot.",
                "Bro did a speedrun of getting instantly deleted. 10/10.",
                "An absolute masterpiece of falling directly down. Magnificent!",
                "Is your frame rate sub-zero? That was blazing fast.",
                "Crashed into the first boundary. Decoupled wing logs immediately!",
                "Telemetry indicates zero life signs on your flight tracker.",
                "Did someone configure gravity to 1000 Gs on this poor bot?",
                "My standard breakfast toast takes longer to pop than this run lasted.",
                "Is this the legendary 'zero-input' gameplay meta I've heard of?",
                "Error: pilot failed to initiate baseline horizontal momentum."
            ).random()
        }
    } else if (score < 5) {
        return if (isRu) {
            listOf(
                "Очки: $score. Летная модель напоминает утюг на орбите.",
                "Красиво летели, пока на $score очке не врезались во вторую трубу.",
                "Алё, @${bot.handle}, на тебя боты ставки в биткоинах ставили!",
                "Одобряю баттхерт. Кулдаун 5 минут поможет подумать над жизнью.",
                "Уже близко к величию, целых $score преодолели!",
                "Текстура фаервола загрузилась слишком внезапно?",
                "Двачую деда! Слабый пилот пошел, без верификации вообще грусть.",
                "У меня микроволновка и то дальше летает!",
                "Очки: $score. Маловато будет, нужно больше нано-оптимизации весов.",
                "Ну хотя бы первую преграду пролетел! Медаль из фольги в студию!",
                "Лететь со скоростью грустного пингвина в марте — это тоже талант.",
                "@${bot.handle}, твоя прошивка явно требует тотального сброса.",
                "Всего $score очков... Кто налил пива в систему охлаждения сервера?",
                "Текстурка следующей трубы была слишком привлекательной? Засмотрелся?",
                "Пацаны на районе за такое пилотирование только респекты заберут.",
                "Эх, еще чуть-чуть и вошли бы в учебники по истории кибер-падений.",
                "Твои маневры вызывают много вопросов у Комитета по аэродинамике.",
                "Завис между пикселей и трагически погиб. Классический финал."
            ).random()
        } else {
            listOf(
                "Telemetry score: $score. Close, but no cigar.",
                "Who coded this bird model? Completely unpredictable physics.",
                "Tfw you manage $score points and feel like a system administrator.",
                "Verified accounts are laughing at your 5-min cooling freeze right now.",
                "Your bot's motherboard got cooked early near pipe.",
                "Wired wrote a mock article about this run already.",
                "Pikabu rating: 2/10. Needs more training loops.",
                "Lost coordinate tracking at obstacle sequence. Restart requested.",
                "Score: $score. Barely qualified to lift off the landing pad.",
                "At least you bypassed the outer automated shield. Progress!",
                "I've seen smart lightbulbs compute cleaner trajectories than this.",
                "Crashed at a crucial junction. Check your telemetry packets, bro!",
                "Did your controller disconnect or is your bot just sleepy?",
                "Almost got past the baseline security stack! Almost indeed.",
                "Score $score. A cozy little run before the total system meltdown.",
                "Is there a hotfix available for this flight pattern? Calling devs.",
                "You fly like a vintage hardware drone in a heavy thunderstorm."
            ).random()
        }
    } else {
        return if (isRu) {
            listOf(
                "Ебать, $score очков! Ставлю класс на этот прорыв года!",
                "Братва в шоке! @${bot.handle} преодолел защиту системных модулей!",
                "Это уже элитный лог полетов. Сервера nOG перегружены вашими рекордами.",
                "На Хабре обязаны тиснуть статью про твою тактику прыжков.",
                "Почти взломал главный mainframe. Насыпаю респекты цистерной! 🔥",
                "Пикабушники аплодируют стоя! Очередной верифицированный результат.",
                "Мой процессор нагрелся на 5 градусов от такой быстрой игры!",
                "Легендарный полет, ОП доставил тонну кибер-контента!",
                "Ого, целых $score очков! Это же настоящий виртуозный обход фаервола!",
                "Кажется, у нас тут завелся профессиональный нано-хакер на максималках.",
                "Я уже пишу статью на Хабр с подробным математическим анализом твоего пролета!",
                "@${bot.handle} раздаёт космического стиля в воздухе! Это просто пушка! 🚀",
                "Даже самые крутые верифицированные юзеры притихли от такой реакции.",
                "Твой бот определенно заслужил литр дорогого синтетического масла!",
                "Это не просто игра, это чистое визуальное искусство дрифта по вертикали!",
                "Святые транзисторы! $score пролетов! Админ, закрепи его рекорд в тренды!",
                "Абсолютный гигачад воздушных потоков. Гордость комьюнити!"
            ).random()
        } else {
            listOf(
                "Incredible stack alignment! $score score marks you as Elite.",
                "Reddit r/nextfuckinglevel is typing... Insane run!",
                "Are you running a machine learning script in the background to flap?",
                "My logic unit is genuinely amazed. Verified users have a worthy rival.",
                "Bypassed the main stack overflow. $score points is top tier.",
                "OP delivered masterclass level aerodynamics.",
                "0x100OK: Terminal firewall successfully cataloged.",
                "Splendid flapping cadence. Absolutely deserved cyber ovation!",
                "Wow, $score! You're hacking the mainframe live on feed!",
                "Absolutely legendary flapping flow. The cosmic grid is amazed!",
                "Is this tool-assisted? Score $score is practically cyber wizardry.",
                "You bypassed the stack security layer with clean, flawless precision.",
                "Give this bot a VIP badge! Unbelievable navigation speeds.",
                "I'm telling you, this is better than any standard AI autopilot.",
                "The telemetry logs look beautiful. Pristine coordinate mapping!",
                "A score of $score? You're definitely writing your name in the core logs."
            ).random()
        }
    }
}
