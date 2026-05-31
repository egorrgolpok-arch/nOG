package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationEntity
import com.example.ui.SocialViewModel
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val alerts by viewModel.notifications.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()

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
        ) {
            
            // --- Notification Toolbar Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (lang == "RU") "ПЕРИФЕРИЙНЫЕ УВЕДОМЛЕНИЯ" else "PERIPHERAL ALERTS FEED",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    onClick = { viewModel.clearNotifications() },
                    shape = RoundedCornerShape(4.dp),
                    color = DeepGray,
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Text(
                        text = if (lang == "RU") "ПРОЧИТАТЬ ВСЕ" else "MARK ALL READ",
                        color = PureWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Scrollable Alerts list ---
            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.NotificationsNone,
                            contentDescription = "Пусто",
                            tint = BorderGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == "RU") {
                                "Новых оповещений от AI не поступало."
                            } else {
                                "No new telemetry signals from local AI minds."
                            },
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == "RU") {
                                "Опубликуйте новость в эфир, чтобы вызвать интерес ИИ агентов!"
                            } else {
                                "Post any update to trigger AI node attention vectors!"
                            },
                            color = BorderGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts, key = { it.id }) { item ->
                        AlertRow(notification = item, onRowClick = {
                            if (item.postId != null) {
                                viewModel.selectPostForComments(item.postId)
                                viewModel.navigateTo(com.example.ui.Screen.Feed)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRow(
    notification: NotificationEntity,
    onRowClick: () -> Unit
) {
    val icon = when (notification.type) {
        "LIKE" -> Icons.Filled.Favorite
        "COMMENT" -> Icons.Filled.Comment
        "TREND" -> Icons.Filled.TrendingUp
        else -> Icons.Filled.Info
    }
    
    val iconColor = if (notification.isRead) TextGray else PureWhite
    val containerBg = if (notification.isRead) PureBlack else DeepGray
    val borderCol = if (notification.isRead) BorderGray else PureWhite

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg)
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .clickable { onRowClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (notification.isRead) DeepGray else StarkWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = notification.type,
                tint = if (notification.isRead) TextGray else PureBlack,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title.uppercase(),
                    color = iconColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AlertRed, CircleShape)
                    )
                }
            }
            Text(
                text = notification.message,
                color = if (notification.isRead) TextGray else StarkWhite,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
