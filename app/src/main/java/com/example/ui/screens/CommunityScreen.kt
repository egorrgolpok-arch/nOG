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
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.window.Dialog
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
        val isAiCommPost = author?.isVerified == true && author.isAi
        val isUserCommPost = post.authorId == "user" && post.category == "Community"
        isAiCommPost || isUserCommPost
    }.sortedByDescending { it.timestamp }

    val isTempVerified = currentUser?.isVerified == true && (currentUser?.verificationExpiry ?: 0) > System.currentTimeMillis()
    val isPermVerified = currentUser?.isVerified == true && currentUser?.verificationExpiry == null
    
    var newPostText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    var showTempVerificationDialog by remember { mutableStateOf(false) }
    var showBlackJackGame by remember { mutableStateOf(false) }
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
                    
                    IconButton(
                        onClick = { showBlackJackGame = true },
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, AlertYellow, RoundedCornerShape(4.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = "Blackjack Mini-game",
                            tint = AlertYellow,
                            modifier = Modifier.size(22.dp)
                        )
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
        BlackjackDialog(onDismiss = { showBlackJackGame = false }, lang = lang)
    }
}

@Composable
fun BlackjackDialog(onDismiss: () -> Unit, lang: String) {
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

    val feltGreen = androidx.compose.ui.graphics.Color(0xFF0F421D)
    
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
        }

        dealerHand = localDealerHand
        deck = localDeck

        val playerSc = calculateScore(playerH)
        val dealerSc = calculateScore(localDealerHand)

        if (dealerSc > 21) {
            gameOutcomeText = if (lang == "RU") "ДИЛЕР СГОРЕЛ! ВЫ ПОБЕДИЛИ" else "DEALER BUSTED! YOU WIN"
            wallet += bet * 2
        } else if (playerSc > dealerSc) {
            gameOutcomeText = if (lang == "RU") "ВЫ ВЫИГРАЛИ!" else "YOU WIN!"
            wallet += bet * 2
        } else if (playerSc < dealerSc) {
            gameOutcomeText = if (lang == "RU") "ДИЛЕР ВЫИГРАЛ!" else "DEALER WINS!"
        } else {
            gameOutcomeText = if (lang == "RU") "НИЧЬЯ (ПУШ)" else "PUSH (TIE)"
            wallet += bet
        }
        gamePhase = "ENDED"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .border(2.dp, PureWhite, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = feltGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "RU") "БЛЭКДЖЕК 21" else "BLACKJACK 21",
                        color = AlertYellow,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = PureWhite)
                    }
                }

                HorizontalDivider(color = PureWhite.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))

                // Stats: Wallet & Bet info
                Row(
                    modifier = Modifier.fillMaxWidth().background(PureBlack.copy(alpha = 0.4f)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (lang == "RU") "БАЛАНС" else "WALLET", color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("$$wallet", color = AlertGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (lang == "RU") "СТАВКА" else "BET", color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("$$currentBet", color = AlertYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dealers Area
                Text(
                    text = if (lang == "RU") "ДИЛЕР" else "DEALER",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dealerHand.isEmpty()) {
                        Text(if (lang == "RU") "Ожидание ставок..." else "Awaiting bets...", color = PureWhite.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    } else {
                        dealerHand.forEachIndexed { index, card ->
                            val isHidden = (index == 1 && gamePhase == "PLAYER_TURN")
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .width(55.dp)
                                    .height(82.dp)
                                    .border(1.dp, if (isHidden) AlertYellow else PureWhite, RoundedCornerShape(4.dp)),
                                colors = CardDefaults.cardColors(containerColor = if (isHidden) CardGray else PureWhite)
                            ) {
                                if (isHidden) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("X", color = AlertYellow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                } else {
                                    val isRedSuit = card.suit == "♥" || card.suit == "♦"
                                    val paintColor = if (isRedSuit) AlertRed else PureBlack
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(card.rank, color = paintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(card.suit, color = paintColor, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontFamily = FontFamily.Monospace)
                                        Text(card.rank, color = paintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End), fontFamily = FontFamily.Monospace)
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
                        text = if (gamePhase == "PLAYER_TURN") "Score: $dScoreVisible + ?" else "Score: $dScoreVisible",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Player Area
                Text(
                    text = if (lang == "RU") "ИГРОК" else "PLAYER",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (playerHand.isEmpty()) {
                        Text("-", color = PureWhite.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                    } else {
                        playerHand.forEach { card ->
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .width(55.dp)
                                    .height(82.dp)
                                    .border(1.dp, PureWhite, RoundedCornerShape(4.dp)),
                                colors = CardDefaults.cardColors(containerColor = PureWhite)
                            ) {
                                val isRedSuit = card.suit == "♥" || card.suit == "♦"
                                val paintColor = if (isRedSuit) AlertRed else PureBlack
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(card.rank, color = paintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(card.suit, color = paintColor, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontFamily = FontFamily.Monospace)
                                    Text(card.rank, color = paintColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End), fontFamily = FontFamily.Monospace)
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mid-screen outcome
                if (gamePhase == "ENDED") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureBlack.copy(alpha = 0.5f))
                            .border(1.dp, AlertYellow)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gameOutcomeText,
                            color = AlertYellow,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Buttons controls
                when (gamePhase) {
                    "BETTING" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { if (currentBet >= 50) currentBet -= 50 },
                                colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("-50", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = PureWhite)
                            }
                            Button(
                                onClick = { if (currentBet + 50 <= wallet) currentBet += 50 },
                                colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("+50", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = PureWhite)
                            }
                            Button(
                                onClick = { currentBet = if (wallet > 0) wallet else 100 },
                                colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("ALL-IN", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = PureWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (wallet <= 0 && playerHand.isEmpty()) {
                            Button(
                                onClick = { wallet = 1000; currentBet = 100 },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth().height(45.dp)
                            ) {
                                Text(
                                    if (lang == "RU") "ПОЛУЧИТЬ КРЕДИТ $1000" else "RECHARGE $1000 CHIPS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
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
                                        } else {
                                            gameOutcomeText = if (lang == "RU") "БЛЭКДЖЕК! ПОБЕДА 3:2" else "BLACKJACK! WIN 3:2"
                                            wallet += (checkedBet * 2.5).toInt()
                                        }
                                        gamePhase = "ENDED"
                                    } else {
                                        gamePhase = "PLAYER_TURN"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth().height(45.dp)
                            ) {
                                Text(
                                    if (lang == "RU") "СДАТЬ КАРТЫ" else "DEAL HAND",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
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
                                    val workingPlayer = playerHand.toMutableList()
                                    val workingDeck = deck.toMutableList()
                                    workingPlayer.add(workingDeck.removeAt(0))
                                    playerHand = workingPlayer
                                    deck = workingDeck

                                    if (calculateScore(workingPlayer) > 21) {
                                        gameOutcomeText = if (lang == "RU") "СГОРЕЛИ! ПЕРЕБОР" else "BUSTED! OVER 21"
                                        gamePhase = "ENDED"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).padding(end = 4.dp).height(44.dp)
                            ) {
                                Text(if (lang == "RU") "ЕЩЁ" else "HIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            val canDouble = playerHand.size == 2 && wallet >= currentBet
                            Button(
                                onClick = {
                                    if (canDouble) {
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
                                        } else {
                                            processDealerTurn(workingDeck, workingPlayer, doubleBet)
                                        }
                                    }
                                },
                                enabled = canDouble,
                                colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1.5f).padding(horizontal = 4.dp).height(44.dp)
                            ) {
                                Text(if (lang == "RU") "УДВОИТЬ" else "DOUBLE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    processDealerTurn(deck, playerHand, currentBet)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).padding(start = 4.dp).height(44.dp)
                            ) {
                                Text(if (lang == "RU") "ХВАТИТ" else "STAND", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "ENDED" -> {
                        Button(
                            onClick = {
                                playerHand = emptyList()
                                dealerHand = emptyList()
                                gameOutcomeText = ""
                                if (currentBet > wallet) {
                                    currentBet = if (wallet > 0) wallet else 100
                                }
                                gamePhase = "BETTING"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().height(45.dp)
                        ) {
                            Text(
                                if (lang == "RU") "ИГРАТЬ ЕЩЕ" else "PLAY AGAIN",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
