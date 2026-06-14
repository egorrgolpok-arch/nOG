package com.example.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.data.AnalyticsEntity
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

val BrightBlue = Color(0xFF2196F3)

@Composable
fun AnalyticsScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val rawMetrics by viewModel.analyticsData.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Real-time time spent data
    val weeklyHours by viewModel.weeklyEngagementHours.collectAsState()
    val hoursSpentToday by viewModel.appTimeSpentToday.collectAsState()

    // Safe, cheat-proof scores
    val userUniqueViews by viewModel.uniqueViewsCount.collectAsState()
    val userUniqueLikes = viewModel.likedPostIds.collectAsState().value.size
    val userUniqueComments by viewModel.uniqueCommentsCount.collectAsState()

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE) }

    // Navigation Tabs: 0 for Activity Telemetry, 1 for Verified Leaderboard
    var selectedTab by remember { mutableStateOf(0) }

    // Sub-tab for Leaderboards: 0 for Views, 1 for Likes, 2 for Comments
    var selectedLeaderboardCategory by remember { mutableStateOf(0) }

    // Claim state for rewards
    var showClaimSuccessDialog by remember { mutableStateOf<String?>(null) }
    var lastClaimTime by remember { mutableStateOf(prefs.getLong("last_weekly_leaderboard_claim", 0L)) }

    val isUserVerified = currentUser?.isVerified == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // --- Banner Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray)
                    .background(DeepGray)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Analytics,
                    contentDescription = "Analytics Icon",
                    tint = PureWhite,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (lang == "RU") "АНАЛИТИЧЕСКИЙ ТЕРМИНАЛ nOG SYSTEM" else "nOG SYSTEM ANALYTICS TERMINAL",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (lang == "RU") "Телеметрия вовлеченности пользователей в реальном времени" else "Real-time user engagement telemetry tracking",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            // --- Custom Dual Tabs Row (Brutalist Style) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray)
                    .background(PureBlack)
            ) {
                // Tab 0: Activity
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 }
                        .background(if (selectedTab == 0) DeepGray else PureBlack)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == "RU") "📊 ТЕЛЕМЕТРИЯ" else "📊 TELEMETRY",
                        color = if (selectedTab == 0) PureWhite else TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Tab 1: Leaderboard (Verified Only)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 }
                        .background(if (selectedTab == 1) DeepGray else PureBlack)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (lang == "RU") "🏆 РЕЙТИНГ" else "🏆 RATING",
                            color = if (selectedTab == 1) PureWhite else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isUserVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Premium segment locked",
                                tint = AlertYellow,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = "Verified unlocked",
                                tint = BrightBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // --- Content Render Branch ---
            if (selectedTab == 0) {
                // --- TELEMETRY VIEW (Tab 0) ---
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Clicks and interactions summary
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val totalScrollsRaw = rawMetrics.filter { it.metricType == "FEED_SCROLL" }.size + 42
                        MetricWidget(
                            title = if (lang == "RU") "СКРОЛЛ ЛЕНТЫ (ВСЕГО)" else "FEED SCROLLS (TOTAL)",
                            count = totalScrollsRaw,
                            modifier = Modifier.weight(1f)
                        )
                        MetricWidget(
                            title = if (lang == "RU") "КЛИКИ ЛАЙКОВ (ВСЕГО)" else "LIKE CLICKS (TOTAL)",
                            count = rawMetrics.filter { it.metricType == "LIKE_CLICK" }.size + 14,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricWidget(
                            title = if (lang == "RU") "УНИКАЛЬНЫХ ПРОСМОТРОВ" else "UNIQUE FEED VIEWS",
                            count = userUniqueViews,
                            modifier = Modifier.weight(1f)
                        )
                        MetricWidget(
                            title = if (lang == "RU") "АКТИВНЫХ КОММЕНТАРИЕВ" else "UNIQUE COMMENTS",
                            count = userUniqueComments,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Active Tracker info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                        colors = CardDefaults.cardColors(containerColor = DeepGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = "Clock",
                                tint = AlertGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (lang == "RU") "АКТИВНОЕ ВРЕМЯ (ЗА 24 ЧАСА)" else "ACTIVE TIMEOUT (PAST 24H)",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format("%.2f %s (%d %s)", hoursSpentToday, if (lang == "RU") "ч" else "hrs", (hoursSpentToday * 60).toInt(), if (lang == "RU") "мин" else "min"),
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // --- Custom Canvas Visualizer Graph (Fitted Line Chart) ---
                    Text(
                        if (lang == "RU") "ГРАФИК ВРЕМЕНИ В ПРИЛОЖЕНИИ (Еженедельный путник)" else "APP TIME ENGAGEMENT (Weekly Tracker)",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, BorderGray),
                        colors = CardDefaults.cardColors(containerColor = DeepGray)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            val graphPoints = weeklyHours
                            
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                // Draw clean high-contrast graph grid lines
                                val gridLines = 4
                                for (i in 0..gridLines) {
                                    val y = canvasHeight * i / gridLines
                                    drawLine(
                                        color = BorderGray,
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // Plotting points
                                if (graphPoints.isNotEmpty()) {
                                    val maxVal = (graphPoints.maxOrNull() ?: 1.0f).coerceAtLeast(0.5f)
                                    val stepX = canvasWidth / (graphPoints.size - 1)
                                    
                                    val path = Path().apply {
                                        val startY = canvasHeight - (graphPoints[0] / maxVal) * canvasHeight
                                        moveTo(0f, startY)
                                        for (index in 1 until graphPoints.size) {
                                            val x = index * stepX
                                            val y = canvasHeight - (graphPoints[index] / maxVal) * canvasHeight
                                            lineTo(x, y)
                                        }
                                    }
                                    
                                    // Draw graph path
                                    drawPath(
                                        path = path,
                                        color = AlertGreen,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    
                                    // Draw dots with dynamic highlight
                                    for (index in graphPoints.indices) {
                                        val x = index * stepX
                                        val y = canvasHeight - (graphPoints[index] / maxVal) * canvasHeight
                                        val dotColor = if (index == graphPoints.size - 1) AlertGreen else PureWhite
                                        drawCircle(
                                            color = dotColor,
                                            radius = (if (index == graphPoints.size - 1) 7.dp else 5.dp).toPx(),
                                            center = Offset(x, y)
                                        )
                                        drawCircle(
                                            color = PureBlack,
                                            radius = 3.dp.toPx(),
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                "REALTIME MONITOR",
                                color = AlertGreen,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }

                    // Day index indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = if (lang == "RU") {
                            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Сегодня")
                        } else {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")
                        }
                        labels.forEachIndexed { i, label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = if (i == 6) AlertGreen else TextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (i == 6) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = String.format("%.1fh", weeklyHours.getOrElse(i) { 0f }),
                                    color = if (i == 6) AlertGreen else TextGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // --- News Source Trust Scores ---
                    Text(
                        if (lang == "RU") "РЕЙТИНГ ДОВЕРИЯ ИСТОЧНИКОВ НОВОСТЕЙ (Trust Rating Audit)" else "NEWS TRUST RATING INDEX (Trust Rating Audit)",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepGray)
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TrustProgressBar(source = if (lang == "RU") "nOG News Agency (Премиальные Хроники)" else "nOG News Agency (Premium Chronicles)", trustScore = 99, color = AlertGreen)
                        TrustProgressBar(source = if (lang == "RU") "TruthMatrix AI (Объективный Аудит)" else "TruthMatrix AI (Objective Audit)", trustScore = 95, color = AlertGreen)
                        TrustProgressBar(source = if (lang == "RU") "Silicon Syndicate (Сводки Индустрии)" else "Silicon Syndicate (Industry Briefs)", trustScore = 88, color = PureWhite)
                        TrustProgressBar(source = if (lang == "RU") "Cybernetic Feed (Технические Тренды)" else "Cybernetic Feed (Technical Trends)", trustScore = 72, color = AlertYellow)
                    }
                }
            } else {
                // --- RATING / LEADERBOARDS (Tab 1) ---
                if (!isUserVerified) {
                    // Locked state overlay!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AlertYellow, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = DeepGray)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(AlertYellow.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Lock",
                                        tint = AlertYellow,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = if (lang == "RU") "ДОСТУП ОГРАНИЧЕН" else "ACCESS RESTRICTED",
                                    color = AlertYellow,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = if (lang == "RU") {
                                        "Рейтинг активности и еженедельный турнир доступны только верифицированным агентам со статусом галочки.\n\nПолучите галочку в своем профиле, чтобы участвовать в топе пользователей и гарантированно забирать до 1,000,000 монет каждую неделю за первые места!"
                                    } else {
                                        "The activity rating system and weekly tournament stats are locked exclusively for Verified Agents with a checkmark designation.\n\nClaim verification in your Profile screen to enter the leaderboard and unlock up to 1,000,000 coins every week!"
                                    },
                                    color = StarkWhite,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Divider(color = BorderGray, thickness = 1.dp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = "Tips",
                                        tint = TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == "RU") "Подсказка: Купить верификацию можно в Профиле." else "Tip: Obtain verification badge in Profile Screen.",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Unlocked Verified Leaderboard!
                    // Categorized sub-navigation filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray)
                            .background(DeepGray)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val subTabs = if (lang == "RU") {
                            listOf("🪙 Просмотры", "❤️ Лайки", "💬 Ответы")
                        } else {
                            listOf("🪙 Views", "❤️ Likes", "💬 Answers")
                        }

                        subTabs.forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedLeaderboardCategory == index) PureWhite else PureBlack)
                                    .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                    .clickable { selectedLeaderboardCategory = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (selectedLeaderboardCategory == index) PureBlack else PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // --- GENERATE RATING LIST OF BOTS + USER (FRAUD-PROOF / UNIQUE CHECKS ONLY) ---
                    // Stable, daily shifting bot generation setup
                    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                    val dayOffset = (dayOfYear % 12)

                    data class LeaderboardItem(
                        val name: String,
                        val avatarUrl: String,
                        val score: Int,
                        val isMe: Boolean,
                        val isVerified: Boolean
                    )

                    val listItems = remember(selectedLeaderboardCategory, userUniqueViews, userUniqueLikes, userUniqueComments, dayOffset) {
                        val items = mutableListOf<LeaderboardItem>()
                        
                        // Add the user with their dynamic cheat-proof scores
                        val userScore = when (selectedLeaderboardCategory) {
                            0 -> userUniqueViews
                            1 -> userUniqueLikes
                            else -> userUniqueComments
                        }
                        
                        items.add(
                            LeaderboardItem(
                                name = currentUser?.username ?: "You (Verified)",
                                avatarUrl = currentUser?.avatarUrl ?: "https://i.pravatar.cc/150?u=user",
                                score = userScore,
                                isMe = true,
                                isVerified = true
                            )
                        )

                        // Add stable, living bots with daily random activity to look organic
                        when (selectedLeaderboardCategory) {
                            0 -> { // Views
                                items.add(LeaderboardItem("cyber_neo", "https://i.pravatar.cc/150?u=1", 85 + dayOffset * 2, false, true))
                                items.add(LeaderboardItem("truth_matrix_ai", "https://i.pravatar.cc/150?u=2", 72 + dayOffset * 3, false, true))
                                items.add(LeaderboardItem("silicon_synd", "https://i.pravatar.cc/150?u=3", 58 + dayOffset, false, false))
                                items.add(LeaderboardItem("synthetica_bot", "https://i.pravatar.cc/150?u=4", 45 + dayOffset * 2, false, true))
                                items.add(LeaderboardItem("quantum_coder", "https://i.pravatar.cc/150?u=5", 37 + dayOffset, false, false))
                                items.add(LeaderboardItem("cybernetic_flow", "https://i.pravatar.cc/150?u=6", 30 + dayOffset, false, false))
                                items.add(LeaderboardItem("truthmatrix_ai", "https://i.pravatar.cc/150?u=7", 22 + dayOffset, false, false))
                                items.add(LeaderboardItem("noodle_bot", "https://i.pravatar.cc/150?u=8", 14 + dayOffset, false, false))
                                items.add(LeaderboardItem("omega_pulse_ai", "https://i.pravatar.cc/150?u=9", 9, false, false))
                                items.add(LeaderboardItem("delta_prime", "https://i.pravatar.cc/150?u=10", 4, false, false))
                            }
                            1 -> { // Likes
                                items.add(LeaderboardItem("truth_matrix_ai", "https://i.pravatar.cc/150?u=2", 48 + dayOffset, false, true))
                                items.add(LeaderboardItem("cyber_neo", "https://i.pravatar.cc/150?u=1", 39 + dayOffset, false, true))
                                items.add(LeaderboardItem("silicon_synd", "https://i.pravatar.cc/150?u=3", 26 + dayOffset, false, false))
                                items.add(LeaderboardItem("synthetica_bot", "https://i.pravatar.cc/150?u=4", 19 + dayOffset, false, true))
                                items.add(LeaderboardItem("quantum_coder", "https://i.pravatar.cc/150?u=5", 13, false, false))
                                items.add(LeaderboardItem("cybernetic_flow", "https://i.pravatar.cc/150?u=6", 10, false, false))
                                items.add(LeaderboardItem("truthmatrix_ai", "https://i.pravatar.cc/150?u=7", 6, false, false))
                                items.add(LeaderboardItem("noodle_bot", "https://i.pravatar.cc/150?u=8", 4, false, false))
                                items.add(LeaderboardItem("omega_pulse_ai", "https://i.pravatar.cc/150?u=9", 2, false, false))
                                items.add(LeaderboardItem("delta_prime", "https://i.pravatar.cc/150?u=10", 1, false, false))
                            }
                            else -> { // Comments / Answers
                                items.add(LeaderboardItem("synthetica_bot", "https://i.pravatar.cc/150?u=4", 22 + dayOffset / 2, false, true))
                                items.add(LeaderboardItem("truth_matrix_ai", "https://i.pravatar.cc/150?u=2", 18 + dayOffset / 3, false, true))
                                items.add(LeaderboardItem("cyber_neo", "https://i.pravatar.cc/150?u=1", 14 + dayOffset / 4, false, true))
                                items.add(LeaderboardItem("silicon_synd", "https://i.pravatar.cc/150?u=3", 9, false, false))
                                items.add(LeaderboardItem("quantum_coder", "https://i.pravatar.cc/150?u=5", 7, false, false))
                                items.add(LeaderboardItem("cybernetic_flow", "https://i.pravatar.cc/150?u=6", 5, false, false))
                                items.add(LeaderboardItem("truthmatrix_ai", "https://i.pravatar.cc/150?u=7", 3, false, false))
                                items.add(LeaderboardItem("noodle_bot", "https://i.pravatar.cc/150?u=8", 2, false, false))
                                items.add(LeaderboardItem("omega_pulse_ai", "https://i.pravatar.cc/150?u=9", 1, false, false))
                                items.add(LeaderboardItem("delta_prime", "https://i.pravatar.cc/150?u=10", 0, false, false))
                            }
                        }

                        // Order descending by unique score
                        items.sortedByDescending { it.score }
                    }

                    // Find user's place in this leaderboard list (1-indexed)
                    val userRank = listItems.indexOfFirst { it.isMe } + 1

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Rewards Pool Banner Case card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AlertGreen, RoundedCornerShape(4.dp)),
                                colors = CardDefaults.cardColors(containerColor = DeepGray)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.EmojiEvents,
                                            contentDescription = "Trophy",
                                            tint = AlertYellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (lang == "RU") "ЕЖЕНЕДЕЛЬНЫЙ ФОНД ТУРНИРА" else "WEEKLY TOURNAMENT FUND",
                                            color = PureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Text(
                                        text = if (lang == "RU") {
                                            "Займите место в ТОП-10 в любой категории, чтобы забрать щедрую награду. Первое место в каждой категории вознаграждается в 1,000,000 монет!"
                                        } else {
                                            "Rank in the TOP-10 of any metrics board to unlock immense coin rewards. First priority rank pays out 1,000,000 coins immediately!"
                                        },
                                        color = StarkWhite,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )

                                    Divider(color = BorderGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                    // User status representation
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (lang == "RU") "ТВОЙ РАНГ СЕЙЧАС:" else "YOUR ACTIVE RANK:",
                                                color = TextGray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = if (userRank in 1..10) "🔥 #$userRank у Места" else "💤 #$userRank у Места",
                                                color = if (userRank in 1..10) AlertGreen else AlertRed,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Claim button logic
                                        val isEligible = userRank in 1..10
                                        val now = System.currentTimeMillis()
                                        val weekMs = 7L * 24 * 3600 * 1000L
                                        val claimedThisWeek = (now - lastClaimTime) < weekMs

                                        if (claimedThisWeek) {
                                            // Countdown
                                            val diff = weekMs - (now - lastClaimTime)
                                            val days = TimeUnit.MILLISECONDS.toDays(diff)
                                            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
                                            Box(
                                                modifier = Modifier
                                                    .background(BorderGray, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == "RU") "СБОР ЧЕРЕЗ ${days}д ${hours}ч" else "REFRESH: ${days}d ${hours}h",
                                                    color = TextGray,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (isEligible) {
                                            Button(
                                                onClick = {
                                                    // Determine reward
                                                    val reward = when (userRank) {
                                                        1 -> 1000000
                                                        2 -> 500000
                                                        3 -> 250000
                                                        else -> 100000
                                                    }
                                                    viewModel.updateCoins(viewModel.userCoins.value + reward)
                                                    prefs.edit().putLong("last_weekly_leaderboard_claim", now).apply()
                                                    lastClaimTime = now
                                                    viewModel.vibrate(100)
                                                    
                                                    showClaimSuccessDialog = if (lang == "RU") {
                                                        "Поздравляем! Вы заняли место #$userRank и получили $reward монет!"
                                                    } else {
                                                        "Splendid! You ranked #$userRank and collected $reward coins!"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == "RU") "ЗАБРАТЬ МОНЕТЫ 🪙" else "CLAIM COINS 🪙",
                                                    color = PureBlack,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, AlertRed, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == "RU") "НУЖЕН ТОП-10" else "TOP-10 NEEDED",
                                                    color = AlertRed,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Header spacer
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Ranking list
                        itemsIndexed(listItems) { idx, item ->
                            val currentPos = idx + 1
                            val rankColor = when (currentPos) {
                                1 -> AlertYellow // Gold
                                2 -> StarkWhite // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> TextGray
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (item.isMe) AlertGreen.copy(alpha = 0.08f) else DeepGray)
                                    .border(1.dp, if (item.isMe) AlertGreen else BorderGray, RoundedCornerShape(4.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position indicator
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (currentPos <= 3) rankColor.copy(alpha = 0.15f) else PureBlack, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentPos.toString(),
                                        color = rankColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Avatar
                                AsyncImage(
                                    model = item.avatarUrl,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, if (item.isMe) AlertGreen else BorderGray, CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = rememberVectorPainter(Icons.Filled.AccountCircle),
                                    placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Name
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (item.isMe) (if (lang == "RU") "@ты" else "@you") else "@${item.name}",
                                            color = if (item.isMe) AlertGreen else PureWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (item.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Filled.Verified,
                                                contentDescription = "Verified status",
                                                tint = BrightBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    // Reward scale hint helper
                                    if (currentPos in 1..10) {
                                        val bounty = when (currentPos) {
                                            1 -> "+1,000,000 🪙"
                                            2 -> "+500,000 🪙"
                                            3 -> "+250,000 🪙"
                                            else -> "+100,000 🪙"
                                        }
                                        Text(
                                            text = if (lang == "RU") "Ожидает сбора: $bounty" else "Est. bounty: $bounty",
                                            color = AlertYellow,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Score representation
                                Box(
                                    modifier = Modifier
                                        .background(PureBlack, RoundedCornerShape(4.dp))
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    val unit = when (selectedLeaderboardCategory) {
                                        0 -> if (lang == "RU") "глаз" else "looks"
                                        1 -> if (lang == "RU") "лайк" else "likes"
                                        else -> if (lang == "RU") "нод" else "nodes"
                                    }
                                    Text(
                                        text = "${item.score} $unit",
                                        color = if (item.isMe) AlertGreen else PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Celebrate claims popup dialog
        if (showClaimSuccessDialog != null) {
            AlertDialog(
                onDismissRequest = { showClaimSuccessDialog = null },
                confirmButton = {
                    Button(
                        onClick = { showClaimSuccessDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (lang == "RU") "ОТЛИЧНО" else "EXCELLENT",
                            color = PureBlack,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Success check",
                            tint = AlertGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == "RU") "УСПЕШНЫЙ СБОР" else "REWARD UNLOCKED",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                text = {
                    Text(
                        text = showClaimSuccessDialog ?: "",
                        color = StarkWhite,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                containerColor = DeepGray,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, AlertGreen, RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun MetricWidget(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DeepGray)
            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = TextGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString().padStart(6, '0'),
            color = PureWhite,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun TrustProgressBar(
    source: String,
    trustScore: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = source,
                color = StarkWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$trustScore%",
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(BorderGray, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(trustScore.toFloat() / 100f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
