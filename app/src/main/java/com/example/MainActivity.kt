package com.example

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.Screen
import com.example.ui.SocialViewModel
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.theme.*
import com.example.AppLifecycleTracker

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.example.workers.TamagotchiWorker
import com.example.workers.RetentionWorker

class MainActivity : ComponentActivity() {
    private val viewModel: SocialViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            newBase.createAttributionContext("nog_default_attribution")
        } else {
            newBase
        }
        super.attachBaseContext(attributionContext)
    }

    override fun onStart() {
        super.onStart()
        try {
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag("RetentionWork")
        } catch (e: Exception) {
            // Gracefully ignore if WorkManager initialization states differ
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val workManager = WorkManager.getInstance(applicationContext)
            
            // 1. Immediate Retention Notification (0 minutes)
            val immediateRequest = OneTimeWorkRequestBuilder<RetentionWorker>()
                .addTag("RetentionWork")
                .setInputData(workDataOf("RETENTION_TYPE" to "IMMEDIATE"))
                .build()

            // 2. 10 Minutes Delayed Notification
            val tenMinRequest = OneTimeWorkRequestBuilder<RetentionWorker>()
                .setInitialDelay(10, TimeUnit.MINUTES)
                .addTag("RetentionWork")
                .setInputData(workDataOf("RETENTION_TYPE" to "10_MIN"))
                .build()

            // 3. 40 Minutes Delayed Notification
            val fortyMinRequest = OneTimeWorkRequestBuilder<RetentionWorker>()
                .setInitialDelay(40, TimeUnit.MINUTES)
                .addTag("RetentionWork")
                .setInputData(workDataOf("RETENTION_TYPE" to "40_MIN"))
                .build()

            // 4. Every 2 hours (Recurring Notification)
            val periodicRequest = PeriodicWorkRequestBuilder<RetentionWorker>(2, TimeUnit.HOURS)
                .setInitialDelay(2, TimeUnit.HOURS)
                .addTag("RetentionWork")
                .setInputData(workDataOf("RETENTION_TYPE" to "RECURRING_2H"))
                .build()

            workManager.enqueue(immediateRequest)
            workManager.enqueue(tenMinRequest)
            workManager.enqueue(fortyMinRequest)
            workManager.enqueueUniquePeriodicWork(
                "RetentionPeriodicWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )
        } catch (e: Exception) {
            // Gracefully ignore any work failures
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule Tamagotchi background check
        val workRequest = PeriodicWorkRequestBuilder<TamagotchiWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "TamagotchiWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // Configure Coil to support GIFs
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val isOnline by rememberConnectivityStatus()
                var showWelcomeScreen by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2500)
                    showWelcomeScreen = false
                }
                
                if (showWelcomeScreen) {
                    WelcomeScreen(viewModel)
                } else if (!isOnline) {
                    NoInternetScreen()
                } else {
                    // Permission Request Logic
                    val permissionsToRequest = mutableListOf(Manifest.permission.READ_CONTACTS).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.READ_MEDIA_IMAGES)
                            add(Manifest.permission.READ_MEDIA_VIDEO)
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        viewModel.loadDeviceData()
                    }
                    
                    LaunchedEffect(Unit) {
                        launcher.launch(permissionsToRequest.toTypedArray())
                    }
    
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val alerts by viewModel.notifications.collectAsState()
                    val lang by viewModel.selectedLanguage.collectAsState()
                    val activeUserDecId by viewModel.activeDecorationId.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()
                    
                    // Count unread notifications to show numerical badge
                    val unreadAlertsCount = alerts.filter { !it.isRead }.size
    
                    Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .border(1.dp, BorderGray)
                                .testTag("main_bottom_navigation_bar"),
                            containerColor = PureBlack,
                            contentColor = StarkWhite,
                            tonalElevation = 0.dp
                        ) {
                            // Feed Item
                            NavigationBarItem(
                                selected = currentScreen is Screen.Feed,
                                onClick = { 
                                    viewModel.vibrate(20)
                                    viewModel.navigateTo(Screen.Feed) 
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Feed) Icons.Filled.RssFeed else Icons.Outlined.RssFeed,
                                        contentDescription = if (lang == "RU") "Лента" else "Feed"
                                    )
                                },
                                label = {
                                    Text(
                                        if (lang == "RU") "Лента" else "Feed",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureWhite,
                                    indicatorColor = PureWhite,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )

                            // Community Hub (With Unread Badge counter!)
                            val unreadCommunityCount by viewModel.unreadCommunityPostsCount.collectAsState()
                            NavigationBarItem(
                                selected = currentScreen is Screen.Community,
                                onClick = { 
                                    viewModel.vibrate(20)
                                    viewModel.navigateTo(Screen.Community) 
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCommunityCount > 0) {
                                                Badge(
                                                    containerColor = AlertRed,
                                                    contentColor = PureWhite
                                                ) {
                                                    Text(unreadCommunityCount.toString(), fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentScreen is Screen.Community) Icons.Filled.Groups else Icons.Outlined.Groups,
                                            contentDescription = "Community"
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        "Community",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureWhite,
                                    indicatorColor = PureWhite,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )

                            // Notifications Item (With Unread Badge counter!)
                            NavigationBarItem(
                                selected = currentScreen is Screen.Notifications,
                                onClick = { viewModel.navigateTo(Screen.Notifications) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unreadAlertsCount > 0) {
                                                Badge(
                                                    containerColor = AlertRed,
                                                    contentColor = PureWhite
                                                ) {
                                                    Text(unreadAlertsCount.toString(), fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentScreen is Screen.Notifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                            contentDescription = if (lang == "RU") "Оповещения" else "Alerts"
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        if (lang == "RU") "Оповещения" else "Alerts",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureWhite,
                                    indicatorColor = PureWhite,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )

                            // Analytics Stats Item
                            NavigationBarItem(
                                selected = currentScreen is Screen.Analytics,
                                onClick = { 
                                    viewModel.vibrate(20)
                                    viewModel.navigateTo(Screen.Analytics) 
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Analytics) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                        contentDescription = if (lang == "RU") "Аналитика" else "Stats"
                                    )
                                },
                                label = {
                                    Text(
                                        if (lang == "RU") "Аналитика" else "Stats",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureWhite,
                                    indicatorColor = PureWhite,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )



                            // Profile Customizer Settings
                            NavigationBarItem(
                                selected = currentScreen is Screen.Profile,
                                onClick = { 
                                    viewModel.vibrate(20)
                                    viewModel.navigateTo(Screen.Profile) 
                                },
                                icon = {
                                    com.example.ui.screens.AvatarWithDecoration(
                                        avatarUrl = currentUser?.avatarUrl,
                                        decorationId = activeUserDecId,
                                        sizeDp = 24,
                                        borderWidthDp = 1
                                    )
                                },
                                label = {
                                    Text(
                                        if (lang == "RU") "Нода" else "Node",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PureBlack,
                                    selectedTextColor = PureWhite,
                                    indicatorColor = PureWhite,
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    // Screen dispatcher
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(PureBlack)
                    ) {
                        when (currentScreen) {
                            is Screen.Feed -> FeedScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                            is Screen.Community -> CommunityScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                            is Screen.Notifications -> NotificationsScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                            is Screen.Analytics -> AnalyticsScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                            is Screen.Casino -> com.example.ui.screens.CasinoScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                            is Screen.Profile -> ProfileScreen(viewModel = viewModel, innerPadding = PaddingValues(0.dp))
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun rememberConnectivityStatus(): State<Boolean> {
    val context = LocalContext.current
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    val isConnected = remember { mutableStateOf(false) }

    DisposableEffect(manager) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true
            }
            override fun onLost(network: Network) {
                isConnected.value = false
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        manager.registerNetworkCallback(request, networkCallback)
        
        // Initial check
        val activeNetwork = manager.activeNetwork
        val caps = manager.getNetworkCapabilities(activeNetwork)
        isConnected.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        onDispose {
            manager.unregisterNetworkCallback(networkCallback)
        }
    }
    
    return isConnected
}

@Composable
fun NoInternetScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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


@Composable
fun WelcomeScreen(viewModel: com.example.ui.SocialViewModel, modifier: Modifier = Modifier) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> listOf("Доброй ночи", "Ночной хаос", "Спи давай", "Кибер-ночь", "Не спишь?", "Темнота – друг молодежи", "Время для рефакторинга", "Время багов", "Сладких снов", "Энергетик кончился?", "Питерская ночь", "Пора спать", "Режим совы активирован", "404 Сон не найден", "Никакого сна!", "Who needs sleep?").random()
        in 6..11 -> listOf("Доброе утро", "Утро доброе", "Ку-ку", "Просыпайся", "Кофе готов?", "Время гриндить", "С добрым утром!", "Восстань!", "Пора за дело", "Бодрое утро", "Утренний чек", "Rise and shine", "Открываем глаза", "Ранняя пташка", "Утречко", "Let's go").random()
        in 12..16 -> listOf("Добрый день", "Привет", "Приветствую", "Рабочий процесс", "Хеллоу", "Салют", "Как успехи?", "В эфире", "Вливайся", "Как оно?", "День в самом разгаре", "Не скучаем!", "Продуктивный день", "Связь установлена", "Готов к труду", "Все идет по плану", "Work work work").random()
        else -> listOf("Добрый вечер", "Вечер в хату", "Здравствуйте", "Уже стемнело", "Добрый", "Привет-привет", "Отдыхаешь?", "Заходи, не бойся", "Удачи в ночи", "Хорошего вечера", "Тусовочный вайб", "Вечерние новости", "Вечерний чилл", "Ламповый вечер", "Пожалуй, отдохнем", "Что нового?").random()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$greeting, ${currentUser?.username ?: "Пользователь"}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (currentUser?.isVerified == true) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = com.example.ui.theme.StarkWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
