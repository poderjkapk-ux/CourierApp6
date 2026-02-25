package com.restify.courierapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    // Функція для запуску служби геолокації
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTracker::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація OpenStreetMap (потрібно для відображення карти)
        Configuration.getInstance().userAgentValue = packageName

        val sharedPref = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val coroutineScope = rememberCoroutineScope()
                    val savedCookie = sharedPref.getString("cookie", null)

                    // --- НОВЕ: Управління життєвим циклом WebSocket ---
                    LaunchedEffect(Unit) {
                        if (savedCookie != null) {
                            RetrofitClient.webSocketManager.connect(savedCookie)
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            RetrofitClient.webSocketManager.disconnect()
                        }
                    }
                    // ---------------------------------------------------

                    // БЛОК ЗАПИТУ ДОЗВОЛІВ (GPS та Сповіщення)
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

                    // Запитуємо дозволи при старті
                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            if (savedCookie != null) {
                                startLocationService()
                            }
                        }
                    }

                    val startDestination = if (savedCookie != null) "orders" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {

                        // РОУТ 1: ЛОГІН
                        composable("login") {
                            var isLoading by remember { mutableStateOf(false) }
                            var errorMessage by remember { mutableStateOf<String?>(null) }

                            LoginScreen(
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onLoginClick = { phone, password ->
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.login(phone, password)
                                            if (response.isSuccessful || response.code() == 302 || response.code() == 303) {
                                                val tokenCookie = response.headers().values("Set-Cookie").firstOrNull { it.contains("courier_token") }

                                                if (tokenCookie != null) {
                                                    val cookieValue = tokenCookie.split(";")[0]
                                                    sharedPref.edit().putString("cookie", cookieValue).apply()

                                                    // --- НОВЕ: Підключаємо WebSocket одразу після успішного входу ---
                                                    RetrofitClient.webSocketManager.connect(cookieValue)
                                                    // ----------------------------------------------------------------

                                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val token = task.result
                                                            coroutineScope.launch {
                                                                try {
                                                                    RetrofitClient.apiService.sendFcmToken(cookieValue, token)
                                                                } catch (e: Exception) {}
                                                            }
                                                        }
                                                    }

                                                    if (permissionsState.allPermissionsGranted) {
                                                        startLocationService()
                                                    }

                                                    navController.navigate("orders") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                } else {
                                                    errorMessage = "Помилка: Немає токена"
                                                }
                                            } else {
                                                errorMessage = "Невірний телефон або пароль"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Помилка мережі"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }

                        // РОУТ 2: СПИСОК ЗАМОВЛЕНЬ
                        composable("orders") {
                            var ordersList by remember { mutableStateOf<List<OpenOrder>>(emptyList()) }
                            var isLoading by remember { mutableStateOf(true) }
                            var isOnline by remember { mutableStateOf(true) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchData() {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val activeJobRes = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (activeJobRes.active) {
                                            navController.navigate("active_order") {
                                                popUpTo("orders") { inclusive = true }
                                            }
                                            return@launch
                                        }
                                        ordersList = RetrofitClient.apiService.getOpenOrders(currentCookie, lat = 0.0, lon = 0.0)
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }

                            LaunchedEffect(Unit) { fetchData() }

                            // --- НОВЕ: Слухаємо WebSocket події для миттєвого оновлення списку ---
                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")
                                        if (type == "new_order" || type == "job_update") {
                                            fetchData() // Оновлюємо список
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            // -----------------------------------------------------------------------

                            OrdersListScreen(
                                orders = ordersList,
                                isLoading = isLoading,
                                isOnline = isOnline,
                                onToggleStatus = { newStatus ->
                                    isOnline = newStatus
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.toggleStatus(currentCookie)
                                        } catch (e: Exception) {
                                            isOnline = !newStatus
                                        }
                                    }
                                },
                                onRefresh = { fetchData() },
                                onAcceptOrder = { jobId ->
                                    coroutineScope.launch {
                                        try {
                                            val res = RetrofitClient.apiService.acceptOrder(currentCookie, jobId)
                                            if (res.isSuccessful) fetchData()
                                        } catch (e: Exception) {}
                                    }
                                }
                            )
                        }

                        // РОУТ 3: АКТИВНЕ ЗАМОВЛЕННЯ (З підтримкою чату та карти)
                        composable("active_order") {
                            var activeJob by remember { mutableStateOf<ActiveJobDetail?>(null) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchActiveJob() {
                                coroutineScope.launch {
                                    try {
                                        val res = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (res.active && res.job != null) activeJob = res.job
                                        else navController.navigate("orders") { popUpTo("active_order") { inclusive = true } }
                                    } catch (e: Exception) {}
                                }
                            }

                            LaunchedEffect(Unit) { fetchActiveJob() }

                            // --- НОВЕ: Слухаємо WebSocket події для оновлення активного замовлення ---
                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")
                                        // Оновлюємо, якщо статус замовлення змінився на сервері
                                        if (type == "job_update" || type == "job_ready") {
                                            fetchActiveJob()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            // --------------------------------------------------------------------------

                            activeJob?.let { job ->
                                ActiveOrderScreen(
                                    job = job,
                                    cookie = currentCookie, // Передаємо кукі для чату
                                    onRefresh = { fetchActiveJob() },
                                    onArrivedPickup = { jobId ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.arrivedAtPickup(currentCookie, jobId); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    },
                                    onUpdateStatus = { jobId, status ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.updateJobStatus(currentCookie, jobId, status); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}