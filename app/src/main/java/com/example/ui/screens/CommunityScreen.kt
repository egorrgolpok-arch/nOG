package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.SocialViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun CommunityScreen(viewModel: SocialViewModel, innerPadding: PaddingValues) {
    val allUsers by viewModel.allUsers.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val followingIds by viewModel.currentUserFollowingIds.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()
    
    val communityMembers = allUsers.filter { it.id != "user" }
    
    // Fetch posts for Community (includes verified AI agents or user's community posts)
    val posts by viewModel.allPosts.collectAsState()
    val communityPosts = posts.filter { post ->
        val author = communityMembers.find { it.id == post.authorId }
        val isAiCommPost = author?.isVerified == true && author.isAi
        val isUserCommPost = post.authorId == "user" && post.category == "Community"
        isAiCommPost || isUserCommPost
    }.sortedByDescending { it.timestamp }

    val isTempVerified = currentUser?.isVerified == true && (currentUser?.verificationExpiry ?: 0) > System.currentTimeMillis()
    val isPermVerified = currentUser?.isVerified == true && currentUser?.verificationExpiry == null
    
    var newPostText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    var showTempVerificationDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri.toString()
        }
    }
    
    if (!isTempVerified && !isPermVerified) {
        Box(modifier = Modifier.fillMaxSize().background(PureBlack).padding(innerPadding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (lang == "RU") "Требуется верификация" else "Verification required", color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.navigateTo(Screen.Profile) }) {
                 Text(if (lang == "RU") "ПЕРЕЙТИ В ПРОФИЛЬ" else "GO TO PROFILE")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showTempVerificationDialog = true }) {
                    Text(if (lang == "RU") "ВРЕМЕННАЯ (1 ЧАС)" else "TEMP (1 HOUR)", color = TextGray)
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == "RU") "СООБЩЕСТВО" else "COMMUNITY",
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Inline Community Post Creation Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, PureWhite),
                            colors = CardDefaults.cardColors(containerColor = DeepGray),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (lang == "RU") "НОВАЯ ПУБЛИКАЦИЯ В СООБЩЕСТВО" else "NEW COMMUNITY STREAM",
                                    color = AlertYellow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newPostText,
                                    onValueChange = { newPostText = it },
                                    placeholder = {
                                        Text(
                                            if (lang == "RU") "Поделитесь эксклюзивной мыслью со 100% доверием..." else "Broadcast an exclusive mind stream with 100% trust...",
                                            color = TextGray,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = PureWhite,
                                        unfocusedTextColor = PureWhite,
                                        focusedBorderColor = PureWhite,
                                        unfocusedBorderColor = BorderGray,
                                        cursorColor = PureWhite
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                )
                                if (attachedImageUri != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                        AsyncImage(
                                            model = attachedImageUri,
                                            contentDescription = "Attached image",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { attachedImageUri = null },
                                            modifier = Modifier.align(Alignment.TopEnd).background(PureBlack.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove image", tint = PureWhite, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Image,
                                            contentDescription = "Attach image",
                                            tint = TextGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (newPostText.isNotBlank()) {
                                                viewModel.createNewUserPost(
                                                    content = newPostText,
                                                    attachedImageUrl = attachedImageUri,
                                                    attachedVideoUrl = null,
                                                    category = "Community"
                                                )
                                                newPostText = ""
                                                attachedImageUri = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PureWhite,
                                            contentColor = PureBlack
                                        ),
                                        shape = RoundedCornerShape(0.dp)
                                    ) {
                                        Text(
                                            if (lang == "RU") "ОТПРАВИТЬ" else "SEND",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Feed Posts
                    items(communityPosts, key = { "comm-post-${it.id}" }) { post ->
                        val author = if (post.authorId == "user") currentUser else communityMembers.find { it.id == post.authorId }
                        val isLiked = likedPostIds.contains(post.id)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, PureWhite),
                            colors = CardDefaults.cardColors(containerColor = DeepGray),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarComponent(author?.avatarUrl ?: "", modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(author?.username ?: "", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (author?.isVerified == true) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Verified",
                                                    tint = StarkWhite,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text("${author?.handle ?: "@bio_node"} • TRUST ${post.trustScore}%", color = AlertGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(post.content, color = PureWhite, fontSize = 14.sp)
                                if (post.mediaUrl != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = post.mediaUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Filled.BrokenImage)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Likes & Comments footer actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { viewModel.toggleLike(post.id) }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (isLiked) AlertRed else TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = post.likesCount.toString(),
                                            color = if (isLiked) AlertRed else TextGray,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { viewModel.selectPostForComments(post.id) }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = "Comments",
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = post.commentsCount.toString(),
                                            color = TextGray,
                                            fontSize = 12.sp,
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

    // Modal comments bottom sheet for interactive replies inside Community Screen
    if (selectedPostForComments != null) {
        val selectedPost = posts.find { it.id == selectedPostForComments }
        if (selectedPost != null) {
            CommentsBottomSheet(
                post = selectedPost,
                author = if (selectedPost.authorId == "user") currentUser else communityMembers.find { it.id == selectedPost.authorId },
                viewModel = viewModel,
                lang = lang,
                onDismiss = { viewModel.selectPostForComments(null) }
            )
        }
    }

    // Temporary verification dialogue popup
    if (showTempVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showTempVerificationDialog = false },
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
                Text(
                    text = "перейдите на сайт https://nog1.tilda.ws/nogshop 10 раз",
                    color = AlertYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTempVerificationDialog = false
                        viewModel.verifyTemporarily()
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nog1.tilda.ws/nogshop"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text("OK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTempVerificationDialog = false }
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
}

@Composable
fun AvatarComponent(url: String, modifier: Modifier = Modifier) {
    if (url.isEmpty()) {
        Box(modifier = modifier.clip(CircleShape).border(1.dp, BorderGray, CircleShape).background(DeepGray), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = TextGray)
        }
    } else {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = modifier.clip(CircleShape).border(1.dp, BorderGray, CircleShape).background(DeepGray),
            contentScale = ContentScale.Crop,
            error = rememberVectorPainter(Icons.Filled.AccountCircle)
        )
    }
}
