package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import com.example.ui.ChatMessage

@Composable
fun NogAiScreen(
    viewModel: SocialViewModel,
    innerPadding: PaddingValues
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.chatLoading.collectAsState()
    var textInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            
            // --- Screen Banner Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray)
                    .background(DeepGray)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "nOG AI Icon",
                    tint = PureWhite,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "nOG AI_MAIN_FRAME v3.0",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unconstrained neural auditor of the cybernetic space",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Conversations Scrolling List ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg = msg)
                }
                
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(DeepGray, RoundedCornerShape(4.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = PureWhite,
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Вычисление синапсов nOG AI...",
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

            // --- Chat Input Controls in Black & White Contrast ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Задать любой вопрос nOG AI (в духе Grok)...", color = TextGray, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = StarkWhite,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = BorderGray,
                        cursorColor = PureWhite
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendNogAiMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("chat_send_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Отправить сообщение"
                    )
                }
            }
        }
    }
}

// --- Composable: Speech Bubble in Dark Brutal style ---
@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignEnd = msg.isUser
    val bubbleColor = if (alignEnd) PureWhite else DeepGray
    val textColor = if (alignEnd) PureBlack else StarkWhite
    val borderColor = if (alignEnd) PureWhite else BorderGray

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(4.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = msg.text,
                    color = textColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (alignEnd) "Вы" else "nOG AI Assistant",
                color = TextGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
