package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.SocialViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// --- DATA STRUCTURES ---
data class AvatarDecoration(
    val id: Int,
    val name: String,
    val rarity: String, // "ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ", "ЭКСКЛЮЗИВНАЯ"
    val basePrice: Int,
    val styleType: Int // matches rendering logic
)

// --- DETERMINISTIC PROCEDURAL GENERATOR ---
object DecorationGenerator {
    private val adjectivesRu = listOf(
        "Анонимный", "Резиновый", "Шитпостинговый", "Забаненный", "Гипертрофированный", "Силиконовый", 
        "Полупроводниковый", "Паленый", "Оверклокнутый", "Майнерский", "Грязный", "Жидкий", 
        "Нейросетевой", "Пиксельный", "Копченый", "Дутый", "Квантовый", "Ржавый", "Кринжовый", 
        "Базированный", "Офлайновый", "Ультрафиолетовый", "Синтетический", "Глючный", "Пьяный", 
        "Токсичный", "Аналоговый", "Картонный", "Админский", "Мемный", "Шальной"
    )

    private val nounsRu = listOf(
        "Процессор", "Резистор", "Конденсатор", "Аватар", "Майнер", "Админ", "Тред", "Кабель", 
        "Кулер", "Рекорд", "Модератор", "Чипсет", "Абузер", "Носок", "Верификатор", "Корпус", 
        "Транзистор", "Баг", "Код", "Мануал", "Текстурпак", "Провод", "Ноут", "Шланг", "Скриншот", 
        "Блокчейн", "Сервер", "Анонимус", "Антивирус", "Интеграл"
    )

    private val suffixesRu = listOf(
        "в масле", "от Габена", "без регистрации", "3.0", "Pro Max", "из DNS", "на коленке", 
        "в депрессии", "под пивом", "киберпанк", "с подсветкой", "из 2007"
    )

    private val adjectivesEn = listOf(
        "Anonymous", "Rubber", "Shitpost", "Banned", "Overloaded", "Silicon", 
        "Sourced", "Pirated", "Overclocked", "Mining", "Dirty", "Liquid", 
        "Neural", "Pixelated", "Smoked", "Bloated", "Quantum", "Rusty", "Cringey", 
        "Based", "Offline", "Ultraviolet", "Synthetic", "Glitchy", "Drunk", 
        "Toxic", "Analog", "Cardboard", "Admin", "Meme", "Wild"
    )

    private val nounsEn = listOf(
        "CPU", "Resistor", "Capacitor", "Avatar", "Miner", "Sysadmin", "Thread", "Wire", 
        "Cooler", "Record", "Mod", "Chipset", "Abuser", "Sock", "Validator", "Case", 
        "Transistor", "Bug", "Code", "Manual", "TexturePack", "Cable", "Laptop", "Pipe", "Screenshot", 
        "Blockchain", "Host", "Anon", "Antivirus", "Gate"
    )

    private val suffixesEn = listOf(
        "in mineral oil", "by Gabe", "unregistered", "3.0", "Pro Max", "OEM edition", "cooked on knees", 
        "depressed", "under beer", "cyberpunk", "RGB styled", "since 2007"
    )

    fun generateDecoration(id: Int, lang: String): AvatarDecoration {
        val rand = Random(id.toLong() * 1117)
        
        // 15% chance to be named after user or predefined bot handle
        val isNamedAfterContact = rand.nextInt(100) < 15
        
        val rawName = if (isNamedAfterContact) {
            val contactHandle = when (id % 12) {
                0 -> "@user_node"
                1 -> "@bio_node"
                2 -> "@admin"
                3 -> "@silicon_core"
                4 -> "@cyber_god"
                5 -> "@elon_bot"
                6 -> "@crypto_kid"
                7 -> "@meme_engine"
                8 -> "@glitch_net"
                9 -> "@bio_hacker"
                10 -> "@quantum_ai"
                else -> "@retro_node"
            }
            if (lang == "RU") {
                val prefixes = listOf("Ирокез", "Корона", "Сияние", "Маска", "Нимб", "Шлем", "Очки", "Кулер")
                "${prefixes[rand.nextInt(prefixes.size)]} $contactHandle"
            } else {
                val prefixes = listOf("Mohawk of", "Crown of", "Symmetry of", "Visor of", "Halo of", "Helmet of", "Glow of", "Fan of")
                "${prefixes[rand.nextInt(prefixes.size)]} $contactHandle"
            }
        } else {
            val adj = if (lang == "RU") adjectivesRu[rand.nextInt(adjectivesRu.size)] else adjectivesEn[rand.nextInt(adjectivesEn.size)]
            val noun = if (lang == "RU") nounsRu[rand.nextInt(nounsRu.size)] else nounsEn[rand.nextInt(nounsEn.size)]
            val suff = if (lang == "RU") suffixesRu[rand.nextInt(suffixesRu.size)] else suffixesEn[rand.nextInt(suffixesEn.size)]
            
            if (rand.nextBoolean()) {
                "$adj $noun $suff"
            } else {
                "$adj $noun"
            }
        }

        // Rarities deterministically
        val rarity = when (id % 10) {
            in 0..5 -> "ОБЫЧНАЯ" // 60% Common
            in 6..7 -> "РЕДКАЯ"  // 20% Rare
            8 -> "АХУЕННАЯ"      // 10% Awesome
            else -> "НЕВЕБЕЙШАЯ" // 10% Insane
        }

        val basePrice = when (rarity) {
            "ОБЫЧНАЯ" -> 15000
            "РЕДКАЯ" -> 35000
            "АХУЕННАЯ" -> 75000
            else -> 150000
        }

        val styleType = (id % 10) + 1

        return AvatarDecoration(id, rawName, rarity, basePrice, styleType)
    }

    // 10 Luxury Exclusives
    fun getExclusiveDecorations(lang: String): List<AvatarDecoration> {
        return listOf(
            AvatarDecoration(
                201, 
                if (lang == "RU") "Плазменные Крылья Демона 🔥" else "Plasma Demon Wings 🔥", 
                "ЭКСКЛЮЗИВНАЯ", 350000, 11
            ),
            AvatarDecoration(
                202, 
                if (lang == "RU") "Рог Радужного Единорога 🦄" else "Rainbow Unicorn Horn 🦄", 
                "ЭКСКЛЮЗИВНАЯ", 400000, 12
            ),
            AvatarDecoration(
                203, 
                if (lang == "RU") "Орбита Космической Сингулярности 🌌" else "Singularity Event Horizon 🌌", 
                "ЭКСКЛЮЗИВНАЯ", 450000, 13
            ),
            AvatarDecoration(
                204, 
                if (lang == "RU") "Аура Золотых Искр Творца ✨" else "Golden Spark Aura ✨", 
                "ЭКСКЛЮЗИВНАЯ", 500000, 14
            ),
            AvatarDecoration(
                205, 
                if (lang == "RU") "Токсичный Кибернетический Глитч 🦠" else "Toxic Bio Cyberglitch 🦠", 
                "ЭКСКЛЮЗИВНАЯ", 550000, 15
            ),
            AvatarDecoration(
                206, 
                if (lang == "RU") "Адская Корона Кровавого Лорда 👑" else "Hellish Lord Crown 👑", 
                "ЭКСКЛЮЗИВНАЯ", 600000, 16
            ),
            AvatarDecoration(
                207, 
                if (lang == "RU") "Оверлорд-Монокультура 🧬" else "Overlord Monoculture 🧬", 
                "ЭКСКЛЮЗИВНАЯ", 650000, 17
            ),
            AvatarDecoration(
                208, 
                if (lang == "RU") "Сияние Истинного Избранника 🌟" else "True Chosen One's Halo 🌟", 
                "ЭКСКЛЮЗИВНАЯ", 700000, 18
            ),
            AvatarDecoration(
                209, 
                if (lang == "RU") "Хакерский Код Матрицы 👾" else "Matrix Hack Screen 👾", 
                "ЭКСКЛЮЗИВНАЯ", 750000, 19
            ),
            AvatarDecoration(
                210, 
                if (lang == "RU") "Черный Нимб Шестого Ангела 🪽" else "Sixth Angel Dark Ring 🪽", 
                "ЭКСКЛЮЗИВНАЯ", 1000000, 20
            )
        )
    }

    // Get 200 normal + 10 exclusives
    fun getAll(lang: String): List<AvatarDecoration> {
        val list = mutableListOf<AvatarDecoration>()
        for (i in 1..200) {
            list.add(generateDecoration(i, lang))
        }
        return list
    }
}

// --- COMPOSE RENDER COMPONENT ---
@Composable
fun AvatarWithDecoration(
    avatarUrl: Any?,
    decorationId: Int?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48,
    borderWidthDp: Int = 1
) {
    Box(
        modifier = modifier
            .size((sizeDp + 16).dp), // leave margin for custom canvas elements
        contentAlignment = Alignment.Center
    ) {
        // Current user or Bot circular avatar
        AsyncImage(
            model = avatarUrl ?: "https://robohash.org/unknown.png?size=200x200&set=set1",
            contentDescription = "Avatar with Dec",
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(borderWidthDp.dp, PureWhite, CircleShape),
            contentScale = ContentScale.Crop,
            error = rememberVectorPainter(Icons.Filled.AccountCircle),
            placeholder = rememberVectorPainter(Icons.Filled.AccountCircle)
        )

        // Draw custom decoration if active
        if (decorationId != null && decorationId > 0) {
            val infiniteTransition = rememberInfiniteTransition()
            
            // Animation values for visual richness
            val angleRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing)
                )
            )

            val scalePulse by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val breatheAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Canvas(
                modifier = Modifier
                    .size((sizeDp + 16).dp)
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val avatarRadius = (sizeDp.dp.toPx()) / 2f
                val decorRadius = avatarRadius + 4.dp.toPx()

                // Procedurally resolve styleType & colors using deterministic math on decorationId
                val decorationItem = if (decorationId >= 201) {
                    DecorationGenerator.getExclusiveDecorations("EN").find { it.id == decorationId }
                } else {
                    DecorationGenerator.generateDecoration(decorationId, "EN")
                }
                
                val styleType = decorationItem?.styleType ?: ((decorationId % 10) + 1)
                
                val premiumProceduralColors = listOf(
                    Color(0xFFFF1493), // Hot Pink
                    Color(0xFF00FFFF), // Electric Cyan
                    Color(0xFFFF4500), // Fire Orange
                    Color(0xFF38EF7D), // Neon Green
                    Color(0xFFD500F9), // Purple Power
                    Color(0xFFFFEA00), // Sunshine Yellow
                    Color(0xFF00E5FF), // Cyber Aqua
                    Color(0xFFFF1744), // Crimson Red
                    Color(0xFF9C27B0), // Mystical Violet
                    Color(0xFF3F51B5), // Deep Indigo
                    Color(0xFF00E676), // Spring Mint Glow
                    Color(0xFFFF9100)  // Gold Amber
                )

                val primaryColor = premiumProceduralColors[decorationId % premiumProceduralColors.size]
                val secondaryColor = premiumProceduralColors[(decorationId + 3) % premiumProceduralColors.size]
                val tertiaryColor = premiumProceduralColors[(decorationId + 7) % premiumProceduralColors.size]

                when (styleType) {
                    1 -> { // Neon Procedural Ring with multiple dots
                        drawCircle(
                            color = primaryColor.copy(alpha = breatheAlpha),
                            radius = decorRadius,
                            style = Stroke(width = (2.dp.toPx() + (decorationId % 3) * 0.5f.dp.toPx()))
                        )
                        // Dynamic number of dots orbiting
                        val dotCount = 1 + (decorationId % 4)
                        val rotDir = if (decorationId % 2 == 0) 1f else -1f
                        for (i in 0 until dotCount) {
                            val orbitAngle = angleRotation * rotDir + i * (360f / dotCount)
                            val rad = Math.toRadians(orbitAngle.toDouble())
                            drawCircle(
                                color = if (i == 0) Color.White else secondaryColor.copy(alpha = 0.9f),
                                radius = (2.dp.toPx() + (i % 2).dp.toPx()),
                                center = Offset(
                                    centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat(),
                                    centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat()
                                )
                            )
                        }
                    }
                    2 -> { // Concentric Cyber Dashed Rings
                        // Inner dasher
                        val innerRadius = decorRadius - 2.dp.toPx()
                        val innerDash = floatArrayOf(15f + (decorationId % 15), 15f + (decorationId % 10))
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.8f),
                            radius = innerRadius,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(innerDash, angleRotation))
                        )
                        // Outer dasher spinning backwards
                        val outerDash = floatArrayOf(30f, 20f)
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.6f),
                            radius = decorRadius + 1.5.dp.toPx(),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(outerDash, -angleRotation))
                        )
                    }
                    3 -> { // Procedural Crown with Crystals
                        val crownStyle = decorationId % 3
                        val crownColor = if (decorationId % 4 == 0) Color(0xFFFFD700) else primaryColor // Gold or primary neon color
                        
                        if (crownStyle == 0) {
                            // Elegant Royal Crown
                            val crownPath = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 0.75f)
                                lineTo(centerOffset.x - avatarRadius * 0.5f, centerOffset.y - avatarRadius * 1.25f)
                                lineTo(centerOffset.x, centerOffset.y - avatarRadius * 0.85f)
                                lineTo(centerOffset.x + avatarRadius * 0.5f, centerOffset.y - avatarRadius * 1.25f)
                                lineTo(centerOffset.x + avatarRadius * 0.7f, centerOffset.y - avatarRadius * 0.75f)
                                close()
                            }
                            drawPath(crownPath, color = crownColor)
                            // Crystals
                            drawCircle(secondaryColor, radius = 2.dp.toPx(), center = Offset(centerOffset.x - avatarRadius * 0.5f, centerOffset.y - avatarRadius * 1.25f))
                            drawCircle(tertiaryColor, radius = 2.dp.toPx(), center = Offset(centerOffset.x + avatarRadius * 0.5f, centerOffset.y - avatarRadius * 1.25f))
                            drawCircle(Color.White, radius = 2.dp.toPx(), center = centerOffset.copy(y = centerOffset.y - avatarRadius * 0.85f))
                        } else if (crownStyle == 1) {
                            // Tech Visor Crown
                            val crownPath = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.8f)
                                lineTo(centerOffset.x - avatarRadius * 0.3f, centerOffset.y - avatarRadius * 1.4f)
                                lineTo(centerOffset.x, centerOffset.y - avatarRadius * 1.1f)
                                lineTo(centerOffset.x + avatarRadius * 0.3f, centerOffset.y - avatarRadius * 1.4f)
                                lineTo(centerOffset.x + avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.8f)
                                close()
                            }
                            drawPath(crownPath, color = secondaryColor)
                            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(centerOffset.x, centerOffset.y - avatarRadius * 1.1f))
                        } else {
                            // Laurel Wreath top arc
                            for (i in -3..3) {
                                val leafAngle = i * 20f - 90f
                                val rad = Math.toRadians(leafAngle.toDouble())
                                drawCircle(
                                    color = secondaryColor.copy(alpha = breatheAlpha),
                                    radius = 3.dp.toPx(),
                                    center = Offset(
                                        centerOffset.x + (decorRadius + 1.dp.toPx()) * kotlin.math.cos(rad).toFloat(),
                                        centerOffset.y + (decorRadius + 1.dp.toPx()) * kotlin.math.sin(rad).toFloat()
                                    )
                                )
                            }
                        }
                    }
                    4 -> { // Blazing Fire Border (ProcedURAL COLORS!)
                        // Customized Fire: could be magma, frosty blue, bio-green, shadow fire
                        drawCircle(
                            color = primaryColor,
                            radius = decorRadius * scalePulse,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = secondaryColor,
                            radius = (decorRadius - 2.dp.toPx()) * scalePulse,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Add floating fire sparks
                        for (i in 0..2) {
                            val sAngle = angleRotation * 1.5f + (i * 120)
                            val rad = Math.toRadians(sAngle.toDouble())
                            drawCircle(
                                color = tertiaryColor.copy(alpha = breatheAlpha),
                                radius = 2.5.dp.toPx(),
                                center = Offset(
                                    centerOffset.x + (decorRadius + 3.dp.toPx()) * kotlin.math.cos(rad).toFloat(),
                                    centerOffset.y + (decorRadius + 3.dp.toPx()) * kotlin.math.sin(rad).toFloat()
                                )
                            )
                        }
                    }
                    5 -> { // Kitty Cat / Bunny / Tech Ears
                        val earStyle = decorationId % 4
                        val earColor = primaryColor
                        
                        if (earStyle == 0) {
                            // Cat Ears
                            val leftEar = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.5f)
                                lineTo(centerOffset.x - avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.2f)
                                lineTo(centerOffset.x - avatarRadius * 0.3f, centerOffset.y - avatarRadius * 0.8f)
                                close()
                            }
                            val rightEar = Path().apply {
                                moveTo(centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.5f)
                                lineTo(centerOffset.x + avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.2f)
                                lineTo(centerOffset.x + avatarRadius * 0.3f, centerOffset.y - avatarRadius * 0.8f)
                                close()
                            }
                            drawPath(leftEar, color = earColor)
                            drawPath(rightEar, color = earColor)
                        } else if (earStyle == 1) {
                            // Long Bunny Ears
                            val leftEar = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.6f)
                                cubicTo(
                                    centerOffset.x - avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.7f,
                                    centerOffset.x - avatarRadius * 0.1f, centerOffset.y - avatarRadius * 1.7f,
                                    centerOffset.x - avatarRadius * 0.2f, centerOffset.y - avatarRadius * 0.7f
                                )
                            }
                            val rightEar = Path().apply {
                                moveTo(centerOffset.x + avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.6f)
                                cubicTo(
                                    centerOffset.x + avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.7f,
                                    centerOffset.x + avatarRadius * 0.1f, centerOffset.y - avatarRadius * 1.7f,
                                    centerOffset.x + avatarRadius * 0.2f, centerOffset.y - avatarRadius * 0.7f
                                )
                            }
                            drawPath(leftEar, color = secondaryColor)
                            drawPath(rightEar, color = secondaryColor)
                        } else if (earStyle == 2) {
                            // Cyber Tech Antennas
                            drawRect(
                                color = secondaryColor,
                                topLeft = Offset(centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.3f),
                                size = Size(2.dp.toPx(), avatarRadius * 0.6f)
                            )
                            drawCircle(primaryColor, radius = 3.dp.toPx(), center = Offset(centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.3f))
                            
                            drawRect(
                                color = secondaryColor,
                                topLeft = Offset(centerOffset.x + avatarRadius * 0.7f - 2.dp.toPx(), centerOffset.y - avatarRadius * 1.3f),
                                size = Size(2.dp.toPx(), avatarRadius * 0.6f)
                            )
                            drawCircle(primaryColor, radius = 3.dp.toPx(), center = Offset(centerOffset.x + avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.3f))
                        } else {
                            // Wing Crests
                            val leftWing = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.4f)
                                lineTo(centerOffset.x - avatarRadius * 1.2f, centerOffset.y - avatarRadius * 0.9f)
                                lineTo(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.8f)
                            }
                            val rightWing = Path().apply {
                                moveTo(centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.4f)
                                lineTo(centerOffset.x + avatarRadius * 1.2f, centerOffset.y - avatarRadius * 0.9f)
                                lineTo(centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.8f)
                            }
                            drawPath(leftWing, color = tertiaryColor)
                            drawPath(rightWing, color = tertiaryColor)
                        }
                    }
                    6 -> { // Red / Cyber / Void Horns
                        val hornStyle = decorationId % 3
                        val hornColor = primaryColor
                        
                        if (hornStyle == 0) {
                            val leftHorn = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.7f)
                                quadraticTo(
                                    centerOffset.x - avatarRadius * 1.0f, centerOffset.y - avatarRadius * 1.2f,
                                    centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.3f
                                )
                                quadraticTo(
                                    centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.0f,
                                    centerOffset.x - avatarRadius * 0.3f, centerOffset.y - avatarRadius * 0.8f
                                )
                                close()
                            }
                            val rightHorn = Path().apply {
                                moveTo(centerOffset.x + avatarRadius * 0.6f, centerOffset.y - avatarRadius * 0.7f)
                                quadraticTo(
                                    centerOffset.x + avatarRadius * 1.0f, centerOffset.y - avatarRadius * 1.2f,
                                    centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.3f
                                )
                                quadraticTo(
                                    centerOffset.x + avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.0f,
                                    centerOffset.x + avatarRadius * 0.3f, centerOffset.y - avatarRadius * 0.8f
                                )
                                close()
                            }
                            drawPath(leftHorn, color = hornColor)
                            drawPath(rightHorn, color = hornColor)
                        } else if (hornStyle == 1) {
                            // Celestial Crescent Moon over avatar
                            drawOval(
                                color = secondaryColor.copy(alpha = breatheAlpha),
                                topLeft = Offset(centerOffset.x - 4.dp.toPx(), centerOffset.y - avatarRadius * 1.5f),
                                size = Size(8.dp.toPx(), 8.dp.toPx())
                            )
                        } else {
                            // Dragon horns
                            val leftHorn = Path().apply {
                                moveTo(centerOffset.x - avatarRadius * 0.5f, centerOffset.y - avatarRadius * 0.8f)
                                lineTo(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.5f)
                                lineTo(centerOffset.x - avatarRadius * 0.4f, centerOffset.y - avatarRadius * 0.9f)
                            }
                            val rightHorn = Path().apply {
                                moveTo(centerOffset.x + avatarRadius * 0.5f, centerOffset.y - avatarRadius * 0.8f)
                                lineTo(centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.5f)
                                lineTo(centerOffset.x + avatarRadius * 0.4f, centerOffset.y - avatarRadius * 0.9f)
                            }
                            drawPath(leftHorn, color = secondaryColor)
                            drawPath(rightHorn, color = secondaryColor)
                        }
                    }
                    7 -> { // Matrix Code Rain / Custom Tech Dashed
                        val dashValue = 10f * (1 + decorationId % 4)
                        val blankValue = 15f * (1 + decorationId % 3)
                        drawCircle(
                            color = primaryColor.copy(alpha = breatheAlpha),
                            radius = decorRadius,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashValue, blankValue), angleRotation))
                        )
                        // tech ring crosshairs overlays
                        if (decorationId % 2 == 0) {
                            drawCircle(secondaryColor, radius = decorRadius + 3.dp.toPx(), style = Stroke(width = 0.5.dp.toPx()))
                        }
                    }
                    8 -> { // Sparkling Star Dust Multi-Orbit
                        val starCount = 3 + (decorationId % 4) // Between 3 and 6 orbiting crystals
                        val rotSpeedMultiplier = if (decorationId % 2 == 0) 1.2f else -0.8f
                        
                        for (i in 0 until starCount) {
                            val pAngle = angleRotation * rotSpeedMultiplier + (i * (360f / starCount))
                            val rad = Math.toRadians(pAngle.toDouble())
                            val currentStarColor = if (i % 3 == 0) primaryColor else if (i % 3 == 1) secondaryColor else tertiaryColor
                            
                            drawCircle(
                                color = currentStarColor.copy(alpha = breatheAlpha),
                                radius = (2.2.dp.toPx() + (i % 2).dp.toPx()),
                                center = Offset(
                                    centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat(),
                                    centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat()
                                )
                            )
                        }
                    }
                    9 -> { // Laser Cyber Visor / Target Hud Reticle
                        // Reticle brackets in 4 corners
                        val bracketLen = 6.dp.toPx()
                        val off = decorRadius - 2.dp.toPx()
                        
                        // Top-left
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x - off, centerOffset.y - off), size = Size(bracketLen, 1.5.dp.toPx()))
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x - off, centerOffset.y - off), size = Size(1.5.dp.toPx(), bracketLen))
                        
                        // Top-right
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x + off - bracketLen, centerOffset.y - off), size = Size(bracketLen, 1.5.dp.toPx()))
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x + off, centerOffset.y - off), size = Size(1.5.dp.toPx(), bracketLen))
                        
                        // Bottom-left
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x - off, centerOffset.y + off), size = Size(bracketLen, 1.5.dp.toPx()))
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x - off, centerOffset.y + off - bracketLen), size = Size(1.5.dp.toPx(), bracketLen))
                        
                        // Bottom-right
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x + off - bracketLen, centerOffset.y + off), size = Size(bracketLen, 1.5.dp.toPx()))
                        drawRect(secondaryColor, topLeft = Offset(centerOffset.x + off, centerOffset.y + off - bracketLen), size = Size(1.5.dp.toPx(), bracketLen))

                        // Vertical scanner line sweeping
                        val scanHeightOffset = kotlin.math.sin(Math.toRadians(angleRotation.toDouble() * 2)).toFloat() * avatarRadius
                        drawRect(
                            color = primaryColor.copy(alpha = breatheAlpha * 0.7f),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.9f, centerOffset.y + scanHeightOffset - 1.dp.toPx()),
                            size = Size(avatarRadius * 1.8f, 2.dp.toPx())
                        )
                    }
                    10 -> { // Glowing Angel Holy Ring / Crown of Thorns
                        val isHaloThorns = decorationId % 2 == 0
                        if (isHaloThorns) {
                            // Crown of Thorns
                            drawCircle(
                                color = primaryColor,
                                radius = decorRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            for (i in 0 until 12) {
                                val spikeAngle = i * 30f + angleRotation * 0.3f
                                val rad = Math.toRadians(spikeAngle.toDouble())
                                drawRect(
                                    color = secondaryColor,
                                    topLeft = Offset(
                                        centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat(),
                                        centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat()
                                    ),
                                    size = Size(2.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        } else {
                            // Standard Holy Angelic Halo
                            drawOval(
                                color = primaryColor.copy(alpha = breatheAlpha),
                                topLeft = Offset(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.35f),
                                size = Size(avatarRadius * 1.6f, 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                    
                    // --- EXCLUSIVE PREMIUM DECORATIONS (11 to 20) ---
                    11 -> { // Plasma Demon Wings (ID 201)
                        val leftWing = Path().apply {
                            moveTo(centerOffset.x - avatarRadius * 0.9f, centerOffset.y)
                            cubicTo(
                                centerOffset.x - avatarRadius * 1.5f, centerOffset.y - avatarRadius * 1.2f,
                                centerOffset.x - avatarRadius * 2.2f, centerOffset.y + avatarRadius * 0.4f,
                                centerOffset.x - avatarRadius * 0.9f, centerOffset.y + avatarRadius * 0.8f
                            )
                        }
                        val rightWing = Path().apply {
                            moveTo(centerOffset.x + avatarRadius * 0.9f, centerOffset.y)
                            cubicTo(
                                centerOffset.x + avatarRadius * 1.5f, centerOffset.y - avatarRadius * 1.2f,
                                centerOffset.x + avatarRadius * 2.2f, centerOffset.y + avatarRadius * 0.4f,
                                centerOffset.x + avatarRadius * 0.9f, centerOffset.y + avatarRadius * 0.8f
                            )
                        }
                        drawPath(leftWing, color = Color(0xFF9C27B0))
                        drawPath(rightWing, color = Color(0xFF9C27B0))
                        
                        // golden aura ring
                        drawCircle(Color(0xFFFFD700).copy(alpha = breatheAlpha), radius = decorRadius + 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                    }
                    12 -> { // Rainbow Unicorn Horn (ID 202)
                        val hornPath = Path().apply {
                            moveTo(centerOffset.x - 3.dp.toPx(), centerOffset.y - avatarRadius)
                            lineTo(centerOffset.x, centerOffset.y - avatarRadius * 1.6f)
                            lineTo(centerOffset.x + 3.dp.toPx(), centerOffset.y - avatarRadius)
                            close()
                        }
                        val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta)
                        val brush = Brush.linearGradient(colors = colors, start = Offset(centerOffset.x, centerOffset.y - avatarRadius), end = Offset(centerOffset.x, centerOffset.y - avatarRadius * 1.6f))
                        drawPath(hornPath, brush = brush)
                    }
                    13 -> { // Orbiting Singularity Event Horizon (ID 203)
                        drawCircle(Color.White.copy(alpha = 0.2f), radius = decorRadius + 3.dp.toPx(), style = Stroke(width = 5.dp.toPx()))
                        drawCircle(Color.Black, radius = decorRadius + 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                        drawCircle(Color(0xFFE040FB), radius = decorRadius, style = Stroke(width = 1.dp.toPx()))
                        
                        // orbiting star
                        val rad = Math.toRadians((angleRotation * 2).toDouble())
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 4.dp.toPx(),
                            center = Offset(
                                centerOffset.x + (decorRadius + 3.dp.toPx()) * kotlin.math.cos(rad).toFloat(),
                                centerOffset.y + (decorRadius + 3.dp.toPx()) * kotlin.math.sin(rad).toFloat()
                            )
                        )
                    }
                    14 -> { // Golden Creation Aura Spark (ID 204)
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFFFFEA00), Color(0xFFFFD700), Color.Transparent)),
                            radius = decorRadius + 6.dp.toPx(),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        // multi spokes cross
                        for (i in 0 until 4) {
                            val r = Math.toRadians((angleRotation + i * 90f).toDouble())
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = Offset(
                                    centerOffset.x + (decorRadius + 3.dp.toPx()) * kotlin.math.cos(r).toFloat(),
                                    centerOffset.y + (decorRadius + 3.dp.toPx()) * kotlin.math.sin(r).toFloat()
                                )
                            )
                        }
                    }
                    15 -> { // Toxic Glitch Cyberware Arc (ID 205)
                        drawCircle(Color(0xFF00E676), radius = decorRadius, style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 30f, 10f, 35f), angleRotation)))
                        // neon lines
                        drawCircle(Color(0xFFFF1744), radius = decorRadius + 2.dp.toPx(), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 40f), -angleRotation)))
                    }
                    16 -> { // Blood Lord Crown (ID 206)
                        val lordCrown = Path().apply {
                            moveTo(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.8f)
                            lineTo(centerOffset.x - avatarRadius * 0.6f, centerOffset.y - avatarRadius * 1.4f)
                            lineTo(centerOffset.x - avatarRadius * 0.2f, centerOffset.y - avatarRadius * 0.9f)
                            lineTo(centerOffset.x, centerOffset.y - avatarRadius * 1.5f)
                            lineTo(centerOffset.x + avatarRadius * 0.2f, centerOffset.y - avatarRadius * 0.9f)
                            lineTo(centerOffset.x + avatarRadius * 0.6f, centerOffset.y - avatarRadius * 1.4f)
                            lineTo(centerOffset.x + avatarRadius * 0.8f, centerOffset.y - avatarRadius * 0.8f)
                            close()
                        }
                        drawPath(lordCrown, color = Color(0xFFB71C1C))
                        // red glow circle
                        drawCircle(Color(0xFFFF1744).copy(alpha = breatheAlpha), radius = decorRadius, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    17 -> { // Overlord Helix Orbit (ID 207)
                        drawCircle(Color(0xFF3F51B5), radius = decorRadius, style = Stroke(width = 2.dp.toPx()))
                        // intersecting ellipses
                        drawOval(Color(0xFF00BCD4), topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius * 0.4f), size = Size(decorRadius * 2, decorRadius * 0.8f), style = Stroke(width = 1.dp.toPx()))
                    }
                    18 -> { // True Chosen Heavenly Aura Halo (ID 208)
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(Color(0xFFFF8F00), Color(0xFFFFEB3B), Color(0xFFFF8F00))),
                            radius = decorRadius,
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }
                    19 -> { // Matrix Hacker Glitched Visual Grid (ID 209)
                        drawRect(Color(0xFF00E676).copy(alpha = 0.3f), topLeft = Offset(centerOffset.x - avatarRadius, centerOffset.y - avatarRadius), size = Size(avatarRadius * 2, avatarRadius * 2), style = Stroke(width = 1.dp.toPx()))
                        drawCircle(Color(0xFF00FF33), radius = decorRadius, style = Stroke(width = 2.dp.toPx()))
                    }
                    else -> { // Angelic Holy Dark Sovereign Ring (ID 210 / backup)
                        drawCircle(Color(0xFF1A237E), radius = decorRadius, style = Stroke(width = 3.dp.toPx()))
                        drawOval(
                            color = Color(0xFFD500F9).copy(alpha = breatheAlpha),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.45f),
                            size = Size(avatarRadius * 1.8f, 10.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

// --- MAIN DIALOG DICTIONARY INTERFACE ---
@Composable
fun AvatarDecorationShopDialog(
    viewModel: SocialViewModel,
    lang: String,
    onDismiss: () -> Unit
) {
    var showPurchaseDialogForDec by remember { mutableStateOf<AvatarDecoration?>(null) }
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0 = Все (All), 1 = Купленные (Owned), 2 = Эксклюзивы (Exclusives)
    
    // Refresh states and balances
    LaunchedEffect(Unit) {
        viewModel.checkAndRefreshDecorationExpiry()
    }

    val userCoins by viewModel.userCoins.collectAsState()
    val feedViews by viewModel.feedViews.collectAsState()
    val activeDecId by viewModel.activeDecorationId.collectAsState()
    val purchasedIds by viewModel.purchasedDecorationIds.collectAsState()
    val isClaimable by viewModel.isDailyRewardClaimable.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Determine current weekly exclusive (ID 201 to 210 deterministically based on real week)
    val calendar = Calendar.getInstance()
    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    val exclusiveIndex = (year * 52 + weekOfYear) % 10
    val activeWeeklyExclusiveId = 201 + exclusiveIndex

    val allDecorations = remember(lang) { DecorationGenerator.getAll(lang) }
    val exclusiveList = remember(lang) { DecorationGenerator.getExclusiveDecorations(lang) }
    val currentWeeklyExclusive = remember(activeWeeklyExclusiveId, lang) {
        exclusiveList.find { it.id == activeWeeklyExclusiveId } ?: exclusiveList.first()
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
                    .padding(16.dp)
            ) {
                // Top Header block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == "RU") "МОДЫ АВАТАРОК ⚡" else "AVATAR UPGRADES ⚡",
                            color = AlertYellow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (lang == "RU") "Редкости, Стили, Интеграция в Ноду" else "Rarities, Custom glow, UI Integrations",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(DeepGray, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Stats Wallet banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepGray)
                        .border(1.dp, BorderGray)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == "RU") "Монеты: $userCoins" else "Coins: $userCoins",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == "RU") "Просмотры: $feedViews (10 = 1🪙)" else "Views: $feedViews (10 = 1🪙)",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daily Entry Reward Calendar Banner
                if (isClaimable) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E3A1E))
                            .border(1.dp, Color(0xFF2E7D32))
                            .clickable {
                                viewModel.vibrate(50)
                                viewModel.claimDailyReward()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == "RU") "🎁 ЕЖЕДНЕВНЫЙ БОНУС ДОСТУПЕН!" else "🎁 DAILY ENTRY REWARD READY!",
                                    color = Color(0xFF81C784),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (lang == "RU") "Нажмите чтобы забрать от 100 до 1000 монет!" else "Press to claim from 100 to 1000 coins!",
                                    color = StarkWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.vibrate(50)
                                    viewModel.claimDailyReward()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = PureWhite),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (lang == "RU") "ЗАБРАТЬ" else "CLAIM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepGray)
                            .border(1.dp, BorderGray)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "RU") "📅 Ежедневная награда получена" else "📅 Daily bonus claimed",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (lang == "RU") "Новая в 00:00" else "Next drop at 00:00",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Tabs (Все, Купленные, Эксклюзивы недели, Промокоды, Кейсы)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = if (lang == "RU") {
                        listOf("МАГАЗИН", "КУПЛЕННЫЕ", "ЭКСКЛЮЗИВ", "ПРОМОКОД", "КЕЙСЫ")
                    } else {
                        listOf("SHOP", "OWNED", "EXCLUSIVE", "PROMO", "CASES")
                    }

                    tabs.forEachIndexed { idx, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selectedCategoryTab == idx) PureWhite else DeepGray)
                                .clickable {
                                    viewModel.vibrate(15)
                                    selectedCategoryTab = idx
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selectedCategoryTab == idx) PureBlack else TextGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Dispatcher based on Tab Selected
                when (selectedCategoryTab) {
                    0 -> { // Normal Shop (200 items in columns)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allDecorations) { item ->
                                val isOwned = purchasedIds.contains(item.id)
                                val isActive = activeDecId == item.id
                                val expiryLeft = if (isOwned) viewModel.getDecorationExpiry(item.id) - System.currentTimeMillis() else 0L
                                val isValidOwned = isOwned && expiryLeft > 0L

                                DecorationShopCard(
                                    item = item,
                                    isOwned = isValidOwned,
                                    isActive = isActive,
                                    lang = lang,
                                    onClick = {
                                        if (isValidOwned) {
                                            if (isActive) {
                                                viewModel.unwearDecoration()
                                            } else {
                                                viewModel.wearDecoration(item.id)
                                            }
                                        } else {
                                            showPurchaseDialogForDec = item
                                        }
                                    }
                                )
                            }
                        }
                    }

                    1 -> { // Owned items list
                        val ownedList = remember(purchasableOwnedListTrigger(purchasedIds, allDecorations)) {
                            allDecorations.filter { purchasedIds.contains(it.id) && viewModel.isDecorationOwnedValid(it.id) } +
                            exclusiveList.filter { purchasedIds.contains(it.id) && viewModel.isDecorationOwnedValid(it.id) }
                        }

                        if (ownedList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang == "RU") "У вас пока нет активных купленных украшений." else "You don't own any active decorations yet.",
                                    color = TextGray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(ownedList) { item ->
                                    val isActive = activeDecId == item.id
                                    DecorationShopCard(
                                        item = item,
                                        isOwned = true,
                                        isActive = isActive,
                                        lang = lang,
                                        onClick = {
                                            if (isActive) {
                                                viewModel.unwearDecoration()
                                            } else {
                                                viewModel.wearDecoration(item.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    2 -> { // Weekly Exclusives (Only 1 active this week out of 10)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (lang == "RU") "🚀 ЭКСКЛЮЗИВ СЕЙЧАС В РОТАЦИИ" else "🚀 EXCLUSIVE ROTATION ACTIVE",
                                color = AlertYellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (lang == "RU") "Каждую неделю появляется 1 случайный декор из 10.\nДля покупки обязательна синяя/белая галочка!" else "One random premium item is active each calendar week.\nBlue/white verification tick is required!",
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Render large card of this week's active exclusive
                            Box(
                                modifier = Modifier
                                    .width(260.dp)
                                    .background(DeepGray)
                                    .border(2.dp, AlertYellow, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Custom visual demonstration avatar
                                    AvatarWithDecoration(
                                        avatarUrl = currentUser?.avatarUrl,
                                        decorationId = currentWeeklyExclusive.id,
                                        sizeDp = 80
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = currentWeeklyExclusive.name,
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .background(Color(0xFF8E24AA))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "ЭКСКЛЮЗИВ НЕДЕЛИ" else "WEEKLY EXCLUSIVE",
                                            color = PureWhite,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    val isOwned = purchasedIds.contains(currentWeeklyExclusive.id)
                                    val isActive = activeDecId == currentWeeklyExclusive.id
                                    val isVerified = currentUser?.isVerified == true

                                    if (isOwned && viewModel.isDecorationOwnedValid(currentWeeklyExclusive.id)) {
                                        Button(
                                            onClick = {
                                                viewModel.vibrate(25)
                                                if (isActive) viewModel.unwearDecoration()
                                                else viewModel.wearDecoration(currentWeeklyExclusive.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Color.DarkGray else PureWhite, contentColor = if (isActive) PureWhite else PureBlack),
                                            shape = RoundedCornerShape(2.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isActive) {
                                                    if (lang == "RU") "СНЯТЬ" else "REMOVE"
                                                } else {
                                                    if (lang == "RU") "НАДЕТЬ" else "WEAR"
                                                },
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = if (lang == "RU") "Цена: от ${currentWeeklyExclusive.basePrice} 🪙/день" else "Price: from ${currentWeeklyExclusive.basePrice} 🪙/day",
                                            color = AlertYellow,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        if (!isVerified) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF3E2723))
                                                    .border(1.dp, Color(0xFFD84315))
                                                    .clickable {
                                                        viewModel.vibrate(25)
                                                        viewModel.navigateTo(Screen.Profile)
                                                    }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (lang == "RU") "ТРЕБУЕТСЯ ГАЛОЧКА (ПЕРЕЙТИ)" else "CHECKMARK REQUIRED (GO)",
                                                    color = Color(0xFFFF5722),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.vibrate(25)
                                                    showPurchaseDialogForDec = currentWeeklyExclusive
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
                                                shape = RoundedCornerShape(2.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (lang == "RU") "КУПИТЬ" else "BUY NOW",
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
                    3 -> {
                        PromoCodeTab(viewModel = viewModel, lang = lang)
                    }
                    4 -> {
                        CasesTab(viewModel = viewModel, lang = lang)
                    }
                }
            }
        }
    }

    // Purchase Duration dialog popup
    if (showPurchaseDialogForDec != null) {
        val dec = showPurchaseDialogForDec!!
        
        // Calculate prices scaled for 1 day, 3 days, 1 week
        val price1Day = dec.basePrice
        val price3Days = (dec.basePrice * 2.2f).toInt()
        val price1Week = (dec.basePrice * 4.5f).toInt()

        AlertDialog(
            onDismissRequest = { showPurchaseDialogForDec = null },
            title = {
                Text(
                    text = if (lang == "RU") "Купить: ${dec.name}" else "Buy: ${dec.name}",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (lang == "RU") {
                            "Выберите срок действия украшения. После истечения срока декор автоматически пропадет, но вы можете продлить его покупку."
                        } else {
                            "Select duration. After it expires, the decoration automatically disappears, but you can renew your purchase anytime."
                        },
                        color = TextGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1 Day Option
                    DurationOptionRow(
                        durationText = if (lang == "RU") "1 День" else "1 Day",
                        priceCoins = price1Day,
                        userCoins = userCoins,
                        onClick = {
                            viewModel.vibrate(50)
                            val ok = viewModel.buyDecoration(dec.id, 1, price1Day)
                            if (ok) showPurchaseDialogForDec = null
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Days Option
                    DurationOptionRow(
                        durationText = if (lang == "RU") "3 Дня" else "3 Days",
                        priceCoins = price3Days,
                        userCoins = userCoins,
                        onClick = {
                            viewModel.vibrate(50)
                            val ok = viewModel.buyDecoration(dec.id, 3, price3Days)
                            if (ok) showPurchaseDialogForDec = null
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1 Week Option
                    DurationOptionRow(
                        durationText = if (lang == "RU") "1 Неделя" else "1 Week",
                        priceCoins = price1Week,
                        userCoins = userCoins,
                        onClick = {
                            viewModel.vibrate(50)
                            val ok = viewModel.buyDecoration(dec.id, 7, price1Week)
                            if (ok) showPurchaseDialogForDec = null
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPurchaseDialogForDec = null }) {
                    Text(
                        text = if (lang == "RU") "ОТМЕНА" else "CANCEL",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            containerColor = DeepGray,
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
fun DurationOptionRow(
    durationText: String,
    priceCoins: Int,
    userCoins: Int,
    onClick: () -> Unit
) {
    val canAfford = userCoins >= priceCoins
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (canAfford) Color(0xFF1E1E1E) else Color(0xFF2A1C1C))
            .border(1.dp, if (canAfford) BorderGray else Color(0xFFD32F2F))
            .clickable(enabled = canAfford) { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(durationText, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            if (!canAfford) {
                Text(
                    text = "Недостаточно монет" ,
                    color = Color(0xFFFF8A80),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$priceCoins 🪙",
                color = if (canAfford) AlertYellow else Color(0xFFFF8A80),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DecorationShopCard(
    item: AvatarDecoration,
    isOwned: Boolean,
    isActive: Boolean,
    lang: String,
    onClick: () -> Unit
) {
    val rarityColor = when (item.rarity) {
        "РЕДКАЯ" -> Color(0xFF2196F3)
        "АХУЕННАЯ" -> Color(0xFF9C27B0)
        "НЕВЕБЕЙШАЯ" -> Color(0xFFFFD700)
        else -> TextGray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepGray)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) AlertYellow else BorderGray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Visual demonstration avatar
            AvatarWithDecoration(
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80",
                decorationId = item.id,
                sizeDp = 48
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                color = PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.rarity,
                color = rarityColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isOwned) {
                Box(
                    modifier = Modifier
                        .background(if (isActive) AlertYellow else Color.DarkGray)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) {
                            if (lang == "RU") "ВЫБРАНО" else "WEARING"
                        } else {
                            if (lang == "RU") "НАДЕТЬ" else "WEAR"
                        },
                        color = if (isActive) PureBlack else PureWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (lang == "RU") "от ${item.basePrice} 🪙/день" else "from ${item.basePrice} 🪙/day",
                        color = AlertYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// Helper to update the owned items key on changes
private fun purchasableOwnedListTrigger(ownedIds: Set<Int>, list: List<AvatarDecoration>): Int {
    return ownedIds.hashCode() + list.hashCode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoCodeTab(viewModel: SocialViewModel, lang: String) {
    var codeText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Promo Code Title Icon",
            tint = AlertYellow,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (lang == "RU") "АКТИВАЦИЯ ПРОМОКОДОВ" else "ENTER PROMO CODE",
            color = PureWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (lang == "RU") "Введите секретный код для получения моментального бонуса!" else "Decrypt a secure token to instantly boost your nodes balance!",
            color = TextGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = codeText,
            onValueChange = { codeText = it },
            label = { Text(if (lang == "RU") "Секретный Код" else "Secure Token", color = TextGray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AlertYellow,
                unfocusedBorderColor = BorderGray,
                cursorColor = AlertYellow
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.vibrate(50)
                if (codeText.trim() == "7779208u") {
                    val currentCoins = viewModel.userCoins.value
                    viewModel.updateCoins(currentCoins + 10000)
                    isSuccess = true
                    statusMessage = if (lang == "RU") "УСПЕШНО! Начислено +10,000 монет! 🪙🚀" else "DECRYPTED! Added +10,000 coins! 🪙🚀"
                    viewModel.createSystemNotification(
                        title = if (lang == "RU") "Промокод Активирован 🔑" else "Promo Decrypted 🔑",
                        message = if (lang == "RU") "Код 7779208u принес вам 10000 монет!" else "Token 7779208u successfully synthesized 10k coins!"
                    )
                    codeText = ""
                } else {
                    isSuccess = false
                    statusMessage = if (lang == "RU") "ОШИБКА ДЕШИФРОВАНИЯ: Код недействителен." else "DECRYPTION ERROR: Code signature invalid."
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AlertYellow,
                contentColor = PureBlack
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = if (lang == "RU") "ПОДТВЕРДИТЬ" else "DECRYPT TOKEN",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSuccess) Color(0xFF14301B) else Color(0xFF331414))
                    .border(1.dp, if (isSuccess) AlertGreen else Color(0xFFFF4D4D), RoundedCornerShape(2.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = if (isSuccess) AlertGreen else Color(0xFFFF7B7B),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- CS2 CASE OPENING SYSTEM ---

data class CaseType(
    val id: Int,
    val nameRu: String,
    val nameEn: String,
    val price: Int,
    val descriptionRu: String,
    val descriptionEn: String,
    val minRarity: String,
    val premiumChance: Float,
    val colors: List<Color>
)

object CaseGenerator {
    private val adjCaseRu = listOf(
        "Древний", "Админский", "Базированный", "Шитпостинговый", "Забаненный", "Кринжовый",
        "Пиксельный", "Токсичный", "Желейный", "Полупроводниковый", "Пьяный", "Анонимный",
        "Майнерский", "Урановый", "Сумасшедший", "Коммунистический", "Капиталистический", "Силиконовый",
        "Паленый", "Оверклокнутый", "Синтетический", "Глючный", "Мемный", "Гипертрофированный",
        "Картонный", "Адский", "Золотой", "Космический", "Тайный", "Школьный", "Дворовый"
    )
    private val nounCaseRu = listOf(
        "Кулер", "Резистор", "Аватар", "Конденсатор", "Тред", "Сервер", "Кабель", "Абузер",
        "Блокчейн", "Модератор", "Чипсет", "Интеграл", "Носок", "Верификатор", "Корпус",
        "Баг", "Код", "Скриншот", "Провод", "Ноут", "Шланг", "Анлим", "Гигачад", "Шитпост",
        "Компот", "Сигма", "Думер", "Скуф", "Пельмень", "Кефир", "Сухарик"
    )
    private val suffixCaseRu = listOf(
        "в масле", "от Габена", "в депрессии", "из DNS", "под пивом", "с подсветкой", "из 2007",
        "без регистрации", "3.0", "Pro Max", "на коленке", "киберпанк", "для нищих", "для олигархов",
        "из подвала", "от Дурова", "в кредит", "с алика", "на читах", "за 16 копеек"
    )

    private val adjCaseEn = listOf(
        "Ancient", "Admin's", "Based", "Shitposting", "Banned", "Cringey",
        "Pixelated", "Toxic", "Jelly", "Semiconductor", "Drunk", "Anonymous",
        "Mining", "Uranium", "Crazy", "Communist", "Capitalist", "Silicon",
        "Counterfeit", "Overclocked", "Synthetic", "Glitchy", "Meme", "Overblown",
        "Cardboard", "Infernal", "Golden", "Cosmic", "Secret", "School", "Yard"
    )
    private val nounCaseEn = listOf(
        "Cooler", "Resistor", "Avatar", "Capacitor", "Thread", "Server", "Cable", "Abuser",
        "Blockchain", "Moderator", "Chipset", "Integral", "Sock", "Validator", "Case",
        "Bug", "Code", "Screenshot", "Wire", "Laptop", "Hose", "Unlim", "Gigachad", "Shitpost",
        "Compote", "Sigma", "Doomer", "Skuf", "Dumpling", "Kefir", "Rusk"
    )
    private val suffixCaseEn = listOf(
        "in oil", "by Gabe", "in depression", "from DNS", "under beer", "with RGB", "since 2007",
        "without signup", "3.0", "Pro Max", "on knee", "cyberpunk", "for beggars", "for oligarchs",
        "from basement", "by Durov", "on credit", "from Aliexpress", "on cheats", "for 16 cents"
    )

    private val descTemplatesRu = listOf(
        "Кейс, собранный из остатков {noun} и заправленный {suffix}. Шанс уйти в минус максимальный!",
        "Эксклюзивная подборка, содержащая {adj} {noun}. Выпавшее украшение может озолотить или обанкротить вас.",
        "Этот сундук запечатал лично {noun} в 2007 году. Ретро-вайб с диким риском потерпеть финансовое фиаско.",
        "Говорят, в этом кейсе спрятан {adj} {noun}. Окупаемость крайне мала, но азарт слишком сладок!",
        "Загадочный артефакт, содержащий {noun} {suffix}. Остерегайтесь подделок и тотального ухода в минус!"
    )
    private val descTemplatesEn = listOf(
        "A case made of {noun} leftovers and filled with {suffix}. Extreme risk of going broke!",
        "An exclusive curation containing {adj} {noun}. The dropped frame might make you rich or leave you bankrupted.",
        "This crate was sealed by {noun} back in 2007. Retro vibes with an immense risk of total financial failure.",
        "Rumor has it, a {adj} {noun} is hidden deep inside. Low profit margins, but the thrill is immaculate!",
        "A mysterious artifact holding {noun} {suffix}. Beware of bootlegs and heavy wallet minuses!"
    )

    fun generateCases(lang: String): List<CaseType> {
        val basePrices = listOf(16000, 24000, 32000, 42000, 56000, 75000, 95000, 120000, 150000, 180000)
        val minRarities = listOf("ОБЫЧНАЯ", "ОБЫЧНАЯ", "РЕДКАЯ", "РЕДКАЯ", "АХУЕННАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ", "НЕВЕБЕЙШАЯ", "НЕВЕБЕЙШАЯ", "НЕВЕБЕЙШАЯ")
        val premiumChances = listOf(0.01f, 0.03f, 0.05f, 0.08f, 0.12f, 0.18f, 0.24f, 0.32f, 0.40f, 0.50f)
        
        val gradients = listOf(
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), // dark steel
            listOf(Color(0xFF3D7EAA), Color(0xFFFFE47E)), // golden blue
            listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), // sunset red
            listOf(Color(0xFF11998e), Color(0xFF38ef7d)), // alien green
            listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)), // cosmic neon
            listOf(Color(0xFF00c6ff), Color(0xFF0072ff)), // sky high
            listOf(Color(0xFF7F00FF), Color(0xFFFF007F)), // neon purple
            listOf(Color(0xFFF9D423), Color(0xFFFF4E50)), // magma
            listOf(Color(0xFF4CA1AF), Color(0xFF2C3E50)), // ocean depth
            listOf(Color(0xFF1D976C), Color(0xFF93F9B9))  // minty fresh
        )

        val emojis = listOf("📦", "🎰", "🔥", "☢️", "💎", "🔫", "💀", "👑", "🍕", "🛸")

        return List(10) { i ->
            val rand = Random((i + 5).toLong() * 8813)
            val adjRu = adjCaseRu[rand.nextInt(adjCaseRu.size)]
            val nounRu = nounCaseRu[rand.nextInt(nounCaseRu.size)]
            val suffRu = suffixCaseRu[rand.nextInt(suffixCaseRu.size)]
            
            val adjEn = adjCaseEn[rand.nextInt(adjCaseEn.size)]
            val nounEn = nounCaseEn[rand.nextInt(nounCaseEn.size)]
            val suffEn = suffixCaseEn[rand.nextInt(suffixCaseEn.size)]

            val emoji = emojis[i % emojis.size]

            val nameRu = "Кейс «$adjRu $nounRu $suffRu» $emoji"
            val nameEn = "«$adjEn $nounEn $suffEn» Case $emoji"

            val templateRu = descTemplatesRu[rand.nextInt(descTemplatesRu.size)]
            val descRu = templateRu
                .replace("{adj}", adjRu.lowercase())
                .replace("{noun}", nounRu.lowercase())
                .replace("{suffix}", suffRu)

            val templateEn = descTemplatesEn[rand.nextInt(descTemplatesEn.size)]
            val descEn = templateEn
                .replace("{adj}", adjEn.lowercase())
                .replace("{noun}", nounEn.lowercase())
                .replace("{suffix}", suffEn)

            CaseType(
                id = i + 1,
                nameRu = nameRu,
                nameEn = nameEn,
                price = basePrices[i],
                descriptionRu = descRu,
                descriptionEn = descEn,
                minRarity = minRarities[i],
                premiumChance = premiumChances[i],
                colors = gradients[i % gradients.size]
            )
        }
    }
}

fun getRarityColor(rarity: String): Color {
    return when (rarity) {
        "ОБЫЧНАЯ" -> Color(0xFF4B69FF)
        "РЕДКАЯ" -> Color(0xFF8847FF)
        "АХУЕННАЯ" -> Color(0xFFD32CE6)
        "НЕВЕБЕЙШАЯ" -> Color(0xFFEB4B4B)
        else -> Color(0xFFE4AE39) // Exclusive
    }
}

fun getEstimatedSellValue(rarity: String, id: Int): Int {
    val seed = id.toLong() * 997
    val rand = Random(seed)
    return when (rarity) {
        "ОБЫЧНАЯ" -> 100 + rand.nextInt(101) // 100-200
        "РЕДКАЯ" -> 300 + rand.nextInt(201)  // 300-500
        "АХУЕННАЯ" -> 800 + rand.nextInt(701) // 800-1500
        "НЕВЕБЕЙШАЯ" -> 3000 + rand.nextInt(3001) // 3000-6000
        else -> 20000 + rand.nextInt(30001) // 20000-50000 for exclusive
    }
}

fun getDurationHours(rarity: String): Int {
    return when (rarity) {
        "ОБЫЧНАЯ" -> 1
        "РЕДКАЯ" -> 6
        "АХУЕННАЯ" -> 24
        "НЕВЕБЕЙШАЯ" -> 72
        else -> 168 // 7 days
    }
}

@Composable
fun CasesTab(viewModel: SocialViewModel, lang: String) {
    val userCoins by viewModel.userCoins.collectAsState()
    var selectedCaseForOpening by remember { mutableStateOf<CaseType?>(null) }
    
    // Procedural cases list
    val cases = remember(lang) { CaseGenerator.generateCases(lang) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = if (lang == "RU") "🔫 ОРУЖЕЙНЫЙ КЕЙС-СИМУЛЯТОР CS2" else "🔫 WEAPON CASE OPENER CS2",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = if (lang == "RU") "ШОК-СПИНЫ: ВРЕМЕННЫЙ ДРОП, ВЫСОКИЕ КРИТЫ И БОЛЬШОЙ ШАНС СЛИТЬ ВСЕ В МИНУС!" 
                   else "HYPE SPINS: RENTAL DROPS, RISKY LOSSES & HARDCORE RECYCLING ECONOMICS!",
            color = AlertYellow,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(cases) { caseItem ->
                CaseListItem(
                    caseItem = caseItem,
                    userCoins = userCoins,
                    lang = lang,
                    onOpenRequest = {
                        viewModel.vibrate(50)
                        selectedCaseForOpening = caseItem
                    }
                )
            }
        }
    }

    if (selectedCaseForOpening != null) {
        CaseOpenerDialog(
            caseItem = selectedCaseForOpening!!,
            viewModel = viewModel,
            userCoins = userCoins,
            lang = lang,
            onDismiss = { selectedCaseForOpening = null }
        )
    }
}

@Composable
fun CaseListItem(
    caseItem: CaseType,
    userCoins: Int,
    lang: String,
    onOpenRequest: () -> Unit
) {
    val canAfford = userCoins >= caseItem.price

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(caseItem.colors))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == "RU") caseItem.nameRu else caseItem.nameEn,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${caseItem.price} 🪙",
                        color = if (canAfford) AlertYellow else Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (lang == "RU") caseItem.descriptionRu else caseItem.descriptionEn,
                color = PureWhite.copy(alpha = 0.85f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(getRarityColor(caseItem.minRarity).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .border(1.dp, getRarityColor(caseItem.minRarity), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MIN: ${caseItem.minRarity}",
                        color = getRarityColor(caseItem.minRarity),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onOpenRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack,
                        disabledContainerColor = Color.White.copy(alpha = 0.2f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (lang == "RU") "ОСМОТРЕТЬ КЕЙС" else "INSPECT CASE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun CaseItemCard(item: AvatarDecoration, lang: String) {
    val rarityColor = getRarityColor(item.rarity)
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(115.dp)
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.5.dp, rarityColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarWithDecoration(
                    avatarUrl = null,
                    decorationId = item.id,
                    sizeDp = 30
                )
            }

            Text(
                text = item.name,
                color = PureWhite,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rarityColor)
                    .padding(vertical = 1.dp)
            ) {
                Text(
                    text = item.rarity,
                    color = PureWhite,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CaseOpenerDialog(
    caseItem: CaseType,
    viewModel: SocialViewModel,
    userCoins: Int,
    lang: String,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var wonDecoration by remember { mutableStateOf<AvatarDecoration?>(null) }
    var optionSelected by remember { mutableStateOf(false) } // true when keep/sell option selected and completed
    
    val spinItems = remember { mutableStateListOf<AvatarDecoration>() }
    val spinOffset = remember { Animatable(0f) }
    
    // Play sound / tick haptics whenever center is crossed
    LaunchedEffect(spinOffset.value) {
        val currValue = spinOffset.value
        val rounded = Math.round(currValue)
        if (isSpinning && Math.abs(currValue - rounded) < 0.08f) {
            viewModel.vibrate(12)
        }
    }

    // Helper to request a decoration
    fun getDecorationForRarity(rarity: String): AvatarDecoration {
        if (rarity == "ЭКСКЛЮЗИВНАЯ") {
            return DecorationGenerator.getExclusiveDecorations(lang).random()
        }
        for (attempt in 1..200) {
            val rid = 1000 + Random().nextInt(15000)
            val item = DecorationGenerator.generateDecoration(rid, lang)
            if (item.rarity == rarity) return item
        }
        return DecorationGenerator.generateDecoration(1001, lang)
    }

    // Run custom CS2 opening procedure
    fun triggerOpeningSpin() {
        if (userCoins < caseItem.price || isSpinning) return
        
        // Deduct money immediately
        viewModel.updateCoins(userCoins - caseItem.price)
        
        isSpinning = true
        wonDecoration = null
        optionSelected = false
        
        // Pick the winning rarity category with high risk
        val roll = Random().nextFloat()
        val winRarity = when (caseItem.id) {
            1 -> {
                when {
                    roll < caseItem.premiumChance -> "ЭКСКЛЮЗИВНАЯ"
                    roll < 0.10f -> "НЕВЕБЕЙШАЯ"
                    roll < 0.25f -> "АХУЕННАЯ"
                    roll < 0.55f -> "РЕДКАЯ"
                    else -> "ОБЫЧНАЯ" // Most common
                }
            }
            2 -> {
                when {
                    roll < caseItem.premiumChance -> "ЭКСКЛЮЗИВНАЯ"
                    roll < 0.12f -> "НЕВЕБЕЙШАЯ"
                    roll < 0.32f -> "АХУЕННАЯ"
                    roll < 0.70f -> "РЕДКАЯ"
                    else -> "ОБЫЧНАЯ"
                }
            }
            3, 4 -> {
                when {
                    roll < caseItem.premiumChance -> "ЭКСКЛЮЗИВНАЯ"
                    roll < 0.15f -> "НЕВЕБЕЙШАЯ"
                    roll < 0.45f -> "АХУЕННАЯ"
                    else -> "РЕДКАЯ"
                }
            }
            5, 6 -> {
                when {
                    roll < caseItem.premiumChance -> "ЭКСКЛЮЗИВНАЯ"
                    roll < 0.22f -> "НЕВЕБЕЙШАЯ"
                    else -> "АХУЕННАЯ"
                }
            }
            else -> {
                // High tier cases
                when {
                    roll < caseItem.premiumChance -> "ЭКСКЛЮЗИВНАЯ"
                    else -> "НЕВЕБЕЙШАЯ"
                }
            }
        }
        
        // Populate 35 carousel scroll items
        spinItems.clear()
        val possibleRarities = listOf("ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ", "ЭКСКЛЮЗИВНАЯ")
        
        for (i in 0 until 35) {
            if (i == 32) {
                spinItems.add(getDecorationForRarity(winRarity))
            } else {
                spinItems.add(getDecorationForRarity(possibleRarities.random()))
            }
        }
        
        // Animate the wheel
        coroutineScope.launch {
            spinOffset.snapTo(0f)
            viewModel.vibrate(30)
            spinOffset.animateTo(
                targetValue = 32f,
                animationSpec = tween(
                    durationMillis = 4800,
                    easing = CubicBezierEasing(0.08f, 0.93f, 0.16f, 1f)
                )
            )
            val winner = spinItems[32]
            wonDecoration = winner
            isSpinning = false
            
            // Hype drop extreme haptic trigger
            val isEpicType = winner.rarity == "НЕВЕБЕЙШАЯ" || winner.rarity == "ЭКСКЛЮЗИВНАЯ" || winner.rarity == "АХУЕННАЯ"
            if (isEpicType) {
                viewModel.vibrate(150)
            } else {
                viewModel.vibrate(85)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSpinning && (wonDecoration == null || optionSelected)) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSpinning && (wonDecoration == null || optionSelected), 
            dismissOnClickOutside = !isSpinning && (wonDecoration == null || optionSelected)
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color(0xFF2C2C2C))
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
                        text = if (lang == "RU") "🔫 КЕЙС-СИМУЛЯТОР CS2" else "🔫 CS2 CASER ENGINE",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    if (!isSpinning && (wonDecoration == null || optionSelected)) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Case Presentation Graphic
                Text(
                    text = if (lang == "RU") caseItem.nameRu else caseItem.nameEn,
                    color = AlertYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Carousel Spinner Block
                if (spinItems.isNotEmpty()) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F0F0F))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                    ) {
                        val halfWidth = maxWidth / 2
                        val halfCardWidth = 50.dp
                        
                        Row(
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .graphicsLayer {
                                    translationX = (halfWidth - halfCardWidth - (spinOffset.value * 100f).dp).toPx()
                                }
                                .align(Alignment.CenterStart)
                        ) {
                            spinItems.forEach { item ->
                                CaseItemCard(item = item, lang = lang)
                            }
                        }
                        
                        // Center Red Selector Line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .background(Color.Red)
                                .align(Alignment.Center)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "pointer",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp).align(Alignment.TopCenter)
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropUp,
                            contentDescription = "pointer",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp).align(Alignment.BottomCenter)
                        )
                    }
                } else {
                    // Closed Case graphic
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.radialGradient(listOf(Color(0xFF232526), Color(0xFF414345))))
                            .border(1.dp, Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = "Case",
                                tint = PureWhite,
                                modifier = Modifier.size(45.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (lang == "RU") "Шанс сочной окупаемости минимален!" 
                                       else "Extremely high risk of unprofitability!",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Win Reveal & Recycler Screen!
                if (wonDecoration != null && !optionSelected) {
                    val win = wonDecoration!!
                    val cardBorder = getRarityColor(win.rarity)
                    
                    val estSellValue = getEstimatedSellValue(win.rarity, win.id)
                    val rentHours = getDurationHours(win.rarity)
                    
                    val isHypeDrop = win.rarity == "НЕВЕБЕЙШАЯ" || win.rarity == "ЭКСКЛЮЗИВНАЯ" || win.rarity == "АХУЕННАЯ"
                    
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.98f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    // Display details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(if (isHypeDrop) pulseScale else 1f)
                            .background(if (isHypeDrop) cardBorder.copy(alpha = 0.15f) else cardBorder.copy(alpha = 0.05f))
                            .border(
                                width = if (isHypeDrop) 2.dp else 1.dp, 
                                color = cardBorder, 
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isHypeDrop) {
                                Text(
                                    text = if (lang == "RU") "🚨 СЕКУНДУ ШОКА! ХАЙП-ДРОП! 🚨" else "🚨 INSANE WIN! HYPE DROP ENGINE! 🚨",
                                    color = AlertYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            } else {
                                Text(
                                    text = if (lang == "RU") "ВЫБИТ КЕЙС-ДРОП" else "CASE WEAPON DROPPED",
                                    color = TextGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            
                            AvatarWithDecoration(
                                avatarUrl = null,
                                decorationId = win.id,
                                sizeDp = 64
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = win.name,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = win.rarity,
                                color = cardBorder,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Econ specs
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (lang == "RU") "Цена кейса" else "Case Cost",
                                        color = TextGray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${caseItem.price} 🪙",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (lang == "RU") "Длительность" else "Duration",
                                        color = TextGray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$rentHours ${if (lang == "RU") "ч" else "h"}",
                                            color = AlertYellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (lang == "RU") "Рыночный выкуп" else "Market Value",
                                        color = TextGray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "+$estSellValue 🪙",
                                        color = if (estSellValue >= caseItem.price) AlertYellow else Color(0xFFFF6B6B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val profitLoss = estSellValue - caseItem.price
                            Text(
                                text = if (profitLoss >= 0) {
                                    if (lang == "RU") "📈 ОКУПИЛСЯ НА +$profitLoss 🪙!" else "📈 PROFITABLE BY +$profitLoss 🪙!"
                                } else {
                                    if (lang == "RU") "📉 СЛИВ В МИНУС: $profitLoss 🪙!" else "📉 COIN DEFICIT: $profitLoss 🪙!"
                                },
                                color = if (profitLoss >= 0) AlertYellow else Color(0xFFFF4B2B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ACTIONS buttons: Instant Sell vs Take Frame Rent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // QUICKSELL FOR COINS (MASSIVE LOSS OPTION)
                        Button(
                            onClick = {
                                viewModel.vibrate(40)
                                viewModel.updateCoins(userCoins + estSellValue)
                                optionSelected = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4B1D1D),
                                contentColor = Color(0xFFFF8B8B)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFF4B2B)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(
                                text = if (lang == "RU") "ПРОДАТЬ ЗА\n$estSellValue 🪙" else "QUICKSELL FOR\n$estSellValue 🪙",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp
                            )
                        }

                        // KEEP & RENT Wear
                        Button(
                            onClick = {
                                viewModel.vibrate(80)
                                viewModel.unlockCaseDecoration(win.id, win.name, win.rarity, win.styleType, rentHours)
                                optionSelected = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(
                                text = if (lang == "RU") "ЗАБРАТЬ СЕБЕ СЕЙЧАС" else "CLAIM RENTAL ITEM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (optionSelected) {
                    // Option selected screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B1B1B))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Done",
                                tint = AlertYellow,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == "RU") "ОПЕРАЦИЯ ОБРАБОТАНА!" else "TRANSACTION SETTLED!",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == "RU") "Баланс монет обновлен" else "Coin Ledger Updated",
                                color = TextGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertYellow, contentColor = PureBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = if (lang == "RU") "ЗАКРЫТЬ" else "CLOSE ENGINE",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    // Standard spin Trigger
                    val canAfford = userCoins >= caseItem.price

                    if (!isSpinning) {
                        Button(
                            onClick = { triggerOpeningSpin() },
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertYellow,
                                contentColor = PureBlack,
                                disabledContainerColor = Color.DarkGray,
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (canAfford) {
                                    if (lang == "RU") "ОТКРЫТЬ КЕЙС (ЗА ${caseItem.price} 🪙)" else "OPEN CASE (FOR ${caseItem.price} 🪙)"
                                } else {
                                    if (lang == "RU") "НЕХВАТКА МОНЕТ (НУЖНО ${caseItem.price} 🪙)" else "NEED MORE COINS (${caseItem.price} 🪙)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = AlertYellow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (lang == "RU") "КРУТИМ РУЛЕТКУ..." else "ROULETTE SPINNING...",
                                color = PureWhite,
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
}


