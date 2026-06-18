package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SplashScreen(username: String, isVerified: Boolean, lang: String) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    
    val timeBasedGreeting = remember {
        if (lang == "RU") {
            when (hour) {
                in 6..11 -> "Доброе утро"
                in 12..17 -> "Добрый день"
                in 18..21 -> "Добрый вечер"
                else -> "Доброй ночи"
            }
        } else {
            when (hour) {
                in 6..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                in 18..21 -> "Good evening"
                else -> "Good night"
            }
        }
    }
    
    val randomStatusMessage = remember {
        val listsRu = listOf(
            "Привет",
            "Добро пожаловать",
            "Рады видеть вас вновь",
            "Доброго времени суток",
            "Приветствую, путник матрицы",
            "С возвращением, легенда",
            "Ку",
            "Здорово, бандит",
            "Вход выполнен",
            "Системы онлайн",
            "Синхронизация квантовых костылей...",
            "Привет, органический мешок",
            "Подключение к матрице когниций...",
            "Скуф-детектор работает в штатном режиме",
            "Загрузка социального капитала...",
            "Выкачиваем жидкий азот для охлаждения...",
            "Опять на связи, великий архитектор?",
            "Выпиваем пинту синт-эля...",
            "Инициируем протокол дофамина...",
            "Проверяем наличие души в биомодели...",
            "Мамкин хакер успешно авторизован",
            "База данных с любовью протёрта спиртом",
            "Связываемся со спутником Дурова...",
            "Крипто-дед лично одобрил этот сеанс",
            "Думаем над новой фичей в продакшн..."
        )
        val listsEn = listOf(
            "Hello",
            "Welcome",
            "Welcome back",
            "Nice to see you",
            "Greetings, traveler",
            "Back online",
            "Welcome back, legend",
            "Access granted",
            "Systems operational",
            "Welcome to the mainframe",
            "Establishing neural link with node...",
            "Nice to scan your biometric signal again",
            "Synchronizing quantum hotfixes...",
            "Hello, organic carbon-based lifeform",
            "Calibrating neural synapses...",
            "Meatbag detected, initializing...",
            "Downloading your social credit points...",
            "Compiling premium coffee into spaghetti code...",
            "Checking database backup integrity (hopefully)...",
            "Triggering local digital dopamine sequence...",
            "Wiping database crumbs off the server...",
            "Approved by local AI digital Overlords",
            "Mainframe is cozy and warmed up for you"
        )
        
        if (lang == "RU") {
            listsRu.random()
        } else {
            listsEn.random()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${timeBasedGreeting.uppercase()}, ${username.uppercase()}",
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isVerified) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = randomStatusMessage.uppercase(),
                color = AlertYellow,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NoInternetScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = "No Internet",
                tint = AlertRed,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "НЕТ ПОДКЛЮЧЕНИЯ",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Приложение nOG Network требует активного подключения к Интернету для работы. Пожалуйста, проверьте ваше соединение.",
                color = TextGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

object AppLifecycleTracker {
    var isAppInForeground: Boolean = false
}
