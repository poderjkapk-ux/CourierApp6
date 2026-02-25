package com.restify.courierapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// ==========================================
// 0. ДИЗАЙН-СИСТЕМА (Цвета и Кастомные Компоненты)
// ==========================================

object AppColors {
    val Primary = Color(0xFF006CFF) // Яркий современный синий
    val PrimaryDark = Color(0xFF0050C2)
    val Secondary = Color(0xFF00C853) // Сочный зеленый для денег/успеха
    val Background = Color(0xFFF4F7FC) // Очень светло-серо-голубой фон
    val Surface = Color.White
    val TextPrimary = Color(0xFF1A1C20)
    val TextSecondary = Color(0xFF6C727A)
    val Error = Color(0xFFE53935)
    val ChatBubbleSelf = Color(0xFFE3F2FD)
    val ChatBubbleOther = Color(0xFFF5F6F8)
}

// Кастомная кнопка с градиентом или сплошным цветом
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
        modifier = modifier.height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (enabled) 4.dp else 0.dp, pressedElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Кастомное поле ввода
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
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedLabelColor = AppColors.Primary,
            cursorColor = AppColors.Primary
        ),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true
    )
}

// Элемент списка с иконкой для адресов
@Composable
fun AddressItem(icon: ImageVector, text: String, label: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AppColors.Background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            if (label != null) {
                Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
            }
            Text(text, fontSize = 15.sp, color = AppColors.TextPrimary, lineHeight = 20.sp)
        }
    }
}

// ==========================================
// 1. ЭКРАН АВТОРИЗАЦИИ (Login Screen)
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
        // Верхняя декоративная часть
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.Primary, AppColors.PrimaryDark)
                    )
                )
        ) {
            Text(
                "Restify\nDelivery",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            )
        }

        // Карточка входа
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 200.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Вхід для кур'єра", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Spacer(modifier = Modifier.height(24.dp))

                ModernTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Номер телефону",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Пароль",
                    visualTransformation = PasswordVisualTransformation()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.Error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage, color = AppColors.Error, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                ModernButton(
                    text = "Увійти",
                    onClick = { onLoginClick(phone, password) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    enabled = phone.isNotBlank() && password.isNotBlank(),
                    icon = Icons.Default.ArrowForward
                )
            }
        }
    }
}

// ==========================================
// 2. ЭКРАН СПИСКА ЗАКАЗОВ (Orders List)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OpenOrder>,
    isOnline: Boolean,
    onToggleStatus: (Boolean) -> Unit,
    onAcceptOrder: (Int) -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Доступні замовлення", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface, titleContentColor = AppColors.TextPrimary),
                actions = {
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = if (isOnline) AppColors.Secondary.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
                        modifier = Modifier.padding(end = 16.dp).clickable { onToggleStatus(!isOnline) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(if (isOnline) AppColors.Secondary else Color.Gray, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOnline) "Онлайн" else "Офлайн",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOnline) AppColors.Secondary else Color.Gray
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (orders.isEmpty() && !isLoading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Зараз немає замовлень", color = AppColors.TextSecondary, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(order = order, onAcceptClick = onAcceptOrder)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OpenOrder, onAcceptClick: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Заголовок: Ресторан и Цена
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.restaurantName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${order.fee.toInt()} ₴",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = AppColors.Secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Адреса
            AddressItem(icon = Icons.Default.LocationOn, text = order.restaurantAddress, label = "Забрати")
            Spacer(modifier = Modifier.height(12.dp))
            // Рисуем пунктирную линию между точками
            Box(modifier = Modifier.padding(start = 17.dp).height(12.dp).width(2.dp).background(Color.LightGray))
            Spacer(modifier = Modifier.height(12.dp))
            AddressItem(icon = Icons.Default.Home, text = order.dropoffAddress, label = "Доставити")

            Spacer(modifier = Modifier.height(20.dp))

            // Доп. инфо и кнопка
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (order.distTrip != null) {
                    Text("~${order.distTrip}", fontSize = 13.sp, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                ModernButton(
                    text = "Прийняти",
                    onClick = { onAcceptClick(order.id) },
                    modifier = Modifier.height(44.dp),
                    backgroundColor = AppColors.Primary
                )
            }
        }
    }
}

// ==========================================
// 3. ЭКРАН АКТИВНОГО ЗАКАЗА (Active Order)
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
    val tabs = listOf("Деталі", "Карта", "Чат")

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            Column(modifier = Modifier.background(AppColors.Surface)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Замовлення #${job.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(getStatusText(job.serverStatus), fontSize = 13.sp, color = getStatusColor(job.serverStatus))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
                    actions = {
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Оновити", tint = AppColors.TextSecondary) }
                    }
                )

                // Кастомный TabRow
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = AppColors.Surface,
                    contentColor = AppColors.Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = AppColors.Primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            },
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
                0 -> OrderDetailsView(job, onArrivedPickup, onUpdateStatus)
                1 -> OsmMapView(job.customerLat, job.customerLon)
                2 -> ChatView(job.id, cookie)
            }
        }
    }
}

// Вспомогательные функции для статусов
fun getStatusText(status: String): String = when(status) {
    "assigned" -> "Прямуйте до закладу"
    "arrived_pickup" -> "Очікуйте видачі"
    "ready" -> "Замовлення готове!"
    "picked_up" -> "Прямуйте до клієнта"
    "returning" -> "Повернення коштів"
    else -> status
}

fun getStatusColor(status: String): Color = when(status) {
    "assigned", "picked_up" -> AppColors.Primary
    "ready" -> AppColors.Secondary
    else -> AppColors.TextSecondary
}

// --- ПОД-ЭКРАН: ДЕТАЛИ ---
@Composable
fun OrderDetailsView(
    job: ActiveJobDetail,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(16.dp)
        ) {
            // Карточка Заведения
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ЗАКЛАД", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(job.partnerName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddressItem(icon = Icons.Default.LocationOn, text = job.partnerAddress)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Карточка Клиента
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("КЛІЄНТ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary)
                        // Кнопка звонка
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${job.customerPhone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = AppColors.Primary, modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(job.customerName ?: "Ім'я не вказано", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddressItem(icon = Icons.Default.Home, text = job.customerAddress)

                    if (!job.comment.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("Коментар: ${job.comment}", fontSize = 14.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // Финансовый блок
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Сума замовлення:", color = AppColors.TextSecondary)
                Text("${job.orderPrice} ₴", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ваш дохід:", color = AppColors.TextSecondary, fontSize = 16.sp)
                Text("${job.deliveryFee} ₴", fontWeight = FontWeight.Black, color = AppColors.Secondary, fontSize = 20.sp)
            }

        }

        // Кнопка действия на всю ширину внизу
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .shadow(8.dp)
            .padding(16.dp)) {

            when (job.serverStatus) {
                "assigned" -> ModernButton("Я в закладі", { onArrivedPickup(job.id) }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Primary)
                "arrived_pickup", "ready" -> ModernButton("Забрав замовлення", { onUpdateStatus(job.id, "picked_up") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary)
                "picked_up" -> ModernButton("Успішно доставлено", { onUpdateStatus(job.id, "delivered") }, Modifier.fillMaxWidth(), backgroundColor = AppColors.Secondary)
                "returning" -> Text("Очікування підтвердження повернення...", modifier = Modifier.align(Alignment.Center), color = AppColors.TextSecondary)
            }
        }
    }
}

// --- ПОД-ЭКРАН: КАРТА (OSM) ---
// Оставляем функционально как было, но меняем контейнер
@Composable
fun OsmMapView(lat: Double?, lon: Double?) {
    if (lat == null || lon == null) {
        Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) { Text("Координати відсутні", color = AppColors.TextSecondary) }
        return
    }
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
                val point = GeoPoint(lat, lon)
                controller.setCenter(point)

                val marker = Marker(this)
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Клієнт"
                // Можно заменить иконку маркера на кастомную здесь
                overlays.add(marker)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// --- ПОД-ЭКРАН: ЧАТ ---
@Composable
fun ChatView(jobId: Int, cookie: String) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            try { messages = RetrofitClient.apiService.getChatMessages(cookie, jobId) } catch (e: Exception) {}
            delay(3000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isMe = msg.role == "courier"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(1.dp, RoundedCornerShape(18.dp))
                            .background(
                                if (isMe) AppColors.ChatBubbleSelf else AppColors.Surface,
                                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isMe) 18.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 18.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(msg.text, fontSize = 15.sp, color = AppColors.TextPrimary)
                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Панель ввода сообщения
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = AppColors.Surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Повідомлення...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                        cursorColor = AppColors.Primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            isSending = true
                            scope.launch {
                                try {
                                    RetrofitClient.apiService.sendChatMessage(cookie, jobId, textToSend, "courier")
                                    messages = RetrofitClient.apiService.getChatMessages(cookie, jobId)
                                } catch (e: Exception) {} finally { isSending = false }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppColors.Primary, CircleShape)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Надіслати", tint = Color.White)
                    }
                }
            }
        }
    }
}