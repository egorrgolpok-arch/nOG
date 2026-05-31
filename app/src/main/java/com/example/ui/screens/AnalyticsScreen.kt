package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnalyticsEntity
import com.example.ui.SocialViewModel
import com.example.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val rawMetrics by viewModel.analyticsData.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()

    // Aggregate metrics for visualization
    val likesCount = rawMetrics.filter { it.metricType == "LIKE_CLICK" }.size + 14
    val commentsCount = rawMetrics.filter { it.metricType == "COMMENT_POST" }.size + 8
    val scrollsCount = rawMetrics.filter { it.metricType == "FEED_SCROLL" }.size + 42
    val queriesCount = rawMetrics.filter { it.metricType == "NOG_QUERY" }.size + 2
    
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // --- Metrics Grid ---
            Text(
                if (lang == "RU") "ТЕЛЕМЕТРИЯ СЕТЕВОЙ АКТИВНОСТИ" else "NETWORK ACTIVITY TELEMETRY",
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(title = if (lang == "RU") "СКРОЛЛ ЛЕНТЫ" else "FEED SCROLLS", count = scrollsCount, modifier = Modifier.weight(1f))
                MetricWidget(title = if (lang == "RU") "КЛИКИ ЛАЙКОВ" else "LIKE CLICKS", count = likesCount, modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(title = if (lang == "RU") "ЗАПРОСЫ К ИИ" else "AI QUERIES", count = queriesCount, modifier = Modifier.weight(1f))
                MetricWidget(title = if (lang == "RU") "ОТВЕТЫ В НОДЫ" else "NODE REPLIES", count = commentsCount, modifier = Modifier.weight(1f))
            }

            // --- Custom Canvas Visualizer Graph (Stark Brutalist Line Chart) ---
            Text(
                if (lang == "RU") "КОНТРАСТНЫЙ ГРАФИК ВОВЛЕЧЕННОСТИ (Pulse Graph)" else "CONTRAST ENGAGEMENT INDEX (Pulse Graph)",
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, BorderGray),
                colors = CardDefaults.cardColors(containerColor = DeepGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val graphPoints = listOf(
                        scrollsCount.toFloat() * 1.5f + 10f,
                        likesCount.toFloat() * 3f + 15f,
                        queriesCount.toFloat() * 8f + 5f,
                        commentsCount.toFloat() * 5f + 20f,
                        (scrollsCount + likesCount).toFloat() * 0.8f + 12f,
                        (queriesCount + commentsCount).toFloat() * 3f + 25f
                    )
                    
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
                            val maxVal = (graphPoints.maxOrNull() ?: 100f).coerceAtLeast(50f)
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
                                color = PureWhite,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Draw dots
                            for (index in graphPoints.indices) {
                                val x = index * stepX
                                val y = canvasHeight - (graphPoints[index] / maxVal) * canvasHeight
                                drawCircle(
                                    color = PureWhite,
                                    radius = 6.dp.toPx(),
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
                        "PULSE",
                        color = PureWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
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
                TrustProgressBar(source = if (lang == "RU") "Synthetica News (Эклектический ИИ Стрим)" else "Synthetica News (Eclectic AI Stream)", trustScore = 45, color = AlertRed)
            }
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
