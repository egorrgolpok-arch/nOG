package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    val styleType: Int, // matches rendering logic
    val patternColor: String = "Neon Blue",
    val patternStyleName: String = "Procedural Aura",
    val patternAnimation: String = "Rotating Breathe"
)

// --- DETERMINISTIC PROCEDURAL GENERATOR ---
object DecorationGenerator {
    private val adjectivesRu = listOf(
        "Анонимный", "Резиновый", "Шитпостинговый", "Забаненный", "Гипертрофированный", "Силиконовый", 
        "Полупроводниковый", "Паленый", "Оверклокнутый", "Майнерский", "Грязный", "Жидкий", 
        "Нейросетевой", "Пиксельный", "Копченый", "Дутый", "Квантовый", "Ржавый", "Кринжовый", 
        "Базированный", "Офлайновый", "Ультрафиолетовый", "Синтетический", "Глючный", "Пьяный", 
        "Токсичный", "Аналоговый", "Картонный", "Админский", "Мемный", "Шальной",
        "Галактический", "Бракованный", "Завирусившийся", "Голографический", "Платиновый", "Радиоактивный",
        "Кибернетический", "Пиратский", "Фотонный", "Эпический", "Дедовский", "Элитный", "Пацанский"
    )

    private val nounsRu = listOf(
        "Процессор", "Резистор", "Конденсатор", "Аватар", "Майнер", "Админ", "Тред", "Кабель", 
        "Кулер", "Рекорд", "Модератор", "Чипсет", "Абузер", "Носок", "Верификатор", "Корпус", 
        "Транзистор", "Баг", "Код", "Мануал", "Текстурпак", "Провод", "Ноут", "Шланг", "Скриншот", 
        "Блокчейн", "Сервер", "Анонимус", "Антивирус", "Интеграл",
        "Роутер", "Вирус", "Троян", "Алгоритм", "Фреймворк", "Компилятор", "Пинг", "Контроллер",
        "Аккаунт", "Пароль", "Логин", "Хэш", "Скрипт", "Плагин", "Интерфейс", "Нейрон"
    )

    private val suffixesRu = listOf(
        "в масле", "от Габена", "без регистрации", "3.0", "Pro Max", "из DNS", "на коленке", 
        "в депрессии", "под пивом", "киберпанк", "с подсветкой", "из 2007",
        "с Алика", "для зумеров", "на максималках", "за 300 баксов", "от Илона Маска", 
        "v2.0", "Edition", "с торрента", "без вирусов", "в 4K", "из даркнета"
    )

    private val adjectivesEn = listOf(
        "Anonymous", "Rubber", "Shitpost", "Banned", "Overloaded", "Silicon", 
        "Sourced", "Pirated", "Overclocked", "Mining", "Dirty", "Liquid", 
        "Neural", "Pixelated", "Smoked", "Bloated", "Quantum", "Rusty", "Cringey", 
        "Based", "Offline", "Ultraviolet", "Synthetic", "Glitchy", "Drunk", 
        "Toxic", "Analog", "Cardboard", "Admin", "Meme", "Wild",
        "Galactic", "Defective", "Viral", "Holographic", "Platinum", "Radioactive",
        "Cybernetic", "Bootleg", "Photonic", "Epic", "Boomer", "Elite", "Chad"
    )

    private val nounsEn = listOf(
        "CPU", "Resistor", "Capacitor", "Avatar", "Miner", "Sysadmin", "Thread", "Wire", 
        "Cooler", "Record", "Mod", "Chipset", "Abuser", "Sock", "Validator", "Case", 
        "Transistor", "Bug", "Code", "Manual", "TexturePack", "Cable", "Laptop", "Pipe", "Screenshot", 
        "Blockchain", "Host", "Anon", "Antivirus", "Gate",
        "Router", "Virus", "Trojan", "Algorithm", "Framework", "Compiler", "Ping", "Controller",
        "Account", "Password", "Login", "Hash", "Script", "Plugin", "Interface", "Neuron"
    )

    private val suffixesEn = listOf(
        "in mineral oil", "by Gabe", "unregistered", "3.0", "Pro Max", "OEM edition", "cooked on knees", 
        "depressed", "under beer", "cyberpunk", "RGB styled", "since 2007",
        "from AliExpress", "for zoomers", "on max settings", "for 300 bucks", "by Elon Musk", 
        "v2.0", "Edition", "from torrent", "virus-free", "in 4K", "from darknet"
    )

    fun generateDecoration(id: Int, lang: String): AvatarDecoration {
        val rand = Random(id.toLong() * 1453)
        
        // 1. STYLE TYPE (procedural variety)
        val styleType = (id % 50) + 1
        
        // 2. PATTERNS
        // A. Style Name / View Type
        val styleNamesRu = listOf(
            "Процедурный Нимб", "Двойной Контур", "Королевская Корона", "Цифровой Монокуляр", "Глитч-Волна",
            "ДНК-Вихрь", "Матричная Сетка", "Сингулярность", "Сверхновая Вспышка", "Орбитальный Трекер",
            "Импульсный Щит", "Пиксельный Glow", "Админский Ореол", "Солнечный Ветер", "Квантовый Узел",
            "Биометрический Визор", "Силовой Купол", "Радиоактивный След", "Фотонный Луч", "Ядро Системы"
        )
        val styleNamesEn = listOf(
            "Procedural Halo", "Double Dashed Contour", "Royal Crown", "Digital Monocular", "Glitch Wave",
            "DNA Helix", "Matrix Code Grid", "Singularity Event", "Supernova Flash", "Orbital Tracker",
            "Pulse Shield", "Pixelated LED Glow", "Sysadmin Halo", "Cosmic Solar Wind", "Quantum Spin Node",
            "Biometric Face Visor", "Protective Force Dome", "Radioactive Trail", "Photonic Beam", "Cybernetic Core"
        )
        val styleIndex = rand.nextInt(styleNamesEn.size)
        val patternStyleName = if (lang == "RU") styleNamesRu[styleIndex] else styleNamesEn[styleIndex]
        
        // B. Color Pattern
        val colorNamesRu = listOf("Красный Неон", "Электрик Синий", "Изумрудный Зеленый", "Жгучий Оранжевый", "Кислотный Желтый", "Ультра Фиолетовый", "Платиновый Белый", "Золотая Искра", "Мятная Прохлада", "Космический Багрянец")
        val colorNamesEn = listOf("Neon Red", "Electric Blue", "Emerald Green", "Fiery Orange", "Acid Yellow", "Ultra Purple", "Platinum White", "Golden Sparkle", "Mint Cooler", "Cosmic Crimson")
        val colorIndex = rand.nextInt(colorNamesEn.size)
        val patternColor = if (lang == "RU") colorNamesRu[colorIndex] else colorNamesEn[colorIndex]
        
        // C. Animation Pattern
        val animNamesRu = listOf("Быстрое Вращение", "Медленная Орбита", "Пульсация Дыхания", "Вспышки Свечения", "Реверсивный Спин", "Глитч-Мерцание")
        val animNamesEn = listOf("Fast Rotation", "Slow Orbit", "Breathing Pulsation", "Glowing Flashes", "Reverse Twisting", "Glitch Flickering")
        val animIndex = rand.nextInt(animNamesEn.size)
        val patternAnimation = if (lang == "RU") animNamesRu[animIndex] else animNamesEn[animIndex]
        
        // D. Rarity Pattern & Base Price
        val rarityRoll = rand.nextInt(100)
        val (rarityText, basePrice) = when {
            rarityRoll < 55 -> Pair(if (lang == "RU") "ОБЫЧНАЯ" else "COMMON", 15000 + (id % 12) * 800)
            rarityRoll < 80 -> Pair(if (lang == "RU") "РЕДКАЯ" else "RARE", 35000 + (id % 12) * 1500)
            rarityRoll < 92 -> Pair(if (lang == "RU") "АХУЕННАЯ" else "AWESOME", 75000 + (id % 12) * 3000)
            else -> Pair(if (lang == "RU") "НЕВЕБЕЙШАЯ" else "INSANE", 150000 + (id % 12) * 8000)
        }

        // 3. NAME GENERATION utilizing adjectives, nouns, suffixes
        val isNamedAfterContact = rand.nextInt(100) < 15
        val rawBaseName = if (isNamedAfterContact) {
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
                val prefixes = listOf("Ирокез", "Корона", "Сияние", "Маска", "Нимб", "Шлем", "Очки", "Кулер", "Голограмма", "Паттерн", "Эквалайзер")
                "${prefixes[rand.nextInt(prefixes.size)]} $contactHandle"
            } else {
                val prefixes = listOf("Mohawk of", "Crown of", "Symmetry of", "Visor of", "Halo of", "Helmet of", "Glow of", "Fan of", "Hologram of", "Pattern of", "Equalizer of")
                "${prefixes[rand.nextInt(prefixes.size)]} $contactHandle"
            }
        } else {
            val adj = if (lang == "RU") adjectivesRu[rand.nextInt(adjectivesRu.size)] else adjectivesEn[rand.nextInt(adjectivesEn.size)]
            val noun = if (lang == "RU") nounsRu[rand.nextInt(nounsRu.size)] else nounsEn[rand.nextInt(nounsEn.size)]
            val suff = if (lang == "RU") suffixesRu[rand.nextInt(suffixesRu.size)] else suffixesEn[rand.nextInt(suffixesEn.size)]
            if (rand.nextBoolean()) "$adj $noun $suff" else "$adj $noun"
        }
        
        val rawName = "$rawBaseName ($patternColor)"

        return AvatarDecoration(
            id = id,
            name = rawName,
            rarity = rarityText,
            basePrice = basePrice,
            styleType = styleType,
            patternColor = patternColor,
            patternStyleName = patternStyleName,
            patternAnimation = patternAnimation
        )
    }

    // 20 Luxury Exclusives
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
            ),
            AvatarDecoration(
                211, 
                if (lang == "RU") "Платиновый Шлем Киберсамурая ⚔️" else "Platinum Cybersamurai Helmet ⚔️", 
                "ЭКСКЛЮЗИВНАЯ", 1200000, 21
            ),
            AvatarDecoration(
                212, 
                if (lang == "RU") "Священный Нимб Архангела 👼" else "Archangel Holy Halo 👼", 
                "ЭКСКЛЮЗИВНАЯ", 1300000, 22
            ),
            AvatarDecoration(
                213, 
                if (lang == "RU") "Неоновый Щит Защитника Ноды 🛡️" else "Neon Node Defender Shield 🛡️", 
                "ЭКСКЛЮЗИВНАЯ", 1400000, 23
            ),
            AvatarDecoration(
                214, 
                if (lang == "RU") "Голографический Окуляр Оракула 👁️" else "Oracle Holographic Eye 👁️", 
                "ЭКСКЛЮЗИВНАЯ", 1500000, 24
            ),
            AvatarDecoration(
                215, 
                if (lang == "RU") "Кристаллы Энергии Пустоты 🔮" else "Void Energy Crystals 🔮", 
                "ЭКСКЛЮЗИВНАЯ", 1600000, 25
            ),
            AvatarDecoration(
                216, 
                if (lang == "RU") "Аура Божественного Дракона 🐉" else "Divine Dragon Aura 🐉", 
                "ЭКСКЛЮЗИВНАЯ", 1800000, 26
            ),
            AvatarDecoration(
                217, 
                if (lang == "RU") "Венец Вечного Императора 👑" else "Eternal Emperor Wreath 👑", 
                "ЭКСКЛЮЗИВНАЯ", 2000000, 27
            ),
            AvatarDecoration(
                218, 
                if (lang == "RU") "Гиперкубическая Квантовая Сфера 🧊" else "Hypercubic Quantum Sphere 🧊", 
                "ЭКСКЛЮЗИВНАЯ", 2200000, 28
            ),
            AvatarDecoration(
                219, 
                if (lang == "RU") "Огненная Колесница Феникса 🦅" else "Phoenix Fire Chariot 🦅", 
                "ЭКСКЛЮЗИВНАЯ", 2500000, 29
            ),
            AvatarDecoration(
                220, 
                if (lang == "RU") "Сверхновая Космическая Туманность 🌌" else "Supernova Cosmic Nebula 🌌", 
                "ЭКСКЛЮЗИВНАЯ", 3000000, 30
            ),
            AvatarDecoration(
                221, 
                if (lang == "RU") "Кибернетический Ангельский Аура-Глоб 🛸" else "Cybernetic Angelic Aura Globe 🛸", 
                "ЭКСКЛЮЗИВНАЯ", 3500000, 31
            ),
            AvatarDecoration(
                222, 
                if (lang == "RU") "Шлем Темного Жнеца nOG AI 💀" else "Dark Reaper Helmet nOG AI 💀", 
                "ЭКСКЛЮЗИВНАЯ", 4000000, 32
            ),
            AvatarDecoration(
                223, 
                if (lang == "RU") "Корона Звездного Лорда Метавселенной 👑" else "Crown of Star Lord Metaverse 👑", 
                "ЭКСКЛЮЗИВНАЯ", 4500000, 33
            ),
            AvatarDecoration(
                224, 
                if (lang == "RU") "Огненный Кристалл Магматического Бога 🌋" else "Magma God Fire Crystal 🌋", 
                "ЭКСКЛЮЗИВНАЯ", 5000000, 34
            ),
            AvatarDecoration(
                225, 
                if (lang == "RU") "Неоновое Кольцо Черной Дыры 🌀" else "Neon Black Hole Ring 🌀", 
                "ЭКСКЛЮЗИВНАЯ", 5500000, 35
            ),
            AvatarDecoration(
                226, 
                if (lang == "RU") "Аура Межзвездного Странника 🪐" else "Interstellar Wanderer Aura 🪐", 
                "ЭКСКЛЮЗИВНАЯ", 6000000, 36
            ),
            AvatarDecoration(
                227, 
                if (lang == "RU") "Священный Шлем Валькирии Будущего 🪽" else "Sacred Valkyrie Future Helmet 🪽", 
                "ЭКСКЛЮЗИВНАЯ", 7000000, 37
            ),
            AvatarDecoration(
                228, 
                if (lang == "RU") "Голограмма Искусственного Интеллекта nOG 🧠" else "Holographic AI Mind nOG 🧠", 
                "ЭКСКЛЮЗИВНАЯ", 8000000, 38
            ),
            AvatarDecoration(
                229, 
                if (lang == "RU") "Цифровой Гало-Шитпостер 🎰" else "Digital Halo Shitposter 🎰", 
                "ЭКСКЛЮЗИВНАЯ", 9000000, 39
            ),
            AvatarDecoration(
                230, 
                if (lang == "RU") "Королевское Солнечное Затмение ☀️" else "Royal Solar Eclipse Ring ☀️", 
                "ЭКСКЛЮЗИВНАЯ", 10000000, 40
            )
        )
    }

    // Get 300 normal + 30 exclusives
    fun getAll(lang: String): List<AvatarDecoration> {
        val list = mutableListOf<AvatarDecoration>()
        for (i in 1..300) {
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
    borderWidthDp: Int = 1,
    isLowEnd: Boolean = false
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
            val isSmall = sizeDp <= 56
            
            // Retrieve AI decoration properties if ID is 9999
            val context = LocalContext.current
            val aiDecProps = remember(decorationId) {
                if (decorationId == 9999) {
                    val prefs = context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE)
                    val name = prefs.getString("ai_dec_name", "nOG AI Artifact") ?: "nOG AI Artifact"
                    val rarity = prefs.getString("ai_dec_rarity", "НЕВЕБЕЙШАЯ") ?: "НЕВЕБЕЙШАЯ"
                    val styleType = prefs.getInt("ai_dec_style_type", 1)
                    val colorOffset = prefs.getInt("ai_dec_color_offset", 0)
                    Triple(name, rarity, Pair(styleType, colorOffset))
                } else {
                    Triple("", "", Pair(0, 0))
                }
            }
            
            val angleRotation: Float
            val scalePulse: Float
            val breatheAlpha: Float
            
            if (isLowEnd) {
                angleRotation = 0f
                scalePulse = 1f
                breatheAlpha = 0.9f
            } else {
                val animType = decorationId?.rem(4) ?: 0
                val infiniteTransition = rememberInfiniteTransition(label = "avatar_dec")
                
                val rotDuration = when(animType) {
                    0 -> if (isSmall) 6000 else 4000
                    1 -> if (isSmall) 3000 else 2000 // Fast
                    2 -> if (isSmall) 8000 else 6000 // Slow
                    else -> if (isSmall) 4000 else 2500
                }
                
                val rotEasing = when(animType) {
                    0 -> LinearEasing
                    1 -> LinearOutSlowInEasing
                    2 -> FastOutSlowInEasing
                    else -> LinearEasing
                }

                val rotationState = infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = if (animType == 3) -360f else 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(rotDuration, easing = rotEasing)
                    ),
                    label = "rot"
                )
                angleRotation = rotationState.value
 
                val scaleState = if (isSmall) {
                    null
                } else {
                    val scaleMax = if (animType == 1) 1.08f else 1.04f
                    infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = scaleMax,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200 + (animType * 200), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                }
                scalePulse = scaleState?.value ?: 1f
 
                val alphaState = if (isSmall) {
                    null
                } else {
                    val alphaMin = if (animType == 2) 0.3f else 0.6f
                    infiniteTransition.animateFloat(
                        initialValue = alphaMin,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000 + (animType * 300), easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                }
                breatheAlpha = alphaState?.value ?: 0.9f
            }
 
            Canvas(
                modifier = Modifier
                    .size((sizeDp + 16).dp)
                    .graphicsLayer {
                        rotationZ = angleRotation
                        scaleX = scalePulse
                        scaleY = scalePulse
                        alpha = breatheAlpha
                    }
            ) {
                val angleRotation = 0f
                val scalePulse = 1f
                val breatheAlpha = 1f
 
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val avatarRadius = (sizeDp.dp.toPx()) / 2f
                val decorRadius = avatarRadius + 4.dp.toPx()
 
                // Procedurally resolve styleType & colors using deterministic math on decorationId
                val decorationItem = if (decorationId == 9999) {
                    AvatarDecoration(9999, aiDecProps.first, aiDecProps.second, 0, aiDecProps.third.first)
                } else if (decorationId >= 201) {
                    DecorationGenerator.getExclusiveDecorations("EN").find { it.id == decorationId }
                } else {
                    DecorationGenerator.generateDecoration(decorationId, "EN")
                }
                
                val styleType = if (decorationId == 9999) aiDecProps.third.first else (decorationItem?.styleType ?: ((decorationId % 30) + 1))
                
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
 
                val colorOffset = if (decorationId == 9999) aiDecProps.third.second else (decorationId % 10)
                val primaryColor = premiumProceduralColors[colorOffset % premiumProceduralColors.size]
                val secondaryColor = premiumProceduralColors[(colorOffset + 3) % premiumProceduralColors.size]
                val tertiaryColor = premiumProceduralColors[(colorOffset + 7) % premiumProceduralColors.size]

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
                    20 -> { // Angelic Holy Dark Sovereign Ring (ID 210)
                        drawCircle(Color(0xFF1A237E), radius = decorRadius, style = Stroke(width = 3.dp.toPx()))
                        drawOval(
                            color = Color(0xFFD500F9).copy(alpha = breatheAlpha),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.9f, centerOffset.y - avatarRadius * 1.45f),
                            size = Size(avatarRadius * 1.8f, 10.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    21 -> { // Platinum Cybersamurai Helmet
                        val helmetPath = Path().apply {
                            moveTo(centerOffset.x - avatarRadius * 0.5f, centerOffset.y - avatarRadius * 0.8f)
                            lineTo(centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.5f) // left horn
                            lineTo(centerOffset.x - avatarRadius * 0.2f, centerOffset.y - avatarRadius * 1.1f)
                            lineTo(centerOffset.x, centerOffset.y - avatarRadius * 1.6f) // center crest
                            lineTo(centerOffset.x + avatarRadius * 0.2f, centerOffset.y - avatarRadius * 1.1f)
                            lineTo(centerOffset.x + avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.5f) // right horn
                            lineTo(centerOffset.x + avatarRadius * 0.5f, centerOffset.y - avatarRadius * 0.8f)
                            close()
                        }
                        drawPath(helmetPath, color = Color(0xFFE2E2E2)) // Platinum silver
                        drawCircle(Color(0xFFFF1744), radius = decorRadius, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    22 -> { // Archangel Holy Halo
                        drawOval(
                            brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFD700), Color.Transparent)),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.8f, centerOffset.y - avatarRadius * 1.5f),
                            size = Size(avatarRadius * 1.6f, 15.dp.toPx())
                        )
                        drawOval(
                            color = Color(0xFFFFEA00),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.7f, centerOffset.y - avatarRadius * 1.45f),
                            size = Size(avatarRadius * 1.4f, 10.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    23 -> { // Neon Node Defender Shield
                        val shieldPath = Path().apply {
                            for (i in 0..5) {
                                val angle = Math.toRadians((60 * i + angleRotation / 3).toDouble())
                                val px = centerOffset.x + (decorRadius + 4.dp.toPx()) * kotlin.math.cos(angle).toFloat()
                                val py = centerOffset.y + (decorRadius + 4.dp.toPx()) * kotlin.math.sin(angle).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(shieldPath, color = Color(0xFF00FFFF).copy(alpha = 0.4f), style = Stroke(width = 2.dp.toPx()))
                    }
                    24 -> { // Oracle Holographic Eye
                        drawCircle(Color(0xFF38EF7D).copy(alpha = breatheAlpha), radius = decorRadius, style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(Color(0xFF38EF7D), radius = decorRadius + 4.dp.toPx(), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), angleRotation)))
                        drawLine(Color(0xFF38EF7D), start = Offset(centerOffset.x - decorRadius - 6.dp.toPx(), centerOffset.y), end = Offset(centerOffset.x - decorRadius, centerOffset.y), strokeWidth = 2f)
                        drawLine(Color(0xFF38EF7D), start = Offset(centerOffset.x + decorRadius, centerOffset.y), end = Offset(centerOffset.x + decorRadius + 6.dp.toPx(), centerOffset.y), strokeWidth = 2f)
                    }
                    25 -> { // Void Energy Crystals
                        for (i in 0..3) {
                            val angle = Math.toRadians((90 * i + angleRotation * 1.5).toDouble())
                            val cx = centerOffset.x + (decorRadius + 5.dp.toPx()) * kotlin.math.cos(angle).toFloat()
                            val cy = centerOffset.y + (decorRadius + 5.dp.toPx()) * kotlin.math.sin(angle).toFloat()
                            val crystalPath = Path().apply {
                                moveTo(cx, cy - 6.dp.toPx())
                                lineTo(cx + 4.dp.toPx(), cy)
                                lineTo(cx, cy + 6.dp.toPx())
                                lineTo(cx - 4.dp.toPx(), cy)
                                close()
                            }
                            drawPath(crystalPath, color = Color(0xFFD500F9))
                        }
                    }
                    26 -> { // Divine Dragon Aura
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(Color(0xFFE040FB), Color(0xFF00E5FF), Color(0xFFE040FB))),
                            radius = decorRadius,
                            style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(60f, 40f), -angleRotation))
                        )
                    }
                    27 -> { // Eternal Emperor Wreath
                        for (i in -4..4) {
                            if (i == 0) continue
                            val angleLeft = 180 + i * 25
                            val angleRight = 0 - i * 25
                            
                            val radLeft = Math.toRadians(angleLeft.toDouble())
                            val lx = centerOffset.x + decorRadius * kotlin.math.cos(radLeft).toFloat()
                            val ly = centerOffset.y + decorRadius * kotlin.math.sin(radLeft).toFloat()
                            drawCircle(Color(0xFFFFD700), radius = 3.dp.toPx(), center = Offset(lx, ly))
                            
                            val radRight = Math.toRadians(angleRight.toDouble())
                            val rx = centerOffset.x + decorRadius * kotlin.math.cos(radRight).toFloat()
                            val ry = centerOffset.y + decorRadius * kotlin.math.sin(radRight).toFloat()
                            drawCircle(Color(0xFFFFD700), radius = 3.dp.toPx(), center = Offset(rx, ry))
                        }
                        drawCircle(Color(0xFFFFD700).copy(alpha = 0.6f), radius = decorRadius - 1.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                    }
                    28 -> { // Hypercubic Quantum Sphere
                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset(centerOffset.x - avatarRadius * 0.75f, centerOffset.y - avatarRadius * 0.75f),
                            size = Size(avatarRadius * 1.5f, avatarRadius * 1.5f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(Color(0xFFFF1493).copy(alpha = breatheAlpha), radius = decorRadius, style = Stroke(width = 1.dp.toPx()))
                    }
                    29 -> { // Phoenix Fire Chariot
                        val leftWing = Path().apply {
                            moveTo(centerOffset.x - avatarRadius * 0.9f, centerOffset.y)
                            cubicTo(
                                centerOffset.x - avatarRadius * 1.5f, centerOffset.y - avatarRadius * 0.8f,
                                centerOffset.x - avatarRadius * 2.1f, centerOffset.y + avatarRadius * 0.3f,
                                centerOffset.x - avatarRadius * 0.9f, centerOffset.y + avatarRadius * 0.6f
                            )
                        }
                        val rightWing = Path().apply {
                            moveTo(centerOffset.x + avatarRadius * 0.9f, centerOffset.y)
                            cubicTo(
                                centerOffset.x + avatarRadius * 1.5f, centerOffset.y - avatarRadius * 0.8f,
                                centerOffset.x + avatarRadius * 2.1f, centerOffset.y + avatarRadius * 0.3f,
                                centerOffset.x + avatarRadius * 0.9f, centerOffset.y + avatarRadius * 0.6f
                            )
                        }
                        drawPath(leftWing, brush = Brush.verticalGradient(listOf(Color(0xFFFF4500), Color(0xFFFFEA00))))
                        drawPath(rightWing, brush = Brush.verticalGradient(listOf(Color(0xFFFF4500), Color(0xFFFFEA00))))
                    }
                    30 -> { // Supernova Cosmic Nebula
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(Color(0xFFFF007F), Color(0xFF7F00FF), Color(0xFF00FFFF), Color(0xFFFF007F))),
                            radius = decorRadius,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    31 -> { // Cybernetic Angelic Aura
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), angleRotation)
                        drawCircle(color = primaryColor, radius = decorRadius, style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect))
                        drawCircle(color = secondaryColor.copy(alpha = 0.5f), radius = decorRadius + 5.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                    }
                    32 -> { // Dark Reaper Aura
                        drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = decorRadius)
                        drawCircle(color = primaryColor.copy(alpha = breatheAlpha), radius = decorRadius, style = Stroke(width = 3.dp.toPx()))
                        val spikes = 8
                        for (i in 0 until spikes) {
                            val rad = Math.toRadians((i * (360.0 / spikes)) + angleRotation)
                            val x1 = centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat()
                            val y1 = centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat()
                            val x2 = centerOffset.x + (decorRadius + 10.dp.toPx()) * kotlin.math.cos(rad).toFloat()
                            val y2 = centerOffset.y + (decorRadius + 10.dp.toPx()) * kotlin.math.sin(rad).toFloat()
                            drawLine(color = primaryColor, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 1.dp.toPx())
                        }
                    }
                    33 -> { // Crown of Star Lord
                        drawArc(color = primaryColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius - 10.dp.toPx()), size = Size(decorRadius * 2, decorRadius * 2), style = Stroke(width = 3.dp.toPx()))
                        drawCircle(color = secondaryColor, radius = 4.dp.toPx(), center = Offset(centerOffset.x, centerOffset.y - decorRadius - 10.dp.toPx()))
                    }
                    34 -> { // Magma God Fire Crystal
                        val rectSize = decorRadius * 1.5f
                        drawRect(color = Color(0xFFFF4500).copy(alpha = 0.6f * breatheAlpha), topLeft = Offset(centerOffset.x - rectSize/2, centerOffset.y - rectSize/2), size = Size(rectSize, rectSize), style = Stroke(width = 2.dp.toPx()))
                        drawRect(color = Color(0xFFFFEA00).copy(alpha = 0.4f * breatheAlpha), topLeft = Offset(centerOffset.x - rectSize/2.5f, centerOffset.y - rectSize/2.5f), size = Size(rectSize * 0.8f, rectSize * 0.8f), style = Stroke(width = 2.dp.toPx()))
                    }
                    35 -> { // Neon Black Hole
                        drawCircle(color = Color.Black.copy(alpha = 0.8f), radius = decorRadius - 5.dp.toPx())
                        drawCircle(brush = Brush.sweepGradient(listOf(primaryColor, Color.Transparent, secondaryColor, Color.Transparent)), radius = decorRadius, style = Stroke(width = 4.dp.toPx()))
                    }
                    36 -> { // Interstellar Wanderer
                        drawCircle(color = primaryColor.copy(alpha = 0.4f), radius = decorRadius + (5f * breatheAlpha).dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                        drawCircle(color = secondaryColor.copy(alpha = 0.6f), radius = decorRadius, style = Stroke(width = 2.dp.toPx()))
                        drawCircle(color = tertiaryColor.copy(alpha = 0.8f), radius = decorRadius - 5.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                    }
                    37 -> { // Sacred Valkyrie Helmet
                        val path = Path().apply {
                            moveTo(centerOffset.x - decorRadius, centerOffset.y)
                            lineTo(centerOffset.x, centerOffset.y - decorRadius - 15.dp.toPx())
                            lineTo(centerOffset.x + decorRadius, centerOffset.y)
                        }
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
                        drawCircle(color = secondaryColor, radius = 3.dp.toPx(), center = Offset(centerOffset.x, centerOffset.y - decorRadius - 15.dp.toPx()))
                    }
                    38 -> { // Holographic AI Mind
                        val gridLines = 5
                        for (i in 0..gridLines) {
                            val offset = (decorRadius * 2) * (i.toFloat() / gridLines) - decorRadius
                            drawLine(color = primaryColor.copy(alpha = 0.5f), start = Offset(centerOffset.x - decorRadius, centerOffset.y + offset), end = Offset(centerOffset.x + decorRadius, centerOffset.y + offset), strokeWidth = 1.dp.toPx())
                            drawLine(color = secondaryColor.copy(alpha = 0.5f), start = Offset(centerOffset.x + offset, centerOffset.y - decorRadius), end = Offset(centerOffset.x + offset, centerOffset.y + decorRadius), strokeWidth = 1.dp.toPx())
                        }
                    }
                    39 -> { // Digital Halo
                        drawCircle(color = primaryColor, radius = decorRadius, style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 15f), angleRotation)))
                        drawCircle(color = secondaryColor, radius = decorRadius + 4.dp.toPx(), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), -angleRotation)))
                    }
                    40 -> { // Eclipse Ring
                        drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = decorRadius)
                        drawArc(color = primaryColor, startAngle = angleRotation, sweepAngle = 270f, useCenter = false, topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius), size = Size(decorRadius*2, decorRadius*2), style = Stroke(width = 4.dp.toPx()))
                    }
                    41 -> { // Hexagonal Tech
                        val path = Path()
                        for (i in 0 until 6) {
                            val rad = Math.toRadians((i * 60.0) + angleRotation)
                            val x = centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat()
                            val y = centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
                    }
                    42 -> { // Dual Orbiting Moons
                        drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = decorRadius, style = Stroke(width = 1.dp.toPx()))
                        val rad1 = Math.toRadians(angleRotation.toDouble())
                        drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(centerOffset.x + decorRadius * kotlin.math.cos(rad1).toFloat(), centerOffset.y + decorRadius * kotlin.math.sin(rad1).toFloat()))
                        val rad2 = Math.toRadians(angleRotation.toDouble() + 180.0)
                        drawCircle(color = tertiaryColor, radius = 3.dp.toPx(), center = Offset(centerOffset.x + decorRadius * kotlin.math.cos(rad2).toFloat(), centerOffset.y + decorRadius * kotlin.math.sin(rad2).toFloat()))
                    }
                    43 -> { // Pulsing Energy Shield
                        for (i in 1..3) {
                            drawCircle(color = primaryColor.copy(alpha = 0.2f * i * breatheAlpha), radius = decorRadius - (i * 2).dp.toPx(), style = Stroke(width = 1.5.dp.toPx()))
                        }
                    }
                    44 -> { // Glitching Triangle
                        val path = Path()
                        val glitchOffset = (Math.random() * 10 - 5).toFloat()
                        for (i in 0 until 3) {
                            val rad = Math.toRadians((i * 120.0) + angleRotation)
                            val x = centerOffset.x + decorRadius * kotlin.math.cos(rad).toFloat() + glitchOffset
                            val y = centerOffset.y + decorRadius * kotlin.math.sin(rad).toFloat() - glitchOffset
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
                    }
                    45 -> { // Rotating Crosshairs
                        drawLine(color = primaryColor, start = Offset(centerOffset.x - decorRadius, centerOffset.y), end = Offset(centerOffset.x - decorRadius / 2, centerOffset.y), strokeWidth = 2.dp.toPx())
                        drawLine(color = primaryColor, start = Offset(centerOffset.x + decorRadius / 2, centerOffset.y), end = Offset(centerOffset.x + decorRadius, centerOffset.y), strokeWidth = 2.dp.toPx())
                        drawLine(color = primaryColor, start = Offset(centerOffset.x, centerOffset.y - decorRadius), end = Offset(centerOffset.x, centerOffset.y - decorRadius / 2), strokeWidth = 2.dp.toPx())
                        drawLine(color = primaryColor, start = Offset(centerOffset.x, centerOffset.y + decorRadius / 2), end = Offset(centerOffset.x, centerOffset.y + decorRadius), strokeWidth = 2.dp.toPx())
                        drawCircle(color = secondaryColor.copy(alpha = breatheAlpha), radius = decorRadius / 2, style = Stroke(width = 1.dp.toPx()))
                    }
                    46 -> { // Wavy Liquid Aura
                        val path = Path()
                        for (i in 0..360 step 10) {
                            val rad = Math.toRadians(i.toDouble())
                            val wave = (kotlin.math.sin(rad * 4 + Math.toRadians(angleRotation.toDouble())) * 5.dp.toPx()).toFloat()
                            val x = centerOffset.x + (decorRadius + wave) * kotlin.math.cos(rad).toFloat()
                            val y = centerOffset.y + (decorRadius + wave) * kotlin.math.sin(rad).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = primaryColor.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))
                    }
                    47 -> { // Tech Square Frame
                        drawRect(color = primaryColor, topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius), size = Size(decorRadius * 2, decorRadius * 2), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), angleRotation)))
                        drawRect(color = secondaryColor.copy(alpha = 0.5f), topLeft = Offset(centerOffset.x - decorRadius + 4.dp.toPx(), centerOffset.y - decorRadius + 4.dp.toPx()), size = Size((decorRadius - 4.dp.toPx()) * 2, (decorRadius - 4.dp.toPx()) * 2), style = Stroke(width = 1.dp.toPx()))
                    }
                    48 -> { // Sparkle Star
                        val path = Path()
                        for (i in 0 until 8) {
                            val rad = Math.toRadians((i * 45.0) + angleRotation)
                            val r = if (i % 2 == 0) decorRadius else decorRadius / 2
                            val x = centerOffset.x + r * kotlin.math.cos(rad).toFloat()
                            val y = centerOffset.y + r * kotlin.math.sin(rad).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
                    }
                    49 -> { // Yin Yang Spin
                        drawArc(color = primaryColor, startAngle = angleRotation, sweepAngle = 180f, useCenter = true, topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius), size = Size(decorRadius*2, decorRadius*2))
                        drawArc(color = secondaryColor, startAngle = angleRotation + 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(centerOffset.x - decorRadius, centerOffset.y - decorRadius), size = Size(decorRadius*2, decorRadius*2))
                    }
                    50 -> { // Glowing DNA Helix
                        for (i in 0 until 10) {
                            val yOffset = (i - 5) * (decorRadius / 5)
                            val xOffset = (kotlin.math.sin(i * 0.5 + Math.toRadians(angleRotation.toDouble())) * (decorRadius / 2)).toFloat()
                            drawCircle(color = primaryColor, radius = 2.dp.toPx(), center = Offset(centerOffset.x + xOffset, centerOffset.y + yOffset))
                            drawCircle(color = secondaryColor, radius = 2.dp.toPx(), center = Offset(centerOffset.x - xOffset, centerOffset.y + yOffset))
                            drawLine(color = tertiaryColor.copy(alpha = 0.3f), start = Offset(centerOffset.x + xOffset, centerOffset.y + yOffset), end = Offset(centerOffset.x - xOffset, centerOffset.y + yOffset), strokeWidth = 1.dp.toPx())
                        }
                    }
                    else -> { // Default glowing aura ring
                        drawCircle(
                            color = primaryColor.copy(alpha = breatheAlpha),
                            radius = decorRadius,
                            style = Stroke(width = 2.dp.toPx())
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
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE) }
    var showPurchaseDialogForDec by remember { mutableStateOf<AvatarDecoration?>(null) }
    var showAiPurchaseDialog by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0 = Все (All), 1 = Купленные (Owned), 2 = Эксклюзивы (Exclusives)
    
    var aiGenerationState by remember { mutableStateOf(0) } // 0 = Idle, 1 = Generating, 2 = Success
    var aiGenName by remember { mutableStateOf("") }
    var aiGenRarity by remember { mutableStateOf("") }
    var aiGenStyleType by remember { mutableStateOf(1) }
    var aiGenColorOffset by remember { mutableStateOf(0) }
    var aiGenPrice by remember { mutableStateOf(0) }
    var aiErrorMsg by remember { mutableStateOf<String?>(null) }
    
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

    // Determine current weekly exclusive (ID 201 to 230 deterministically based on real week)
    val calendar = Calendar.getInstance()
    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    val exclusiveIndex = (year * 52 + weekOfYear) % 30
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

                // --- nOG AI DECORATION GENERATOR ---
                Spacer(modifier = Modifier.height(12.dp))
                
                val coroutineScope = rememberCoroutineScope()
                val isVerified = currentUser?.isVerified == true
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF12121A))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFFF007F), Color(0xFF7F00FF), Color(0xFF00FFFF))
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == "RU") "🧬 nOG AI СИНТЕЗАТОР" else "🧬 nOG AI SYNTHESIZER",
                                    color = Color(0xFF00FFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (lang == "RU") "Генерация случайного уникального украшения" else "Synthesize a completely random unique decoration",
                                    color = StarkWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            if (aiGenerationState == 0) {
                                Button(
                                    onClick = {
                                        val lastGenerated = prefs.getLong("ai_dec_last_generated_time", 0L)
                                        val timePassed = System.currentTimeMillis() - lastGenerated
                                        val cooldownMs = 5 * 3600 * 1000L // 5 hours
                                        
                                        if (!isVerified && timePassed < cooldownMs) {
                                            val remainingMs = cooldownMs - timePassed
                                            val hrs = remainingMs / (3600 * 1000)
                                            val mins = (remainingMs % (3600 * 1000)) / (60 * 1000)
                                            aiErrorMsg = if (lang == "RU") {
                                                "Ожидайте еще $hrs ч. $mins мин. (с галочкой лимита нет! ✅)"
                                            } else {
                                                "Cooldown: $hrs hrs $mins mins (no limits with checkmark! ✅)"
                                            }
                                        } else {
                                            viewModel.vibrate(100)
                                            aiErrorMsg = null
                                            aiGenerationState = 1 // Start synthesis animation
                                            
                                            coroutineScope.launch {
                                                delay(3000) // beautiful 3s animation delay
                                                
                                                val rand = java.util.Random()
                                                val rarities = listOf("ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "ЭКСКЛЮЗИВНАЯ", "НЕВЕБЕЙШАЯ")
                                                val raritiesEn = listOf("COMMON", "RARE", "AWESOME", "EXCLUSIVE", "UNBELIEVABLE")
                                                val rarIndex = rand.nextInt(rarities.size)
                                                aiGenRarity = if (lang == "RU") rarities[rarIndex] else raritiesEn[rarIndex]
                                                
                                                aiGenStyleType = rand.nextInt(30) + 1
                                                aiGenColorOffset = rand.nextInt(100)
                                                aiGenPrice = rand.nextInt(280000) + 20000
                                                
                                                val prefixesRu = listOf("Квантовый ", "Кибернетический ", "Хроматический ", "Сингулярный ", "Трансцендентный ", "Сверхпроводимый ", "Эфирный ", "Гиперборейский ", "Фотонный ", "Плазменный ")
                                                val prefixesEn = listOf("Quantum ", "Cybernetic ", "Chromatic ", "Singular ", "Transcendent ", "Superconducting ", "Aetherial ", "Hyperborean ", "Photonic ", "Plasma ")
                                                val coresRu = listOf("Нексус ", "Кристалл ", "Вихрь ", "Глитч ", "Фрагмент ", "Резонатор ", "Спектр ", "Венец ", "Синтезатор ", "Модуль ")
                                                val coresEn = listOf("Nexus ", "Crystal ", "Vortex ", "Glitch ", "Fragment ", "Resonator ", "Spectrum ", "Crest ", "Synthesizer ", "Module ")
                                                val suffix = listOf(" nOG AI", " v2.0 AI", " (Beta-S)").random()
                                                
                                                val pIdx = rand.nextInt(prefixesRu.size)
                                                val cIdx = rand.nextInt(coresRu.size)
                                                aiGenName = if (lang == "RU") {
                                                    "${prefixesRu[pIdx]}${coresRu[cIdx]}$suffix"
                                                } else {
                                                    "${prefixesEn[pIdx]}${coresEn[cIdx]}$suffix"
                                                }
                                                
                                                prefs.edit().putLong("ai_dec_last_generated_time", System.currentTimeMillis()).apply()
                                                
                                                prefs.edit()
                                                    .putString("ai_dec_name", aiGenName)
                                                    .putString("ai_dec_rarity", aiGenRarity)
                                                    .putInt("ai_dec_style_type", aiGenStyleType)
                                                    .putInt("ai_dec_color_offset", aiGenColorOffset)
                                                    .apply()
                                                    
                                                aiGenerationState = 2 // Done generating!
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F00FF), contentColor = PureWhite),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "СИНТЕЗ" else "SYNTHESIZE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        if (aiErrorMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiErrorMsg!!,
                                color = Color(0xFFFF1744),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // ANIMATION STATE: nOG AI IS GENERATING
                        if (aiGenerationState == 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Pulsing beautiful circular loading indicator
                                val infiniteTransition = rememberInfiniteTransition(label = "ai_load")
                                val rotation = infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
                                    label = "rot"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(2.dp, Color(0xFFFF007F), CircleShape)
                                        .graphicsLayer { rotationZ = rotation.value },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF00FFFF), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (lang == "RU") "nOG AI ГЕНЕРИРУЕТ..." else "nOG AI GENERATING...",
                                    color = Color(0xFFFF007F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // SUCCESS STATE: SHOW RESULTING DECORATION + CLAIM BUTTON
                        if (aiGenerationState == 2) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepGray)
                                    .border(1.dp, BorderGray)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Draw the dynamic generated decoration
                                AvatarWithDecoration(
                                    avatarUrl = currentUser?.avatarUrl,
                                    decorationId = 9999,
                                    sizeDp = 48
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = aiGenName,
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = aiGenRarity,
                                            color = when (aiGenRarity) {
                                                "ОБЫЧНАЯ", "COMMON" -> TextGray
                                                "РЕДКАЯ", "RARE" -> Color(0xFF29B6F6)
                                                "АХУЕННАЯ", "AWESOME" -> Color(0xFF81C784)
                                                "ЭКСКЛЮЗИВНАЯ", "EXCLUSIVE" -> Color(0xFFFFB74D)
                                                else -> Color(0xFFE040FB)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "💰 $aiGenPrice",
                                            color = Color(0xFFFFD700),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.vibrate(50)
                                            viewModel.wearTemporaryAIDecoration(
                                                aiGenName,
                                                aiGenRarity,
                                                aiGenStyleType,
                                                aiGenColorOffset
                                            )
                                            aiGenerationState = 0 // Return to idle!
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = PureWhite),
                                        shape = RoundedCornerShape(2.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "БЕСПЛАТНО" else "FREE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            viewModel.vibrate(50)
                                            showAiPurchaseDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F), contentColor = PureWhite),
                                        shape = RoundedCornerShape(2.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "КУПИТЬ..." else "BUY...",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (lang == "RU") "5 мин бесплатно или на срок!" else "5 mins free or purchase!",
                                        color = TextGray,
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
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
                            purchasedIds.map { id ->
                                if (id == 9999) {
                                    val customName = prefs.getString("ai_dec_name", "AI Artifact") ?: "AI Artifact"
                                    val customRarity = prefs.getString("ai_dec_rarity", "НЕВЕБЕЙШАЯ") ?: "НЕВЕБЕЙШАЯ"
                                    val styleType = prefs.getInt("ai_dec_style_type", 1)
                                    AvatarDecoration(
                                        id = 9999,
                                        name = customName,
                                        rarity = customRarity,
                                        basePrice = 0,
                                        styleType = styleType,
                                        patternColor = "AI",
                                        patternStyleName = "AI Generated",
                                        patternAnimation = "Active"
                                    )
                                } else if (id in 201..230) {
                                    exclusiveList.find { it.id == id } ?: DecorationGenerator.generateDecoration(id, lang)
                                } else {
                                    allDecorations.find { it.id == id } ?: DecorationGenerator.generateDecoration(id, lang)
                                }
                            }
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
                                    val expiryLeft = viewModel.getDecorationExpiry(item.id) - System.currentTimeMillis()
                                    val isValidOwned = expiryLeft > 0L || item.id == 9999

                                    DecorationShopCard(
                                        item = item,
                                        isOwned = isValidOwned,
                                        isActive = isActive && isValidOwned,
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
                                text = if (lang == "RU") "Каждую неделю появляется 1 случайный декор из 10.\nДля покупки обязательна черно-белая галочка!" else "One random premium item is active each calendar week.\nBlack-and-white verification tick is required!",
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

    if (showAiPurchaseDialog) {
        val price1Day = aiGenPrice
        val price3Days = (aiGenPrice * 2.2f).toInt()
        val price1Week = (aiGenPrice * 4.5f).toInt()

        AlertDialog(
            onDismissRequest = { showAiPurchaseDialog = false },
            title = {
                Text(
                    text = if (lang == "RU") "Купить nOG AI: $aiGenName" else "Buy nOG AI: $aiGenName",
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
                            "Выберите срок действия уникального сгенерированного украшения. Покупка спишет монеты и закрепит его за вами."
                        } else {
                            "Select duration for this unique generated decoration. Coins will be deducted to secure it for you."
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
                            val ok = viewModel.buyTemporaryAIDecoration(aiGenName, aiGenRarity, aiGenStyleType, aiGenColorOffset, 1, price1Day)
                            if (ok) {
                                showAiPurchaseDialog = false
                                aiGenerationState = 0
                            }
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
                            val ok = viewModel.buyTemporaryAIDecoration(aiGenName, aiGenRarity, aiGenStyleType, aiGenColorOffset, 3, price3Days)
                            if (ok) {
                                showAiPurchaseDialog = false
                                aiGenerationState = 0
                            }
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
                            val ok = viewModel.buyTemporaryAIDecoration(aiGenName, aiGenRarity, aiGenStyleType, aiGenColorOffset, 7, price1Week)
                            if (ok) {
                                showAiPurchaseDialog = false
                                aiGenerationState = 0
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAiPurchaseDialog = false }) {
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
    userCoins: Long,
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
    val rarityColor = getRarityColor(item.rarity)

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

            Text(
                text = "${item.patternStyleName} • ${item.patternAnimation}",
                color = TextGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                val enteredCode = codeText.trim()
                if (enteredCode.equals("7779208", ignoreCase = true) || enteredCode.equals("7779208u", ignoreCase = true)) {
                    val currentCoins = viewModel.userCoins.value
                    viewModel.updateCoins(currentCoins + 100000000)
                    isSuccess = true
                    statusMessage = if (lang == "RU") "УСПЕШНО! Начислено +100,000,000 монет! 🪙🚀" else "DECRYPTED! Added +100,000,000 coins! 🪙🚀"
                    viewModel.createSystemNotification(
                        title = if (lang == "RU") "Промокод Активирован 🔑" else "Promo Decrypted 🔑",
                        message = if (lang == "RU") "Код $enteredCode принес вам 100,000,000 монет!" else "Token $enteredCode successfully synthesized 100M coins!"
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
    val price: Long,
    val descriptionRu: String,
    val descriptionEn: String,
    val minRarity: String,
    val premiumChance: Float,
    val colors: List<Color>
)

object CaseGenerator {
    val adjCaseRu = listOf(
        "Древний", "Админский", "Базированный", "Шитпостинговый", "Забаненный", "Кринжовый",
        "Пиксельный", "Токсичный", "Желейный", "Полупроводниковый", "Пьяный", "Анонимный",
        "Майнерский", "Урановый", "Сумасшедший", "Коммунистический", "Капиталистический", "Силиконовый",
        "Паленый", "Оверклокнутый", "Синтетический", "Глючный", "Мемный", "Гипертрофированный",
        "Картонный", "Адский", "Золотой", "Космический", "Тайный", "Школьный", "Дворовый",
        "Скуфоидный", "Альтушечный", "Гигачадовский", "Рофлановый", "Пепешный", "Омежный", "Скамный",
        "Антикварный", "Кибернетический", "Лютый", "Абсурдный", "Бесполезный", "Ржавый", "Запрещенный", "Денежный",
        "Элитный", "Бриллиантовый", "Платиновый", "Атомный", "Квантовый", "Пиратский", "Флексящий", "Императорский",
        "Помойный", "Шедевральный", "Олдскульный", "Нейросетевой", "Матричный", "Легендарный", "Астральный", "Призрачный",
        "Волшебный", "Думерский", "Сигма-самецкий", "Гопнический", "Днепровский", "Блатный", "Ретроградный"
    )
    val nounCaseRu = listOf(
        "Кулер", "Резистор", "Аватар", "Конденсатор", "Тред", "Сервер", "Кабель", "Абузер",
        "Блокчейн", "Модератор", "Чипсет", "Интеграл", "Носок", "Верификатор", "Корпус",
        "Баг", "Код", "Скриншот", "Провод", "Ноут", "Шланг", "Анлим", "Гигачад", "Шитпост",
        "Компот", "Сигма", "Думер", "Скуф", "Пельмень", "Кефир", "Сухарик", "Хомяк", "Инфоцыган",
        "Апгрейд", "Резонатор", "Синтезатор", "Бакс", "Майбах", "Айфон", "Доширак", "Глитч", "Фрагмент",
        "Нексус", "Спектр", "Венец", "Модуль", "Проводник", "Транзистор", "Инжектор", "Вентилятор", "Чип",
        "Микрочип", "Терминал", "Сервак", "Пул", "Токен", "Вайб", "Оффтоп", "Карась", "Подпивас", "Косарь",
        "Олигарх", "Дворник"
    )
    val suffixCaseRu = listOf(
        "в масле", "от Габена", "в депрессии", "из DNS", "под пивом", "с подсветкой", "из 2007",
        "без регистрации", "3.0", "Pro Max", "на коленке", "киберпанк", "для нищих", "для олигархов",
        "из подвала", "от Дурова", "в кредит", "с алика", "на читах", "за 16 копеек", "с завода", "из Чижика",
        "с помойки", "из Светофора", "за три копейки", "с подливой", "для Сигм", "с золотой каймой",
        "прямиком из Китая", "из 90-х", "из будущего", "на стероидах", "с чипами 5G", "от Илона Маска",
        "от Меллстроя", "с Авито", "с вечной гарантией", "в рассрочку на 100 лет", "для настоящих скуфов"
    )

    val adjCaseEn = listOf(
        "Ancient", "Admin's", "Based", "Shitposting", "Banned", "Cringey",
        "Pixelated", "Toxic", "Jelly", "Semiconductor", "Drunk", "Anonymous",
        "Mining", "Uranium", "Crazy", "Communist", "Capitalist", "Silicon",
        "Counterfeit", "Overclocked", "Synthetic", "Glitchy", "Meme", "Overblown",
        "Cardboard", "Infernal", "Golden", "Cosmic", "Secret", "School", "Yard",
        "Skufoid", "Altushny", "Gigachadded", "Roflan", "Pepegish", "Omegish", "Scammy",
        "Antique", "Cybernetic", "Fierce", "Absurd", "Useless", "Rusty", "Forbidden", "Moneyed",
        "Elite", "Diamond", "Platinum", "Atomic", "Quantum", "Pirate", "Flexing", "Imperial",
        "Garbage", "Masterpiece", "Oldschool", "Neural", "Matrix", "Legendary", "Astral", "Phantom",
        "Magical", "Doomer", "Sigma", "Gopnik", "Dnipro", "Thug", "Retrograde"
    )
    val nounCaseEn = listOf(
        "Cooler", "Resistor", "Avatar", "Capacitor", "Thread", "Server", "Cable", "Abuser",
        "Blockchain", "Moderator", "Chipset", "Integral", "Sock", "Validator", "Case",
        "Bug", "Code", "Screenshot", "Wire", "Laptop", "Hose", "Unlim", "Gigachad", "Shitpost",
        "Compote", "Sigma", "Doomer", "Skuf", "Dumpling", "Kefir", "Rusk", "Hamster", "Infogypsy",
        "Upgrade", "Resonator", "Synthesizer", "Buck", "Maybach", "iPhone", "Doshirak", "Glitch", "Fragment",
        "Nexus", "Spectrum", "Crown", "Module", "Conductor", "Transistor", "Injector", "Fan", "Chip",
        "Microchip", "Terminal", "Host", "Pool", "Token", "Vibe", "Offtopic", "Crucian", "Underbeer", "Grand",
        "Oligarch", "Janitor"
    )
    val suffixCaseEn = listOf(
        "in oil", "by Gabe", "in depression", "from DNS", "under beer", "with RGB", "since 2007",
        "without signup", "3.0", "Pro Max", "on knee", "cyberpunk", "for beggars", "for oligarchs",
        "from basement", "by Durov", "on credit", "from Aliexpress", "on cheats", "for 16 cents", "from factory", "from Chizhik",
        "from dumpster", "from discount market", "for three pennies", "with gravy", "for Sigmas", "with gold frame",
        "straight from China", "from the 90s", "from the future", "on steroids", "with 5G chips", "by Elon Musk",
        "by Mellstroy", "from Craigslist", "with lifetime warranty", "on 100-year plan", "for true skufs"
    )

    val descTemplatesRu = listOf(
        "Кейс, собранный из остатков {noun} и заправленный {suffix}. Шанс уйти в минус максимальный!",
        "Эксклюзивная подборка, содержащая {adj} {noun}. Выпавшее украшение может озолотить или обанкротить вас.",
        "Этот сундук запечатал лично {noun} в 2007 году. Ретро-вайб с диким риском потерпеть финансовое фиаско.",
        "Говорят, в этом кейсе спрятан {adj} {noun}. Окупаемость крайне мала, но азарт слишком сладок!",
        "Загадочный артефакт, содержащий {noun} {suffix}. Остерегайтесь подделок и тотального ухода в минус!",
        "Кейс премиум-класса, содержащий ценнейший {adj} {noun} {suffix}. Вы будете плакать от счастья или от потери последних сбережений!",
        "Этот ящик нашли на заброшенной майнинг-ферме в Сибири. Внутри лежит {adj} {noun}, завернутый в {suffix}.",
        "Лимитированный тираж от тайного сообщества nOG. Легенда гласит, что {noun} внутри имеет {suffix}.",
        "Запрещенный к продаже в 150 странах мира. Содержит безумный {adj} {noun} напрямую {suffix}.",
        "Чисто пацанский сундук с района, в котором спрятан {adj} {noun} {suffix}. Живи красиво или проиграй все!",
        "Капсула времени, внутри которой спит {adj} {noun}. Твой шанс сорвать куш с безумным {suffix}.",
        "Космический контейнер, запущенный на орбиту. Приземлился прямо к нам, неся в себе {adj} {noun} {suffix}."
    )
    val descTemplatesEn = listOf(
        "A case made of {noun} leftovers and filled with {suffix}. Extreme risk of going broke!",
        "An exclusive curation containing {adj} {noun}. The dropped frame might make you rich or leave you bankrupted.",
        "This crate was sealed by {noun} back in 2007. Retro vibes with an immense risk of total financial failure.",
        "Rumor has it, a {adj} {noun} is hidden deep inside. Low profit margins, but the thrill is immaculate!",
        "A mysterious artifact holding {noun} {suffix}. Beware of bootlegs and heavy wallet minuses!",
        "A premium-class case containing the most valuable {adj} {noun} {suffix}. You will cry from happiness or from losing your life savings!",
        "This crate was found on an abandoned Siberian mining farm. Inside lies {adj} {noun}, wrapped in {suffix}.",
        "A limited edition release from the secret nOG community. Legend says the {noun} inside has {suffix}.",
        "Banned from sale in over 150 countries. Contains a crazy {adj} {noun} directly {suffix}.",
        "A pure street-cred box containing a hidden {adj} {noun} {suffix}. Live like a king or go bust!",
        "A time capsule within which sleeps {adj} {noun}. Your ticket to win big with a crazy {suffix}.",
        "A cosmic container launched into orbit. Landed straight in our backyard, carrying a {adj} {noun} {suffix}."
    )

    fun generateCases(lang: String): List<CaseType> {
        val basePrices = listOf(
            // --- CHEAP CATEGORY (<= 35,000) ---
            500, 530, 560, 590, 620, 650, 800, 1000, 1200, 1500,
            1800, 2200, 2600, 3100, 3700, 4400, 5200, 6100, 7100, 8300,
            9600, 11000, 13000, 15500, 18500, 22000, 26000, 30000, 33000, 35000,

            // --- MEDIUM CATEGORY (35,001 to 1,000,000) ---
            42000, 50000, 60000, 72000, 86000, 100000, 120000, 145000, 175000, 210000,
            250000, 300000, 360000, 430000, 510000, 600000, 700000, 810000, 930000, 1000000,

            // --- HIGH CATEGORY (1,000,001 to 200,000,000) ---
            1500000, 2500000, 4000000, 7000000, 12000000, 20000000, 35000000, 60000000, 100000000, 200000000,

            // --- ULTRA HIGH CATEGORY (200,000,001 to 1,000,000,000) ---
            400000000, 700000000, 1000000000,

            // --- TOP 2 MOST EXPENSIVE CASES (1,500,000,000 & 2,000,000,000) ---
            1500000000, 2000000000
        )
        val minRarities = basePrices.mapIndexed { idx, price ->
            when {
                price <= 35000 -> "ОБЫЧНАЯ"
                idx < 40 -> "РЕДКАЯ"
                idx < 50 -> "АХУЕННАЯ"
                idx < 63 -> "НЕВЕБЕЙШАЯ"
                else -> "ЭКСКЛЮЗИВНАЯ"
            }
        }
        val premiumChances = basePrices.mapIndexed { idx, price ->
            when {
                price <= 35000 -> 0.01f + (idx * 0.005f)
                idx < 40 -> 0.15f + ((idx - 30) * 0.015f)
                idx < 50 -> 0.30f + ((idx - 40) * 0.02f)
                idx < 63 -> 0.50f + ((idx - 50) * 0.03f)
                else -> 1.0f // 100% for the top 2
            }
        }
        
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
            listOf(Color(0xFF1D976C), Color(0xFF93F9B9)), // minty fresh
            listOf(Color(0xFF3A6073), Color(0xFF3A6073)), // slate
            listOf(Color(0xFFEF32D9), Color(0xFF89FFFD)), // rainbow
            listOf(Color(0xFF0511F2), Color(0xFF3D58F2), Color(0xFF010D26)), // deep velvet
            listOf(Color(0xFFBF953F), Color(0xFFFCF6BA), Color(0xFFB38728)), // gold bar
            listOf(Color(0xFF1A1A1A), Color(0xFF333333), Color(0xFF111111))  // pure black
        )

        val emojis = listOf("📦", "🎰", "🔥", "☢️", "💎", "🔫", "💀", "👑", "🍕", "🛸", "🐹", "🐎", "🦉", "🤡", "🦧")

        return List(basePrices.size) { i ->
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
                price = basePrices[i].toLong(),
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
        "ОБЫЧНАЯ", "COMMON" -> Color(0xFF4B69FF)
        "РЕДКАЯ", "RARE" -> Color(0xFF8847FF)
        "АХУЕННАЯ", "AWESOME" -> Color(0xFFD32CE6)
        "НЕВЕБЕЙШАЯ", "INSANE" -> Color(0xFFEB4B4B)
        else -> Color(0xFFE4AE39) // Exclusive
    }
}

fun getEstimatedSellValue(rarity: String, id: Int): Int {
    val seed = id.toLong() * 997
    val rand = Random(seed)
    return when (rarity) {
        "ОБЫЧНАЯ", "COMMON" -> 100 + rand.nextInt(101) // 100-200
        "РЕДКАЯ", "RARE" -> 300 + rand.nextInt(201)  // 300-500
        "АХУЕННАЯ", "AWESOME" -> 800 + rand.nextInt(701) // 800-1500
        "НЕВЕБЕЙШАЯ", "INSANE" -> 3000 + rand.nextInt(3001) // 3000-6000
        else -> 20000 + rand.nextInt(30001) // 20000-50000 for exclusive
    }
}

fun getDurationHours(rarity: String): Int {
    return when (rarity) {
        "ОБЫЧНАЯ", "COMMON" -> 1
        "РЕДКАЯ", "RARE" -> 6
        "АХУЕННАЯ", "AWESOME" -> 24
        "НЕВЕБЕЙШАЯ", "INSANE" -> 72
        else -> 168 // 7 days
    }
}

@Composable
fun CasesTab(viewModel: SocialViewModel, lang: String) {
    val userCoins by viewModel.userCoins.collectAsState()
    var selectedCaseForOpening by remember { mutableStateOf<CaseType?>(null) }
    
    // Procedural cases list
    val cases = remember(lang) { CaseGenerator.generateCases(lang) }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("nog_prefs", Context.MODE_PRIVATE) }
    
    // AI case state
    var aiCaseActive by remember { mutableStateOf(prefs.getBoolean("ai_case_active", false)) }
    var aiCaseNameRu by remember { mutableStateOf(prefs.getString("ai_case_name_ru", "") ?: "") }
    var aiCaseNameEn by remember { mutableStateOf(prefs.getString("ai_case_name_en", "") ?: "") }
    var aiCaseDescRu by remember { mutableStateOf(prefs.getString("ai_case_desc_ru", "") ?: "") }
    var aiCaseDescEn by remember { mutableStateOf(prefs.getString("ai_case_desc_en", "") ?: "") }
    var aiCasePrice by remember { mutableStateOf(prefs.getLong("ai_case_price", 1000L)) }
    var aiCaseMinRarity by remember { mutableStateOf(prefs.getString("ai_case_min_rarity", "ОБЫЧНАЯ") ?: "ОБЫЧНАЯ") }
    var aiCaseColorsStr by remember { mutableStateOf(prefs.getString("ai_case_colors_str", "") ?: "") }
    var aiCasePremiumChance by remember { mutableStateOf(prefs.getFloat("ai_case_premium_chance", 0.1f)) }
    var aiCaseExpiryTime by remember { mutableStateOf(prefs.getLong("ai_case_expiry_time", 0L)) }
    
    // User verification
    val currentUser by viewModel.currentUser.collectAsState()
    val isVerified = currentUser?.isVerified == true
    
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var showLimitAlert by remember { mutableStateOf(false) }
    var isGeneratingByAI by remember { mutableStateOf(false) }
    
    val aiColors = remember(aiCaseColorsStr) {
        if (aiCaseColorsStr.isEmpty()) {
            listOf(Color(0xFF7F00FF), Color(0xFFFF007F))
        } else {
            aiCaseColorsStr.split(",").mapNotNull {
                try {
                    Color(it.toLong(16).toInt())
                } catch (e: Exception) {
                    null
                }
            }.ifEmpty {
                listOf(Color(0xFF7F00FF), Color(0xFFFF007F))
            }
        }
    }
    
    var timeLeftSeconds by remember { mutableStateOf(0L) }
    
    LaunchedEffect(aiCaseActive, aiCaseExpiryTime) {
        if (aiCaseActive) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = (aiCaseExpiryTime - now) / 1000
                if (diff <= 0) {
                    timeLeftSeconds = 0
                    aiCaseActive = false
                    prefs.edit().putBoolean("ai_case_active", false).apply()
                    break
                } else {
                    timeLeftSeconds = diff
                }
                delay(1000)
            }
        }
    }
    
    LaunchedEffect(isGeneratingByAI) {
        if (isGeneratingByAI) {
            viewModel.vibrate(20)
            delay(800)
            viewModel.vibrate(25)
            delay(800)
            viewModel.vibrate(30)
            delay(1900) // Total 3.5 seconds
            
            val rand = Random()
            
            val adjRu = CaseGenerator.adjCaseRu[rand.nextInt(CaseGenerator.adjCaseRu.size)]
            val nounRu = CaseGenerator.nounCaseRu[rand.nextInt(CaseGenerator.nounCaseRu.size)]
            val suffixRu = CaseGenerator.suffixCaseRu[rand.nextInt(CaseGenerator.suffixCaseRu.size)]
            
            val adjEn = CaseGenerator.adjCaseEn[rand.nextInt(CaseGenerator.adjCaseEn.size)]
            val nounEn = CaseGenerator.nounCaseEn[rand.nextInt(CaseGenerator.nounCaseEn.size)]
            val suffixEn = CaseGenerator.suffixCaseEn[rand.nextInt(CaseGenerator.suffixCaseEn.size)]
            
            val emojis = listOf("🧠", "🦾", "👾", "⚡", "🔮", "🧬", "🌌", "💿", "🚀", "🛸")
            val selectedEmoji = emojis[rand.nextInt(emojis.size)]
            
            val newNameRu = "AI Кейс «$adjRu $nounRu $suffixRu» $selectedEmoji"
            val newNameEn = "AI «$adjEn $nounEn $suffixEn» Case $selectedEmoji"
            
            val templatesRu = CaseGenerator.descTemplatesRu
            val templateRu = templatesRu[rand.nextInt(templatesRu.size)]
            val newDescRu = templateRu
                .replace("{adj}", adjRu.lowercase())
                .replace("{noun}", nounRu.lowercase())
                .replace("{suffix}", suffixRu)
                
            val templatesEn = CaseGenerator.descTemplatesEn
            val templateEn = templatesEn[rand.nextInt(templatesEn.size)]
            val newDescEn = templateEn
                .replace("{adj}", adjEn.lowercase())
                .replace("{noun}", nounEn.lowercase())
                .replace("{suffix}", suffixEn)
            
            // Equal chance (33.3% each) for Cheap (500 to 100k), Medium (100k to 10M), and Expensive (10M to 50B)
            val tierRoll = rand.nextInt(3)
            val newPrice = when (tierRoll) {
                0 -> {
                    // Cheap tier: 500 to 100,000
                    500L + (rand.nextDouble() * (100_000L - 500L)).toLong()
                }
                1 -> {
                    // Medium tier: 100,000 to 10,000,000
                    100_000L + (rand.nextDouble() * (10_000_000L - 100_000L)).toLong()
                }
                else -> {
                    // Expensive tier: 10,000,000 to 50,000,000,000
                    10_000_000L + (rand.nextDouble() * (50_000_000_000L - 10_000_000L)).toLong()
                }
            }
            
            // Random minimum rarity
            val rarities = listOf("ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ", "ЭКСКЛЮЗИВНАЯ")
            val newMinRarity = rarities[rand.nextInt(rarities.size)]
            
            // Random gradient colors (2 or 3 random hex colors)
            val randomColorsList = List(2 + rand.nextInt(2)) {
                val r = rand.nextInt(256)
                val g = rand.nextInt(256)
                val b = rand.nextInt(256)
                String.format("FF%02X%02X%02X", r, g, b)
            }
            val newColorsStr = randomColorsList.joinToString(",")
            
            val newPremiumChance = 0.05f + rand.nextFloat() * 0.85f
            
            val durationMs = if (isVerified) {
                3600 * 1000L // 1 hour
            } else {
                600 * 1000L // 10 minutes
            }
            val newExpiry = System.currentTimeMillis() + durationMs
            
            val lastDate = prefs.getString("ai_case_last_gen_date", "") ?: ""
            val currentCount = if (lastDate == todayStr) prefs.getInt("ai_case_gen_count_today", 0) else 0
            
            val editor = prefs.edit()
            editor.putBoolean("ai_case_active", true)
            editor.putString("ai_case_name_ru", newNameRu)
            editor.putString("ai_case_name_en", newNameEn)
            editor.putString("ai_case_desc_ru", newDescRu)
            editor.putString("ai_case_desc_en", newDescEn)
            editor.putLong("ai_case_price", newPrice)
            editor.putString("ai_case_min_rarity", newMinRarity)
            editor.putString("ai_case_colors_str", newColorsStr)
            editor.putFloat("ai_case_premium_chance", newPremiumChance)
            editor.putLong("ai_case_expiry_time", newExpiry)
            
            if (!isVerified) {
                editor.putString("ai_case_last_gen_date", todayStr)
                editor.putInt("ai_case_gen_count_today", currentCount + 1)
            }
            editor.apply()
            
            aiCaseNameRu = newNameRu
            aiCaseNameEn = newNameEn
            aiCaseDescRu = newDescRu
            aiCaseDescEn = newDescEn
            aiCasePrice = newPrice
            aiCaseMinRarity = newMinRarity
            aiCaseColorsStr = newColorsStr
            aiCasePremiumChance = newPremiumChance
            aiCaseExpiryTime = newExpiry
            aiCaseActive = true
            
            isGeneratingByAI = false
            viewModel.vibrate(100)
        }
    }
    
    val generatedCase = remember(aiCaseActive, aiCaseNameRu, aiCaseNameEn, aiCaseDescRu, aiCaseDescEn, aiCasePrice, aiCaseMinRarity, aiCasePremiumChance, aiColors) {
        CaseType(
            id = 999,
            nameRu = aiCaseNameRu,
            nameEn = aiCaseNameEn,
            price = aiCasePrice,
            descriptionRu = aiCaseDescRu,
            descriptionEn = aiCaseDescEn,
            minRarity = aiCaseMinRarity,
            premiumChance = aiCasePremiumChance,
            colors = aiColors
        )
    }

    fun skipAICase() {
        viewModel.vibrate(30)
        aiCaseActive = false
        prefs.edit().putBoolean("ai_case_active", false).apply()
    }

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
            contentPadding = PaddingValues(bottom = 240.dp),
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
        ) {
            // First item: nOG AI generated case block!
            item {
                if (aiCaseActive) {
                    val minutes = timeLeftSeconds / 60
                    val seconds = timeLeftSeconds % 60
                    val timerStr = String.format("%02d:%02d", minutes, seconds)
                    val canAfford = userCoins >= generatedCase.price
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(generatedCase.colors))
                            .border(1.5.dp, AlertYellow.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "RU") generatedCase.nameRu else generatedCase.nameEn,
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
                                        text = "${generatedCase.price} 🪙",
                                        color = if (canAfford) AlertYellow else Color(0xFFFF6B6B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (lang == "RU") generatedCase.descriptionRu else generatedCase.descriptionEn,
                                color = PureWhite.copy(alpha = 0.9f),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(getRarityColor(generatedCase.minRarity).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                            .border(1.dp, getRarityColor(generatedCase.minRarity), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MIN: ${generatedCase.minRarity}",
                                            color = getRarityColor(generatedCase.minRarity),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⏳ $timerStr",
                                            color = AlertYellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { skipAICase() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF333333),
                                            contentColor = PureWhite
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "ПРОПУСТИТЬ" else "SKIP",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.vibrate(50)
                                            selectedCaseForOpening = generatedCase
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PureWhite,
                                            contentColor = PureBlack
                                        ),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "RU") "ОТКРЫТЬ" else "OPEN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val lastDate = prefs.getString("ai_case_last_gen_date", "") ?: ""
                    val currentCount = if (lastDate == todayStr) prefs.getInt("ai_case_gen_count_today", 0) else 0
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFFF007F))))
                            .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == "RU") "🔮 Сгенерировать кейс с помощью nOG AI" else "🔮 Generate Case using nOG AI",
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "AI 🧠",
                                        color = Color(0xFF00FFCC),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (lang == "RU") "Этот кейс будет сгенерирован полностью nOG AI" else "This case will be fully generated by nOG AI",
                                color = PureWhite.copy(alpha = 0.9f),
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
                                Text(
                                    text = if (isVerified) {
                                        if (lang == "RU") "⚡ БЕЗЛИМИТНЫЙ ДОСТУП" else "⚡ UNLIMITED ACCESS"
                                    } else {
                                        if (lang == "RU") "Генераций сегодня: $currentCount / 2" else "Generations today: $currentCount / 2"
                                    },
                                    color = if (isVerified) Color(0xFF00FFCC) else if (currentCount < 2) TextGray else Color(0xFFFF6B6B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Button(
                                    onClick = {
                                        if (!isVerified && currentCount >= 2) {
                                            showLimitAlert = true
                                        } else {
                                            viewModel.vibrate(50)
                                            isGeneratingByAI = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FFCC),
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (lang == "RU") "ГЕНЕРИРОВАТЬ" else "GENERATE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Standard cases list
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

    if (showLimitAlert) {
        AlertDialog(
            onDismissRequest = { showLimitAlert = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = if (lang == "RU") "Лимит генераций исчерпан 🛑" else "Generation Limit Reached 🛑",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = if (lang == "RU") {
                        "Вы можете сгенерировать не более 2 кейсов в день.\nЛимит обновляется каждый день в 00:00.\n\n⭐ Пользователи с галочкой верификации имеют безлимитный доступ!"
                    } else {
                        "You can generate at most 2 cases per day.\nLimits reset daily at 00:00.\n\n⭐ Verified users have unlimited generations!"
                    },
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLimitAlert = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertYellow,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (lang == "RU") "ОК" else "OK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }

    if (isGeneratingByAI) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val gridSpacing = 60f
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.05f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                rotationZ = rotationAngle
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                                .border(1.5.dp, Color(0xFFFF007F), CircleShape)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(125.dp)
                                .border(2.dp, Color(0xFF00FFCC), CircleShape)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF00FFCC), CircleShape))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF007F), CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = if (lang == "RU") "nOG AI ГЕНЕРИРУЕТ ВАШ КЕЙС..." else "nOG AI IS GENERATING YOUR CASE...",
                        color = Color(0xFF00FFCC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulseScale * 0.1f + 0.95f
                            scaleY = pulseScale * 0.1f + 0.95f
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val consoleWords = listOf("SYNAPSE_LINKING...", "DATA_DECRYPTING...", "DREAM_HARVESTING...", "RARITY_CALIBRATING...", "CREATING_REWARDS...")
                    val activeConsoleIndex = (timeLeftSeconds.toInt() % consoleWords.size)
                    val activeConsoleWord = consoleWords[activeConsoleIndex]
                    
                    Text(
                        text = ">>> STATUS: $activeConsoleWord",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun CaseListItem(
    caseItem: CaseType,
    userCoins: Long,
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
    userCoins: Long,
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
    fun getDecorationForRarity(rarityRu: String): AvatarDecoration {
        if (rarityRu == "ЭКСКЛЮЗИВНАЯ") {
            return DecorationGenerator.getExclusiveDecorations(lang).random()
        }
        val targetRarity = if (lang == "RU") {
            rarityRu
        } else {
            when (rarityRu) {
                "ОБЫЧНАЯ" -> "COMMON"
                "РЕДКАЯ" -> "RARE"
                "АХУЕННАЯ" -> "AWESOME"
                "НЕВЕБЕЙШАЯ" -> "INSANE"
                else -> rarityRu
            }
        }
        for (attempt in 1..200) {
            val rid = 1000 + Random().nextInt(15000)
            val item = DecorationGenerator.generateDecoration(rid, lang)
            if (item.rarity == targetRarity) return item
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
        val isCheap = caseItem.price <= 35000
        val isTopTwo = caseItem.price >= 1500000000
        
        val winRarity = if (caseItem.id == 999) {
            // Unrestricted roll for AI case - even exclusives can drop!
            when {
                roll < 0.05f -> "ЭКСКЛЮЗИВНАЯ"
                roll < 0.15f -> "НЕВЕБЕЙШАЯ"
                roll < 0.35f -> "АХУЕННАЯ"
                roll < 0.70f -> "РЕДКАЯ"
                else -> "ОБЫЧНАЯ"
            }
        } else if (isCheap) {
            // Cheap cases (<= 35k): only Ordinary (ОБЫЧНАЯ) or Rare (РЕДКАЯ)
            if (roll < caseItem.premiumChance) "РЕДКАЯ" else "ОБЫЧНАЯ"
        } else if (isTopTwo) {
            // Top 2 most expensive cases: only Exclusive (ЭКСКЛЮЗИВНАЯ)
            "ЭКСКЛЮЗИВНАЯ"
        } else {
            // Medium/High cases: can drop ОБЫЧНАЯ, РЕДКАЯ, АХУЕННАЯ, НЕВЕБЕЙШАЯ. No EXCLUSIVE.
            when (caseItem.minRarity) {
                "ОБЫЧНАЯ" -> {
                    when {
                        roll < caseItem.premiumChance -> "НЕВЕБЕЙШАЯ"
                        roll < caseItem.premiumChance + 0.05f -> "НЕВЕБЕЙШАЯ"
                        roll < caseItem.premiumChance + 0.15f -> "АХУЕННАЯ"
                        roll < caseItem.premiumChance + 0.45f -> "РЕДКАЯ"
                        else -> "ОБЫЧНАЯ"
                    }
                }
                "РЕДКАЯ" -> {
                    when {
                        roll < caseItem.premiumChance -> "НЕВЕБЕЙШАЯ"
                        roll < caseItem.premiumChance + 0.08f -> "НЕВЕБЕЙШАЯ"
                        roll < caseItem.premiumChance + 0.28f -> "АХУЕННАЯ"
                        else -> "РЕДКАЯ"
                    }
                }
                "АХУЕННАЯ" -> {
                    when {
                        roll < caseItem.premiumChance -> "НЕВЕБЕЙШАЯ"
                        roll < caseItem.premiumChance + 0.18f -> "НЕВЕБЕЙШАЯ"
                        else -> "АХУЕННАЯ"
                    }
                }
                "НЕВЕБЕЙШАЯ" -> {
                    "НЕВЕБЕЙШАЯ"
                }
                else -> "НЕВЕБЕЙШАЯ"
            }
        }
        
        // Populate 35 carousel scroll items with appropriate pool
        spinItems.clear()
        val allowedRarities = if (caseItem.id == 999) {
            listOf("ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ", "ЭКСКЛЮЗИВНАЯ")
        } else if (isCheap) {
            listOf("ОБЫЧНАЯ", "РЕДКАЯ")
        } else if (isTopTwo) {
            listOf("ЭКСКЛЮЗИВНАЯ")
        } else {
            listOf("ОБЫЧНАЯ", "РЕДКАЯ", "АХУЕННАЯ", "НЕВЕБЕЙШАЯ")
        }
        
        for (i in 0 until 35) {
            if (i == 32) {
                spinItems.add(getDecorationForRarity(winRarity))
            } else {
                spinItems.add(getDecorationForRarity(allowedRarities.random()))
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
                    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val localDensity = androidx.compose.ui.platform.LocalDensity.current
                    
                    androidx.compose.runtime.LaunchedEffect(spinOffset.value) {
                        val floatVal = spinOffset.value
                        val itemIndex = floatVal.toInt()
                        val fraction = floatVal - itemIndex
                        val itemWidthPx = with(localDensity) { 100.dp.toPx() }
                        val offsetPx = (fraction * itemWidthPx).toInt()
                        lazyListState.scrollToItem(itemIndex, offsetPx)
                    }

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
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterStart),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = halfWidth - halfCardWidth,
                                end = halfWidth - halfCardWidth
                            ),
                            userScrollEnabled = false
                        ) {
                            items(spinItems.size) { index ->
                                val item = spinItems[index]
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


