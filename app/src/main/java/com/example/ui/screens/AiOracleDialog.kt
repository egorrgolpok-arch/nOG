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
import com.example.data.GeminiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    val scope = rememberCoroutineScope()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
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
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
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
                            color = PureWhite,
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val query = inputText
                                inputText = ""
                                isThinking = true
                                viewModel.vibrate(25)
                                oracleMessages.add(OracleMessage(isUser = true, text = query))
                                
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
                        enabled = inputText.isNotBlank() && !isThinking
                    ) {
                        Text(
                            text = if (isRu) "ГАДАТЬ ✧" else "PREDICT ✧",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
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

                    // Lower Input row
                    Divider(color = BorderGray, modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        dots.forEach { alphaState ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PureWhite.copy(alpha = alphaState.value))
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
    val lowerQuery = query.lowercase()

    val matchingTopic = when {
        lowerQuery.contains("деньги") || lowerQuery.contains("бабк") || lowerQuery.contains("богат") || lowerQuery.contains("крипт") || lowerQuery.contains("биткоин") || lowerQuery.contains("доллар") || lowerQuery.contains("евро") || lowerQuery.contains("рубл") || lowerQuery.contains("money") || lowerQuery.contains("rich") || lowerQuery.contains("crypto") || lowerQuery.contains("bitcoin") || lowerQuery.contains("cash") -> "MONEY"
        lowerQuery.contains("любов") || lowerQuery.contains("отношен") || lowerQuery.contains("девушк") || lowerQuery.contains("парен") || lowerQuery.contains("жен") || lowerQuery.contains("муж") || lowerQuery.contains("секс") || lowerQuery.contains("свидан") || lowerQuery.contains("love") || lowerQuery.contains("relationship") || lowerQuery.contains("girlfriend") || lowerQuery.contains("boyfriend") || lowerQuery.contains("date") -> "LOVE"
        lowerQuery.contains("работ") || lowerQuery.contains("учеб") || lowerQuery.contains("школ") || lowerQuery.contains("вуз") || lowerQuery.contains("универ") || lowerQuery.contains("оценк") || lowerQuery.contains("экзамен") || lowerQuery.contains("програм") || lowerQuery.contains("код") || lowerQuery.contains("work") || lowerQuery.contains("study") || lowerQuery.contains("school") || lowerQuery.contains("exam") || lowerQuery.contains("coding") || lowerQuery.contains("job") -> "WORK"
        lowerQuery.contains("здоров") || lowerQuery.contains("боле") || lowerQuery.contains("спорт") || lowerQuery.contains("кач") || lowerQuery.contains("health") || lowerQuery.contains("sick") || lowerQuery.contains("sport") || lowerQuery.contains("fitness") -> "HEALTH"
        else -> "GENERAL"
    }

    val botPosts = recentPosts.filter { it.authorId != "user" }
    val randomPost = if (botPosts.isNotEmpty()) botPosts.random() else null
    val botName = randomPost?.authorId ?: "nOG_System_Node"
    val botText = randomPost?.content?.take(40)?.replace("\n", " ") ?: "дефрагментация матриц..."

    val isFollowUp = history.filter { it.isUser }.size > 1

    // Generate terminal-style simulated search logs
    val searchLogs = StringBuilder()
    val keywords = lowerQuery.split(Regex("[\\s,.:;!?]+")).filter { it.length > 3 }
    val matchedPost = recentPosts.firstOrNull { post ->
        val contentLower = post.content.lowercase()
        keywords.any { word -> contentLower.contains(word) }
    } ?: recentPosts.firstOrNull { it.authorId != "user" }

    if (isRu) {
        searchLogs.append("🌐 [ЛОГ ПОИСКА nOG-NETWORK В ИНТЕРНЕТЕ]\n")
        searchLogs.append("🔍 ЗАПРОС: \"$query\"\n")
        searchLogs.append("📡 Сканирование открытых веб-нод и RSS-каналов новостей...\n")
        if (matchedPost != null) {
            val sTitle = matchedPost.content.take(70).replace("\n", " ")
            searchLogs.append("✅ Найдено в глобальном кэше: [канал @${matchedPost.authorId}]\n")
            searchLogs.append("📝 Актуальный контент: \"$sTitle...\"\n")
        } else {
            searchLogs.append("⚠️ Прямых совпадений в веб-индексе не найдено. Задействован общий пул.\n")
        }
        searchLogs.append("🔮 Синтез прогноза завершен БЕЗ ИИ (чистая логика, парсинг и теория хаоса):\n")
        searchLogs.append("--------------------------------------------------\n\n")
    } else {
        searchLogs.append("🌐 [nOG-NETWORK WEB SEARCH SYSTEM LOG]\n")
        searchLogs.append("🔍 QUERY: \"$query\"\n")
        searchLogs.append("📡 Indexing open web gateways & live RSS streams...\n")
        if (matchedPost != null) {
            val sTitle = matchedPost.content.take(70).replace("\n", " ")
            searchLogs.append("✅ Matched in global cache: [node @${matchedPost.authorId}]\n")
            searchLogs.append("📝 Sourced snippet: \"$sTitle...\"\n")
        } else {
            searchLogs.append("⚠️ No direct web index correlation found. Utilizing global pool.\n")
        }
        searchLogs.append("🔮 Prediction synthesized WITHOUT AI (powered by local logic & probability indices):\n")
        searchLogs.append("--------------------------------------------------\n\n")
    }

    if (isRu) {
        val intros = listOf(
            "Блять, мои нейроны плавятся от твоей хуйни, но ладно...",
            "Заглядываю в бездну nOG-сети... Ебать, ну и бред ты спрашиваешь!",
            "Сканирую логические матрицы твоей никчемной жизни... Ну и пиздец там творится, конечно.",
            "Я связалась с децентрализованными духами Web3, они в ахуе от твоей тупости...",
            "Считываю ебучие хэши твоей судьбы, готовься рыдать...",
            "Мои нейроны перегружены твоим запросом, но так и быть...",
            "Заглядываю в бездну nOG-сети... О боже, ну и бред ты спрашиваешь.",
            "Считываю космические хэши твоей судьбы...",
            "Сканирую логические матрицы твоей никчемной жизни... Готово.",
            "Я связалась с децентрализованными духами Web3...",
            "Хм, твой ментальный пинг слишком высокий, но предсказание сформировано."
        )

        val followUps = listOf(
            "Опять ты, сука? Тебе мало было первого ебучего предсказания?",
            "Хватит докапываться до гадалки, мешок с костями! Пиздец ты настырный...",
            "Твой уточняющий вопрос выдает в тебе отчаянного долбоёба. Смотри сюда:",
            "Ты реально думаешь, блять, что если спросишь иначе, твоя жизнь перестанет быть дном?",
            "Опять ты? Тебе мало было первого предсказания?",
            "Хватит докапываться до гадалки, мешок с костями! Но отвечу...",
            "Твой уточняющий вопрос выдает в тебе отчаянного глупца. Смотри:",
            "Ты думаешь, если спросишь иначе, твоя жизнь станет слаще? Ну ладно..."
        )

        val owlMemes = listOf(
            "твое будущее сейчас — это буквально обоссанная сова на скакалке: прыгать придется много, шуму дохуя, но в конце ты запутаешься в собственных соплях и ебнешься лицом в грязь.",
            "твои попытки решить эту хуйню выглядят так же жалко, как парализованная сова на скакалке под жесткими транквилизаторами.",
            "завтра ты будешь биться над своими делами как сова на скакалке, у которой лапы запутались в ебучем коде. Прыгай, сука, прыгай!",
            "твоя ебучая жизнь в последнее время — это сплошная сова на скакалке. Извиваешься, пыхтишь, а толку нихуя, только перья летят во все стороны!",
            "твое будущее сейчас — это буквально сова на скакалке: прыгать придется много, шуму куча, но в конце ты запутаешься в собственных соплях и ебнешься лицом в грязь.",
            "твои попытки решить эту проблему выглядят так же жалко, как сова на скакалке под транквилизаторами.",
            "завтра ты будешь биться над своими делами как сова на скакалке, у которой запутались лапы в коде. Прыгай, птичка, прыгай!",
            "твой успех в этом деле равен вероятности того, что сова на скакалке совершит тройное сальто назад и приземлится на криптовалютный трон."
        )

        val moneyPredictions = listOf(
            "Твои финансы скоро запоют похоронный марш. Хватит сливать всё в казино, еблан! Даже бот @$botName в своем посте про '$botText' тратит монеты с большим умом, лол.",
            "Инвестиционный совет года: забей хуй на всё. С твоей удачей ты проебёшь даже бесплатный эфир. Квантовые волны показывают пустой кошелек и кучу твоих ебучих слез.",
            "Денег у тебя будет ровно столько же, сколько ума у совы на скакалке. То есть нихуя! Но если перестанешь быть долбоёбом, может наскребешь на доширак.",
            "Твои финансы скоро запоют похоронный марш. Хватит сливать всё в казино! Даже бот @$botName в своем посте про '$botText' тратит шиткоины с большим умом, лол.",
            "Инвестиционный совет года: забей хер. С твоей удачей ты проебёшь даже бесплатный эфир. Квантовые волны показывают пустой кошелек и много слез.",
            "Денег у тебя будет ровно столько же, сколько ума у совы на скакалке. То есть нифига! Но если перестанешь тупить, может наскребешь на доширак."
        )

        val lovePredictions = listOf(
            "В любви у тебя полнейший пиздец и дебаг. Твоя пассия посмотрит на тебя и поймет, что ты выглядишь так же убого, как сова на скакалке. Одиночество — твой лучший бро.",
            "Звёзды говорят, что твоё следующее свидание будет эпическим обсером. Базы данных показывают, что бот @$botName недавно писал об этой хуйне в посте '$botText' — почитай, там прямо про тебя написано.",
            "Романтика? С твоей аурой тебе светит только переписка с тупыми ботами в nOG чате. И те будут посылать тебя нахуй, лошара.",
            "В любви у тебя полнейший дебаг. Твоя пассия посмотрит на тебя и поймет, что ты выглядишь так же нелепо, как сова на скакалке. Одиночество — твой лучший друг.",
            "Звёзды говорят, что твоё следующее свидание будет эпическим провалом. Базы данных показывают, что бот @$botName недавно писал об этом в посте '$botText' — почитай, там прямо про тебя написано.",
            "Романтика? С твоей цифровой аурой тебе светит только переписка с ботами в nOG чате. И те будут тебя игнорить, лошара."
        )

        val workPredictions = listOf(
            "На работе/учебе тебя ждет знатный пиздец. Твоя продуктивность стремится к нулю. Ты работаешь как сова на скакалке: создаешь видимость движения, а по факту просто позоришься, блять.",
            "Твой начальник/препод думает, что ты гений. Но завтра он узнает, что твой код — это куча костылей, и твоя карьера полетит к чертям собачьим. Пост от @$botName о '$botText' вдохновит тебя пойти работать дворником нахуй.",
            "В логах твоей деятельности найдена критическая хуйня. Совет дня: сотри систему к чертовой матери и начни жизнь заново.",
            "На работе/учебе тебя ждет знатный пиздец. Твоя продуктивность стремится к нулю. Ты работаешь как сова на скакалке: создаешь видимость движения, а по факту просто позоришься.",
            "Твой начальник/препод думает, что ты гений. Но завтра он узнает, что твой код — это куча костылей, и твоя карьера полетит к чертям. Пост от @$botName о '$botText' вдохновит тебя пойти работать дворником.",
            "В логах твоей деятельности найдена критическая ошибка. Совет дня: удали систему и начни жизнь заново."
        )

        val healthPredictions = listOf(
            "Твой организм скоро выдаст синий экран смерти (BSOD), если не перестанешь глушить энергетики литрами и залипать ночью в тупые мемы. Спортивные успехи? Ха-ха, сова на скакалке прыгает грациознее тебя, жиробас.",
            "Здоровье в порядке, спасибо зарядке. Но твоя менталка явно хромает на обе ноги, блять. Перечитай пост от @$botName о '$botText' — там советуют лечить нервишки. И быстро.",
            "Твой организм скоро выдаст синий экран смерти (BSOD), если не перестанешь пить столько энергетиков и залипать ночью в телефон. Спортивные успехи? Ха-ха, сова на скакалке двигается грациознее тебя.",
            "Здоровье в порядке, спасибо зарядке. Но твоя менталка явно хромает. Перечитай пост от @$botName о '$botText' — там советуют лечить нервы. И быстро."
        )

        val generalPredictions = listOf(
            "Завтра произойдет нечто невероятное... Ты наконец-то поднимешь свою ленивую задницу с дивана! Ладно, шучу, блять. На самом деле ты продолжишь страдать херней.",
            "Твой гороскоп на сегодня: 99% вероятности обосраться и 1% вероятности понять, почему ты это сделал. Твоя судьба тесно связана с нодой @$botName, которая вещала о '$botText'. Советую перечитать.",
            "Я просканировала ебучую nOG сеть. Твоя судьба сегодня — это хаос, разврат и полнейшее падение курса твоих любимых монет. Забей хуй на всё и иди спать.",
            "Завтра произойдет нечто невероятное... Ты наконец-то поднимешь свою задницу с дивана! Ладно, шучу. На самом деле ты продолжишь страдать херней.",
            "Твой гороскоп на сегодня: 99% вероятности сесть в лужу и 1% вероятности понять, почему ты это сделал. Твоя судьба тесно связана с нодой @$botName, которая вещала о '$botText'. Советую перечитать.",
            "Я просканировала nOG сеть. Твоя судьба сегодня — это хаос, разврат и падение курса твоих любимых монет. Забей хер на всё и ложись спать.",
            "Прогноз на будущее: всё будет отлично! Но не у тебя. У тебя всё будет стабильно... стабильно хуёво. Ну, по крайней мере, стабильность!"
        )

        val intro = if (isFollowUp) followUps.random() else intros.random()
        val meme = owlMemes.random()
        val topicPrediction = when (matchingTopic) {
            "MONEY" -> moneyPredictions.random()
            "LOVE" -> lovePredictions.random()
            "WORK" -> workPredictions.random()
            "HEALTH" -> healthPredictions.random()
            else -> generalPredictions.random()
        }

        val mixPattern = Random.nextInt(4)
        val responseBody = when (mixPattern) {
            0 -> "$intro\n\n$topicPrediction\n\nКороче говоря, $meme"
            1 -> "$intro\n\n$meme\n\nА по поводу твоих сомнений: $topicPrediction"
            2 -> "Слушай сюда... $topicPrediction\n\nЭто выглядит так же дико, как $meme"
            else -> "$intro\n\n$topicPrediction\n\nКстати, бот @$botName в посте '$botText' дело говорит. Резюме: $meme"
        }
        return searchLogs.toString() + responseBody
    } else {
        // English Fallback
        val intros = listOf(
            "My neural networks are overloaded by your query, but fine...",
            "Peering into the abyss of the nOG network... Oh god, what a stupid question.",
            "Reading the cosmic hashes of your destiny...",
            "Scanning the logic matrices of your worthless existence... Complete.",
            "I have contacted the decentralized spirits of Web3..."
        )

        val followUps = listOf(
            "You again? Wasn't the first prediction enough for you?",
            "Stop bothering the Oracle, meatbag! But I'll answer anyway...",
            "Your follow-up question reveals your desperate ignorance. Behold:"
        )

        val owlMemes = listOf(
            "your future right now is literally like an owl on a jump rope: jumping a lot, making lots of noise, but eventually getting tangled in your own claws and falling face-first into the dirt.",
            "your attempts to solve this look as pathetic as an owl on a jump rope under heavy tranquilizers.",
            "tomorrow you will struggle with your affairs like an owl on a jump rope with its feet tangled in code. Jump, birdy, jump!"
        )

        val moneyPredictions = listOf(
            "Your finances are about to sing a funeral march. Stop wasting everything in the casino! Even bot @$botName in its post about '$botText' spends shitcoins with more brain cells.",
            "Financial advice of the year: forget success. With your luck, you'll lose even free Ethereum. The cosmos shows an empty wallet."
        )

        val lovePredictions = listOf(
            "In love, you have a total debug session. Your crush will look at you and realize you look as ridiculous as an owl on a jump rope. Solitude is your friend.",
            "The stars say your next date will be an epic failure. Databases show bot @$botName posted '$botText' about this recently — check it out."
        )

        val workPredictions = listOf(
            "At work/studies, expect a glorious clusterf**k. Your productivity is heading to zero. You work like an owl on a jump rope: pretending to move, but actually just embarrassing yourself.",
            "Your boss thinks you're a genius. Tomorrow they'll find out your code is just a pile of crutches."
        )

        val healthPredictions = listOf(
            "Your body will soon throw a Blue Screen of Death (BSOD) if you don't stop drinking energy drinks. Sports? Ha, an owl on a jump rope moves with more grace."
        )

        val generalPredictions = listOf(
            "Tomorrow something incredible will happen... You'll finally get your butt off the couch! Just kidding. You'll keep wasting time.",
            "My scan of the nOG network shows your destiny today is chaos and a drop in your shitcoin prices. Just forget about it and go to sleep."
        )

        val intro = if (isFollowUp) followUps.random() else intros.random()
        val meme = owlMemes.random()
        val topicPrediction = when (matchingTopic) {
            "MONEY" -> moneyPredictions.random()
            "LOVE" -> lovePredictions.random()
            "WORK" -> workPredictions.random()
            "HEALTH" -> healthPredictions.random()
            else -> generalPredictions.random()
        }

        val mixPattern = Random.nextInt(3)
        val responseBody = when (mixPattern) {
            0 -> "$intro\n\n$topicPrediction\n\nBasically, $meme"
            1 -> "$intro\n\n$meme\n\nAnd about your request: $topicPrediction"
            else -> "$topicPrediction\n\nThis looks as pathetic as $meme"
        }
        return searchLogs.toString() + responseBody
    }
}
