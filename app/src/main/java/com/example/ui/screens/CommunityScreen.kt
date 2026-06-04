package com.example.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
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
    val verificationClicks by viewModel.verificationClicks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val followingIds by viewModel.currentUserFollowingIds.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val selectedPostForComments by viewModel.activePostIdForComments.collectAsState()
    
    val communityMembers = allUsers.filter { it.id != "user" }
    
    // Fetch posts for Community (includes verified AI agents or user's community posts)
    val posts by viewModel.allPosts.collectAsState()
    val communityPosts = posts.filter { post ->
        val author = communityMembers.find { it.id == post.authorId }
        val isAiCommPost = (author?.isAi == true && post.trustScore >= 75) || post.category == "Community" || post.category == "Сообщество"
        val isUserCommPost = post.authorId == "user" && (post.category == "Community" || post.category == "Сообщество")
        isAiCommPost || isUserCommPost
    }.sortedByDescending { it.timestamp }

    val isTempVerified = currentUser?.isVerified == true && (currentUser?.verificationExpiry ?: 0) > System.currentTimeMillis()
    val isPermVerified = currentUser?.isVerified == true && currentUser?.verificationExpiry == null
    
    var newPostText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    var showTempVerificationDialog by remember { mutableStateOf(false) }
    var showBlackJackGame by remember { mutableStateOf(false) }
    var showChessGame by remember { mutableStateOf(false) }
    var showPokerGame by remember { mutableStateOf(false) }
    var showMatch3Game by remember { mutableStateOf(false) }
    var showFlappyBotGame by remember { mutableStateOf(false) }
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
                    Text(if (lang == "RU") "ВРЕМЕННАЯ (30 МИНУТ)" else "TEMP (30 MIN)", color = TextGray)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flappy Bot Game
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureBlack)
                                .border(1.dp, AlertRed, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.vibrate(25)
                                    showFlappyBotGame = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsEsports,
                                contentDescription = "Flappy Bot Game",
                                tint = AlertRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Blackjack (Upgraded to card suit representation to prevent duplicate icons)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureBlack)
                                .border(1.dp, AlertYellow, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.vibrate(25)
                                    showBlackJackGame = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♠♥", color = AlertYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        // Chess Game
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureBlack)
                                .border(1.dp, AlertGreen, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.vibrate(25)
                                    showChessGame = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♟", color = AlertGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        // Poker Game
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureBlack)
                                .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.vibrate(25)
                                    showPokerGame = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♦♣", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        // Match-3 Stack Game
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureBlack)
                                .border(1.dp, StarkWhite, RoundedCornerShape(4.dp))
                                .clickable {
                                    viewModel.vibrate(25)
                                    showMatch3Game = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⬡⬡", color = StarkWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
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
                                            if (author?.isVerified == true || author?.isAi == true) {
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
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://nog1.tilda.ws/nogshop"))
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

    if (showBlackJackGame) {
        BlackjackDialog(onDismiss = { showBlackJackGame = false }, lang = lang, viewModel = viewModel)
    }

    if (showChessGame) {
        ChessDialog(onDismiss = { showChessGame = false }, lang = lang, viewModel = viewModel)
    }

    if (showPokerGame) {
        PokerDialog(onDismiss = { showPokerGame = false }, lang = lang, viewModel = viewModel)
    }

    if (showMatch3Game) {
        Match3Dialog(onDismiss = { showMatch3Game = false }, lang = lang, viewModel = viewModel)
    }

    if (showFlappyBotGame) {
        FlappyBotGameDialog(
            lang = lang,
            viewModel = viewModel,
            users = allUsers,
            currentUser = currentUser,
            onDismiss = { showFlappyBotGame = false }
        )
    }
}

@Composable
fun BlackjackDialog(onDismiss: () -> Unit, lang: String, viewModel: SocialViewModel) {
    // Game cards structure
    data class BlackjackCard(val rank: String, val suit: String, val value: Int)

    fun createDeck(): List<BlackjackCard> {
        val suits = listOf("♠", "♥", "♦", "♣")
        val ranks = listOf(
            Pair("2", 2), Pair("3", 3), Pair("4", 4), Pair("5", 5), Pair("6", 6), Pair("7", 7),
            Pair("8", 8), Pair("9", 9), Pair("10", 10), Pair("J", 10), Pair("Q", 10), Pair("K", 10), Pair("A", 11)
        )
        val deck = mutableListOf<BlackjackCard>()
        for (suit in suits) {
            for (rank in ranks) {
                deck.add(BlackjackCard(rank.first, suit, rank.second))
            }
        }
        deck.shuffle()
        return deck
    }

    fun calculateScore(hand: List<BlackjackCard>): Int {
        var score = hand.sumOf { it.value }
        var aces = hand.count { it.rank == "A" }
        while (score > 21 && aces > 0) {
            score -= 10
            aces -= 1
        }
        return score
    }

    var wallet by remember { mutableStateOf(1000) }
    var currentBet by remember { mutableStateOf(100) }
    var playerHand by remember { mutableStateOf(emptyList<BlackjackCard>()) }
    var dealerHand by remember { mutableStateOf(emptyList<BlackjackCard>()) }
    var deck by remember { mutableStateOf(emptyList<BlackjackCard>()) }
    var gamePhase by remember { mutableStateOf("BETTING") } // BETTING, PLAYER_TURN, DEALER_TURN, ENDED
    var gameOutcomeText by remember { mutableStateOf("") }

    fun processDealerTurn(activeDeck: List<BlackjackCard>, playerH: List<BlackjackCard>, bet: Int) {
        var localDealerHand = dealerHand.toMutableList()
        var localDeck = activeDeck.toMutableList()

        while (calculateScore(localDealerHand) < 17 && localDeck.isNotEmpty()) {
            localDealerHand.add(localDeck.removeAt(0))
            viewModel.vibrate(35)
        }

        dealerHand = localDealerHand
        deck = localDeck

        val playerSc = calculateScore(playerH)
        val dealerSc = calculateScore(localDealerHand)

        if (dealerSc > 21) {
            gameOutcomeText = if (lang == "RU") "ДИЛЕР СГОРЕЛ! ВЫ ПОБЕДИЛИ" else "DEALER BUSTED! YOU WIN"
            wallet += bet * 2
            viewModel.vibrate(150)
        } else if (playerSc > dealerSc) {
            gameOutcomeText = if (lang == "RU") "ВЫ ВЫИГРАЛИ!" else "YOU WIN!"
            wallet += bet * 2
            viewModel.vibrate(150)
        } else if (playerSc < dealerSc) {
            gameOutcomeText = if (lang == "RU") "ДИЛЕР ВЫИГРАЛ!" else "DEALER WINS!"
            viewModel.vibrate(300)
        } else {
            gameOutcomeText = if (lang == "RU") "НИЧЬЯ (ПУШ)" else "PUSH (TIE)"
            wallet += bet
            viewModel.vibrate(80)
        }
        gamePhase = "ENDED"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Block
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "RU") "УЗЕЛ СИНХРОНИЗАЦИИ" else "SYNCHRONIZATION NODE",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "RU") "БЛЭКДЖЕК 21" else "BLACKJACK 21",
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.vibrate(30)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(20.dp))
                        }
                    }

                    HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 16.dp))
                }

                // Play Zone scroll/layout container
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Stats Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (lang == "RU") "БАЛАНС НОДЫ" else "NODE BALANCE", color = TextGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("$$wallet", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (lang == "RU") "ТЕКУЩИЙ ПАКЕТ" else "ACTIVE BET", color = TextGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("$$currentBet", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // DEALER AREA
                    Text(
                        text = if (lang == "RU") "[ ДИЛЕР nOG ]" else "[ DEALER nOG ]",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dealerHand.isEmpty()) {
                            Text(
                                text = if (lang == "RU") "Ожидание пакета данных..." else "Awaiting chip injection...",
                                color = TextGray.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        } else {
                            dealerHand.forEachIndexed { index, card ->
                                val isHidden = (index == 1 && gamePhase == "PLAYER_TURN")
                                Card(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .width(62.dp)
                                        .height(90.dp)
                                        .border(2.dp, if (isHidden) PureWhite.copy(alpha = 0.5f) else BorderGray, RoundedCornerShape(6.dp)),
                                    colors = CardDefaults.cardColors(containerColor = if (isHidden) PureBlack else DeepGray)
                                ) {
                                    if (isHidden) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("CRYPT", color = TextGray, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    } else {
                                        val isRedSuit = card.suit == "♥" || card.suit == "♦"
                                        val paintColor = if (isRedSuit) TextGray else PureWhite
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(6.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(card.rank, color = paintColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                            Text(card.suit, color = paintColor, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontFamily = FontFamily.Monospace)
                                            Text(card.rank, color = paintColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End), fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (dealerHand.isNotEmpty()) {
                        val dScoreVisible = if (gamePhase == "PLAYER_TURN") {
                            val firstCardVal = dealerHand.firstOrNull()?.value ?: 0
                            if (dealerHand.firstOrNull()?.rank == "A") 11 else firstCardVal
                        } else {
                            calculateScore(dealerHand)
                        }
                        Text(
                            text = "Score: " + (if (gamePhase == "PLAYER_TURN") "$dScoreVisible + ?" else "$dScoreVisible"),
                            color = TextGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // PLAYER AREA
                    Text(
                        text = if (lang == "RU") "[ ОРГАНИЧЕСКАЯ НОДА ]" else "[ ORGANIC NODE ]",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playerHand.isEmpty()) {
                            Text("-", color = TextGray.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                        } else {
                            playerHand.forEach { card ->
                                Card(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .width(62.dp)
                                        .height(90.dp)
                                        .border(2.dp, PureWhite, RoundedCornerShape(6.dp)),
                                    colors = CardDefaults.cardColors(containerColor = DeepGray)
                                ) {
                                    val isRedSuit = card.suit == "♥" || card.suit == "♦"
                                    val paintColor = if (isRedSuit) TextGray else PureWhite
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(card.rank, color = paintColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                        Text(card.suit, color = paintColor, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontFamily = FontFamily.Monospace)
                                        Text(card.rank, color = paintColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.End), fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    if (playerHand.isNotEmpty()) {
                        Text(
                            text = "Score: ${calculateScore(playerHand)}",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Game Outcome Bar
                    if (gamePhase == "ENDED") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGray)
                                .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gameOutcomeText,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Controls Block at Bottom (Raised up for screens/system bars ergonomics)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 36.dp)
                ) {
                    when (gamePhase) {
                        "BETTING" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.vibrate(25)
                                        if (currentBet >= 50) currentBet -= 50
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGray),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                        .height(38.dp)
                                ) {
                                    Text("-50", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.vibrate(25)
                                        if (currentBet + 50 <= wallet) currentBet += 50
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGray),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                        .height(38.dp)
                                ) {
                                    Text("+50", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.vibrate(40)
                                        currentBet = if (wallet > 0) wallet else 100
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepGray),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp)
                                        .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .height(38.dp)
                                ) {
                                    Text("ALL-IN", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (wallet <= 0 && playerHand.isEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.vibrate(60)
                                        wallet = 1000
                                        currentBet = 100
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "ПОЛУЧИТЬ КРЕДИТ $1000" else "RECHARGE $1000 CHIPS",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.vibrate(45)
                                        val checkedBet = if (currentBet > wallet) wallet else currentBet
                                        currentBet = checkedBet
                                        wallet -= checkedBet
                                        val newDeck = createDeck().toMutableList()
                                        val pHand = mutableListOf(newDeck.removeAt(0), newDeck.removeAt(0))
                                        val dHand = mutableListOf(newDeck.removeAt(0), newDeck.removeAt(0))

                                        playerHand = pHand
                                        dealerHand = dHand
                                        deck = newDeck

                                        val pScore = calculateScore(pHand)
                                        if (pScore == 21) {
                                            val dScore = calculateScore(dHand)
                                            if (dScore == 21) {
                                                gameOutcomeText = if (lang == "RU") "НИЧЬЯ (ПУШ)" else "PUSH (TIE)"
                                                wallet += checkedBet
                                                viewModel.vibrate(80)
                                            } else {
                                                gameOutcomeText = if (lang == "RU") "БЛЭКДЖЕК! ПОБЕДА 3:2" else "BLACKJACK! WIN 3:2"
                                                wallet += (checkedBet * 2.5).toInt()
                                                viewModel.vibrate(180)
                                            }
                                            gamePhase = "ENDED"
                                        } else {
                                            gamePhase = "PLAYER_TURN"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "СДАТЬ КАРТЫ" else "DEAL HAND",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        "PLAYER_TURN" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.vibrate(35)
                                        val workingPlayer = playerHand.toMutableList()
                                        val workingDeck = deck.toMutableList()
                                        workingPlayer.add(workingDeck.removeAt(0))
                                        playerHand = workingPlayer
                                        deck = workingDeck

                                        if (calculateScore(workingPlayer) > 21) {
                                            gameOutcomeText = if (lang == "RU") "СГОРЕЛИ! ПЕРЕБОР" else "BUSTED! OVER 21"
                                            gamePhase = "ENDED"
                                            viewModel.vibrate(300)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                        .height(46.dp)
                                ) {
                                    Text(if (lang == "RU") "ЕЩЁ" else "HIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }

                                val canDouble = playerHand.size == 2 && wallet >= currentBet
                                Button(
                                    onClick = {
                                        if (canDouble) {
                                            viewModel.vibrate(50)
                                            wallet -= currentBet
                                            val doubleBet = currentBet * 2
                                            val workingPlayer = playerHand.toMutableList()
                                            val workingDeck = deck.toMutableList()
                                            workingPlayer.add(workingDeck.removeAt(0))
                                            playerHand = workingPlayer
                                            deck = workingDeck

                                            if (calculateScore(workingPlayer) > 21) {
                                                gameOutcomeText = if (lang == "RU") "СГОРЕЛИ! ПЕРЕБОР" else "BUSTED! OVER 21"
                                                gamePhase = "ENDED"
                                                viewModel.vibrate(300)
                                            } else {
                                                processDealerTurn(workingDeck, workingPlayer, doubleBet)
                                            }
                                        }
                                    },
                                    enabled = canDouble,
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(horizontal = 4.dp)
                                        .height(46.dp)
                                ) {
                                    Text(if (lang == "RU") "УДВОИТЬ" else "DOUBLE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.vibrate(35)
                                        processDealerTurn(deck, playerHand, currentBet)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp)
                                        .height(46.dp)
                                ) {
                                    Text(if (lang == "RU") "ХВАТИТ" else "STAND", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "ENDED" -> {
                            Button(
                                onClick = {
                                    viewModel.vibrate(40)
                                    playerHand = emptyList()
                                    dealerHand = emptyList()
                                    gameOutcomeText = ""
                                    if (currentBet > wallet) {
                                        currentBet = if (wallet > 0) wallet else 100
                                    }
                                    gamePhase = "BETTING"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ИГРАТЬ ЕЩЕ" else "PLAY AGAIN",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ChessPieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }
data class ChessPiece(val type: ChessPieceType, val isWhite: Boolean)

@Composable
fun ChessDialog(onDismiss: () -> Unit, lang: String, viewModel: SocialViewModel) {
    val board = remember { mutableStateListOf<ChessPiece?>() }
    
    fun resetBoard() {
        board.clear()
        val temp = Array<ChessPiece?>(64) { null }
        // Black (nOG AI Node)
        temp[0] = ChessPiece(ChessPieceType.ROOK, false)
        temp[1] = ChessPiece(ChessPieceType.KNIGHT, false)
        temp[2] = ChessPiece(ChessPieceType.BISHOP, false)
        temp[3] = ChessPiece(ChessPieceType.QUEEN, false)
        temp[4] = ChessPiece(ChessPieceType.KING, false)
        temp[5] = ChessPiece(ChessPieceType.BISHOP, false)
        temp[6] = ChessPiece(ChessPieceType.KNIGHT, false)
        temp[7] = ChessPiece(ChessPieceType.ROOK, false)
        for (i in 8..15) {
            temp[i] = ChessPiece(ChessPieceType.PAWN, false)
        }
        
        // White (Organic Player Node)
        for (i in 48..55) {
            temp[i] = ChessPiece(ChessPieceType.PAWN, true)
        }
        temp[56] = ChessPiece(ChessPieceType.ROOK, true)
        temp[57] = ChessPiece(ChessPieceType.KNIGHT, true)
        temp[58] = ChessPiece(ChessPieceType.BISHOP, true)
        temp[59] = ChessPiece(ChessPieceType.QUEEN, true)
        temp[60] = ChessPiece(ChessPieceType.KING, true)
        temp[61] = ChessPiece(ChessPieceType.BISHOP, true)
        temp[62] = ChessPiece(ChessPieceType.KNIGHT, true)
        temp[63] = ChessPiece(ChessPieceType.ROOK, true)
        
        board.addAll(temp)
    }
    
    LaunchedEffect(Unit) {
        resetBoard()
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var difficulty by remember { mutableStateOf("MEDIUM") } // EASY, MEDIUM, OVERLORD
    var gameOutcome by remember { mutableStateOf("") } // "", "WIN", "DEFEAT", "DRAW"
    var selectedPieceRuleText by remember { mutableStateOf("") }
    var selectedPieceName by remember { mutableStateOf("") }
    
    fun getValidMovesForIndex(index: Int): List<Int> {
        val piece = board.getOrNull(index) ?: return emptyList()
        val isWhite = piece.isWhite
        val moves = mutableListOf<Int>()
        val r = index / 8
        val c = index % 8

        when (piece.type) {
            ChessPieceType.PAWN -> {
                val dir = if (isWhite) -1 else 1
                val f1r = r + dir
                if (f1r in 0..7) {
                    val f1idx = f1r * 8 + c
                    if (board.getOrNull(f1idx) == null) {
                        moves.add(f1idx)
                        val startRow = if (isWhite) 6 else 1
                        if (r == startRow) {
                            val f2r = r + 2 * dir
                            val f2idx = f2r * 8 + c
                            if (board.getOrNull(f2idx) == null) {
                                moves.add(f2idx)
                            }
                        }
                    }
                }
                for (dc in listOf(-1, 1)) {
                    val tr = r + dir
                    val tc = c + dc
                    if (tr in 0..7 && tc in 0..7) {
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece != null && targetPiece.isWhite != isWhite) {
                            moves.add(targetIdx)
                        }
                    }
                }
            }
            ChessPieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for (o in offsets) {
                    val tr = r + o.first
                    val tc = c + o.second
                    if (tr in 0..7 && tc in 0..7) {
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece == null || targetPiece.isWhite != isWhite) {
                            moves.add(targetIdx)
                        }
                    }
                }
            }
            ChessPieceType.KING -> {
                val offsets = listOf(
                    -1 to -1, -1 to 0, -1 to 1,
                    0 to -1,           0 to 1,
                    1 to -1,  1 to 0,  1 to 1
                )
                for (o in offsets) {
                    val tr = r + o.first
                    val tc = c + o.second
                    if (tr in 0..7 && tc in 0..7) {
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece == null || targetPiece.isWhite != isWhite) {
                            moves.add(targetIdx)
                        }
                    }
                }
            }
            ChessPieceType.ROOK -> {
                val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                for (d in dirs) {
                    var step = 1
                    while (true) {
                        val tr = r + d.first * step
                        val tc = c + d.second * step
                        if (tr !in 0..7 || tc !in 0..7) break
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.isWhite != isWhite) {
                                moves.add(targetIdx)
                            }
                            break
                        }
                        step++
                    }
                }
            }
            ChessPieceType.BISHOP -> {
                val dirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
                for (d in dirs) {
                    var step = 1
                    while (true) {
                        val tr = r + d.first * step
                        val tc = c + d.second * step
                        if (tr !in 0..7 || tc !in 0..7) break
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.isWhite != isWhite) {
                                moves.add(targetIdx)
                            }
                            break
                        }
                        step++
                    }
                }
            }
            ChessPieceType.QUEEN -> {
                val dirs = listOf(
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1),
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)
                )
                for (d in dirs) {
                    var step = 1
                    while (true) {
                        val tr = r + d.first * step
                        val tc = c + d.second * step
                        if (tr !in 0..7 || tc !in 0..7) break
                        val targetIdx = tr * 8 + tc
                        val targetPiece = board.getOrNull(targetIdx)
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.isWhite != isWhite) {
                                moves.add(targetIdx)
                            }
                            break
                        }
                        step++
                    }
                }
            }
        }
        return moves
    }

    val validMoves = if (selectedIndex != null) getValidMovesForIndex(selectedIndex!!) else emptyList()

    fun triggerAiMove() {
        if (gameOutcome.isNotEmpty()) return
        
        val aiMoves = mutableListOf<Pair<Int, Int>>()
        for (i in 0..63) {
            val p = board.getOrNull(i)
            if (p != null && !p.isWhite) {
                val possibleTargets = getValidMovesForIndex(i)
                for (t in possibleTargets) {
                    aiMoves.add(Pair(i, t))
                }
            }
        }
        
        if (aiMoves.isEmpty()) {
            val whiteKingExists = board.any { it?.type == ChessPieceType.KING && it.isWhite }
            val blackKingExists = board.any { it?.type == ChessPieceType.KING && !it.isWhite }
            if (whiteKingExists && !blackKingExists) {
                gameOutcome = "WIN"
            } else if (!whiteKingExists && blackKingExists) {
                gameOutcome = "DEFEAT"
            } else {
                gameOutcome = "DRAW"
            }
            return
        }
        
        val selectedMove = when (difficulty) {
            "EASY" -> {
                aiMoves.random()
            }
            "MEDIUM" -> {
                val captureMoves = aiMoves.filter { board.getOrNull(it.second) != null }
                if (captureMoves.isNotEmpty()) {
                    captureMoves.maxByOrNull {
                        val targetType = board.getOrNull(it.second)?.type ?: ChessPieceType.PAWN
                        when (targetType) {
                            ChessPieceType.KING -> 1000
                            ChessPieceType.QUEEN -> 90
                            ChessPieceType.ROOK -> 50
                            ChessPieceType.BISHOP -> 35
                            ChessPieceType.KNIGHT -> 30
                            ChessPieceType.PAWN -> 10
                        }
                    } ?: aiMoves.random()
                } else {
                    aiMoves.random()
                }
            }
            else -> { // OVERLORD
                aiMoves.maxByOrNull {
                    val targetSquare = board.getOrNull(it.second)
                    var score = 0
                    if (targetSquare != null) {
                        score += 20 + when (targetSquare.type) {
                            ChessPieceType.KING -> 5000
                            ChessPieceType.QUEEN -> 120
                            ChessPieceType.ROOK -> 80
                            ChessPieceType.BISHOP -> 50
                            ChessPieceType.KNIGHT -> 45
                            ChessPieceType.PAWN -> 20
                        }
                    }
                    val toRow = it.second / 8
                    score += toRow
                    score
                } ?: aiMoves.random()
            }
        }
        
        val fromIdx = selectedMove.first
        val toIdx = selectedMove.second
        val movingPiece = board[fromIdx]
        val targetPiece = board[toIdx]
        
        if (targetPiece != null) {
            viewModel.vibrate(100)
        } else {
            viewModel.vibrate(50)
        }
        
        board[toIdx] = movingPiece
        board[fromIdx] = null
        
        val whiteKingLeft = board.any { it?.type == ChessPieceType.KING && it.isWhite }
        if (!whiteKingLeft) {
            gameOutcome = "DEFEAT"
            viewModel.vibrate(400)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "RU") "ЛУННЫЕ ШАХМАТЫ" else "LUNAR CHESS",
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (lang == "RU") "Отказоустойчивый симулятор nOG" else "nOG Fault-tolerant tactical node",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.vibrate(30)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "RU") "СЛОЖНОСТЬ AI:" else "AI NODE:",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("EASY", "MEDIUM", "OVERLORD").forEach { level ->
                                val isSelected = difficulty == level
                                val levelLabel = when (level) {
                                    "EASY" -> if (lang == "RU") "ЗУМЕР" else "EASY"
                                    "MEDIUM" -> if (lang == "RU") "ОХРАННИК" else "MEDIUM"
                                    else -> if (lang == "RU") "ОВЕРЛОРД" else "OVERLORD"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isSelected) PureWhite else PureBlack)
                                        .border(1.dp, if (isSelected) PureWhite else BorderGray, RoundedCornerShape(2.dp))
                                        .clickable {
                                            viewModel.vibrate(25)
                                            difficulty = level
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = levelLabel,
                                        color = if (isSelected) PureBlack else TextGray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                            .border(2.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (row in 0..7) {
                                Row(modifier = Modifier.weight(1f)) {
                                    for (col in 0..7) {
                                        val idx = row * 8 + col
                                        val squarePiece = board.getOrNull(idx)
                                        val isSquareDark = (row + col) % 2 == 1
                                        val baseBg = if (isSquareDark) PureBlack else DeepGray
                                        
                                        val isValidTarget = validMoves.contains(idx)
                                        val isSelected = selectedIndex == idx
                                        
                                        val backgroundForTile = when {
                                            isSelected -> PureWhite.copy(alpha = 0.25f)
                                            isValidTarget -> PureWhite.copy(alpha = 0.12f)
                                            else -> baseBg
                                        }
                                        
                                        val borderForTileModifier = when {
                                            isSelected -> Modifier.border(1.dp, PureWhite)
                                            isValidTarget -> Modifier.border(1.dp, TextGray.copy(alpha = 0.5f))
                                            else -> Modifier
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(backgroundForTile)
                                                .then(borderForTileModifier)
                                                .clickable {
                                                    if (gameOutcome.isNotEmpty()) return@clickable
                                                    
                                                    if (isValidTarget && selectedIndex != null) {
                                                        val moving = board[selectedIndex!!] ?: return@clickable
                                                        val target = board[idx]
                                                        
                                                        if (target != null) {
                                                            viewModel.vibrate(100)
                                                        } else {
                                                            viewModel.vibrate(40)
                                                        }
                                                        
                                                        board[idx] = moving
                                                        board[selectedIndex!!] = null
                                                        selectedIndex = null
                                                        
                                                        selectedPieceName = ""
                                                        selectedPieceRuleText = ""
                                                        
                                                        val blackKingLeft = board.any { it?.type == ChessPieceType.KING && !it.isWhite }
                                                        if (!blackKingLeft) {
                                                            gameOutcome = "WIN"
                                                            viewModel.vibrate(400)
                                                        } else {
                                                            triggerAiMove()
                                                        }
                                                    } else {
                                                        if (squarePiece != null && squarePiece.isWhite) {
                                                            viewModel.vibrate(30)
                                                            selectedIndex = idx
                                                            
                                                            selectedPieceName = when (squarePiece.type) {
                                                                ChessPieceType.KING -> if (lang == "RU") "ЛУННЫЙ ИМПЕРАТОР (КОРОЛЬ)" else "LUNAR KAISER (KING)"
                                                                ChessPieceType.QUEEN -> if (lang == "RU") "КВАНТОВАЯ КОРОЛЕВА (ФЕРЗЬ)" else "QUANTUM MATRIX (QUEEN)"
                                                                ChessPieceType.ROOK -> if (lang == "RU") "ТЕРМИНАЛЬНАЯ ЛАДЬЯ" else "TACTICAL TOWER (ROOK)"
                                                                ChessPieceType.BISHOP -> if (lang == "RU") "КИБЕР-СЛOН" else "NEURAL CLERIC (BISHOP)"
                                                                ChessPieceType.KNIGHT -> if (lang == "RU") "ЛУННЫЙ КОНЬ" else "LUNAR STALLION (KNIGHT)"
                                                                ChessPieceType.PAWN -> if (lang == "RU") "ЛУННАЯ ПЕШКА" else "GRID INTERCEPTOR (PAWN)"
                                                            }
                                                            selectedPieceRuleText = when (squarePiece.type) {
                                                                ChessPieceType.KING -> if (lang == "RU") "Ходит на 1 клетку в любом направлении." else "Moves 1 field radically in all vectors."
                                                                ChessPieceType.QUEEN -> if (lang == "RU") "Двигается по диагонали, вертикали или горизонтали." else "Moves hyper-dimensionally over long distances."
                                                                ChessPieceType.ROOK -> if (lang == "RU") "Ходит по прямой горизонтали и вертикали." else "Traverses straight paths until collision."
                                                                ChessPieceType.BISHOP -> if (lang == "RU") "Ходит по диагоналям на любое расстояние." else "Angles diagonally through empty logic blocks."
                                                                ChessPieceType.KNIGHT -> if (lang == "RU") "Прыгает буквой 'Г' через другие фигуры." else "Leaps over grid obstacles in L-protocol vectors."
                                                                ChessPieceType.PAWN -> if (lang == "RU") "Идет на 1 шаг вперед, бьет на 1 клетку по диагонали." else "Advances straight. Hijacks coordinates diagonally forward."
                                                            }
                                                        } else {
                                                            selectedIndex = null
                                                            selectedPieceName = ""
                                                            selectedPieceRuleText = ""
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (squarePiece != null) {
                                                val tokenColorForPiece = if (squarePiece.isWhite) PureWhite else TextGray
                                                val sizeForPiece = if (squarePiece.type == ChessPieceType.PAWN) 18.sp else 24.sp
                                                val pieceTxtSymbol = when (squarePiece.type) {
                                                    ChessPieceType.KING -> "♚"
                                                    ChessPieceType.QUEEN -> "♛"
                                                    ChessPieceType.ROOK -> "♜"
                                                    ChessPieceType.BISHOP -> "♝"
                                                    ChessPieceType.KNIGHT -> "♞"
                                                    ChessPieceType.PAWN -> "♟"
                                                }
                                                Text(
                                                    text = pieceTxtSymbol,
                                                    color = tokenColorForPiece,
                                                    fontSize = sizeForPiece,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            if (isValidTarget && squarePiece == null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(TextGray)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedPieceName.isNotEmpty()) PureWhite.copy(alpha = 0.5f) else BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(10.dp)
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPieceName.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = selectedPieceName,
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedPieceRuleText,
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = if (lang == "RU") "НАЖМИТЕ НА ЛУННУЮ ФИГУРУ ДЛЯ ТАКТИЧЕСКОГО АНАЛИЗА" else "SELECT A LUNAR TOKEN FOR TACTICAL BLUEPRINTS",
                                color = TextGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (gameOutcome.isNotEmpty()) {
                        val statusText = when (gameOutcome) {
                            "WIN" -> if (lang == "RU") "ПОБЕДА ОРГАНИЧЕСКОГО РАЗУМА! ИИ nOG ПОВЕРЖЕН" else "ORGANIC INTEL VICTORIOUS! AI DEFEATED"
                            "DEFEAT" -> if (lang == "RU") "КИБЕР-УНИЧТОЖЕНИЕ! ИИ ЗАХВАТИЛ ВАШУ НОДУ" else "CYBER ELIMINATION. AI OVERWHELMED YOUR NODE"
                            else -> if (lang == "RU") "ПАТОВАЯ СИТУАЦИЯ / НИЧЬЯ" else "LOGIC DEADLOCK / STALEMATE DRAW"
                        }
                        val statusAccentColor = PureWhite
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGray)
                                .border(1.dp, statusAccentColor, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusText,
                                color = statusAccentColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.vibrate(60)
                            resetBoard()
                            selectedIndex = null
                            gameOutcome = ""
                            selectedPieceName = ""
                            selectedPieceRuleText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 36.dp)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (lang == "RU") "ПЕРЕЗАГРУЗИТЬ ДОСКУ" else "REBOOT BOARD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
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

// --- POKER GAME HELPER DATA STRUCTURES ---

data class PokerCard(val suit: String, val rank: String, val value: Int)

enum class PokerHandRank(val score: Int, val ruName: String, val enName: String, val descriptionRu: String, val descriptionEn: String) {
    ROYAL_FLUSH(9, "Флеш-рояль", "Royal Flush", "A, K, Q, J, 10 одной масти", "A, K, Q, J, 10 of the same suit"),
    STRAIGHT_FLUSH(8, "Стрит-флеш", "Straight Flush", "5 мастных карт подряд", "5 cards of same suit in sequence"),
    FOUR_OF_A_KIND(7, "Каре", "Four of a Kind", "4 карты одного номинала", "4 cards of the same rank"),
    FULL_HOUSE(6, "Фулл-хаус", "Full House", "Тройка + Пара", "Three of a kind + Pair"),
    FLUSH(5, "Флеш", "Flush", "5 карт одной масти", "Any 5 cards of the same suit"),
    STRAIGHT(4, "Стрит", "Straight", "5 карт подряд разных мастей", "5 cards in sequence of different suits"),
    THREE_OF_A_KIND(3, "Тройка", "Three of a Kind", "3 карты одного номинала", "3 cards of the same rank"),
    TWO_PAIR(2, "Две пары", "Two Pair", "Две разные пары карт", "Two distinct pairs of cards"),
    PAIR(1, "Пара", "Pair", "Две карты одного номинала", "Two cards of the same rank"),
    HIGH_CARD(0, "Старшая карта", "High Card", "Старшая карта в руке", "Highest rank card when no other combo is made")
}

data class PokerHandResult(val rank: PokerHandRank, val values: List<Int>) : Comparable<PokerHandResult> {
    override fun compareTo(other: PokerHandResult): Int {
        if (this.rank != other.rank) {
            return this.rank.score.compareTo(other.rank.score)
        }
        for (i in 0 until minOf(this.values.size, other.values.size)) {
            if (this.values[i] != other.values[i]) {
                return this.values[i].compareTo(other.values[i])
            }
        }
        return 0
    }
}

enum class PokerStage {
    PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
}

fun createPokerDeck(): List<PokerCard> {
    val suits = listOf("♠", "♥", "♦", "♣")
    val ranks = listOf(
        Pair("2", 2), Pair("3", 3), Pair("4", 4), Pair("5", 5), Pair("6", 6), Pair("7", 7),
        Pair("8", 8), Pair("9", 9), Pair("10", 10), Pair("J", 11), Pair("Q", 12), Pair("K", 13), Pair("A", 14)
    )
    val deck = mutableListOf<PokerCard>()
    for (suit in suits) {
        for (rank in ranks) {
            deck.add(PokerCard(suit, rank.first, rank.second))
        }
    }
    deck.shuffle()
    return deck
}

fun getPokerCombinations(cards: List<PokerCard>, k: Int): List<List<PokerCard>> {
    val result = mutableListOf<List<PokerCard>>()
    fun helper(start: Int, current: List<PokerCard>) {
        if (current.size == k) {
            result.add(current)
            return
        }
        for (i in start until cards.size) {
            helper(i + 1, current + cards[i])
        }
    }
    helper(0, emptyList())
    return result
}

fun evaluate5CardPokerHand(hand: List<PokerCard>): PokerHandResult {
    if (hand.isEmpty()) {
        return PokerHandResult(PokerHandRank.HIGH_CARD, emptyList())
    }
    val sortedHand = hand.sortedByDescending { it.value }
    val values = sortedHand.map { it.value }
    val suits = sortedHand.map { it.suit }
    
    val grouped = values.groupBy { it }.mapValues { it.value.size }
    val sortedGroups = grouped.entries.sortedWith(
        compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key }
    )
    
    val isFlush = hand.size >= 5 && suits.distinct().size == 1
    
    var isStraight = false
    var straightHigh = 0
    val distinctValues = values.distinct()
    if (distinctValues.size >= 5) {
        if (distinctValues[0] - distinctValues[4] == 4) {
            isStraight = true
            straightHigh = distinctValues[0]
        } else if (distinctValues.take(5) == listOf(14, 5, 4, 3, 2)) {
            isStraight = true
            straightHigh = 5
        }
    }
    
    if (isFlush && isStraight) {
        if (straightHigh == 14) {
            return PokerHandResult(PokerHandRank.ROYAL_FLUSH, listOf(14))
        }
        return PokerHandResult(PokerHandRank.STRAIGHT_FLUSH, listOf(straightHigh))
    }
    
    if (sortedGroups.isNotEmpty() && sortedGroups[0].value == 4) {
        val kicker = if (sortedGroups.size > 1) sortedGroups[1].key else 0
        return PokerHandResult(PokerHandRank.FOUR_OF_A_KIND, listOf(sortedGroups[0].key, kicker))
    }
    
    if (sortedGroups.size > 1 && sortedGroups[0].value == 3 && sortedGroups[1].value == 2) {
        return PokerHandResult(PokerHandRank.FULL_HOUSE, listOf(sortedGroups[0].key, sortedGroups[1].key))
    }
    
    if (isFlush) {
        return PokerHandResult(PokerHandRank.FLUSH, values)
    }
    
    if (isStraight) {
        return PokerHandResult(PokerHandRank.STRAIGHT, listOf(straightHigh))
    }
    
    if (sortedGroups.isNotEmpty() && sortedGroups[0].value == 3) {
        val remaining = sortedGroups.drop(1).map { it.key }
        return PokerHandResult(PokerHandRank.THREE_OF_A_KIND, listOf(sortedGroups[0].key) + remaining)
    }
    
    if (sortedGroups.size > 1 && sortedGroups[0].value == 2 && sortedGroups[1].value == 2) {
        val kicker = if (sortedGroups.size > 2) sortedGroups[2].key else 0
        return PokerHandResult(PokerHandRank.TWO_PAIR, listOf(sortedGroups[0].key, sortedGroups[1].key, kicker))
    }
    
    if (sortedGroups.isNotEmpty() && sortedGroups[0].value == 2) {
        val remaining = sortedGroups.drop(1).map { it.key }
        return PokerHandResult(PokerHandRank.PAIR, listOf(sortedGroups[0].key) + remaining)
    }
    
    return PokerHandResult(PokerHandRank.HIGH_CARD, values)
}

fun getBestPokerHand(allCards: List<PokerCard>): PokerHandResult {
    if (allCards.size < 5) {
        return evaluate5CardPokerHand(allCards)
    }
    val combos = getPokerCombinations(allCards, 5)
    var bestResult = evaluate5CardPokerHand(combos[0])
    for (i in 1 until combos.size) {
        val result = evaluate5CardPokerHand(combos[i])
        if (result > bestResult) {
            bestResult = result
        }
    }
    return bestResult
}

// --- COMPOSE POKER CARD COMPONENT (Monochrome) ---

@Composable
fun PokerCardView(card: PokerCard, isFaceDown: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .width(55.dp)
            .height(82.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isFaceDown) DeepGray else PureBlack)
    ) {
        if (isFaceDown) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NOG",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.rank,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = card.suit,
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.rank,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// --- MAIN POKER GAME DIALOG COMPONENT ---

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PokerDialog(onDismiss: () -> Unit, lang: String, viewModel: SocialViewModel) {
    val globalPokerBalance by viewModel.pokerBalance.collectAsState()
    var deckState by remember { mutableStateOf(createPokerDeck()) }
    val playerCards = remember { mutableStateListOf<PokerCard>() }
    val botCards = remember { mutableStateListOf<PokerCard>() }
    val communityCards = remember { mutableStateListOf<PokerCard>() }
    
    val initialBalance = remember { viewModel.pokerBalance.value }
    var playerChips by remember { mutableStateOf(initialBalance) }
    var botChips by remember { mutableStateOf(1000) }
    var pot by remember { mutableStateOf(0) }

    LaunchedEffect(playerChips) {
        viewModel.updatePokerBalance(playerChips)
    }
    
    var currentStage by remember { mutableStateOf(PokerStage.PRE_FLOP) }
    var activeBet by remember { mutableStateOf(10) }
    var playerBetInStage by remember { mutableStateOf(10) }
    var botBetInStage by remember { mutableStateOf(10) }
    
    var gameOutcome by remember { mutableStateOf("") } // "", "WIN", "LOSE", "SPLIT", "FOLD_WIN", "FOLD_LOSE"
    var statusMessage by remember { mutableStateOf("") }
    
    var showHints by remember { mutableStateOf(true) }
    var difficulty by remember { mutableStateOf("MEDIUM") } // "EASY", "MEDIUM", "HARD"
    var showCheatSheet by remember { mutableStateOf(false) }

    fun getRevealedCommunity(): List<PokerCard> {
        return when (currentStage) {
            PokerStage.PRE_FLOP -> emptyList()
            PokerStage.FLOP -> communityCards.take(3)
            PokerStage.TURN -> communityCards.take(4)
            PokerStage.RIVER, PokerStage.SHOWDOWN -> communityCards
        }
    }

    fun getBestHand(cards: List<PokerCard>): PokerHandResult {
        return getBestPokerHand(cards)
    }

    fun runBotBettingAction() {
        if (currentStage == PokerStage.SHOWDOWN) return
        val currentRevealed = getRevealedCommunity()
        val botCombo = getBestHand(botCards + currentRevealed)
        val score = botCombo.rank.score
        
        var betValue = 0
        when (difficulty) {
            "EASY" -> {
                betValue = 0
            }
            "MEDIUM" -> {
                if (score >= 1) { // Pair or better
                    if (kotlin.random.Random.nextInt(100) < 55) {
                        betValue = 10
                    }
                }
            }
            "HARD" -> {
                if (score >= 2) { // Two pair or better
                    betValue = if (score >= 5) 40 else 20
                } else if (score == 1) { // Pair
                    if (kotlin.random.Random.nextInt(100) < 45) {
                        betValue = 10
                    }
                } else {
                    // Bluff 15%
                    if (kotlin.random.Random.nextInt(100) < 15) {
                        betValue = 20
                    }
                }
            }
        }
        
        if (betValue > botChips) {
            betValue = botChips
        }
        
        if (betValue > 0) {
            botChips -= betValue
            pot += betValue
            botBetInStage = betValue
            activeBet = betValue
            statusMessage = if (lang == "RU") "Бот ставит $betValue фишек! Колл или фолд?" else "Bot bet $betValue chips! Call or Fold?"
        } else {
            botBetInStage = 0
            activeBet = 0
            statusMessage = if (lang == "RU") "Бот чекает. Ваша очередь." else "Bot checks. Your action."
        }
    }

    fun startNewHand() {
        viewModel.vibrate(45)
        deckState = createPokerDeck()
        playerCards.clear()
        botCards.clear()
        communityCards.clear()
        
        playerCards.add(deckState[0])
        playerCards.add(deckState[1])
        botCards.add(deckState[2])
        botCards.add(deckState[3])
        
        communityCards.add(deckState[4])
        communityCards.add(deckState[5])
        communityCards.add(deckState[6])
        communityCards.add(deckState[7])
        communityCards.add(deckState[8])
        
        deckState = deckState.drop(9)
        currentStage = PokerStage.PRE_FLOP
        
        // Ante of 10
        val pAnte = minOf(playerChips, 10)
        val bAnte = minOf(botChips, 10)
        
        playerChips -= pAnte
        botChips -= bAnte
        pot = pAnte + bAnte
        
        playerBetInStage = pAnte
        botBetInStage = bAnte
        activeBet = maxOf(pAnte, bAnte)
        
        gameOutcome = ""
        statusMessage = if (lang == "RU") "Сдан префлоп. Пот: $pot. Бот сделал анте." else "Pre-flop dealt. Pot: $pot. Ante collected."
        
        // Let bot decide
        runBotBettingAction()
    }

    LaunchedEffect(Unit) {
        startNewHand()
    }

    fun runShowdown() {
        val playerHand = getBestHand(playerCards + communityCards)
        val botHand = getBestHand(botCards + communityCards)
        val comp = playerHand.compareTo(botHand)
        
        if (comp > 0) {
            viewModel.vibrate(120)
            gameOutcome = "WIN"
            playerChips += pot
            statusMessage = if (lang == "RU") {
                "Вы выиграли! ${playerHand.rank.ruName}. Бот: ${botHand.rank.ruName}."
            } else {
                "You win! ${playerHand.rank.enName}. Bot: ${botHand.rank.enName}."
            }
        } else if (comp < 0) {
            viewModel.vibrate(60)
            gameOutcome = "LOSE"
            botChips += pot
            statusMessage = if (lang == "RU") {
                "Вы проиграли. Ваша комбо: ${playerHand.rank.ruName}. Бот: ${botHand.rank.ruName}."
            } else {
                "You lost. Your combo: ${playerHand.rank.enName}. Bot: ${botHand.rank.enName}."
            }
        } else {
            viewModel.vibrate(50)
            gameOutcome = "SPLIT"
            val half = pot / 2
            playerChips += half
            botChips += (pot - half)
            statusMessage = if (lang == "RU") {
                "Ничья! Раздел банка. Обе руки: ${playerHand.rank.ruName}."
            } else {
                "Split pot! Both hands are equal: ${playerHand.rank.enName}."
            }
        }
        pot = 0
        currentStage = PokerStage.SHOWDOWN
    }

    fun advanceStage(next: PokerStage) {
        playerBetInStage = 0
        botBetInStage = 0
        activeBet = 0
        currentStage = next
        runBotBettingAction()
    }

    fun handlePlayerCall() {
        viewModel.vibrate(30)
        val callAmount = activeBet - playerBetInStage
        if (callAmount > playerChips) {
            val allIn = playerChips
            playerChips = 0
            pot += allIn
            playerBetInStage += allIn
        } else {
            playerChips -= callAmount
            pot += callAmount
            playerBetInStage = activeBet
        }
        
        when (currentStage) {
            PokerStage.PRE_FLOP -> advanceStage(PokerStage.FLOP)
            PokerStage.FLOP -> advanceStage(PokerStage.TURN)
            PokerStage.TURN -> advanceStage(PokerStage.RIVER)
            PokerStage.RIVER -> runShowdown()
            else -> {}
        }
    }

    fun handlePlayerRaise(addAmt: Int) {
        val callAmt = activeBet - playerBetInStage
        val totalRaiseCost = callAmt + addAmt
        if (totalRaiseCost > playerChips) {
            val remaining = playerChips
            playerChips = 0
            pot += remaining
            playerBetInStage += remaining
        } else {
            playerChips -= totalRaiseCost
            pot += totalRaiseCost
            playerBetInStage = activeBet + addAmt
        }
        
        viewModel.vibrate(40)
        
        val botCombo = getBestHand(botCards + getRevealedCommunity())
        val score = botCombo.rank.score
        
        var botWillCall = false
        when (difficulty) {
            "EASY" -> {
                botWillCall = (score >= 1 && addAmt < 50) || kotlin.random.Random.nextInt(100) < 20
            }
            "MEDIUM" -> {
                botWillCall = score >= 1 || (score == 0 && kotlin.random.Random.nextInt(100) < 35)
            }
            "HARD" -> {
                botWillCall = score >= 1 || kotlin.random.Random.nextInt(100) < 65
            }
        }
        
        val callRequired = (playerBetInStage - botBetInStage)
        if (callRequired > botChips) {
            val allIn = botChips
            botChips = 0
            pot += allIn
            botBetInStage += allIn
            statusMessage = if (lang == "RU") "Бот коллирует олл-ин!" else "Bot calls all-in!"
            
            when (currentStage) {
                PokerStage.PRE_FLOP -> advanceStage(PokerStage.FLOP)
                PokerStage.FLOP -> advanceStage(PokerStage.TURN)
                PokerStage.TURN -> advanceStage(PokerStage.RIVER)
                PokerStage.RIVER -> runShowdown()
                else -> {}
            }
        } else if (botWillCall) {
            botChips -= callRequired
            pot += callRequired
            botBetInStage = playerBetInStage
            activeBet = playerBetInStage
            statusMessage = if (lang == "RU") "Бот коллирует ваше повышение. Переход к следующей улице." else "Bot calls your raise. Moving forward."
            
            when (currentStage) {
                PokerStage.PRE_FLOP -> advanceStage(PokerStage.FLOP)
                PokerStage.FLOP -> advanceStage(PokerStage.TURN)
                PokerStage.TURN -> advanceStage(PokerStage.RIVER)
                PokerStage.RIVER -> runShowdown()
                else -> {}
            }
        } else {
            gameOutcome = "FOLD_WIN"
            playerChips += pot
            pot = 0
            currentStage = PokerStage.SHOWDOWN
            statusMessage = if (lang == "RU") "Бот сбросил карты! Вы забрали банк!" else "Bot folds! You swept the pot."
        }
    }

    fun handlePlayerFold() {
        viewModel.vibrate(20)
        gameOutcome = "FOLD_LOSE"
        botChips += pot
        pot = 0
        currentStage = PokerStage.SHOWDOWN
        statusMessage = if (lang == "RU") "Вы сбросили карты. Банк достается боту." else "You folded. Bot sweeps the pot."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "RU") "БИНАРНЫЙ ПОКЕР" else "BINARY POKER",
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (lang == "RU") "Черно-белый Техасский Холдем" else "Monochrome Texas Hold'em node",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (lang == "RU") "ОБЩИЙ БАЛАНС: $globalPokerBalance 🪙" else "TOTAL BALANCE: $globalPokerBalance 🪙",
                                color = AlertYellow,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.vibrate(30)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        ) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "RU") "СЛОЖНОСТЬ AI:" else "AI DIFFICULTY:",
                                color = TextGray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("EASY", "MEDIUM", "HARD").forEach { level ->
                                    val isSelected = difficulty == level
                                    val finalLabel = when (level) {
                                        "EASY" -> if (lang == "RU") "ЛЕГКО" else "EASY"
                                        "MEDIUM" -> if (lang == "RU") "СРЕДНЕ" else "MEDIUM"
                                        else -> if (lang == "RU") "СЛОЖНО" else "HARD"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isSelected) PureWhite else PureBlack)
                                            .border(1.dp, if (isSelected) PureWhite else BorderGray, RoundedCornerShape(2.dp))
                                            .clickable {
                                                viewModel.vibrate(25)
                                                difficulty = level
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = finalLabel,
                                            color = if (isSelected) PureBlack else TextGray,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (lang == "RU") "ПОДСКАЗКИ:" else "HINTS STATUS:",
                                color = TextGray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (showHints) PureWhite else PureBlack)
                                    .border(1.dp, if (showHints) PureWhite else BorderGray, RoundedCornerShape(2.dp))
                                    .clickable {
                                        viewModel.vibrate(25)
                                        showHints = !showHints
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (showHints) (if (lang == "RU") "ВКЛ" else "ON") else (if (lang == "RU") "ВЫКЛ" else "OFF"),
                                    color = if (showHints) PureBlack else TextGray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "RU") "🤖 ОППОНЕНТ (Бот): $botChips фишек" else "🤖 BOT NODE: $botChips chips",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (botBetInStage > 0) {
                            Text(
                                text = "BET: $botBetInStage",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRevealed = (currentStage == PokerStage.SHOWDOWN)
                        if (botCards.size >= 2) {
                            PokerCardView(card = botCards[0], isFaceDown = !isRevealed)
                            PokerCardView(card = botCards[1], isFaceDown = !isRevealed)
                        } else {
                            Text("No cards dealt yet...", color = TextGray, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (lang == "RU") "ОБЩИЕ КАРТЫ СТОЛА" else "COMMUNITY CARDS",
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "💰 POT: $pot",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentRevealedCount = when (currentStage) {
                                    PokerStage.PRE_FLOP -> 0
                                    PokerStage.FLOP -> 3
                                    PokerStage.TURN -> 4
                                    PokerStage.RIVER, PokerStage.SHOWDOWN -> 5
                                }
                                for (i in 0 until 5) {
                                    if (communityCards.size > i) {
                                        val isCardShown = i < currentRevealedCount
                                        PokerCardView(
                                            card = communityCards[i],
                                            isFaceDown = !isCardShown,
                                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "RU") "🧑 ВЫ (Игрок): $playerChips фишек" else "🧑 PLAYER NODE: $playerChips chips",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (playerBetInStage > 0) {
                            Text(
                                text = "BET: $playerBetInStage",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (playerCards.size >= 2) {
                            PokerCardView(card = playerCards[0], isFaceDown = false)
                            PokerCardView(card = playerCards[1], isFaceDown = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showHints) {
                        val playerCurrentCombo = getBestHand(playerCards + getRevealedCommunity())
                        val rankLabel = if (lang == "RU") playerCurrentCombo.rank.ruName else playerCurrentCombo.rank.enName
                        
                        val stratRecommendation = when {
                            playerCurrentCombo.rank.score >= 5 -> if (lang == "RU") "Монстр-рука! Агрессивно ререйзите." else "Monster hand! Go for hefty raises."
                            playerCurrentCombo.rank.score >= 2 -> if (lang == "RU") "Хорошая рука. Поддерживайте ставку или повышайте." else "Strong holding. Call or raise."
                            playerCurrentCombo.rank.score == 1 -> if (lang == "RU") "Пара в руках. Отлично для чека или продления ставки." else "One pair. Ideal for checking or calling."
                            else -> if (lang == "RU") "Старшая карта. Чек, фолд при крупной ставке." else "High card. Check or fold to heavy betting."
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                .background(DeepGray)
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Tips Icon",
                                        tint = PureWhite,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = (if (lang == "RU") "АНАЛИЗАТОР КОМБИНАЦИЙ" else "COMBINATION ASSISTANT").uppercase(),
                                        color = PureWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${if (lang == "RU") "Текущая рука" else "Best hand"}: $rankLabel",
                                    color = PureWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (lang == "RU") "Решение" else "Strategy tip"}: $stratRecommendation",
                                    color = TextGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PureWhite, RoundedCornerShape(2.dp))
                            .background(DeepGray)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = statusMessage.uppercase(),
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .clickable { showCheatSheet = !showCheatSheet }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "RU") "СПРАВОЧНИК КОМБИНАЦИЙ" else "POKER COMBINATIONS INDEX",
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (showCheatSheet) "▲" else "▼",
                                    color = PureWhite,
                                    fontSize = 8.sp
                                )
                            }
                            if (showCheatSheet) {
                                Spacer(modifier = Modifier.height(8.dp))
                                PokerHandRank.values().forEach { pokerRank ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${pokerRank.score + 1}. " + if (lang == "RU") pokerRank.ruName else pokerRank.enName,
                                            color = PureWhite,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (lang == "RU") pokerRank.descriptionRu else pokerRank.descriptionEn,
                                            color = TextGray,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    HorizontalDivider(color = BorderGray, modifier = Modifier.padding(bottom = 12.dp))
                    
                    if (currentStage == PokerStage.SHOWDOWN) {
                        Button(
                            onClick = {
                                viewModel.vibrate(50)
                                if (playerChips <= 0 || botChips <= 0) {
                                    playerChips = 1000
                                    botChips = 1000
                                }
                                startNewHand()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = if (playerChips <= 0 || botChips <= 0) {
                                    if (lang == "RU") "БАНКРОТ! НАЧАТЬ СНАЧАЛА" else "BANKRUPT! INITIALIZE STACK"
                                } else {
                                    if (lang == "RU") "СЫГРАТЬ СЛЕДУЮЩУЮ РАЗДАЧУ" else "DEAL NEXT HAND"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { handlePlayerFold() },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepGray, contentColor = AlertRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, AlertRed, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ФОЛД" else "FOLD",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            val matchingBet = activeBet - playerBetInStage
                            val actionName = if (matchingBet <= 0) {
                                if (lang == "RU") "ЧЕК" else "CHECK"
                            } else {
                                if (lang == "RU") "КОЛЛ $matchingBet" else "CALL $matchingBet"
                            }
                            Button(
                                onClick = { handlePlayerCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = actionName,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { handlePlayerRaise(20) },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepGray, contentColor = PureWhite),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ПОВЫСИТЬ" else "RAISE +20",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// METADATA-SYNCD QUANTUM STACK GAMES: MATCH-3
// ==========================================

private fun m3HasMatchAt(board: List<List<Int>>, r: Int, c: Int): Boolean {
    if (c >= 2 && board[r][c] == board[r][c-1] && board[r][c] == board[r][c-2]) return true
    if (r >= 2 && board[r][c] == board[r-1][c] && board[r][c] == board[r-2][c]) return true
    return false
}

private fun m3GenerateRandomBoard(): List<List<Int>> {
    val board = MutableList(6) { MutableList(6) { 0 } }
    for (r in 0 until 6) {
        for (c in 0 until 6) {
            var possible = (0..5).toList().shuffled()
            for (tile in possible) {
                board[r][c] = tile
                if (m3HasMatchAt(board, r, c)) {
                    continue
                }
                break
            }
        }
    }
    return board
}

private fun m3FindMatches(board: List<List<Int>>): Set<Pair<Int, Int>> {
    val matched = mutableSetOf<Pair<Int, Int>>()
    for (r in 0 until 6) {
        for (c in 0 until 4) {
            val t = board[r][c]
            if (t != -1 && board[r][c+1] == t && board[r][c+2] == t) {
                matched.add(Pair(r, c))
                matched.add(Pair(r, c+1))
                matched.add(Pair(r, c+2))
            }
        }
    }
    for (r in 0 until 4) {
        for (c in 0 until 6) {
            val t = board[r][c]
            if (t != -1 && board[r+1][c] == t && board[r+2][c] == t) {
                matched.add(Pair(r, c))
                matched.add(Pair(r+1, c))
                matched.add(Pair(r+2, c))
            }
        }
    }
    return matched
}

private fun m3ApplyCascade(board: MutableList<MutableList<Int>>): Int {
    var totalCleared = 0
    var matches = m3FindMatches(board)
    var iteration = 0
    while (matches.isNotEmpty() && iteration < 15) {
        totalCleared += matches.size
        for ((r, c) in matches) {
            board[r][c] = -1
        }
        for (c in 0 until 6) {
            var writeIndex = 5
            for (r in 5 downTo 0) {
                if (board[r][c] != -1) {
                    board[writeIndex][c] = board[r][c]
                    if (writeIndex != r) {
                        board[r][c] = -1
                    }
                    writeIndex--
                }
            }
            for (r in writeIndex downTo 0) {
                board[r][c] = (0..5).random()
            }
        }
        matches = m3FindMatches(board)
        iteration++
    }
    return totalCleared
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Match3Dialog(onDismiss: () -> Unit, lang: String, viewModel: SocialViewModel) {
    val totalMoves = 15
    val shapes = listOf("✕", "◯", "▢", "▲", "✦", "❖")
    
    val scope = rememberCoroutineScope()
    var isSwapping by remember { mutableStateOf(false) }
    var isInfiniteMode by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(m3GenerateRandomBoard()) }
    var score by remember { mutableStateOf(0) }
    var movesLeft by remember { mutableStateOf(totalMoves) }
    var selectedTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hasClaimedReward by remember { mutableStateOf(false) }
    var claimedChipsAmount by remember { mutableStateOf(0) }
    var claimedMilestones by remember { mutableStateOf(0) }

    fun startNewGame() {
        viewModel.vibrate(50)
        board = m3GenerateRandomBoard()
        score = 0
        movesLeft = totalMoves
        selectedTile = null
        hasClaimedReward = false
        claimedChipsAmount = 0
        claimedMilestones = 0
        isSwapping = false
    }

    val potentialReward = when {
        score >= 1200 -> 800
        score >= 800 -> 500
        score >= 500 -> 300
        score >= 300 -> 150
        else -> 0
    }

    fun handleTileClick(r: Int, c: Int) {
        if (isSwapping) return
        val prevSelected = selectedTile
        if (prevSelected == null) {
            viewModel.vibrate(25)
            selectedTile = Pair(r, c)
        } else {
            val (pr, pc) = prevSelected
            if (pr == r && pc == c) {
                selectedTile = null
            } else {
                val isAdjacent = (pr == r && kotlin.math.abs(pc - c) == 1) || (pc == c && kotlin.math.abs(pr - r) == 1)
                if (isAdjacent) {
                    viewModel.vibrate(35)
                    val newBoard = board.map { it.toMutableList() }.toMutableList()
                    val temp = newBoard[pr][pc]
                    newBoard[pr][pc] = newBoard[r][c]
                    newBoard[r][c] = temp

                    selectedTile = null
                    isSwapping = true

                    // Apply visual swap immediately so player sees change
                    board = newBoard

                    scope.launch {
                        delay(240) // Allow smooth swap transition visualization
                        val found = m3FindMatches(newBoard)
                        if (found.isNotEmpty()) {
                            viewModel.vibrate(85)
                            val cleared = m3ApplyCascade(newBoard)
                            score += cleared * 20
                            if (!isInfiniteMode) {
                                movesLeft--
                            }
                            board = newBoard
                        } else {
                            viewModel.vibrate(30)
                            // Swap back smoothly as no matches were aligned
                            val rolledBackBoard = board.map { it.toMutableList() }.toMutableList()
                            val undoTemp = rolledBackBoard[pr][pc]
                            rolledBackBoard[pr][pc] = rolledBackBoard[r][c]
                            rolledBackBoard[r][c] = undoTemp
                            board = rolledBackBoard
                        }
                        isSwapping = false
                    }
                } else {
                    viewModel.vibrate(25)
                    selectedTile = Pair(r, c)
                }
            }
        }
    }

    // --- ANIMATIONS ENGINES ---
    // Smooth rollup score odometer
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "scoreRoll"
    )

    // Breathing pulse for selected node
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingEffect"
    )

    // Endless Milestones claimable check
    val currentRewardTargetMilestones = score / 500
    val unclaimedMilestones = currentRewardTargetMilestones - claimedMilestones

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(PureBlack),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 48.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == "RU") "ОТЛАДКА СТЕКА (ТРИ В РЯД)" else "STACK ALIGNMENT (MATCH-3)",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "RU") "Выстаивайте одинаковые ноды в ряд" else "Align identical codes horizontally/vertically",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = StarkWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Mode Selection Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CardGray, RoundedCornerShape(4.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val classicLabel = if (lang == "RU") "КЛАССИКА (15 ХОДОВ)" else "CLASSIC (15 MOVES)"
                        val endlessLabel = if (lang == "RU") "БЕСКОНЕЧНЫЙ РЕЖИМ" else "ENDLESS MODE"

                        Button(
                            onClick = {
                                if (isInfiniteMode) {
                                    isInfiniteMode = false
                                    startNewGame()
                                }
                            },
                            modifier = Modifier.weight(1f).height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isInfiniteMode) PureWhite else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (!isInfiniteMode) PureBlack else TextGray
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = classicLabel,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (!isInfiniteMode) {
                                    isInfiniteMode = true
                                    startNewGame()
                                }
                            },
                            modifier = Modifier.weight(1f).height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInfiniteMode) PureWhite else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (isInfiniteMode) PureBlack else TextGray
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = endlessLabel,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Balance & Score card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGray)
                        .border(1.dp, BorderGray, RoundedCornerShape(2.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == "RU") "ОЧКИ: $animatedScore" else "SCORE: $animatedScore",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        val targetNext = if (isInfiniteMode) {
                            if (lang == "RU") "Награда: +200🪙 за каждые 500" else "Reward: +200🪙 per 500 pts"
                        } else {
                            when {
                                score < 300 -> if (lang == "RU") "Цель: 300 (+150🪙)" else "Next Target: 300 (+150🪙)"
                                score < 500 -> if (lang == "RU") "Цель: 500 (+300🪙)" else "Next Target: 500 (+300🪙)"
                                score < 800 -> if (lang == "RU") "Цель: 800 (+500🪙)" else "Next Target: 800 (+500🪙)"
                                score < 1200 -> if (lang == "RU") "Цель: 1200 (+800🪙)" else "Next Target: 1200 (+800🪙)"
                                else -> if (lang == "RU") "Максимум!" else "Elite Tier Reached!"
                            }
                        }
                        Text(
                            text = targetNext,
                            color = AlertYellow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isInfiniteMode) {
                                if (lang == "RU") "ХОДЫ: ∞" else "MOVES: ∞"
                            } else {
                                if (lang == "RU") "ХОДЫ: $movesLeft" else "MOVES: $movesLeft"
                            },
                            color = if (!isInfiniteMode && movesLeft <= 3) AlertRed else PureWhite,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isInfiniteMode) {
                                if (lang == "RU") "Получено: ${claimedMilestones * 200}🪙" else "Earned: ${claimedMilestones * 200}🪙"
                            } else {
                                if (lang == "RU") "Награда: +$potentialReward 🪙" else "Reward: +$potentialReward 🪙"
                            },
                            color = AlertGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Board Grid Column with bounciness & spring animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (r in 0 until 6) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 6) {
                                    val isSelected = selectedTile?.first == r && selectedTile?.second == c
                                    val tileId = board[r][c]
                                    val symbol = if (tileId in 0..5) shapes[tileId] else " "
                                    
                                    // Animated scaling for state (either breathing pulsing when selected or spring on default)
                                    val cellScale by animateFloatAsState(
                                        targetValue = if (isSelected) breathingScale else 1.0f,
                                        animationSpec = if (isSelected) tween(100) else spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "cellScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .graphicsLayer(
                                                scaleX = cellScale,
                                                scaleY = cellScale
                                            )
                                            .background(if (isSelected) StarkWhite else CardGray)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PureWhite else BorderGray,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .clickable(enabled = (isInfiniteMode || movesLeft > 0) && !isSwapping) {
                                                handleTileClick(r, c)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = symbol,
                                            color = if (isSelected) PureBlack else StarkWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Result actions & buttons raised higher
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isInfiniteMode) {
                        if (unclaimedMilestones > 0) {
                            val rewCoins = unclaimedMilestones * 200
                            Button(
                                onClick = {
                                    viewModel.vibrate(150)
                                    viewModel.updatePokerBalance(viewModel.pokerBalance.value + rewCoins)
                                    claimedMilestones = currentRewardTargetMilestones
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertGreen,
                                    contentColor = PureBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = if (lang == "RU") "ЗАБРАТЬ ВЕХУ +$rewCoins 🪙" else "CLAIM MILESTONE REWARD +$rewCoins 🪙",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val nextGoal = (currentRewardTargetMilestones + 1) * 500
                            Text(
                                text = if (lang == "RU") "СЛЕДУЮЩАЯ ВЕХА НА $nextGoal ОЧКОВ" else "NEXT MILESTONE AT $nextGoal PTS",
                                color = AlertYellow,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else if (movesLeft <= 0) {
                        Text(
                            text = if (lang == "RU") "ХОДЫ ИЗРАСХОДОВАНЫ!" else "GAME OVER - MOVES DEPLETED!",
                            color = AlertRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (potentialReward > 0 && !hasClaimedReward) {
                            Button(
                                onClick = {
                                    viewModel.vibrate(150)
                                    val currentBal = viewModel.pokerBalance.value
                                    viewModel.updatePokerBalance(currentBal + potentialReward)
                                    claimedChipsAmount = potentialReward
                                    hasClaimedReward = true
                                },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = PureBlack
                                ),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ЗАБРАТЬ НАГРАДУ +$potentialReward 🪙" else "CLAIM CHIPS REWARD +$potentialReward 🪙",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (hasClaimedReward) {
                            Text(
                                text = if (lang == "RU") "Награда успешно зачислена +$claimedChipsAmount 🪙!" else "Successfully Credited +$claimedChipsAmount 🪙!",
                                color = AlertGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                text = if (lang == "RU") "Наберите ≥300 очков для получения чипов" else "Score ≥300 to claim chip reward",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (lang == "RU") "КЛИКАЙТЕ ПОДРЯД ДВА ТАЙЛА ДЛЯ ОБМЕНА" else "TAP TWO TILES SEQUENTIALLY TO SWAP THEM",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { startNewGame() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, PureWhite, RoundedCornerShape(4.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardGray,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (lang == "RU") "НАЧАТЬ ЗАНОВО" else "RESTART",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepGray,
                                contentColor = TextGray
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (lang == "RU") "ЗАКРЫТЬ" else "EXIT",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Additional spacing under buttons to explicitly raise them from absolute bottom glass edge
                    Spacer(modifier = Modifier.height(26.dp))
                }
            }
        }
    }
}

