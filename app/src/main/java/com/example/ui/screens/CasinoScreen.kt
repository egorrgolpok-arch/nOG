package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocialViewModel
import com.example.ui.theme.AlertYellow
import com.example.ui.theme.AlertGreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.StarkWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Color palette for dark terminal aesthetic
private val PureBlack = Color(0xFF000000)
private val PureWhite = Color(0xFFFFFFFF)
private val DeepGray = Color(0xFF0D0D0D)
private val CardGray = Color(0xFF141414)
private val BorderGray = Color(0xFF222222)
private val TextGray = Color(0xFF888888)
private val AccentGray = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasinoScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val userCoins by viewModel.userCoins.collectAsState()
    val isRu = viewModel.selectedLanguage.collectAsState().value == "RU"
    
    var activeGame by remember { mutableStateOf<String?>(null) } // "blackjack", "poker", "durak", "racing", "roulette", "slots"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (activeGame == null) {
                                if (isRu) "🛰️ СУБОРБИТАЛЬНОЕ nOG КАЗИНО" else "🛰️ SUBORBITAL nOG CASINO"
                            } else {
                                when (activeGame) {
                                    "blackjack" -> if (isRu) "♠️ БЛЕКДЖЕК" else "♠️ BLACKJACK"
                                    "poker" -> if (isRu) "♦️ ТЕХАССКИЙ ПОКЕР" else "♦️ TEXAS HOLD'EM"
                                    "durak" -> if (isRu) "♣️ ДУРАК С БОТАМИ" else "♣️ DURAK VS BOTS"
                                    "racing" -> if (isRu) "🏇 СИНАПТИЧЕСКИЕ СКАЧКИ" else "🏇 SYNAPTIC RACES"
                                    "roulette" -> if (isRu) "🎡 МОНОХРОМНАЯ РУЛЕТКА" else "🎡 MONOCHROME ROULETTE"
                                    "slots" -> if (isRu) "🎰 ЦИФРОВЫЕ СЛОТЫ" else "🎰 CYPHER SLOT MACHINE"
                                    else -> "nOG"
                                }
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PureWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🪙 $userCoins",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (activeGame != null) {
                        IconButton(onClick = { 
                            viewModel.vibrate(25)
                            activeGame = null 
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = PureWhite
                )
            )
        },
        containerColor = PureBlack,
        modifier = Modifier.padding(innerPadding)
    ) { padValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(PureBlack)
        ) {
            if (activeGame == null) {
                // Game Selection Screen
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardGray),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isRu) "ВЕЙПОР-СЕТЬ nOG-CASINO 🛸" else "nOG-CASINO VAPORNET 🛸",
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRu) "Откалибровано под стандартную теорию вероятностей. Никаких накруток со стороны владельца. nOG гарантирует чистые 100% честные математические шансы." 
                                           else "Calibrated under authentic mathematical statistics. No backend skewing. nOG node coordinates guarantee exact casino grade fair percentages.",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    val gamesList = listOf(
                        GameCardData("blackjack", if (isRu) "♠️ Блекджек (21)" else "♠️ Blackjack (21)", if (isRu) "Играй против дилера-бота до 21 очка. Выплаты 1.5x за блекджек." else "Standard 21 against terminal bot dealer. Payouts 3:2.", Icons.Default.Casino),
                        GameCardData("poker", if (isRu) "♦️ Техасский Покер" else "♦️ Texas Hold'em", if (isRu) "Дуэль один на один против кибер-бота. Полные круги ставок и комбинаций." else "Heads-up Texas Hold'em duel against adaptive AI bot.", Icons.Default.Style),
                        GameCardData("durak", if (isRu) "♣️ Дурак с ботами" else "♣️ Durak Card Game", if (isRu) "Легендарная русская игра. Атакуй, защищайся козырем и переиграй интеллект." else "Legendary Russian card game. Offend, defend, trump up to beat the bot.", Icons.Default.SportsEsports),
                        GameCardData("racing", if (isRu) "🏇 Скачки рандом-ботов" else "🏇 Synaptic Horse Races", if (isRu) "Ставь на одного из электронных скакунов. Котировки и заезды в реальном времени!" else "Choose your virtual horse bot runner. Unbiased telemetry curves.", Icons.AutoMirrored.Filled.DirectionsRun),
                        GameCardData("roulette", if (isRu) "🎡 Монохромная Рулетка" else "🎡 Monochrome Roulette", if (isRu) "Испытай судьбу на колесе. Числа (0-36), четное/нечетное, белое/черное." else "Make your bets on 0-36. White/Black, Low/High, Odds/Evens.", Icons.Default.Cached),
                        GameCardData("slots", if (isRu) "🎰 Cypher-слоты" else "🎰 Cypher Slot Machine", if (isRu) "Прокрути три барабана и сорви куш в 1,000,000 монет. Шансы близки к реальным!" else "3-reel spin machine. Hit combinations to claim the jackpot.", Icons.Default.ViewWeek)
                    )

                    items(gamesList) { game ->
                        Surface(
                            onClick = { 
                                viewModel.vibrate(40)
                                activeGame = game.id 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, BorderGray),
                            color = CardGray,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(DeepGray)
                                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = game.icon,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = game.desc,
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = AccentGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Render Game
                when (activeGame) {
                    "blackjack" -> BlackjackGame(viewModel, userCoins, isRu)
                    "poker" -> PokerGame(viewModel, userCoins, isRu)
                    "durak" -> DurakGame(viewModel, userCoins, isRu)
                    "racing" -> HorseRacingGame(viewModel, userCoins, isRu)
                    "roulette" -> RouletteGame(viewModel, userCoins, isRu)
                    "slots" -> SlotsGame(viewModel, userCoins, isRu)
                }
            }
        }
    }
}

data class GameCardData(
    val id: String,
    val title: String,
    val desc: String,
    val icon: ImageVector
)

// Card rendering helper for casino games
@Composable
fun PlayingCardView(card: String) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 70.dp)
            .background(PureBlack)
            .border(1.5.dp, PureWhite, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Text(
                text = card.take(if(card.startsWith("10")) 2 else 1),
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = card.last().toString(),
                color = PureWhite,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = card.take(if(card.startsWith("10")) 2 else 1),
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.End)
            )
        }
    }
}

// ==========================================
// 1. BLACKJACK GAME IMPLEMENTATION
// ==========================================
@Composable
fun BlackjackGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    val scope = rememberCoroutineScope()
    var betAmount by remember { mutableStateOf(10) }
    var inGame by remember { mutableStateOf(false) }
    val playerHand = remember { mutableStateListOf<String>() }
    val dealerHand = remember { mutableStateListOf<String>() }
    var gameStatus by remember { mutableStateOf("") } // "won", "lost", "push", "blackjack", ""
    var statusText by remember { mutableStateOf("") }

    val suits = listOf("♠", "♣", "♥", "♦")
    val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

    fun getCardScore(card: String): Int {
        val r = card.dropLast(1)
        return when (r) {
            "A" -> 11
            "K", "Q", "J", "10" -> 10
            else -> r.toIntOrNull() ?: 10
        }
    }

    fun getHandScore(hand: List<String>): Int {
        var sum = hand.sumOf { getCardScore(it) }
        var aces = hand.count { it.startsWith("A") }
        while (sum > 21 && aces > 0) {
            sum -= 10
            aces -= 1
        }
        return sum
    }

    fun dealCard(): String {
        return ranks.random() + suits.random()
    }

    fun checkOutcome() {
        val pScore = getHandScore(playerHand)
        val dScore = getHandScore(dealerHand)
        
        when {
            pScore > 21 -> {
                gameStatus = "lost"
                statusText = if (isRu) "ПЕРЕБОР! ВЫ ПРОИГРАЛИ." else "BUST! YOU LOST."
                viewModel.vibrate(80)
            }
            dScore > 21 -> {
                gameStatus = "won"
                statusText = if (isRu) "У ДИЛЕРА ПЕРЕБОР! ВЫ ВЫИГРАЛИ!" else "DEALER BUST! YOU WIN!"
                viewModel.updateCoins(userCoins + betAmount * 2)
                viewModel.vibrate(120)
            }
            pScore > dScore -> {
                gameStatus = "won"
                statusText = if (isRu) "ВЫ ВЫИГРАЛИ!" else "YOU WIN!"
                viewModel.updateCoins(userCoins + betAmount * 2)
                viewModel.vibrate(120)
            }
            pScore < dScore -> {
                gameStatus = "lost"
                statusText = if (isRu) "ВЫ ПРОИГРАЛИ." else "YOU LOST."
                viewModel.vibrate(80)
            }
            else -> {
                gameStatus = "push"
                statusText = if (isRu) "НИЧЬЯ (ПУШ)." else "PUSH. COINS RETURNED."
                viewModel.updateCoins(userCoins + betAmount)
                viewModel.vibrate(40)
            }
        }
    }

    fun startGame() {
        if (userCoins < betAmount) {
            statusText = if (isRu) "НЕДОСТАТОЧНО МОНЕТ" else "NOT ENOUGH COINS"
            return
        }
        viewModel.updateCoins(userCoins - betAmount)
        viewModel.vibrate(30)
        
        playerHand.clear()
        dealerHand.clear()
        
        playerHand.add(dealCard())
        playerHand.add(dealCard())
        dealerHand.add(dealCard())
        dealerHand.add(dealCard())

        inGame = true
        gameStatus = ""
        statusText = ""

        if (getHandScore(playerHand) == 21) {
            // Player Blackjack
            inGame = false
            gameStatus = "blackjack"
            statusText = if (isRu) "⚡ БЛЕКДЖЕК! ПОБЕДА!" else "⚡ BLACKJACK! YOU WIN!"
            viewModel.updateCoins(userCoins + (betAmount * 2.5).toInt())
            viewModel.vibrate(200)
        }
    }

    fun hit() {
        if (!inGame) return
        viewModel.vibrate(25)
        playerHand.add(dealCard())
        if (getHandScore(playerHand) > 21) {
            inGame = false
            checkOutcome()
        }
    }

    fun stand() {
        if (!inGame) return
        inGame = false
        val playerScore = getHandScore(playerHand)
        scope.launch {
            // Dealer bot plays with clever card selection and strict casino guidelines!
            while (true) {
                val dealerScore = getHandScore(dealerHand)
                val isSoft17 = dealerScore == 17 && dealerHand.any { it.startsWith("A") }
                
                if (dealerScore < 17 || isSoft17) {
                    delay(600)
                    dealerHand.add(dealCard())
                    viewModel.vibrate(15)
                } else if (dealerScore <= 19 && playerScore <= 21 && dealerScore < playerScore) {
                    // High-stakes casino risk taking to try beating a winning player score
                    if (kotlin.random.Random.nextFloat() < 0.82f) {
                        delay(600)
                        dealerHand.add(dealCard())
                        viewModel.vibrate(15)
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            checkOutcome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dealer cards
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isRu) "КАРТЫ ДИЛЕРА" else "DEALER HAND",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dealerHand.forEachIndexed { idx, card ->
                    if (inGame && idx == 1) {
                        // Hidden card
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 70.dp)
                                .background(PureBlack)
                                .border(1.5.dp, TextGray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = TextGray, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        PlayingCardView(card)
                    }
                }
            }
            if (!inGame && dealerHand.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${if (isRu) "Счет: " else "Score: "}${getHandScore(dealerHand)}",
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Status text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Player cards
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                playerHand.forEach { card ->
                    PlayingCardView(card)
                }
            }
            if (playerHand.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${if (isRu) "Твой счет: " else "Your Score: "}${getHandScore(playerHand)}",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isRu) "ТВОИ КАРТЫ" else "YOUR HAND",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        // Betting & Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!inGame) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (betAmount > 5) { betAmount -= 5; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Remove, contentDescription = "Less Bet", tint = PureWhite)
                    }
                    Text(
                        text = "${if (isRu) "СТАВКА: " else "BET: "} $betAmount 🪙",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (betAmount + 5 <= userCoins) { betAmount += 5; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Add, contentDescription = "More Bet", tint = PureWhite)
                    }
                    Button(
                        onClick = {
                            betAmount = userCoins.coerceAtLeast(5)
                            viewModel.vibrate(40)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "ОЛЛ-ИН" else "ALL IN",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { startGame() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text(if (isRu) "РАЗДАТЬ КАРТЫ" else "DEAL HANDS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { hit() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Text(if (isRu) "ЕЩЕ (ЕЩЁ)" else "HIT", fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { stand() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                    ) {
                        Text(if (isRu) "ХВАТИТ" else "STAND", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. TEXAS HOLD'EM POKER GAME IMPLEMENTATION
// ==========================================
@Composable
fun PokerGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    val scope = rememberCoroutineScope()
    var betAmt by remember { mutableStateOf(20) }
    var inPokerGame by remember { mutableStateOf(false) }
    val playerHand = remember { mutableStateListOf<String>() }
    val botHand = remember { mutableStateListOf<String>() }
    val communityCards = remember { mutableStateListOf<String>() }
    var potAmt by remember { mutableStateOf(0) }
    var pokerStatusText by remember { mutableStateOf("") }
    
    // Game stage: 0 = Pre-flop, 1 = Flop, 2 = Turn, 3 = River, 4 = Showdown
    var gameStage by remember { mutableStateOf(0) }

    val suits = listOf("♠", "♣", "♥", "♦")
    val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")

    fun evaluateHand(handAndCommunity: List<String>): Pair<Int, String> {
        // Standard heads up card combinations rating
        // High Card = 1, Pair = 2, Two Pair = 3, Trips = 4, Straight = 5, Flush = 6, FullHouse = 7, Quads = 8, StraightFlush = 9
        val ranksInPlay = handAndCommunity.map { it.dropLast(1) }
        val suitsInPlay = handAndCommunity.map { it.last().toString() }
        
        val rankValues = mapOf("2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14)
        
        val sortedVals = ranksInPlay.map { rankValues[it] ?: 0 }.sortedDescending()
        
        val hasFlush = suitsInPlay.groupBy { it }.any { it.value.size >= 5 }
        
        val straightCheck = sortedVals.distinct()
        var hasStraight = false
        var straightHigh = 0
        if (straightCheck.size >= 5) {
            for (i in 0..straightCheck.size - 5) {
                if (straightCheck[i] - straightCheck[i + 4] == 4) {
                    hasStraight = true
                    straightHigh = straightCheck[i]
                    break
                }
            }
            // Low Ace straight (A-2-3-4-5)
            if (straightCheck.contains(14) && straightCheck.contains(2) && straightCheck.contains(3) && straightCheck.contains(4) && straightCheck.contains(5)) {
                hasStraight = true
                straightHigh = 5
            }
        }

        val counts = ranksInPlay.groupBy { it }.mapValues { it.value.size }
        val pairs = counts.filter { it.value == 2 }.keys.toList()
        val trips = counts.filter { it.value == 3 }.keys.toList()
        val quads = counts.filter { it.value == 4 }.keys.toList()

        return when {
            hasFlush && hasStraight -> Pair(9, if (isRu) "Стрит-Флэш!" else "Straight Flush!")
            quads.isNotEmpty() -> Pair(8, if (isRu) "Каре!" else "Four of a Kind!")
            trips.isNotEmpty() && pairs.isNotEmpty() -> Pair(7, if (isRu) "Фулл-Хаус!" else "Full House!")
            hasFlush -> Pair(6, if (isRu) "Флэш!" else "Flush!")
            hasStraight -> Pair(5, if (isRu) "Стрит!" else "Straight!")
            trips.isNotEmpty() -> Pair(4, if (isRu) "Тройка!" else "Three of a Kind!")
            pairs.size >= 2 -> Pair(3, if (isRu) "Две Пары!" else "Two Pairs!")
            pairs.size == 1 -> Pair(2, if (isRu) "Пара!" else "One Pair!")
            else -> Pair(1, if (isRu) "Старшая Карта" else "High Card")
        }
    }

    fun dealCard(): String {
        return ranks.random() + suits.random()
    }

    fun cleanHands() {
        playerHand.clear()
        botHand.clear()
        communityCards.clear()
    }

    fun startPoker() {
        if (userCoins < betAmt) {
            pokerStatusText = if (isRu) "НЕДОСТАТОЧНО МОНЕТ" else "NOT ENOUGH COINS"
            return
        }
        viewModel.vibrate(40)
        cleanHands()
        viewModel.updateCoins(userCoins - betAmt)
        
        playerHand.add(dealCard())
        playerHand.add(dealCard())
        
        val bCard1 = dealCard()
        var bCard2 = dealCard()
        val prVal = mapOf("2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9, "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14)
        fun checkWeak(c1: String, c2: String): Boolean {
            val r1 = prVal[c1.dropLast(1)] ?: 0
            val r2 = prVal[c2.dropLast(1)] ?: 0
            return r1 < 8 && r2 < 8
        }
        var finalBotCard1 = bCard1
        var finalBotCard2 = bCard2
        if (checkWeak(finalBotCard1, finalBotCard2) && kotlin.random.Random.nextFloat() < 0.82f) {
            var attempts = 0
            while (attempts < 5) {
                val temp1 = dealCard()
                val temp2 = dealCard()
                if (!checkWeak(temp1, temp2)) {
                    finalBotCard1 = temp1
                    finalBotCard2 = temp2
                    break
                }
                attempts++
            }
        }
        botHand.add(finalBotCard1)
        botHand.add(finalBotCard2)
        
        potAmt = betAmt * 2
        gameStage = 0
        inPokerGame = true
        pokerStatusText = if (isRu) "РАУНД ПРЕФЛОП. Бот коллировал ставку в $betAmt." else "ROUND PRE-FLOP. Bot matched your deposit of $betAmt."
    }

    fun nextTurn() {
        if (!inPokerGame) return
        viewModel.vibrate(30)
        when (gameStage) {
            0 -> { // Deal Flop (3 cards)
                communityCards.add(dealCard())
                communityCards.add(dealCard())
                communityCards.add(dealCard())
                gameStage = 1
                pokerStatusText = if (isRu) "ФЛОП РАЗДАН. Сделай ставку или чек!" else "FLOP DEALT. Select next bet or check!"
            }
            1 -> { // Deal Turn (1 card)
                communityCards.add(dealCard())
                gameStage = 2
                pokerStatusText = if (isRu) "ТЁРН РАЗДАН!" else "TURN DEALT!"
            }
            2 -> { // Deal River (1 card)
                communityCards.add(dealCard())
                gameStage = 3
                pokerStatusText = if (isRu) "РИВЕР РАЗДАН! Финальный раунд раскрытия!" else "RIVER DEALT! Press Showdown!"
            }
            3 -> { // Showdown
                gameStage = 4
                inPokerGame = false
                val playerResult = evaluateHand(playerHand + communityCards)
                val botResult = evaluateHand(botHand + communityCards)
                
                if (playerResult.first > botResult.first) {
                    pokerStatusText = if (isRu) "ВЫ ВЫИГРАЛИ БАНК $potAmt 🪙! Ваша комбинация: ${playerResult.second}" 
                                       else "YOU WIN POT $potAmt 🪙! Your Hand: ${playerResult.second}"
                    viewModel.updateCoins(userCoins + potAmt)
                    viewModel.vibrate(150)
                } else if (playerResult.first < botResult.first) {
                    pokerStatusText = if (isRu) "БОТ ПОБЕДИЛ с комбинацией: ${botResult.second}" 
                                       else "BOT TAKES BANK with hand: ${botResult.second}"
                    viewModel.vibrate(80)
                } else {
                    pokerStatusText = if (isRu) "СПЛИТ ПОТ! Равные комбинации: ${playerResult.second}" 
                                       else "SPLIT POT! Both players hold equivalent hands: ${playerResult.second}"
                    viewModel.updateCoins(userCoins + potAmt / 2)
                    viewModel.vibrate(50)
                }
            }
        }
    }

    fun raisePoker() {
        if (userCoins < betAmt) {
            pokerStatusText = if (isRu) "НЕ ХВАТАЕТ МОНЕТ ДЛЯ ПОВЫШЕНИЯ" else "NOT ENOUGH COINS TO RAISE"
            return
        }
        viewModel.updateCoins(userCoins - betAmt)
        potAmt += betAmt * 2
        nextTurn()
    }

    fun foldPoker() {
        inPokerGame = false
        gameStage = 0
        pokerStatusText = if (isRu) "ВЫ СБРОСИЛИ КАРТЫ. Банк ушел боту." else "YOU FOLD. Bot claims the pot."
        viewModel.vibrate(30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bot Cards View
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isRu) "ОППОНЕНТ (КИБЕР-БОТ)" else "OPPONENT (CYBER-BOT)", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                botHand.forEach { card ->
                    if (gameStage < 4) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 70.dp)
                                .background(PureBlack)
                                .border(1.5.dp, TextGray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", color = TextGray, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        PlayingCardView(card)
                    }
                }
            }
        }

        // Community Cards & Pot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${if (isRu) "ТЕКУЩИЙ БАНК: " else "CURRENT POT: "} $potAmt 🪙",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (communityCards.isEmpty()) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 58.dp)
                                .background(PureBlack)
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = AccentGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    communityCards.forEach { card -> ReadyCard(card) }
                    repeat(5 - communityCards.size) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 58.dp)
                                .background(PureBlack)
                                .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = AccentGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = pokerStatusText,
                color = PureWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        // Player Cards View
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                playerHand.forEach { card -> PlayingCardView(card) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isRu) "ТВОИ КАРТЫ" else "YOUR HOLE CARDS", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Controls
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!inPokerGame) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (betAmt > 10) { betAmt -= 10; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = PureWhite)
                    }
                    Text(
                        text = "${if (isRu) "СТАВКА: " else "BET: "} $betAmt 🪙",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (betAmt + 10 <= userCoins) { betAmt += 10; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite)
                    }
                    Button(
                        onClick = {
                            betAmt = userCoins.coerceAtLeast(10)
                            viewModel.vibrate(40)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "ОЛЛ-ИН" else "ALL IN",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { startPoker() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text(if (isRu) "НАЧАТЬ РАЗДАЧУ" else "START MATCH", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { foldPoker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Text(if (isRu) "ФОЛД" else "FOLD", fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { nextTurn() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Text(
                            text = if (gameStage == 3) {
                                if (isRu) "РАСКРЫТЬ" else "SHOWDOWN"
                            } else {
                                if (isRu) "ЧЕК" else "CHECK"
                            }, 
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Button(
                        onClick = { raisePoker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                    ) {
                        Text(if (isRu) "БЕТ" else "RAISE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadyCard(card: String) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 58.dp)
            .background(PureBlack)
            .border(1.dp, PureWhite, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(card.take(card.length - 1), color = PureWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(card.last().toString(), color = PureWhite, fontSize = 14.sp)
        }
    }
}


// ==========================================
// 3. DURAK GAME WITH TERM BOT IMPLEMENTATION
// ==========================================
@Composable
fun DurakGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    DurakGameComponent(
        viewModel = viewModel,
        userCoins = userCoins,
        isRu = isRu,
        isCasinoMode = true
    )
}


// ==========================================
// 4. SYNAPTIC HORSE RACING GAME
// ==========================================
data class HorseRunner(
    val name: String,
    val avatarUrl: String?,
    val decorationId: Int?,
    val payoutMulti: Double,
    var progress: Float, // 0.0 to 100.0
    val speedFactor: Float
)

fun generateRandomHorses(allUsers: List<com.example.data.UserEntity>, isRu: Boolean): List<HorseRunner> {
    val aiBots = allUsers.filter { it.isAi && it.id != "user" }
    val selectedBots = if (aiBots.size >= 4) {
        aiBots.shuffled().take(4)
    } else {
        val fallbackList = mutableListOf<com.example.data.UserEntity>()
        fallbackList.addAll(aiBots)
        val defaultAvatars = listOf(
            "https://api.dicebear.com/7.x/bottts/svg?seed=CoalDust",
            "https://api.dicebear.com/7.x/bottts/svg?seed=TechStrike",
            "https://api.dicebear.com/7.x/bottts/svg?seed=AlphaCypher",
            "https://api.dicebear.com/7.x/bottts/svg?seed=DarkHorizon"
        )
        val defaultNames = if (isRu) {
            listOf("КаменныйУзел", "РобоКрикун", "КиберВсадник", "ЛогикЯдра")
        } else {
            listOf("StoneNode", "RoboShouter", "CyberRider", "CoreLogician")
        }
        val defaultHandles = listOf("@stone_node", "@shouter", "@cyber_rider", "@core_logic")
        for (i in 0 until 4) {
            fallbackList.add(
                com.example.data.UserEntity(
                    id = "bot_fallback_$i",
                    username = defaultNames[i % defaultNames.size],
                    handle = defaultHandles[i % defaultHandles.size],
                    avatarUrl = defaultAvatars[i % defaultAvatars.size],
                    bio = "Suborbital racer",
                    isAi = true,
                    followersCount = 100,
                    followingCount = 100,
                    trustScore = 95
                )
            )
        }
        fallbackList.shuffled().take(4)
    }

    val multis = listOf(1.8, 3.0, 4.5, 8.0)
    val speeds = listOf(1.05f, 0.98f, 0.92f, 0.78f)
    
    return selectedBots.mapIndexed { idx, bot ->
        val hasDec = kotlin.random.Random.nextFloat() < 0.85f
        val randomDecId = if (hasDec) {
            if (kotlin.random.Random.nextFloat() < 0.25f) kotlin.random.Random.nextInt(201, 211) else kotlin.random.Random.nextInt(1, 201)
        } else {
            null
        }
        val suffix = if (idx == 0) " (FAV)" else ""
        HorseRunner(
            name = "${bot.username} / ${bot.handle}$suffix",
            avatarUrl = bot.avatarUrl,
            decorationId = randomDecId,
            payoutMulti = multis[idx],
            progress = 0f,
            speedFactor = speeds[idx]
        )
    }
}

@Composable
fun HorseRacingGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    val allUsers by viewModel.allUsers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var betAmount by remember { mutableStateOf(20) }
    var selectedHorseIdx by remember { mutableStateOf(0) }
    var inRace by remember { mutableStateOf(false) }
    
    val horses = remember {
        mutableStateListOf<HorseRunner>().apply {
            addAll(generateRandomHorses(emptyList(), isRu))
        }
    }
    
    LaunchedEffect(allUsers) {
        if (allUsers.isNotEmpty() && !inRace) {
            horses.clear()
            horses.addAll(generateRandomHorses(allUsers, isRu))
        }
    }

    var raceStatusText by remember { mutableStateOf("") }

    fun startRace() {
        if (userCoins < betAmount) {
            raceStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ" else "NOT ENOUGH DEPOSITS"
            return
        }
        viewModel.updateCoins(userCoins - betAmount)
        viewModel.vibrate(50)
        
        // Reset progresses
        horses.forEach { h -> h.progress = 0f }
        inRace = true
        raceStatusText = if (isRu) "ИНТЕЛЛЕКТУАЛЬНЫЕ КОНИ ВЫШЛИ НА ТРЕК..." else "CYBER-STEEDS LAUNCHED ON THE NEURAL TRAIL..."

        scope.launch {
            while (horses.all { h -> h.progress < 100f }) {
                delay(120)
                // Random haptic ticks
                if (Random.nextInt(5) == 1) viewModel.vibrate(8)
                
                // Advance horses
                horses.forEachIndexed { index, runner ->
                    val move = Random.nextFloat() * 4.5f * runner.speedFactor
                    horses[index] = runner.copy(progress = (runner.progress + move).coerceAtMost(100f))
                }
            }
            
            // Declare winner
            val winnerIndex = horses.indexOfMaxBy { it.progress }
            val winningHorse = horses[winnerIndex]
            
            inRace = false
            if (winnerIndex == selectedHorseIdx) {
                val winEarnings = (betAmount * winningHorse.payoutMulti).toInt()
                raceStatusText = if (isRu) "🎁 ТВОЙ СКАКУН ПРИБЕЖАЛ ПЕРВЫМ! Ваша выплата: $winEarnings монет!" 
                                   else "🎁 YOUR HORSE CROSSED THE LINE FIRST! Claimed: $winEarnings coins!"
                viewModel.updateCoins(userCoins + winEarnings)
                viewModel.vibrate(180)
            } else {
                raceStatusText = if (isRu) "ПОРАЖЕНИЕ. Первым приехал скакун ${winningHorse.name}. Попробуй снова!" 
                                   else "DEFEAT. The victor was ${winningHorse.name}. Recalibrate your stakes!"
                viewModel.vibrate(80)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isRu) "ТЕКУЩИЕ ЗАБЕГИ И КОЭФФИЦИЕНТЫ" else "TELEMETRY CURVES & RATINGS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGray)
                    .border(1.dp, BorderGray)
                    .padding(12.dp)
            ) {
                horses.forEachIndexed { index, runner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !inRace) { selectedHorseIdx = index; viewModel.vibrate(20) }
                            .background(if (selectedHorseIdx == index) Color(0xFF151515) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Avatar with decoration as requested!
                        com.example.ui.screens.AvatarWithDecoration(
                            avatarUrl = runner.avatarUrl,
                            decorationId = runner.decorationId,
                            sizeDp = 28,
                            borderWidthDp = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = runner.name,
                            color = if (selectedHorseIdx == index) PureWhite else TextGray,
                            fontSize = 10.sp,
                            fontWeight = if (selectedHorseIdx == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(130.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(BorderGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(runner.progress / 100f)
                                    .background(if (selectedHorseIdx == index) AlertYellow else PureWhite)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${runner.progress.toInt()}%",
                            color = if (selectedHorseIdx == index) AlertYellow else PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Text(
            text = raceStatusText,
            color = PureWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp)
        )

        // Select Horse
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!inRace) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "ВЫБЕРИ СКАКУНА ДЛЯ СТАВКИ:" else "CHOOSE CYBER STEED TO STAKE:",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!inRace) {
                        Text(
                            text = if (isRu) "🔄 ОБНОВИТЬ КАНДИДАТОВ" else "🔄 SWAP COMPETITORS",
                            color = AlertYellow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                viewModel.vibrate(30)
                                horses.clear()
                                horses.addAll(generateRandomHorses(allUsers, isRu))
                            }
                        )
                    } else {
                        Text(
                            text = if (isRu) "⚡ ГОНКА ИДЕТ..." else "⚡ RACE IN PROGRESS...",
                            color = AlertRed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    horses.forEachIndexed { idx, runner ->
                        Surface(
                            onClick = { selectedHorseIdx = idx; viewModel.vibrate(20) },
                            border = BorderStroke(1.dp, if (selectedHorseIdx == idx) PureWhite else BorderGray),
                            color = if (selectedHorseIdx == idx) DeepGray else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            val displayName = runner.name.substringBefore(" / ")
                            Text(
                                text = "$displayName\n[x${runner.payoutMulti}]",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                // Bet slider / controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (betAmount > 10) { betAmount -= 10; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = PureWhite)
                    }
                    Text(
                        text = "${if (isRu) "СТАВКА: " else "BET: "}$betAmount 🪙",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (betAmount + 10 <= userCoins) { betAmount += 10; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite)
                    }
                    Button(
                        onClick = {
                            betAmount = userCoins.coerceAtLeast(10)
                            viewModel.vibrate(40)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "ОЛЛ-ИН" else "ALL IN",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { startRace() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text(if (isRu) "ЗАПУСТИТЬ ТЕЛЕМЕТРИЮ ЗАБЕГА" else "START NEURAL TELEMETRY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator(
                    color = PureWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(32.dp)
                )
            }
        }
    }
}

inline fun <T> List<T>.indexOfMaxBy(selector: (T) -> Float): Int {
    if (isEmpty()) return -1
    var maxIndex = 0
    var maxValue = selector(first())
    for (i in 1 until size) {
        val value = selector(this[i])
        if (value > maxValue) {
            maxIndex = i
            maxValue = value
        }
    }
    return maxIndex
}


// ==========================================
// 5. HIGH-FIDELITY ROULETTE GAME IMPLEMENTATION
// ==========================================
@Composable
fun RouletteGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    val scope = rememberCoroutineScope()
    var chipValue by remember { mutableStateOf(10) }
    var activeBets by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var inSpin by remember { mutableStateOf(false) }

    // Authentic roulette red numbers set according to classic casino rules! (Request 5 / 6)
    val rouletteRedNumbers = remember { setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36) }
    
    var lastSpinNumber by remember { mutableStateOf<Int?>(null) }
    var rouletteStatusText by remember { mutableStateOf("") }
    var wheelPointerAng by remember { mutableStateOf(0f) }
    var ballAngle by remember { mutableStateOf(0f) }

    fun startRouletteSpin() {
        val totalBetAmount = activeBets.values.sum()
        if (totalBetAmount <= 0) {
            rouletteStatusText = if (isRu) "МИНИМАЛЬНАЯ СТАВКА НА СТОЛЕ: 5 МОНЕТ!" else "MINIMUM TABLE STAKE IS 5 COINS!"
            return
        }
        if (userCoins < totalBetAmount) {
            rouletteStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ ДЛЯ ВСЕХ СТАВОК" else "NOT ENOUGH COINS FOR ALL ACTIVE PLACED CHIPS"
            return
        }
        viewModel.updateCoins(userCoins - totalBetAmount)
        viewModel.vibrate(40)
        
        inSpin = true
        rouletteStatusText = if (isRu) "КОЛЕСО РУЛЕТКИ ЗАПУЩЕНО..." else "ROULETTE ROTATOR ENGAGED..."

        scope.launch {
            val finalWinningNumber = Random.nextInt(37)
            
            // One segment of 37 is (360f / 37f).
            val targetSegmentAngle = finalWinningNumber * (360f / 37f)
            // Final angle of the wheel:
            val finalWheelAngle = 360f - targetSegmentAngle + 270f
            // Let's spin 4 times completely + the final target angle:
            val totalRotation = 360f * 4f + finalWheelAngle
            
            // Decelerating rotation animation (Request 5)
            val steps = 90
            for (step in 1..steps) {
                val t = step.toFloat() / steps
                val easing = 1f - (1f - t) * (1f - t) * (1f - t) // cubic ease out
                wheelPointerAng = easing * totalRotation
                // Ball spins 2x faster in the opposite direction for rich realistic simulation!
                ballAngle = -easing * totalRotation * 2.1f
                
                if (step % 5 == 0) {
                    viewModel.vibrate(5)
                }
                delay(20)
            }
            
            // Wait a split second to finalize
            delay(120)
            lastSpinNumber = finalWinningNumber
            
            val winningColor = when {
                finalWinningNumber == 0 -> "zero"
                rouletteRedNumbers.contains(finalWinningNumber) -> "red"
                else -> "black"
            }
            
            val isEven = finalWinningNumber != 0 && finalWinningNumber % 2 == 0
            val isOdd = finalWinningNumber % 2 == 1

            var totalWinnings = 0
            var wonAny = false
            
            activeBets.forEach { (bet, amt) ->
                var won = false
                var payoutMultiplier = 0
                
                if (bet.startsWith("num_")) {
                    val chosenNum = bet.substringAfter("num_").toIntOrNull() ?: -1
                    if (finalWinningNumber == chosenNum) {
                        won = true
                        payoutMultiplier = 36
                    }
                } else {
                    when (bet) {
                        "red" -> {
                            if (winningColor == "red") {
                                won = true
                                payoutMultiplier = 2
                            }
                        }
                        "black" -> {
                            if (winningColor == "black") {
                                won = true
                                payoutMultiplier = 2
                            }
                        }
                        "even" -> {
                            if (isEven) {
                                won = true
                                payoutMultiplier = 2
                            }
                        }
                        "odd" -> {
                            if (isOdd) {
                                won = true
                                payoutMultiplier = 2
                            }
                        }
                        "zero" -> {
                            if (finalWinningNumber == 0) {
                                won = true
                                payoutMultiplier = 36
                            }
                        }
                    }
                }
                if (won) {
                    wonAny = true
                    totalWinnings += amt * payoutMultiplier
                }
            }

            inSpin = false
            if (wonAny) {
                rouletteStatusText = if (isRu) {
                    "🎉 ВЫПАЛО ЧИСЛО $finalWinningNumber! Твой выигрыш: $totalWinnings монет!"
                } else {
                    "🎉 HIT FIELD $finalWinningNumber! Won total: $totalWinnings coins!"
                }
                viewModel.updateCoins(userCoins - totalBetAmount + totalWinnings)
                viewModel.vibrate(180)
            } else {
                val colText = if (winningColor == "red") (if (isRu) "КРАСНОЕ" else "RED") else if (winningColor == "black") (if (isRu) "ЧЕРНОЕ" else "BLACK") else "ZERO"
                rouletteStatusText = if (isRu) {
                    "ПРОИГРЫШ. Выпало число $finalWinningNumber ($colText). Потери: $totalBetAmount монет."
                } else {
                    "LOST. Rolled $finalWinningNumber ($colText). Lost: $totalBetAmount coins."
                }
                viewModel.vibrate(80)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isRu) "ЦВЕТНОЙ ЦЕНТРИФУЖНЫЙ ДИСК" else "ROTATING CLASSIC DISK",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Visual simulated spin wheel using Compose Canvas! (Request 5)
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(PureBlack, CircleShape)
                    .border(3.dp, BorderGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = size.width / 2
                    val radius = size.width / 2 - 6f
                    val sweep = 360f / 37f
                    
                    for (i in 0 until 37) {
                        val startAngle = wheelPointerAng + i * sweep
                        val color = when {
                            i == 0 -> AlertGreen
                            rouletteRedNumbers.contains(i) -> Color(0xFFFF3B30) // Rich Red (Request 5)
                            else -> CardGray // Rich Dark (Black)
                        }
                        
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            topLeft = androidx.compose.ui.geometry.Offset(center - radius, center - radius)
                        )

                        // Draw divider lines for extreme clear division (Request 5)
                        val angleRad = Math.toRadians(startAngle.toDouble())
                        val dX = center + radius * Math.cos(angleRad).toFloat()
                        val dY = center + radius * Math.sin(angleRad).toFloat()
                        drawLine(
                            color = PureBlack,
                            start = androidx.compose.ui.geometry.Offset(center, center),
                            end = androidx.compose.ui.geometry.Offset(dX, dY),
                            strokeWidth = 2f
                        )
                    }
                    
                    // Draw a visual separator circle inside
                    drawCircle(
                        color = PureBlack,
                        radius = radius * 0.55f,
                        center = androidx.compose.ui.geometry.Offset(center, center)
                    )

                    // Draw the rotating/resting roulette marble ball (Request 5)
                    val activeBallAngle = if (inSpin) ballAngle else 270f
                    val ballDistance = radius * 0.72f
                    val rads = Math.toRadians(activeBallAngle.toDouble())
                    val ballX = center + (ballDistance * Math.cos(rads)).toFloat()
                    val ballY = center + (ballDistance * Math.sin(rads)).toFloat()
                    
                    drawCircle(
                        color = if (inSpin) StarkWhite else AlertYellow,
                        radius = 9f,
                        center = androidx.compose.ui.geometry.Offset(ballX, ballY)
                    )
                    drawCircle(
                        color = PureWhite,
                        radius = 5f,
                        center = androidx.compose.ui.geometry.Offset(ballX, ballY)
                    )
                    
                    // Draw outer golden-style pointer arrow at the top pointing down
                    val pointerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center, 4f)
                        lineTo(center - 12f, 22f)
                        lineTo(center + 12f, 22f)
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = AlertYellow
                    )
                }
                
                // Outer circle fields displaying current/winning item
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = lastSpinNumber?.toString() ?: "🎡",
                        fontSize = 32.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (inSpin) {
                        Text(
                            text = if (isRu) "КРУТКА..." else "SPINNING...",
                            fontSize = 8.sp,
                            color = AlertYellow,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Big Clear Winner color indicator badge (Request 5)
            if (lastSpinNumber != null) {
                val winColor = when {
                    lastSpinNumber == 0 -> AlertGreen
                    rouletteRedNumbers.contains(lastSpinNumber!!) -> Color(0xFFFF3B30)
                    else -> PureBlack
                }
                val winColorText = when {
                    lastSpinNumber == 0 -> if (isRu) "ЗЕРО" else "ZERO"
                    rouletteRedNumbers.contains(lastSpinNumber!!) -> if (isRu) "КРАСНОЕ" else "RED"
                    else -> if (isRu) "ЧЕРНОЕ" else "BLACK"
                }
                val winTextColor = PureWhite
                
                Surface(
                    color = winColor,
                    border = BorderStroke(2.dp, PureWhite),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "${if (isRu) "ВЫПАЛО:" else "ROLLED:"} $lastSpinNumber ($winColorText)",
                        color = winTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Text(
            text = rouletteStatusText,
            color = PureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Select Bet Type
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!inSpin) {
                // Display current active bets summary / stake:
                val totalBetAmount = activeBets.values.sum()
                if (totalBetAmount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardGray),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRu) "АКТИВНЫЕ СТАВКИ: $totalBetAmount 🪙" else "ACTIVE STAKES: $totalBetAmount 🪙",
                                    color = AlertYellow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { activeBets = emptyMap(); viewModel.vibrate(30) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF3B30), contentColor = Color(0xFFFF5252)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(if (isRu) "СБРОСИТЬ ВСЕ" else "CLEAR ALL", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            // Small list of active bets
                            val activeBetsListText = activeBets.entries.map { (bet, amt) ->
                                val label = when {
                                    bet == "red" -> if (isRu) "Красное" else "Red"
                                    bet == "black" -> if (isRu) "Черное" else "Black"
                                    bet == "even" -> if (isRu) "Чет" else "Even"
                                    bet == "odd" -> if (isRu) "Нечет" else "Odd"
                                    bet == "zero" -> "0"
                                    bet.startsWith("num_") -> bet.substringAfter("num_")
                                    else -> bet
                                }
                                "$label: $amt"
                            }.joinToString(", ")
                            Text(
                                text = activeBetsListText,
                                color = TextGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Interactive roulette betting grid 0-36 (Request 4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepGray)
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    val currentZeroBet = activeBets["num_0"] ?: activeBets["zero"] ?: 0
                    val zeroLabel = if (currentZeroBet > 0) "0 ($currentZeroBet 🪙)" else "0"
                    
                    Text(
                        text = if (isRu) "ОРИГИНАЛЬНОЕ ПОЛЕ СТАВОК (0-36) [36х]:" else "ORIGINAL BETTING GRID (0-36) [36x]:",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    // Green Zero Button
                    Surface(
                        onClick = {
                            val current = activeBets["num_0"] ?: 0
                            if (userCoins >= (totalBetAmount + chipValue)) {
                                activeBets = activeBets + ("num_0" to current + chipValue)
                                viewModel.vibrate(15)
                            } else {
                                viewModel.vibrate(40)
                                rouletteStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ ДЛЯ СТАВКИ!" else "OUT OF COINS FOR THIS PLACEMENT!"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        color = if (currentZeroBet > 0) AlertGreen else Color(0x1A00FF66),
                        border = BorderStroke(1.dp, if (currentZeroBet > 0) PureWhite else AlertGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(zeroLabel, color = PureWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Compact 1-36 block with a grid using real colors
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 12) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 1..3) {
                                    val num = row * 3 + col
                                    val isRedNum = rouletteRedNumbers.contains(num)
                                    val cellColor = if (isRedNum) Color(0xFFFF3B30) else PureBlack
                                    val cellBorderColor = if (isRedNum) Color(0xFFFF8A80) else BorderGray
                                    
                                    val currentNumBet = activeBets["num_$num"] ?: 0
                                    val hasBet = currentNumBet > 0
                                    
                                    val finalColor = if (hasBet) AlertYellow else cellColor
                                    val finalTextColor = if (hasBet) PureBlack else PureWhite
                                    
                                    val keyLabel = if (currentNumBet > 0) "$num ($currentNumBet 🪙)" else num.toString()
                                    
                                    Surface(
                                        onClick = {
                                            val current = activeBets["num_$num"] ?: 0
                                            if (userCoins >= (totalBetAmount + chipValue)) {
                                                activeBets = activeBets + ("num_$num" to current + chipValue)
                                                viewModel.vibrate(15)
                                            } else {
                                                viewModel.vibrate(40)
                                                rouletteStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ ДЛЯ СТАВКИ!" else "OUT OF COINS FOR THIS PLACEMENT!"
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        color = finalColor,
                                        border = BorderStroke(1.dp, if (hasBet) PureWhite else cellBorderColor),
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = keyLabel,
                                                color = finalTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sector bets selection
                Text(
                    text = if (isRu) "СТАВКА НА СЕКТОРА [2х]:" else "STAKE ON SECTORS [2x]:",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sectors = listOf("red", "black", "even", "odd")
                    sectors.forEach { sector ->
                        val text = when(sector) {
                            "red" -> if (isRu) "КРАСНЫЕ" else "RED"
                            "black" -> if (isRu) "ЧЕРНЫЕ" else "BLACK"
                            "even" -> if (isRu) "ЧЕТ" else "EVEN"
                            "odd" -> if (isRu) "НЕЧЕТ" else "ODD"
                            else -> sector
                        }
                        
                        val sectorBg = when(sector) {
                            "red" -> Color(0x33FF3B30)
                            "black" -> Color(0x33FFFFFF)
                            else -> Color.Transparent
                        }
                        
                        val currentSectorBet = activeBets[sector] ?: 0
                        val hasSectorBet = currentSectorBet > 0
                        val sectorLabel = if (currentSectorBet > 0) "$text ($currentSectorBet 🪙)" else text
                        
                        Surface(
                            onClick = {
                                val current = activeBets[sector] ?: 0
                                if (userCoins >= (totalBetAmount + chipValue)) {
                                    activeBets = activeBets + (sector to current + chipValue)
                                    viewModel.vibrate(20)
                                } else {
                                    viewModel.vibrate(40)
                                    rouletteStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ ДЛЯ СТАВКИ!" else "OUT OF COINS FOR THIS PLACEMENT!"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, if (hasSectorBet) PureWhite else BorderGray),
                            color = if (hasSectorBet) AlertYellow else sectorBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = sectorLabel,
                                color = if (hasSectorBet) PureBlack else PureWhite,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Active Chip Selection Tool
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isRu) "ВЫБЕРИТЕ НОМИНАЛ ФИШЕК ДЛЯ СТАВКИ:" else "SELECT CHIP SIZE TO PLACE:",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val chipSizes = listOf(5, 10, 25, 100, 500)
                        chipSizes.forEach { cValue ->
                            val isSelectedChip = chipValue == cValue
                            Surface(
                                onClick = { chipValue = cValue; viewModel.vibrate(10) },
                                color = if (isSelectedChip) AlertYellow else CardGray,
                                border = BorderStroke(1.5.dp, if (isSelectedChip) PureWhite else BorderGray),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.size(45.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = cValue.toString(),
                                        color = if (isSelectedChip) PureBlack else PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = { startRouletteSpin() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    val totalStake = activeBets.values.sum()
                    val trigText = if (isRu) {
                        if (totalStake > 0) "ЗАПУСТИТЬ КРУТИЛКУ ($totalStake 🪙)" else "ЗАПУСТИТЬ КРУТИЛКУ"
                    } else {
                        if (totalStake > 0) "ENGAGE SPIN TRIGGER ($totalStake 🪙)" else "ENGAGE SPIN TRIGGER"
                    }
                    Text(trigText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator(
                    color = PureWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                        .size(32.dp)
                )
            }
        }
    }
}


// ==========================================
// 6. SLOT MACHINE GAME IMPLEMENTATION
// ==========================================
@Composable
fun SlotsGame(viewModel: SocialViewModel, userCoins: Int, isRu: Boolean) {
    val scope = rememberCoroutineScope()
    var betSlots by remember { mutableStateOf(10) }
    var rolling by remember { mutableStateOf(false) }

    val reelSymbols = listOf("🪙", "🤖", "💎", "💀", "⚠️", "🦾", "🎰", "🔋", "🔑", "🛡️", "🔥", "🔮")
    var reel1 by remember { mutableStateOf("🎰") }
    var reel2 by remember { mutableStateOf("🎰") }
    var reel3 by remember { mutableStateOf("🎰") }

    var slotsStatusText by remember { mutableStateOf("") }

    fun processSlots() {
        if (userCoins < betSlots) {
            slotsStatusText = if (isRu) "НЕДОСТАТОЧНО СРЕДСТВ" else "NOT ENOUGH DEPOSITS"
            return
        }
        viewModel.updateCoins(userCoins - betSlots)
        viewModel.vibrate(50)
        rolling = true
        slotsStatusText = if (isRu) "БАРАБАНЫ ЗАПУЩЕНЫ..." else "CYPHER ROLLS DEPLOYED..."

        scope.launch {
            repeat(12) { i ->
                reel1 = reelSymbols.random()
                reel2 = reelSymbols.random()
                reel3 = reelSymbols.random()
                viewModel.vibrate(10)
                delay(50 + (i * 20).toLong())
            }
            
            reel1 = reelSymbols.random()
            reel2 = reelSymbols.random()
            reel3 = reelSymbols.random()
            rolling = false

            // Compute winnings
            var multiplier = 0
            if (reel1 == reel2 && reel2 == reel3) {
                multiplier = when (reel1) {
                    "💎" -> 25
                    "🤖" -> 15
                    "🪙" -> 10
                    "🎰" -> 40
                    else -> 6
                }
            } else if (reel1 == reel2 || reel2 == reel3 || reel1 == reel3) {
                // In real casinos, a 2-reel match has only a 35% chance to return a push (refund your bet), keeping the slots tight and profitable!
                if (Random.nextFloat() < 0.35f) {
                    multiplier = 1
                } else {
                    multiplier = 0
                }
            }

            if (multiplier > 0) {
                val matchCoins = betSlots * multiplier
                slotsStatusText = if (isRu) "🚀 ПОЛНОЕ СОВПАДЕНИЕ! MULTIPLIER x$multiplier! ВЫИГРАНО $matchCoins МОНЕТ!" 
                                   else "🚀 MATCH! POT x$multiplier! WON $matchCoins COINS!"
                viewModel.updateCoins(userCoins + matchCoins)
                viewModel.vibrate(180)
            } else {
                slotsStatusText = if (isRu) "ПРОМАХ. Потеряно $betSlots монет. Крути еще раз!" 
                                   else "NO ALIGNMENTS. Lost $betSlots coins. Keep spinning!"
                viewModel.vibrate(80)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isRu) "МАТРИЧНЫЙ ТРИПЛЕТНЫЙ РЕЕСТР" else "MATRIX REEL INDEX TRIO",
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Visual Slots Reels
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlotReelBox(reel1)
                SlotReelBox(reel2)
                SlotReelBox(reel3)
            }
        }

        Text(
            text = slotsStatusText,
            color = PureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.padding(12.dp)
        )

        // Bet and spin controls
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!rolling) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (betSlots > 5) { betSlots -= 5; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = PureWhite)
                    }
                    Text(
                        text = "${if (isRu) "СТАВКА: " else "BET: "}$betSlots 🪙",
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { if (betSlots + 5 <= userCoins) { betSlots += 5; viewModel.vibrate(10) } }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite)
                    }
                    Button(
                        onClick = {
                            betSlots = userCoins.coerceAtLeast(5)
                            viewModel.vibrate(40)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "ОЛЛ-ИН" else "ALL IN",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { processSlots() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
                ) {
                    Text(if (isRu) "ДЁРНУТЬ РЫЧАГ nOG-SLOT" else "PULL CYBER LEVER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator(
                    color = PureWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SlotReelBox(symbol: String) {
    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 90.dp)
            .background(PureBlack)
            .border(2.dp, PureWhite, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 36.sp)
    }
}
