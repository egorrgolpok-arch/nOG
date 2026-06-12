package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocialViewModel
import com.example.ui.theme.AlertYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Color palette matching terminal dark cyber theme
private val PureBlack = Color(0xFF000000)
private val PureWhite = Color(0xFFFFFFFF)
private val DeepGray = Color(0xFF0D0D0D)
private val CardGray = Color(0xFF141414)
private val BorderGray = Color(0xFF222222)
private val TextGray = Color(0xFF888888)
private val AccentGray = Color(0xFF555555)
private val RedSuitColor = Color(0xFFFF4B4B)

data class DurakPlayerState(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val decorationId: Int?,
    val isHuman: Boolean,
    val cards: List<String> = emptyList(),
    val isOut: Boolean = false,
    val place: Int = 0 // Winning place: 1, 2, ...
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DurakGameComponent(
    viewModel: SocialViewModel,
    userCoins: Int,
    isRu: Boolean,
    isCasinoMode: Boolean,
    onDismiss: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val allUsers by viewModel.allUsers.collectAsState(initial = emptyList())
    
    // Configurable state before game starts
    var opponentCount by remember { mutableStateOf(3) } // 1 to 5 bot opponents
    var difficultyLevel by remember { mutableStateOf(if (isCasinoMode) "Hard" else "Medium") } // "Easy", "Medium", "Hard"
    var betAmount by remember { mutableStateOf(15) } // Casino mode bet
    
    // Game Active States
    var inDurak by remember { mutableStateOf(false) }
    val players = remember { mutableStateListOf<DurakPlayerState>() }
    val deck = remember { mutableStateListOf<String>() }
    var trumpCard by remember { mutableStateOf("") }
    var trumpSuit by remember { mutableStateOf("") }
    var remainingCardsCount by remember { mutableStateOf(0) }
    var durakText by remember { mutableStateOf("") }
    
    var currentAttackerId by remember { mutableStateOf("") }
    var currentDefenderId by remember { mutableStateOf("") }
    
    // Cards currently on the table for this round
    val activeAttackCards = remember { mutableStateListOf<String>() }
    val activeDefendCards = remember { mutableStateListOf<String>() }
    
    // Lobby seat preview helper
    val lobbyBots = remember { mutableStateListOf<DurakPlayerState>() }
    
    // Suits & values lists
    val suits = listOf("♠", "♣", "♥", "♦")
    val ranks36 = listOf("6", "7", "8", "9", "10", "J", "Q", "K", "A")
    val ranks52 = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A")
    
    fun getRanks(): List<String> {
        val totalPlayers = opponentCount + 1
        return if (totalPlayers > 4) ranks52 else ranks36
    }
    
    fun getCardValue(card: String): Int {
        val r = card.dropLast(1)
        return getRanks().indexOf(r)
    }
    
    // Re-roll lobby bots with random name, avatar & decorations as requested!
    fun rollLobbyBots() {
        if (inDurak) return
        lobbyBots.clear()
        val aiBots = allUsers.filter { it.isAi && it.id != "user" }
        val shuffledBots = if (aiBots.size >= opponentCount) {
            aiBots.shuffled().take(opponentCount)
        } else {
            val templates = mutableListOf<com.example.data.UserEntity>()
            templates.addAll(aiBots)
            val defAvatars = listOf(
                "https://api.dicebear.com/7.x/bottts/svg?seed=CyberDurak0",
                "https://api.dicebear.com/7.x/bottts/svg?seed=GlitchSteed1",
                "https://api.dicebear.com/7.x/bottts/svg?seed=QuantumWarden",
                "https://api.dicebear.com/7.x/bottts/svg?seed=NeonBuster",
                "https://api.dicebear.com/7.x/bottts/svg?seed=ZeroDay"
            )
            val defNames = if (isRu) {
                listOf("ПротоКрипер", "ФлешЮзер", "ХакерБот", "Синапс", "ДвойнойКлик")
            } else {
                listOf("ProtoCreeper", "FlashUser", "HackerBot", "SynapsePlayer", "DoubleClock")
            }
            val defHandles = listOf("@proto_creep", "@flash_user", "@hacker_bot", "@synapse", "@dbl_click")
            for (i in 0 until 5) {
                templates.add(
                    com.example.data.UserEntity(
                        id = "bot_durak_fallback_$i",
                        username = defNames[i % defNames.size],
                        handle = defHandles[i % defHandles.size],
                        avatarUrl = defAvatars[i % defAvatars.size],
                        bio = "Cyber opponent",
                        isAi = true,
                        followersCount = 50,
                        followingCount = 100,
                        trustScore = 90
                    )
                )
            }
            templates.shuffled().take(opponentCount)
        }
        
        shuffledBots.forEachIndexed { i, bot ->
            // Randomly assign a decoration out of the custom decoration portfolio!
            val hasDec = Random.nextFloat() < 0.8f
            val randomDecId = if (hasDec) {
                if (Random.nextFloat() < 0.25f) Random.nextInt(201, 211) else Random.nextInt(1, 201)
            } else {
                null
            }
            lobbyBots.add(
                DurakPlayerState(
                    id = bot.id,
                    name = bot.username,
                    avatarUrl = bot.avatarUrl,
                    decorationId = randomDecId,
                    isHuman = false
                )
            )
        }
    }
    
    // Auto re-roll lobby when opponentCount changes or when users are loaded
    LaunchedEffect(opponentCount, allUsers) {
        if (allUsers.isNotEmpty() && !inDurak) {
            rollLobbyBots()
        }
    }
    
    fun getNextActivePlayer(fromId: String): DurakPlayerState? {
        val idx = players.indexOfFirst { it.id == fromId }
        if (idx == -1) return null
        for (i in 1 until players.size) {
            val next = players[(idx + i) % players.size]
            if (!next.isOut && (next.cards.isNotEmpty() || deck.isNotEmpty())) {
                return next
            }
        }
        return null
    }
    
    fun refillHands() {
        val attIdx = players.indexOfFirst { it.id == currentAttackerId }
        if (attIdx == -1) return
        
        val refillOrder = mutableListOf<DurakPlayerState>()
        for (i in 0 until players.size) {
            refillOrder.add(players[(attIdx + i) % players.size])
        }
        
        val updatedPlayers = players.toMutableList()
        for (orderPlayer in refillOrder) {
            val pIdx = updatedPlayers.indexOfFirst { it.id == orderPlayer.id }
            if (pIdx == -1 || updatedPlayers[pIdx].isOut) continue
            
            val currentCards = updatedPlayers[pIdx].cards.toMutableList()
            while (currentCards.size < 6 && deck.isNotEmpty()) {
                currentCards.add(deck.removeAt(0))
            }
            updatedPlayers[pIdx] = updatedPlayers[pIdx].copy(cards = currentCards)
        }
        
        // Mark players out if hand is empty and deck is empty
        var placeCount = updatedPlayers.count { it.place > 0 }
        val updatedWithOut = updatedPlayers.map { p ->
            if (!p.isOut && p.cards.isEmpty() && deck.isEmpty()) {
                placeCount++
                p.copy(isOut = true, place = placeCount)
            } else {
                p
            }
        }
        
        remainingCardsCount = deck.size
        players.clear()
        players.addAll(updatedWithOut)
    }
    
    fun checkForEndGame(): Boolean {
        // Find how many players are remaining (still have cards)
        val activeCount = players.count { !it.isOut }
        if (activeCount <= 1) {
            inDurak = false
            val lastActive = players.find { !it.isOut }
            if (lastActive != null) {
                if (lastActive.isHuman) {
                    // Player lost! Player is Durak.
                    durakText = if (isRu) "💀 ТЫ ДУРАК! ТЫ ПРОИГРАЛ И ОСТАЛСЯ В ДУРАКАХ..." else "💀 GAME OVER! YOU LOST AND REMAINED THE FOOL (DURAK)."
                    if (isCasinoMode) {
                        // Lost bet is already deducted
                    }
                } else {
                    // Bot lost, Player escaped!
                    val wonStatus = if (isRu) "👑 ТЫ ВЫИГРАЛ! ${lastActive.name} ОСТАЛСЯ В ДУРАКАХ!" else "👑 VICTORY! ${lastActive.name} PLAYED FOOL!"
                    durakText = wonStatus
                    if (isCasinoMode) {
                        viewModel.updateCoins(userCoins + betAmount * 2)
                    }
                    viewModel.vibrate(150)
                }
            } else {
                // Draw
                durakText = if (isRu) "🤝 НИЧЬЯ! Все покинули игру удачно." else "🤝 DRAW ROUND! All players successfully exited."
                if (isCasinoMode) {
                    viewModel.updateCoins(userCoins + betAmount) // return bet
                }
            }
            return true
        }
        return false
    }
    
    fun selectCardByDifficulty(
        cards: List<String>,
        difficulty: String,
        trump: String,
        isAttack: Boolean
    ): String {
        if (cards.size == 1) return cards[0]
        
        val activeDiff = if (isCasinoMode) "Hard" else difficulty
        val nonTrumps = cards.filter { !it.endsWith(trump) }
        
        return when (activeDiff) {
            "Easy", "Легко" -> {
                cards.random()
            }
            "Hard", "Сложно" -> {
                if (isAttack) {
                    // Smart attack: play lowest rank card, prefer non-trumps
                    if (nonTrumps.isNotEmpty()) {
                        nonTrumps.minByOrNull { getCardValue(it) }!!
                    } else {
                        cards.minByOrNull { getCardValue(it) }!!
                    }
                } else {
                    // Smart defense: use cheapest valid card that is higher
                    if (nonTrumps.isNotEmpty()) {
                        nonTrumps.minByOrNull { getCardValue(it) }!!
                    } else {
                        cards.minByOrNull { getCardValue(it) }!!
                    }
                }
            }
            else -> { // "Medium"
                if (nonTrumps.isNotEmpty()) {
                    nonTrumps.minByOrNull { getCardValue(it) }!!
                } else {
                    cards.minByOrNull { getCardValue(it) }!!
                }
            }
        }
    }
    
    // Automated simulation of Bot playing against Bot or other events
    fun runBotTurnSimulation() {
        if (!inDurak) return
        scope.launch {
            delay(1200)
            if (!inDurak) return@launch
            
            val att = players.find { it.id == currentAttackerId }
            val def = players.find { it.id == currentDefenderId }
            
            if (att == null || def == null) return@launch
            
            if (!att.isHuman) {
                // CASE: Bot attacks!
                val botIdx = players.indexOfFirst { it.id == att.id }
                if (botIdx == -1) return@launch
                val hand = players[botIdx].cards
                
                val playable = if (activeAttackCards.isEmpty()) {
                    hand
                } else {
                    val ranksOnTable = (activeAttackCards + activeDefendCards).map { it.dropLast(1) }
                    hand.filter { it.dropLast(1) in ranksOnTable }
                }
                
                if (playable.isNotEmpty() && activeAttackCards.size < 6) {
                    // Bot attacks
                    val card = selectCardByDifficulty(playable, difficultyLevel, trumpSuit, isAttack = true)
                    val newHand = hand.toMutableList().apply { remove(card) }
                    players[botIdx] = players[botIdx].copy(cards = newHand)
                    activeAttackCards.add(card)
                    viewModel.vibrate(12)
                    durakText = if (isRu) "${att.name} атакует картой $card!" else "${att.name} plays attack: $card!"
                    
                    // Trigger defense
                    delay(1200)
                    if (def.isHuman) {
                        durakText = if (isRu) "${att.name} атаковал тебя картой $card! Защищайся козырем или картой той же масти старше." 
                                   else "${att.name} attacked you with $card! Defend or take cards."
                    } else {
                        // Bot defends against Bot
                        val defIdx = players.indexOfFirst { it.id == def.id }
                        val defHand = players[defIdx].cards
                        val defCandidates = defHand.filter { defCard ->
                            val defSuit = defCard.last().toString()
                            val attSuit = card.last().toString()
                            val defVal = getCardValue(defCard)
                            val attVal = getCardValue(card)
                            
                            if (defSuit == attSuit) {
                                defVal > attVal
                            } else {
                                defSuit == trumpSuit && attSuit != trumpSuit
                            }
                        }
                        
                        if (defCandidates.isNotEmpty()) {
                            val defCard = selectCardByDifficulty(defCandidates, difficultyLevel, trumpSuit, isAttack = false)
                            val newDefHand = defHand.toMutableList().apply { remove(defCard) }
                            players[defIdx] = players[defIdx].copy(cards = newDefHand)
                            activeDefendCards.add(defCard)
                            viewModel.vibrate(12)
                            durakText = if (isRu) "${def.name} защитился картой $defCard!" else "${def.name} defended with $defCard!"
                            
                            // Trigger next attack cycle from Bot or Bita
                            delay(1200)
                            if (playable.size <= 1 || Random.nextFloat() < 0.35f) {
                                // Bot calls Bita (beaten!)
                                durakText = if (isRu) "Бита! Игроки добирают карты." else "Beaten! Refilling hands."
                                activeAttackCards.clear()
                                activeDefendCards.clear()
                                refillHands()
                                if (!checkForEndGame()) {
                                    // Defender becomes new attacker clockwise
                                    val nextAttacker = currentDefenderId
                                    val nextDef = getNextActivePlayer(nextAttacker)
                                    currentAttackerId = nextAttacker
                                    currentDefenderId = nextDef?.id ?: ""
                                    durakText = if (isRu) "Очередь хода переходит к ${players.find { it.id == currentAttackerId }?.name}"
                                                else "Turn moves to ${players.find { it.id == currentAttackerId }?.name}"
                                }
                            } else {
                                // Bot throws another card!
                                runBotTurnSimulation()
                            }
                        } else {
                            // Bot can't defend and takes cards
                            durakText = if (isRu) "${def.name} не может защититься и забирает стол!" else "${def.name} cannot defend and claims the table!"
                            val finalHand = defHand + activeAttackCards + activeDefendCards
                            players[defIdx] = players[defIdx].copy(cards = finalHand)
                            activeAttackCards.clear()
                            activeDefendCards.clear()
                            refillHands()
                            if (!checkForEndGame()) {
                                // Skip defender's turn to attack, next person clockwise attacks
                                val nextAttacker = getNextActivePlayer(def.id)
                                val nextDef = nextAttacker?.let { getNextActivePlayer(it.id) }
                                currentAttackerId = nextAttacker?.id ?: ""
                                currentDefenderId = nextDef?.id ?: ""
                            }
                        }
                    }
                } else {
                    // No playable card, bot calls Beaten!
                    durakText = if (isRu) "Бита! Стол пуст." else "Beaten! Table cleared."
                    activeAttackCards.clear()
                    activeDefendCards.clear()
                    refillHands()
                    if (!checkForEndGame()) {
                        val nextAttacker = currentDefenderId
                        val nextDef = getNextActivePlayer(nextAttacker)
                        currentAttackerId = nextAttacker
                        currentDefenderId = nextDef?.id ?: ""
                    }
                }
            } else {
                // Human is the attacker, Bot is defender
                if (activeAttackCards.isNotEmpty() && activeAttackCards.size > activeDefendCards.size) {
                    val targetAttack = activeAttackCards.last()
                    val botIdx = players.indexOfFirst { it.id == def.id }
                    if (botIdx != -1) {
                        val hand = players[botIdx].cards
                        val defCandidates = hand.filter { defCard ->
                            val defSuit = defCard.last().toString()
                            val attSuit = targetAttack.last().toString()
                            val defVal = getCardValue(defCard)
                            val attVal = getCardValue(targetAttack)
                            
                            if (defSuit == attSuit) {
                                defVal > attVal
                            } else {
                                defSuit == trumpSuit && attSuit != trumpSuit
                            }
                        }
                        
                        if (defCandidates.isNotEmpty()) {
                            // Bot defends
                            val defCard = selectCardByDifficulty(defCandidates, difficultyLevel, trumpSuit, isAttack = false)
                            val newDefHand = hand.toMutableList().apply { remove(defCard) }
                            players[botIdx] = players[botIdx].copy(cards = newDefHand)
                            activeDefendCards.add(defCard)
                            viewModel.vibrate(15)
                            durakText = if (isRu) "${def.name} отбился козырной или старшей картой $defCard. Подкинь еще!" 
                                       else "${def.name} beat with $defCard. Throw more or complete round!"
                        } else {
                            // Bot takes
                            durakText = if (isRu) "${def.name} берет карты со стола!" else "${def.name} can't beat and takes cards!"
                            val finalHand = hand + activeAttackCards + activeDefendCards
                            players[botIdx] = players[botIdx].copy(cards = finalHand)
                            activeAttackCards.clear()
                            activeDefendCards.clear()
                            refillHands()
                            if (!checkForEndGame()) {
                                // Skip defender turn, next clockwise attacks
                                val nextAttacker = getNextActivePlayer(def.id)
                                val nextDef = nextAttacker?.let { getNextActivePlayer(it.id) }
                                currentAttackerId = nextAttacker?.id ?: ""
                                currentDefenderId = nextDef?.id ?: ""
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Trigger automated simulations if active player/defender roles are bot-governed
    LaunchedEffect(currentAttackerId, currentDefenderId, activeAttackCards.size, activeDefendCards.size, inDurak) {
        if (inDurak && currentAttackerId.isNotEmpty() && currentDefenderId.isNotEmpty()) {
            val att = players.find { it.id == currentAttackerId }
            val def = players.find { it.id == currentDefenderId }
            if (att != null && def != null) {
                // If attacker is bot and active attacks equal active defenses, bot plays next attack card or calls Beaten
                if (!att.isHuman && activeAttackCards.size == activeDefendCards.size) {
                    runBotTurnSimulation()
                }
                // If human player attacked, and bot has to defend
                if (att.isHuman && activeAttackCards.size > activeDefendCards.size) {
                    runBotTurnSimulation()
                }
            }
        }
    }
    
    fun startDurakGame() {
        if (isCasinoMode && userCoins < betAmount) {
            durakText = if (isRu) "НЕДОСТАТОЧНО COINS" else "NOT ENOUGH COINS"
            viewModel.vibrate(100)
            return
        }
        
        if (isCasinoMode) {
            viewModel.updateCoins(userCoins - betAmount)
        }
        viewModel.vibrate(45)
        
        // Generate deck
        val currentRanks = getRanks()
        deck.clear()
        for (suit in suits) {
            for (rank in currentRanks) {
                deck.add(rank + suit)
            }
        }
        deck.shuffle()
        
        // Clear board
        activeAttackCards.clear()
        activeDefendCards.clear()
        players.clear()
        
        // Add human player
        val humanInitialHand = mutableListOf<String>()
        repeat(6) {
            if (deck.isNotEmpty()) humanInitialHand.add(deck.removeAt(0))
        }
        players.add(
            DurakPlayerState(
                id = "player",
                name = if (isRu) "Ты (Командир)" else "You (Commander)",
                avatarUrl = viewModel.currentUser.value?.avatarUrl,
                decorationId = viewModel.activeDecorationId.value,
                isHuman = true,
                cards = humanInitialHand
            )
        )
        
        // Add bots
        lobbyBots.forEach { bot ->
            val botHand = mutableListOf<String>()
            repeat(6) {
                if (deck.isNotEmpty()) botHand.add(deck.removeAt(0))
            }
            players.add(bot.copy(cards = botHand))
        }
        
        // Set Trump
        if (deck.isNotEmpty()) {
            trumpCard = deck.last()
            trumpSuit = trumpCard.last().toString()
        } else {
            trumpCard = "A♠"
            trumpSuit = "♠"
        }
        remainingCardsCount = deck.size
        
        // Turn management (choose starting attacker)
        // Usually, person with lowest trump card. Let's make it player by default, or random active.
        currentAttackerId = "player"
        val firstDefender = getNextActivePlayer(currentAttackerId)
        currentDefenderId = firstDefender?.id ?: ""
        
        inDurak = true
        durakText = if (isRu) "Раздача завершена. Твой ход! Атакуй ${firstDefender?.name} любой картой." 
                   else "Game started! Your turn. Attack ${firstDefender?.name} with any card."
    }
    
    fun playerAttack(card: String) {
        val playerIdx = players.indexOfFirst { it.isHuman }
        if (playerIdx == -1) return
        val hand = players[playerIdx].cards
        
        // Validate attack
        val isValid = if (activeAttackCards.isEmpty()) {
            true
        } else {
            val ranksOnTable = (activeAttackCards + activeDefendCards).map { it.dropLast(1) }
            card.dropLast(1) in ranksOnTable
        }
        
        if (isValid && activeAttackCards.size < 6) {
            val updatedHand = hand.toMutableList().apply { remove(card) }
            players[playerIdx] = players[playerIdx].copy(cards = updatedHand)
            activeAttackCards.add(card)
            viewModel.vibrate(20)
            
            durakText = if (isRu) "Ты атаковал соперника картой $card! Ожидание ответа..." 
                       else "You played attack: $card! Waiting for bot respond..."
        } else {
            viewModel.vibrate(40)
            durakText = if (isRu) "Нельзя походить картой $card. Выберите карту того же ранга." 
                       else "Invalid attack with $card. Suit has to match ranks of tabletop."
        }
    }
    
    fun playerDefend(card: String) {
        if (activeAttackCards.isEmpty() || activeAttackCards.size <= activeDefendCards.size) return
        val targetAttack = activeAttackCards.last()
        
        val playerIdx = players.indexOfFirst { it.isHuman }
        if (playerIdx == -1) return
        val hand = players[playerIdx].cards
        
        // Check if valid coverage
        val defSuit = card.last().toString()
        val attSuit = targetAttack.last().toString()
        val defVal = getCardValue(card)
        val attVal = getCardValue(targetAttack)
        
        val isValid = if (defSuit == attSuit) {
            defVal > attVal
        } else {
            defSuit == trumpSuit && attSuit != trumpSuit
        }
        
        if (isValid) {
            val updatedHand = hand.toMutableList().apply { remove(card) }
            players[playerIdx] = players[playerIdx].copy(cards = updatedHand)
            activeDefendCards.add(card)
            viewModel.vibrate(20)
            
            durakText = if (isRu) "Ты отбился картой $card!" else "You successfully defended with $card!"
            
            // Check if further bot actions
            scope.launch {
                delay(1000)
                // Bot can throw in card if they have the same rank
                val attPlayer = players.find { it.id == currentAttackerId }
                if (attPlayer != null && !attPlayer.isHuman) {
                    val attIdx = players.indexOfFirst { it.id == attPlayer.id }
                    val attHand = players[attIdx].cards
                    val ranksOnTable = (activeAttackCards + activeDefendCards).map { it.dropLast(1) }
                    val throwables = attHand.filter { it.dropLast(1) in ranksOnTable }
                    
                    if (throwables.isNotEmpty() && activeAttackCards.size < 6 && Random.nextFloat() < 0.65f) {
                        val cardToThrow = selectCardByDifficulty(throwables, difficultyLevel, trumpSuit, isAttack = true)
                        val updatedAttHand = attHand.toMutableList().apply { remove(cardToThrow) }
                        players[attIdx] = players[attIdx].copy(cards = updatedAttHand)
                        activeAttackCards.add(cardToThrow)
                        viewModel.vibrate(15)
                        durakText = if (isRu) "${attPlayer.name} подкинул карту $cardToThrow! Отбейся." 
                                   else "${attPlayer.name} threw in $cardToThrow! Defend."
                    } else {
                        // Complete turn, player beat it
                        durakText = if (isRu) "Бита! Ты успешно защитил свой раунд." else "Beaten! You covered the round."
                        activeAttackCards.clear()
                        activeDefendCards.clear()
                        refillHands()
                        if (!checkForEndGame()) {
                            // defender becomes attacker
                            val nextAtt = currentDefenderId
                            val nextDef = getNextActivePlayer(nextAtt)
                            currentAttackerId = nextAtt
                            currentDefenderId = nextDef?.id ?: ""
                        }
                    }
                }
            }
        } else {
            viewModel.vibrate(40)
            durakText = if (isRu) "Нельзя отбиться картой $card против $targetAttack!" 
                       else "Cannot beat $targetAttack using $card!"
        }
    }
    
    fun finishTurn() {
        if (!inDurak) return
        viewModel.vibrate(20)
        
        val att = players.find { it.id == currentAttackerId }
        val def = players.find { it.id == currentDefenderId }
        if (att == null || def == null) return
        
        if (att.isHuman) {
            // Human is attacker, decides to call Beaten (Бита)
            durakText = if (isRu) "Объявлена бита. Карты уходят в сброс." else "Beaten called. Table discarded."
            activeAttackCards.clear()
            activeDefendCards.clear()
            refillHands()
            if (!checkForEndGame()) {
                // defender becomes next attacker
                val nextAtt = currentDefenderId
                val nextDef = getNextActivePlayer(nextAtt)
                currentAttackerId = nextAtt
                currentDefenderId = nextDef?.id ?: ""
            }
        } else if (def.isHuman) {
            // Human is defender, cannot defend and takes cards
            durakText = if (isRu) "Ты забрал карты со стола!" else "You took cards from table!"
            val playerIdx = players.indexOfFirst { it.isHuman }
            if (playerIdx != -1) {
                val finalHand = players[playerIdx].cards + activeAttackCards + activeDefendCards
                players[playerIdx] = players[playerIdx].copy(cards = finalHand)
            }
            activeAttackCards.clear()
            activeDefendCards.clear()
            refillHands()
            if (!checkForEndGame()) {
                // Skip human turn to attack, next clockwise becomes attacker
                val nextAtt = getNextActivePlayer(def.id)
                val nextDef = nextAtt?.let { getNextActivePlayer(it.id) }
                currentAttackerId = nextAtt?.id ?: ""
                currentDefenderId = nextDef?.id ?: ""
            }
        }
    }
    
    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(8.dp)
    ) {
        if (!inDurak) {
            // Setting/Lobby Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardGray),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = AlertYellow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu) "КЛИЕНТ СЕТИ ДУРАКА 🌐" else "CYBER DURAK CLIENT 🌐",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRu) 
                                "Оппоненты - это верифицированные ИИ-боты комьюнити со своими никами, аватарами и визуальными украшениями. Настройте количество ботов для создания подсетей!" 
                                else "Opponents are verified community AI-agents decorated with random suborbital inventory masks. Tune grid sockets to deploy node setups!",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Bot Count Selector
                Text(
                    text = if (isRu) "ЧИСЛО СОПЕРНИКОВ В СЕТИ: $opponentCount" else "AI NODE OPPONENTS: $opponentCount",
                    color = AccentGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { num ->
                        val isSel = opponentCount == num
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(1.dp, if (isSel) AlertYellow else BorderGray, RoundedCornerShape(4.dp))
                                .background(if (isSel) DeepGray else CardGray)
                                .clickable {
                                    viewModel.vibrate(15)
                                    opponentCount = num
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🤖 $num",
                                color = if (isSel) AlertYellow else PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                
                // Difficulty Settings
                if (!isCasinoMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isRu) "СЛОЖНОСТЬ ВЫЧИСЛЕНИЙ БОТОВ:" else "AI COGNITIVE COMPLEXITY:",
                        color = AccentGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val levels = listOf("Easy", "Medium", "Hard")
                        val levelsRu = listOf("Легко", "Средно", "Сложно")
                        
                        levels.forEachIndexed { i, lev ->
                            val isSel = difficultyLevel == lev
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .border(1.dp, if (isSel) AlertYellow else BorderGray, RoundedCornerShape(4.dp))
                                    .background(if (isSel) DeepGray else CardGray)
                                    .clickable {
                                        viewModel.vibrate(15)
                                        difficultyLevel = lev
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isRu) levelsRu[i] else lev.uppercase(),
                                    color = if (isSel) AlertYellow else PureWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Casino Betting Selector
                if (isCasinoMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isRu) "СТАВКА НА КОН: $betAmount 🪙" else "COSMIC POOL STAKE: $betAmount 🪙",
                        color = AccentGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (betAmount > 5) { betAmount -= 5; viewModel.vibrate(10) } },
                            modifier = Modifier.background(CardGray, RoundedCornerShape(4.dp)).border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Sub bet", tint = PureWhite)
                        }
                        Text(
                            text = "$betAmount 🪙",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)
                        )
                        IconButton(
                            onClick = { if (betAmount + 5 <= userCoins) { betAmount += 5; viewModel.vibrate(10) } },
                            modifier = Modifier.background(CardGray, RoundedCornerShape(4.dp)).border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add bet", tint = PureWhite)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bots Seated Preview Grid as requested!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "УЧАСТНИКИ ЗА СТОЛОМ:" else "SEATED OPPONENT NODES:",
                        color = AccentGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRu) "🔄 ОБНОВИТЬ БОТОВ" else "🔄 RE-ROLL SEATS",
                        color = AlertYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            viewModel.vibrate(20)
                            rollLobbyBots()
                        }
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .background(CardGray, RoundedCornerShape(4.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    lobbyBots.forEach { bot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarWithDecoration(
                                avatarUrl = bot.avatarUrl,
                                decorationId = bot.decorationId,
                                sizeDp = 28,
                                borderWidthDp = 1
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = bot.name,
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (bot.decorationId != null && bot.decorationId > 0) {
                                        if (isRu) "⚡ Системное украшение: #${bot.decorationId}" else "⚡ Equipped inv: #${bot.decorationId}"
                                    } else {
                                        if (isRu) "Нет маски акцента" else "Raw matrix scan"
                                    },
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { startDurakGame() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack)
                ) {
                    Text(
                        text = if (isRu) "РОВНЯТЬ СЕТЬ: НАЧАТЬ ИГРУ" else "DEPLOY PROTOCOL: INITIALIZE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // =============================
            // ACTIVE BOARD PLAYING GAMEPLAY
            // =============================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                // Scrollable Board Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Top opponents row representation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGray, RoundedCornerShape(4.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (isRu) "СОПЕРНИКИ В ОТСЕКЕ:" else "CURRENT ACTIVE OPPONENTS:",
                            color = AccentGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            players.filter { !it.isHuman }.forEach { bot ->
                                val isAtt = bot.id == currentAttackerId
                                val isDef = bot.id == currentDefenderId
                                
                                val outline = if (isAtt) Color(0xFFFF5D5D) else if (isDef) AlertYellow else BorderGray
                                val bg = if (isAtt) Color(0x22FF5D5D) else if (isDef) Color(0x22FFD60A) else DeepGray
                                
                                Row(
                                    modifier = Modifier
                                        .border(1.dp, outline, RoundedCornerShape(4.dp))
                                        .background(bg)
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarWithDecoration(
                                        avatarUrl = bot.avatarUrl,
                                        decorationId = bot.decorationId,
                                        sizeDp = 26,
                                        borderWidthDp = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bot.name,
                                                color = PureWhite,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 75.dp)
                                            )
                                            if (bot.isOut) {
                                                Text(
                                                    text = " 🏆 #${bot.place}",
                                                    color = AlertYellow,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        
                                        if (bot.isOut) {
                                            Text(
                                                text = if (isRu) "ВЫШЕЛ" else "OUT",
                                                color = TextGray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "🎴 x${bot.cards.size}",
                                                    color = AlertYellow,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAtt) "⚔️" else if (isDef) "🎯" else "🛡️",
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Game log feed (Console scroll)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(35.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = durakText,
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                    
                    // Main Desk playing field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(DeepGray, RoundedCornerShape(4.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trump on leftover deck
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = if (isRu) "КОЗЫРЬ" else "TRUMP",
                                color = TextGray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            DurakReadyCard(trumpCard)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🧬 $remainingCardsCount ${if (isRu) "карт" else "left"}",
                                color = AccentGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Live covered and uncovered active table cards
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeAttackCards.forEachIndexed { i, att ->
                                Box(
                                    modifier = Modifier.size(width = 54.dp, height = 75.dp)
                                ) {
                                    DurakPlayingCardView(att)
                                    if (activeDefendCards.size > i) {
                                        Box(
                                            modifier = Modifier.offset(x = 10.dp, y = 14.dp)
                                        ) {
                                            DurakPlayingCardView(activeDefendCards[i])
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Your hands controller
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        val humanState = players.find { it.isHuman }
                        val currentHand = humanState?.cards ?: emptyList()
                        val amIAttacker = currentAttackerId == "player"
                        val amIDefender = currentDefenderId == "player"
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRu) "ТВОЙ АККУМУЛЯТОР КАРТ (${currentHand.size}):" 
                                       else "YOUR HAND CARD POOL (${currentHand.size}):",
                                color = AccentGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (amIAttacker) "⚔️ АТАКА" else if (amIDefender) "🎯 ЗАЩИТА" else "🛡️ В ОЖИДАНИИ",
                                color = if (amIAttacker) Color(0xFFFF5D5D) else if (amIDefender) AlertYellow else TextGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            currentHand.forEach { card ->
                                Box(
                                    modifier = Modifier.clickable {
                                        if (amIAttacker) {
                                            playerAttack(card)
                                        } else if (amIDefender) {
                                            playerDefend(card)
                                        }
                                    }
                                ) {
                                    DurakPlayingCardView(card)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Docked Controls at the bottom of the screen (Request 4)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val amIAttacker = currentAttackerId == "player"
                    val amIDefender = currentDefenderId == "player"
                    
                    Button(
                        onClick = { finishTurn() },
                        enabled = amIAttacker || amIDefender,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray, contentColor = PureWhite),
                        border = BorderStroke(1.dp, if (amIAttacker || amIDefender) AlertYellow else BorderGray)
                    ) {
                        val textStr = if (amIAttacker) {
                            if (isRu) "БИТА / ЗАВЕРШИТЬ ХОД" else "CALL BITA (COMPLETE)"
                        } else {
                            if (isRu) "ВЗЯТЬ ВСЕ КАРТЫ" else "TAKE TABLE"
                        }
                        Text(
                            text = textStr,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Forfeit / surrender
                    Button(
                        onClick = {
                            viewModel.vibrate(30)
                            inDurak = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF4B4B), contentColor = RedSuitColor),
                        border = BorderStroke(1.dp, RedSuitColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isRu) "СДАТЬСЯ" else "FORFEIT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Subordinate card helper to support red colors
@Composable
fun DurakReadyCard(card: String) {
    if (card.isEmpty()) return
    val suit = card.last().toString()
    val isRed = suit == "♥" || suit == "♦"
    Card(
        modifier = Modifier
            .size(width = 38.dp, height = 54.dp)
            .border(1.dp, if (isRed) RedSuitColor else PureWhite, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepGray),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(2.dp)
        ) {
            Text(
                text = card.take(card.length - 1),
                color = if (isRed) RedSuitColor else PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = suit,
                color = if (isRed) RedSuitColor else PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DurakPlayingCardView(card: String) {
    if (card.isEmpty()) return
    val suit = card.last().toString()
    val isRed = suit == "♥" || suit == "♦"
    val contentColor = if (isRed) RedSuitColor else PureWhite
    
    Card(
        modifier = Modifier
            .size(width = 46.dp, height = 65.dp)
            .border(1.dp, contentColor, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = PureBlack),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rankText = card.take(card.length - 1)
            Text(
                text = rankText,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = suit,
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = rankText,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
