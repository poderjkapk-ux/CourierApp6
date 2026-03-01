package com.restify.courierapp

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Locale

// ==========================================
// 0. ДИЗАЙН-СИСТЕМА (Кольори та Кастомні Компоненти)
// ==========================================

object AppColors {
    val Primary = Color(0xFF1E293B) // Глибокий преміальний темний (Dark Slate)
    val PrimaryDark = Color(0xFF0F172A)
    val Secondary = Color(0xFF10B981) // Соковитий смарагдовий
    val Background = Color(0xFFF8FAFC) // Чистий світло-сірий фон
    val Surface = Color.White
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val Error = Color(0xFFEF4444)
    val ChatBubbleSelf = Color(0xFF1E293B)
    val ChatTextSelf = Color.White
    val ChatBubbleOther = Color(0xFFF1F5F9)
    val ChatTextOther = Color(0xFF0F172A)
    val Warning = Color(0xFFF59E0B)
    val Inactive = Color(0xFFCBD5E1)
}

// Кастомна кнопка з кольором
@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (enabled) 6.dp else 0.dp, pressedElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// Кастомне поле вводу
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppColors.TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = AppColors.Primary,
            cursorColor = AppColors.Primary,
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Background.copy(alpha = 0.5f)
        ),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true
    )
}

// Елемент списку з іконкою для адрес
@Composable
fun AddressItem(icon: ImageVector, text: String, label: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            if (label != null) {
                Text(label, fontSize = 13.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
            }
            Text(text, fontSize = 16.sp, color = AppColors.TextPrimary, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ==========================================
// КОМПОНЕНТИ ТАЙМЕРІВ ТА ЧАСУ
// ==========================================

@Composable
fun CurrentTimeDisplay() {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = format.format(Date())
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .background(AppColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = currentTime,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.Primary
        )
    }
}

@Composable
fun StepTimer(
    startTimeIso: String?,
    endTimeIso: String?,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (startTimeIso.isNullOrEmpty()) return

    var durationText by remember { mutableStateOf("00:00") }

    LaunchedEffect(startTimeIso, endTimeIso, isActive) {
        while (true) {
            try {
                val start = Instant.parse(startTimeIso)
                val end = if (!endTimeIso.isNullOrEmpty()) {
                    Instant.parse(endTimeIso)
                } else if (isActive) {
                    Instant.now()
                } else {
                    null
                }

                if (end != null) {
                    val duration = Duration.between(start, end)
                    val s = duration.seconds
                    if (s >= 0) {
                        val m = s / 60
                        val h = m / 60
                        durationText = if (h > 0) {
                            String.format("%02d:%02d:%02d", h, m % 60, s % 60)
                        } else {
                            String.format("%02d:%02d", m, s % 60)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ігноруємо помилки парсингу дат
            }

            if (!endTimeIso.isNullOrEmpty() || !isActive) {
                break // Зупиняємо "тікання", якщо час зафіксований
            }
            delay(1000L)
        }
    }

    val backgroundColor = if (!endTimeIso.isNullOrEmpty()) {
        AppColors.Secondary.copy(alpha = 0.15f)
    } else if (isActive) {
        AppColors.Primary.copy(alpha = 0.15f)
    } else {
        Color.LightGray.copy(alpha = 0.2f)
    }

    val textColor = if (!endTimeIso.isNullOrEmpty()) AppColors.Secondary else if (isActive) AppColors.Primary else AppColors.TextSecondary

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⏱ $durationText",
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}


// ==========================================
// 1. ЕКРАН АВТОРИЗАЦІЇ (Login Screen)
// ==========================================
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(colors = listOf(AppColors.Primary, AppColors.PrimaryDark)))
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(28.dp)
            ) {
                Text("Restify", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text("Delivery", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 220.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = AppColors.PrimaryDark.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Вхід для кур'єра", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Spacer(modifier = Modifier.height(28.dp))

                ModernTextField(value = phone, onValueChange = { phone = it }, label = "Номер телефону", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(16.dp))
                ModernTextField(value = password, onValueChange = { password = it }, label = "Пароль", visualTransformation = PasswordVisualTransformation())

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMessage, color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                ModernButton(text = "Увійти", onClick = { onLoginClick(phone, password) }, modifier = Modifier.fillMaxWidth(), isLoading = isLoading, enabled = phone.isNotBlank() && password.isNotBlank(), icon = Icons.Default.ArrowForward)
            }
        }
    }
}

// ==========================================
// 2. ЕКРАН СПИСКУ ЗАМОВЛЕНЬ (Orders List)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OpenOrder>,
    isOnline: Boolean,
    onToggleStatus: (Boolean) -> Unit,
    onAcceptOrder: (Int) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToHistory: () -> Unit,
    isLoading: Boolean
) {
    LaunchedEffect(Unit) {
        RetrofitClient.webSocketManager.messages.collect { messageJson ->
            try {
                val json = JSONObject(messageJson)
                val type = json.optString("type")
                if (type == "new_order" || type == "job_update") {
                    delay(500)
                    onRefresh()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Доступні", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background, titleContentColor = AppColors.TextPrimary),
                actions = {
                    CurrentTimeDisplay()
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.DateRange, contentDescription = "Історія", tint = AppColors.Primary)
                    }
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = if (isOnline) AppColors.Secondary.copy(alpha = 0.15f) else AppColors.TextSecondary.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(end = 20.dp).clip(RoundedCornerShape(50)).clickable { onToggleStatus(!isOnline) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(if (isOnline) AppColors.Secondary else AppColors.TextSecondary, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isOnline) "Онлайн" else "Офлайн", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isOnline) AppColors.Secondary else AppColors.TextSecondary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (orders.isEmpty() && !isLoading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Зараз немає замовлень", color = AppColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(orders) { order -> OrderCard(order = order, onAcceptClick = onAcceptOrder) }
                }
            }
        }
    }
}

// ОНОВЛЕНА КАРТКА ЗАМОВЛЕННЯ З ПЛАВНИМ РОЗГОРТАННЯМ
@Composable
fun OrderCard(order: OpenOrder, onAcceptClick: (Int) -> Unit) {
    var isAccepting by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val paymentInfo = when (order.paymentType) {
        "prepaid" -> Pair("✅ Оплачено", AppColors.Secondary)
        "cash" -> Pair("💵 Готівка", AppColors.Warning)
        "buyout" -> Pair("💰 Викуп", AppColors.Error)
        else -> Pair(order.paymentType, AppColors.Primary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clickable { expanded = !expanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // КОМПАКТНА ЧАСТИНА
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.restaurantName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!expanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.restaurantAddress, fontSize = 14.sp, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(modifier = Modifier.background(AppColors.Secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("${order.fee.toInt()} ₴", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AppColors.Secondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.background(paymentInfo.second.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(paymentInfo.first, color = paymentInfo.second, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (order.distToRest != null) {
                    Box(modifier = Modifier.background(AppColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("🏃 ~${order.distToRest} км", color = AppColors.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    tint = AppColors.TextSecondary
                )
            }

            // ДЕТАЛЬНА ЧАСТИНА (ВІДЧИНЯЄТЬСЯ)
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Інформація про ВИКУП
                if (order.paymentType == "buyout") {
                    Box(modifier = Modifier.fillMaxWidth().background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(
                            text = "Сума викупу: ${order.price} ₴ (Ви сплачуєте цю суму в закладі, а клієнт повертає її вам при доставці)",
                            color = AppColors.Error, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AddressItem(icon = Icons.Default.LocationOn, text = order.restaurantAddress, label = "Забрати")

                Box(modifier = Modifier.padding(start = 19.dp, top = 6.dp, bottom = 6.dp).height(20.dp).width(2.dp).background(Color.LightGray.copy(alpha = 0.5f)))

                AddressItem(icon = Icons.Default.Home, text = order.dropoffAddress, label = "Доставити")

                if (!order.comment.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text("Коментар: ${order.comment}", fontSize = 14.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (order.distTrip != null) {
                        Text("📍 Маршрут: ~${order.distTrip} км", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    ModernButton(
                        text = "Прийняти",
                        onClick = {
                            isAccepting = true
                            onAcceptClick(order.id)
                        },
                        modifier = Modifier.height(48.dp).width(140.dp),
                        backgroundColor = AppColors.Primary,
                        isLoading = isAccepting
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. ЕКРАН АКТИВНОГО ЗАМОВЛЕННЯ (Active Order)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrderScreen(
    job: ActiveJobDetail,
    cookie: String,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Деталі", "Чат")

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(AppColors.Surface, AppColors.Background)))
                    .shadow(elevation = 8.dp, spotColor = AppColors.Primary.copy(alpha = 0.1f))
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Замовлення #${job.id}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                            Text(
                                text = getStatusText(job.serverStatus, job.isReady),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = getStatusColor(job.serverStatus, job.isReady)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        CurrentTimeDisplay()
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.padding(end = 8.dp).background(AppColors.Primary.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Оновити", tint = AppColors.Primary)
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 4.dp,
                            color = AppColors.Primary
                        )
                    },
                    divider = { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold, fontSize = 16.sp) },
                            selectedContentColor = AppColors.Primary,
                            unselectedContentColor = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            when (selectedTabIndex) {
                0 -> OrderDetailsView(job, onArrivedPickup, onUpdateStatus, onRefresh)
                1 -> ChatView(job.id, cookie)
            }
        }
    }
}

fun getStatusText(status: String, isReady: Boolean): String {
    if (isReady) return "Замовлення готове! Можна забирати."
    return when(status) {
        "assigned" -> "Прямуйте до закладу"
        "arrived_pickup" -> "Очікуйте видачі"
        "ready" -> "Замовлення готове! Можна забирати."
        "picked_up" -> "Прямуйте до клієнта"
        "returning" -> "Повернення коштів у заклад"
        else -> status
    }
}

fun getStatusColor(status: String, isReady: Boolean): Color {
    if (isReady) return AppColors.Secondary
    return when(status) {
        "assigned", "picked_up" -> AppColors.Primary
        "ready" -> AppColors.Secondary
        "returning" -> AppColors.Warning
        else -> AppColors.TextSecondary
    }
}

// --- ПІД-ЕКРАН: ДЕТАЛІ ЗАМОВЛЕННЯ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsView(
    job: ActiveJobDetail,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var isActionLoading by remember(job.serverStatus) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RetrofitClient.webSocketManager.messages.collect { messageJson ->
            try {
                val json = JSONObject(messageJson)
                val type = json.optString("type")
                if (type == "job_ready" || type == "job_update") {
                    delay(500)
                    onRefresh()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val handleRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            onRefresh()
            delay(500)
            isRefreshing = false
        }
    }

    val openGoogleMaps = { address: String, lat: Double?, lon: Double? ->
        try {
            val uri = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                Uri.parse("google.navigation:q=$lat,$lon")
            } else {
                Uri.parse("google.navigation:q=${Uri.encode(address)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(address)}")))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Помилка відкриття карт", Toast.LENGTH_SHORT).show()
        }
    }

    val isStep1Active = job.serverStatus in listOf("assigned", "arrived_pickup", "ready")
    val isStep1Done = job.serverStatus in listOf("picked_up", "delivered", "returning")
    val isStep2Active = job.serverStatus == "picked_up"
    val isStep2Done = job.serverStatus in listOf("delivered", "returning")

    val isOrderReady = job.isReady || job.serverStatus == "ready"

    Column(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = handleRefresh,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {

                // --- НОВИЙ БЛОК: КРАСИВЕ СПОВІЩЕННЯ ПРО ГОТОВНІСТЬ ЗАМОВЛЕННЯ ---
                if (isOrderReady) {
                    item {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = AppColors.Secondary.copy(alpha = 0.4f))
                            .background(Brush.horizontalGradient(listOf(AppColors.Secondary, Color(0xFF059669))), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("ЗАМОВЛЕННЯ ГОТОВЕ!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Можете забирати пакунок у закладі.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // --- ФІНАНСИ ТА ОПЛАТА ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("ВАШ ДОХІД", color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${job.deliveryFee} ₴", fontWeight = FontWeight.Black, color = AppColors.Secondary, fontSize = 28.sp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("СУМА ЗАМОВЛЕННЯ", color = AppColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${job.orderPrice} ₴", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AppColors.TextPrimary)
                                }
                            }

                            val paymentInfo = when (job.paymentType) {
                                "prepaid" -> Pair("✅ ОПЛАЧЕНО (Гроші не беремо)", AppColors.Secondary)
                                "cash" -> Pair("💵 ГОТІВКА (Взяти ${job.orderPrice} ₴)", AppColors.Warning)
                                "buyout" -> Pair("💰 ВИКУП (Викупити за ${job.orderPrice} ₴)", AppColors.Error)
                                else -> Pair("Оплата: ${job.paymentType}", AppColors.Primary)
                            }

                            Box(modifier = Modifier.fillMaxWidth().background(paymentInfo.second.copy(alpha = 0.12f)).padding(16.dp)) {
                                Text(paymentInfo.first, fontWeight = FontWeight.ExtraBold, color = paymentInfo.second, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }

                            if (!job.comment.isNullOrEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF7ED)).padding(16.dp)) {
                                    Text("💬 Коментар: ${job.comment}", fontSize = 15.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // --- КАРТКА ЗАКЛАДУ ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isStep1Active || isStep1Done) 1f else 0.5f)
                            .border(BorderStroke(if (isStep1Active) 2.dp else 1.dp, if (isStep1Active) AppColors.Primary else Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
                            .shadow(if (isStep1Active) 12.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = if (isStep1Active) AppColors.Primary.copy(alpha = 0.2f) else Color.Black.copy(0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.background(if (isStep1Active) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("КРОК 1: ЗАКЛАД", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isStep1Active) AppColors.Primary else AppColors.TextSecondary, letterSpacing = 1.sp)
                                        }
                                        // ТАЙМЕР ПУТІ В ЗАКЛАД І ОЧІКУВАННЯ
                                        StepTimer(
                                            startTimeIso = job.assignedAt,
                                            endTimeIso = job.pickedUpAt,
                                            isActive = isStep1Active
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(job.partnerName, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            AddressItem(icon = Icons.Default.LocationOn, text = job.partnerAddress)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.partnerAddress, null, null) },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                if (!job.partnerPhone.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Card(
                                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f)),
                                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                            try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${job.partnerPhone}"))) } catch (e: Exception) {}
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- КАРТКА КЛІЄНТА ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (isStep2Active || isStep2Done) 1f else 0.5f)
                            .border(BorderStroke(if (isStep2Active) 2.dp else 1.dp, if (isStep2Active) AppColors.Primary else Color.LightGray.copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
                            .shadow(if (isStep2Active) 12.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = if (isStep2Active) AppColors.Primary.copy(alpha = 0.2f) else Color.Black.copy(0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.background(if (isStep2Active) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("КРОК 2: КЛІЄНТ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isStep2Active) AppColors.Primary else AppColors.TextSecondary, letterSpacing = 1.sp)
                                        }
                                        // ТАЙМЕР ДОСТАВКИ КЛІЄНТУ
                                        StepTimer(
                                            startTimeIso = job.pickedUpAt,
                                            endTimeIso = job.deliveredAt,
                                            isActive = isStep2Active
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(job.customerName ?: "Ім'я не вказано", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            AddressItem(icon = Icons.Default.Home, text = job.customerAddress)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.customerAddress, job.customerLat, job.customerLon) },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f)),
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).clickable {
                                        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${job.customerPhone}"))) } catch (e: Exception) {}
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // --- КАРТКА ПОВЕРНЕННЯ КОШТІВ ---
                if (job.isReturnRequired && isStep2Done) {
                    val isStep3Active = job.serverStatus == "returning"
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(BorderStroke(2.dp, AppColors.Warning), RoundedCornerShape(24.dp)).shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AppColors.Warning.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(AppColors.Warning.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("КРОК 3: ПОВЕРНЕННЯ КОШТІВ", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AppColors.Warning, letterSpacing = 1.sp)
                                    }
                                    // ТАЙМЕР ПОВЕРНЕННЯ КОШТІВ
                                    StepTimer(
                                        startTimeIso = job.deliveredAt,
                                        endTimeIso = job.completedAt,
                                        isActive = isStep3Active
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Поверніть гроші в заклад", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppColors.TextPrimary)
                                Spacer(modifier = Modifier.height(20.dp))
                                AddressItem(icon = Icons.Default.ArrowForward, text = job.partnerAddress)

                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { openGoogleMaps(job.partnerAddress, null, null) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, AppColors.Warning),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Warning)
                                ) {
                                    Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Маршрут назад", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- КНОПКА ДІЇ ЗНИЗУ З ПРИКОЛЬНОЮ ТІННЮ ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .shadow(24.dp, spotColor = Color.Black.copy(alpha = 0.15f))
                .padding(20.dp)
        ) {
            when (job.serverStatus) {
                "assigned" -> ModernButton("Я в закладі", { isActionLoading = true; onArrivedPickup(job.id) }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Primary, isLoading = isActionLoading)
                "arrived_pickup", "ready" -> ModernButton("Забрав замовлення", { isActionLoading = true; onUpdateStatus(job.id, "picked_up") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary, isLoading = isActionLoading)
                "picked_up" -> ModernButton("Успішно доставлено", { isActionLoading = true; onUpdateStatus(job.id, "delivered") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary, isLoading = isActionLoading)
                "returning" -> Text("Очікування підтвердження повернення...", modifier = Modifier.align(Alignment.Center).padding(8.dp), color = AppColors.TextSecondary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --- ПІД-ЕКРАН: ЧАТ ---
@Composable
fun ChatView(jobId: Int, cookie: String) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        try {
            messages = RetrofitClient.apiService.getChatMessages(cookie, jobId)
        } catch (e: Exception) {}
    }

    LaunchedEffect(Unit) {
        RetrofitClient.webSocketManager.messages.collect { messageJson ->
            try {
                val json = JSONObject(messageJson)
                if (json.optString("type") == "chat_message" && json.optInt("job_id") == jobId) {
                    val newMessage = ChatMessage(
                        role = json.optString("role"),
                        text = json.optString("text"),
                        time = json.optString("time")
                    )
                    messages = messages + newMessage
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isMe = msg.role == "courier"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .background(
                                if (isMe) AppColors.ChatBubbleSelf else AppColors.Surface,
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isMe) 20.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 20.dp
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(msg.text, fontSize = 16.sp, color = if (isMe) AppColors.ChatTextSelf else AppColors.TextPrimary, lineHeight = 22.sp)
                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = if (isMe) AppColors.ChatTextSelf.copy(alpha = 0.7f) else AppColors.TextSecondary,
                                modifier = Modifier.align(Alignment.End).padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = AppColors.Surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Повідомлення...", color = AppColors.TextSecondary) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        cursorColor = AppColors.Primary,
                        focusedContainerColor = AppColors.Background,
                        unfocusedContainerColor = AppColors.Background
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""

                            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val newMsg = ChatMessage(role = "courier", text = textToSend, time = currentTime)
                            messages = messages + newMsg

                            isSending = true
                            scope.launch {
                                try {
                                    listState.animateScrollToItem(0)
                                    RetrofitClient.apiService.sendChatMessage(cookie, jobId, textToSend, "courier")
                                    messages = RetrofitClient.apiService.getChatMessages(cookie, jobId)
                                } catch (e: Exception) {
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (inputText.isNotBlank()) AppColors.Primary else AppColors.Inactive, CircleShape)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Надіслати", tint = Color.White, modifier = Modifier.size(22.dp).padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ЕКРАН ІСТОРІЇ ЗАМОВЛЕНЬ (History)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryOrder>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Історія замовлень", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = AppColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (history.isEmpty() && !isLoading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Історія порожня", color = AppColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history) { order -> HistoryOrderCard(order) }
                }
            }
        }
    }
}

@Composable
fun HistoryOrderCard(order: HistoryOrder) {
    val isDelivered = order.status == "delivered"

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Замовлення #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppColors.TextPrimary)
                Text(order.date, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))
            AddressItem(icon = Icons.Default.LocationOn, text = order.address)

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isDelivered) AppColors.Secondary.copy(alpha = 0.1f) else AppColors.Error.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isDelivered) "Виконано" else "Скасовано",
                        color = if (isDelivered) AppColors.Secondary else AppColors.Error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text("+${order.price} ₴", fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (isDelivered) AppColors.Secondary else AppColors.TextSecondary)
            }
        }
    }
}