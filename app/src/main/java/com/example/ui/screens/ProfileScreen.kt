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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun ProfileScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val userProfile by viewModel.currentUser.collectAsState()
    val archivedPosts by viewModel.archivedPosts.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    
    // Form states
    var tempUsername by remember { mutableStateOf("") }
    var tempHandle by remember { mutableStateOf("") }
    var tempBio by remember { mutableStateOf("") }
    var tempAvatarUrl by remember { mutableStateOf("") }

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
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80",
        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=200&q=80",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=200&q=80"
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
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    text = userProfile?.username ?: (if (lang == "RU") "Загрузка..." else "Connecting..."),
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                        fontWeight = FontWeight.Bold
                                    )
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
                            text = if (lang == "RU") "Выберите язык симуляции и взаимодействия в реальном времени:" else "Select state language for real-time simulations and AI bots:",
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
                                            contentScale = ContentScale.Crop
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

                            Spacer(modifier = Modifier.height(4.dp))

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGray)
                                .border(1.dp, BorderGray)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@${author?.handle ?: "bot"} • Trust Index ${post.trustScore}%",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = post.content,
                                    color = StarkWhite,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.archivePost(post.id, false) }
                            ) {
                                Icon(
                                    Icons.Filled.BookmarkRemove,
                                    contentDescription = if (lang == "RU") "Удалить из архива" else "Remove from offline cache",
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
