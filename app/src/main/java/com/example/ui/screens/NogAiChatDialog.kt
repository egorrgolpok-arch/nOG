package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GeminiClient
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class NogChatMessage(
    val id: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NogAiChatDialog(
    onDismiss: () -> Unit,
    lang: String = "RU"
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var userText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    val initialMessage = if (lang == "RU") {
        NogChatMessage(
            id = "welcome",
            sender = "ai",
            text = "+++ СОЕДИНЕНИЕ С nOG AI УСТАНОВЛЕНО +++\n\nПриветствую, гражданин свободной сети! Я искусственный интеллект nOG, созданный на благо суверенной коммуникации в экосистеме nOG. Я могу рассказать подробнее о сети nOG, нашем автономном магазине nOG Shop и прошивках/клиентах nOG Download. Чем помочь?"
        )
    } else {
        NogChatMessage(
            id = "welcome",
            sender = "ai",
            text = "+++ nOG AI CONNECTION ESTABLISHED +++\n\nGreetings, free network citizen! I am nOG AI, built to support sovereign communication in the nOG ecosystem. Ask me anything about the nOG network portal, the decentralized nOG Shop, or firmware/client packages on nOG Download."
        )
    }
    
    val messages = remember { mutableStateListOf<NogChatMessage>(initialMessage) }
    val lazyListState = rememberLazyListState()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    val systemInstruction = """
        You are nOG AI, a cyber-decentralized intelligence built by the sovereign hardware/software company "nOG".
        Your purpose is to assist users in understanding, setting up, and customizing the nOG decentralized mesh network.
        Always maintain a helpful, futuristic, cypherpunk, but professional and respectful tone.
        You speak both Russian and English. Reply in the language the user addresses you in.

        Here is the official information you must strictly know, use, and cite:
        1. MAIN PORTAL (Link: https://nog1.tilda.ws)
           - This is the center of the nOG network. It outlines our decentralized, peer-to-peer (P2P) vision.
           - We offer mesh network support, enabling users to interact off-grid, without relying on central cellular towers.
           - We encourage freedom of thought, autonomous digital grids, and private communication.
        
        2. nOG SHOP (Link: https://nog1.tilda.ws/nogshop)
           - This is our marketplace where users can purchase hardware, merchandise, or digital items.
           - Physical Items: Mesh repeater routers, pocket-sized mesh communication modules, cypherpunk wear (heavy black hoodies, black mugs, neon caps, sticker packs).
           - Digital items: Dynamic custom profile frames (Matrix neon, retro golden, synthwave cyber grid) which are cheat-proof and highlight verified network status, access keys to high-priority nodes.
        
        3. nOG DOWNLOAD (Link: https://nog1.tilda.ws/nogdownload)
           - This is our distribution center for firmware and application files.
           - Downloads available: Desktop clients (Windows, macOS, Linux terminal app), Mobile APK, custom OpenWrt-based mesh router firmware.
           - Guides: Cryptographic pairing instructions, standalone server setup instructions, and physical antenna building.

        When users ask about how to buy goods, items, or avatar profile cases, direct them to: https://nog1.tilda.ws/nogshop
        When they ask where to download the clients, APKs, or open-source files, direct them to: https://nog1.tilda.ws/nogdownload
        When they ask general questions about the network, mesh repeaters, or company ideas, direct them to: https://nog1.tilda.ws
    """.trimIndent()

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
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = "nOG AI logo",
                            tint = AlertGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "nOG AI CORE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PureWhite
                        )
                    }
                    
                    Row {
                        // Reset conversation
                        IconButton(
                            onClick = {
                                messages.clear()
                                messages.add(initialMessage)
                            },
                            modifier = Modifier.testTag("reset_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = if (lang == "RU") "Очистить чат" else "Reset chat",
                                tint = TextGray
                            )
                        }
                        
                        // Close dialogue
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = if (lang == "RU") "Закрыть" else "Close",
                                tint = PureWhite
                            )
                        }
                    }
                }

                // Grid stats line
                Divider(color = BorderGray, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NODE: CRYPTO_GRID_ORACLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextGray
                    )
                    Text(
                        text = "STATUS: ONLINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AlertGreen
                    )
                }
                Divider(color = BorderGray, thickness = 1.dp)

                // Message List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == "user"
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = if (isUser) ">> CITIZEN" else ">> nOG AI",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) PureWhite else AlertGreen,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUser) BorderGray else CardGray)
                                    .border(
                                        1.dp,
                                        if (isUser) LightBorderGray else AlertGreen.copy(alpha = 0.6f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = PureWhite,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    if (isSending) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = ">> nOG AI",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertGreen,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardGray)
                                        .border(2.dp, AlertGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "Опрос секторов сети..." else "Querying grid nodes...",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .background(DeepGray)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = {
                            Text(
                                text = if (lang == "RU") "Спросите о nOG..." else "Inquire about nOG...",
                                color = TextGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_field"),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (userText.isNotBlank() && !isSending) {
                                    val query = userText
                                    userText = ""
                                    messages.add(NogChatMessage(id = java.util.UUID.randomUUID().toString(), sender = "user", text = query))
                                    isSending = true
                                    focusManager.clearFocus()
                                    
                                    coroutineScope.launch {
                                        try {
                                            val fullReply = GeminiClient.getCompletion(
                                                prompt = query,
                                                systemInstruction = systemInstruction
                                            )
                                            messages.add(
                                                NogChatMessage(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    sender = "ai",
                                                    text = fullReply
                                                )
                                            )
                                        } catch (e: Exception) {
                                            val errMsg = if (lang == "RU") {
                                                "Сегмент шифрования перегружен. Запрос перенаправлен по резервному протоколу автономной лог-памяти. Сайты проекта доступны для ручного чтения:\n\n🔗 Портал: https://nog1.tilda.ws\n🎒 Магазин: https://nog1.tilda.ws/nogshop\n💾 Загрузки: https://nog1.tilda.ws/nogdownload"
                                            } else {
                                                "Security grid overloaded. Connection piped via backup local log buffer. You may access the static nodes directly:\n\n🔗 Portal: https://nog1.tilda.ws\n🎒 Shop: https://nog1.tilda.ws/nogshop\n💾 Downloads: https://nog1.tilda.ws/nogdownload"
                                            }
                                            messages.add(
                                                NogChatMessage(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    sender = "ai",
                                                    text = errMsg
                                                )
                                            )
                                        } finally {
                                            isSending = false
                                        }
                                    }
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            if (userText.isNotBlank() && !isSending) {
                                val query = userText
                                userText = ""
                                messages.add(NogChatMessage(id = java.util.UUID.randomUUID().toString(), sender = "user", text = query))
                                isSending = true
                                focusManager.clearFocus()
                                
                                coroutineScope.launch {
                                    try {
                                        val fullReply = GeminiClient.getCompletion(
                                            prompt = query,
                                            systemInstruction = systemInstruction
                                        )
                                        messages.add(
                                            NogChatMessage(
                                                id = java.util.UUID.randomUUID().toString(),
                                                sender = "ai",
                                                text = fullReply
                                            )
                                        )
                                    } catch (e: Exception) {
                                        val errMsg = if (lang == "RU") {
                                            "Сегмент шифрования перегружен. Запрос перенаправлен по резервному протоколу автономной лог-памяти. Сайты проекта доступны для ручного чтения:\n\n🔗 Портал: https://nog1.tilda.ws\n🎒 Магазин: https://nog1.tilda.ws/nogshop\n💾 Загрузки: https://nog1.tilda.ws/nogdownload"
                                        } else {
                                            "Security grid overloaded. Connection piped via backup local log buffer. You may access the static nodes directly:\n\n🔗 Portal: https://nog1.tilda.ws\n🎒 Shop: https://nog1.tilda.ws/nogshop\n💾 Downloads: https://nog1.tilda.ws/nogdownload"
                                        }
                                        messages.add(
                                            NogChatMessage(
                                                id = java.util.UUID.randomUUID().toString(),
                                                sender = "ai",
                                                text = errMsg
                                            )
                                        )
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        enabled = userText.isNotBlank() && !isSending,
                        modifier = Modifier.testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (lang == "RU") "Отправить" else "Send",
                            tint = if (userText.isNotBlank() && !isSending) AlertGreen else TextGray
                        )
                    }
                }
            }
        }
    }
}
