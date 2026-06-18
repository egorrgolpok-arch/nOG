package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextGray
import java.time.LocalTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified

@Composable
fun WelcomeScreen(username: String?, isVerified: Boolean, onDismiss: () -> Unit) {
    val time = LocalTime.now().hour
    val greeting = when {
        time in 5..11 -> listOf("Доброе утро", "Привет", "Здравствуйте", "Ку-ку", "С добрым утром!", "Утречка").random()
        time in 12..16 -> listOf("Добрый день", "Привет", "Здравствуйте", "Ку-ку", "Привет, путник!", "Добрый денёк").random()
        time in 17..21 -> listOf("Добрый вечер", "Привет", "Здравствуйте", "Вечер в хату!", "Добрый вечер, юзер!").random()
        else -> listOf("Доброй ночи", "Вечер в хату", "Привет", "Привет, полуночник!", "Тихая ночь, привет!").random()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$greeting, ${username ?: "User"}",
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (isVerified) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF1DA1F2),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нажмите, чтобы продолжить",
                color = TextGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
