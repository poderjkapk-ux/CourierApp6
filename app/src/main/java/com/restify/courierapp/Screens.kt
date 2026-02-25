package com.restify.courierapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. ЕКРАН АВТОРИЗАЦІЇ
// ==========================================
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Вхід для кур'єра", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Номер телефону") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(errorMessage, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onLoginClick(phone, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && phone.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Увійти", fontSize = 18.sp)
            }
        }
    }
}

// ==========================================
// 2. ЕКРАН ВІЛЬНИХ ЗАМОВЛЕНЬ
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    orders: List<OpenOrder>,
    onAcceptOrder: (Int) -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вільні замовлення", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Немає доступних замовлень", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🍽 Заклад: ${order.restaurantName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("📍 Звідки: ${order.restaurantAddress}", modifier = Modifier.padding(top = 4.dp))
                            Text("🏠 Куди: ${order.dropoffAddress}", modifier = Modifier.padding(top = 4.dp))

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("💰 Доставка: ${order.fee} грн", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            if (!order.comment.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Коментар: ${order.comment}", color = Color.DarkGray, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onAcceptOrder(order.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Прийняти замовлення")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ЕКРАН АКТИВНОГО ЗАМОВЛЕННЯ
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrderScreen(
    job: ActiveJobDetail,
    onArrivedPickup: (Int) -> Unit,
    onUpdateStatus: (Int, String) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("В роботі #${job.id}") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Оновити")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🍽 Заклад: ${job.partnerName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("📍 Адреса: ${job.partnerAddress}")

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("👤 Клієнт: ${job.customerName ?: "Без імені"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("🏠 Куди: ${job.customerAddress}")

                    // Клікабельний телефон клієнта
                    Text(
                        text = "📞 Телефон: ${job.customerPhone}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${job.customerPhone}"))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Ціна замовлення: ${job.orderPrice} грн")
                    Text("💰 Доставка: ${job.deliveryFee} грн", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)

                    if (!job.comment.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                            Text(job.comment, modifier = Modifier.padding(8.dp), color = Color(0xFFE65100))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Логіка зміни кнопок в залежності від статусу на сервері
            when (job.serverStatus) {
                "assigned" -> {
                    Button(
                        onClick = { onArrivedPickup(job.id) },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("Я в закладі", fontSize = 18.sp)
                    }
                }
                "arrived_pickup", "ready" -> {
                    Button(
                        onClick = { onUpdateStatus(job.id, "picked_up") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                    ) {
                        Text("Забрав замовлення", fontSize = 18.sp)
                    }
                }
                "picked_up" -> {
                    Button(
                        onClick = { onUpdateStatus(job.id, "delivered") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                    ) {
                        Text("Доставлено клієнту", fontSize = 18.sp)
                    }
                }
                "returning" -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Повернення коштів в заклад...", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
