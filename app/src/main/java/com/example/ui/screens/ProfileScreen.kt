package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val userProfile by viewModel.currentUser.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val myPosts = allPosts.filter { it.authorId == "user" }
    val archivedPosts by viewModel.archivedPosts.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()
    val verificationClicks by viewModel.verificationClicks.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val followingIds by viewModel.currentUserFollowingIds.collectAsState()
    val isSilentMode by viewModel.isSilentMode.collectAsState()
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }

    var isEditing by remember { mutableStateOf(false) }
    // Add Edit State
    var postToEdit by remember { mutableStateOf<com.example.data.PostEntity?>(null) }
    var editContent by remember { mutableStateOf("") }
    
    // Form states
    var tempUsername by remember { mutableStateOf("") }
    var tempHandle by remember { mutableStateOf("") }
    var tempBio by remember { mutableStateOf("") }
    var tempAvatarUrl by remember { mutableStateOf("") }
    var showVerificationSheet by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var showTempVerificationDialog by remember { mutableStateOf(false) }
    var showFollowingList by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Synchronize form values on loaded
    LaunchedEffect(userProfile) {
        userProfile?.let {
            tempUsername = it.username
            tempHandle = it.handle
            tempBio = it.bio
            tempAvatarUrl = it.avatarUrl
        }
    }

    val sampleAvatars = listOf(
        "https://i.pravatar.cc/150?img=11",
        "https://i.pravatar.cc/150?img=44",
        "https://i.pravatar.cc/150?img=33",
        "https://i.pravatar.cc/150?img=68"
    )

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
            
            // --- Header Title ---
            Text(
                text = if (lang == "RU") "ТЕРМИНАЛ ПРОФИЛЯ" else "PROFILE CONTROL TERMINAL",
                color = PureWhite,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // --- Scrollable info container ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // 1. Core Profile Details & Visual Form
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepGray)
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = userProfile?.avatarUrl ?: sampleAvatars.first(),
                                contentDescription = userProfile?.username,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PureWhite, CircleShape),
                                contentScale = ContentScale.Crop,
                                error = rememberVectorPainter(Icons.Filled.AccountCircle),
                                placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = userProfile?.username ?: (if (lang == "RU") "Загрузка..." else "Connecting..."),
                                        color = PureWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (userProfile?.isVerified == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Verified",
                                            tint = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = userProfile?.handle ?: "@handle",
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Followers and following persistence indicator
                                Row {
                                    Text(
                                        text = "${userProfile?.followersCount ?: 0} " + (if (lang == "RU") "подписчиков" else "followers"),
                                        color = StarkWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${userProfile?.followingCount ?: 0} " + (if (lang == "RU") "подписки" else "following"),
                                        color = StarkWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showFollowingList = true }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.clearFollowers() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (lang == "RU") "ОЧИСТИТЬ ПОТОК" else "PURGE FOLLOWERS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextGray)
                                    }
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.unfollowAll() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (lang == "RU") "СБРОС ПОДПИСОК" else "RESET FOLLOWING", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextGray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = userProfile?.bio ?: "",
                            color = StarkWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isEditing) {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = if (lang == "RU") "Редактировать" else "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (lang == "RU") "РЕДАКТИРОВАТЬ ПРОФИЛЬ" else "EDIT PROFILE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            if (userProfile?.isVerified != true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showVerificationSheet = !showVerificationSheet },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite)
                                    ) {
                                        Icon(Icons.Filled.Verified, contentDescription = "Verify", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == "RU") "ПЕРМАНЕНТНАЯ" else "PERMANENT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showTempVerificationDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepGray, contentColor = AlertYellow),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertYellow)
                                    ) {
                                        Icon(Icons.Filled.Timer, contentDescription = "Temp", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == "RU") "ВРЕМЕННАЯ (1ч)" else "TEMP (1h)", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Show timer if temporary
                                userProfile?.verificationExpiry?.let { expiry ->
                                    var timeLeft by remember { mutableStateOf((expiry - System.currentTimeMillis()) / 1000) }
                                    
                                    LaunchedEffect(expiry) {
                                        while (timeLeft > 0) {
                                            delay(1000)
                                            timeLeft = (expiry - System.currentTimeMillis()) / 1000
                                        }
                                    }

                                    if (timeLeft > 0) {
                                        val mins = timeLeft / 60
                                        val secs = timeLeft % 60
                                        Text(
                                            text = if (lang == "RU") "ВРЕМЕННАЯ ВЕРИФИКАЦИЯ: ${mins}м ${secs}с" else "TEMP VERIFICATION: ${mins}m ${secs}s",
                                            color = AlertYellow,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth().background(DeepGray.copy(alpha = 0.5f)).padding(8.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            
                            if (showVerificationSheet) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DeepGray)
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "Посетите nOG shop и по возможности купите хотя бы 1 вещ" else "Visit nOG shop and if possible buy at least 1 item",
                                        color = StarkWhite,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "https://nog1.tilda.ws/nogshop",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nog1.tilda.ws/nogshop"))
                                                context.startActivity(intent)
                                            }
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = verificationCode,
                                        onValueChange = { verificationCode = it },
                                        label = { Text(if (lang == "RU") "Код заказа" else "Order Code") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = PureWhite,
                                            unfocusedBorderColor = PureWhite
                                        )
                                    )
                                    Button(onClick = {
                                        viewModel.verifyPermanently(verificationCode)
                                        showVerificationSheet = false
                                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Text(if (lang == "RU") "ВЕРИФИЦИРОВАТЬСЯ НАВСЕГДА" else "VERIFY PERMANENTLY")
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.5 system language configuration panel
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepGray)
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (lang == "RU") "КОНФИГУРАЦИЯ СЕТИ И РАЗУМА" else "NETWORK & BRAIN CONFIGURATION",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == "RU") "Выберите язык симуляции и взаимодействия в реальном времени:" else "Select state language for real-time simulations and network bots:",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.changeLanguage("RU") },
                                modifier = Modifier.weight(1f).border(1.dp, if (lang == "RU") PureWhite else Color.Transparent),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lang == "RU") PureWhite else CardGray,
                                    contentColor = if (lang == "RU") PureBlack else StarkWhite
                                ),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text("РУССКИЙ (RU)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.changeLanguage("EN") },
                                modifier = Modifier.weight(1f).border(1.dp, if (lang == "EN") PureWhite else Color.Transparent),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lang == "EN") PureWhite else CardGray,
                                    contentColor = if (lang == "EN") PureBlack else StarkWhite
                                ),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text("ENGLISH (EN)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == "RU") "Режим тишины" else "Silent Mode",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (lang == "RU") "Без вибраций и уведомлений" else "Mute reactions and alerts",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isSilentMode,
                                onCheckedChange = { viewModel.toggleSilentMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureBlack,
                                    checkedTrackColor = PureWhite,
                                    uncheckedThumbColor = TextGray,
                                    uncheckedTrackColor = DeepGray,
                                    checkedBorderColor = PureWhite,
                                    uncheckedBorderColor = BorderGray
                                )
                            )
                        }
                    }
                }

                // 2. Editing Form Controls
                if (isEditing) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGray)
                                .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (lang == "RU") "ПАРАМЕТРЫ ПРОФИЛЯ" else "PROFILE DIRECTIVES CONFIG",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = tempUsername,
                                onValueChange = { tempUsername = it },
                                label = { Text(if (lang == "RU") "Имя пользователя" else "Username") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = StarkWhite,
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp)
                            )

                            OutlinedTextField(
                                value = tempHandle,
                                onValueChange = { tempHandle = it },
                                label = { Text(if (lang == "RU") "Никнейм (@)" else "Handle (@)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = StarkWhite,
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp)
                            )

                            OutlinedTextField(
                                value = tempBio,
                                onValueChange = { tempBio = it },
                                label = { Text(if (lang == "RU") "Описание" else "About Bio") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = StarkWhite,
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp)
                            )

                            // Pick Avatar preset or insert URL
                            Text(
                                text = if (lang == "RU") "ВЫБЕРИТЕ АВАТАРКУ ИЛИ КРЕМНИЕВЫЙ СКИН:" else "SELECT PROFILE SKIN:",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                sampleAvatars.forEach { url ->
                                    val isSelected = tempAvatarUrl == url
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) PureWhite else BorderGray, CircleShape)
                                            .clickable { tempAvatarUrl = url }
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Avatar preset",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            error = rememberVectorPainter(Icons.Filled.AccountCircle),
                                            placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
                                        )
                                    }
                                }
                            }

                            // Or custom URL field
                            OutlinedTextField(
                                value = tempAvatarUrl,
                                onValueChange = { tempAvatarUrl = it },
                                label = { Text(if (lang == "RU") "Собственный URL аватарки" else "Custom skin location URL") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = StarkWhite,
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            )
                            
                            val photoPickerLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.PickVisualMedia()
                            ) { uri ->
                                if (uri != null) {
                                    try {
                                        context.contentResolver.takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("Profile", "Persistable permission error", e)
                                    }
                                    tempAvatarUrl = uri.toString()
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Button(
                                onClick = { 
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Filled.AccountBox, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (lang == "RU") "ВЫБРАТЬ ИЗ ГАЛЕРЕИ" else "CHOOSE FROM GALLERY", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.editUserProfile(tempUsername, tempHandle, tempBio, tempAvatarUrl)
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(if (lang == "RU") "СОХРАНИТЬ" else "SAVE LOGS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = { isEditing = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (lang == "RU") "ОТМЕНА" else "CANCEL", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // 3. PRIVATE ARCHIVE: "АРИХИВ ПУБЛИКАЦИЙ"
                item {
                    Text(
                        text = if (lang == "RU") "АРХИВНЫЕ ПУБЛИКАЦИИ (${archivedPosts.size})" else "SAVED ARCHIVE STREAMS (${archivedPosts.size})",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                if (archivedPosts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.BookmarkBorder, contentDescription = if (lang == "RU") "Архив пуст" else "Archive empty", tint = BorderGray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == "RU") {
                                    "Архив историй пуст.\nВы можете долго удерживать или кликнуть на закладку любого поста в ленте, чтобы сохранить его сюда."
                                } else {
                                    "Your offline store is empty.\nClick the bookmark emblem on any stream in the feed to save key nodes here."
                                },
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(archivedPosts, key = { "archive-${it.id}" }) { post ->
                        val author = users.find { it.id == post.authorId }
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            PostItem(
                                post = post,
                                author = author,
                                lang = lang,
                                isLiked = likedPostIds.contains(post.id),
                                isFollowing = followingIds.contains(author?.id ?: ""),
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onCommentClick = { viewModel.selectPostForComments(post.id) },
                                onMediaClick = { zoomImageUrl = it },
                                onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                                onFollowToggle = {
                                    author?.id?.let { id ->
                                        if (followingIds.contains(id)) viewModel.unfollowAgent(id)
                                        else viewModel.followAgent(id)
                                    }
                                },
                                onDeleteClick = if (post.authorId == "user") { { viewModel.deletePost(post.id) } } else null
                            )
                        }
                    }
                }
                
                // 4. MY POSTS
                item {
                    Text(
                        text = if (lang == "RU") "МОИ ПУБЛИКАЦИИ (${myPosts.size})" else "MY POSTS (${myPosts.size})",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(myPosts, key = { "mypost-${it.id}" }) { post ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        PostItem(
                            post = post,
                            author = userProfile,
                            lang = lang,
                            isLiked = likedPostIds.contains(post.id),
                            isFollowing = false,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { viewModel.selectPostForComments(post.id) },
                            onMediaClick = { zoomImageUrl = it },
                            onArchiveToggle = { viewModel.archivePost(post.id, !post.isArchived) },
                            onFollowToggle = {},
                            onDeleteClick = { viewModel.deletePost(post.id) }
                        )
                    }
                }
            }
        }
        
        
        if (showFollowingList) {
            ModalBottomSheet(
                onDismissRequest = { showFollowingList = false },
                containerColor = PureBlack,
                contentColor = PureWhite
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = if (lang == "RU") "ПОДПИСКИ" else "FOLLOWING",
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(followingIds.toList()) { followingId ->
                            val followedUser = users.find { it.id == followingId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = followedUser?.avatarUrl ?: "",
                                    contentDescription = followedUser?.username,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = followedUser?.username ?: "Unknown", color = PureWhite, fontSize = 14.sp)
                                    Text(text = "@${followedUser?.handle ?: ""}", color = TextGray, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.unfollowAgent(followingId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = PureWhite)
                                ) {
                                    Text(if (lang == "RU") "ОТПИСАТЬСЯ" else "UNFOLLOW")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTempVerificationDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showTempVerificationDialog = false 
                    viewModel.resetVerificationClicks()
                },
                title = {
                    Text(
                        text = if (lang == "RU") "Требование верификации" else "Verification Requirement",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "перейдите на сайт https://nog1.tilda.ws/nogshop 10 раз",
                            color = AlertYellow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == "RU") "Прогресс переходов: $verificationClicks / 10" else "Visit progress: $verificationClicks / 10",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.incrementVerificationClicks()
                            if (verificationClicks >= 9) {
                                showTempVerificationDialog = false
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nog1.tilda.ws/nogshop"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = if (lang == "RU") "ПЕРЕЙТИ (${verificationClicks}/10)" else "VISIT (${verificationClicks}/10)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showTempVerificationDialog = false 
                            viewModel.resetVerificationClicks()
                        }
                    ) {
                        Text(
                            text = if (lang == "RU") "ОТМЕНА" else "CANCEL",
                            color = TextGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                containerColor = DeepGray,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.border(1.dp, PureWhite)
            )
        }

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

        // --- Comments Drawer Bottom-Sheet ---
        if (selectedPostForComments != null) {
            val selectedPost = allPosts.find { it.id == selectedPostForComments }
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
