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
import com.example.ui.theme.*

@Composable
fun CommunityScreen(viewModel: SocialViewModel, innerPadding: PaddingValues) {
    val allUsers by viewModel.allUsers.collectAsState()
    val lang by viewModel.selectedLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val followingIds by viewModel.currentUserFollowingIds.collectAsState()
    
    val communityMembers = allUsers.filter { it.id != "user" }
    
    val isTempVerified = currentUser?.isVerified == true && (currentUser?.verificationExpiry ?: 0) > System.currentTimeMillis()
    val isPermVerified = currentUser?.isVerified == true && currentUser?.verificationExpiry == null
    
    if (!isTempVerified && !isPermVerified) {
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            Text(if (lang == "RU") "Требуется верификация" else "Verification required", color = PureWhite)
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

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Only show verified AI agents in community tab
                    items(communityMembers.filter { it.isVerified && it.isAi }) { user ->
                        val isF = followingIds.contains(user.id)
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray),
                            colors = CardDefaults.cardColors(containerColor = DeepGray)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AvatarComponent(user.avatarUrl, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.username, color = PureWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(user.handle, color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                
                                Button(
                                    onClick = {
                                        if (isF) viewModel.unfollowAgent(user.id)
                                        else viewModel.followAgent(user.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isF) AlertRed else PureWhite)
                                ) {
                                    Text(
                                        text = if (isF) (if (lang == "RU") "ОТПИСАТЬСЯ" else "UNFOLLOW") else (if (lang == "RU") "ПОДПИСАТЬСЯ" else "FOLLOW"),
                                        color = if (isF) PureWhite else PureBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun AvatarComponent(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = "Avatar",
        modifier = modifier.clip(CircleShape).border(1.dp, BorderGray, CircleShape),
        contentScale = ContentScale.Crop
    )
}
