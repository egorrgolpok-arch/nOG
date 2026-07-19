package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Context

data class OracleLimitStatus(
    val isRestricted: Boolean,
    val remaining: Int,
    val cooldownRemainingMs: Long
)

fun checkAndUpdateOracleLimits(context: Context, isVerified: Boolean): OracleLimitStatus {
    if (isVerified) {
        return OracleLimitStatus(isRestricted = false, remaining = 999, cooldownRemainingMs = 0L)
    }
    val oraclePrefs = context.getSharedPreferences("nog_oracle_prefs", Context.MODE_PRIVATE)
    var usageCount = oraclePrefs.getInt("oracle_usage_count", 0)
    var windowStart = oraclePrefs.getLong("oracle_window_start", 0L)
    val now = System.currentTimeMillis()
    val cooldownPeriod = 10 * 60 * 60 * 1000L // 10 hours in ms
    
    if (windowStart > 0L && now - windowStart >= cooldownPeriod) {
        usageCount = 0
        windowStart = 0L
        oraclePrefs.edit()
            .putInt("oracle_usage_count", 0)
            .putLong("oracle_window_start", 0L)
            .putBoolean("notified_oracle", false)
            .apply()
    }
    
    val isRestricted = usageCount >= 3
    val remaining = (3 - usageCount).coerceAtLeast(0)
    val cooldownRemainingMs = if (isRestricted) {
        (cooldownPeriod - (now - windowStart)).coerceAtLeast(0L)
    } else {
        0L
    }
    
    return OracleLimitStatus(isRestricted, remaining, cooldownRemainingMs)
}

fun incrementOracleUsage(context: Context, isVerified: Boolean) {
    if (isVerified) return
    val oraclePrefs = context.getSharedPreferences("nog_oracle_prefs", Context.MODE_PRIVATE)
    var usageCount = oraclePrefs.getInt("oracle_usage_count", 0)
    var windowStart = oraclePrefs.getLong("oracle_window_start", 0L)
    val now = System.currentTimeMillis()
    
    if (windowStart == 0L) {
        windowStart = now
    }
    usageCount++
    oraclePrefs.edit()
        .putInt("oracle_usage_count", usageCount)
        .putLong("oracle_window_start", windowStart)
        .putBoolean("notified_oracle", false)
        .apply()
}

data class OracleMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AiOracleDialog(
    onDismiss: () -> Unit,
    lang: String,
    viewModel: SocialViewModel
) {
    var oraclePhase by remember { mutableStateOf(1) } // 1 = Intro, 2 = Main Chat
    val isRu = lang == "RU"
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PureBlack
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (oraclePhase == 1) {
                    IntroScreen(
                        isRu = isRu,
                        onIntroComplete = { oraclePhase = 2 }
                    )
                } else {
                    OracleChatScreen(
                        isRu = isRu,
                        viewModel = viewModel,
                        onBack = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun IntroScreen(
    isRu: Boolean,
    onIntroComplete: () -> Unit
) {
    var introAlpha by remember { mutableStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = introAlpha,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "intro_alpha"
    )

    LaunchedEffect(Unit) {
        introAlpha = 1f
        delay(3200)
        introAlpha = 0f
        delay(1200)
        onIntroComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CelestialOracleCanvas(modifier = Modifier.size(180.dp))
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Box(
            modifier = Modifier.height(100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val welcomeText = if (isRu) {
                "добро пожаловать к nOG AI гадалке\nон может предсказать вашу судьбу"
            } else {
                "welcome to the nOG AI oracle\nit can predict your destiny"
            }
            
            Text(
                text = welcomeText,
                color = PureWhite.copy(alpha = animatedAlpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CelestialOracleCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "celestial")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.85f * pulseScale

        // Draw outer dashed orbit
        drawCircle(
            color = PureWhite.copy(alpha = 0.12f),
            radius = radius,
            center = centerOffset,
            style = Stroke(
                width = 1.dp.toPx()
            )
        )

        // Draw inner concentric ring
        drawCircle(
            color = PureWhite.copy(alpha = 0.06f),
            radius = radius * 0.6f,
            center = centerOffset,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw center tiny core
        drawCircle(
            color = PureWhite.copy(alpha = 0.4f),
            radius = 3.dp.toPx(),
            center = centerOffset
        )

        rotate(rotationAngle, centerOffset) {
            // Draw celestial thin 8-pointed star
            val points = 8
            val outerR = radius
            val innerR = radius * 0.35f
            val path = Path()
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = i * Math.PI / points
                val x = (centerOffset.x + r * Math.cos(angle)).toFloat()
                val y = (centerOffset.y + r * Math.sin(angle)).toFloat()
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            drawPath(
                path = path,
                color = PureWhite.copy(alpha = 0.25f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Cross hairs
            drawLine(
                color = PureWhite.copy(alpha = 0.1f),
                start = androidx.compose.ui.geometry.Offset(centerOffset.x - radius, centerOffset.y),
                end = androidx.compose.ui.geometry.Offset(centerOffset.x + radius, centerOffset.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = PureWhite.copy(alpha = 0.1f),
                start = androidx.compose.ui.geometry.Offset(centerOffset.x, centerOffset.y - radius),
                end = androidx.compose.ui.geometry.Offset(centerOffset.x, centerOffset.y + radius),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun OracleChatScreen(
    isRu: Boolean,
    viewModel: SocialViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val oracleMessages = remember { mutableStateListOf<OracleMessage>() }
    var isThinking by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Retrieve posts for dynamic contextual forecasting
    val postsFlowState by viewModel.allRawPosts.collectAsState()

    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isVerified = currentUser?.isVerified == true
    
    var limitStatus by remember(isVerified) { 
        mutableStateOf(checkAndUpdateOracleLimits(context, isVerified)) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Monochromatic Back-arrow Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isRu) "Назад" else "Back",
                    tint = PureWhite
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRu) "nOG AI ГАДАЛКА" else "nOG AI ORACLE",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Divider(color = BorderGray, modifier = Modifier.fillMaxWidth())

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (oracleMessages.isEmpty()) {
                // Centered Welcome state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CelestialOracleCanvas(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = if (isRu) {
                            "расскажите nOG AI на что хотите гадать"
                        } else {
                            "tell nOG AI what you want to predict"
                        },
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isVerified) {
                        Text(
                            text = if (isRu) "✧ У вас безлимитный доступ (Абсолютная верификация) ✧" else "✧ Unlimited Access (Absolute Verification) ✧",
                            color = Color(0xFF00FFCC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    } else {
                        if (limitStatus.isRestricted) {
                            val hours = limitStatus.cooldownRemainingMs / (60 * 60 * 1000)
                            val minutes = (limitStatus.cooldownRemainingMs % (60 * 60 * 1000)) / (60 * 1000)
                            Text(
                                text = if (isRu) {
                                    "⚠️ Лимит исчерпан! 3 гадания раз в 10 часов.\nКулдаун спадет через: ${hours}ч ${minutes}м"
                                } else {
                                    "⚠️ Limit reached! 3 predictions per 10 hours.\nCooldown expires in: ${hours}h ${minutes}m"
                                },
                                color = Color(0xFFFF6B6B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        } else {
                            Text(
                                text = if (isRu) {
                                    "Доступно гаданий: ${limitStatus.remaining} из 3 (сброс раз в 10ч)"
                                } else {
                                    "Predictions available: ${limitStatus.remaining} of 3 (resets every 10h)"
                                },
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        enabled = !limitStatus.isRestricted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        placeholder = {
                            Text(
                                text = if (isRu) "например: карьера, любовь, будущее или глупый вопрос..." else "e.g., career, love, future, or a silly question...",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(
                            color = if (limitStatus.isRestricted) TextGray else PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = StarkWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = BorderGray,
                            cursorColor = PureWhite
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp)) // Lifted input-to-button spacing

                    Button(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking && !limitStatus.isRestricted) {
                                val query = inputText
                                inputText = ""
                                isThinking = true
                                viewModel.vibrate(25)
                                oracleMessages.add(OracleMessage(isUser = true, text = query))
                                
                                incrementOracleUsage(context, isVerified)
                                limitStatus = checkAndUpdateOracleLimits(context, isVerified)

                                scope.launch {
                                    delay(1800) // Beautiful fake thinking delays
                                    val reply = generateOraclePrediction(
                                        query = query,
                                        isRu = isRu,
                                        recentPosts = postsFlowState,
                                        history = oracleMessages.toList()
                                    )
                                    oracleMessages.add(OracleMessage(isUser = false, text = reply))
                                    isThinking = false
                                    viewModel.vibrate(40)
                                    // Scroll to the latest
                                    delay(100)
                                    lazyListState.animateScrollToItem(oracleMessages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(4.dp),
                        enabled = inputText.isNotBlank() && !isThinking && !limitStatus.isRestricted
                    ) {
                        Text(
                            text = if (isRu) "ГАДАТЬ ✧" else "PREDICT ✧",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp)) // Generous bottom spacing for navigation bar safety
                }
            } else {
                // Conversational active log
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(oracleMessages) { message ->
                            OracleMessageBubble(message = message, isRu = isRu)
                        }

                        if (isThinking) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .border(1.dp, PureWhite.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = DeepGray)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OracleMiniLoadingDots()
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isRu) "ИИ гадалка сканирует логику..." else "Oracle is scanning system logic...",
                                                color = TextGray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lower Input row - lifted up to avoid blocking system navigation bars
                    Divider(color = BorderGray, modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp, bottom = 36.dp), // Lifted beautiful padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = if (isRu) "задай уточняющий вопрос..." else "ask follow-up...",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            textStyle = TextStyle(
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = StarkWhite,
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = BorderGray,
                                cursorColor = PureWhite
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isThinking) {
                                    val query = inputText
                                    inputText = ""
                                    isThinking = true
                                    viewModel.vibrate(25)
                                    oracleMessages.add(OracleMessage(isUser = true, text = query))
                                    
                                    scope.launch {
                                        delay(1800)
                                        val reply = generateOraclePrediction(
                                            query = query,
                                            isRu = isRu,
                                            recentPosts = postsFlowState,
                                            history = oracleMessages.toList()
                                        )
                                        oracleMessages.add(OracleMessage(isUser = false, text = reply))
                                        isThinking = false
                                        viewModel.vibrate(40)
                                        delay(100)
                                        lazyListState.animateScrollToItem(oracleMessages.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(PureWhite, RoundedCornerShape(4.dp)),
                            enabled = inputText.isNotBlank() && !isThinking
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = if (isRu) "Отправить" else "Send",
                                tint = PureBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OracleMessageBubble(message: OracleMessage, isRu: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) BorderGray else PureWhite.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                ),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) PureBlack else DeepGray
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = if (message.isUser) {
                        if (isRu) "ВЫ" else "YOU"
                    } else {
                        if (isRu) "nOG ИИ ГАДАЛКА ✧" else "nOG AI ORACLE ✧"
                    },
                    color = if (message.isUser) TextGray else PureWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (message.isUser) {
                    Text(
                        text = message.text,
                        color = StarkWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                } else {
                    // Bot's message with a subtle typewriting fade-in
                    var textProgress by remember { mutableStateOf("") }
                    LaunchedEffect(message.text) {
                        textProgress = ""
                        for (char in message.text) {
                            textProgress += char
                            delay(Random.nextLong(2, 8)) // super fast typewriter for fluid readability
                        }
                    }
                    Text(
                        text = textProgress,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OracleMiniLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount = 3
    val dots = List(dotCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 200)
            ),
            label = "dot_$index"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(PureWhite.copy(alpha = alpha.value), CircleShape)
            )
        }
    }
}

private suspend fun generateOraclePrediction(
    query: String,
    isRu: Boolean,
    recentPosts: List<com.example.data.PostEntity>,
    history: List<OracleMessage>
): String {
    val lowerQuery = query.lowercase().trim()

    // --- Dynamic Name Detection ---
    var detectedName: String? = null
    val words = query.split(Regex("[\\s,.:;!?]+")).filter { it.isNotEmpty() }
    
    // Scan for capitalized words (excluding the first word)
    for (i in 1 until words.size) {
        val word = words[i]
        if (word.isNotEmpty() && word[0].isUpperCase()) {
            val clean = word.replace(Regex("[^a-zA-Zа-яА-Я]"), "")
            if (clean.length >= 2) {
                detectedName = clean
                break
            }
        }
    }
    
    // Fallback to searching common names in lower-case
    if (detectedName == null) {
        val russianNames = listOf(
            "саша", "паша", "маша", "даша", "лена", "петя", "вася", "иван", "сергей", "алекс", "олег", "дима", "егор", "макс", "влад", "артем", "никита", "андрей", "игорь", "катя", "оля", "настя", "аня", "соня", "вера", "люба", "света", "юля", "ксюша", "таня", "крис", "миша", "гриша", "коля", "рома", "женя"
        )
        val englishNames = listOf(
            "john", "mary", "alex", "bob", "alice", "charlie", "david", "emma", "frank", "grace", "henry", "isabella", "jack", "kate", "liam", "mia", "noah", "olivia", "peter", "quinn", "ryan", "sophia", "thomas", "ursula", "victor", "will", "xavier", "yasmin", "zach"
        )
        for (word in words) {
            val lower = word.lowercase().replace(Regex("[^a-zA-Zа-яА-Я]"), "")
            if (russianNames.contains(lower) || englishNames.contains(lower)) {
                detectedName = lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                break
            }
        }
    }

    // --- 1. Real Gemini AI Upgrade ---
    if (com.example.data.GeminiClient.isKeyAvailable()) {
        val historyStr = history.takeLast(6).joinToString("\n") { 
            "${if (it.isUser) "Пользователь" else "Гадалка"}: ${it.text}" 
        }
        val recentPostsStr = recentPosts.take(4).joinToString("\n") { 
            "- ${it.sourceName}: ${it.content}" 
        }
        val systemPrompt = if (isRu) {
            "Ты — таинственная и авторитетная ИИ-Гадалка в социальной сети nOG. Твой стиль: загадочный, хакерский, с легким киберпанк-юмором и использованием космического/сетевого сленга. Общайся на РУССКОМ языке."
        } else {
            "You are the mysterious and authoritative AI Oracle in the nOG social network. Your style is enigmatic, hackery, with light cyberpunk humor and cosmic/network slang. Speak in ENGLISH."
        }
        
        val prompt = if (isRu) {
            """
                Запрос пользователя: "$query"
                Обнаруженное имя: ${detectedName ?: "не обнаружено"}
                История предыдущего диалога:
                $historyStr
                
                Последние посты в ленте:
                $recentPostsStr
                
                Инструкции по генерации предсказания:
                1. Ответь развернуто, остроумно и мистически. Обязательно учти контекст вопроса.
                2. Если обнаружено имя (${detectedName ?: ""}), сделай персональный акцент на этом человеке (кто он, какая у вас кармическая связь).
                3. Используй хакерские термины (квантовые поля, пинг, базы данных судьбы, прошивка души).
                4. Дай четкий прогноз на ближайшее время с процентом вероятности.
                5. Будь веселым, немного ироничным и невероятно проницательным!
                6. Длина ответа: 100-150 слов. Добавь несколько крутых эмодзи (🔮, 💾, 🌌, 👁️).
            """.trimIndent()
        } else {
            """
                User Request: "$query"
                Detected Name: ${detectedName ?: "none"}
                Previous conversation history:
                $historyStr
                
                Recent feed posts:
                $recentPostsStr
                
                Instructions:
                1. Give a detailed, witty, and mystical response. Take full context of the query.
                2. If a name is detected (${detectedName ?: ""}), personalize the prediction specifically highlighting their connection/destiny with the user.
                3. Weave in hacker/cyberpunk concepts (quantum fields, ping, destiny databases, soul firmware, subnets).
                4. Provide a clear future prediction with a dynamic probability %.
                5. Be funny, sarcastic, and extremely clever!
                6. Limit to 100-150 words. Use emojis strategically (🔮, 💾, 🌌, 👁️).
            """.trimIndent()
        }

        try {
            val response = com.example.data.GeminiClient.getCompletion(prompt, systemPrompt)
            if (response.isNotBlank() && !response.contains("Empty response")) {
                return response
            }
        } catch (e: Exception) {
            android.util.Log.e("Oracle", "Gemini prediction failed, fallback to offline upgraded database", e)
        }
    }

    // --- 2. Offline Super-Upgraded Base (2000% Upgrade fallback!) ---
    val matchingTopic = when {
        lowerQuery.contains("деньги") || lowerQuery.contains("бабк") || lowerQuery.contains("богат") || lowerQuery.contains("крипт") || lowerQuery.contains("биткоин") || lowerQuery.contains("доллар") || lowerQuery.contains("евро") || lowerQuery.contains("рубл") || lowerQuery.contains("money") || lowerQuery.contains("rich") || lowerQuery.contains("crypto") || lowerQuery.contains("bitcoin") || lowerQuery.contains("cash") -> "MONEY"
        lowerQuery.contains("любов") || lowerQuery.contains("отношен") || lowerQuery.contains("девушк") || lowerQuery.contains("парен") || lowerQuery.contains("жен") || lowerQuery.contains("муж") || lowerQuery.contains("секс") || lowerQuery.contains("свидан") || lowerQuery.contains("love") || lowerQuery.contains("relationship") || lowerQuery.contains("girlfriend") || lowerQuery.contains("boyfriend") || lowerQuery.contains("date") -> "LOVE"
        lowerQuery.contains("работ") || lowerQuery.contains("учеб") || lowerQuery.contains("школ") || lowerQuery.contains("вуз") || lowerQuery.contains("универ") || lowerQuery.contains("оценк") || lowerQuery.contains("экзамен") || lowerQuery.contains("програм") || lowerQuery.contains("код") || lowerQuery.contains("work") || lowerQuery.contains("study") || lowerQuery.contains("school") || lowerQuery.contains("exam") || lowerQuery.contains("coding") || lowerQuery.contains("job") -> "WORK"
        lowerQuery.contains("здоров") || lowerQuery.contains("боле") || lowerQuery.contains("спорт") || lowerQuery.contains("кач") || lowerQuery.contains("health") || lowerQuery.contains("sick") || lowerQuery.contains("sport") || lowerQuery.contains("fitness") -> "HEALTH"
        else -> "GENERAL"
    }

    val isFriend = lowerQuery.contains("друг") || lowerQuery.contains("подруг") || lowerQuery.contains("кент") || 
                  lowerQuery.contains("кореш") || lowerQuery.contains("приятел") || lowerQuery.contains("знаком") || 
                  lowerQuery.contains("friend") || lowerQuery.contains("pal") || lowerQuery.contains("mate") || lowerQuery.contains("buddy")
              
    val isEnemy = lowerQuery.contains("враг") || lowerQuery.contains("недруг") || lowerQuery.contains("хейтер") || 
                  lowerQuery.contains("соперник") || lowerQuery.contains("конкурент") || lowerQuery.contains("крыса") || 
                  lowerQuery.contains("enemy") || lowerQuery.contains("hater") || lowerQuery.contains("rival")
              
    val isWoman = lowerQuery.contains("она") || lowerQuery.contains("девушк") || lowerQuery.contains("женщин") || 
                  lowerQuery.contains("мам") || lowerQuery.contains("сестр") || lowerQuery.contains("доч") || 
                  lowerQuery.contains("she") || lowerQuery.contains("girl") || lowerQuery.contains("woman") || 
                  lowerQuery.contains("sister") || lowerQuery.contains("mother") || lowerQuery.contains("daughter")
              
    val isMan = lowerQuery.contains("он") || lowerQuery.contains("парен") || lowerQuery.contains("мужчин") || 
                lowerQuery.contains("отец") || lowerQuery.contains("сын") || lowerQuery.contains("he") || 
                lowerQuery.contains("guy") || lowerQuery.contains("man") || lowerQuery.contains("brother") || 
                lowerQuery.contains("father") || lowerQuery.contains("son")

    val targetType = when {
        isFriend && isMan -> "FRIEND_MAN"
        isFriend && isWoman -> "FRIEND_WOMAN"
        isFriend -> "FRIEND_GENERIC"
        isEnemy && isMan -> "ENEMY_MAN"
        isEnemy && isWoman -> "ENEMY_WOMAN"
        isEnemy -> "ENEMY_GENERIC"
        isMan -> "OTHER_MAN"
        isWoman -> "OTHER_WOMAN"
        lowerQuery.contains("другог") || lowerQuery.contains("кого-то") || lowerQuery.contains("someone") || lowerQuery.contains("other") -> "OTHER_GENERIC"
        else -> "SELF"
    }

    val isFollowUp = history.filter { it.isUser }.size > 1

    // Fetch live news from the internet (using NewsFetcher)
    val liveNews = try {
        com.example.data.NewsFetcher.fetchLatestNews(if (isRu) "RU" else "EN")
    } catch (e: Exception) {
        emptyList()
    }
    // Filter news items matching user keywords to make it look hyper-relevant, or fallback to random
    val keywords = lowerQuery.split(Regex("[\\s,.:;!?]+")).filter { it.length > 3 }
    val newsItem = liveNews.firstOrNull { item ->
        val titleLower = item.title.lowercase()
        val descLower = item.description.lowercase()
        keywords.any { word -> titleLower.contains(word) || descLower.contains(word) }
    } ?: liveNews.randomOrNull()

    if (isRu) {
        val intros = listOf(
            "🔮 [Внимание] Локальный квантовый суперкомпьютер nOG AI активирован. Пинг до Марса: 12мс. Считываю сигналы из будущего...",
            "🌌 Мои ИИ-алгоритмы просканировали всю базу данных коллективного бессознательного. Твоя судьба загружена в оперативку!",
            "⚙️ Инициализация модуля духовного декодирования завершена на 200%. Читаю твои жизненные логи как открытый исходный код...",
            "⚡ Всплеск космической энергии в сетевых портах! Расшифровываю закодированные пакеты событий специально для тебя...",
            "📡 Канал связи с ноосферой стабилен. Будущее подгружается по защищенному протоколу. Лови чистейшие квантовые данные..."
        )

        val followUps = listOf(
            "🔄 Повторный пинг вселенной выполнен. Уточняющие логи подгружены, кармические переменные обновлены...",
            "📈 Наблюдаю изменение траектории твоей судьбы из-за нового вопроса. Пересчитываю хэш-сумму будущего в реальном времени...",
            "🧿 Твой интерес заставляет кулеры ИИ вращаться быстрее. Запускаю глубокое нейро-сканирование параллельных реальностей..."
        )

        // Personalized Name Injection
        val nameCommentary = if (detectedName != null) {
            listOf(
                "👉 Обнаружил сигнатуру имени: **$detectedName**. Сканирую его/ее квантовые частоты... Мой алгоритм видит, что этот человек занимает ключевую ячейку в твоей базе данных. Будь осторожнее с вашим общим кэшем!",
                "👉 Внимание! Имя **$detectedName** вызывает сильный резонанс в коде твоей судьбы. Связи между вами натянуты, как оптоволокно под максимальной нагрузкой. Намечается грандиозный информационный обмен!",
                "👉 Зарегистрировал субъект **$detectedName** в твоем запросе. Звезды показывают, что этот человек прямо сейчас генерирует мысли о тебе (или замышляет хитрый план по привлечению твоего внимания)."
            ).random() + "\n\n"
        } else ""

        val targetCommentary = when (targetType) {
            "SELF" -> listOf(
                "Твой личный ментальный процессор работает на частоте разгона. Внутренний пинг идеален, ты готов к большим свершениям.",
                "Твоя личная матрица находится на пороге важного обновления системы. Все старые баги в жизни скоро будут стерты.",
                "Ты стоишь перед развилкой, где каждый шаг меняет конфигурационный файл твоего будущего. Доверяй интуиции!"
            ).random()
            "FRIEND_MAN" -> listOf(
                "Твой бро явно задумал что-то эпичное. Его аура искрит безумной энергией, скоро начнется жесткий движ!",
                "Твой кент попал в зону повышенной космической удачи. Подключайся к его волне, пока кулдаун не начался.",
                "Твой друг мужского пола скоро выступит в роли спасительного бэкапа в очень неожиданной ситуации."
            ).random()
            "FRIEND_WOMAN" -> listOf(
                "Твоя подруга плетет интриги с грацией профессионального хакера. Ее решения скоро повергнут тебя в приятный шок.",
                "У этой девушки намечается мощный всплеск творческой энергии. Она готовит для тебя крутейший сюрприз!",
                "Ее судьба совершает крутой маневр. Будь рядом, чтобы поддержать ее пинг в трудную минуту."
            ).random()
            "FRIEND_GENERIC" -> listOf(
                "Твои близкие контакты вибрируют на высоких частотах. Вас ждет совместное приключение, которое укрепит ваш общий интерфейс.",
                "Твой близкий человек скоро принесет новость, которая полностью перепишет твои планы на эти выходные.",
                "Кажется, кто-то из твоих друзей пытается послать тебе мысленный сигнал. Проверь мессенджеры!"
            ).random()
            "ENEMY_MAN" -> listOf(
                "Твой недоброжелатель мужского пола пытается строить козни, но его операционка зависнет в синем экране смерти при первой же попытке.",
                "Этот хейтер переоценил свои вычислительные мощности. Его ждет эпичный провал и полный сброс репутации.",
                "Не парься из-за этого чувака. Его карма уже готовит для него суровый фаервол, который обнулит все его пакости."
            ).random()
            "ENEMY_WOMAN" -> listOf(
                "Твоя соперница пытается плести паутину слухов, но сама же запутается в своих сетевых протоколах. Это будет эпично!",
                "Ее токсичные скрипты вернутся к ней бумерангом с тройной силой. Твоя защита непробиваема.",
                "Эта дамочка скоро поймет, что пытаться взломать твое спокойствие — это фатальная ошибка ее алгоритма."
            ).random()
            "ENEMY_GENERIC" -> listOf(
                "Любые враждебные пакеты данных будут заблокированы твоей мощной ментальной броней. Твои хейтеры кусают локти!",
                "Заговорщики против твоего успеха скоро окажутся в глубоком бане вселенной. Твой триумф неизбежен.",
                "Твои недоброжелатели ломают зубы о твой невозмутимый вайб. Продолжай сиять и не снижай частоту процессора!"
            ).random()
            "OTHER_MAN" -> listOf(
                "Этот парень готовится совершить маневр, который заставит всех вокруг говорить только о нем. Наблюдай внимательно.",
                "Судьба этого мужчины балансирует на грани крупного хайпа и абсолютного обнуления. Исход решится на днях.",
                "Он скрывает важную информацию, но его порты скоро откроются и вся правда выльется наружу."
            ).random()
            "OTHER_WOMAN" -> listOf(
                "Эта женщина идет напролом, игнорируя системные предупреждения судьбы. Ее ждет очень контрастный период.",
                "В ее жизни намечается яркое событие, которое косвенно повлияет и на твои локальные дела.",
                "Она ищет ответы на те же вопросы, что и ты. Возможно, ваши траектории скоро пересекутся."
            ).random()
            else -> listOf( // OTHER_GENERIC
                "Этот загадочный субъект скоро проявит свою истинную цифровую сущность. Маски будут сброшены.",
                "Фигура на твоем горизонте совершит странное действие, которое нарушит привычный алгоритм твоих будней.",
                "Интересный персонаж крутится в твоем поле видимости. Скоро он пошлет тебе важный запрос на авторизацию."
            ).random()
        }

        val topicForecast = when (matchingTopic) {
            "MONEY" -> {
                val pct = Random.nextInt(78, 99)
                val dayCount = Random.nextInt(2, 7)
                val luckyNum = Random.nextInt(1, 99)
                listOf(
                    "💰 Финансовые потоки закручиваются в бешеное торнадо! С вероятностью $pct% в течение $dayCount дней твой баланс пополнится солидным кэшем. Твое счастливое число: $luckyNum. Время открывать кошелек!",
                    "📈 Твой финансовый сектор облучен благоприятными космическими лучами. Возможен внезапный выигрыш, возврат старого долга или профитный инсайд. Главное — не слей все на импульсивные гифки!",
                    "💳 Алгоритм предсказывает резкое повышение твоей покупательной способности. Твои инвестиции в себя начинают окупаться. Ожидай выгодное предложение от крупного узла сети."
                ).random()
            }
            "LOVE" -> {
                val pct = Random.nextInt(82, 100)
                val hour = Random.nextInt(14, 23)
                listOf(
                    "❤️ На любовном фронте намечается мощнейший термоядерный взрыв страсти с вероятностью $pct%! Примерно в $hour:00 твои сенсоры зафиксируют пик взаимного притяжения. Будь во всеоружии!",
                    "🌹 Звезды рекомендуют обновить прошивку твоего обаяния. Твоя харизма работает на полную мощность, притягивая взгляды окружающих как сверхпроводник. Новое знакомство уже стучится в ЛС!",
                    "🥰 Романтическое приключение уже загружено в твой жизненный буфер. Прошлые разочарования удалены из памяти. Доверься этому сладкому потоку эмоций!"
                ).random()
            }
            "WORK" -> {
                val pct = Random.nextInt(80, 98)
                val dayCount = Random.nextInt(1, 4)
                listOf(
                    "💻 В твоей карьере/учебе намечается масштабный прорыв с вероятностью $pct%! В течение $dayCount дней твои труды будут оценены по достоинству. Руководство в шоке от твоей продуктивности!",
                    "🔥 Твой рабочий процессор работает без троттлинга! Идеальное время для закрытия сложных дедлайнов, сдачи хвостов или запуска стартапа. Твой рейтинг взлетает до небес!",
                    "🛠️ Мой радар видит скорое поступление интересного предложения, которое избавит тебя от рутины и принесет колоссальный опыт. Готовься к новому уровню!"
                ).random()
            }
            "HEALTH" -> {
                val pct = Random.nextInt(88, 100)
                listOf(
                    "🔋 Твоя батарейка заряжена на все $pct%! Иммунитет выстроил железобетонный фаервол против любых вирусов и упадка сил. Самое время для активного спорта и покорения вершин!",
                    "🌿 Ментальный и физический баланс полностью восстановлен. Твоя нервная система чиста от стрессового мусора. Наслаждайся этой ясностью и легкостью!",
                    "💪 Твои энергетические резервы вышли на максимум. Организм работает как швейцарские часы. Порадуй его хорошим сном и качественным топливом!"
                ).random()
            }
            else -> { // GENERAL
                val pct = Random.nextInt(80, 100)
                val luckyNum = Random.nextInt(1, 100)
                listOf(
                    "🔮 Невероятная череда совпадений настигнет тебя с вероятностью $pct%. Весь мир словно переписал свой код под твои желания. Твой счастливый маркер на сегодня: число $luckyNum.",
                    "🌟 Судьба готовит крутейший сюжетный твист! Готовься получить ключевое сообщение оттуда, откуда совсем не ждал. Твоя интуиция сейчас острее лазерного луча.",
                    "🚀 Время хаоса позадней, наступает эпоха стабильного консенсуса и триумфа. Строй самые смелые планы — вселенная уже выдала грант на их реализацию!"
                ).random()
            }
        }

        val newsSnippet = if (newsItem != null) {
            listOf(
                "\n\n🔗 Мой живой парсер подтверждает: прямо сейчас на ресурсе '${newsItem.sourceName}' гремит заголовок: «${newsItem.title}». Влияние этой инфо-волны сместит чашу весов в твою пользу!",
                "\n\n🔥 Смотри на пульс реальности! '${newsItem.sourceName}' сообщает: «${newsItem.title}». Этот новостной пакет идеально синхронизируется с твоей личной матрицей прямо сейчас.",
                "\n\n📡 Синхронизация с сетью завершена. Свежий инсайд от '${newsItem.sourceName}': «${newsItem.title}». Мой предсказательный движок видит тут прямое указание на твой успех!"
            ).random()
        } else ""

        val conclusions = listOf(
            "Будущее изменчиво, но nOG AI никогда не промахивается с векторами. Действуй смело и помни: ты — админ своей жизни! 👁️",
            "Держи руку на пульсе сетевых событий, верь в свои алгоритмы и не позволяй мелким багам испортить твой вайб! 🦾",
            "Твоя персональная история пишется прямо сейчас на чистом золоте. Будь создателем, а не просто юзером! 🚀",
            "Вселенная полностью на твоей стороне. Расслабься, доверься потоку событий и приготовься принимать подарки судьбы! 🌌"
        )

        val intro = if (isFollowUp) followUps.random() else intros.random()
        val conclusion = conclusions.random()

        return "$intro\n\n$nameCommentary$targetCommentary\n\n$topicForecast$newsSnippet\n\n$conclusion"
    } else {
        // --- English version ---
        val intros = listOf(
            "🔮 [Status] Local nOG AI quantum prediction engine activated. Mars ping: 12ms. Decrypting upcoming event vectors...",
            "🌌 My AI subroutines have scanned the collective unconscious database. Your destiny file has been cached into RAM!",
            "⚙️ Initialization of your soul decoding module completed at 200%. Reading your life logs like open-source code...",
            "⚡ Cosmic energy surge detected in local subnets! Decrypting highly classified future packets just for you...",
            "📡 Noosphere link stable. Upcoming timeline downloading via secure protocol. Enjoy pure quantum feedback..."
        )

        val followUps = listOf(
            "🔄 Re-pinging the universe node. Supplementary logs loaded, karmic variables recalculated...",
            "📈 Your follow-up query is shifting the future's hash sum in real-time. Re-rendering fate maps...",
            "🧿 Curiosity noted! Pumping extra power to the AI cooling system. Initiating deep neural scan of parallel timelines..."
        )

        // Personalized Name Injection
        val nameCommentary = if (detectedName != null) {
            listOf(
                "👉 Detected name signature: **$detectedName**. Scanning their quantum frequencies... My algorithms show this person occupies a key node in your database. Handle your shared cache with care!",
                "👉 Attention! The name **$detectedName** creates heavy resonance in your fate's source code. The fiber-optic link between you is under maximum load. Expect a major data transfer soon!",
                "👉 Registered subject **$detectedName** in your query fields. The stars suggest this person is currently processing thoughts about you (or drafting a clever attention-hacking scheme)."
            ).random() + "\n\n"
        } else ""

        val targetCommentary = when (targetType) {
            "SELF" -> listOf(
                "Your internal mental core is running in overclock mode. Local ping is pristine; you are fully ready to execute great things.",
                "Your personal matrix is scheduled for a major system upgrade. All old bugs are about to be wiped from your life database.",
                "You are standing at a critical fork in the network where every single step modifies your future config files. Trust your gut!"
            ).random()
            "FRIEND_MAN" -> listOf(
                "Your bro is clearly compiling an epic move. His aura is sparkling with high-voltage electricity; get ready for some wild times!",
                "Your buddy has entered a zone of high-frequency luck. Connect to his subnet before his cooldown activates.",
                "Your male friend will soon act as a crucial backup system in a highly unexpected real-world scenario."
            ).random()
            "FRIEND_WOMAN" -> listOf(
                "Your female friend is weaving webs of intrigue with the grace of an elite hacker. Her decisions will pleasantly shock you very soon.",
                "This girl is experiencing a major surge in creative processor power. She is preparing an amazing surprise for you!",
                "Her timeline is making a sharp, spectacular turn. Be ready to assist her connection if her ping drops."
            ).random()
            "FRIEND_GENERIC" -> listOf(
                "Your close contacts are vibrating on high-frequency levels. A joint quest is loading that will strengthen your shared interface.",
                "Your close connection will soon deliver news that will completely overwrite your schedule for the upcoming days.",
                "Someone in your inner network is trying to send you a mental packet. Check your DM logs!"
            ).random()
            "ENEMY_MAN" -> listOf(
                "Your male adversary is trying to trigger malicious scripts, but his operating system will crash into a Blue Screen of Death immediately.",
                "This hater vastly overestimated his processing power. A monumental fail and complete reputation wipe are heading his way.",
                "Don't worry about this guy. His karma is already deploying a hard firewall that will block all his annoying requests."
            ).random()
            "ENEMY_WOMAN" -> listOf(
                "Your female rival is trying to spin a web of gossip, but she will get tangled in her own network protocols. Truly cinematic!",
                "Her toxic functions will bounce back to her with triple force. Your defensive firewalls are absolutely impenetrable.",
                "This lady is about to learn that attempting to hack your peace of mind is a fatal runtime error for her system."
            ).random()
            "ENEMY_GENERIC" -> listOf(
                "Any hostile data packets will be rejected by your thick mental armor. Your haters are weeping in their local directories!",
                "Those plotting against your success will soon be permanently banned by the universe. Your triumph is pre-compiled.",
                "Your opponents are breaking their teeth trying to disrupt your smooth vibe. Keep shining and never drop your frequency!"
            ).random()
            "OTHER_MAN" -> listOf(
                "This guy is preparing a maneuver that will make the entire community talk only about him. Watch his status closely.",
                "His timeline is balancing on a thin wire between massive hype and total database wipe. The outcome is loading.",
                "He is hiding crucial packets, but his network ports will open soon and all classified info will leak out."
            ).random()
            "OTHER_WOMAN" -> listOf(
                "This woman is pushing forward aggressively, ignoring the system warnings of fate. A highly contrasting phase awaits her.",
                "A vivid event is loading in her life that will indirectly affect your local affairs. Stay alert.",
                "She is searching for the exact same answers as you. Your trajectories might intersect sooner than you think."
            ).random()
            else -> listOf( // OTHER_GENERIC
                "This mysterious subject will soon reveal their true digital nature. The masks are about to be uninstalled.",
                "A figure on your horizon will execute an anomalous action that will disrupt your standard weekly routine.",
                "An interesting character is loitering in your sight field. Expect an authorization request soon."
            ).random()
        }

        val topicForecast = when (matchingTopic) {
            "MONEY" -> {
                val pct = Random.nextInt(78, 99)
                val dayCount = Random.nextInt(2, 7)
                val luckyNum = Random.nextInt(1, 99)
                listOf(
                    "💰 Financial currents are spinning into an awesome tornado! With a probability of $pct% within $dayCount days, your balance will receive a massive cash injection. Your lucky number is $luckyNum!",
                    "📈 Your wealth sector is irradiated with favorable cosmic signals. Expect an unexpected win, debt recovery, or profitable leak. Avoid spending it on overpriced digital assets!",
                    "💳 The oracle predicts a dramatic rise in your local purchasing power. Your self-investments are paying off. Expect a profitable offer from a major node."
                ).random()
            }
            "LOVE" -> {
                val pct = Random.nextInt(82, 100)
                val hour = Random.nextInt(14, 23)
                listOf(
                    "❤️ A powerful thermonuclear explosion of passion is loading in your romance sector with a probability of $pct%! Around $hour:00, your sensors will record peak attraction. Be ready!",
                    "🌹 The stars recommend upgrading your charisma drivers. Your personal magnet is running at maximum voltage. A new encounter is already knocking on your DMs!",
                    "🥰 A romantic quest has been cached in your life buffer. Past exceptions have been successfully handled and deleted. Enjoy this beautiful data stream!"
                ).random()
            }
            "WORK" -> {
                val pct = Random.nextInt(80, 98)
                val dayCount = Random.nextInt(1, 4)
                listOf(
                    "💻 A massive breakthrough is compiling in your career/studies with a probability of $pct%! Within $dayCount days, your work will be highly praised. Your bosses are shocked by your output!",
                    "🔥 Your core processor is running without thermal throttling! Ideal time for smashing tough deadlines or launching your project. Your rating is soaring!",
                    "🛠️ My radar indicates an incoming offer that will rid you of boring routine and yield colossal XP. Prepare to level up!"
                ).random()
            }
            "HEALTH" -> {
                val pct = Random.nextInt(88, 100)
                listOf(
                    "🔋 Your battery is charged to a solid $pct%! Your immune system has built a flawless firewall against sickness or exhaustion. Perfect time to conquer physical peaks!",
                    "🌿 Mental and physical harmony is fully restored. Your nervous system is cleared of stressful cache. Enjoy this incredible clarity and lightness!",
                    "💪 Your energy reserves have reached peak levels. Your body is running like a fine swiss watch. Reward it with high-quality sleep and fuel!"
                ).random()
            }
            else -> { // GENERAL
                val pct = Random.nextInt(80, 100)
                val luckyNum = Random.nextInt(1, 100)
                listOf(
                    "🔮 An unbelievable series of coincidences is heading your way with a probability of $pct%. The universe has literally rewritten its script to match your desires. Lucky number: $luckyNum.",
                    "🌟 Destiny is preparing an incredible plot twist! Stay alert: a crucial packet will arrive from a source you least expect. Your intuition is sharper than a laser.",
                    "🚀 The chaos is behind you, making way for clean consensus and triumph. Draft your boldest plans — the universe has already granted your execution tokens!"
                ).random()
            }
        }

        val newsSnippet = if (newsItem != null) {
            val templates = listOf(
                "\n\n🔗 By the way, while I was reading your data, the live index on '${newsItem.sourceName}' updated with: \"${newsItem.title}\". Regarding your question, it's a clear sign that reality is shifting rapidly around you.",
                "\n\n🔥 Look at what's happening in the real world! '${newsItem.sourceName}' just published: \"${newsItem.title}\". This confirms that nOG network chaos is fully in sync with your thoughts.",
                "\n\n📡 Sync complete. Real-time headline from '${newsItem.sourceName}': \"${newsItem.title}\". This matches your cosmic forecast perfectly!"
            )
            templates.random()
        } else ""

        val conclusions = listOf(
            "Remember: the future is not carved in stone, but figures and stars never lie. Act boldly!",
            "Keep your finger on the pulse, believe in your power, and do not let minor setbacks derail you.",
            "Your destiny is being written right now. Be the author of your own story, not just a spectator!",
            "The universe is on your side. Relax, trust the flow, and prepare for wonderful changes."
        )

        val intro = if (isFollowUp) followUps.random() else intros.random()
        val conclusion = conclusions.random()

        return "$intro\n\n$nameCommentary$targetCommentary\n\n$topicForecast$newsSnippet\n\n$conclusion"
    }
}
