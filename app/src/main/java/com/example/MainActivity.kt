package com.example

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

class MainActivity : ComponentActivity() {
    private val viewModel: SocialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                // Permission Request Logic
                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                } else {
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Handle result
                }
                
                LaunchedEffect(Unit) {
                    launcher.launch(permissionsToRequest.toTypedArray())
                }

                val currentScreen by viewModel.currentScreen.collectAsState()
                val alerts by viewModel.notifications.collectAsState()
                val lang by viewModel.selectedLanguage.collectAsState()
                
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
                                onClick = { viewModel.navigateTo(Screen.Feed) },
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

                            // Community Hub
                            NavigationBarItem(
                                selected = currentScreen is Screen.Community,
                                onClick = { viewModel.navigateTo(Screen.Community) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Community) Icons.Filled.Groups else Icons.Outlined.Groups,
                                        contentDescription = "Community"
                                    )
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
                                onClick = { viewModel.navigateTo(Screen.Analytics) },
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
                                onClick = { viewModel.navigateTo(Screen.Profile) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen is Screen.Profile) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                                        contentDescription = if (lang == "RU") "Нода" else "Node"
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
                    when (currentScreen) {
                        is Screen.Feed -> FeedScreen(viewModel = viewModel, innerPadding = innerPadding)
                        is Screen.Community -> CommunityScreen(viewModel = viewModel, innerPadding = innerPadding)
                        is Screen.Notifications -> NotificationsScreen(viewModel = viewModel, innerPadding = innerPadding)
                        is Screen.Analytics -> AnalyticsScreen(viewModel = viewModel, innerPadding = innerPadding)
                        is Screen.Profile -> ProfileScreen(viewModel = viewModel, innerPadding = innerPadding)
                    }
                }
            }
        }
    }
}
