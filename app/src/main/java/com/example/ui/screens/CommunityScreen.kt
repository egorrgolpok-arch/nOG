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
import androidx.compose.ui.window.DialogProperties
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
        val isAiCommPost = author?.isVerified == true && author.isAi && post.trustScore in 95..100
        val isUserCommPost = post.authorId == "user" && post.category == "Community"
        isAiCommPost || isUserCommPost
    }.sortedByDescending { it.timestamp }

    val isTempVerified = currentUser?.isVerified == true && (currentUser?.verificationExpiry ?: 0) > System.currentTimeMillis()
    val isPermVerified = currentUser?.isVerified == true && currentUser?.verificationExpiry == null
    
    var newPostText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    var showTempVerificationDialog by remember { mutableStateOf(false) }
    var showBlackJackGame by remember { mutableStateOf(false) }
    var showChessGame by remember { mutableStateOf(false) }
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
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                        IconButton(
                            onClick = { showChessGame = true },
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.dp, AlertGreen, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("♟", color = AlertGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
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
        BlackjackDialog(onDismiss = { showBlackJackGame = false }, lang = lang, viewModel = viewModel)
    }

    if (showChessGame) {
        ChessDialog(onDismiss = { showChessGame = false }, lang = lang, viewModel = viewModel)
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
                                color = AlertYellow,
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
                            Text("$$wallet", color = AlertGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (lang == "RU") "ТЕКУЩИЙ ПАКЕТ" else "ACTIVE BET", color = TextGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("$$currentBet", color = AlertYellow, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                                        .border(2.dp, if (isHidden) AlertYellow else BorderGray, RoundedCornerShape(6.dp)),
                                    colors = CardDefaults.cardColors(containerColor = if (isHidden) PureBlack else DeepGray)
                                ) {
                                    if (isHidden) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("CRYPT", color = AlertYellow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    } else {
                                        val isRedSuit = card.suit == "♥" || card.suit == "♦"
                                        val paintColor = if (isRedSuit) AlertRed else PureWhite
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
                                    val paintColor = if (isRedSuit) AlertRed else PureWhite
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
                                .border(1.dp, AlertYellow, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gameOutcomeText,
                                color = AlertYellow,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Controls Block at Bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
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
                                        .border(1.dp, AlertYellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .height(38.dp)
                                ) {
                                    Text("ALL-IN", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AlertYellow, fontWeight = FontWeight.Bold)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
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
                                colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
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
                                color = AlertGreen,
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
                    modifier = Modifier.weight(1f),
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
                                        .background(if (isSelected) AlertGreen else PureBlack)
                                        .border(1.dp, if (isSelected) AlertGreen else BorderGray, RoundedCornerShape(2.dp))
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
                                            isSelected -> AlertYellow.copy(alpha = 0.3f)
                                            isValidTarget -> AlertGreen.copy(alpha = 0.25f)
                                            else -> baseBg
                                        }
                                        
                                        val borderForTileModifier = when {
                                            isSelected -> Modifier.border(1.dp, AlertYellow)
                                            isValidTarget -> Modifier.border(1.dp, AlertGreen)
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
                                                val tokenColorForPiece = if (squarePiece.isWhite) androidx.compose.ui.graphics.Color(0xFF80DEEA) else AlertYellow
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
                                                        .background(AlertGreen)
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
                            .border(1.dp, if (selectedPieceName.isNotEmpty()) AlertGreen.copy(alpha = 0.5f) else BorderGray, RoundedCornerShape(4.dp))
                            .background(DeepGray)
                            .padding(10.dp)
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPieceName.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = selectedPieceName,
                                    color = AlertGreen,
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
                        val statusAccentColor = if (gameOutcome == "WIN") AlertGreen else AlertRed
                        
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
                }

                Button(
                    onClick = {
                        viewModel.vibrate(60)
                        resetBoard()
                        selectedIndex = null
                        gameOutcome = ""
                        selectedPieceName = ""
                        selectedPieceRuleText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertGreen, contentColor = PureBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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
