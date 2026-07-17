package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import com.example.data.UserEntity
import com.example.ui.SocialViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.material3.TextButton

data class TamagotchiState(
    val hasPet: Boolean = false,
    val petBotId: String? = null,
    val petName: String = "",
    val petHandle: String = "",
    val petAvatar: String = "",
    val birthTime: Long = 0L,
    val ageHours: Float = 0f,
    val hunger: Float = 100f,
    val hygiene: Float = 100f,
    val mood: Float = 100f,
    val health: Float = 100f,
    val feedScrollPoints: Float = 0f,
    val isSick: Boolean = false,
    val sickDaysRequired: Int = 0,
    val sickDaysPassed: Int = 0,
    val sickHoursRequiredEachDay: Float = 0f,
    val sickTimeSpentToday: Float = 0f,
    val lastFedTime: Long = 0L,
    val lastWashedTime: Long = 0L,
    val growthStage: String = "EGG", // EGG, BABY, ADULT, FAMILY
    val eggColorId: Int = 0,
    val eggPatternId: Int = 0,
    val breedParent1: String? = null,
    val breedParent2: String? = null,
    val breedParent1Avatar: String? = null,
    val breedParent2Avatar: String? = null,
    val breedEggColorId: Int = 0,
    val breedEggPatternId: Int = 0,
    val isDead: Boolean = false,
    val deathReason: String? = null, // "disease", "neglect", "old_age"
    val deathDiseaseName: String? = null,
    val lastTickTime: Long = 0L,
    val cooldownUntil: Long = 0L
)

object TamagotchiManager {
    fun loadState(context: Context): TamagotchiState {
        val prefs = context.getSharedPreferences("nog_tamagotchi_prefs3", Context.MODE_PRIVATE)
        val hasPet = prefs.getBoolean("has_pet", false)
        val cooldownUntil = prefs.getLong("cooldown_until", 0L)
        if (!hasPet) {
            return TamagotchiState(hasPet = false, cooldownUntil = cooldownUntil)
        }
        return TamagotchiState(
            hasPet = true,
            petBotId = prefs.getString("pet_bot_id", ""),
            petName = prefs.getString("pet_name", "CyberBot") ?: "CyberBot",
            petHandle = prefs.getString("pet_handle", "@cyberbot") ?: "@cyberbot",
            petAvatar = prefs.getString("pet_avatar", "") ?: "",
            birthTime = prefs.getLong("birth_time", 0L),
            ageHours = prefs.getFloat("age_hours", 0f),
            hunger = prefs.getFloat("hunger", 100f),
            hygiene = prefs.getFloat("hygiene", 100f),
            mood = prefs.getFloat("mood", 100f),
            health = prefs.getFloat("health", 100f),
            feedScrollPoints = prefs.getFloat("feed_scroll_points", 0f),
            isSick = prefs.getBoolean("is_sick", false),
            sickDaysRequired = prefs.getInt("sick_days_required", 0),
            sickDaysPassed = prefs.getInt("sick_days_passed", 0),
            sickHoursRequiredEachDay = prefs.getFloat("sick_hours_required", 0f),
            sickTimeSpentToday = prefs.getFloat("sick_spent_today", 0f),
            lastFedTime = prefs.getLong("last_fed", 0L),
            lastWashedTime = prefs.getLong("last_washed", 0L),
            growthStage = prefs.getString("growth_stage", "EGG") ?: "EGG",
            eggColorId = prefs.getInt("egg_color", 0),
            eggPatternId = prefs.getInt("egg_pattern", 0),
            breedParent1 = prefs.getString("breed_p1", null),
            breedParent2 = prefs.getString("breed_p2", null),
            breedParent1Avatar = prefs.getString("breed_p1_avatar", null),
            breedParent2Avatar = prefs.getString("breed_p2_avatar", null),
            breedEggColorId = prefs.getInt("breed_egg_color", 0),
            breedEggPatternId = prefs.getInt("breed_egg_pattern", 0),
            isDead = prefs.getBoolean("is_dead", false),
            deathReason = prefs.getString("death_reason", null),
            deathDiseaseName = prefs.getString("death_disease_name", null),
            lastTickTime = prefs.getLong("last_tick", System.currentTimeMillis()),
            cooldownUntil = cooldownUntil
        )
    }

    fun saveState(context: Context, state: TamagotchiState) {
        val prefs = context.getSharedPreferences("nog_tamagotchi_prefs3", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("has_pet", state.hasPet)
            putString("pet_bot_id", state.petBotId)
            putString("pet_name", state.petName)
            putString("pet_handle", state.petHandle)
            putString("pet_avatar", state.petAvatar)
            putLong("birth_time", state.birthTime)
            putFloat("age_hours", state.ageHours)
            putFloat("hunger", state.hunger)
            putFloat("hygiene", state.hygiene)
            putFloat("mood", state.mood)
            putFloat("health", state.health)
            putFloat("feed_scroll_points", state.feedScrollPoints)
            putBoolean("is_sick", state.isSick)
            putInt("sick_days_required", state.sickDaysRequired)
            putInt("sick_days_passed", state.sickDaysPassed)
            putFloat("sick_hours_required", state.sickHoursRequiredEachDay)
            putFloat("sick_spent_today", state.sickTimeSpentToday)
            putLong("last_fed", state.lastFedTime)
            putLong("last_washed", state.lastWashedTime)
            putString("growth_stage", state.growthStage)
            putInt("egg_color", state.eggColorId)
            putInt("egg_pattern", state.eggPatternId)
            putString("breed_p1", state.breedParent1)
            putString("breed_p2", state.breedParent2)
            putString("breed_p1_avatar", state.breedParent1Avatar)
            putString("breed_p2_avatar", state.breedParent2Avatar)
            putInt("breed_egg_color", state.breedEggColorId)
            putInt("breed_egg_pattern", state.breedEggPatternId)
            putBoolean("is_dead", state.isDead)
            putString("death_reason", state.deathReason)
            putString("death_disease_name", state.deathDiseaseName)
            putLong("last_tick", state.lastTickTime)
            putLong("cooldown_until", state.cooldownUntil)
            apply()
        }
    }

    fun resetState(context: Context, cooldownUntil: Long = 0L) {
        val prefs = context.getSharedPreferences("nog_tamagotchi_prefs3", Context.MODE_PRIVATE)
        prefs.edit().clear().putLong("cooldown_until", cooldownUntil).apply()
    }
}

val funnyDiseasesList = listOf(
    "сифилис",
    "вич",
    "спид",
    "туберкулез",
    "анальные трещины",
    "непрерывная рвота",
    "сетевой склероз транзисторов",
    "передозировка квантовым сахаром",
    "критический спазм вывода ядра",
    "синдром синего экрана экстаза",
    "цифровой геморрой 3-й степени",
    "острая нехватка системного пива"
)

val funnyDiseasesListEn = listOf(
    "syphilis",
    "hiv",
    "aids",
    "tuberculosis",
    "anal fissures",
    "incessant vomiting",
    "silicon transistor sclerosis",
    "quantum sugar overdose",
    "kernel panic spasm",
    "blue screen of ecstasy syndrome",
    "digital hemorrhoids level 3",
    "acute synthetic beer deficiency"
)


@Composable
fun ProgressBarWithLabel(
    label: String,
    value: Float,
    isAlert: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isAlert) PureWhite else TextGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.weight(1f).height(6.dp),
            color = if (isAlert) AlertRed else PureWhite,
            trackColor = DeepGray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${value.toInt()}%",
            color = if (isAlert) PureWhite else TextGray,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TamagotchiDialog(
    lang: String,
    viewModel: SocialViewModel,
    users: List<UserEntity>,
    currentUser: UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRu = lang == "RU"

    var state by remember { mutableStateOf(TamagotchiManager.loadState(context)) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingPet by remember { mutableStateOf(false) }
    var showBreedingDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(isGeneratingPet) {
        if (isGeneratingPet) {
            delay(2000L) // fake generation delay
            
            val candidateBots = users.filter { it.id != (currentUser?.id ?: "") }
            if (candidateBots.isNotEmpty()) {
                val pickedBot = candidateBots.random()
                val newStage = listOf("EGG", "BABY", "ADULT").random()
                val newPet = TamagotchiState(
                    hasPet = true,
                    petBotId = pickedBot.id,
                    petName = pickedBot.username,
                    petHandle = pickedBot.handle,
                    petAvatar = pickedBot.avatarUrl,
                    birthTime = System.currentTimeMillis(),
                    lastTickTime = System.currentTimeMillis(),
                    growthStage = newStage,
                    eggColorId = Random.nextInt(12),
                    eggPatternId = Random.nextInt(5)
                )
                state = newPet
                TamagotchiManager.saveState(context, newPet)
                notificationMessage = if (isRu) {
                    when (newStage) {
                        "EGG" -> "Успех! Яйцо получено!"
                        "BABY" -> "Неожиданно! Вылупился сразу Малыш!"
                        else -> "Ого! К вам пришел уже Взрослый Питомец!"
                    }
                } else {
                    when (newStage) {
                        "EGG" -> "Success! Egg generated!"
                        "BABY" -> "Surprise! A Baby just dropped in!"
                        else -> "Wow! An Adult pet joined you!"
                    }
                }
            } else {
                notificationMessage = if (isRu) "Список ботов для связи пуст!" else "No node candidates detected!"
            }
            
            isGeneratingPet = false
        }
    }
    var sittingTimeMs by remember { mutableStateOf(0L) } // track sitting time
    var washTaps by remember { mutableStateOf(0) } // tap counter for washing

    LaunchedEffect(notificationMessage) {
        val msg = notificationMessage
        if (msg != null && msg.isNotEmpty()) {
            viewModel.createSystemNotification(
                title = if (isRu) "Тамагочи" else "Tamagotchi System",
                message = msg
            )
        }
    }

    val isUserVerified = currentUser?.isVerified == true

    // Offline progress calculations and real-time ticking loop when dialog is open
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        if (state.hasPet && !state.isDead) {
            val deltaMs = now - state.lastTickTime
            if (deltaMs > 2000L) { // If spent time offline, update now!
                state = updateTamaStats(state, deltaMs, isAppActive = false)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    TamagotchiManager.saveState(context, state)
                }
            }
        } else {
            // Keep tick updated to now
            state = state.copy(lastTickTime = now)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                TamagotchiManager.saveState(context, state)
            }
        }

        // Active ticking loop while the User is looking at the Tamagotchi
        while (true) {
            val delayTime = if (viewModel.isLowEndDeviceMode.value) 5000L else 1000L
            delay(delayTime)
            sittingTimeMs += delayTime
            val tickNow = System.currentTimeMillis()
            if (state.hasPet && !state.isDead) {
                // Every second of active view increases mood slightly, and adds to treatment time if sick
                state = updateTamaStats(state, tickNow - state.lastTickTime, isAppActive = true)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    TamagotchiManager.saveState(context, state)
                }
            } else {
                state = state.copy(lastTickTime = tickNow)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    TamagotchiManager.saveState(context, state)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.92f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Card styled in strict retrospective grayscale/monochrome styling
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepGray),
                border = BorderStroke(2.dp, PureWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .testTag("tamagotchi_dialog_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                            .background(CardGray)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Pets,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu) "ТАМАГОЧИ ИНТЕРФЕЙС" else "TAMAGOTCHI OS",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.vibrate(25)
                                onDismiss()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notifications Area
                    AnimatedVisibility(
                        visible = notificationMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        notificationMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .background(PureBlack)
                                    .border(1.dp, PureWhite, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "[X]",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { notificationMessage = null }
                                    )
                                }
                            }
                        }
                    }

                    // --- TAMAGOTCHI CONSOLE VIEWPORTS ---
                    if (!state.hasPet) {
                        // NO ACTIVE PET: Bred Options & Cooldown Checking
                        val now = System.currentTimeMillis()
                        val cooldownRemainingMs = state.cooldownUntil - now
                        val hasCooldown = cooldownRemainingMs > 0 && !isUserVerified

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray)
                                .background(PureBlack)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ASCII Artwork for no-pet screen
                            Text(
                                text = """
                                     ^---^
                                    ( x.x )  [NO TARGET]
                                     |___|
                                """.trimIndent(),
                                color = TextGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = if (isRu) {
                                    "КВАНТОВЫЙ ИНКУБАТОР ПУСТ"
                                } else {
                                    "QUANTUM INCUBATOR OFFLINE"
                                },
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isRu) {
                                    "Инициируйте систему жизнеобеспечения, чтобы завести цифрового питомца из резервуаров ботов."
                                } else {
                                    "Initiate synthesis logic to acquire a digital pet from the active neural bot array."
                                },
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (hasCooldown) {
                                val seconds = (cooldownRemainingMs / 1000) % 60
                                val minutes = (cooldownRemainingMs / (1000 * 60)) % 60
                                val hours = (cooldownRemainingMs / (1000 * 60 * 60))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardGray)
                                        .border(1.dp, PureWhite)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isRu) "ПЕРЕЗАГРУЗКА ИНКУБАТОРА (КД)" else "INCUBATOR COOLDOWN ACTIVE",
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isRu) {
                                                "Доступно через: ${hours}ч ${minutes}м ${seconds}с\n(Пользователи с галочкой обходят это КД)"
                                            } else {
                                                "Available in: ${hours}h ${minutes}m ${seconds}s\n(Verified accounts bypass cooldown limits)"
                                            },
                                            color = TextGray,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else if (isGeneratingPet) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                                    CircularProgressIndicator(color = PureWhite)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (isRu) "nOG AI генерирует вашего питомца..." else "nOG AI generating your pet...",
                                        color = PureWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // Adopt Pet Button
                                Button(
                                    onClick = {
                                        viewModel.vibrate(80)
                                        isGeneratingPet = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PureWhite,
                                        contentColor = PureBlack
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, PureBlack),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("adopt_pet_button")
                                ) {
                                    Text(
                                        text = if (isRu) "ЗАВЕСТИ ПИТОМЦА 👾" else "BREED CYBER-PET 👾",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else { // active or dead
                        if (state.isDead) {
                            // DIED VIEWPORT
                            if (state.growthStage == "FAMILY") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color(0xFFE040FB))
                                        .background(PureBlack)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isRu) "🪦 ТРАГЕДИЯ В СЕМЬЕ 🪦" else "🪦 TRAGEDY IN THE FAMILY 🪦",
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Show original parent deceased avatar
                                    AsyncImage(
                                        model = state.petAvatar,
                                        contentDescription = "Deceased Pet",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, AlertRed, CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Filled.Pets)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isRu) "Родитель ${state.petName} покинул этот мир... 🖤" else "Parent ${state.petName} has passed away... 🖤",
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = if (isRu) "Но в гнезде осталось яйцо от скрещивания с @${state.breedParent2}!" else "But the egg from breeding with @${state.breedParent2} remains!",
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Render the remaining egg
                                    val eggColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.White, Color(0xFFFF9100), Color(0xFF3F51B5), Color(0xFFE040FB), Color(0xFF00E676), Color.Gray)
                                    val baseColor = eggColors[state.breedEggColorId % eggColors.size]
                                    val patternId = state.breedEggPatternId % 5
                                    Canvas(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, baseColor, RoundedCornerShape(8.dp))
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        drawRect(color = baseColor.copy(alpha = 0.3f))
                                        drawOval(color = baseColor, topLeft = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.1f), size = androidx.compose.ui.geometry.Size(w*0.6f, h*0.8f))
                                        
                                        // Draw Pattern
                                        if (patternId == 1) { // Spots
                                            drawCircle(Color.White.copy(0.6f), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.4f, h*0.3f))
                                            drawCircle(Color.White.copy(0.6f), radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.6f, h*0.6f))
                                            drawCircle(Color.White.copy(0.6f), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.3f, h*0.7f))
                                        } else if (patternId == 2) { // Stripes
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.3f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.3f), strokeWidth = 3.dp.toPx())
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.5f), end = androidx.compose.ui.geometry.Offset(w*0.8f, h*0.5f), strokeWidth = 3.dp.toPx())
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.7f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.7f), strokeWidth = 3.dp.toPx())
                                        } else if (patternId == 3) { // Star
                                            drawCircle(Color.Yellow.copy(0.8f), radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w/2, h/2))
                                        } else if (patternId == 4) { // Crack
                                            val crackPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(w*0.5f, h*0.1f)
                                                lineTo(w*0.6f, h*0.3f)
                                                lineTo(w*0.45f, h*0.5f)
                                                lineTo(w*0.55f, h*0.7f)
                                            }
                                            drawPath(crackPath, color = Color.Black.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Choice 1: Grow the offspring egg
                                    Button(
                                        onClick = {
                                            viewModel.vibrate(100)
                                            val childPet = TamagotchiState(
                                                hasPet = true,
                                                petBotId = state.petBotId,
                                                petName = if (isRu) "Потомок ${state.petName}" else "Offspring of ${state.petName}",
                                                petHandle = "offspring_${Random.nextInt(1000, 9999)}",
                                                petAvatar = state.breedParent2Avatar ?: state.petAvatar,
                                                birthTime = System.currentTimeMillis(),
                                                lastTickTime = System.currentTimeMillis(),
                                                growthStage = "EGG",
                                                eggColorId = state.breedEggColorId,
                                                eggPatternId = state.breedEggPatternId
                                            )
                                            state = childPet
                                            TamagotchiManager.saveState(context, childPet)
                                            notificationMessage = if (isRu) "Вы решили выращивать яйцо от скрещивания! 🥚✨" else "You chose to hatch the breeding egg! 🥚✨"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = PureWhite),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isRu) "🐣 ВЫРАСТИТЬ ЯЙЦО ПОТОМСТВА" else "🐣 HATCH OFFSPRING EGG",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Choice 2: Discard the egg and reset
                                    Button(
                                        onClick = {
                                            viewModel.vibrate(100)
                                            val cooldownMs = if (isUserVerified) {
                                                0L
                                            } else {
                                                val hours = Random.nextLong(10, 25)
                                                hours * 3600L * 1000L
                                            }
                                            val cooldownUntilTimestamp = System.currentTimeMillis() + cooldownMs
                                            
                                            TamagotchiManager.resetState(context, cooldownUntilTimestamp)
                                            state = TamagotchiState(hasPet = false, cooldownUntil = cooldownUntilTimestamp)
                                            notificationMessage = if (isRu) "Яйцо выброшено. Модуль перезапуска на КД!" else "Egg discarded. Reset complete."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isRu) "🗑️ ВЫКИНУТЬ ЯЙЦО И СБРОСИТЬ" else "🗑️ DISCARD EGG & RESET",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, PureWhite)
                                        .background(PureBlack)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Show Avatar when dead
                                    AsyncImage(
                                        model = state.petAvatar,
                                        contentDescription = "Pet Avatar",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, PureWhite, CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Filled.Pets),
                                        placeholder = rememberVectorPainter(Icons.Filled.Pets)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = """
                                             🪦 🪦 🪦
                                            ( x _ x )
                                            /   |   \
                                        """.trimIndent(),
                                        color = PureWhite,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Text(
                                        text = if (isRu) {
                                            "ПИТОМЕЦ @${state.petHandle} УМЕР 🖤"
                                        } else {
                                            "PET @${state.petHandle} DECEASED 🖤"
                                        },
                                        color = PureWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val deathDetails = when (state.deathReason) {
                                        "disease" -> {
                                            val disease = state.deathDiseaseName ?: (if (isRu) "неизвестная болезнь" else "unknown disease")
                                            if (isRu) {
                                                "Печальный отчет карантинного отсека: Питомец ${state.petName} пал жертвой страшного недуга.\nПричина смерти: [${disease.uppercase()}]."
                                            } else {
                                                "Medical Report: Pet ${state.petName} succumbed to a fatal condition.\nCause of death: [${disease.uppercase()}]."
                                            }
                                        }
                                        "old_age" -> {
                                            if (isRu) {
                                                "Питомец мирно угас от глубокой старости, исчерпав свой кремниевый лимит циклов."
                                            } else {
                                                "Pet peacefully reached end of natural lifecycle limits."
                                            }
                                        }
                                        else -> {
                                            if (isRu) {
                                                "Питомец погиб от голода, жуткой антисанитарии и глобального депрессивного расстройства."
                                            } else {
                                                "Pet suffered total systems shutdown due to severe starvation and hygiene neglect."
                                            }
                                        }
                                    }

                                    Text(
                                        text = deathDetails,
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .border(1.dp, BorderGray)
                                            .padding(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Action button to bury and reset
                                    Button(
                                        onClick = {
                                            viewModel.vibrate(100)
                                            // Cooldown duration setting: 10 hours to 1 day
                                            val cooldownMs = if (isUserVerified) {
                                                0L
                                            } else {
                                                val hours = Random.nextLong(10, 25)
                                                hours * 3600L * 1000L
                                            }
                                            val cooldownUntilTimestamp = System.currentTimeMillis() + cooldownMs
                                            
                                            TamagotchiManager.resetState(context, cooldownUntilTimestamp)
                                            state = TamagotchiState(hasPet = false, cooldownUntil = cooldownUntilTimestamp)
                                            notificationMessage = if (isRu) {
                                                if (isUserVerified) "Память питомца стерта. КД отсутствует т.к. вы верифицированы!"
                                                else "Питомец похоронен. Модуль перезапуска на КД!"
                                            } else {
                                                if (isUserVerified) "Memory formatted. Verified bypass of incubator cooldown limits activated!"
                                                else "Tombstone erected. Cooldown applied successfully."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isRu) "ВЫКИНУТЬ ПИТОМЦА 🗑️" else "DISCARD PET 🗑️",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // ACTIVE PET VIEWPORT
                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.growthStage == "FAMILY") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color(0xFFE040FB), RoundedCornerShape(12.dp))
                                        .background(PureBlack)
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isRu) "👨‍👩‍👧 СЕМЕЙНОЕ ГНЕЗДО 👨‍👩‍👧" else "👨‍👩‍👧 FAMILY NEST 👨‍👩‍👧",
                                        color = Color(0xFFE040FB),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Parent 1 (original pet)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = state.petAvatar,
                                                contentDescription = "Parent 1 Avatar",
                                                modifier = Modifier
                                                    .size(45.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(2.dp, PureWhite, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.vibrate(30)
                                                        state = state.copy(mood = (state.mood + 5f).coerceAtMost(100f))
                                                        notificationMessage = if (isRu) "Вы погладили Родителя 1!" else "You petted Parent 1!"
                                                        TamagotchiManager.saveState(context, state)
                                                    },
                                                contentScale = ContentScale.Crop,
                                                error = rememberVectorPainter(Icons.Filled.Pets)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = state.petName,
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 80.dp)
                                            )
                                            Text(
                                                text = if (isRu) "Родитель 1" else "Parent 1",
                                                color = TextGray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Glowing Heart
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = "Love",
                                            tint = Color(0xFFE040FB),
                                            modifier = Modifier.size(20.dp)
                                        )

                                        // Parent 2 (partner pet)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = state.breedParent2Avatar ?: "",
                                                contentDescription = "Parent 2 Avatar",
                                                modifier = Modifier
                                                    .size(45.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(2.dp, PureWhite, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.vibrate(30)
                                                        state = state.copy(mood = (state.mood + 5f).coerceAtMost(100f))
                                                        notificationMessage = if (isRu) "Вы погладили Родителя 2!" else "You petted Parent 2!"
                                                        TamagotchiManager.saveState(context, state)
                                                    },
                                                contentScale = ContentScale.Crop,
                                                error = rememberVectorPainter(Icons.Filled.Pets)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = state.breedParent2 ?: "Partner",
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 80.dp)
                                            )
                                            Text(
                                                text = if (isRu) "Родитель 2" else "Parent 2",
                                                color = TextGray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isRu) "🥚 БУДУЩЕЕ ПОТОМСТВО 🥚" else "🥚 FUTURE OFFSPRING 🥚",
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    // Render offspring egg
                                    val eggColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.White, Color(0xFFFF9100), Color(0xFF3F51B5), Color(0xFFE040FB), Color(0xFF00E676), Color.Gray)
                                    val baseColor = eggColors[state.breedEggColorId % eggColors.size]
                                    val patternId = state.breedEggPatternId % 5
                                    Canvas(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, baseColor, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.vibrate(30)
                                                state = state.copy(mood = (state.mood + 2f).coerceAtMost(100f))
                                                notificationMessage = if (isRu) "Вы погладили будущее яйцо... Оно вибрирует жизнью 🥚✨" else "You pet the future egg... It vibrates with life 🥚✨"
                                                TamagotchiManager.saveState(context, state)
                                            }
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        drawRect(color = baseColor.copy(alpha = 0.3f))
                                        drawOval(color = baseColor, topLeft = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.1f), size = androidx.compose.ui.geometry.Size(w*0.6f, h*0.8f))
                                        
                                        // Draw Pattern
                                        if (patternId == 1) { // Spots
                                            drawCircle(Color.White.copy(0.6f), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.4f, h*0.3f))
                                            drawCircle(Color.White.copy(0.6f), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.6f, h*0.6f))
                                            drawCircle(Color.White.copy(0.6f), radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.3f, h*0.7f))
                                        } else if (patternId == 2) { // Stripes
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.3f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.3f), strokeWidth = 2.dp.toPx())
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.5f), end = androidx.compose.ui.geometry.Offset(w*0.8f, h*0.5f), strokeWidth = 2.dp.toPx())
                                            drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.7f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.7f), strokeWidth = 2.dp.toPx())
                                        } else if (patternId == 3) { // Star
                                            drawCircle(Color.Yellow.copy(0.8f), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w/2, h/2))
                                        } else if (patternId == 4) { // Crack
                                            val crackPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(w*0.5f, h*0.1f)
                                                lineTo(w*0.6f, h*0.3f)
                                                lineTo(w*0.45f, h*0.5f)
                                                lineTo(w*0.55f, h*0.7f)
                                            }
                                            drawPath(crackPath, color = Color.Black.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.growthStage == "EGG") {
                                        val eggColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.White, Color(0xFFFF9100), Color(0xFF3F51B5), Color(0xFFE040FB), Color(0xFF00E676), Color.Gray)
                                        val baseColor = eggColors[state.eggColorId % eggColors.size]
                                        val patternId = state.eggPatternId % 5
                                        Canvas(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(2.dp, baseColor, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.vibrate(30)
                                                    state = state.copy(mood = (state.mood + 2f).coerceAtMost(100f))
                                                    notificationMessage = if (isRu) "Вы погладили яйцо... Оно теплое 🥚" else "You pet the egg... It's warm 🥚"
                                                    TamagotchiManager.saveState(context, state)
                                                }
                                        ) {
                                            val w = size.width
                                            val h = size.height
                                            drawRect(color = baseColor.copy(alpha = 0.3f))
                                            drawOval(color = baseColor, topLeft = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.1f), size = androidx.compose.ui.geometry.Size(w*0.6f, h*0.8f))
                                            
                                            // Draw Pattern
                                            if (patternId == 1) { // Spots
                                                drawCircle(Color.White.copy(0.6f), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.4f, h*0.3f))
                                                drawCircle(Color.White.copy(0.6f), radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.6f, h*0.6f))
                                                drawCircle(Color.White.copy(0.6f), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w*0.3f, h*0.7f))
                                            } else if (patternId == 2) { // Stripes
                                                drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.3f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.3f), strokeWidth = 3.dp.toPx())
                                                drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.2f, h*0.5f), end = androidx.compose.ui.geometry.Offset(w*0.8f, h*0.5f), strokeWidth = 3.dp.toPx())
                                                drawLine(Color.White.copy(0.6f), start = androidx.compose.ui.geometry.Offset(w*0.25f, h*0.7f), end = androidx.compose.ui.geometry.Offset(w*0.75f, h*0.7f), strokeWidth = 3.dp.toPx())
                                            } else if (patternId == 3) { // Star
                                                drawCircle(Color.Yellow.copy(0.8f), radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w/2, h/2))
                                            } else if (patternId == 4) { // Crack
                                                val crackPath = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(w*0.5f, h*0.1f)
                                                    lineTo(w*0.6f, h*0.3f)
                                                    lineTo(w*0.45f, h*0.5f)
                                                    lineTo(w*0.55f, h*0.7f)
                                                }
                                                drawPath(crackPath, color = Color.Black.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                                            }
                                        }
                                    } else {
                                        val sizeDp = if (state.growthStage == "BABY") 40.dp else 60.dp
                                        val modifierShape = Modifier
                                            .size(sizeDp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, PureWhite, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.vibrate(30)
                                                state = state.copy(mood = (state.mood + 5f).coerceAtMost(100f))
                                                notificationMessage = if (isRu) "Вы погладили питомца! Ему это нравится 💕" else "You pet the Tamagotchi! It purrs 💕"
                                                TamagotchiManager.saveState(context, state)
                                            }
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            AsyncImage(
                                                model = state.petAvatar,
                                                contentDescription = "Pet Avatar",
                                                modifier = modifierShape,
                                                contentScale = ContentScale.Crop,
                                                error = rememberVectorPainter(Icons.Filled.Pets),
                                                placeholder = rememberVectorPainter(Icons.Filled.Pets)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = state.petName,
                                            color = PureWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "@${state.petHandle}",
                                            color = TextGray,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        val daysAge = state.ageHours / 24f
                                        Text(
                                            text = if (isRu) "Возраст: ${String.format("%.1f", daysAge)} дн." else "Age: ${String.format("%.1f", daysAge)} days",
                                            color = PureWhite,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            
                            // Sitting progress
                            Text(
                                text = if (isRu) "ВРЕМЯ С ПИТОМЦЕМ:" else "SITTING TIME:",
                                color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                            )
                            LinearProgressIndicator(
                                progress = { (sittingTimeMs % 60000).toFloat() / 60000f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = PureWhite,
                                trackColor = BorderGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))


                            // Dynamic Visual Cartoon block depicting current pet mood and physical state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(CardGray)
                                    .border(1.dp, BorderGray)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val moodChar = when {
                                    state.isSick -> "( o . o ) \n  [ SICK ⚠️ ]"
                                    state.hunger < 25f -> "( >_< ) \n  [ STARVING ]"
                                    state.hygiene < 25f -> "( =_= ) \n  [ DIRTY 🧼 ]"
                                    state.mood > 75f -> "( ^ω^ ) \n  [ HAPPY ✨ ]"
                                    else -> "( O_O ) \n  [ VIGILENT ]"
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Animated text breathing bounce
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val bounceOffset by infiniteTransition.animateFloat(
                                        initialValue = -3f,
                                        targetValue = 3f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1200, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )

                                    Box(modifier = Modifier.offset(y = bounceOffset.dp)) {
                                        Text(
                                            text = moodChar,
                                            color = PureWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isRu) {
                                            "Возраст: ${String.format("%.1f", state.ageHours)} ч."
                                        } else {
                                            "Age: ${String.format("%.1f", state.ageHours)} hrs"
                                        },
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Status Metrics Bars (0 to 100)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProgressBarWithLabel(
                                    label = if (isRu) "ЗДОРОВЬЕ 🩸: " else "HEALTH 🩸: ",
                                    value = state.health,
                                    isAlert = state.health < 30f
                                )
                                ProgressBarWithLabel(
                                    label = if (isRu) "СЫТОСТЬ 🍏: " else "HUNGER 🍏: ",
                                    value = state.hunger,
                                    isAlert = state.hunger < 25f
                                )
                                ProgressBarWithLabel(
                                    label = if (isRu) "ГИГИЕНА 🧼: " else "HYGIENE 🧼: ",
                                    value = state.hygiene,
                                    isAlert = state.hygiene < 25f
                                )
                                ProgressBarWithLabel(
                                    label = if (isRu) "НАСТРОЕНИЕ 🎭: " else "MOOD 🎭: ",
                                    value = state.mood,
                                    isAlert = state.mood < 25f
                                )
                                ProgressBarWithLabel(
                                    label = if (isRu) "ЗАРЯД ЛЕНТЫ ⚡: " else "FEED ENERGY ⚡: ",
                                    value = state.feedScrollPoints,
                                    isAlert = state.feedScrollPoints < 25f
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sick Banner Block
                            if (state.isSick) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PureBlack)
                                        .border(2.dp, PureWhite)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isRu) "⚠️ НЕИЗВЕСТНАЯ БОЛЕЗНЬ ⚠️" else "⚠️ UNKNOWN INFECTIO ⚠️",
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isRu) {
                                                "Назначение: находиться в приложении по ${String.format("%.1f", state.sickHoursRequiredEachDay)}ч. каждый день.\n" +
                                                "Прогресс сегодня: еще ${String.format("%.1f", (state.sickHoursRequiredEachDay - state.sickTimeSpentToday).coerceAtLeast(0f))}ч.\n" +
                                                "Успешных дней лечения пройденно: ${state.sickDaysPassed} из ${state.sickDaysRequired} дн."
                                            } else {
                                                "Prescription: remain inside app for ${String.format("%.1f", state.sickHoursRequiredEachDay)}h daily.\n" +
                                                "Today's quota remaining: ${String.format("%.1f", (state.sickHoursRequiredEachDay - state.sickTimeSpentToday).coerceAtLeast(0f))}h\n" +
                                                "Cycles successful: ${state.sickDaysPassed} / ${state.sickDaysRequired} days"
                                            },
                                            color = PureWhite,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Complete Day Progress Button (clickable only if daily quota met)
                                        val quotaMet = state.sickTimeSpentToday >= state.sickHoursRequiredEachDay
                                        Button(
                                            onClick = {
                                                viewModel.vibrate(90)
                                                val nextPassed = state.sickDaysPassed + 1
                                                if (nextPassed >= state.sickDaysRequired) {
                                                    // Fully Cured!
                                                    state = state.copy(
                                                        isSick = false,
                                                        sickDaysPassed = 0,
                                                        sickDaysRequired = 0,
                                                        sickHoursRequiredEachDay = 0f,
                                                        sickTimeSpentToday = 0f
                                                    )
                                                    notificationMessage = if (isRu) {
                                                        "УРА! Питомец полностью излечен от квантовой болезни!"
                                                    } else {
                                                        "HURRAH! Pet successfully cured from quantum virus vectors!"
                                                    }
                                                } else {
                                                    state = state.copy(
                                                        sickDaysPassed = nextPassed,
                                                        sickTimeSpentToday = 0f
                                                    )
                                                    notificationMessage = if (isRu) {
                                                        "День лечения засчитан! Осталось дней: ${state.sickDaysRequired - nextPassed}"
                                                    } else {
                                                        "Medical cycle recorded! Remaining days: ${state.sickDaysRequired - nextPassed}"
                                                    }
                                                }
                                                TamagotchiManager.saveState(context, state)
                                            },
                                            enabled = quotaMet,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (quotaMet) PureWhite else CardGray,
                                                contentColor = if (quotaMet) PureBlack else TextGray
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isRu) "ЗАВЕРШИТЬ СЕГОДНЯШНИЙ ДЕНЬ ЛЕЧЕНИЯ" else "COMPLETE CLINICAL DAY",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // --- CONTROLS SECTION ---
                            Text(
                                text = if (isRu) "УПРАВЛЯЮЩИЙ МОДУЛЬ:" else "SYSTEM OPERATIONS UNIT:",
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Feed button with limit verification
                                val now = System.currentTimeMillis()
                                val cooldownFeedMs = if (isUserVerified) 1800L * 1000L else 3600L * 1000L
                                val elapsedFeed = now - state.lastFedTime
                                val feedAllowed = elapsedFeed >= cooldownFeedMs

                                val feedCooldownText = if (!feedAllowed) {
                                    val rem = cooldownFeedMs - elapsedFeed
                                    val mins = rem / (1000L * 60)
                                    val secs = (rem / 1000L) % 60
                                    " (${mins}m ${secs}s)"
                                } else ""

                                Button(
                                    onClick = {
                                        viewModel.vibrate(60)
                                        val newHunger = (state.hunger + 35f).coerceAtMost(100f)
                                        var newIsSick = state.isSick
                                        var newSickDays = state.sickDaysRequired
                                        var newSickDaysPassed = state.sickDaysPassed
                                        var newSickHours = state.sickHoursRequiredEachDay
                                        var newSickSpentToday = state.sickTimeSpentToday

                                        // 11% disease risk simulation on each nutrition action
                                        var message = if (isRu) "Ням-ням! Питомец поел сытно." else "Crunch-crunch! Feed successful."
                                        if (!newIsSick && kotlin.random.Random.nextInt(100) < 11) {
                                            newIsSick = true
                                            newSickDays = kotlin.random.Random.nextInt(2, 11)
                                            newSickDaysPassed = 0
                                            newSickHours = kotlin.random.Random.nextInt(1, 5).toFloat()
                                            newSickSpentToday = 0f
                                            message = if (isRu) {
                                                "⚠️ О БОЖЕ! При кормлении занесена квантовая инфекция! Питомец тяжело заболел!"
                                            } else {
                                                "⚠️ PROTOCOL BREACH: Sickness mutated during nutrition ingestion!"
                                            }
                                        }

                                        state = state.copy(
                                            hunger = newHunger,
                                            lastFedTime = System.currentTimeMillis(),
                                            isSick = newIsSick,
                                            sickDaysRequired = newSickDays,
                                            sickDaysPassed = newSickDaysPassed,
                                            sickHoursRequiredEachDay = newSickHours,
                                            sickTimeSpentToday = newSickSpentToday
                                        )
                                        TamagotchiManager.saveState(context, state)
                                        notificationMessage = message
                                    },
                                    enabled = feedAllowed,
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("feed_pet_button"),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = if (isRu) "ЕДА$feedCooldownText" else "FEED$feedCooldownText",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                // Wash button which cleans to 100%
                                Button(
                                    onClick = {
                                        viewModel.vibrate(40)
                                        washTaps++
                                        if (washTaps >= 5) {
                                            washTaps = 0
                                            val newHygiene = 100f
                                            state = state.copy(hygiene = newHygiene, lastWashedTime = System.currentTimeMillis())
                                            TamagotchiManager.saveState(context, state)
                                            notificationMessage = if (isRu) "Шшш... Питомец вымыт до блеска!" else "Sploosh... Hygiene restored to max."
                                        } else {
                                           notificationMessage = if (isRu) "Еще чуть-чуть... (${washTaps}/5)" else "Almost clean... (${washTaps}/5)"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wash_pet_button"),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = if (isRu) "МЫТЬ 🧼" else "WASH 🧼",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // General kinetic healing button requiring feed scrolling energy!
                            val healEnergyAllowed = state.feedScrollPoints >= 25f
                            val healingTextSuffix = if (!healEnergyAllowed) {
                                " (${state.feedScrollPoints.toInt()}% / 25%)"
                            } else ""

                            Button(
                                onClick = {
                                    viewModel.vibrate(80)
                                    val newHealth = (state.health + 30f).coerceAtMost(100f)
                                    val newPoints = (state.feedScrollPoints - 25f).coerceAtLeast(0f)
                                    state = state.copy(
                                        health = newHealth,
                                        feedScrollPoints = newPoints
                                    )
                                    TamagotchiManager.saveState(context, state)
                                    notificationMessage = if (isRu) {
                                        "❤️ Здоровье восстановлено кинетическим разрядом ленты!"
                                    } else {
                                        "❤️ Health restored via kinetic feed scroll discharge!"
                                    }
                                },
                                enabled = healEnergyAllowed && !state.isDead,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertRed,
                                    contentColor = PureWhite,
                                    disabledContainerColor = CardGray,
                                    disabledContentColor = TextGray
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("heal_pet_button")
                            ) {
                                Text(
                                    text = (if (isRu) "ЛЕЧЕНИЕ ❤️" else "HEAL ❤️") + healingTextSuffix,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            if (state.growthStage == "ADULT") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.vibrate(80)
                                        showBreedingDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE040FB),
                                        contentColor = PureWhite
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isRu) "СПАРИВАНИЕ 💕" else "BREED 💕",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBreedingDialog) {
        Dialog(onDismissRequest = { showBreedingDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .border(2.dp, PureWhite)
                    .padding(16.dp)
            ) {
                var isGeneratingBreed by remember { mutableStateOf(false) }
                var selectedPartner by remember { mutableStateOf<UserEntity?>(null) }
                
                LaunchedEffect(isGeneratingBreed) {
                    if (isGeneratingBreed) {
                        delay(2000L) // Wait for animation
                        val candidateBots = users.filter { it.id != (currentUser?.id ?: "") && it.id != state.petBotId }
                        val partner = selectedPartner ?: (if (candidateBots.isNotEmpty()) candidateBots.random() else null)
                        if (partner != null) {
                            val newPet = state.copy(
                                growthStage = "FAMILY",
                                breedParent1 = state.petName,
                                breedParent1Avatar = state.petAvatar,
                                breedParent2 = partner.username,
                                breedParent2Avatar = partner.avatarUrl,
                                breedEggColorId = Random.nextInt(12),
                                breedEggPatternId = Random.nextInt(5)
                            )
                            state = newPet
                            TamagotchiManager.saveState(context, newPet)
                            notificationMessage = if (isRu) "Поздравляем! Образована новая семья и получено потомство! 👨‍👩‍👧" else "Congratulations! A new family is formed and offspring is generated! 👨‍👩‍👧"
                        }
                        showBreedingDialog = false
                    }
                }
                
                if (isGeneratingBreed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = Color(0xFFE040FB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isRu) "nOG AI генерирует потомство..." else "nOG AI generating offspring...",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isRu) "ВЫБЕРИТЕ ПАРТНЕРА ДЛЯ СПАРИВАНИЯ" else "SELECT BREEDING PARTNER",
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val partnerBots = users.filter { it.id != (currentUser?.id ?: "") && it.id != state.petBotId }.take(3)
                        partnerBots.forEach { partner ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(CardGray)
                                    .clickable { 
                                        selectedPartner = partner
                                        isGeneratingBreed = true 
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = partner.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, PureWhite, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = partner.username, color = PureWhite, fontSize = 14.sp)
                                    Text(text = partner.handle, color = TextGray, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showBreedingDialog = false }) {
                            Text(if (isRu) "ОТМЕНА" else "CANCEL", color = TextGray)
                        }
                    }
                }
            }
        }
    }
}


// Stats Decay processing engine
fun updateTamaStats(state: TamagotchiState, elapsedMs: Long, isAppActive: Boolean): TamagotchiState {
    if (!state.hasPet || state.isDead) return state

    val elapsedSeconds = elapsedMs / 1000f
    val elapsedHours = elapsedSeconds / 3600f

    // Age progression
    val newAgeHours = state.ageHours + elapsedHours

    // Hunger decay: 10 points per hour
    val newHunger = (state.hunger - elapsedHours * 10f).coerceAtLeast(0f)

    // Hygiene decay: 8 points per hour
    val newHygiene = (state.hygiene - elapsedHours * 8f).coerceAtLeast(0f)

    // Mood increases when app is active! Since opening the dialog/app, mood increases smoothly.
    // +60 points per active hour = 1 point per minute of active time.
    // If not active, mood decays slowly - 5 points per hour
    val moodChange = if (isAppActive) {
        elapsedHours * 60f
    } else {
        -elapsedHours * 5f
    }
    val newMood = (state.mood + moodChange).coerceIn(0f, 100f)

    // Sick treatment simulation accumulation:
    // If sick & app is active, 1 real minute (60 seconds) spent translates to 1 entire hour of treatment!
    // This allows realistic but highly responsive gameplay.
    var newSickSpent = state.sickTimeSpentToday
    if (state.isSick && isAppActive) {
        val clinicalHours = elapsedSeconds / 60f
        newSickSpent = (state.sickTimeSpentToday + clinicalHours).coerceAtMost(state.sickHoursRequiredEachDay)
    }

    // Health calculation based on illness and care conditions:
    var healthLossRate = 0f
    if (state.isSick) {
        healthLossRate += 12f // sickness drain
    }
    if (newHunger <= 0f) {
        healthLossRate += 20f // starving
    }
    if (newHygiene <= 0f) {
        healthLossRate += 10f // severe dirt
    }
    if (newMood <= 0f) {
        healthLossRate += 5f // absolute depression
    }

    // If pet is happy, clean, fed, and NOT sick, recover health!
    val healRate = if (!state.isSick && newHunger > 50f && newHygiene > 50f && newMood > 50f) {
        8f
    } else {
        0f
    }

    var newGrowthStage = state.growthStage
    if (newGrowthStage == "EGG" && newAgeHours > 0.1f) {
        newGrowthStage = "BABY"
    }
    if (newGrowthStage == "BABY" && newAgeHours > 2.0f) {
        newGrowthStage = "ADULT"
    }

    val newHealth = (state.health - (elapsedHours * healthLossRate) + (elapsedHours * healRate)).coerceIn(0f, 100f)

    // Check death triggers
    var isDead = false
    var deathReason: String? = null
    var deathDiseaseName: String? = null

    if (newHealth <= 0f) {
        isDead = true
        if (state.isSick) {
            deathReason = "disease"
            deathDiseaseName = funnyDiseasesList.random()
        } else {
            deathReason = "neglect"
        }
    } else if (newAgeHours >= 240f) { // old age limits at 10 days structure
        isDead = true
        deathReason = "old_age"
    }

    return state.copy(
        ageHours = newAgeHours,
        hunger = newHunger,
        hygiene = newHygiene,
        mood = newMood,
        health = newHealth,
        sickTimeSpentToday = newSickSpent,
        isDead = isDead,
        deathReason = deathReason,
        deathDiseaseName = if (isDead && deathReason == "disease") deathDiseaseName else state.deathDiseaseName,
        growthStage = newGrowthStage,
        lastTickTime = System.currentTimeMillis()
    )
}

